import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

public class sender{
	
	////////////////////////// HELPER FUNCTIONS ////////////////////////////////////////
	
	////////////////////////// Send a udp packet 
	public static void udt_send(DatagramSocket senderSocket, packet p, String host, int r_port) throws IOException {
		
		// Translate host to IP address using DNS
		InetAddress IPAddress = InetAddress.getByName(host);
		
		// Get bytes from packet data
		 byte[] sendData = p.getUDPdata();
		 
		// Create a datagram with data-to-send, length, IP, and port
		DatagramPacket sendPacket = 
				new DatagramPacket(sendData, sendData.length, IPAddress, r_port);
		
		// Send the datagram to the server
		senderSocket.send(sendPacket);
	}
	
	////////////////////////// Receive an acknowledgment packet
	static String special = "0xDEADBEEF";
	public static byte[] rdt_rcv(DatagramSocket senderSocket) throws Exception{
		byte[] receiveData = new byte[1024];
		
		// I want to pad the beginning of my original receiving packet with a random
		// string. Since I am not blocking, I need another way to check if I am 
		// actually receiving a valid packet from the reciever.
		for(int i=0; i < special.getBytes().length; i++){
			receiveData[i] = special.getBytes()[i];
		}
		
		// Create a data-to-receive packet
		DatagramPacket receivePacket = 
				new DatagramPacket(receiveData, receiveData.length);
		
		// Read datagram from the server
		try {
			senderSocket.receive(receivePacket);
		} catch (SocketTimeoutException e) {
			// TODO Auto-generated catch block
		}
		
		return receivePacket.getData();
	}
	
	////////////////////////// Check if a received acknowledgment is valid
	public static boolean valid_ack(byte[] b){
		String valid = new String(b);
		valid = valid.substring(0, 10);
		if(valid.equals(special)) {
			return false;
		} else {
			return true;
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
	public static boolean timeout(long start, long current){
		if((current - start) >= 5) {
			return true;
		} else {
			return false;
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
		DatagramSocket senderSocket = new DatagramSocket(senderPort);
		
		// With this option set to a non-zero timeout, a call to receive() for this 
		// DatagramSocket will block for only this amount of time.
		senderSocket.setSoTimeout(1000);
		
		// Initialize timer variables
		boolean timerOn = false;
		long start = 0;
		
		while (true) {	
			// Check for timeout
			if(start != 0 && timerOn && timeout(start, System.currentTimeMillis())) {
				
				// Start timer
				start = startTime();
				timerOn = true;
				
				// Send packets from base - nextseqnum
				for(int i=sendBase; i < nextSeqNum; i++) {
					udt_send(senderSocket, queue.get(i), emulatorHost, recieverPort);
					
					// Write to seqnum.log
					writeToFile("seqnum.log", Integer.toString(queue.get(i).getSeqNum()));
				}
			}
			
			// Send packet if window is not full
			if(nextSeqNum < queue.size() && nextSeqNum < (sendBase + window)) {
				
				// Send the packet
				udt_send(senderSocket, queue.get(nextSeqNum), emulatorHost, recieverPort);
				
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
			byte[] acknowledgment = rdt_rcv(senderSocket);
			
			// Because we are not blocking here, we need to have a way to actually check if we have received a proper
			// acknowledgment.
			if(valid_ack(acknowledgment) == true){
			
				// Parse our bytes to a packet
				packet receivedPacket = packet.parseUDPdata(acknowledgment);
				
				ack = receivedPacket.getSeqNum();
							
				if(ack+1 == nextSeqNum) {
					sendBase = ack + 1;
					// stop timer
					start = 0;
					timerOn = false;
				}
				
				// Write to ack.log
				writeToFile("ack.log", Integer.toString(ack));
			}
				
			// CHECK FOR EOT
			if(sendBase == queue.size()) {
				udt_send(senderSocket, packet.createEOT(nextSeqNum), emulatorHost, recieverPort);
				senderSocket.close();
				return;
			}
		}
	}
}