package dm.kafka.consumer;

public class EncodeBase64 implements DataTransformer {

	@Override
	public String alter(String data) {
		return Utils.encodeB64(data);
	}
	
	@Override
	public void close() {
	}


}
