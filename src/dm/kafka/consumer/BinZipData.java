package dm.kafka.consumer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BinZipData implements DataReplaceXmit{
	
	
	/*
	 Computation for max message payload to AWS Queue
	 X compress to Y base64 to Z
	 X = all messages that will be sent to AWS QUEUE
	 Y = the zip of X
	 Z = base64 encoding of Y
	 
	 #AMax of 256K (262144 bytes) can be transmitted to an AWS queues 
	 #reserving 100 bytes for attributes in payload
	 Z max = 256*1024 - 100 = 262044//reserving 100 characters for
	 Base 64 encoding changes size ceil(Y/3)*4
	
	 262044*3/4 = 196533
	
	 Y max = 196533
	

	 */
	static final int MAX_MSG_BOBY_SIZE = 196533;
	DataSink sink;
	//StringBuilder messageBuffer = new StringBuilder();
	List<String> messageBuffer = new ArrayList<>();
	int binSize;
	int messageSize = 0;
	OneShotTimer mTimer = null;
	int mBinMsgTimeoutInMinutes = 0;
	public BinZipData(DataSink inf, int aBinSize ,int maxMsgTimeinMinutes){
		sink = inf;
		binSize = aBinSize;
		mBinMsgTimeoutInMinutes = maxMsgTimeinMinutes;

		if(binSize>MAX_MSG_BOBY_SIZE){
			binSize = MAX_MSG_BOBY_SIZE;
		}

	}
	
	public void close(){
		synchronized(messageBuffer){
			if(messageSize>0){
				sendToSink();
			}
		}
	}
	
	public void send(String msg){
		synchronized(messageBuffer){
			if(msg!= null && msg.length()>0){
				int newMesSize = messageSize+msgZipSize(msg);
				if(newMesSize > binSize){//send out
					if(messageSize > 0){
						sendToSink();
					}
					binMessages(msg);
					if(messageSize >= binSize){//Send our message if it is >= max message size
						sendToSink();
					}
				}else if (newMesSize == binSize){
					//messageBuffer.append(',');
					binMessages(msg);
					sendToSink();
				}else{//aggregating messages into 1 big Queue message
					//if(messageBuffer.length() > 0){
					//	messageBuffer.append(',');
					//}
					binMessages(msg);
				}
			}
		}
	}
	
	private String zipData(List<String> msgs){

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {

			/*
			 * File is not on the disk, test.txt indicates only the file name to
			 * be put into the zip
			 */
			int index = 1;
			for(String msg: msgs){
				ZipEntry entry = new ZipEntry(""+index);
	
				zos.putNextEntry(entry);
				zos.write(msg.toString().getBytes());
				zos.closeEntry();
				index++;
			}

			/*
			 * use more Entries to add more files and use closeEntry() to close
			 * each file entry
			 */

		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] out = baos.toByteArray();
		String ret = Utils.encodeB64(out);
		System.out.println("BinZipData:org msg size ="+messageSize+" zip size = "+out.length+" b64 size="+ret.length()+" compression%="+(100*out.length)/messageSize);
		
		return ret;

	}

	@Override
	public void dataTimeOut() {
		// TODO Auto-generated method stub
		synchronized(messageBuffer){
			if(messageSize>0){
				sendToSink();
			}
			mTimer = null;
		}
		
	}
	
	private void binMessages(String msg){
		if(msg!= null && msg.length()>0){
			messageBuffer.add(msg);
			messageSize+=msgZipSize(msg);
			if(mTimer == null){
				mTimer = new OneShotTimer(this,mBinMsgTimeoutInMinutes);
			}
		}
	}
	
	private void sendToSink(){
		if(messageBuffer.size()>0){
			sink.xmitData(zipData(messageBuffer));
			messageBuffer.clear();
			messageSize = 0;
		}
	}
	
	//These numbers were experimentally gotten
	private int msgZipSize(String msg){
		int msgSize = msg.length();
		int zipSig = msgSize +150;
		
		if(msgSize>8000){
			zipSig = (int)Math.ceil(msgSize*0.80);
		}else if(msgSize>3000){
			zipSig = (int)Math.ceil(msgSize*0.85);
		}else if(msgSize>1700){
			zipSig = (int)Math.ceil(msgSize*0.90);
		}else if(msgSize>1000){
			zipSig = (int)Math.ceil(msgSize*0.95);
		}else if(msgSize>900){
			zipSig = msgSize;
		}
		
		return zipSig;
	}

}
