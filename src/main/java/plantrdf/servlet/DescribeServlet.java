package plantrdf.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DescribeServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5798564486501498686L;

	static {
		Authenticator.setDefault(new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return PASSWORD_AUTH.get();
			}
		});
	}

	private static final ThreadLocal<PasswordAuthentication> PASSWORD_AUTH = new ThreadLocal<PasswordAuthentication>();

	private PasswordAuthentication credentials;

	@Override
	public void init() throws ServletException {
		credentials = new PasswordAuthentication("plantrdf", "m3tl".toCharArray());
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getPathInfo();
		int pos = pathInfo.indexOf('/', 1);
		if(pos == -1) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid resource URI: "+req.getRequestURI());
			return;
		}

		String sesameRepos = "/openrdf-sesame/repositories/";
		String repo = pathInfo.substring(1, pos);
		String graph = "http://plantrdf-morethancode.rhcloud.com/gardens/"+repo;

		boolean htmlParam = (req.getParameter("html") != null);
		boolean editParam = (req.getParameter("edit") != null);
		boolean showHtml = htmlParam || editParam;
		boolean acceptHtml = false;
		for(Enumeration<String> iter = req.getHeaders("Accept"); iter.hasMoreElements(); ) {
			if(iter.nextElement().contains("html")) {
				acceptHtml = true;
				break;
			}
		}
		if(!showHtml && acceptHtml) {
			sendRedirect(resp, req.getRequestURL().append("?html").toString());
			return;
		}

		URL namespaceUrl = createUrl(req, sesameRepos+repo+"/namespaces");
		final Map<String,String> nsMap = new HashMap<String,String>();
		PASSWORD_AUTH.set(credentials);
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			URLConnection nsConn = namespaceUrl.openConnection();
			nsConn.setRequestProperty("Accept", "application/sparql-results+xml");
			InputStream nsIn = nsConn.getInputStream();
			try {
				parser.parse(nsIn, new DefaultHandler() {
					String name;
					String prefix;
					String namespace;
					StringBuilder buf;

					@Override
					public void startElement(String uri, String localName, String qName, Attributes attributes)
							throws SAXException {
						if("result".equals(qName)) {
							prefix = null;
							namespace = null;
						}
						else if("binding".equals(qName)) {
							name = attributes.getValue("name");
							buf = new StringBuilder();
						}
					}

					@Override
					public void endElement(String uri, String localName, String qName) throws SAXException {
						if("result".equals(qName)) {
							nsMap.put(namespace, prefix);
						}
						else if("binding".equals(qName)) {
							String v = buf.toString().trim();
							if("prefix".equals(name)) {
								prefix = v;
							}
							else if("namespace".equals(name)) {
								namespace = v;
							}
							name = null;
							buf = null;
						}
					}

					@Override
					public void characters(char[] ch, int start, int length) throws SAXException {
						if(buf != null) {
							buf.append(ch, start, length);
						}
					}
					
				}, namespaceUrl.toString());
			}
			finally {
				nsIn.close();
			}
		}
		catch (ParserConfigurationException e) {
			throw new ServletException(e);
		}
		catch (SAXException e) {
			throw new ServletException(e);
		}
		finally {
			PASSWORD_AUTH.remove();
		}

		URL xslUrl = createUrl(req, req.getContextPath()+"/describe.xsl");
		String describeQuery;
		String resource = req.getRequestURL().toString();
		String hashNamespace = req.getRequestURL().append('#').toString();
		boolean isHashNamespace = nsMap.containsKey(hashNamespace);
		if(isHashNamespace) {
			describeQuery = "describe <"+resource+"> ?s "
					+ "where {"
					+ " select distinct ?s "
					+ " where {"
					+ "  filter(strstarts(str(?s), \""+hashNamespace+"\"))"
					+ "  ?s ?p ?o ."
					+ " }"
					+ "}";
		}
		else {
			describeQuery = "describe <"+resource+">";
		}
		URL describeUrl = createUrl(req, sesameRepos+repo+"?query="+URLEncoder.encode(describeQuery, "UTF-8"));

		if(showHtml) {
			resp.setContentType("application/xhtml+xml");
			PASSWORD_AUTH.set(credentials);
			try {
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer t = tf.newTransformer(new StreamSource(xslUrl.toString()));
				t.setParameter("resource", resource);
				if(editParam) {
					t.setParameter("graph", graph);
					t.setParameter("updateEndpoint", sesameRepos+repo+"/statements");
				}
				URLConnection describeConn = describeUrl.openConnection();
				describeConn.setRequestProperty("Accept", "application/rdf+xml");
				InputStream describeIn = describeConn.getInputStream();
				try {
					t.transform(new StreamSource(describeIn, describeUrl.toString()), new StreamResult(resp.getOutputStream()));
				}
				finally {
					describeIn.close();
				}
			}
			catch (TransformerConfigurationException e) {
				throw new ServletException(e);
			}
			catch (TransformerException e) {
				throw new ServletException(e);
			}
			finally {
				PASSWORD_AUTH.remove();
			}
		}
		else if(!acceptHtml) {
			sendRedirect(resp, describeUrl.toString());
		}
		else {
			throw new AssertionError("Unreachable");
		}
	}

	private static URL createUrl(HttpServletRequest req, String path) throws MalformedURLException {
		return new URL(req.getScheme(), req.getServerName(), req.getServerPort(), path);
	}

	private static void sendRedirect(HttpServletResponse resp, String url) throws IOException {
		resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
		resp.setHeader("Location", url);
	}
}
