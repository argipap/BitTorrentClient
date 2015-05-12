package BEncode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class BEncoder {
	
	private static ArrayList<String> piecesList = new ArrayList<String>();
	
    private static void encodeObject(Object o, OutputStream out) throws IOException {
        if (o instanceof String)
            encodeString((String)o, out);
        else if (o instanceof Map)
            encodeMap((Map)o, out);
        else if (o instanceof byte[])
            encodeBytes((byte[])o, out);
        else if (o instanceof Number)
            encodeLong(((Number) o).longValue(), out);
        else
            throw new Error("Unencodable type");
    }
    
    /**returns the pieces */
    public ArrayList getPieces(){
    	return piecesList;
    }
    
    public static String byteArrayToHexString(byte[] b) {
  	  String result = "";
  	  for (int i=0; i < b.length; i++) {
  	    result +=
  	          Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
  	  }
  	  return result;
  	}
    
    private static void encodeLong(long value, OutputStream out) throws IOException {
        out.write('i');
        out.write(Long.toString(value).getBytes("US-ASCII"));
        out.write('e');
    }
    private static void encodeBytes(byte[] bytes, OutputStream out) throws IOException {
        out.write(Integer.toString(bytes.length).getBytes("US-ASCII"));
        out.write(':');
        out.write(bytes);
    }
    private static void encodeString(String str, OutputStream out) throws IOException {
        encodeBytes(str.getBytes("UTF-8"), out);
    }
    private static void encodeMap(Map<String,Object> map, OutputStream out) throws IOException{
        // Sort the map. A generic encoder should sort by key bytes
        SortedMap<String,Object> sortedMap = new TreeMap<String, Object>(map);
        out.write('d');
        for (Entry<String, Object> e : sortedMap.entrySet()) {
            encodeString(e.getKey(), out);
            encodeObject(e.getValue(), out);
        }
        out.write('e');
    }
    private static byte[] hashPieces(File file, int pieceLength) throws IOException {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("SHA1 not supported");
        }
        InputStream in = new FileInputStream(file);
        ByteArrayOutputStream pieces = new ByteArrayOutputStream();
        byte[] bytes = new byte[pieceLength];
        int pieceByteCount  = 0, readCount = in.read(bytes, 0, pieceLength);
        while (readCount != -1) {
            pieceByteCount += readCount;
            sha1.update(bytes, 0, readCount);
            if (pieceByteCount == pieceLength) {
                pieceByteCount = 0;
                pieces.write(sha1.digest());
                sha1.update(bytes, 0, readCount);
                String pcs = byteArrayToHexString(sha1.digest());
                piecesList.add(pcs);
            }
            readCount = in.read(bytes, 0, pieceLength-pieceByteCount);
        }
        in.close();
        if (pieceByteCount > 0){
            pieces.write(sha1.digest());
            String pcs = byteArrayToHexString(sha1.digest());
            piecesList.add(pcs);
        }
        return pieces.toByteArray();
    }
    public static void createTorrent(File file, File sharedFile, String announceURL) throws IOException {
        final int pieceLength = 512*1024;
        Map<String,Object> info = new HashMap<String,Object>();
        info.put("name", sharedFile.getName());
        info.put("length", sharedFile.length());
        info.put("piece length", pieceLength);
        info.put("pieces", hashPieces(sharedFile, pieceLength));
        int count=0;
        Map<String,Object> piecesInfo = new HashMap<String,Object>();
        //System.out.println("sss   "+piecesList.size());
        for (String s : piecesList){
        	piecesInfo.put("piece"+count, s);
        	count++;
        }
        info.put("piecesInfo",piecesInfo);
        Map<String,Object> metainfo = new HashMap<String,Object>();
        metainfo.put("announce", announceURL);
        metainfo.put("info", info);
        OutputStream out = new FileOutputStream(file);
        encodeMap(metainfo, out);
        out.close();
    }

    public static void main(String[] args) throws Exception {
    	String real = "/home/argi/Downloads/Nurses2XXX/Nurses2XXX-CD2.avi";
    	String test = "/home/argi/Dev/BitTorrentClient/NursesXXX.log";
    	String newfile = "/Users/chris/Documents/workspace/BitTorrentClient/vid3.mpeg";
        createTorrent(new File("/Users/chris/Documents/workspace/BitTorrentClient/vid3.torrent"), new File(newfile), "http://127.0.0.1:6666");
    }
}