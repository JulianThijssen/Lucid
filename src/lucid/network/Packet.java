package lucid.network;

import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import lucid.exceptions.PacketException;
import lucid.util.Log;
import lucid.util.LogLevel;

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
	
	/**
	 * Type of the packet
	 * Type is a short because normal use of this library
	 * should easily have less than 65535 types of packets.
	 */
	private short type = -1;
	
	/** The address the packet originated from */
	private InetSocketAddress address = null;
	
	/** Position of the mark in the array */
	private int pos = 0;
	
	/** Holds all the data in a packet */
	private byte[] data = null;
	
	
	public Packet(short type) {
		this.type = type;
	}
	
	public short getType() {
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
	
	private void increaseCapacity(int delta) {
		if(data == null) {data = new byte[delta]; return;}
		byte[] b = new byte[getLength() + delta];
		for(int i = 0; i < getLength(); i++) {
			b[i] = data[i];
		}
		data = b;
	}
	
	private void incrementPosition(int offset) {
		pos += offset;
	}
	
	public void addBoolean(boolean b) {
		increaseCapacity(1);
		if (b) {
			data[pos] = TRUE;
		} else {
			data[pos] = FALSE;
		}
		pos += 1;
	}
	
	public void addByte(byte b) {
		increaseCapacity(2);
		data[pos] = BYTE;
		data[pos+1] = b;
		pos += 2;
	}
	
	public void addShort(short s) {
		increaseCapacity(3);
		data[pos] = SHORT;
		data[pos+1] = (byte) (s >> 8);
		data[pos+2] = (byte) (s >> 0);
		pos += 3;
	}
	
	public void addInt(int i) {
		increaseCapacity(5);
		data[pos] = INTEGER;
		data[pos+1] = (byte) (i >> 24);
		data[pos+2] = (byte) (i >> 16);
		data[pos+3] = (byte) (i >> 8);
		data[pos+4] = (byte) (i >> 0);
		pos += 5;
	}
	
	public void addFloat(float f) {
		increaseCapacity(5);
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.putFloat(f);
		data[pos] = FLOAT;
		data[pos+1] = buf.get(0);
		data[pos+2] = buf.get(1);
		data[pos+3] = buf.get(2);
		data[pos+4] = buf.get(3);
		pos += 5;
	}
	
	public void addString(String s) {
		increaseCapacity(3 + s.length());
		data[pos] = STRING;
		data[pos+1] = (byte) (s.length() >> 8);
		data[pos+2] = (byte) (s.length() >> 0);
		for(int i = 0; i < s.length(); i++) {
			data[pos+3+i] = (byte) s.charAt(i);
		}
		pos += 3 + s.length();
	}
	
	public void addLong(long l) {
		increaseCapacity(9);
		data[pos] = LONG;
		data[pos+1] = (byte) (l >> 56);
		data[pos+2] = (byte) (l >> 48);
		data[pos+3] = (byte) (l >> 40);
		data[pos+4] = (byte) (l >> 32);
		data[pos+5] = (byte) (l >> 24);
		data[pos+6] = (byte) (l >> 16);
		data[pos+7] = (byte) (l >> 8);
		data[pos+8] = (byte) (l >> 0);
		pos += 9;
	}
	
	public boolean getBoolean() throws PacketException {
		byte b = data[pos];
		
		if (b != FALSE && b != TRUE) {
			throw new PacketException("Tried to read boolean at position: " + pos);
		}
		incrementPosition(1);
		
		return b == TRUE;
	}
	
	public byte getByte() throws PacketException {
		if(data[pos] != BYTE) {
			throw new PacketException("Tried to read byte at position: " + pos);
		}
		byte b = data[pos+1];
		incrementPosition(2);
		
		return b;
	}
	
	public short getShort() throws PacketException {
		if (data[pos] != SHORT) {
			throw new PacketException("Tried to read short at position: " + pos);
		}
		byte[] b = new byte[2];
		b[0] = data[pos+1];
		b[1] = data[pos+2];
		
		incrementPosition(3);
		return (short) (b[0] << 8  & 0x0000FF00 | b[1] << 0 & 0x000000FF);
	}
	
	public int getInt() throws PacketException {
		if (data[pos] != INTEGER) {
			throw new PacketException("Tried to read int at position: " + pos);
		}
		byte[] b = new byte[4];
		b[0] = data[pos+1];
		b[1] = data[pos+2];
		b[2] = data[pos+3];
		b[3] = data[pos+4];
		
		incrementPosition(5);
		return  b[0] << 24 & 0xFF000000 |
				b[1] << 16 & 0x00FF0000 |
				b[2] << 8  & 0x0000FF00 |
				b[3] << 0  & 0x000000FF;
	}
	
	public float getFloat() throws PacketException {
		if (data[pos] != FLOAT) {
			throw new PacketException("Tried to read float at position: " + pos);
		}
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.put(data[pos+1]);
		buf.put(data[pos+2]);
		buf.put(data[pos+3]);
		buf.put(data[pos+4]);
		buf.flip();
		
		incrementPosition(5);
		return buf.getFloat();
	}
	
	public String getString() throws PacketException {
		String s = "";
		if(data[pos] != STRING) {
			throw new PacketException("Tried to read string at position: " + pos);
		}
		int length = (data[pos+1] << 8) | (data[pos+2]);
		for(int i = 0; i < length; i++) {
			s = s + (char) data[pos+3+i];
		}
		
		incrementPosition(3 + length);
		return s;
	}
	
	public long getLong() throws PacketException {
		if (data[pos] != LONG) {
			throw new PacketException("Tried to read long at position: " + pos);
		}
		
		long l = 0;
		
		int shift = 56;
		for (int i = 0; i < 8; i++) {
			byte b = data[pos+1+i];
			long mask = 0x00000000000000FFL << shift;
			l |= (((long) b) << shift) & mask;
			shift -= 8;
		}
		
		incrementPosition(9);
		return l;
	}
	
	public byte[] getData() {
		byte b[] = new byte[getLength() + 4];
		b[0] = (byte) (type >> 8);
		b[1] = (byte) (type >> 0);
		
		b[2] = (byte) (getLength() >> 8);
		b[3] = (byte) (getLength() >> 0);
		
		for(int i = 0; i < getLength(); i++) {
			b[4 + i] = data[i];
		}
		return b;
	}
	
	public void setData(byte[] b) {
		data = b;
	}
	
	public static Packet fromByteBuffer(ByteBuffer buffer) {
		if (type == -1) {
			short type = buffer.getShort();
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
		ByteBuffer buffer = ByteBuffer.allocate(packet.getLength() + 4);
		byte[] data = packet.getData();
		buffer.put(data);
		buffer.flip();
		
		return buffer;
	}
	
	//Turns the packet into string form
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Type: %04X \n Data: ", type));
		for(int i = 0; i < getLength(); i++) {
			sb.append(String.format("%02X ", data[i]));
		}
		return sb.toString();
	}
}
