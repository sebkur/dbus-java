/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

import static org.freedesktop.dbus.Gettext._;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusAddress
{

	final static Logger logger = LoggerFactory.getLogger(BusAddress.class);

	private String type;
	private Map<String, String> parameters;

	public BusAddress(String address) throws ParseException
	{
		if (null == address || "".equals(address)) {
			throw new ParseException(_("Bus address is blank"), 0);
		}
		logger.trace("Parsing bus address: " + address);
		String[] ss = address.split(":", 2);
		if (ss.length < 2) {
			throw new ParseException(_("Bus address is invalid: ") + address,
					0);
		}
		type = ss[0];
		logger.trace("Transport type: " + type);
		String[] ps = ss[1].split(",");
		parameters = new HashMap<String, String>();
		for (String p : ps) {
			String[] kv = p.split("=", 2);
			parameters.put(kv[0], kv[1]);
		}
		logger.trace("Transport options: " + parameters);
	}

	public String getType()
	{
		return type;
	}

	public String getParameter(String key)
	{
		return parameters.get(key);
	}

	@Override
	public String toString()
	{
		return type + ": " + parameters;
	}

}
