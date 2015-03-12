// common packet class used by both SENDER and RECEIVER

import java.nio.ByteBuffer;

public class packet {
	
	// constants
	private final int maxDataLength = 500;
	private final int SeqNumModulo = 32;
	
	// data members
	private int type;
	private int seqnum;
	private int sent;
	private String data;
	
	//////////////////////// CONSTRUCTORS //////////////////////////////////////////
	
	// hidden constructor to prevent creation of invalid packets
	private packet(int Type, int SeqNum, int Sent, String strData) throws Exception {
		// if data seqment larger than allowed, then throw exception
		if (strData.length() > maxDataLength)
			throw new Exception("data too large (max 500 chars)");
			
		type = Type;
		seqnum = SeqNum % SeqNumModulo;
		sent = Sent;
		data = strData;
	}
	
	// special packet constructors to be used in place of hidden constructor
	public static packet createACK(int SeqNum) throws Exception {
		return new packet(0, SeqNum, 0, new String());
	}
	
	public static packet createPacket(int SeqNum, String data) throws Exception {
		return new packet(1, SeqNum, 0, data);
	}
	
	public static packet createEOT(int SeqNum) throws Exception {
		return new packet(2, SeqNum, 0, new String());
	}
	
	///////////////////////// PACKET DATA //////////////////////////////////////////
	
	public int getType() {
		return type;
	}
	
	public int getSeqNum() {
		return seqnum;
	}
	
	public int getLength() {
		return data.length();
	}
	
	public int sent() {
		return sent;
	}
	
	public byte[] getData() {
		return data.getBytes();
	}
	
	//////////////////////////// UDP HELPERS ///////////////////////////////////////
	
	public byte[] getUDPdata() {
		ByteBuffer buffer = ByteBuffer.allocate(512);
		buffer.putInt(type);
        buffer.putInt(seqnum);
        buffer.putInt(sent);
        buffer.putInt(data.length());
        buffer.put(data.getBytes(),0,data.length());
		return buffer.array();
	}
	
	public static packet parseUDPdata(byte[] UDPdata) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
		int type = buffer.getInt();
		int seqnum = buffer.getInt();
		int sent = buffer.getInt();
		int length = buffer.getInt();
		byte data[] = new byte[length];
		buffer.get(data, 0, length);
		return new packet(type, seqnum, sent, new String(data));
	}
}