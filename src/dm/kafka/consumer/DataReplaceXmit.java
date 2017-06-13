package dm.kafka.consumer;

public interface DataReplaceXmit {
	
	public void close();
	
	public void send(String msg);
	
	public void dataTimeOut();

}
