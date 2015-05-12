package threading;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class RecThread implements Runnable {
	Thread s;
	int sport;
	Messaging messaging;


	public RecThread(int sp,String nodeId,String ip,int port) {
		sport = sp;
		new Thread(this, "Receiving Thread").start();
		messaging =  new Messaging(nodeId, ip, port);
		// s.start();
	}

	public void run() {
		try {
			System.out.println("RecThread is starting...");
			ServerSocket server = null;
			Socket socket = null;
			DataInputStream dis = null;
			try {
				server = new ServerSocket(sport);
			} catch (Exception e) {
				System.err.println("Error in creating receiving socket.");
			}
			server.setSoTimeout(0);
			System.out.println("RecThread started.");

			while (true) {
				socket = server.accept();
				dis = new DataInputStream(new BufferedInputStream(
						socket.getInputStream()));
				try {
					String line = dis.readUTF();
					 String[] message = line.split(":");
					 if (message[0].equals("CHOKE") ||
					 message[0].equals("UNCHOKE") 
					|| message[0].equals("INFORM") 
					|| message[0].equals("BITFIELD") 
					|| message[0].equals("BITFIELD-REPLY") 
					|| message[0].equals("REQUEST") 
					|| message[0].equals("PIECE") 
					|| message[0].equals("HAVE")
					|| message[0].equals("REMOVE-LEECHER") 
					
							 ){
						 //do nothing
					 }else{
						 System.out.println("\n*Got message " + line + " from node "
									+ socket.getInetAddress().toString() + ":"
									+ socket.getPort());
					 }
					messaging.messageManager(line,socket.getRemoteSocketAddress());

				} catch (IOException ioe) {
					System.err.println("Error in receiving data.");
				} catch (Exception e3) {
					System.err.println("Malformed message received.");
					e3.printStackTrace();
				}
			}

		} catch (Exception e2) {
			System.err.println("Error in receiving.");
		}
	}
}