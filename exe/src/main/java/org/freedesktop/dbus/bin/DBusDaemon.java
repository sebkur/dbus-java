/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.bin;

import static org.freedesktop.dbus.Gettext._;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.freedesktop.DBus;
import org.freedesktop.dbus.AbstractConnection;
import org.freedesktop.dbus.BusAddress;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.DirectConnection;
import org.freedesktop.dbus.Error;
import org.freedesktop.dbus.Introspectables;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.Message;
import org.freedesktop.dbus.MessageReader;
import org.freedesktop.dbus.MessageWriter;
import org.freedesktop.dbus.MethodCall;
import org.freedesktop.dbus.MethodReturn;
import org.freedesktop.dbus.Transport;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.exceptions.FatalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cx.ath.matthew.unix.UnixServerSocket;
import cx.ath.matthew.unix.UnixSocket;
import cx.ath.matthew.unix.UnixSocketAddress;

/**
 * A replacement DBusDaemon
 */
public class DBusDaemon extends Thread
{

	final static Logger logger = LoggerFactory.getLogger(DBusDaemon.class);

	public static final int QUEUE_POLL_WAIT = 500;

	static class Connstruct
	{
		public UnixSocket usock;
		public Socket tsock;
		public MessageReader min;
		public MessageWriter mout;
		public String unique;

		public Connstruct(UnixSocket sock)
		{
			this.usock = sock;
			min = new MessageReader(sock.getInputStream());
			mout = new MessageWriter(sock.getOutputStream());
		}

		public Connstruct(Socket sock) throws IOException
		{
			this.tsock = sock;
			min = new MessageReader(sock.getInputStream());
			mout = new MessageWriter(sock.getOutputStream());
		}

		@Override
		public String toString()
		{
			return null == unique ? ":?-?" : unique;
		}
	}

	static class MagicMap<A, B>
	{
		private Map<A, LinkedList<B>> m;
		private LinkedList<A> q;
		private String name;

		public MagicMap(String name)
		{
			m = new HashMap<A, LinkedList<B>>();
			q = new LinkedList<A>();
			this.name = name;
		}

		public A head()
		{
			return q.getFirst();
		}

		public void putFirst(A a, B b)
		{
			logger.debug("<" + name + "> Queueing {" + a + " => " + b + "}");
			if (m.containsKey(a)) {
				m.get(a).add(b);
			} else {
				LinkedList<B> l = new LinkedList<B>();
				l.add(b);
				m.put(a, l);
			}
			q.addFirst(a);
		}

		public void putLast(A a, B b)
		{
			logger.debug("<" + name + "> Queueing {" + a + " => " + b + "}");
			if (m.containsKey(a)) {
				m.get(a).add(b);
			} else {
				LinkedList<B> l = new LinkedList<B>();
				l.add(b);
				m.put(a, l);
			}
			q.addLast(a);
		}

		public List<B> remove(A a)
		{
			logger.debug("<" + name + "> Removing {" + a + "}");
			q.remove(a);
			return m.remove(a);
		}

		public int size()
		{
			return q.size();
		}
	}

	public class DBusServer extends Thread
			implements DBus, DBus.Introspectable, DBus.Peer
	{
		public DBusServer()
		{
			setName("Server");
		}

		public Connstruct c;
		public Message m;

		@Override
		public boolean isRemote()
		{
			return false;
		}

		@Override
		public String Hello()
		{
			logger.debug("enter");
			synchronized (c) {
				if (null != c.unique) {
					throw new org.freedesktop.DBus.Error.AccessDenied(
							_("Connection has already sent a Hello message"));
				}
				synchronized (unique_lock) {
					c.unique = ":1." + (++next_unique);
				}
			}
			synchronized (names) {
				names.put(c.unique, c);
			}

			logger.warn("Client " + c.unique + " registered");

			try {
				send(c, new DBusSignal("org.freedesktop.DBus",
						"/org/freedesktop/DBus", "org.freedesktop.DBus",
						"NameAcquired", "s", c.unique));
				DBusSignal s = new DBusSignal("org.freedesktop.DBus",
						"/org/freedesktop/DBus", "org.freedesktop.DBus",
						"NameOwnerChanged", "sss", c.unique, "", c.unique);
				send(null, s);
			} catch (DBusException DBe) {
				if (AbstractConnection.EXCEPTION_DEBUG) {
					logger.error("", DBe);
				}
			}
			logger.debug("exit");
			return c.unique;
		}

		@Override
		public String[] ListNames()
		{
			logger.debug("enter");
			String[] ns;
			synchronized (names) {
				Set<String> nss = names.keySet();
				ns = nss.toArray(new String[0]);
			}
			logger.debug("exit");
			return ns;
		}

		@Override
		public boolean NameHasOwner(String name)
		{
			logger.debug("enter");
			boolean rv;
			synchronized (names) {
				rv = names.containsKey(name);
			}
			logger.debug("exit");
			return rv;
		}

		@Override
		public String GetNameOwner(String name)
		{
			logger.debug("enter");
			Connstruct owner = names.get(name);
			String o;
			if (null == owner) {
				o = "";
			} else {
				o = owner.unique;
			}
			logger.debug("exit");
			return o;
		}

		@Override
		public UInt32 GetConnectionUnixUser(String connection_name)
		{
			logger.debug("enter");
			logger.debug("exit");
			return new UInt32(0);
		}

		@Override
		public UInt32 StartServiceByName(String name, UInt32 flags)
		{
			logger.debug("enter");
			logger.debug("exit");
			return new UInt32(0);
		}

		@Override
		public UInt32 RequestName(String name, UInt32 flags)
		{
			logger.debug("enter");

			boolean exists = false;
			synchronized (names) {
				if (!(exists = names.containsKey(name))) {
					names.put(name, c);
				}
			}

			int rv;
			if (exists) {
				rv = DBus.DBUS_REQUEST_NAME_REPLY_EXISTS;
			} else {
				logger.warn("Client " + c.unique + " acquired name " + name);
				rv = DBus.DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER;
				try {
					send(c, new DBusSignal("org.freedesktop.DBus",
							"/org/freedesktop/DBus", "org.freedesktop.DBus",
							"NameAcquired", "s", name));
					send(null, new DBusSignal("org.freedesktop.DBus",
							"/org/freedesktop/DBus", "org.freedesktop.DBus",
							"NameOwnerChanged", "sss", name, "", c.unique));
				} catch (DBusException DBe) {
					if (AbstractConnection.EXCEPTION_DEBUG) {
						logger.error("", DBe);
					}
				}
			}

			logger.debug("exit");
			return new UInt32(rv);
		}

		@Override
		public UInt32 ReleaseName(String name)
		{
			logger.debug("enter");

			boolean exists = false;
			synchronized (names) {
				if ((exists = (names.containsKey(name)
						&& names.get(name).equals(c)))) {
					names.remove(name);
				}
			}

			int rv;
			if (!exists) {
				rv = DBus.DBUS_RELEASE_NAME_REPLY_NON_EXISTANT;
			} else {
				logger.warn("Client " + c.unique + " acquired name " + name);
				rv = DBus.DBUS_RELEASE_NAME_REPLY_RELEASED;
				try {
					send(c, new DBusSignal("org.freedesktop.DBus",
							"/org/freedesktop/DBus", "org.freedesktop.DBus",
							"NameLost", "s", name));
					send(null, new DBusSignal("org.freedesktop.DBus",
							"/org/freedesktop/DBus", "org.freedesktop.DBus",
							"NameOwnerChanged", "sss", name, c.unique, ""));
				} catch (DBusException DBe) {
					if (AbstractConnection.EXCEPTION_DEBUG) {
						logger.error("", DBe);
					}
				}
			}

			logger.debug("exit");
			return new UInt32(rv);
		}

		@Override
		public void AddMatch(String matchrule) throws Error.MatchRuleInvalid
		{
			logger.debug("enter");
			logger.trace("Adding match rule: " + matchrule);
			synchronized (sigrecips) {
				if (!sigrecips.contains(c)) {
					sigrecips.add(c);
				}
			}
			logger.debug("exit");
			return;
		}

		@Override
		public void RemoveMatch(String matchrule) throws Error.MatchRuleInvalid
		{
			logger.debug("enter");
			logger.trace("Removing match rule: " + matchrule);
			logger.debug("exit");
			return;
		}

		@Override
		public String[] ListQueuedOwners(String name)
		{
			logger.debug("enter");
			logger.debug("exit");
			return new String[0];
		}

		@Override
		public UInt32 GetConnectionUnixProcessID(String connection_name)
		{
			logger.debug("enter");
			logger.debug("exit");
			return new UInt32(0);
		}

		@Override
		public Byte[] GetConnectionSELinuxSecurityContext(String a)
		{
			logger.debug("enter");
			logger.debug("exit");
			return new Byte[0];
		}

		@Override
		public void ReloadConfig()
		{
			logger.debug("enter");
			logger.debug("exit");
			return;
		}

		@SuppressWarnings("unchecked")
		private void handleMessage(Connstruct c, Message m) throws DBusException
		{
			logger.debug("enter");
			logger.trace("Handling message " + m + " from " + c.unique);
			if (!(m instanceof MethodCall)) {
				return;
			}
			Object[] args = m.getParameters();

			Class<? extends Object>[] cs = new Class[args.length];

			for (int i = 0; i < cs.length; i++) {
				cs[i] = args[i].getClass();
			}

			java.lang.reflect.Method meth = null;
			Object rv = null;

			try {
				meth = DBusServer.class.getMethod(m.getName(), cs);
				try {
					this.c = c;
					this.m = m;
					rv = meth.invoke(dbus_server, args);
					if (null == rv) {
						send(c, new MethodReturn("org.freedesktop.DBus",
								(MethodCall) m, null), true);
					} else {
						String sig = Marshalling
								.getDBusType(meth.getGenericReturnType())[0];
						send(c, new MethodReturn("org.freedesktop.DBus",
								(MethodCall) m, sig, rv), true);
					}
				} catch (InvocationTargetException ITe) {
					if (AbstractConnection.EXCEPTION_DEBUG) {
						logger.error("", ITe);
					}
					if (AbstractConnection.EXCEPTION_DEBUG) {
						logger.error("", ITe.getCause());
					}
					send(c, new org.freedesktop.dbus.Error(
							"org.freedesktop.DBus", m, ITe.getCause()));
				} catch (DBusExecutionException DBEe) {
					if (AbstractConnection.EXCEPTION_DEBUG) {
						logger.error("", DBEe);
					}
					send(c, new org.freedesktop.dbus.Error(
							"org.freedesktop.DBus", m, DBEe));
				} catch (Exception e) {
					if (AbstractConnection.EXCEPTION_DEBUG) {
						logger.error("", e);
					}
					send(c, new org.freedesktop.dbus.Error(
							"org.freedesktop.DBus", c.unique,
							"org.freedesktop.DBus.Error.GeneralError",
							m.getSerial(), "s",
							_("An error occurred while calling ")
									+ m.getName()));
				}
			} catch (NoSuchMethodException NSMe) {
				send(c, new org.freedesktop.dbus.Error("org.freedesktop.DBus",
						c.unique, "org.freedesktop.DBus.Error.UnknownMethod",
						m.getSerial(), "s",
						_("This service does not support ") + m.getName()));
			}

			logger.debug("exit");
		}

		@Override
		public String Introspect()
		{
			return Introspectables.DOCTYPE + "\n<node>\n"
					+ "  <interface name=\"org.freedesktop.DBus.Introspectable\">\n"
					+ "    <method name=\"Introspect\">\n"
					+ "      <arg name=\"data\" direction=\"out\" type=\"s\"/>\n"
					+ "    </method>\n" + "  </interface>\n"
					+ "  <interface name=\"org.freedesktop.DBus\">\n"
					+ "    <method name=\"RequestName\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "      <arg direction=\"in\" type=\"u\"/>\n"
					+ "      <arg direction=\"out\" type=\"u\"/>\n"
					+ "    </method>\n" + "    <method name=\"ReleaseName\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "      <arg direction=\"out\" type=\"u\"/>\n"
					+ "    </method>\n"
					+ "    <method name=\"StartServiceByName\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "      <arg direction=\"in\" type=\"u\"/>\n"
					+ "      <arg direction=\"out\" type=\"u\"/>\n"
					+ "    </method>\n" + "    <method name=\"Hello\">\n"
					+ "      <arg direction=\"out\" type=\"s\"/>\n"
					+ "    </method>\n" + "    <method name=\"NameHasOwner\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "      <arg direction=\"out\" type=\"b\"/>\n"
					+ "    </method>\n" + "    <method name=\"ListNames\">\n"
					+ "      <arg direction=\"out\" type=\"as\"/>\n"
					+ "    </method>\n"
					+ "    <method name=\"ListActivatableNames\">\n"
					+ "      <arg direction=\"out\" type=\"as\"/>\n"
					+ "    </method>\n" + "    <method name=\"AddMatch\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "    </method>\n" + "    <method name=\"RemoveMatch\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "    </method>\n" + "    <method name=\"GetNameOwner\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "      <arg direction=\"out\" type=\"s\"/>\n"
					+ "    </method>\n"
					+ "    <method name=\"ListQueuedOwners\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "      <arg direction=\"out\" type=\"as\"/>\n"
					+ "    </method>\n"
					+ "    <method name=\"GetConnectionUnixUser\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "      <arg direction=\"out\" type=\"u\"/>\n"
					+ "    </method>\n"
					+ "    <method name=\"GetConnectionUnixProcessID\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "      <arg direction=\"out\" type=\"u\"/>\n"
					+ "    </method>\n"
					+ "    <method name=\"GetConnectionSELinuxSecurityContext\">\n"
					+ "      <arg direction=\"in\" type=\"s\"/>\n"
					+ "      <arg direction=\"out\" type=\"ay\"/>\n"
					+ "    </method>\n" + "    <method name=\"ReloadConfig\">\n"
					+ "    </method>\n"
					+ "    <signal name=\"NameOwnerChanged\">\n"
					+ "      <arg type=\"s\"/>\n" + "      <arg type=\"s\"/>\n"
					+ "      <arg type=\"s\"/>\n" + "    </signal>\n"
					+ "    <signal name=\"NameLost\">\n"
					+ "      <arg type=\"s\"/>\n" + "    </signal>\n"
					+ "    <signal name=\"NameAcquired\">\n"
					+ "      <arg type=\"s\"/>\n" + "    </signal>\n"
					+ "  </interface>\n" + "</node>";
		}

		@Override
		public void Ping()
		{
		}

		@Override
		public void run()
		{
			logger.debug("enter");
			while (_run) {
				Message m;
				List<WeakReference<Connstruct>> wcs;
				// block on outqueue
				synchronized (localqueue) {
					while (localqueue.size() == 0) {
						try {
							localqueue.wait();
						} catch (InterruptedException Ie) {
						}
					}
					m = localqueue.head();
					wcs = localqueue.remove(m);
				}
				if (null != wcs) {
					try {
						for (WeakReference<Connstruct> wc : wcs) {
							Connstruct c = wc.get();
							if (null != c) {
								logger.trace("<localqueue> Got message " + m
										+ " from " + c);
								handleMessage(c, m);
							}
						}
					} catch (DBusException DBe) {
						if (AbstractConnection.EXCEPTION_DEBUG) {
							logger.error("", DBe);
						}
					}
				} else {
					logger.info("Discarding " + m + " connection reaped");
				}
			}
			logger.debug("exit");
		}
	}

	public class Sender extends Thread
	{
		public Sender()
		{
			setName("Sender");
		}

		@Override
		public void run()
		{
			logger.debug("enter");
			while (_run) {

				logger.trace(
						"Acquiring lock on outqueue and blocking for data");
				Message m = null;
				List<WeakReference<Connstruct>> wcs = null;
				// block on outqueue
				synchronized (outqueue) {
					while (outqueue.size() == 0) {
						try {
							outqueue.wait();
						} catch (InterruptedException Ie) {
						}
					}

					m = outqueue.head();
					wcs = outqueue.remove(m);
				}
				if (null != wcs) {
					for (WeakReference<Connstruct> wc : wcs) {
						Connstruct c = wc.get();
						if (null != c) {
							logger.trace("<outqueue> Got message " + m + " for "
									+ c.unique);
							logger.info(
									"Sending message " + m + " to " + c.unique);
							try {
								c.mout.writeMessage(m);
							} catch (IOException IOe) {
								if (AbstractConnection.EXCEPTION_DEBUG) {
									logger.error("", IOe);
								}
								removeConnection(c);
							}
						}
					}
				} else {
					logger.info("Discarding " + m + " connection reaped");
				}
			}
			logger.debug("exit");
		}
	}

	public class Reader extends Thread
	{
		private Connstruct conn;
		private WeakReference<Connstruct> weakconn;
		private boolean _lrun = true;

		public Reader(Connstruct conn)
		{
			this.conn = conn;
			weakconn = new WeakReference<Connstruct>(conn);
			setName("Reader");
		}

		public void stopRunning()
		{
			_lrun = false;
		}

		@Override
		public void run()
		{
			logger.debug("enter");
			while (_run && _lrun) {

				Message m = null;
				try {
					m = conn.min.readMessage();
				} catch (IOException IOe) {
					if (AbstractConnection.EXCEPTION_DEBUG) {
						logger.error("", IOe);
					}
					removeConnection(conn);
				} catch (DBusException DBe) {
					if (AbstractConnection.EXCEPTION_DEBUG) {
						logger.error("", DBe);
					}
					if (DBe instanceof FatalException) {
						removeConnection(conn);
					}
				}

				if (null != m) {
					logger.info("Read " + m + " from " + conn.unique);
					synchronized (inqueue) {
						inqueue.putLast(m, weakconn);
						inqueue.notifyAll();
					}
				}
			}
			conn = null;
			logger.debug("exit");
		}
	}

	private Map<Connstruct, Reader> conns = new HashMap<Connstruct, Reader>();
	private HashMap<String, Connstruct> names = new HashMap<String, Connstruct>();
	private MagicMap<Message, WeakReference<Connstruct>> outqueue = new MagicMap<Message, WeakReference<Connstruct>>(
			"out");
	private MagicMap<Message, WeakReference<Connstruct>> inqueue = new MagicMap<Message, WeakReference<Connstruct>>(
			"in");
	private MagicMap<Message, WeakReference<Connstruct>> localqueue = new MagicMap<Message, WeakReference<Connstruct>>(
			"local");
	private List<Connstruct> sigrecips = new ArrayList<Connstruct>();
	private boolean _run = true;
	private int next_unique = 0;
	private Object unique_lock = new Object();
	DBusServer dbus_server = new DBusServer();
	Sender sender = new Sender();

	public DBusDaemon()
	{
		setName("Daemon");
		synchronized (names) {
			names.put("org.freedesktop.DBus", null);
		}
	}

	@SuppressWarnings("unchecked")
	private void send(Connstruct c, Message m)
	{
		send(c, m, false);
	}

	private void send(Connstruct c, Message m, boolean head)
	{
		logger.debug("enter");
		if (null == c) {
			logger.trace("Queing message " + m + " for all connections");
		} else {
			logger.trace("Queing message " + m + " for " + c.unique);
		}
		// send to all connections
		if (null == c) {
			synchronized (conns) {
				synchronized (outqueue) {
					for (Connstruct d : conns.keySet()) {
						if (head) {
							outqueue.putFirst(m,
									new WeakReference<Connstruct>(d));
						} else {
							outqueue.putLast(m,
									new WeakReference<Connstruct>(d));
						}
					}
					outqueue.notifyAll();
				}
			}
		} else {
			synchronized (outqueue) {
				if (head) {
					outqueue.putFirst(m, new WeakReference<Connstruct>(c));
				} else {
					outqueue.putLast(m, new WeakReference<Connstruct>(c));
				}
				outqueue.notifyAll();
			}
		}
		logger.debug("exit");
	}

	@SuppressWarnings("unchecked")
	private List<Connstruct> findSignalMatches(DBusSignal sig)
	{
		logger.debug("enter");
		List<Connstruct> l;
		synchronized (sigrecips) {
			l = new ArrayList<Connstruct>(sigrecips);
		}
		logger.debug("exit");
		return l;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run()
	{
		logger.debug("enter");
		while (_run) {
			try {
				Message m;
				List<WeakReference<Connstruct>> wcs;
				synchronized (inqueue) {
					while (0 == inqueue.size()) {
						try {
							inqueue.wait();
						} catch (InterruptedException Ie) {
						}
					}

					m = inqueue.head();
					wcs = inqueue.remove(m);
				}
				if (null != wcs) {
					for (WeakReference<Connstruct> wc : wcs) {
						Connstruct c = wc.get();
						if (null != c) {
							logger.info("<inqueue> Got message " + m + " from "
									+ c.unique);
							// check if they have hello'd
							if (null == c.unique && (!(m instanceof MethodCall)
									|| !"org.freedesktop.DBus"
											.equals(m.getDestination())
									|| !"Hello".equals(m.getName()))) {
								send(c, new Error("org.freedesktop.DBus", null,
										"org.freedesktop.DBus.Error.AccessDenied",
										m.getSerial(), "s",
										_("You must send a Hello message")));
							} else {
								try {
									if (null != c.unique) {
										m.setSource(c.unique);
									}
								} catch (DBusException DBe) {
									if (AbstractConnection.EXCEPTION_DEBUG) {
										logger.error("", DBe);
									}
									send(c, new Error("org.freedesktop.DBus",
											null,
											"org.freedesktop.DBus.Error.GeneralError",
											m.getSerial(), "s",
											_("Sending message failed")));
								}

								if ("org.freedesktop.DBus"
										.equals(m.getDestination())) {
									synchronized (localqueue) {
										localqueue.putLast(m, wc);
										localqueue.notifyAll();
									}
								} else {
									if (m instanceof DBusSignal) {
										List<Connstruct> list = findSignalMatches(
												(DBusSignal) m);
										for (Connstruct d : list) {
											send(d, m);
										}
									} else {
										Connstruct dest = names
												.get(m.getDestination());

										if (null == dest) {
											send(c, new Error(
													"org.freedesktop.DBus",
													null,
													"org.freedesktop.DBus.Error.ServiceUnknown",
													m.getSerial(), "s",
													MessageFormat.format(
															_("The name `{0}' does not exist"),
															new Object[] { m
																	.getDestination() })));
										} else {
											send(dest, m);
										}
									}
								}
							}
						}
					}
				}
			} catch (DBusException DBe) {
				if (AbstractConnection.EXCEPTION_DEBUG) {
					logger.error("", DBe);
				}
			}
		}
		logger.debug("exit");

	}

	private void removeConnection(Connstruct c)
	{
		logger.debug("enter");
		boolean exists;
		synchronized (conns) {
			if ((exists = conns.containsKey(c))) {
				Reader r = conns.get(c);
				r.stopRunning();
				conns.remove(c);
			}
		}
		if (exists) {
			try {
				if (null != c.usock) {
					c.usock.close();
				}
				if (null != c.tsock) {
					c.tsock.close();
				}
			} catch (IOException IOe) {
			}
			synchronized (names) {
				List<String> toRemove = new ArrayList<String>();
				for (String name : names.keySet()) {
					if (names.get(name) == c) {
						toRemove.add(name);
						try {
							send(null, new DBusSignal("org.freedesktop.DBus",
									"/org/freedesktop/DBus",
									"org.freedesktop.DBus", "NameOwnerChanged",
									"sss", name, c.unique, ""));
						} catch (DBusException DBe) {
							if (AbstractConnection.EXCEPTION_DEBUG) {
								logger.error("", DBe);
							}
						}
					}
				}
				for (String name : toRemove) {
					names.remove(name);
				}
			}
		}
		logger.debug("exit");
	}

	public void addSock(UnixSocket us)
	{
		logger.debug("enter");
		logger.warn("New Client");
		Connstruct c = new Connstruct(us);
		Reader r = new Reader(c);
		synchronized (conns) {
			conns.put(c, r);
		}
		r.start();
		logger.debug("exit");
	}

	public void addSock(Socket s) throws IOException
	{
		logger.debug("enter");
		logger.warn("New Client");
		Connstruct c = new Connstruct(s);
		Reader r = new Reader(c);
		synchronized (conns) {
			conns.put(c, r);
		}
		r.start();
		logger.debug("exit");
	}

	public static void syntax()
	{
		System.out.println(
				"Syntax: DBusDaemon [--version] [-v] [--help] [-h] [--listen address] [-l address] [--print-address] [-r] [--pidfile file] [-p file] [--addressfile file] [-a file] [--unix] [-u] [--tcp] [-t] ");
		System.exit(1);
	}

	public static void version()
	{
		System.out.println(
				"D-Bus Java Version: " + System.getProperty("Version"));
		System.exit(1);
	}

	public static void saveFile(String data, String file) throws IOException
	{
		PrintWriter w = new PrintWriter(new FileOutputStream(file));
		w.println(data);
		w.close();
	}

	public static void main(String args[]) throws Exception
	{
		logger.debug("enter");
		String addr = null;
		String pidfile = null;
		String addrfile = null;
		boolean printaddress = false;
		boolean unix = true;
		boolean tcp = false;

		// parse options
		try {
			for (int i = 0; i < args.length; i++) {
				if ("--help".equals(args[i]) || "-h".equals(args[i])) {
					syntax();
				} else if ("--version".equals(args[i])
						|| "-v".equals(args[i])) {
					version();
				} else if ("--listen".equals(args[i]) || "-l".equals(args[i])) {
					addr = args[++i];
				} else if ("--pidfile".equals(args[i])
						|| "-p".equals(args[i])) {
					pidfile = args[++i];
				} else if ("--addressfile".equals(args[i])
						|| "-a".equals(args[i])) {
					addrfile = args[++i];
				} else if ("--print-address".equals(args[i])
						|| "-r".equals(args[i])) {
					printaddress = true;
				} else if ("--unix".equals(args[i]) || "-u".equals(args[i])) {
					unix = true;
					tcp = false;
				} else if ("--tcp".equals(args[i]) || "-t".equals(args[i])) {
					tcp = true;
					unix = false;
				} else {
					syntax();
				}
			}
		} catch (ArrayIndexOutOfBoundsException AIOOBe) {
			syntax();
		}

		// generate a random address if none specified
		if (null == addr && unix) {
			addr = DirectConnection.createDynamicSession();
		} else if (null == addr && tcp) {
			addr = DirectConnection.createDynamicTCPSession();
		}

		BusAddress address = new BusAddress(addr);
		if (null == address.getParameter("guid")) {
			addr += ",guid=" + Transport.genGUID();
			address = new BusAddress(addr);
		}

		// print address to stdout
		if (printaddress) {
			System.out.println(addr);
		}

		// print address to file
		if (null != addrfile) {
			saveFile(addr, addrfile);
		}

		// print PID to file
		if (null != pidfile) {
			saveFile(System.getProperty("Pid"), pidfile);
		}

		// start the daemon
		logger.warn("Binding to " + addr);
		if ("unix".equals(address.getType())) {
			doUnix(address);
		} else if ("tcp".equals(address.getType())) {
			doTCP(address);
		} else {
			throw new Exception("Unknown address type: " + address.getType());
		}
		logger.debug("exit");
	}

	private static void doUnix(BusAddress address) throws IOException
	{
		logger.debug("enter");
		UnixServerSocket uss;
		if (null != address.getParameter("abstract")) {
			uss = new UnixServerSocket(new UnixSocketAddress(
					address.getParameter("abstract"), true));
		} else {
			uss = new UnixServerSocket(
					new UnixSocketAddress(address.getParameter("path"), false));
		}
		DBusDaemon d = new DBusDaemon();
		d.start();
		d.sender.start();
		d.dbus_server.start();

		// accept new connections
		while (d._run) {
			UnixSocket s = uss.accept();
			if ((new Transport.SASL()).auth(Transport.SASL.MODE_SERVER,
					Transport.SASL.AUTH_EXTERNAL, address.getParameter("guid"),
					s.getOutputStream(), s.getInputStream(), s)) {
				// s.setBlocking(false);
				d.addSock(s);
			} else {
				s.close();
			}
		}
		logger.debug("exit");
	}

	private static void doTCP(BusAddress address) throws IOException
	{
		logger.debug("enter");
		ServerSocket ss = new ServerSocket(
				Integer.parseInt(address.getParameter("port")), 10,
				InetAddress.getByName(address.getParameter("host")));
		DBusDaemon d = new DBusDaemon();
		d.start();
		d.sender.start();
		d.dbus_server.start();

		// accept new connections
		while (d._run) {
			Socket s = ss.accept();
			boolean authOK = false;
			try {
				authOK = (new Transport.SASL()).auth(Transport.SASL.MODE_SERVER,
						Transport.SASL.AUTH_EXTERNAL,
						address.getParameter("guid"), s.getOutputStream(),
						s.getInputStream(), null);
			} catch (Exception e) {
				logger.debug("", e);
			}
			if (authOK) {
				d.addSock(s);
			} else {
				s.close();
			}
		}
		logger.debug("exit");
	}

}
