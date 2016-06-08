package plantrdf.servlet;

import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.URLConnection;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import plantrdf.content.text.BooleanContentHandler;

@WebListener
public class ConfigurationContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		URLConnection.setContentHandlerFactory(new ContentHandlerFactory() {
			@Override
			public ContentHandler createContentHandler(String mimetype) {
				if (MediaTypes.BOOLEAN_CONTENT_TYPE.equals(mimetype)) {
					return new BooleanContentHandler();
				}
				return null;
			}
		});
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}
}
