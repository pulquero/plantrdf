package plantrdf.content.text;

import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;
import java.net.URLConnection;

public class BooleanContentHandler extends ContentHandler {
	public static boolean getContent(InputStream in) throws IOException {
		byte[] buf = new byte[5];
		int len;
		try {
			len = in.read(buf);
		}
		finally {
			in.close();
		}
		return "true".equals(new String(buf, 0, len, "US-ASCII"));
	}

	@Override
	public Object getContent(URLConnection urlc) throws IOException {
		return getContent(urlc.getInputStream());
	}
}
