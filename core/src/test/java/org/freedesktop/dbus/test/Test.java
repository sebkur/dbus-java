/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.freedesktop.DBus;
import org.freedesktop.DBus.Error.MatchRuleInvalid;
import org.freedesktop.DBus.Error.ServiceUnknown;
import org.freedesktop.DBus.Error.UnknownObject;
import org.freedesktop.DBus.Introspectable;
import org.freedesktop.DBus.Peer;
import org.freedesktop.DBus.Properties;
import org.freedesktop.dbus.CallbackHandler;
import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.NotConnected;

class testnewclass implements TestNewInterface
{
	@Override
	public boolean isRemote()
	{
		return false;
	}

	@Override
	public String getName()
	{
		return toString();
	}
}

class testclass implements TestRemoteInterface, TestRemoteInterface2,
		TestSignalInterface, TestSignalInterface2, Properties
{
	private DBusConnection conn;

	public testclass(DBusConnection conn)
	{
		this.conn = conn;
	}

	@Override
	public String Introspect()
	{
		return "Not XML";
	}

	@Override
	public int[][] teststructstruct(TestStruct3 in)
	{
		List<List<Integer>> lli = in.b;
		int[][] out = new int[lli.size()][];
		for (int j = 0; j < out.length; j++) {
			out[j] = new int[lli.get(j).size()];
			for (int k = 0; k < out[j].length; k++) {
				out[j][k] = lli.get(j).get(k);
			}
		}
		return out;
	}

	@Override
	public float testfloat(float[] f)
	{
		if (f.length < 4 || f[0] != 17.093f || f[1] != -23f || f[2] != 0.0f
				|| f[3] != 31.42f) {
			Test.fail("testfloat got incorrect array");
		}
		return f[0];
	}

	@Override
	public void newpathtest(Path p)
	{
		if (!p.toString().equals("/new/path/test")) {
			Test.fail("new path test got wrong path");
		}
	}

	@Override
	public void waitawhile()
	{
		System.out.println("Sleeping.");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException Ie) {
		}
		System.out.println("Done sleeping.");
	}

	@Override
	public <A> TestTuple<String, List<Integer>, Boolean> show(A in)
	{
		System.out.println("Showing Stuff: " + in.getClass() + "(" + in + ")");
		if (!(in instanceof Integer) || ((Integer) in).intValue() != 234) {
			Test.fail("show received the wrong arguments");
		}
		DBusCallInfo info = DBusConnection.getCallInfo();
		List<Integer> l = new ArrayList<Integer>();
		l.add(1953);
		return new TestTuple<String, List<Integer>, Boolean>(info.getSource(),
				l, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T dostuff(TestStruct foo)
	{
		System.out.println("Doing Stuff " + foo);
		System.out.println(" -- (" + foo.a.getClass() + ", " + foo.b.getClass()
				+ ", " + foo.c.getClass() + ")");
		if (!(foo instanceof TestStruct) || !(foo.a instanceof String)
				|| !(foo.b instanceof UInt32) || !(foo.c instanceof Variant)
				|| !"bar".equals(foo.a) || foo.b.intValue() != 52
				|| !(foo.c.getValue() instanceof Boolean)
				|| ((Boolean) foo.c.getValue()).booleanValue() != true) {
			Test.fail("dostuff received the wrong arguments");
		}
		return (T) foo.c.getValue();
	}

	/** Local classes MUST implement this to return false */
	@Override
	public boolean isRemote()
	{
		return false;
	}

	/** The method we are exporting to the Bus. */
	@Override
	public List<Integer> sampleArray(List<String> ss, Integer[] is, long[] ls)
	{
		System.out.println("Got an array:");
		for (String s : ss) {
			System.out.println("--" + s);
		}
		if (ss.size() != 5 || !"hi".equals(ss.get(0))
				|| !"hello".equals(ss.get(1)) || !"hej".equals(ss.get(2))
				|| !"hey".equals(ss.get(3)) || !"aloha".equals(ss.get(4))) {
			Test.fail("sampleArray, String array contents incorrect");
		}
		System.out.println("Got an array:");
		for (Integer i : is) {
			System.out.println("--" + i);
		}
		if (is.length != 4 || is[0].intValue() != 1 || is[1].intValue() != 5
				|| is[2].intValue() != 7 || is[3].intValue() != 9) {
			Test.fail("sampleArray, Integer array contents incorrect");
		}
		System.out.println("Got an array:");
		for (long l : ls) {
			System.out.println("--" + l);
		}
		if (ls.length != 4 || ls[0] != 2 || ls[1] != 6 || ls[2] != 8
				|| ls[3] != 12) {
			Test.fail("sampleArray, Integer array contents incorrect");
		}
		List<Integer> v = new ArrayList<Integer>();
		v.add(-1);
		v.add(-5);
		v.add(-7);
		v.add(-12);
		v.add(-18);
		return v;
	}

	@Override
	public String getName()
	{
		return "This Is A UTF-8 Name: س !!";
	}

	@Override
	public String getNameAndThrow() throws TestException
	{
		throw new TestException("test");
	}

	@Override
	public boolean check()
	{
		System.out.println("Being checked");
		return false;
	}

	@Override
	public <T> int frobnicate(List<Long> n, Map<String, Map<UInt16, Short>> m,
			T v)
	{
		if (null == n) {
			Test.fail("List was null");
		}
		if (n.size() != 3) {
			Test.fail("List was wrong size (expected 3, actual " + n.size()
					+ ")");
		}
		if (n.get(0) != 2L || n.get(1) != 5L || n.get(2) != 71L) {
			Test.fail("List has wrong contents");
		}
		if (!(v instanceof Integer)) {
			Test.fail("v not an Integer");
		}
		if (((Integer) v) != 13) {
			Test.fail("v is incorrect");
		}
		if (null == m) {
			Test.fail("Map was null");
		}
		if (m.size() != 1) {
			Test.fail("Map was wrong size");
		}
		if (!m.keySet().contains("stuff")) {
			Test.fail("Incorrect key");
		}
		Map<UInt16, Short> mus = m.get("stuff");
		if (null == mus) {
			Test.fail("Sub-Map was null");
		}
		if (mus.size() != 3) {
			Test.fail("Sub-Map was wrong size");
		}
		if (!(new Short((short) 5).equals(mus.get(new UInt16(4))))) {
			Test.fail("Sub-Map has wrong contents");
		}
		if (!(new Short((short) 6).equals(mus.get(new UInt16(5))))) {
			Test.fail("Sub-Map has wrong contents");
		}
		if (!(new Short((short) 7).equals(mus.get(new UInt16(6))))) {
			Test.fail("Sub-Map has wrong contents");
		}
		return -5;
	}

	@Override
	public DBusInterface getThis(DBusInterface t)
	{
		if (!t.equals(this)) {
			Test.fail("Didn't get this properly");
		}
		return this;
	}

	@Override
	public void throwme() throws TestException
	{
		throw new TestException("test");
	}

	@Override
	public TestSerializable<String> testSerializable(byte b,
			TestSerializable<String> s, int i)
	{
		System.out.println("Recieving TestSerializable: " + s);
		if (b != 12 || i != 13 || !(s.getInt() == 1)
				|| !(s.getString().equals("woo")) || !(s.getList().size() == 3)
				|| !(s.getList().get(0) == 1) || !(s.getList().get(1) == 2)
				|| !(s.getList().get(2) == 3)) {
			Test.fail("Error in recieving custom synchronisation");
		}
		return s;
	}

	@Override
	public String recursionTest()
	{
		try {
			TestRemoteInterface tri = conn.getRemoteObject("foo.bar.Test",
					"/Test", TestRemoteInterface.class);
			return tri.getName();
		} catch (DBusException DBe) {
			Test.fail("Failed with error: " + DBe);
			return "";
		}
	}

	@Override
	public int overload(String s)
	{
		return 1;
	}

	@Override
	public int overload(byte b)
	{
		return 2;
	}

	@Override
	public int overload()
	{
		DBusCallInfo info = DBusConnection.getCallInfo();
		if ("org.freedesktop.dbus.test.AlternateTestInterface"
				.equals(info.getInterface())) {
			return 3;
		} else if ("org.freedesktop.dbus.test.TestRemoteInterface"
				.equals(info.getInterface())) {
			return 4;
		} else {
			return -1;
		}
	}

	@Override
	public List<List<Integer>> checklist(List<List<Integer>> lli)
	{
		return lli;
	}

	@Override
	public TestNewInterface getNew()
	{
		testnewclass n = new testnewclass();
		try {
			conn.exportObject("/new", n);
		} catch (DBusException DBe) {
			throw new DBusExecutionException(DBe.getMessage());
		}
		return n;
	}

	@Override
	public void sig(Type[] s)
	{
		if (s.length != 2 || !s[0].equals(Byte.class)
				|| !(s[1] instanceof ParameterizedType)
				|| !Map.class.equals(((ParameterizedType) s[1]).getRawType())
				|| ((ParameterizedType) s[1])
						.getActualTypeArguments().length != 2
				|| !String.class.equals(
						((ParameterizedType) s[1]).getActualTypeArguments()[0])
				|| !Integer.class.equals(((ParameterizedType) s[1])
						.getActualTypeArguments()[1])) {
			Test.fail("Didn't send types correctly");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void complexv(Variant<? extends Object> v)
	{
		if (!"a{ss}".equals(v.getSig()) || !(v.getValue() instanceof Map)
				|| ((Map<Object, Object>) v.getValue()).size() != 1
				|| !"moo".equals(
						((Map<Object, Object>) v.getValue()).get("cow"))) {
			Test.fail("Didn't send variant correctly");
		}
	}

	@Override
	public void reg13291(byte[] as, byte[] bs)
	{
		if (as.length != bs.length) {
			Test.fail("didn't receive identical byte arrays");
		}
		for (int i = 0; i < as.length; i++) {
			if (as[i] != bs[i]) {
				Test.fail("didn't receive identical byte arrays");
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> A Get(String interface_name, String property_name)
	{
		return (A) new Path("/nonexistant/path");
	}

	@Override
	public <A> void Set(String interface_name, String property_name, A value)
	{
	}

	@Override
	public Map<String, Variant> GetAll(String interface_name)
	{
		return new HashMap<String, Variant>();
	}

	@Override
	public Path pathrv(Path a)
	{
		return a;
	}

	@Override
	public List<Path> pathlistrv(List<Path> a)
	{
		return a;
	}

	@Override
	public Map<Path, Path> pathmaprv(Map<Path, Path> a)
	{
		return a;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Variant> svm()
	{
		HashMap<String, Variant> properties = new HashMap<String, Variant>();
		HashMap<String, Variant<String>> parameters = new HashMap<String, Variant<String>>();

		parameters.put("Name", new Variant<String>("Joe"));
		parameters.put("Password", new Variant<String>("abcdef"));

		properties.put("Parameters", new Variant(parameters, "a{sv}"));
		return properties;
	}
}

/**
 * Typed signal handler for renamed signal
 */
class renamedsignalhandler
		implements DBusSigHandler<TestSignalInterface2.TestRenamedSignal>
{
	/** Handling a signal */
	@Override
	public void handle(TestSignalInterface2.TestRenamedSignal t)
	{
		if (false == Test.done5) {
			Test.done5 = true;
		} else {
			Test.fail("SignalHandler R has been run too many times");
		}
		System.out.println("SignalHandler R Running");
		System.out.println("string(" + t.value + ") int(" + t.number + ")");
		if (!"Bar".equals(t.value) || !(new UInt32(42)).equals(t.number)) {
			Test.fail("Incorrect TestRenamedSignal parameters");
		}
	}
}

/**
 * Empty signal handler
 */
class emptysignalhandler
		implements DBusSigHandler<TestSignalInterface.EmptySignal>
{
	/** Handling a signal */
	@Override
	public void handle(TestSignalInterface.EmptySignal t)
	{
		if (false == Test.done7) {
			Test.done7 = true;
		} else {
			Test.fail("SignalHandler E has been run too many times");
		}
		System.out.println("SignalHandler E Running");
	}
}

/**
 * Disconnect handler
 */
class disconnecthandler implements DBusSigHandler<DBus.Local.Disconnected>
{
	private DBusConnection conn;
	private renamedsignalhandler sh;

	public disconnecthandler(DBusConnection conn, renamedsignalhandler sh)
	{
		this.conn = conn;
		this.sh = sh;
	}

	/** Handling a signal */
	@Override
	public void handle(DBus.Local.Disconnected t)
	{
		if (false == Test.done6) {
			Test.done6 = true;
			System.out.println("Handling disconnect, unregistering handler");
			try {
				conn.removeSigHandler(
						TestSignalInterface2.TestRenamedSignal.class, sh);
			} catch (DBusException DBe) {
				DBe.printStackTrace();
				Test.fail("Disconnect handler threw an exception: " + DBe);
			}
		}
	}
}

/**
 * Typed signal handler
 */
class pathsignalhandler
		implements DBusSigHandler<TestSignalInterface.TestPathSignal>
{
	/** Handling a signal */
	@Override
	public void handle(TestSignalInterface.TestPathSignal t)
	{
		System.out.println("Path sighandler: " + t);
	}
}

/**
 * Typed signal handler
 */
class signalhandler implements DBusSigHandler<TestSignalInterface.TestSignal>
{
	/** Handling a signal */
	@Override
	public void handle(TestSignalInterface.TestSignal t)
	{
		if (false == Test.done1) {
			Test.done1 = true;
		} else {
			Test.fail("SignalHandler 1 has been run too many times");
		}
		System.out.println("SignalHandler 1 Running");
		System.out.println("string(" + t.value + ") int(" + t.number + ")");
		if (!"Bar".equals(t.value) || !(new UInt32(42)).equals(t.number)) {
			Test.fail("Incorrect TestSignal parameters");
		}
	}
}

/**
 * Untyped signal handler
 */
class arraysignalhandler
		implements DBusSigHandler<TestSignalInterface.TestArraySignal>
{
	/** Handling a signal */
	@Override
	public void handle(TestSignalInterface.TestArraySignal t)
	{
		try {
			if (false == Test.done2) {
				Test.done2 = true;
			} else {
				Test.fail("SignalHandler 2 has been run too many times");
			}
			System.out.println("SignalHandler 2 Running");
			if (t.v.size() != 1) {
				Test.fail(
						"Incorrect TestArraySignal array length: should be 1, actually "
								+ t.v.size());
			}
			System.out.println("Got a test array signal with Parameters: ");
			for (String str : t.v.get(0).a) {
				System.out.println("--" + str);
			}
			System.out.println(t.v.get(0).b.getType());
			System.out.println(t.v.get(0).b.getValue());
			if (!(t.v.get(0).b.getValue() instanceof UInt64)
					|| 567L != ((UInt64) t.v.get(0).b.getValue()).longValue()
					|| t.v.get(0).a.size() != 5
					|| !"hi".equals(t.v.get(0).a.get(0))
					|| !"hello".equals(t.v.get(0).a.get(1))
					|| !"hej".equals(t.v.get(0).a.get(2))
					|| !"hey".equals(t.v.get(0).a.get(3))
					|| !"aloha".equals(t.v.get(0).a.get(4))) {
				Test.fail("Incorrect TestArraySignal parameters");
			}

			if (t.m.keySet().size() != 2) {
				Test.fail(
						"Incorrect TestArraySignal map size: should be 2, actually "
								+ t.m.keySet().size());
			}
			if (!(t.m.get(new UInt32(1)).b.getValue() instanceof UInt64)
					|| 678L != ((UInt64) t.m.get(new UInt32(1)).b.getValue())
							.longValue()
					|| !(t.m.get(new UInt32(42)).b.getValue() instanceof UInt64)
					|| 789L != ((UInt64) t.m.get(new UInt32(42)).b.getValue())
							.longValue()) {
				Test.fail("Incorrect TestArraySignal parameters");
			}

		} catch (Exception e) {
			e.printStackTrace();
			Test.fail("SignalHandler 2 threw an exception: " + e);
		}
	}
}

/**
 * Object path signal handler
 */
class objectsignalhandler
		implements DBusSigHandler<TestSignalInterface.TestObjectSignal>
{
	@Override
	public void handle(TestSignalInterface.TestObjectSignal s)
	{
		if (false == Test.done3) {
			Test.done3 = true;
		} else {
			Test.fail("SignalHandler 3 has been run too many times");
		}
		System.out.println(s.otherpath);
	}
}

/**
 * handler which should never be called
 */
class badarraysignalhandler<T extends DBusSignal> implements DBusSigHandler<T>
{
	/** Handling a signal */
	@Override
	public void handle(T s)
	{
		Test.fail("This signal handler shouldn't be called");
	}
}

/**
 * Callback handler
 */
class callbackhandler implements CallbackHandler<String>
{
	@Override
	public void handle(String r)
	{
		System.out.println("Handling callback: " + r);
		Collator col = Collator.getInstance();
		col.setDecomposition(Collator.FULL_DECOMPOSITION);
		col.setStrength(Collator.PRIMARY);
		if (0 != col.compare("This Is A UTF-8 Name: ﺱ !!", r)) {
			Test.fail("call with callback, wrong return value");
		}
		if (Test.done4) {
			Test.fail("Already ran callback handler");
		}
		Test.done4 = true;
	}

	@Override
	public void handleError(DBusExecutionException e)
	{
		System.out.println("Handling error callback: " + e + " message = '"
				+ e.getMessage() + "'");
		if (!(e instanceof TestException)) {
			Test.fail("Exception is of the wrong sort");
		}
		Collator col = Collator.getInstance();
		col.setDecomposition(Collator.FULL_DECOMPOSITION);
		col.setStrength(Collator.PRIMARY);
		if (0 != col.compare("test", e.getMessage())) {
			Test.fail("Exception has the wrong message");
		}
		if (Test.done8) {
			Test.fail("Already ran callback error handler");
		}
		Test.done8 = true;
	}
}

/**
 * This is a test program which sends and recieves a signal, implements, exports
 * and calls a remote method.
 */
public class Test
{
	public static boolean done1 = false;
	public static boolean done2 = false;
	public static boolean done3 = false;
	public static boolean done4 = false;
	public static boolean done5 = false;
	public static boolean done6 = false;
	public static boolean done7 = false;
	public static boolean done8 = false;

	public static void fail(String message)
	{
		System.out.println("Test Failed: " + message);
		System.err.println("Test Failed: " + message);
		if (null != serverconn) {
			serverconn.disconnect();
		}
		if (null != clientconn) {
			clientconn.disconnect();
		}
		System.exit(1);
	}

	static DBusConnection serverconn = null;
	static DBusConnection clientconn = null;

	@SuppressWarnings("unchecked")
	public static void main(String[] args)
	{
		try {
			System.out.println("Creating Connection");
			serverconn = DBusConnection.getConnection(DBusConnection.SESSION);
			clientconn = DBusConnection.getConnection(DBusConnection.SESSION);
			serverconn.setWeakReferences(true);
			clientconn.setWeakReferences(true);

			System.out.println("Registering Name");
			serverconn.requestBusName("foo.bar.Test");

			/**
			 * This gets a remote object matching our bus name and exported
			 * object path.
			 */
			Peer peer = clientconn.getRemoteObject("foo.bar.Test", "/Test",
					Peer.class);
			DBus dbus = clientconn.getRemoteObject("org.freedesktop.DBus",
					"/org/freedesktop/DBus", DBus.class);

			System.out.print("Listening for signals...");
			signalhandler sigh = new signalhandler();
			renamedsignalhandler rsh = new renamedsignalhandler();
			try {
				/**
				 * This registers an instance of the test class as the signal
				 * handler for the TestSignal class.
				 */
				clientconn.addSigHandler(TestSignalInterface.EmptySignal.class,
						new emptysignalhandler());
				clientconn.addSigHandler(TestSignalInterface.TestSignal.class,
						sigh);
				clientconn.addSigHandler(
						TestSignalInterface2.TestRenamedSignal.class, rsh);
				clientconn.addSigHandler(DBus.Local.Disconnected.class,
						new disconnecthandler(clientconn, rsh));
				String source = dbus.GetNameOwner("foo.bar.Test");
				clientconn.addSigHandler(
						TestSignalInterface.TestArraySignal.class, source, peer,
						new arraysignalhandler());
				clientconn.addSigHandler(
						TestSignalInterface.TestObjectSignal.class,
						new objectsignalhandler());
				clientconn.addSigHandler(
						TestSignalInterface.TestPathSignal.class,
						new pathsignalhandler());
				badarraysignalhandler<TestSignalInterface.TestSignal> bash = new badarraysignalhandler<TestSignalInterface.TestSignal>();
				clientconn.addSigHandler(TestSignalInterface.TestSignal.class,
						bash);
				clientconn.removeSigHandler(
						TestSignalInterface.TestSignal.class, bash);
				System.out.println("done");
			} catch (MatchRuleInvalid MRI) {
				Test.fail("Failed to add handlers: " + MRI.getMessage());
			} catch (DBusException DBe) {
				Test.fail("Failed to add handlers: " + DBe.getMessage());
			}

			System.out.println("Listening for Method Calls");
			testclass tclass = new testclass(serverconn);
			testclass tclass2 = new testclass(serverconn);
			/**
			 * This exports an instance of the test class as the object /Test.
			 */
			serverconn.exportObject("/Test", tclass);
			serverconn.exportObject("/BadTest", tclass);
			serverconn.exportObject("/BadTest2", tclass2);
			serverconn.addFallback("/FallbackTest", tclass);

			// explicitly unexport object
			serverconn.unExportObject("/BadTest");
			// implicitly unexport object
			tclass2 = null;
			System.gc();
			System.runFinalization();
			System.gc();
			System.runFinalization();
			System.gc();
			System.runFinalization();

			System.out.println("Sending Signal");
			/**
			 * This creates an instance of the Test Signal, with the given
			 * object path, signal name and parameters, and broadcasts in on the
			 * Bus.
			 */
			serverconn.sendSignal(new TestSignalInterface.TestSignal(
					"/foo/bar/Wibble", "Bar", new UInt32(42)));
			serverconn.sendSignal(
					new TestSignalInterface.EmptySignal("/foo/bar/Wibble"));
			serverconn.sendSignal(new TestSignalInterface2.TestRenamedSignal(
					"/foo/bar/Wibble", "Bar", new UInt32(42)));

			System.out.println("These things are on the bus:");
			String[] names = dbus.ListNames();
			for (String name : names) {
				System.out.println("\t" + name);
			}

			System.out.println("Getting our introspection data");
			/**
			 * This gets a remote object matching our bus name and exported
			 * object path.
			 */
			Introspectable intro = clientconn.getRemoteObject("foo.bar.Test",
					"/", Introspectable.class);
			/** Get introspection data */
			String data;/*
						 * = intro.Introspect(); if (null == data ||
						 * !data.startsWith("<!DOCTYPE"))
						 * fail("Introspection data invalid");
						 * System.out.println("Got Introspection Data: \n"+data)
						 * ;
						 */
			intro = clientconn.getRemoteObject("foo.bar.Test", "/Test",
					Introspectable.class);
			/** Get introspection data */
			data = intro.Introspect();
			if (null == data || !data.startsWith("<!DOCTYPE")) {
				fail("Introspection data invalid");
			}
			System.out.println("Got Introspection Data: \n" + data);

			// setup bus name set
			Set<String> peers = serverconn.new PeerSet();
			peers.add("org.freedesktop.DBus");
			clientconn.requestBusName("test.testclient");
			peers.add("test.testclient");
			clientconn.releaseBusName("test.testclient");

			System.out.println("Pinging ourselves");
			/** Call ping. */
			for (int i = 0; i < 10; i++) {
				long then = System.currentTimeMillis();
				peer.Ping();
				long now = System.currentTimeMillis();
				System.out.println("Ping returned in " + (now - then) + "ms.");
			}

			System.out.println("Calling Method0/1");
			/**
			 * This gets a remote object matching our bus name and exported
			 * object path.
			 */
			TestRemoteInterface tri = (TestRemoteInterface) clientconn
					.getPeerRemoteObject("foo.bar.Test", "/Test");
			System.out.println("Got Remote Object: " + tri);
			/** Call the remote object and get a response. */
			String rname = tri.getName();
			System.out.println("Got Remote Name: " + rname);

			Map<String, Variant> svmmap = tri.svm();
			System.out.println(svmmap.toString());
			if (!"{ Parameters => [{ Name => [Joe],Password => [abcdef] }] }"
					.equals(svmmap.toString())) {
				fail("incorrect reply from svm");
			}

			Path path = new Path("/nonexistantwooooooo");
			Path p = tri.pathrv(path);
			System.out.println(path.toString() + " => " + p.toString());
			if (!path.equals(p)) {
				fail("pathrv incorrect");
			}
			List<Path> paths = new ArrayList<Path>();
			paths.add(path);
			List<Path> ps = tri.pathlistrv(paths);
			System.out.println(paths.toString() + " => " + ps.toString());
			if (!paths.equals(ps)) {
				fail("pathlistrv incorrect");
			}
			Map<Path, Path> pathm = new HashMap<Path, Path>();
			pathm.put(path, path);
			Map<Path, Path> pm = tri.pathmaprv(pathm);
			System.out.println(pathm.toString() + " => " + pm.toString());
			System.out.println(pm.containsKey(path) + " " + pm.get(path) + " "
					+ path.equals(pm.get(path)));
			System.out.println(pm.containsKey(p) + " " + pm.get(p) + " "
					+ p.equals(pm.get(p)));
			for (Path q : pm.keySet()) {
				System.out.println(q);
				System.out.println(pm.get(q));
			}
			if (!pm.containsKey(path) || !path.equals(pm.get(path))) {
				fail("pathmaprv incorrect");
			}

			serverconn.sendSignal(new TestSignalInterface.TestPathSignal(
					"/Test", path, paths, pathm));

			Collator col = Collator.getInstance();
			col.setDecomposition(Collator.FULL_DECOMPOSITION);
			col.setStrength(Collator.PRIMARY);
			if (0 != col.compare("This Is A UTF-8 Name: ﺱ !!", rname)) {
				fail("getName return value incorrect");
			}
			System.out.println("sending it to sleep");
			tri.waitawhile();
			System.out.println("testing floats");
			if (17.093f != tri
					.testfloat(new float[] { 17.093f, -23f, 0.0f, 31.42f })) {
				fail("testfloat returned the wrong thing");
			}
			System.out.println("Structs of Structs");
			List<List<Integer>> lli = new ArrayList<List<Integer>>();
			List<Integer> li = new ArrayList<Integer>();
			li.add(1);
			li.add(2);
			li.add(3);
			lli.add(li);
			lli.add(li);
			lli.add(li);
			TestStruct3 ts3 = new TestStruct3(new TestStruct2(
					new ArrayList<String>(), new Variant<Integer>(0)), lli);
			int[][] out = tri.teststructstruct(ts3);
			if (out.length != 3) {
				fail("teststructstruct returned the wrong thing: "
						+ Arrays.deepToString(out));
			}
			for (int[] o : out) {
				if (o.length != 3 || o[0] != 1 || o[1] != 2 || o[2] != 3) {
					fail("teststructstruct returned the wrong thing: "
							+ Arrays.deepToString(out));
				}
			}

			System.out.println("frobnicating");
			List<Long> ls = new ArrayList<Long>();
			ls.add(2L);
			ls.add(5L);
			ls.add(71L);
			Map<UInt16, Short> mus = new HashMap<UInt16, Short>();
			mus.put(new UInt16(4), (short) 5);
			mus.put(new UInt16(5), (short) 6);
			mus.put(new UInt16(6), (short) 7);
			Map<String, Map<UInt16, Short>> msmus = new HashMap<String, Map<UInt16, Short>>();
			msmus.put("stuff", mus);
			int rint = tri.frobnicate(ls, msmus, 13);
			if (-5 != rint) {
				fail("frobnicate return value incorrect");
			}

			System.out.println("Doing stuff asynchronously with callback");
			clientconn.callWithCallback(tri, "getName", new callbackhandler());
			System.out.println(
					"Doing stuff asynchronously with callback, which throws an error");
			clientconn.callWithCallback(tri, "getNameAndThrow",
					new callbackhandler());

			/** call something that throws */
			try {
				System.out.println("Throwing stuff");
				tri.throwme();
				Test.fail("Method Execution should have failed");
			} catch (TestException Te) {
				System.out.println("Remote Method Failed with: "
						+ Te.getClass().getName() + " " + Te.getMessage());
				if (!Te.getMessage().equals("test")) {
					Test.fail("Error message was not correct");
				}
			}

			/* Test type signatures */
			List<Type> ts = new ArrayList<Type>();
			Marshalling.getJavaType("ya{si}", ts, -1);
			tri.sig(ts.toArray(new Type[0]));

			tri.newpathtest(new Path("/new/path/test"));

			/** Try and call an invalid remote object */
			try {
				System.out.println("Calling Method2");
				tri = clientconn.getRemoteObject("foo.bar.NotATest", "/Moofle",
						TestRemoteInterface.class);
				System.out.println("Got Remote Name: " + tri.getName());
				Test.fail("Method Execution should have failed");
			} catch (ServiceUnknown SU) {
				System.out.println("Remote Method Failed with: "
						+ SU.getClass().getName() + " " + SU.getMessage());
			}

			/** Try and call an invalid remote object */
			try {
				System.out.println("Calling Method3");
				tri = clientconn.getRemoteObject("foo.bar.Test", "/Moofle",
						TestRemoteInterface.class);
				System.out.println("Got Remote Name: " + tri.getName());
				Test.fail("Method Execution should have failed");
			} catch (UnknownObject UO) {
				System.out.println("Remote Method Failed with: "
						+ UO.getClass().getName() + " " + UO.getMessage());
			}

			/** Try and call an explicitly unexported object */
			try {
				System.out.println("Calling Method4");
				tri = clientconn.getRemoteObject("foo.bar.Test", "/BadTest",
						TestRemoteInterface.class);
				System.out.println("Got Remote Name: " + tri.getName());
				Test.fail("Method Execution should have failed");
			} catch (UnknownObject UO) {
				System.out.println("Remote Method Failed with: "
						+ UO.getClass().getName() + " " + UO.getMessage());
			}

			/** Try and call an implicitly unexported object */
			try {
				System.out.println("Calling Method5");
				tri = clientconn.getRemoteObject("foo.bar.Test", "/BadTest2",
						TestRemoteInterface.class);
				System.out.println("Got Remote Name: " + tri.getName());
				Test.fail("Method Execution should have failed");
			} catch (UnknownObject UO) {
				System.out.println("Remote Method Failed with: "
						+ UO.getClass().getName() + " " + UO.getMessage());
			}

			System.out.println("Calling Method6");
			tri = clientconn.getRemoteObject("foo.bar.Test",
					"/FallbackTest/0/1", TestRemoteInterface.class);
			intro = clientconn.getRemoteObject("foo.bar.Test",
					"/FallbackTest/0/4", Introspectable.class);
			System.out.println("Got Fallback Name: " + tri.getName());
			System.out.println(
					"Fallback Introspection Data: \n" + intro.Introspect());

			System.out.println("Testing Properties returning Paths");
			Properties prop = clientconn.getRemoteObject("foo.bar.Test",
					"/Test", Properties.class);
			Path prv = (Path) prop.Get("foo.bar", "foo");
			System.out.println("Got path " + prv);
			System.out.println("Calling Method7--9");
			/**
			 * This gets a remote object matching our bus name and exported
			 * object path.
			 */
			TestRemoteInterface2 tri2 = clientconn.getRemoteObject(
					"foo.bar.Test", "/Test", TestRemoteInterface2.class);
			System.out.print("Calling the other introspect method: ");
			String intro2 = tri2.Introspect();
			System.out.println(intro2);
			if (0 != col.compare("Not XML", intro2)) {
				fail("Introspect return value incorrect");
			}

			/** Call the remote object and get a response. */
			TestTuple<String, List<Integer>, Boolean> rv = tri2.show(234);
			System.out.println("Show returned: " + rv);
			if (!serverconn.getUniqueName().equals(rv.a) || 1 != rv.b.size()
					|| 1953 != rv.b.get(0) || true != rv.c.booleanValue()) {
				fail("show return value incorrect (" + rv.a + "," + rv.b + ","
						+ rv.c + ")");
			}

			System.out.println("Doing stuff asynchronously");
			DBusAsyncReply<Boolean> stuffreply = clientconn.callMethodAsync(
					tri2, "dostuff", new TestStruct("bar", new UInt32(52),
							new Variant<Boolean>(new Boolean(true))));

			System.out.println("Checking bools");
			if (tri2.check()) {
				fail("bools are broken");
			}

			List<String> l = new ArrayList<String>();
			l.add("hi");
			l.add("hello");
			l.add("hej");
			l.add("hey");
			l.add("aloha");
			System.out.println("Sampling Arrays:");
			List<Integer> is = tri2.sampleArray(l, new Integer[] { 1, 5, 7, 9 },
					new long[] { 2, 6, 8, 12 });
			System.out.println("sampleArray returned an array:");
			for (Integer i : is) {
				System.out.println("--" + i);
			}
			if (is.size() != 5 || is.get(0).intValue() != -1
					|| is.get(1).intValue() != -5 || is.get(2).intValue() != -7
					|| is.get(3).intValue() != -12
					|| is.get(4).intValue() != -18) {
				fail("sampleArray return value incorrect");
			}

			System.out.println("Get This");
			if (!tclass.equals(tri2.getThis(tri2))) {
				fail("Didn't get the correct this");
			}

			Boolean b = stuffreply.getReply();
			System.out.println("Do stuff replied " + b);
			if (true != b.booleanValue()) {
				fail("dostuff return value incorrect");
			}

			System.out.print("Sending Array Signal...");
			/**
			 * This creates an instance of the Test Signal, with the given
			 * object path, signal name and parameters, and broadcasts in on the
			 * Bus.
			 */
			List<TestStruct2> tsl = new ArrayList<TestStruct2>();
			tsl.add(new TestStruct2(l, new Variant<UInt64>(new UInt64(567))));
			Map<UInt32, TestStruct2> tsm = new HashMap<UInt32, TestStruct2>();
			tsm.put(new UInt32(1),
					new TestStruct2(l, new Variant<UInt64>(new UInt64(678))));
			tsm.put(new UInt32(42),
					new TestStruct2(l, new Variant<UInt64>(new UInt64(789))));
			serverconn.sendSignal(
					new TestSignalInterface.TestArraySignal("/Test", tsl, tsm));

			System.out.println("done");

			System.out.print("testing custom serialization...");
			List<Integer> v = new ArrayList<Integer>();
			v.add(1);
			v.add(2);
			v.add(3);
			TestSerializable<String> s = new TestSerializable<String>(1, "woo",
					v);
			s = tri2.testSerializable((byte) 12, s, 13);
			System.out.print("returned: " + s);
			if (s.getInt() != 1 || !s.getString().equals("woo")
					|| s.getList().size() != 3 || s.getList().get(0) != 1
					|| s.getList().get(1) != 2 || s.getList().get(2) != 3) {
				fail("Didn't get back the same TestSerializable");
			}

			System.out.println("done");

			System.out.print("testing complex variants...");
			Map m = new HashMap();
			m.put("cow", "moo");
			tri2.complexv(new Variant(m, "a{ss}"));
			System.out.println("done");

			System.out.print("testing recursion...");

			if (0 != col.compare("This Is A UTF-8 Name: ﺱ !!",
					tri2.recursionTest())) {
				fail("recursion test failed");
			}

			System.out.println("done");

			System.out.print("testing method overloading...");
			tri = clientconn.getRemoteObject("foo.bar.Test", "/Test",
					TestRemoteInterface.class);
			if (1 != tri2.overload("foo")) {
				Test.fail("wrong overloaded method called");
			}
			if (2 != tri2.overload((byte) 0)) {
				Test.fail("wrong overloaded method called");
			}
			if (3 != tri2.overload()) {
				Test.fail("wrong overloaded method called");
			}
			if (4 != tri.overload()) {
				Test.fail("wrong overloaded method called");
			}
			System.out.println("done");

			System.out.print("reg13291...");
			byte[] as = new byte[10];
			for (int i = 0; i < 10; i++) {
				as[i] = (byte) (100 - i);
			}
			tri.reg13291(as, as);
			System.out.println("done");

			System.out.print("Testing nested lists...");
			lli = new ArrayList<List<Integer>>();
			li = new ArrayList<Integer>();
			li.add(1);
			lli.add(li);
			List<List<Integer>> reti = tri2.checklist(lli);
			if (reti.size() != 1 || reti.get(0).size() != 1
					|| reti.get(0).get(0) != 1) {
				Test.fail("Failed to check nested lists");
			}
			System.out.println("done");

			System.out.print("Testing dynamic object creation...");
			TestNewInterface tni = tri2.getNew();
			System.out.print(tni.getName() + " ");
			System.out.println("done");

			/* send an object in a signal */
			serverconn.sendSignal(new TestSignalInterface.TestObjectSignal(
					"/foo/bar/Wibble", tclass));

			/**
			 * Pause while we wait for the DBus messages to go back and forth.
			 */
			Thread.sleep(1000);

			// check that bus name set has been trimmed
			if (peers.size() != 1) {
				fail("peers hasn't been trimmed");
			}
			if (!peers.contains("org.freedesktop.DBus")) {
				fail("peers contains the wrong name");
			}

			System.out.println("Checking for outstanding errors");
			DBusExecutionException DBEe = serverconn.getError();
			if (null != DBEe) {
				throw DBEe;
			}
			DBEe = clientconn.getError();
			if (null != DBEe) {
				throw DBEe;
			}

			System.out.println("Disconnecting");
			/** Disconnect from the bus. */
			clientconn.disconnect();
			serverconn.disconnect();

			System.out.println("Trying to do things after disconnection");

			/** Remove sig handler */
			clientconn.removeSigHandler(TestSignalInterface.TestSignal.class,
					sigh);

			/** Call a method when disconnected */
			try {
				System.out.println(
						"getName() suceeded and returned: " + tri.getName());
				fail("Should not succeed when disconnected");
			} catch (NotConnected NC) {
				System.out.println("getName() failed with exception " + NC);
			}
			clientconn = null;
			serverconn = null;

			if (!done1) {
				fail("Signal handler 1 failed to be run");
			}
			if (!done2) {
				fail("Signal handler 2 failed to be run");
			}
			if (!done3) {
				fail("Signal handler 3 failed to be run");
			}
			if (!done4) {
				fail("Callback handler failed to be run");
			}
			if (!done5) {
				fail("Signal handler R failed to be run");
			}
			if (!done6) {
				fail("Disconnect handler failed to be run");
			}
			if (!done7) {
				fail("Signal handler E failed to be run");
			}
			if (!done8) {
				fail("Error callback handler failed to be run");
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected Exception Occurred: " + e);
		}
	}
}
