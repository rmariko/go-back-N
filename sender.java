import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

public class sender{
	
	////////////////////////// HELPER FUNCTIONS ////////////////////////////////////////
	
	// Send a udp packet 
	public static void udt_send(DatagramSocket senderSocket, packet p, String host, int r_port) throws IOException {
		// Translate host to IP address using DNS
		InetAddress IPAddress = InetAddress.getByName(host);
		
		byte[] sendData = new byte[1024];
		
		// Get bytes from packet data
		sendData = p.getUDPdata();
		
		// Create a datagram with data-to-send, length, IP, and port
		DatagramPacket sendPacket = 
				new DatagramPacket(sendData, sendData.length, IPAddress, r_port);
		
		// Send the datagram to the server
		senderSocket.send(sendPacket);
	}
	
	// Receive an ack packet
	public static packet rdt_rcv(DatagramSocket senderSocket) throws Exception{
		byte[] receiveData = new byte[1024];
		
		// Create a data-to-receive packet
		DatagramPacket receivePacket = 
				new DatagramPacket(receiveData, receiveData.length);
	
		// Read datagram from the server
		senderSocket.receive(receivePacket);
		
		return packet.parseUDPdata(receivePacket.getData());
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
	
	///////////////////////// MAIN FUNCTION //////////////////////////////////////////
	public static void main(String args[]) throws Exception{
		// Command line arguments
		String emulatorHost = args[0];
		int recieverPort = Integer.parseInt(args[1]);
		int senderPort = Integer.parseInt(args[2]);
		String filePath = args[3];
		
		// Variables 
		int window = 10;
		int sendBase = 0;
		int seq = 0;
		int nextSeqNum = 0;
		int ack = 0;
		ArrayList<packet> queue = new ArrayList<packet>();
		
		// Read file data into packets and add them to the queue to be sent
		try {
			StringBuilder data = new StringBuilder();
			FileInputStream fileInput = new FileInputStream(filePath);
			char c;
			while (fileInput.available() > 0){
				if (data.toString().length() != 500){
					c = (char) fileInput.read();
					data.append(c);	
				} else {
					// Create a packet and add it to the queue to send
					packet p = packet.createPacket(seq, data.toString());
					queue.add(p);
										
					// Increase seq # and clear data for next read
					seq++;
					data.setLength(0);
				}
			}
			// Close the file
			fileInput.close();
			
			// I am at the end of my file but have <= totalChars
			if ((data.toString().length() <= 500) && (data.toString().length() > 0)){
				// Create a packet and add it to the queue to send
				packet p = packet.createPacket(seq, data.toString());
				queue.add(p);
				
				// Increase seq # and clear data
				seq++;
				data.setLength(0);
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		///////////////////////// SEND PACKETS //////////////////////////////////////////
		DatagramSocket senderSocket = new DatagramSocket();
		
		// With this option set to a non-zero timeout, a call to receive() for this 
		// DatagramSocket will block for only this amount of time.
		senderSocket.setSoTimeout(1000);
		
		while (true) {
			// Send packet if window is not full
			if(nextSeqNum < (sendBase + window)) {
				System.out.println("NOTFULL");
				
				// Send the packet
				udt_send(senderSocket, queue.get(nextSeqNum), emulatorHost, recieverPort);
				
				// Write to seqnum.log
				writeToFile("seqnum.log", Integer.toString(queue.get(nextSeqNum).getSeqNum()));
				
				// Start timer
				if (sendBase == nextSeqNum) {
					// start timer
				}
				nextSeqNum++;
			}
			
			//// TIMEOUT?
			
			
			// Recieve a packet
			packet receivedPacket = rdt_rcv(senderSocket);
			ack = receivedPacket.getSeqNum();
			
			// Write to seqnum.log
			writeToFile("ack.log", Integer.toString(ack));
			
			sendBase = ack + 1;
			if(sendBase == nextSeqNum) {
				// stop timer
			} else {
				// start timer
			}
			
		}
	}
}