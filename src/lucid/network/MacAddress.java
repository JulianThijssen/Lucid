package lucid.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import lucid.util.Log;

public class MacAddress {
	public static long getAddress() {
		try {
			InetAddress ip = InetAddress.getLocalHost();
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			byte[] mac = network.getHardwareAddress();
			
			ByteBuffer macbuffer = ByteBuffer.allocate(8);
			macbuffer.put((byte) 0);
			macbuffer.put((byte) 0);
			macbuffer.put(mac);
			macbuffer.flip();
			
			return macbuffer.getLong();
		} catch(UnknownHostException e) {
			Log.error("Failed to get local MAC address, local host could not be resolved into an address");
		} catch(BufferOverflowException e) {
			Log.error("Failed to get local MAC address, address is longer than 6 bytes.");
		} catch(BufferUnderflowException e) {
			Log.error("Failed to get local MAC address, address is less than 6 bytes.");
		} catch (Exception e) {
			Log.error("Failed to get local MAC address for unknown reason");
		}
		return -1;
	}
}
