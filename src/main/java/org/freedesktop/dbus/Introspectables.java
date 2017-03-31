package org.freedesktop.dbus;

public class Introspectables
{

	public static final String DTD = "http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd";

	public static final String DOCTYPE = "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\"\n"
			+ "\"" + DTD + "\">";

	public static final String DOCTYPE_REGEX = "<!DOCTYPE node PUBLIC [\"']-//freedesktop//DTD D-BUS Object Introspection 1.0//EN[\"']\\s*"
			+ "[\"']" + DTD + "[\"']>";

}
