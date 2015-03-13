import java.io.*;
import java.net.*;

public class receiver {
	
	///////////////////////// HELPERS ///////////////////////////////////////
	public static void sendAck(DatagramSocket socket, int type, int expSeqNum, InetAddress IPAddress, int port) throws Exception {
		byte[] dataToSend = new byte[1024];
		
		if (type == 0) {
			dataToSend = packet.createACK(expSeqNum).getUDPdata();
		} else if (type == 2) {
			dataToSend = packet.createEOT(expSeqNum).getUDPdata();
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
		int expSeqNum = 0;
		
		////////////////////////////////////////////////////////////////
		// Create a transaction socket for sender's request
		DatagramSocket transactionSocket = new DatagramSocket(receivePort);
		
		int most_recent = -1;
		
		while (true) {
			byte[] dataReceived = new byte[1024];
			
			// Create space for the datagram received 
			DatagramPacket receivePacket =
					new DatagramPacket(dataReceived, dataReceived.length);
			
			// Receive the datagram
			transactionSocket.receive(receivePacket);
			
			packet rp = packet.parseUDPdata(receivePacket.getData());
		
			// Grab the IP address, port # of the sender
			InetAddress IPAddress = InetAddress.getByName(emulatorHost);
			
			// Check sequence number of the packet
			if(rp.getType() == 2) {
				// This is an EOT packet, so send one back
				sendAck(transactionSocket, 2, rp.getSeqNum(), IPAddress, sendPort);
				transactionSocket.close();
				return;
			
			} else if (rp.getType() == 1) {
				// Write to log
				writeToFile("arrival.log", Integer.toString(rp.getSeqNum()));
				
				if (rp.getSeqNum() == expSeqNum) {
					most_recent = rp.getSeqNum();
					// Send ack packet
					sendAck(transactionSocket, 0, rp.getSeqNum(), IPAddress, sendPort);
					
					// Append data to file
					writeToFile(filePath, new String(rp.getData()));
					
					// Increment the expected seq number
					expSeqNum++;
				} else {
					// Send ack for the most recently received in-order packet
					sendAck(transactionSocket, 0, most_recent, IPAddress, sendPort);
				}
			}
		}
	}
}