package plantrdf.util;

import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class SAXParserFactoryPooledObjectFactory extends BasePooledObjectFactory<SAXParserFactory> {

	@Override
	public SAXParserFactory create() throws Exception {
		return SAXParserFactory.newInstance();
	}

	@Override
	public PooledObject<SAXParserFactory> wrap(SAXParserFactory factory) {
		return new DefaultPooledObject<>(factory);
	}

}
