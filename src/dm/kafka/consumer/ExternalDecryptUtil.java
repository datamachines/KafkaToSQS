package dm.kafka.consumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;
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
import org.apache.commons.crypto.utils.Utils;

public class ExternalDecryptUtil {
	
	SecretKeySpec key;
	
	public void init(String cKey){
		key = new SecretKeySpec(getUTF8Bytes(cKey), "AES");
	}

	static public String[] unBin(String queueData){
		return queueData.split(",");
	}
	public String decrypt(String data) {
		 
	       
	        Properties properties = new Properties();
	        //Creates a CryptoCipher instance with the transformation and properties.
	        final String transform = "AES/CBC/PKCS5Padding";


	        String[] orgData = data.split(":");
	        byte[] ivBytes = dm.kafka.consumer.Utils.decodeB64Bin(orgData[0]);
	        byte[] bytes = dm.kafka.consumer.Utils.decodeB64Bin(orgData[1]);
	        


	        IvParameterSpec iv = new IvParameterSpec(ivBytes);
	        // Now reverse the process
	        try (CryptoCipher decipher = Utils.getCipherInstance(transform, properties)) {
	            decipher.init(Cipher.DECRYPT_MODE, key, iv);
	            byte [] decoded = new byte[bytes.length*2];
	            int total = decipher.doFinal(bytes, 0, bytes.length, decoded, 0);
	            byte[] sub = new byte[total];
	            System.arraycopy(decoded, 0, sub, 0, total);
	            decipher.close();
	            return new String(sub, StandardCharsets.UTF_8);
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			}
	        
	        return null;
	}
	
	/**
	 * Converts String to UTF8 bytes
	 *
	 * @param input the input string
	 * @return UTF8 bytes
	 */
	private byte[] getUTF8Bytes(String input) {
	    return input.getBytes(StandardCharsets.UTF_8);
	}
	

	

}
