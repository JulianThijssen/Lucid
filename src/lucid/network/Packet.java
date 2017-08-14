package lucid.network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * The packet format specifies each piece of data must be preceded by a byte indicating its type.
 * Below follows a list of these types and their values:
 * False      - 0x00
 * True       - 0x01
 * Byte       - 0x02
 * Short      - 0x03
 * Integer    - 0x04
 * Float      - 0x05
 * String     - 0x06
 * Long       - 0x07
 * 
 * @author Julian Thijssen
 */

public class Packet {
	/* Type of the packet */
	private int type = -1;
	
	/* The address the packet originated from */
	private InetSocketAddress address = null;
	
	/* Position of the mark in the array */
	private int pos = 0;
	
	/* Holds all the data in a packet */
	private byte[] data = null;
	
	public Packet(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
	
	public InetSocketAddress getSource() {
		return address;
	}
	
	public void setSource(InetSocketAddress address) {
		this.address = address;
	}
	
	public int getLength() {
		return data.length;
	}
	
	public void reset() {
		pos = 0;
	}
	
	public void addBoolean(boolean b) {
		increaseCapacity(1);
		if (b) {
			data[pos++] = 0x01;
		} else {
			data[pos++] = 0x00;
		}
	}
	
	public void addByte(byte b) {
		increaseCapacity(2);
		data[pos++] = 0x02;
		data[pos++] = b;
	}
	
	public void addShort(short s) {
		increaseCapacity(3);
		data[pos++] = 0x03;
		data[pos++] = (byte) (s >> 8);
		data[pos++] = (byte) (s >> 0);
	}
	
	public void addInt(int i) {
		increaseCapacity(5);
		data[pos++] = 0x04;
		data[pos++] = (byte) (i >> 24);
		data[pos++] = (byte) (i >> 16);
		data[pos++] = (byte) (i >> 8);
		data[pos++] = (byte) (i >> 0);
	}
	
	public void addFloat(float f) {
		increaseCapacity(5);
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.putFloat(f);
		data[pos++] = 0x05;
		data[pos++] = buf.get(0);
		data[pos++] = buf.get(1);
		data[pos++] = buf.get(2);
		data[pos++] = buf.get(3);
	}
	
	public void addString(String s) {
		increaseCapacity(3 + s.length());
		data[pos++] = 0x06;
		data[pos++] = (byte) (s.length() >> 8);
		data[pos++] = (byte) (s.length() >> 0);
		for(int i = 0; i < s.length(); i++) {
			data[pos++] = (byte) s.charAt(i);
		}
	}
	
	public void addLong(long l) {
		increaseCapacity(9);
		data[pos++] = 0x07;
		data[pos++] = (byte) (l >> 56);
		data[pos++] = (byte) (l >> 48);
		data[pos++] = (byte) (l >> 40);
		data[pos++] = (byte) (l >> 32);
		data[pos++] = (byte) (l >> 24);
		data[pos++] = (byte) (l >> 16);
		data[pos++] = (byte) (l >> 8);
		data[pos++] = (byte) (l >> 0);
	}
	
	public boolean getBoolean() {
		byte b = data[pos++];
		if(b == 0x01) {
			return true;
		}
		return false;
	}
	
	public byte getByte() {
		if(data[pos++] != 0x02) {
			return -1;
		}
		return data[pos++];
	}
	
	public short getShort() {
		if (data[pos++] != 0x03) {
			return -1;
		}
		byte[] b = new byte[2];
		b[0] = data[pos++];
		b[1] = data[pos++];
		return (short) (b[0] << 8 | b[1] << 0);
	}
	
	public int getInt() {
		if (data[pos++] != 0x04) {
			return -1;
		}
		byte[] b = new byte[4];
		b[0] = data[pos++];
		b[1] = data[pos++];
		b[2] = data[pos++];
		b[3] = data[pos++];
		return b[0] << 24 | b[1] << 16 | b[2] << 8 | b[3] << 0;
	}
	
	public float getFloat() {
		if (data[pos++] != 0x05) {
			return -1;
		}
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.put(data[pos++]);
		buf.put(data[pos++]);
		buf.put(data[pos++]);
		buf.put(data[pos++]);
		buf.flip();
		return buf.getFloat();
	}
	
	public String getString() {
		String s = "";
		if(data[pos++] != 0x06) {
			return null;
		}
		int length = (data[pos++] << 8) | (data[pos++]);
		for(int i = 0; i < length; i++) {
			s = s + (char) data[pos++];
		}
		return s;
	}
	
	public long getLong() {
		if (data[pos++] != 0x07) {
			return -1;
		}
		
		long l = 0;
		
		int shift = 56;
		for (int i = 0; i < 8; i++) {
			byte b = data[pos++];
			long mask = 0x00000000000000FFL << shift;
			l |= (((long) b) << shift) & mask;
			shift -= 8;
		}
		
		return l;
	}
	
	public void increaseCapacity(int delta) {
		if(data == null) {data = new byte[delta]; return;}
		byte[] b = new byte[data.length + delta];
		for(int i = 0; i < data.length; i++) {
			b[i] = data[i];
		}
		data = b;
	}
	
	public byte[] getData() {
		if (data == null) { return null; }
		
		byte b[] = new byte[data.length + 3];
		b[0] = (byte) type;
		b[1] = (byte) (data.length >> 8);
		b[2] = (byte) (data.length >> 0);
		for(int i = 0; i < data.length; i++) {
			b[3 + i] = data[i];
		}
		return b;
	}
	
	public static Packet fromByteBuffer(ByteBuffer buffer) {
		int type = buffer.get();
		if (type == -1) {
			return null;
		}
		
		int len = buffer.getShort();
		
		Packet packet = new Packet(type);
		byte[] b = new byte[len];
		for(int i = 0; i < len; i++) {
			b[i] = buffer.get();
		}
		packet.setData(b);
		
		return packet;
	}
	
	public static ByteBuffer toByteBuffer(Packet packet) {
		ByteBuffer buffer = ByteBuffer.allocate(packet.getLength() + 3);
		byte[] data = packet.getData();
		buffer.put(data);
		buffer.flip();
		
		return buffer;
	}
	
	public void setData(byte[] b) {
		data = b;
	}
	
	//Turns the packet into string form
	@Override
	public String toString() {
		String s = "";
		s = s + String.format("Type: %02X \n Data: ", type);
		for(int i = 0; i < data.length; i++) {
			s = s + String.format("%02X ", data[i]);
		}
		return s;
	}
}
