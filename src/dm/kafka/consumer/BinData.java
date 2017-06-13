package dm.kafka.consumer;

public class BinData implements DataReplaceXmit{
	static final int MAX_MSG_BOBY_SIZE = 1024*256 - 100;//reserving 100 characters for
	DataSink sink;
	StringBuilder messageBuffer = new StringBuilder();
	int binSize;
	public BinData(DataSink inf, int aBinSize ){
		sink = inf;
		binSize = aBinSize;
		

		if(binSize>MAX_MSG_BOBY_SIZE){
			binSize = MAX_MSG_BOBY_SIZE;
		}

	}
	
	public void close(){
		synchronized(messageBuffer){
			if(messageBuffer.length()>0){
				sink.xmitData(messageBuffer.toString());
				messageBuffer.setLength(0);//clear buffer
			}
		}
	}
	
	public void send(String msg){
		synchronized(messageBuffer){
			int newMesSize = messageBuffer.length()+msg.length()+2;//2 - 1 for ',' message separator and 1 more charactor for NULL terminated String
			if(newMesSize > binSize){//send out
				if(messageBuffer.length() > 0){
					sink.xmitData(messageBuffer.toString());
					messageBuffer.setLength(0);//clear buffer
				}
				messageBuffer.append(msg);
				if(messageBuffer.length() >= binSize){//Send our message if it is >= max message size
					sink.xmitData(messageBuffer.toString());
					messageBuffer.setLength(0);//clear buffer
				}
			}else if (newMesSize == binSize){
				messageBuffer.append(',');
				messageBuffer.append(msg);
				sink.xmitData(messageBuffer.toString());
				messageBuffer.setLength(0);//clear buffer
			}else{//aggregating messages into 1 big Queue message
				if(messageBuffer.length() > 0){
					messageBuffer.append(',');
				}
				messageBuffer.append(msg);
			}
		}
	}

	@Override
	public void dataTimeOut() {
		// TODO Auto-generated method stub
		synchronized(messageBuffer){
			if(messageBuffer.length()>0){
				sink.xmitData(messageBuffer.toString());
				messageBuffer.setLength(0);//clear buffer
			}
		}
		
	}

}
