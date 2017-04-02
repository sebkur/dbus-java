/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import org.freedesktop.dbus.BusAddress;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Message;
import org.freedesktop.dbus.MethodCall;
import org.freedesktop.dbus.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLowLevel
{

	final static Logger logger = LoggerFactory.getLogger(TestLowLevel.class);

	public static void main(String[] args) throws Exception
	{
		String addr = System.getenv("DBUS_SESSION_BUS_ADDRESS");
		logger.debug(addr);
		BusAddress address = new BusAddress(addr);
		logger.debug(address.toString());

		Transport conn = new Transport(address);

		Message m = new MethodCall("org.freedesktop.DBus",
				"/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello",
				(byte) 0, null);
		conn.mout.writeMessage(m);
		m = conn.min.readMessage();
		logger.debug(m.getClass().toString());
		logger.debug(m.toString());
		m = conn.min.readMessage();
		logger.debug(m.getClass().toString());
		logger.debug(m.toString());
		m = conn.min.readMessage();
		logger.debug(m.toString());
		m = new MethodCall("org.freedesktop.DBus", "/", null, "Hello", (byte) 0,
				null);
		conn.mout.writeMessage(m);
		m = conn.min.readMessage();
		logger.debug(m.toString());

		m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus",
				"org.freedesktop.DBus", "RequestName", (byte) 0, "su",
				"org.testname", 0);
		conn.mout.writeMessage(m);
		m = conn.min.readMessage();
		logger.debug(m.toString());
		m = new DBusSignal(null, "/foo", "org.foo", "Foo", null);
		conn.mout.writeMessage(m);
		m = conn.min.readMessage();
		logger.debug(m.toString());
		conn.disconnect();
	}

}
