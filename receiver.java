import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class receiver {
	
	///////////////////////// HELPERS ///////////////////////////////////////
	public static void sendAck(DatagramSocket socket, int type, int expSeqNum, InetAddress IPAddress, int port) {
		byte[] dataToSend = new byte[1024];
		
		if (type == 0) {
			datatoSend = packet.createACK(expSeqNum).getUDPdata();
		} else if (type == 2) {
			datatoSend = packet.createEOT(expSeqNum).getUDPdata();
		}
		
		// Create a datagram to send to the sender
		DatagramPacket sendPacket =
				new DatagramPacket(dataToSend, dataToSend.length, IPAddress, port);
		
		// Write out the datagram to the socket
		socket.send(sendPacket);
	}	
	
	///// Write to a file /////
	public static void writeToFile(String fileName, String data) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
			out.println(data);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	///////////////////////// MAIN ///////////////////////////////////////
	public static void main(String args[]) throws Exception{
		// Command line arguments
		String emulatorHost = args[0];
		int sendPort = Integer.parseInt(args[1]);
		int receivePort = Integer.parseInt(args[2]);
		String filePath = args[3];
		
		// Variables
		expSeqNum = 0;
		
		////////////////////////////////////////////////////////////////
		// Create a transaction socket for sender's request
		DatagramSocket transactionSocket = new DatagramSocket(0);
		byte[] dataReceived = new byte[1024];
		
		// Create space for the datagram received 
		DatagramPacket receivePacket =
				new DatagramPacket(dataReceived, dataReceived.length);
		
		// Receive the datagram
		transactionSocket.receive(receivePacket);
		
		packet rp = packet.parseUDPdata(receivePacket.getData());
		
		// Grab the IP address, port # of the sender
		InetAddress IPAddress = receivePacket.getAddress();
		int port = receivePacket.getPort();
		
		// Write to log
		writeToFile("arrival.log", Integer.toString(rp.getSeqNum()));
		
		// Check sequence number of the packet
		if(rp.getType() == 2) {
			// This is an EOT packet, so send one back
			sendAck(transactionSocket, 2, expSeqNum, IPAddress, port);
		
		} else if (rp.getType() == 1) {
			// Send ack packet
			sendAck(transactionSocket, 0, expSeqNum, IPAddress, port);
			if (rp.getSeqNum() == expSeqNum) {
				// Append data to file
				writeToFile(filePath, new String(rp.getData()));
				
				// Increment the expected seq number
				expSeqNum++;
			}
		}
	}
}