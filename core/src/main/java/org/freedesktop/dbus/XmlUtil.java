/*
   D-Bus Java Implementation
   Copyright (c) 2017 Sebastian Kuerten

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class XmlUtil
{

	private static final byte[] NO_DATA = new byte[0];

	public static void disableDtdTypeResolving(DocumentBuilder builder)
	{
		builder.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
			{
				return new InputSource(new ByteArrayInputStream(NO_DATA));
			}
		});
	}

}
