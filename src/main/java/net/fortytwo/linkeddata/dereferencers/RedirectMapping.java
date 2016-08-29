/**
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package net.fortytwo.linkeddata.dereferencers;

import java.net.URL;

/**
 * @author markh
 *
 */
public interface RedirectMapping {

	String translate(String redirectUrl, URL originalUrl);
}
