package org.freedesktop;

import java.util.List;

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

		String idDisks = "org.freedesktop.UDisks";
		String idDevices = "org.freedesktop.UDisks.Device";

		UDisks disks = conn.getRemoteObject(idDisks, "/org/freedesktop/UDisks",
				UDisks.class);

		Path[] paths = disks.EnumerateDevices();

		for (Path path : paths) {
			UDisks.Device disk = conn.getRemoteObject(idDisks, path.getPath(),
					UDisks.Device.class);

			DBus.Properties diskProps = conn.getRemoteObject(idDisks,
					path.getPath(), DBus.Properties.class);
			List<String> mountPaths = diskProps.Get(idDevices,
					"DeviceMountPaths");

			UInt64 size = diskProps.Get(idDevices, "DeviceSize");
			Boolean isPartitionTable = diskProps.Get(idDevices,
					"DeviceIsPartitionTable");
			Boolean isSystemInternal = diskProps.Get(idDevices,
					"DeviceIsSystemInternal");
			Boolean presentationHide = diskProps.Get(idDevices,
					"DevicePresentationHide");
			String partitionType = diskProps.Get(idDevices, "PartitionType");

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
