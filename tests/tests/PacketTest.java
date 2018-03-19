package tests;

import static org.junit.Assert.*;

import org.junit.Test;

import lucid.network.Packet;

public class PacketTest {

	@Test
	public void test() {
		// Integer test
		for (int a = 0; a < 100000; a += 100) {
			Packet packet = new Packet((short) 0);
			
			packet.addInt(a);
			packet.reset();
			int b = packet.getInt();
			
			if (a != b) {
				fail("Packet integer test fail: " + a + " != " + b);
			}
		}
		
		// Float test
		for (float a = 0; a < 100000; a += 100) {
			Packet packet = new Packet((short) 0);
			
			packet.addFloat(a);
			packet.reset();
			float b = packet.getFloat();
			
			if (a != b) {
				fail("Packet float test fail: " + a + " != " + b);
			}
		}
	}

}
