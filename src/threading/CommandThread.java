package threading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import BEncode.BDecoder;
import Nodes.Tracker;
import Nodes.UserNode;

public class CommandThread implements Runnable {

	Messaging messaging;
	//HashMap<String, String> peers;
	//HashMap<String,String> interested;

	public CommandThread(String nodeId, String ip, int port) {
		new Thread(this, "Command Thread").start();

		messaging = new Messaging(nodeId, ip, port);
	}

	public void run() {
		try {
			System.out.println("CommandThread is starting...");

			boolean done = false;
			BufferedReader inputReader = new BufferedReader(
					new InputStreamReader(System.in));
			String inputData;
			Thread.sleep(1000);
			System.out.println("CommandThread started.");
			while (done == false) {
				try {
					System.out.print(">");
					inputData = inputReader.readLine();
					if (inputData.equals("exit")) {
						System.out
								.println("Node is exiting. Disconnected from system...");
						System.out.println("...");
						System.out.println("Disconnected. Bye :)");

						System.exit(0);
					} else if (inputData.equalsIgnoreCase("trackerhelp")) {
						showTrackerHelp();

					}  else if (inputData.equalsIgnoreCase("nodehelp")) {
						showNodeHelp();

					} else if (inputData.equalsIgnoreCase("sendMessage")) {
						messaging.sendMessage("127.0.0.1", 6666, "TEST",
								"eisai malakasss!");
					} else if (inputData.equalsIgnoreCase("showPeers")) {
						System.out.println("================================");
						System.out.println("Number of peers: "+ UserNode.peers.size());
						System.out.println("----------------------------");
						System.out.println("Number of seeders: "+UserNode.seeders);
						System.out.println("Number of leechers: "+UserNode.leechers);
						System.out.println("----------------------------");
						System.out.println("Unchoked peers: "+UserNode.unchokedPeers.get());
						System.out.println("----------------------------");
						Set set = UserNode.peers.entrySet();
						Iterator i = set.iterator();
						while (i.hasNext()) {
							Map.Entry me = (Map.Entry) i.next();
							System.out.print(me.getKey() + "  ");
							System.out.println(me.getValue()  + "  " + UserNode.connected.get(me.getKey()));
							System.out.println("Client is:" + UserNode.interested.get(me.getKey())+ "  " + UserNode.choked.get(me.getKey()));
							System.out.println("Peer is:" + UserNode.interestedPeers.get(me.getKey())+ "  " + UserNode.chokedPeers.get(me.getKey()));
							System.out.println("----------------------------");
						}
					} else if (inputData.equalsIgnoreCase("showInterested")) {
						//do nothing
					}else if (inputData.equalsIgnoreCase("showPiecesHM")){
						Set set = UserNode.piecesHM.entrySet();
						Iterator i = set.iterator();
						while (i.hasNext()) {
							Map.Entry me = (Map.Entry) i.next();
							System.out.print(me.getKey() + "  ");
							System.out.println(me.getValue());
						}
					}else if(inputData.equalsIgnoreCase("combine")){
						UserNode.combineParts();
						
					}else if (inputData.equalsIgnoreCase("showPiecesNumbers")){
						Set set = UserNode.piecesNumbers.entrySet();
						Iterator i = set.iterator();
						while (i.hasNext()) {
							Map.Entry me = (Map.Entry) i.next();
							System.out.print(me.getKey() + "  ");
							System.out.println(me.getValue());
						}
					}else if (inputData.equalsIgnoreCase("showTrackerState")){
						Set set = Tracker.files.entrySet();
						Iterator i = set.iterator();
						System.out.println("===============================");
						while (i.hasNext()) {
							Map.Entry me = (Map.Entry) i.next();
							System.out.println(me.getKey() + "\n------------------------");
							
							System.out.println(" Seeders: "+Tracker.seeders.get(me.getKey()));
							System.out.println(" Leechers: "+Tracker.leechers.get(me.getKey()));
							System.out.println(me.getValue());
							System.out.println("\n");
						}
						System.out.println("===============================");
					}else if (inputData.equalsIgnoreCase("showPeersPiecesHM")){
						Set set = UserNode.peersPiecesHM.entrySet();
						Iterator i = set.iterator();
						while (i.hasNext()) {
							Map.Entry me = (Map.Entry) i.next();
							System.out.print(me.getKey() + "  ");
							System.out.println(me.getValue());
						}
						
					}
					else {
						System.out
								.println("Wrong input. Type help for available commands. And apphelp for application's avaialabel commands.");
					}

				} catch (IOException e) {
					System.out.println("Error reading keyboard input.");
				}

			}

		} catch (Exception e2) {
			System.err.println("Error in command thread.");
			e2.printStackTrace();
		}
	}

	private void showTrackerHelp() {
		System.out.println("Available tracker commands are:");
		System.out.println("\tshowTrackerState");
		System.out.println("\trackerhelp");
		System.out.println("\texit");
	}
	private void showNodeHelp() {
		System.out.println("Available node commands are:");
		System.out.println("\tshowPiecesHM");
		System.out.println("\tshowPiecesNumbers");
		System.out.println("\tcombine");
		System.out.println("\tnodehelp");
		System.out.println("\texit");
	}

}