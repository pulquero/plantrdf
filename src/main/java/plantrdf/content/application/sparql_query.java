package plantrdf.content.application;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ContentHandler;
import java.net.URLConnection;

public class sparql_query extends ContentHandler {
	public static String getContent(InputStream in) throws IOException {
		StringBuilder content = new StringBuilder();
		char[] buf = new char[4096];
		int len;
		try(Reader reader = new InputStreamReader(in, "UTF-8")) {
			while((len = reader.read(buf)) != -1) {
				content.append(buf, 0, len);
			}
		}
		return content.toString();
	}

	@Override
	public Object getContent(URLConnection urlc) throws IOException {
		return getContent(urlc.getInputStream());
	}

	@Override
	public Object getContent(URLConnection urlc, Class[] classes) throws IOException {
		for(Class<?> cls : classes) {
			if(cls == InputStream.class) {
				return urlc.getInputStream();
			}
			else if(cls == String.class) {
				return getContent(urlc);
			}
		}
		return null;
	}
}
