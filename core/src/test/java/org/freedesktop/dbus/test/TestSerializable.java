/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.DBusSerializable;
import org.freedesktop.dbus.exceptions.DBusException;

public class TestSerializable<A> implements DBusSerializable
{

	private int a;
	private String b;
	private List<Integer> c;

	public TestSerializable(int a, A b, List<Integer> c)
	{
		this.a = a;
		this.b = b.toString();
		this.c = c;
	}

	public TestSerializable()
	{
	}

	public void deserialize(int a, String b, List<Integer> c)
	{
		this.a = a;
		this.b = b;
		this.c = new ArrayList<Integer>(c);
	}

	@Override
	public Object[] serialize() throws DBusException
	{
		return new Object[] { a, b, c };
	}

	public int getInt()
	{
		return a;
	}

	public String getString()
	{
		return b;
	}

	public List<Integer> getList()
	{
		return c;
	}

	@Override
	public String toString()
	{
		return "TestSerializable{" + a + "," + b + "," + c + "}";
	}

}
