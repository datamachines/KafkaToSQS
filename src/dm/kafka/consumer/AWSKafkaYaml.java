package dm.kafka.consumer;
import java.util.Map;


public class AWSKafkaYaml {
	private Map<String, String> awsParms;
	private Map<String, String> kafkaParms;
	private Map<String, String> dataProcessing;
	
	public Map<String, String> getAwsParms() {
		return awsParms;
	}
	public void setAwsParms(Map<String, String> awsParms) {
		this.awsParms = awsParms;
	}
	public Map<String, String> getKafkaParms() {
		return kafkaParms;
	}
	public void setKafkaParms(Map<String, String> kafkaParms) {
		this.kafkaParms = kafkaParms;
	}
	public Map<String, String> getDataProcessing() {
		return dataProcessing;
	}
	public void setDataProcessing(Map<String, String> dataProcessing) {
		this.dataProcessing = dataProcessing;
	}

	
	
}
