package org.freedesktop.dbus.bin;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.freedesktop.DBus;
import org.freedesktop.UDisks;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

import de.topobyte.utilities.apache.commons.cli.OptionHelper;

public class ListDisks
{

	private static final String OPTION_VERBOSE = "verbose";

	public static void main(String[] args) throws DBusException
	{
		Options options = new Options();
		OptionHelper.add(options, "v", OPTION_VERBOSE, false, false,
				"show more information");

		CommandLine line = null;
		try {
			line = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			System.out.println("Error while parsing command line");
			System.out.println(e.getMessage());
			new HelpFormatter().printHelp(ListDisks.class.getSimpleName(),
					options);
			System.exit(1);
		}

		boolean verbose = line.hasOption(OPTION_VERBOSE);

		int connection = DBusConnection.SYSTEM;

		DBusConnection conn = DBusConnection.getConnection(connection);

		String idDisks = "org.freedesktop.UDisks";
		String idDevices = "org.freedesktop.UDisks.Device";

		UDisks disks = conn.getRemoteObject(idDisks, "/org/freedesktop/UDisks",
				UDisks.class);

		Path[] paths = disks.EnumerateDevices();
		Arrays.sort(paths);

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

			if (verbose) {
				Map<String, Variant> all = diskProps.GetAll(idDevices);
				for (String key : all.keySet()) {
					System.out.println(key + " " + all.get(key));
				}
			}
		}

		conn.disconnect();
	}

	private static String size(UInt64 size)
	{
		long s = size.longValue();
		if (s > 1000000000000L) {
			double g = s / 1000000000000.;
			return size(g, "T");
		} else if (s > 1000000000) {
			double g = s / 1000000000.;
			return size(g, "G");
		} else if (s > 1000000) {
			double m = s / 1000000.;
			return size(m, "M");
		} else if (s > 1000) {
			double k = s / 1000.;
			return size(k, "K");
		}
		return Long.toString(s);
	}

	private static NumberFormat nf;
	static {
		nf = NumberFormat.getNumberInstance();
		DecimalFormat df = (DecimalFormat) nf;
		df.setMinimumFractionDigits(1);
		df.setMaximumFractionDigits(2);
		df.setGroupingUsed(false);
	}

	private static String size(double size, String unit)
	{
		return nf.format(size) + unit;
	}

}
