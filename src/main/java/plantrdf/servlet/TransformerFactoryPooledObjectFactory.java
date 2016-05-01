package plantrdf.servlet;

import javax.xml.transform.TransformerFactory;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class TransformerFactoryPooledObjectFactory extends BasePooledObjectFactory<TransformerFactory> {

	@Override
	public TransformerFactory create() throws Exception {
		return TransformerFactory.newInstance();
	}

	@Override
	public PooledObject<TransformerFactory> wrap(TransformerFactory factory) {
		return new DefaultPooledObject<>(factory);
	}

}
