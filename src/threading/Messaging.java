package threading;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.sun.org.apache.xml.internal.serializer.utils.Messages;

import Nodes.Tracker;
import Nodes.UserNode;

public class Messaging {

	private String nodeId;
	
	
	

	public String ip;
	public int port;

	public Messaging(String nodeId, String ip, int port) {

		this.nodeId = nodeId;
		this.ip = ip;
		this.port = port;
	}

	public void messageManager(String mes, SocketAddress sa) {
		String[] message = mes.split(":");

		if (message[0].equals("ANNOUNCE")) {
			System.out.println("oeoe");
			if (Tracker.files.get(message[1]) == null){
				HashMap<String, String> helper = new HashMap<String, String>();
				helper.put(message[3], message[4]+":"+message[5]);
				Tracker.files.put(message[1], helper);
				if (message[2].equals("seeder")){
					Tracker.seeders.put(message[1],1);
					Tracker.leechers.put(message[1],0);
				}else{
					Tracker.seeders.put(message[1],0);
					Tracker.leechers.put(message[1],1);
				}
			}else{
				if (Tracker.files.get(message[1]).get(message[3]) == null){		
					if (message[2].equals("seeder")){
						Tracker.seeders.put(message[1],Tracker.seeders.get(message[1])+1);
					}else{
						Tracker.leechers.put(message[1],Tracker.leechers.get(message[1])+1);
					}
					Tracker.files.get(message[1]).put(message[3], message[4]+":"+message[5]);
				}
			}
			Tracker.peers.put(message[3], message[4]+":"+message[5]);
			
			sendMessage(message[4],Integer.parseInt(message[5]),"INFORM",message[1]+":"+Tracker.seeders.get(message[1])+":"+Tracker.leechers.get(message[1])+":"+HMtoSTRING(Tracker.files.get(message[1])));
			
			
		} else if (message[0].equals("INFORM")) {

			UserNode.seeders = Integer.parseInt(message[2]);
			UserNode.leechers = Integer.parseInt(message[3]);
			for (int i=4;i<message.length-1;i=i+3){
				if (!message[i].equals(nodeId)){
					if (UserNode.peers.get(message[i]) == null){
						UserNode.peers.put(message[i], message[i+1]+":"+message[i+2]);
						UserNode.connected.put(message[i], "inactive");
						UserNode.interested.put(message[i], "not-interested");
						UserNode.interestedPeers.put(message[i], "not-interested");
						UserNode.choked.put(message[i], "choked");
						UserNode.chokedPeers.put(message[i], "choked");
					}
				}
			}
			
			
			
		} else if (message[0].equals("HANDSHAKE")) {
			if (UserNode.peers.get(message[2]) == null){
				UserNode.peers.put(message[2], message[3]+":"+message[4]);
				UserNode.connected.put(message[2], "inactive");
				UserNode.interested.put(message[2], "not-interested");
				UserNode.interestedPeers.put(message[2], "not-interested");
				UserNode.choked.put(message[2], "choked");
				UserNode.chokedPeers.put(message[2], "choked");
			}		
			
			//if (UserNode.connected.get(message[2]).equals("inactive")){
				System.out.println("[*]Connecting with "+message[2]);
				sendMessage(message[3],
					Integer.parseInt(message[4]),
					"HANDSHAKE-REPLY", message[1] + ":" + nodeId + ":" + ip + ":"
							+ port);
				UserNode.connected.put(message[2], "active");
				
				Timer helperTimer = new Timer();
				final String target = message[2];
				UserNode.alives.put(message[2], helperTimer);
				UserNode.alives.get(message[2]).schedule(new TimerTask() {

		            @Override
		            public void run() {
		            	
		            	
		            	UserNode.peers.remove(target);
		            	UserNode.interested.remove(target);
		            	UserNode.choked.remove(target);
		            	UserNode.interestedPeers.remove(target);
		            	if (UserNode.chokedPeers.get(target).equals("unchoked")){
		            		UserNode.unchokedPeers.decrementAndGet();
		            	}
		            	UserNode.chokedPeers.remove(target);
		            	UserNode.connected.remove(target);
		            	UserNode.alives.get(target).cancel();
		            	UserNode.alives.remove(target);
		                
		            }
		        }, 31000, 31000);
			
			//}
			

		}  else if (message[0].equals("HANDSHAKE-REPLY")) {
			if (UserNode.connected.get(message[2]).equals("inactive")){
				UserNode.connected.put(message[2], "active");
				
				Timer helperTimer = new Timer();
				final String target = message[2];
				UserNode.alives.put(message[2], helperTimer);
				UserNode.alives.get(message[2]).schedule(new TimerTask() {

		            @Override
		            public void run() {
		            	
		            	
		            	
		            	UserNode.peers.remove(target);
		            	UserNode.interested.remove(target);
		            	UserNode.choked.remove(target);
		            	UserNode.interestedPeers.remove(target);
		            	if (UserNode.chokedPeers.get(target).equals("unchoked")){
		            		UserNode.unchokedPeers.decrementAndGet();
		            	}
		            	UserNode.chokedPeers.remove(target);
		            	UserNode.connected.remove(target);
		            	UserNode.alives.get(target).cancel();
		            	UserNode.alives.remove(target);
		                
		            }
		        }, 31000, 31000);
			}
			System.out.println("[*]Sending bitfield to "+message[2]);
			sendMessage(message[3],
					Integer.parseInt(message[4]),
					"BITFIELD", message[1] + ":" + nodeId + ":" + ip + ":"
							+ port + ":" + HMtoSTRING2(UserNode.piecesHM));
			
		}  else if (message[0].equals("BITFIELD")) {
			System.out.println("[*]Sending bitfield to "+message[2]);
			sendMessage(message[3],
					Integer.parseInt(message[4]),
					"BITFIELD-REPLY", message[1] + ":" + nodeId + ":" + ip + ":"
							+ port +":"+ HMtoSTRING2(UserNode.piecesHM));
			
			HashMap<String, Integer> helper = new HashMap<String, Integer>();
			boolean isInterested = false;
			for (int i=5;i<message.length-1;i=i+2){
				helper.put(message[i], Integer.parseInt(message[i+1]));
				if (Integer.parseInt(message[i+1])==1 && UserNode.piecesHM.get(message[i])==0){
					isInterested = true;
				}
			}
			UserNode.peersPiecesHM.put(message[2], helper);
			if (isInterested == true){
				UserNode.interested.put(message[2], "interested");
				sendMessage(message[3],
						Integer.parseInt(message[4]),
						"INTERESTED", message[1] + ":" + nodeId + ":" + ip + ":"
								+ port);
			}
			
			
		} else if (message[0].equals("BITFIELD-REPLY")) {
			HashMap<String, Integer> helper = new HashMap<String, Integer>();
			boolean isInterested = false;
			for (int i=5;i<message.length-1;i=i+2){
				helper.put(message[i], Integer.parseInt(message[i+1]));
				if (Integer.parseInt(message[i+1])==1 && UserNode.piecesHM.get(message[i])==0){
					isInterested = true;
				}
			}
			UserNode.peersPiecesHM.put(message[2], helper);
			if (isInterested == true){
				UserNode.interested.put(message[2], "interested");
				sendMessage(message[3],
						Integer.parseInt(message[4]),
						"INTERESTED", message[1] + ":" + nodeId + ":" + ip + ":"
								+ port);
			}
			
		}else if (message[0].equals("INTERESTED")) {
			System.out.println("[*] Node "+message[2]+ " is interested.");
			UserNode.interestedPeers.put(message[2], "interested");
		}else if (message[0].equals("NOT-INTERESTED")) {
			System.out.println("[*] Node "+message[2]+ " is not-interested anymore.");
			UserNode.interestedPeers.put(message[2], "not-interested");
		}else if (message[0].equals("CHOKE")) {
			if (UserNode.choked.get(message[2]) != null){
				System.out.println("[*] Node "+message[2]+ " choked me.");
				if (UserNode.choked.get(message[2]).equals("unchoked")){
					UserNode.choked.put(message[2], "choked");
				}
			}
		}else if (message[0].equals("UNCHOKE")) {
			if (UserNode.choked.get(message[2]) != null){
				System.out.println("[*] Node "+message[2]+ " unchoked me.");
				if (UserNode.choked.get(message[2]).equals("choked")){
					UserNode.choked.put(message[2], "unchoked");
				}
			}
		}else if (message[0].equals("REQUEST")) {
			System.out.println("[*] Sending piece "+message[5]+ " to peer " + message[2]);
			try {
				
				
            	File myFile = new File("../"+nodeId+"/parts."+UserNode.onoma+"/"+message[5]+".part");


            	Socket socket = new Socket(message[3], Integer.parseInt(message[6]));
            	
            	
            	int count;
				byte[] buffer = new byte[1024];

				OutputStream out = socket.getOutputStream();
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(myFile));
				while ((count = in.read(buffer)) >= 0) {
				     out.write(buffer, 0, count);
				     out.flush();
				}
            	
            	
				socket.close();
				
				
				
			    UserNode.peersPiecesHM.get(message[2]).put(message[5], 1);
			    sendMessage(message[3],
    					Integer.parseInt(message[4]),
    					"PIECE", UserNode.getFilename() + ":" + nodeId + ":" + ip + ":"
    							+ port + ":"+ message[5]);
				
				
			} catch (Exception e) {
				System.out.println("[X]Failed to send piece!");
				e.printStackTrace();
			} 
			
			
			
		}else if (message[0].equals("PIECE")) {
			System.out.println("[*]Got piece "+message[5]+" from peer "+message[2]+"!");
			UserNode.piecesHM.put(message[5],1);
			Set set = UserNode.connected.entrySet();
			Iterator i = set.iterator();
			while (i.hasNext()) {
				Map.Entry me = (Map.Entry) i.next();
				if (me.getValue().equals("active")){
					sendMessage(UserNode.peers.get(me.getKey()).split(":")[0],
	    					Integer.parseInt(UserNode.peers.get(me.getKey()).split(":")[1]),
	    					"HAVE", UserNode.getFilename() + ":" + nodeId + ":" + ip + ":"
	    							+ port + ":"+ message[5]);
				}
			}
			
			boolean areAll = true;
			Set set2 = UserNode.piecesHM.entrySet();
			Iterator i2 = set2.iterator();
			while (i2.hasNext()) {
				Map.Entry me2 = (Map.Entry) i2.next();
				if ((Integer)me2.getValue()==0){
					areAll = false;
				}
			}
			
			if (areAll == true){
				UserNode.combineParts();
				UserNode.nodeState = "seeder";
				sendMessage(UserNode.announce.split("//")[1].split(":")[0],
    					Integer.parseInt(UserNode.announce.split("//")[1].split(":")[1]),
    					"REMOVE-LEECHER", message[1] + ":"+"seeder"+":" + nodeId + ":" + ip + ":"
    							+ port);
				Set set3 = UserNode.interested.entrySet();
				Iterator i3 = set3.iterator();
				while (i3.hasNext()) {
					Map.Entry me3 = (Map.Entry) i3.next();
					UserNode.interested.put((String) me3.getKey(), "not-interested");
					sendMessage(UserNode.peers.get(me3.getKey()).split(":")[0],
							Integer.parseInt(UserNode.peers.get(me3.getKey()).split(":")[1]),
							"NOT-INTERESTED", message[1] + ":" + nodeId + ":" + ip + ":"
									+ port);
				}
			}
			
			
			
		}else if (message[0].equals("HAVE")) {
			System.out.println("[*]Got HAVE "+message[5]+" from peer "+message[2]+"!");
			UserNode.peersPiecesHM.get(message[2]).put(message[5],1);
			if (UserNode.piecesHM.get(message[5]) == 0){
				if (UserNode.interested.get(message[2]).equals("not-interested")){
					UserNode.interested.put(message[2], "interested");
					sendMessage(message[3],
							Integer.parseInt(message[4]),
							"INTERESTED", message[1] + ":" + nodeId + ":" + ip + ":"
									+ port);
				}
			}
			
		}else if (message[0].equals("REMOVE-LEECHER")) {
			Tracker.leechers.put(message[1],Tracker.leechers.get(message[1])-1);
			Tracker.seeders.put(message[1],Tracker.seeders.get(message[1])+1);
			
		}else if (message[0].equals("KEEP-ALIVE")) {
			
			if (UserNode.alives.get(message[2])!= null){
				System.out.println("[*]Canceling old timer for peer "+message[2]+". Starting again.");
				UserNode.alives.get(message[2]).cancel();
				UserNode.alives.remove(message[2]);
				
			Timer helperTimer = new Timer();
			final String target = message[2];
			UserNode.alives.put(message[2], helperTimer);
			UserNode.alives.get(message[2]).schedule(new TimerTask() {

	            @Override
	            public void run() {
	            	
	            	
	            	
	            	UserNode.peers.remove(target);
	            	UserNode.interested.remove(target);
	            	UserNode.choked.remove(target);
	            	UserNode.interestedPeers.remove(target);
	            	if (UserNode.chokedPeers.get(target).equals("unchoked")){
	            		UserNode.unchokedPeers.decrementAndGet();
	            	}
	            	UserNode.chokedPeers.remove(target);
	            	UserNode.connected.remove(target);
	            	UserNode.alives.get(target).cancel();
	            	UserNode.alives.remove(target);
	                
	            }
	        }, 31000, 31000);
			
			
			
			
			
			}
		}

	}
	

	private String getNodeAddressString(String nid) {
		String[] helper = UserNode.peers.get(nid).split(":");
		System.out.println(helper[0]);
		return helper[0];
	}

	private Integer getNodePort(String nid) {
		String[] helper = UserNode.peers.get(nid).split(":");
		return Integer.parseInt(helper[1]);
	}

	public String HMtoSTRING(HashMap <String, String> hm){
		String helper="";
		
		Set set = hm.entrySet();
		Iterator i = set.iterator();
		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			helper = helper + me.getKey() + ":" + me.getValue()+":";
		}
		
		return helper;
	}
	
	
	public static String HMtoSTRING2(HashMap <String, Integer> hm){
		String helper="";
		
		Set set = hm.entrySet();
		Iterator i = set.iterator();
		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			helper = helper + me.getKey() + ":" + me.getValue()+":";
		}
		
		return helper;
	}
	
	
	
	
	public void sendMessage(String iparg, int portarg, String header,
			String content) {

		Socket soc = null;
		DataOutputStream dos = null;

		try {
			soc = new Socket();
			InetSocketAddress isa = new InetSocketAddress(iparg, portarg);
			System.out.println(isa);
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
			e.printStackTrace();
		}

	}

}