package dm.kafka.consumer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.crypto.cipher.CryptoCipher;
import org.apache.commons.crypto.cipher.CryptoCipherFactory;
import org.apache.commons.crypto.cipher.CryptoCipherFactory.CipherProvider;
import org.apache.commons.crypto.random.CryptoRandom;
import org.apache.commons.crypto.random.CryptoRandomFactory;
import org.apache.commons.crypto.utils.Utils;

public class AESEncrypt implements DataTransformer {
	
	static final int MIN_OUT_BUFFER_SIZE = 32;
	final String transform = "AES/CBC/PKCS5Padding";
    CryptoCipher encipher;
    SecretKeySpec key;
    IvParameterSpec iv;
    byte[] IVBytes = new byte[16];
    CryptoRandom random;
    
    static String checkKey(String cipherKey){
    	String ret = null;
    	try {
    		SecretKeySpec Testkey = new SecretKeySpec(getUTF8Bytes(cipherKey),"AES");
    		IvParameterSpec TestIv = new IvParameterSpec(getUTF8Bytes("1234567890123456"));
    		Properties properties = new Properties();
	    	properties.setProperty(CryptoCipherFactory.CLASSES_KEY,
	    			CipherProvider.OPENSSL.getClassName());
    		CryptoCipher test = Utils.getCipherInstance("AES/CBC/PKCS5Padding", properties);
    		test.init(Cipher.ENCRYPT_MODE, Testkey, TestIv);
    		test.close();
    	}catch (Throwable e) {
    		 ret = e.getMessage();
    	}
    	return ret;
    }
    
	public AESEncrypt(String cipherKey){
		
	    
	    try {
		    key = new SecretKeySpec(getUTF8Bytes(cipherKey),"AES");
		    Properties properties = new Properties();
	    	properties.setProperty(CryptoCipherFactory.CLASSES_KEY,
	    			CipherProvider.OPENSSL.getClassName());
			encipher = Utils.getCipherInstance(transform, properties);
			
			
			Properties randProperties = new Properties();
			randProperties.put(CryptoRandomFactory.CLASSES_KEY,
	            CryptoRandomFactory.RandomProvider.OPENSSL.getClassName());

	        // Gets the 'CryptoRandom' instance.
	        random = CryptoRandomFactory.getCryptoRandom(randProperties);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private String encrypt(String orgData) {
		byte[] input = getUTF8Bytes(orgData);
		int len = input.length*2;
		if(len <MIN_OUT_BUFFER_SIZE){
			len = MIN_OUT_BUFFER_SIZE;
		}
	    byte[] output = new byte[len];
	    int total  = 0;
		try{
			random.nextBytes(IVBytes);
			iv = new IvParameterSpec(IVBytes);
			
		    //Initializes the cipher with ENCRYPT_MODE, key and iv.
		    encipher.init(Cipher.ENCRYPT_MODE, key, iv);
		    //Continues a multiple-part encryption/decryption operation for byte array.
		    int updateBytes = encipher.update(input, 0, input.length, output, 0);
		    //System.out.println(updateBytes);
		    //We must call doFinal at the end of encryption/decryption.
		    int finalBytes = encipher.doFinal(input, 0, 0, output, updateBytes);
		    //System.out.println(finalBytes);
		    total = updateBytes+finalBytes;
		    
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ShortBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				encipher.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return dm.kafka.consumer.Utils.encodeB64(IVBytes) + ":" + dm.kafka.consumer.Utils.encodeB64(output,total);
	}

	@Override
	public String alter(String data) {
		return encrypt(data);
	}
	
	@Override
	public void close() {
		 try {
			encipher.close();
		} catch (IOException e) {
		}
	}
	
	
	/**
	 * Converts String to UTF8 bytes
	 *
	 * @param input the input string
	 * @return UTF8 bytes
	 */
	static private byte[] getUTF8Bytes(String input) {
	    return input.getBytes(StandardCharsets.UTF_8);
	}
	
}
