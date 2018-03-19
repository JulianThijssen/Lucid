package lucid.network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * 
 * @author Julian Thijssen
 */

public class Packet {
	/**
	 * The packet format specifies each piece of data must be preceded by a byte indicating its type.
	 * Below follows a list of these types and their values:
	 */
	private static final byte FALSE   = 0;
	private static final byte TRUE    = 1;
	private static final byte BYTE    = 2;
	private static final byte SHORT   = 3;
	private static final byte INTEGER = 4;
	private static final byte FLOAT   = 5;
	private static final byte STRING  = 6;
	private static final byte LONG    = 7;
	
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
		return data != null ? data.length : 0;
	}
	
	public void reset() {
		pos = 0;
	}
	
	public void addBoolean(boolean b) {
		increaseCapacity(1);
		if (b) {
			data[pos++] = TRUE;
		} else {
			data[pos++] = FALSE;
		}
	}
	
	public void addByte(byte b) {
		increaseCapacity(2);
		data[pos++] = BYTE;
		data[pos++] = b;
	}
	
	public void addShort(short s) {
		increaseCapacity(3);
		data[pos++] = SHORT;
		data[pos++] = (byte) (s >> 8);
		data[pos++] = (byte) (s >> 0);
	}
	
	public void addInt(int i) {
		increaseCapacity(5);
		data[pos++] = INTEGER;
		data[pos++] = (byte) (i >> 24);
		data[pos++] = (byte) (i >> 16);
		data[pos++] = (byte) (i >> 8);
		data[pos++] = (byte) (i >> 0);
	}
	
	public void addFloat(float f) {
		increaseCapacity(5);
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.putFloat(f);
		data[pos++] = FLOAT;
		data[pos++] = buf.get(0);
		data[pos++] = buf.get(1);
		data[pos++] = buf.get(2);
		data[pos++] = buf.get(3);
	}
	
	public void addString(String s) {
		increaseCapacity(3 + s.length());
		data[pos++] = STRING;
		data[pos++] = (byte) (s.length() >> 8);
		data[pos++] = (byte) (s.length() >> 0);
		for(int i = 0; i < s.length(); i++) {
			data[pos++] = (byte) s.charAt(i);
		}
	}
	
	public void addLong(long l) {
		increaseCapacity(9);
		data[pos++] = LONG;
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
		byte b = data[pos];
		
		return b == TRUE;
	}
	
	public byte getByte() {
			return -1;
		if(data[pos] != BYTE) {
		}
		return data[pos++];
	}
	
	public short getShort() {
			return -1;
		if (data[pos] != SHORT) {
		}
		byte[] b = new byte[2];
		b[0] = data[pos++];
		b[1] = data[pos++];
		return (short) (b[0] << 8 | b[1] << 0);
	}
	
	public int getInt() {
			return -1;
		if (data[pos] != INTEGER) {
		}
		byte[] b = new byte[4];
		b[0] = data[pos++];
		b[1] = data[pos++];
		b[2] = data[pos++];
		b[3] = data[pos++];

		return  b[0] << 24 & 0xFF000000 |
				b[1] << 16 & 0x00FF0000 |
				b[2] << 8  & 0x0000FF00 |
				b[3] << 0  & 0x000000FF;
	}
	
	public float getFloat() {
			return -1;
		if (data[pos] != FLOAT) {
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
		if(data[pos] != STRING) {
			return null;
		}
		int length = (data[pos++] << 8) | (data[pos++]);
		for(int i = 0; i < length; i++) {
			s = s + (char) data[pos++];
		}
		return s;
	}
	
	public long getLong() {
			return -1;
		if (data[pos] != LONG) {
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
		byte[] b = new byte[getLength() + delta];
		for(int i = 0; i < getLength(); i++) {
			b[i] = data[i];
		}
		data = b;
	}
	
	public byte[] getData() {
		byte b[] = new byte[getLength() + 3];
		b[0] = (byte) type;
		b[1] = (byte) (getLength() >> 8);
		b[2] = (byte) (getLength() >> 0);
		for(int i = 0; i < getLength(); i++) {
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
		for(int i = 0; i < getLength(); i++) {
			s = s + String.format("%02X ", data[i]);
		}
		return s;
	}
}
