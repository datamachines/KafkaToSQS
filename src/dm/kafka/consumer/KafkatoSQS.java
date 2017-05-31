package dm.kafka.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KafkatoSQS {

	public static void main(String[] args) {
		AWSKafkaConfig config = AWSKafkaConfig.parseConfig("java -jar package.jar dm.kafka.consumer.KafkatoSQS",args); 

		ExecutorService executor = Executors.newFixedThreadPool(1);
		final List<ConsumerLoop> consumers = new ArrayList<>();

		ConsumerLoop consumer = new ConsumerLoop(config);
		consumers.add(consumer);
		executor.submit(consumer);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				for (ConsumerLoop consumer : consumers) {
					consumer.shutdown();
				}
				executor.shutdown();
				try {
					executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
				}
			}
		});
	}

}
