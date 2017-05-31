package dm.kafka.consumer;

//import java.util.HashMap;
import java.util.List;
//import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

public class ConsumerLoop implements Runnable {
	  private final KafkaConsumer<String, String> consumer;
	  private final List<String> topics;
	  SQSProducer msgSnder = new SQSProducer();
		
		

	  public ConsumerLoop(AWSKafkaConfig config) {
		msgSnder.init(config);
	    topics = config.getKafkaTopics();
	    Properties props = new Properties();
	    props.put("bootstrap.servers", config.getKafkaServers());
	    props.put("group.id", config.getKafkaGrpId());
	    props.put("key.deserializer", StringDeserializer.class.getName());
	    props.put("value.deserializer", StringDeserializer.class.getName());
	    this.consumer = new KafkaConsumer<>(props);
	  }
	  @Override
	  public void run() {
	    try {
	      consumer.subscribe(topics);
	      while (true) {
	        ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
	        for (ConsumerRecord<String, String> record : records) {
	          msgSnder.sendMessages(record.value());
	        }
	      }
	    } catch (WakeupException e) {
	      // ignore for shutdown 
	    } finally {
	      consumer.close();
	    }
	  }
	  public void shutdown() {
	    consumer.wakeup();
	  }
	}
