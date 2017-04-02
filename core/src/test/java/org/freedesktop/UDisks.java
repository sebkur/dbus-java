package org.freedesktop;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Path;

public interface UDisks extends DBusInterface
{

	public Path[] EnumerateDevices();

	public interface Device extends DBusInterface
	{

	}

}
