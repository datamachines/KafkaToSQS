package dm.kafka.consumer;

public class OutTest implements DataReplaceXmit{
	static final int MAX_MSG_BOBY_SIZE = 1024*256 - 100;//reserving 100 characters for
	DataSink sink;
	StringBuilder messageBuffer = new StringBuilder();
	int binSize;
	public OutTest(DataSink inf, int aBinSize ){
		sink = inf;
		binSize = aBinSize;
		

		if(binSize>MAX_MSG_BOBY_SIZE){
			binSize = MAX_MSG_BOBY_SIZE;
		}

	}
	
	public void close(){
	}
	
	public void send(String msg){
		System.out.println(msg);
	}

	@Override
	public void dataTimeOut() {
		// TODO Auto-generated method stub
		
	}

}
