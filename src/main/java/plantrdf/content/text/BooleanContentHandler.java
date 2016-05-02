package plantrdf.content.text;

import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;
import java.net.URLConnection;

public class BooleanContentHandler extends ContentHandler {
	@Override
	public Object getContent(URLConnection urlc) throws IOException {
		byte[] buf = new byte[5];
		int len;
		try(InputStream in = urlc.getInputStream()) {
			len = in.read(buf);
		}
		return "true".equals(new String(buf, 0, len, "US-ASCII"));
	}
}
