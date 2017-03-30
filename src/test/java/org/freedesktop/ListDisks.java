package org.freedesktop;

import java.util.Vector;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.exceptions.DBusException;

public class ListDisks
{

	public static void main(String[] args) throws DBusException
	{
		int connection = DBusConnection.SYSTEM;

		DBusConnection conn = DBusConnection.getConnection(connection);

		UDisks disks = conn.getRemoteObject("org.freedesktop.UDisks",
				"/org/freedesktop/UDisks", UDisks.class);

		Path[] paths = disks.EnumerateDevices();
		for (Path path : paths) {
			UDisks.Device disk = conn.getRemoteObject("org.freedesktop.UDisks",
					path.getPath(), UDisks.Device.class);

			DBus.Properties diskProps = conn.getRemoteObject(
					"org.freedesktop.UDisks", path.getPath(),
					DBus.Properties.class);
			Vector<String> mountPaths = diskProps
					.Get("org.freedesktop.UDisks.Device", "DeviceMountPaths");

			UInt64 size = diskProps.Get("org.freedesktop.UDisks.Device",
					"DeviceSize");
			Boolean isPartitionTable = diskProps.Get(
					"org.freedesktop.UDisks.Device", "DeviceIsPartitionTable");
			Boolean isSystemInternal = diskProps.Get(
					"org.freedesktop.UDisks.Device", "DeviceIsSystemInternal");
			Boolean presentationHide = diskProps.Get(
					"org.freedesktop.UDisks.Device", "DevicePresentationHide");
			String partitionType = diskProps
					.Get("org.freedesktop.UDisks.Device", "PartitionType");

			boolean skip = isPartitionTable || partitionType.equals("0x05");
			if (skip) {
				continue;
			}

			System.out.println(path);

			for (String mountPath : mountPaths) {
				System.out.println("mount path: " + mountPath);
			}

			System.out.println("isPartitionTable: " + isPartitionTable);
			System.out.println("isSystemInternal: " + isSystemInternal);
			System.out.println("PresentationHide: " + presentationHide);
			System.out.println("PartitionType: " + partitionType);
			System.out.println("Size: " + size(size));
		}

		conn.disconnect();
	}

	private static String size(UInt64 size)
	{
		long s = size.longValue();
		if (s > 1000000000) {
			long m = s / 1000000000;
			return Long.toString(m) + "G";
		} else if (s > 1000000) {
			long m = s / 1000000;
			return Long.toString(m) + "M";
		} else if (s > 1000) {
			long k = s / 1000;
			return Long.toString(k) + "K";
		}
		return Long.toString(s);
	}

}
