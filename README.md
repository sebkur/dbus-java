# D-Bus Java README

This is an evolution of the Java DBus bindings originally developed by
Matthew Johnson from 2006 to 2009 which is listed on the
[DBusBindings page](https://www.freedesktop.org/wiki/Software/DBusBindings/)
on freedesktop.org and available through [this repository](https://cgit.freedesktop.org/dbus/dbus-java/).

The following things have been changed:

* Use gradle for building the project
* Get dependency jars from Maven repositories
* Replace the logging scheme with slf4j
* Use camel case names for all classes
* Formatted code
* Eliminate usage of `java.util.Vector`
* Reduce errors concerning DTD validation

Also some features have been added:

* Some utilities for listing UDisk devices

## See also

More information is available in the [changelog](changelog.md) and the [TODO](TODO.md) list.

## Executables

The installed programs are documented in their respective man pages.

**ListDBus** lists the names currently connected to a bus.

**CreateInterface** is a tool for creating interface stubs for D-Bus programs. It
will connect to services running on the bus and introspect on them to find
their API. This will then be written as Java interface definitions suitable for
importing into your program. A file containing the introspection data can be
used instead.

**DBusViewer** is a graphical tool which combines the two tools. It will list the
names on a bus and allow you to introspect on them and save the result as Java
interface files. This currently only introspects on the root object, however.

**DBusCall** can be used to execute methods on D-Bus objects.

**ListDisks** is a tool that lists disks by analyzing the udisks namespace.

## Old, outdated README content

Compilation and installation is described in the INSTALL file.

This will install two jar files, three binaries and some documentation in the
form of an HTML/PDF guide to writing D-Bus Java programs, JavaDoc API for the
library and man pages for the supplied programs. Read the documentation, it's
there for a reason.

To run a Java program using D-Bus you need to have the libdbus-java,
libunix-java and libdebug jar files in your classpath and the libunix-java
shared library in your library path. With the default install paths you may
have to do something like:

java -cp /usr/local/share/java/dbus.jar:/usr/local/share/java/unix.jar:/usr/local/share/java/debug-disable.jar -Djava.library.path=/usr/local/lib/jni
