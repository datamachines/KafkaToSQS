package dm.kafka.consumer;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class Utils {
	
	public static String encodeB64(String data){
		try {
			return Base64.getEncoder().encodeToString(data.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
			return "";
		}
	}
	public static String encodeB64(byte[] data){
		return Base64.getEncoder().encodeToString(data);
	}
	public static String encodeB64(byte[] data,int total){
		byte[] sub = new byte[total];
		System.arraycopy(data, 0, sub, 0, total);
		return Base64.getEncoder().encodeToString(sub);
	}
	
	public static byte[] decodeB64Bin(String data){
		return Base64.getDecoder().decode(data);
	}
	public static byte[] decodeB64Bin(byte[] data){
		return Base64.getDecoder().decode(data);
	}
	
	public static String decodeB64(String data){
		try {
			return new String(Base64.getDecoder().decode(data), "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
			return "";
		}
		
	}
	public static String decodeB64(byte[] data){
		try {
			return new String(Base64.getDecoder().decode(data), "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
			return "";
		}
	} 
    
}
