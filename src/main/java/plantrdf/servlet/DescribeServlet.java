package plantrdf.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import plantrdf.content.text.BooleanContentHandler;
import plantrdf.util.SAXParserFactoryPooledObjectFactory;

public class DescribeServlet extends HttpServlet {
	private static final long serialVersionUID = -5798564486501498686L;

	private static final String HTML_CONTENT_TYPE = "application/xhtml+xml";
	private static final String RDF_CONTENT_TYPE = "application/rdf+xml";
	private static final String BOOLEAN_CONTENT_TYPE = "text/boolean";

	private static final String ACCEPT_HEADER = "Accept";

	private static final String HTML_EXT = "html";
	private static final String ACTION_PARAM = "action";
	private static final String EDIT_ACTION = "edit";
	private static final String OBSERVE_ACTION = "observe";
	private static final String VIEW_PLANT_ACTION = "view-plant";

	static {
		Authenticator.setDefault(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return PASSWORD_AUTH.get();
			}
		});
	}

	private static final ThreadLocal<PasswordAuthentication> PASSWORD_AUTH = new ThreadLocal<PasswordAuthentication>();

	private String sesameUrl;
	private PasswordAuthentication credentials;
	private ObjectPool<SAXParserFactory> parserFactoryPool;
	private Map<String,String> queries;
	private Templates describeXslt;
	private Templates plantXslt;

	@Override
	public void init() throws ServletException {
		ServletContext ctx = getServletContext();
		sesameUrl = getInitParameter("sesameUrl");
		String username = getInitParameter("username");
		String password = getInitParameter("password");
		credentials = new PasswordAuthentication(username, password.toCharArray());
		queries = loadQueries(ctx);
		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setMinIdle(1);
		poolConfig.setSoftMinEvictableIdleTimeMillis(TimeUnit.SECONDS.toMillis(10l));
		poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.MINUTES.toMillis(1l));
		poolConfig.setTestOnBorrow(false);
		poolConfig.setTestOnCreate(false);
		poolConfig.setTestOnReturn(false);
		poolConfig.setTestWhileIdle(false);
		parserFactoryPool = new GenericObjectPool<>(new SAXParserFactoryPooledObjectFactory(), poolConfig);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		describeXslt = createTemplates(transformerFactory, "describe.xsl", ctx);
		plantXslt = createTemplates(transformerFactory, "plant.xsl", ctx);

		URLConnection.setContentHandlerFactory(new ContentHandlerFactory() {
			@Override
			public ContentHandler createContentHandler(String mimetype) {
				if (BOOLEAN_CONTENT_TYPE.equals(mimetype)) {
					return new BooleanContentHandler();
				}
				return null;
			}
		});
	}

	private static Templates createTemplates(TransformerFactory tf, String xslt, ServletContext ctx) throws ServletException
	{
		try {
			return tf.newTemplates(new StreamSource(ctx.getResource("/WEB-INF/xsl/"+xslt).toString()));
		} catch (TransformerConfigurationException | MalformedURLException e) {
			throw new ServletException(e);
		}
	}

	private static Map<String,String> loadQueries(ServletContext ctx) throws ServletException
	{
		Map<String,String> queries = new HashMap<>();
		try {
			for(String queryFile : ctx.getResourcePaths("/WEB-INF/queries/")) {
				String name = queryFile.substring(queryFile.lastIndexOf('/')+1, queryFile.lastIndexOf('.'));
				String query = ctx.getResource(queryFile).getContent().toString();
				queries.put(name, query);
			}
		} catch(IOException e) {
			throw new ServletException(e);
		}
		return queries;
	}

	@Override
	public void destroy() {
		parserFactoryPool.close();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getPathInfo();
		int pos = pathInfo.indexOf('/', 1);
		if (pos == -1) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid resource URI: " + req.getRequestURI());
			return;
		}

		String sesameRepos = sesameUrl + "repositories/";
		String repo = pathInfo.substring(1, pos);
		String graph = "http://plantrdf-morethancode.rhcloud.com/gardens/" + repo;

		URL endpoint = createUrl(req, sesameRepos + repo);

		int extPos = pathInfo.lastIndexOf('.');
		String resource;
		String ext;
		if(extPos != -1) {
			StringBuffer reqUrl = req.getRequestURL();
			resource = reqUrl.substring(0, reqUrl.length()-pathInfo.length()+extPos);
			ext = pathInfo.substring(extPos+1);
		} else {
			resource = req.getRequestURL().toString();
			ext = null;
		}

		boolean showHtml = HTML_EXT.equals(ext);
		if(ext != null && !showHtml) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, String.format("No such resource: %s", resource));
			return;
		}

		PASSWORD_AUTH.set(credentials);
		try {
			if(!ask(queryUrl(endpoint, queries.get("exists"), Collections.singletonMap("resource", iri(resource))))) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, String.format("No such resource: %s", resource));
				return;
			}
			String action = req.getParameter(ACTION_PARAM);
			boolean isRedirected = showHtml || action != null;
			boolean acceptHtml = false;
			for (Enumeration<String> iter = req.getHeaders(ACCEPT_HEADER); iter.hasMoreElements();) {
				if (iter.nextElement().contains("html")) {
					acceptHtml = true;
					break;
				}
			}

			if (!isRedirected && acceptHtml) {
				String redirectUrl;
				if (ask(queryUrl(endpoint, queries.get("isPlant"), Collections.singletonMap("resource", iri(resource))))) {
					boolean doObserve = false;
					Cookie[] cookies = req.getCookies();
					if(cookies != null) {
						for(Cookie cookie : cookies) {
							if ("observe".equals(cookie.getName())) {
								doObserve = "on".equals(cookie.getValue());
								break;
							}
						}
					}
					if (doObserve) {
						redirectUrl = req.getRequestURL().append('?').append(ACTION_PARAM).append('=').append(OBSERVE_ACTION).toString();
					} else {
						redirectUrl = req.getRequestURL().append('.').append(HTML_EXT).append('?').append(ACTION_PARAM).append('=').append(VIEW_PLANT_ACTION).toString();
					}
				} else {
					redirectUrl = req.getRequestURL().append('.').append(HTML_EXT).toString();
				}
				sendRedirect(resp, redirectUrl);
				return;
			}

			if(OBSERVE_ACTION.equals(action)) {
				doObservation(graph, resource, req, resp);
			}
			else {
				String contentType;
				if(showHtml) {
					contentType = HTML_CONTENT_TYPE;
				} else {
					contentType = RDF_CONTENT_TYPE;
				}
				doRdf(endpoint, graph, resource, contentType, action, req, resp);
			}
		} finally {
			PASSWORD_AUTH.remove();
		}
	}

	private void doObservation(String graph, String resource, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.sendRedirect("/observation.html?graph="+URLEncoder.encode(graph, "UTF-8")+"&plant="+URLEncoder.encode(resource, "UTF-8"));
	}

	private void doRdf(URL endpoint, String graph, String resource, String contentType, String action, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		URL namespaceUrl = new URL(endpoint.toString()+"/namespaces");
		final Map<String, String> nsMap = new HashMap<String, String>();
		try {
			SAXParserFactory parserFactory = parserFactoryPool.borrowObject();
			try {
				SAXParser parser = parserFactory.newSAXParser();
				URLConnection nsConn = namespaceUrl.openConnection();
				nsConn.setRequestProperty(ACCEPT_HEADER, "application/sparql-results+xml");
				try (InputStream nsIn = nsConn.getInputStream()) {
					parser.parse(nsIn, new DefaultHandler() {
						String name;
						String prefix;
						String namespace;
						StringBuilder buf;

						@Override
						public void startElement(String uri, String localName, String qName, Attributes attributes)
								throws SAXException {
							if ("result".equals(qName)) {
								prefix = null;
								namespace = null;
							} else if ("binding".equals(qName)) {
								name = attributes.getValue("name");
								buf = new StringBuilder();
							}
						}

						@Override
						public void endElement(String uri, String localName, String qName) throws SAXException {
							if ("result".equals(qName)) {
								nsMap.put(namespace, prefix);
							} else if ("binding".equals(qName)) {
								String v = buf.toString().trim();
								if ("prefix".equals(name)) {
									prefix = v;
								} else if ("namespace".equals(name)) {
									namespace = v;
								}
								name = null;
								buf = null;
							}
						}

						@Override
						public void characters(char[] ch, int start, int length) throws SAXException {
							if (buf != null) {
								buf.append(ch, start, length);
							}
						}

					}, namespaceUrl.toString());
				}
			} finally {
				parserFactoryPool.returnObject(parserFactory);
			}
		} catch (Exception e) {
			throw new IOException(e);
		}

		Templates stylesheet;
		String describeQuery;
		if (VIEW_PLANT_ACTION.equals(action)) {
			stylesheet = plantXslt;
			describeQuery = queries.get("describePlant");
		} else {
			stylesheet = describeXslt;
			String hashNamespace = resource + "#";
			boolean isHashNamespace = nsMap.containsKey(hashNamespace);
			if (isHashNamespace) {
				describeQuery = String.format(
					"describe <%1$s> ?s " + "where {" + " select distinct ?s " + " where {"
						+ "  filter(strstarts(str(?s), \"%2$s\"))" + "  ?s ?p ?o ." + " }" + "}",
					resource, hashNamespace);
			} else {
				describeQuery = queries.get("describeResource");
			}
		}
		URL describeUrl = queryUrl(endpoint, describeQuery, Collections.singletonMap("resource", iri(resource)));

		if (contentType.startsWith(HTML_CONTENT_TYPE)) {
			resp.setContentType(contentType);
			try {
				Transformer transformer = stylesheet.newTransformer();
				transformer.setParameter("resource", resource);
				if (EDIT_ACTION.equals(action)) {
					transformer.setParameter("graph", graph);
					transformer.setParameter("updateEndpoint", new URL(endpoint, "/statements").toString());
				}
				try (InputStream describeIn = rdf(describeUrl)) {
					transformer.transform(new StreamSource(describeIn, describeUrl.toString()),
							new StreamResult(resp.getOutputStream()));
				}
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else {
			sendRedirect(resp, describeUrl.toString());
		}
	}

	private static boolean ask(URL queryUrl) throws IOException {
		URLConnection conn = queryUrl.openConnection();
		conn.setRequestProperty(ACCEPT_HEADER, BOOLEAN_CONTENT_TYPE);
		return (Boolean) conn.getContent(new Class[] {Boolean.class});
	}

	private static InputStream rdf(URL queryUrl) throws IOException {
		URLConnection conn = queryUrl.openConnection();
		conn.setRequestProperty(ACCEPT_HEADER, RDF_CONTENT_TYPE);
		return conn.getInputStream();
	}

	private static URL queryUrl(URL endpoint, String query, Map<String,String> params) throws IOException {
		StringBuilder buf = new StringBuilder(endpoint.getPath());
		buf.append("?query=").append(URLEncoder.encode(query, "UTF-8"));
		for(Map.Entry<String,String> entry : params.entrySet()) {
			buf.append("&$").append(entry.getKey()).append("=").append(entry.getValue());
		}
		return new URL(endpoint, buf.toString());
	}

	private static String iri(String value) {
		return "<"+value+">";
	}

	private static URL createUrl(HttpServletRequest req, String path) throws MalformedURLException {
		return new URL(new URL(req.getScheme(), req.getServerName(), req.getServerPort(), req.getContextPath()), path);
	}

	private static void sendRedirect(HttpServletResponse resp, String url) throws IOException {
		resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
		resp.setHeader("Location", url);
	}
}
