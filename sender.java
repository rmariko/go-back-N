import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

public class sender{
	
	////////////////////////// HELPER FUNCTIONS ////////////////////////////////////////
	
	// Global Variables 
	private static int window = 10;
	private static int sendBase = 0;
	private static int seq = 0;
	private static int nextSeqNum = 0;
	private static int ack = 0;
	private static boolean timerOn = false;
	private static long start = 0;
	private static ArrayList<packet> queue = new ArrayList<packet>();
	
	private static String emulatorHost;
	private static int recieverPort;
	private static DatagramSocket senderSocket;
	
	////////////////////////// Send a udp packet 
	public static void udt_send(packet p) {
		
		// Translate host to IP address using DNS
		InetAddress IPAddress;
		try {
			IPAddress = InetAddress.getByName(emulatorHost);
			
			// Get bytes from packet data
			 byte[] sendData = p.getUDPdata();
			 
			// Create a datagram with data-to-send, length, IP, and port
			DatagramPacket sendPacket = 
					new DatagramPacket(sendData, sendData.length, IPAddress, recieverPort);
			
			// Send the datagram to the server
			try {
				senderSocket.send(sendPacket);
				
			} catch (IOException e) {
				// Do nothing
			}
			
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
	}
	
	////////////////////////// Receive an acknowledgment packet
	public static void rdt_rcv() {
		byte[] receiveData = new byte[1024];
		
		// Create a data-to-receive packet
		DatagramPacket receivePacket = 
				new DatagramPacket(receiveData, receiveData.length);
		
		// Read datagram from the server
		try {
			senderSocket.receive(receivePacket);
			
			// Parse our bytes to a packet
			packet receivedPacket = packet.parseUDPdata(receivePacket.getData());
			
			ack = receivedPacket.getSeqNum();
			
			sendBase = ack + 1;
			if(sendBase == nextSeqNum) {
				// stop timer
				start = 0;
				timerOn = false;
			} else {
				// start timer
				start = startTime();
				timerOn = true;
			}
			
			// Write to ack.log
			writeToFile("ack.log", Integer.toString(ack));
			
		} catch (SocketTimeoutException e) {
			// Do nothing on timeout
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	////////////////////////// Write to a file
	public static void writeToFile(String fileName, String data) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
			out.println(data);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	////////////////////////// Start timer by getting current time
	public static long startTime() {
		return System.currentTimeMillis();
	}
	
	////////////////////////// Check for timeout
	public static void timeout(long current){
		if(start != 0 && timerOn){
			if((current - start) >= 5) {
				// Start timer
				start = startTime();
				timerOn = true;
				
				// Send packets from base - nextseqnum
				for(int i=sendBase; i < nextSeqNum; i++) {
					udt_send(queue.get(i));
					
					// Write to seqnum.log
					writeToFile("seqnum.log", Integer.toString(queue.get(i).getSeqNum()));
				}
			}
		}
	}
	
	///////////////////////// MAIN FUNCTION //////////////////////////////////////////
	public static void main(String args[]) throws Exception{
		// Command line arguments
		emulatorHost = args[0];
		recieverPort = Integer.parseInt(args[1]);
		int senderPort = Integer.parseInt(args[2]);
		String filePath = args[3];
		
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
		 senderSocket = new DatagramSocket(senderPort);
		
		// With this option set to a non-zero timeout, a call to receive() for this 
		// DatagramSocket will block for only this amount of time.
		senderSocket.setSoTimeout(1000);
		
		while (true) {	
			// Check for timeout
			timeout(System.currentTimeMillis());
			
			// Send packet if window is not full
			if(nextSeqNum < queue.size() && nextSeqNum < (sendBase + window)) {
				
				// Send the packet
				udt_send(queue.get(nextSeqNum));
				
				// Write to seqnum.log
				writeToFile("seqnum.log", Integer.toString(queue.get(nextSeqNum).getSeqNum()));
				
				if (sendBase == nextSeqNum) {
					// start timer
					start = startTime();
					timerOn = true;
				}
				nextSeqNum++;
			}
			
			// Try to receive a packet
			rdt_rcv();
				
			// CHECK FOR EOT
			if(sendBase == queue.size()) {
				udt_send(packet.createEOT(nextSeqNum));
				senderSocket.close();
				return;
			}
		}
	}
}