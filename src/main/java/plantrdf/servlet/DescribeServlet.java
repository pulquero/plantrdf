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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
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
import plantrdf.util.TransformerFactoryPooledObjectFactory;

public class DescribeServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5798564486501498686L;

	private static final String PLANT_CLASS = "http://plantrdf-morethancode.rhcloud.com/schema#Plant";

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
	private ObjectPool<TransformerFactory> transformerFactoryPool;

	@Override
	public void init() throws ServletException {
		sesameUrl = getInitParameter("sesameUrl");
		String username = getInitParameter("username");
		String password = getInitParameter("password");
		credentials = new PasswordAuthentication(username, password.toCharArray());
		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setMinIdle(1);
		poolConfig.setSoftMinEvictableIdleTimeMillis(TimeUnit.SECONDS.toMillis(10l));
		poolConfig.setTimeBetweenEvictionRunsMillis(TimeUnit.MINUTES.toMillis(1l));
		poolConfig.setTestOnBorrow(false);
		poolConfig.setTestOnCreate(false);
		poolConfig.setTestOnReturn(false);
		poolConfig.setTestWhileIdle(false);
		parserFactoryPool = new GenericObjectPool<>(new SAXParserFactoryPooledObjectFactory(), poolConfig);
		transformerFactoryPool = new GenericObjectPool<>(new TransformerFactoryPooledObjectFactory(), poolConfig);

		URLConnection.setContentHandlerFactory(new ContentHandlerFactory() {
			@Override
			public ContentHandler createContentHandler(String mimetype) {
				if ("text/boolean".equals(mimetype)) {
					return new BooleanContentHandler();
				}
				return null;
			}
		});
	}

	@Override
	public void destroy() {
		parserFactoryPool.close();
		transformerFactoryPool.close();
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

		String resource = req.getRequestURL().toString();

		boolean htmlParam = (req.getParameter("html") != null);
		boolean editParam = (req.getParameter("edit") != null);
		boolean showHtml = htmlParam || editParam;
		boolean acceptHtml = false;
		for (Enumeration<String> iter = req.getHeaders("Accept"); iter.hasMoreElements();) {
			if (iter.nextElement().contains("html")) {
				acceptHtml = true;
				break;
			}
		}

		PASSWORD_AUTH.set(credentials);
		try {
			if (!showHtml && acceptHtml) {
				String redirectUrl;
				String askQuery = String.format("ask where {<%s> a <%s>}", resource, PLANT_CLASS);
				URL plantUrl = createUrl(req, sesameRepos + repo + "?query=" + URLEncoder.encode(askQuery, "UTF-8"));
				URLConnection plantConn = plantUrl.openConnection();
				plantConn.setRequestProperty("Accept", "text/boolean");
				if ((Boolean) plantConn.getContent(new Class[] {Boolean.class})) {
					redirectUrl = "/observation.html";
				} else {
					redirectUrl = req.getRequestURL().append("?html").toString();
				}
				sendRedirect(resp, redirectUrl);
				return;
			}

			URL namespaceUrl = createUrl(req, sesameRepos + repo + "/namespaces");
			final Map<String, String> nsMap = new HashMap<String, String>();
			try {
				SAXParserFactory parserFactory = parserFactoryPool.borrowObject();
				try {
					SAXParser parser = parserFactory.newSAXParser();
					URLConnection nsConn = namespaceUrl.openConnection();
					nsConn.setRequestProperty("Accept", "application/sparql-results+xml");
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
				throw new ServletException(e);
			}

			URL xslUrl = createUrl(req, "describe.xsl");
			String describeQuery;
			String hashNamespace = req.getRequestURL().append('#').toString();
			boolean isHashNamespace = nsMap.containsKey(hashNamespace);
			if (isHashNamespace) {
				describeQuery = String.format(
						"describe <%s> ?s " + "where {" + " select distinct ?s " + " where {"
								+ "  filter(strstarts(str(?s), \"%s\"))" + "  ?s ?p ?o ." + " }" + "}",
						resource, hashNamespace);
			} else {
				describeQuery = String.format("describe <%s>", resource);
			}
			URL describeUrl = createUrl(req,
					sesameRepos + repo + "?query=" + URLEncoder.encode(describeQuery, "UTF-8"));

			if (showHtml) {
				resp.setContentType("application/xhtml+xml");
				try {
					TransformerFactory transformerFactory = transformerFactoryPool.borrowObject();
					try {
						Transformer transformer = transformerFactory
								.newTransformer(new StreamSource(xslUrl.toString()));
						transformer.setParameter("resource", resource);
						if (editParam) {
							transformer.setParameter("graph", graph);
							transformer.setParameter("updateEndpoint", sesameRepos + repo + "/statements");
						}
						URLConnection describeConn = describeUrl.openConnection();
						describeConn.setRequestProperty("Accept", "application/rdf+xml");
						try (InputStream describeIn = describeConn.getInputStream()) {
							transformer.transform(new StreamSource(describeIn, describeUrl.toString()),
									new StreamResult(resp.getOutputStream()));
						}
					} finally {
						transformerFactoryPool.returnObject(transformerFactory);
					}
				} catch (Exception e) {
					throw new ServletException(e);
				}
			} else if (!acceptHtml) {
				sendRedirect(resp, describeUrl.toString());
			} else {
				throw new AssertionError("Unreachable code");
			}
		} finally {
			PASSWORD_AUTH.remove();
		}
	}

	private static URL createUrl(HttpServletRequest req, String path) throws MalformedURLException {
		return new URL(new URL(req.getScheme(), req.getServerName(), req.getServerPort(), req.getContextPath()), path);
	}

	private static void sendRedirect(HttpServletResponse resp, String url) throws IOException {
		resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
		resp.setHeader("Location", url);
	}
}
