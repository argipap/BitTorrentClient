package Nodes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import BEncode.BDecoder;

import threading.CommandThread;
import threading.Messaging;
import threading.RecThread;

public class UserNode {

	private static String nodeId;
	public static String onoma;
	public static int seeders = 0;
	public static int leechers = 0;
	public static String nodeState;
	Random random = new Random();
	public static ArrayList <String> lastUnchoked = new ArrayList<String>();
	public static final AtomicInteger unchokedPeers = new AtomicInteger();
	public static HashMap <String, Timer> alives = new HashMap<String, Timer>();
	public static HashMap<String, String> peers = new HashMap<String, String>();
	public static HashMap<String, String> connected = new HashMap<String, String>();
	public static HashMap<String, String> interested = new HashMap<String, String>();
	public static HashMap<String, String> interestedPeers = new HashMap<String, String>();
	public static HashMap<String, String> choked = new HashMap<String, String>();
	public static HashMap<String, String> chokedPeers = new HashMap<String, String>();
	public static HashMap<String, HashMap<String,Integer>> peersPiecesHM = new HashMap<String, HashMap<String,Integer>>();
	private static ArrayList<String> piecesList = new ArrayList<String>();
	public static HashMap<String, Integer> piecesHM = new HashMap<String, Integer>();
	public static HashMap<Integer, String> piecesNumbers = new HashMap<Integer, String>();

	public String ip;
	public int port;

	private int minimizeKeys = 6;
	private String exchangeFile;
	public static String announce;
	private static String filename;
	private static String fileSize;
	private static int fileSizeINT;
	public static int pieceLengthINT;
	private String pathFakelou;
	private ArrayList<String> chunks = new ArrayList<String>();

	// **************************************************************************************init

	/** for user node */
	public UserNode(String iparg, int portarg) {
		ip = iparg;
		port = portarg;
		System.out.println("ip: " + ip + " port: " + port);
		nodeId = ip + port;
		nodeId = getKeyOfObject(nodeId);
		System.out.println("CLIENT ID IS: "+nodeId);
		File fakelos = new File("../"+nodeId);
		pathFakelou = "../"+nodeId+"/";
		if (!fakelos.exists()){
			fakelos.mkdirs();
		}
		
		/** start the receiving Thread */
		new RecThread(portarg, nodeId, ip, port);
		
		initNode();// for parsing torrent file
		// split for taking the ip from the announce message
		String existip = announce.split("//")[1].split(":")[0];
		// split for taking the port from the announce message
		String existport = announce.split("//")[1].split(":")[1];
		//connect to tracker
		
		
		connectToTracker(iparg, portarg, existip, Integer.parseInt(existport));
	}

	private void initFirstNode(String iparg, int portarg) {
		
		
		

		/** start the command line Thread */
		new CommandThread(nodeId, ip, port);
		

	}

	/** connect node to tracker - sends a JOIN message */
	private void connectToTracker(String iparg, int portarg, String existiparg,
			int existportarg) {

		initFirstNode(iparg, portarg);


	}
	


	private void informTracker(String what) {

			sendMessage(announce.split("//")[1].split(":")[0],
					Integer.parseInt(announce.split("//")[1].split(":")[1]),
					"ANNOUNCE", getFilename() + ":"+what+":" + nodeId + ":" + ip + ":"
							+ port);
			//final String what2 = what;
			Timer reannouncer = new Timer();
			reannouncer.schedule(new TimerTask() {

	            @Override
	            public void run() {
	                System.out.println("Reannouncing every 30 seconds.");
	                sendMessage(announce.split("//")[1].split(":")[0],
	    					Integer.parseInt(announce.split("//")[1].split(":")[1]),
	    					"ANNOUNCE", getFilename() + ":"+nodeState+":" + nodeId + ":" + ip + ":"
	    							+ port);
	            }
	        }, 30000, 30000);
			
	}
	
	
	
	private void keepAlives() {
		
		System.out.println("KeepAliver start!");
		Timer keepAliver = new Timer();
		keepAliver.schedule(new TimerTask() {

            @Override
            public void run() {
            	
            	Set set = connected.entrySet();
				Iterator i = set.iterator();
				while (i.hasNext()) {
					Map.Entry me = (Map.Entry) i.next();
					//System.out.println(me.getKey()+"   "+me.getValue());
					if (me.getValue().equals("active")){
						//System.out.println("[*]Connecting with "+me.getKey());
						
						sendMessage(peers.get(me.getKey()).split(":")[0],
		    					Integer.parseInt(peers.get(me.getKey()).split(":")[1]),
		    					"KEEP-ALIVE", getFilename() + ":" + nodeId + ":" + ip + ":"
		    							+ port);
						sendMessage(peers.get(me.getKey()).split(":")[0],
								Integer.parseInt(peers.get(me.getKey()).split(":")[1]),
								"BITFIELD-REPLY", getFilename() + ":" + nodeId + ":" + ip + ":"
										+ port +":"+ Messaging.HMtoSTRING2(piecesHM));
						
					}
				}
                
            }
        }, 15000, 15000);
	}
	
	

	/** connect with all peers who have the file */
	private void connectWithPeers() {
		
		System.out.println("Connector start!");
		Timer connector = new Timer();
		connector.schedule(new TimerTask() {

            @Override
            public void run() {
            	
            	Set set = connected.entrySet();
				Iterator i = set.iterator();
				while (i.hasNext()) {
					Map.Entry me = (Map.Entry) i.next();
					//System.out.println(me.getKey()+"   "+me.getValue());
					if (me.getValue().equals("inactive")){
						System.out.println("[*]Connecting with "+me.getKey());
						
						sendMessage(peers.get(me.getKey()).split(":")[0],
		    					Integer.parseInt(peers.get(me.getKey()).split(":")[1]),
		    					"HANDSHAKE", getFilename() + ":" + nodeId + ":" + ip + ":"
		    							+ port);
						
					}
				}
                
            }
        }, 5000, 5000);
	}
	
	
	//RANDOM UNCHOKING IMPLEMENTATION- 2 RANDOMS EVERY TIME
private void unchokePeers() {
		
		System.out.println("Unchoker start!");
		Timer unchoker = new Timer();
		unchoker.schedule(new TimerTask() {

            @Override
            public void run() {
            	
            		
            	
            	if (chokedPeers.size()>0){
            	ArrayList<String> keys      = new ArrayList<String>(chokedPeers.keySet());
            	String       randomKey = keys.get( random.nextInt(keys.size()) );
            	String       value     = chokedPeers.get(randomKey);
            	
            		
            		final int unchokeHelper = unchokedPeers.get();
            		if (unchokeHelper < 2 ){
            			if (value.equals("choked") && connected.get(randomKey).equals("active")){
            				System.out.println("[*]Unchoking peer "+randomKey);
            				
            				sendMessage(peers.get(randomKey).split(":")[0],
		    					Integer.parseInt(peers.get(randomKey).split(":")[1]),
		    					"UNCHOKE", getFilename() + ":" + nodeId + ":" + ip + ":"
		    							+ port);
            				chokedPeers.put((String) randomKey,"unchoked");
            				lastUnchoked.add(randomKey);
            				unchokedPeers.incrementAndGet();
            			}else if (value.equals("unchoked")){
            				
            			}
                    }else{
                    	//System.out.println("=== randomKey->"+randomKey+"    lastUnchoked->"+lastUnchoked.get(0));
                    	
                    	
                    	
                    	if (!lastUnchoked.contains(randomKey)){
                    		
                    	System.out.println("[*]Max unchocking capacity reached. Chocking peer "+lastUnchoked.get(0));
                    	//AN EXOUME FTASEI STO MEGISTO ARITHMO EPITREPOMENWN UNCHOCKED
                    	//BGAZOUME TON PALAIOTERO UNCHOCKED KAI VAZOUME ENAN ALLO TUXAIO
                    
                    	sendMessage(peers.get(lastUnchoked.get(0)).split(":")[0],
		    					Integer.parseInt(peers.get(lastUnchoked.get(0)).split(":")[1]),
		    					"CHOKE", getFilename() + ":" + nodeId + ":" + ip + ":"
		    							+ port);
                    	chokedPeers.put((String) lastUnchoked.get(0),"choked");
                    	unchokedPeers.decrementAndGet();
                    	lastUnchoked.remove(0);
                    	
                    	if (value.equals("choked") && connected.get(randomKey).equals("active")){
                    	System.out.println("[*]Unchoking peer "+randomKey);
        				
        				sendMessage(peers.get(randomKey).split(":")[0],
	    					Integer.parseInt(peers.get(randomKey).split(":")[1]),
	    					"UNCHOKE", getFilename() + ":" + nodeId + ":" + ip + ":"
	    							+ port);
        				chokedPeers.put((String) randomKey,"unchoked");
        				lastUnchoked.add(randomKey);
        				unchokedPeers.incrementAndGet();
                    	}
                    	}
                    	
                    }
                
            }
            }
        }, 5000, 5000);
	}



//RANDOM GETPIECES IMPLEMENTATION
private void getRandomPieces() {
	
	System.out.println("Piecegetter start!");
	Timer piecegetter = new Timer();
	piecegetter.schedule(new TimerTask() {

        @Override
        public void run() {
        	
        	//AN O CLIENT EINAI UNCHOKED APO TON PEER KAI EINAI INTRESTED STON PEER STEILE AITHSH
        	//System.out.println("choked size->"+choked.size());
        	if (choked.size()>0){
        		System.out.println("[*]Checking...");
        		Set set = choked.entrySet();
				Iterator i = set.iterator();
				while (i.hasNext()) {
					Map.Entry me = (Map.Entry) i.next();
					//System.out.println(me.getKey()+"   "+me.getValue());
					if (me.getValue().equals("unchoked")){
						if (interested.get(me.getKey()).equals("interested")){
							//pare ena random kommati pou xreiazetai
							ArrayList<String> keys2      = new ArrayList<String>();
							Set set2 = piecesHM.entrySet();
							Iterator i2 = set2.iterator();
							while (i2.hasNext()) {
								Map.Entry me2 = (Map.Entry) i2.next();
								if ((Integer)me2.getValue()!=1 && peersPiecesHM.get(me.getKey()).get(me2.getKey()) == 1){
									keys2.add((String) me2.getKey());
								}
							}
							
							if (keys2.size()>0){
			            	String       randomKey = keys2.get( random.nextInt(keys2.size()) );
			            	int       value     = peersPiecesHM.get(me.getKey()).get(randomKey);
							
			            	System.out.println("[*]Requesting piece "+randomKey+" from peer "+me.getKey());
			            	
			            	final int portforserver = random.nextInt(50000)+10000;
			            	final String randomKey2 = randomKey;
			            	final String nodetarget = (String) me.getKey();
			            	
			            	final Timer sendingTimer = new Timer();
			        		sendingTimer.schedule(new TimerTask() {

			                    @Override
			                    public void run() {
		                    	
			                    	
			                    	
			                       	
					        		sendMessage(peers.get(nodetarget).split(":")[0],
					    					Integer.parseInt(peers.get(nodetarget).split(":")[1]),
					    					"REQUEST", getFilename() + ":" + nodeId + ":" + ip + ":"
					    							+ port + ":" + randomKey2 + ":" + portforserver);
			                    	
			                    	sendingTimer.cancel();
			                        
 
			                    }
			                }, 150, 150);
			            	
			        		try {
			                	File myFile = new File("../"+nodeId+"/parts."+onoma+"/"+randomKey2+".part");
			                	
			                	
			                	ServerSocket server_socket = new ServerSocket(portforserver);

			    				Socket socket = server_socket.accept();
			    				
			                	
			                	FileOutputStream fos = new FileOutputStream(myFile);
			                	BufferedOutputStream out = new BufferedOutputStream(fos);
			                	byte[] buffer = new byte[1024];
			                	int count;
			                	InputStream in = socket.getInputStream();
			                	while((count=in.read(buffer)) >=0){
			                		fos.write(buffer, 0, count);
			                	}
			                	fos.close();
			                	
			                	
			                	
			                	socket.close();
			                	server_socket.close();
			                	
			                	
			                  } catch (Exception e) {
			                	  System.out.println("PROBLEM IN OPENING SOCKET FOR PIECE");
									e.printStackTrace();
								}
	                       
			                
							}
			            	
						}
						
						
					}
				}
        		
            
        	}
        }
    }, 5000, 5000);
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

	/** Initialize node procedure */
	/** 1.Choose file which want to exchange */
	/** 2.Read from the correspodent torrent the data and parse them */
	private void initNode() {
		System.out.println("Please enter the path of the file you want to exchange:");
		Scanner user_input = new Scanner(System.in);
		onoma = user_input.next();
		
		System.out.println("Please enter torrent file name only, no ending:");
		String torrentfilename = user_input.next();

		
			setExchangeFile(pathFakelou+onoma);
	
			BDecoder.print(new File(pathFakelou+torrentfilename+".torrent"),
					new File(pathFakelou+torrentfilename+".log"));
			System.out.println("Path: " + pathFakelou + onoma + " chosen");
			parseTorrent(pathFakelou+torrentfilename+".log");
			
			File fakelos2 = new File("../"+nodeId+"/parts."+onoma);
			String pathFakelou2 = "../"+nodeId+"/parts."+onoma+"/";
			if (!fakelos2.exists()){
				fakelos2.mkdirs();
				System.out.println("O FAKELOS FTIAXTIKE");
			}
			
			if (new File(getExchangeFile()).exists()){
				System.out.println("THIS CLIENT IS SEEDER");
				nodeState = "seeder";
				informTracker(nodeState);
				initStateSeeder();
			}else{
				System.out.println("THIS CLIENT IS LEECHER");
				nodeState = "leecher";
				informTracker(nodeState);
				initStateLeecher();
			}
			connectWithPeers();
			unchokePeers();
			getRandomPieces();
			keepAlives();
		
	}


	private void initStateLeecher() {

		try {
			System.out.println("i am leecher");
			
			
			
			Set set = UserNode.piecesHM.entrySet();
			Iterator i = set.iterator();
			while (i.hasNext()) {
				Map.Entry me = (Map.Entry) i.next();
				File fi = new File("../"+nodeId+"/parts."+onoma+"/"+me.getKey()+".part");
				if (fi.exists()){
					System.out.println("piece " + me.getKey() + " exists");
					piecesHM.put((String) me.getKey(), 1);
				}else{
					piecesHM.put((String) me.getKey(), 0);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** parse decoded log file created from torrent decoding */
	private void initStateSeeder() {
		// TODO - compare the parts of the file with hash values
		System.out
				.println("Please wait for the splitting of the file to chunks...");
		try {
			splitFile(new File(getExchangeFile()));
			ArrayList<String> torrentChunks = getChunks();
			ArrayList<String> fileChunks = getPieces();
			
			System.out.println(getPieces());
			System.out.println(getChunks());
			boolean passed[] =new boolean [torrentChunks.size()];
			int i = 0;
			for (String tpieces : torrentChunks) {
				for (String fpieces : fileChunks) {
					if (tpieces.contentEquals(fpieces)) {
						passed[i] = true;
						break;
					}
					else {
						passed[i] = false;
					}
				}
				i++;
			}
			boolean pass=false;
			
			for (int j = 0;j<passed.length;j++){
				//System.out.println(passed[j]);
				if (passed[j] == false) {
					System.out.println("Hashes don't match");
					break;
				} else {
					pass = true;
				}
			}
			if (pass==true){
				System.out.println("All hashes are correct");
			}else{
				System.out.println("PROBLEM IN THE HASHES");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** parse decoded log file created from torrent decoding */
	private void parseTorrent(String torrentpathLog) {
		BufferedReader br = null;
		ArrayList<String> torrentChunks = new ArrayList<String>();

		try {

			String sCurrentLine;

			br = new BufferedReader(new FileReader(torrentpathLog));

			while ((sCurrentLine = br.readLine()) != null) {
				// parse data

				if (sCurrentLine.contains("announce")) {
					setAnnounce(sCurrentLine.split("=")[1].replace(" ", ""));
				} else if (sCurrentLine.contains("name")) {
					setFilename(sCurrentLine.split("=")[1].replace(" ", ""));
				}else if(sCurrentLine.contains("array")){
					//do nothing
				} else if (sCurrentLine.contains("piece length")) {
					// setFileSize(sCurrentLine.split("=")[1].replace(" ", ""));
					pieceLengthINT = Integer.parseInt(sCurrentLine.split("=")[1].replace(" ", ""));
				}else if (sCurrentLine.contains("length")) {
					System.out.println(sCurrentLine);
					setFileSize(sCurrentLine.split("=")[1].replace(" ", ""));
					fileSizeINT = Integer.parseInt(sCurrentLine.split("=")[1].replace(" ", ""));
				}  else if (sCurrentLine.matches("(.*)piece[0-9]+(.*)")) {
					torrentChunks.add(sCurrentLine.split("=")[1].replace(" ",
							""));
					String helper = sCurrentLine.split("=")[0].replace(" ", "");
					helper = helper.substring(5);
					piecesNumbers.put(Integer.parseInt(helper), sCurrentLine.split("=")[1].replace(" ", ""));
					piecesHM.put(sCurrentLine.split("=")[1].replace(" ", ""), 0);
					setChunks(torrentChunks);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		System.out.println(getAnnounce());
		System.out.println(getFilename());
		System.out.println(getFileSize());
		//System.out.println(getChunks());

	}

	private void splitFile(File sharedFile) throws IOException {
		final int pieceLength = 512 * 1024;
		int count = 0;
		Map<String, Object> piecesInfo = new HashMap<String, Object>();
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("pieces", hashPieces(sharedFile, pieceLength));
		// System.out.println("sss   "+piecesList.size());
		for (String s : piecesList) {
			piecesInfo.put("piece" + count, s);
			count++;
		}
	}

	private static byte[] hashPieces(File file, int pieceLength)
			throws IOException {
		MessageDigest sha1;
		try {
			sha1 = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			throw new Error("SHA1 not supported");
		}
		InputStream in = new FileInputStream(file);
		
		ByteArrayOutputStream pieces = new ByteArrayOutputStream();
		byte[] bytes = new byte[pieceLength];
		byte [] helperbytes = null;
		int pieceByteCount = 0, readCount = in.read(bytes, 0, pieceLength);
		while (readCount != -1) {
			pieceByteCount += readCount;
			sha1.update(bytes, 0, readCount);
			if (pieceByteCount == pieceLength) {
				pieceByteCount = 0;
				pieces.write(sha1.digest());
				sha1.update(bytes, 0, readCount);
				String pcs = byteArrayToHexString(sha1.digest());
				piecesList.add(pcs);
				System.out.println("edw->"+pcs);
				OutputStream out = new FileOutputStream(new File("../"+nodeId+"/parts."+onoma+"/"+pcs+".part"));
				out.write(bytes);
				
				out.close();
				piecesHM.put(pcs, 1);
			}
			if (pieceByteCount>0){
				helperbytes = new byte [pieceByteCount];
				readCount = in.read(helperbytes, 0, pieceByteCount);
				System.out.println("rc= " + readCount);
			}else{
				readCount = in.read(bytes, 0, pieceLength - pieceByteCount);
			}

		}
		in.close();
		if (pieceByteCount > 0) {
			pieces.write(sha1.digest());
			String pcs = byteArrayToHexString(sha1.digest());
			piecesList.add(pcs);
			System.out.println("edw2->"+pcs);
			OutputStream out = new FileOutputStream(new File("../"+nodeId+"/parts."+onoma+"/"+pcs+".part"));
			System.out.println("OOEOEOEOEOEOEOOE");
			System.out.println(helperbytes.length);
			System.out.println(pieceByteCount);
			
			//for (helperbytess){
				
			//}
					
			System.out.println("-------------------------------------");
			out.write(helperbytes);
			out.close();
			piecesHM.put(pcs, 1);
		}
		return pieces.toByteArray();
	}

	public static String byteArrayToHexString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	
	
	
	
	public static void combineParts(){
		
		int flagger = 0;
		Set set = UserNode.piecesHM.entrySet();
		Iterator i = set.iterator();
		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			if ((int)me.getValue()==0){
				flagger = 1;
				break;
			}
		}
		
		if (flagger == 0){
			System.out.println("COMBINING PARTS for "+onoma+"...");
			File arxeio = new File("../"+nodeId+"/"+onoma);
			if (arxeio.exists()){
				System.out.println("FILE ALREADY EXISTS, NO NEED TO COMBINE");
			}else{
				System.out.println("CREATING FILE...");
				
				
				//byte [] helperByte = new byte[pieceLengthINT*piecesNumbers.size()];
				byte [] helperByte = new byte[fileSizeINT];
				System.out.println("MHKOS ARXEIOU -> "+helperByte.length);
				try {
					OutputStream out = new FileOutputStream(new File("../"+nodeId+"/"+onoma));
					
					
					ByteBuffer target = ByteBuffer.wrap(helperByte);
				for (int j =0; j<piecesNumbers.size();j++){
					Path kommati = Paths.get ("../"+nodeId+"/parts."+onoma+"/"+piecesNumbers.get(j)+".part");
					byte [] data2 = Files.readAllBytes(kommati); 
					System.out.println(j + "-> Diavasa tosa bytes-> " + data2.length);
					target.put(data2);
				}
				
				
				out.write(helperByte);
				out.close();
				
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
			}
			
		}else{
			System.out.println("PARTS MISSING, CANNOT COMBINE.");
		}
		
	}
	
	
	
	/** getters & setters */
	public String getExchangeFile() {
		return exchangeFile;
	}

	public void setExchangeFile(String exchangeFile) {
		this.exchangeFile = exchangeFile;
	}

	public String getAnnounce() {
		return announce;
	}

	public void setAnnounce(String announce) {
		this.announce = announce;
	}

	public String getFileSize() {
		return fileSize;
	}

	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}

	public ArrayList<String> getChunks() {
		return chunks;
	}

	public void setChunks(ArrayList<String> chunks) {
		this.chunks = chunks;
	}

	public static String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	/** returns the pieces */
	public ArrayList<String> getPieces() {
		return piecesList;
	}
	
	
	
	
	
	
	

	public static void main(String[] args) {
		System.out.println("User Node is being initialized...");
		try {
			if (args.length == 2) {
				UserNode node = new UserNode(args[0], Integer.parseInt(args[1]));

			} else {
				System.err.println("Arguments are USER's IP, USER's PORT.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Arguments are USER's IP, USER's PORT.");
		}
	}

}