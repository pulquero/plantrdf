package plantrdf.content.application;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ContentHandler;
import java.net.URLConnection;

public class sparql_query extends ContentHandler {
	@Override
	public Object getContent(URLConnection urlc) throws IOException {
		StringBuilder content = new StringBuilder();
		char[] buf = new char[4096];
		int len;
		try(Reader in = new InputStreamReader(urlc.getInputStream(), "UTF-8")) {
			while((len = in.read(buf)) != -1) {
				content.append(buf, 0, len);
			}
		}
		return content.toString();
	}
}
