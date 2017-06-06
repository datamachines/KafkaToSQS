package dm.kafka.consumer;

public interface DataTransformer {
	
	public String alter(String data);
	

	public void close();
}
