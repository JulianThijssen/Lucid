package tests;

import static org.junit.Assert.*;

import org.junit.Test;

import lucid.exceptions.PacketException;
import lucid.network.Packet;

public class PacketTest {
	@Test
	public void booleanTest() throws PacketException {
		try {
			Packet packet = new Packet((short) 0);
			
			packet.addBoolean(false);
			packet.addBoolean(true);
			packet.reset();
			boolean a = packet.getBoolean();
			boolean b = packet.getBoolean();

			if (a != false) {
				fail("Packet boolean test fail: " + a + " != " + false);
			}
			if (b != true) {
				fail("Packet boolean test fail: " + b + " != " + true);
			}
		} catch (PacketException e) {
			fail("Boolean packet exception");
		}
	}
	
	@Test
	public void byteTest() throws PacketException {
		try {
			for (byte a = Byte.MIN_VALUE; a < Byte.MAX_VALUE; a++) {
				Packet packet = new Packet((short) 0);
				
				packet.addByte(a);
				packet.reset();
				int b = packet.getByte();
				
				if (a != b) {
					fail("Packet byte test fail: " + a + " != " + b);
				}
			}
		} catch (PacketException e) {
			fail("Byte packet exception");
		}
	}
	
	@Test
	public void shortTest() throws PacketException {
		try {
			for (short a = Short.MIN_VALUE; a < Short.MAX_VALUE; a++) {
				Packet packet = new Packet((short) 0);
				
				packet.addShort(a);
				packet.reset();
				int b = packet.getShort();
				
				if (a != b) {
					fail("Packet short test fail: " + a + " != " + b);
				}
			}
		} catch (PacketException e) {
			fail("Short packet exception");
		}
	}
	
	@Test
	public void integerTest() throws PacketException {
		try {
			for (int a = -100000; a < 100000; a++) {
				Packet packet = new Packet((short) 0);
				
				packet.addInt(a);
				packet.reset();
				int b = packet.getInt();
				
				if (a != b) {
					fail("Packet integer test fail: " + a + " != " + b);
				}
			}
		} catch (PacketException e) {
			fail("Integer packet exception");
		}
	}
	
	@Test
	public void floatTest() throws PacketException {
		try {
			for (float a = -100000; a < 100000; a++) {
				Packet packet = new Packet((short) 0);
				
				packet.addFloat(a);
				packet.reset();
				float b = packet.getFloat();
				
				if (a != b) {
					fail("Packet float test fail: " + a + " != " + b);
				}
			}
		} catch (PacketException e) {
			fail("Float packet exception");
		}
	}
	
	@Test
	public void longTest() throws PacketException {
		try {
			for (long a = -100000; a < 100000; a++) {
				Packet packet = new Packet((short) 0);
				
				packet.addLong(a);
				packet.reset();
				long b = packet.getLong();
				
				if (a != b) {
					fail("Packet long test fail: " + a + " != " + b);
				}
			}
		} catch (PacketException e) {
			fail("Long packet exception");
		}
	}
	
	@Test
	public void stringTest() throws PacketException {
		try {
			Packet packet = new Packet((short) 0);
			
			String a = "Test string 1234567890 !@#$%^&*()";
			packet.addString(a);
			packet.reset();
			String b = packet.getString();
			
			if (!a.equals(b)) {
				fail("Packet string test fail: " + a + " != " + b);
			}
		} catch (PacketException e) {
			fail("String packet exception");
		}
	}
}
