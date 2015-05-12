package Nodes;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;

import threading.CommandThread;
import threading.RecThread;

public class Tracker{

	private String nodeId;
	public static HashMap<String, String> peers = new HashMap<String, String>();
	public static HashMap<String, HashMap<String,String>> files = new HashMap<String, HashMap<String,String>>();
	public static HashMap<String, Integer> seeders = new HashMap<String, Integer>();
	public static HashMap<String, Integer> leechers = new HashMap<String, Integer>();
	private HashMap<String, String> interested = new HashMap<String, String>();

	public String ip;
	public int port;

	private int minimizeKeys = 6;

	// **************************************************************************************init

	/** for tracker */
	public Tracker(String iparg, int portarg) {
		initFirstNode(iparg, portarg);
	}

//	/** for user node */
//	public Tracker(String iparg, int portarg, String existiparg, int existportarg) {
//		connectToSwarm(iparg, portarg, existiparg, existportarg);
//	}

	private void initFirstNode(String iparg, int portarg) {
		ip = iparg;
		port = portarg;
		System.out.println("ip: " + ip + " port: " + port);
		nodeId = ip + port;
		nodeId = getKeyOfObject(nodeId);

		/** start the receiving Thread */
		new RecThread(portarg, nodeId, ip, port);

		/** start the command line Thread */
		new CommandThread(nodeId, ip, port);

	}



	private void initExchangeFile(String fileUrl) {
		// TODO - ask the user to enter url of the file which want to exchange
		// TODO - read from the corresponding torrent file size, ip address of
		// tracker and hash values of pieces

	}

	private void informTracker() {
		// TODO - connect to the tracker and inform him about that he wants the
		// specific file
		// TODO - Tracker informs the node for the list of other nodes that
		// exchange the specific file
	}

	private void initState() {
		
	}

	// **************************************************************************send
	// messages

	public void sendMessage(String iparg, int portarg, String header,
			String content) {

		Socket soc = null;
		DataOutputStream dos = null;

		try {
			soc = new Socket();
			InetSocketAddress isa = new InetSocketAddress(iparg, portarg);

			soc.connect(isa, 0);
			dos = new DataOutputStream(new BufferedOutputStream(
					soc.getOutputStream()));
			String l = header + ":" + content;
			dos.writeUTF(l);
			dos.flush();

			if (dos != null)
				dos.close();
			if (soc != null)
				soc.close();
		} catch (Exception e) {
			System.err.println("Error in sending message to remote node.");
		}

	}

	// ***************************************************************************hashing
	// with sha1

	public String getKeyOfObject(String keyy) {
		try {
			System.out.println("Generating nodeId with SHA1...");
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			keyy = calculateHash(sha1, keyy);
		} catch (Exception e) {
			System.err.println("Error in generating nodeId.");
		}
		System.out.println("Getting " + minimizeKeys * 8
				+ "bit out of SHA1 160bit...");
		keyy = keyy
				.substring(0, keyy.length() - (keyy.length() - minimizeKeys));
		return keyy;
	}

	public String getKeyOfObjectNoVerbose(String keyy) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			keyy = calculateHash(sha1, keyy);
		} catch (Exception e) {
			System.err.println("Error in generating nodeId.");
		}
		keyy = keyy
				.substring(0, keyy.length() - (keyy.length() - minimizeKeys));

		return keyy;
	}

	private String calculateHash(MessageDigest algorithm, String message)
			throws Exception {

		algorithm.update(message.getBytes());

		byte[] hash = algorithm.digest();

		return byteArray2Hex(hash);
	}

	private static String byteArray2Hex(byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}
	
	
	
	
	
	
	public static void main (String [] args){
		System.out.println("Tracker is being initialized...");
		try{
			if (args.length == 2){
				Tracker tracker = new Tracker(args[0],Integer.parseInt(args[1]));
				
			}
//			else if (args.length == 4){
//				Tracker tracker = new Tracker(args[0],Integer.parseInt(args[1]), args[2],Integer.parseInt(args[3]));	
//			}
			else{
				System.err.println("Arguments are IP,and TRACKER's PORT.");
				//System.err.println("Arguments are IP, TRACKER's PORT, EXISTING IP, EXISTING PORT for other nodes that are connected to the tracker.");
			}
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Arguments are IP,and TRACKER's PORT.");
			//System.err.println("Arguments are IP, PORT, DIRECTORY PATH, TRACKER's PORT, EXISTING IP, EXISTING PORT for other nodes that are connected to the tracker.");
		}
	}
	
	
}