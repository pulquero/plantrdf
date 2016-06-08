package plantrdf.servlet;

import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.FileNameMap;
import java.net.URLConnection;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import plantrdf.content.text.BooleanContentHandler;

@WebListener
public class ConfigurationContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		URLConnection.setFileNameMap(new FileNameMap() {
			private FileNameMap delegate = URLConnection.getFileNameMap();
			@Override
			public String getContentTypeFor(String fileName) {
				if(fileName.endsWith(".rq")) {
					return MediaTypes.SPARQL_QUERY_CONTENT_TYPE;
				}
				return delegate.getContentTypeFor(fileName);
			}
		});
		URLConnection.setContentHandlerFactory(new ContentHandlerFactory() {
			@Override
			public ContentHandler createContentHandler(String mimetype) {
				if (MediaTypes.BOOLEAN_CONTENT_TYPE.equals(mimetype)) {
					return new BooleanContentHandler();
				} else if (MediaTypes.SPARQL_QUERY_CONTENT_TYPE.equals(mimetype)) {
					return new plantrdf.content.application.sparql_query();
				}
				return null;
			}
		});
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
}
