package dm.kafka.consumer;


import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;

public class SQSProducer {
	Session session;
	MessageProducer producer;
	SQSConnection connection;
	AWSKafkaConfig config;
	int seqNum = 1;
	String deDuplicationId = "NA";
	
	public void init(AWSKafkaConfig config){
		
		this.config = config;
		deDuplicationId  = config.getDeDeupPrefix();
		SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
		        new ProviderConfiguration(),
		        AmazonSQSClientBuilder.standard()
		                .withRegion(config.getRegions())
		                .withCredentials(config.getCredentialsProvider())
		        );
		try{ 
			// Create the connection.
			connection = connectionFactory.createConnection();
			
			// Create the queue if needed
	        //ExampleCommon.ensureQueueExists(connection, config.getQueueName());
	            
	        // Create the session
	        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        producer = session.createProducer( session.createQueue( config.getQueueName() ) );
	        
	     // Get the wrapped client
	        AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();

	        // Create an Amazon SQS FIFO queue named TestQueue.fifo, if it does not already exist
	        if (!client.queueExists(config.getQueueName())) {
	            Map<String, String> attributes = new HashMap<String, String>();
	            attributes.put("FifoQueue", "true");
	            attributes.put("ContentBasedDeduplication", "true");
	            client.createQueue(new CreateQueueRequest().withQueueName(config.getQueueName()).withAttributes(attributes));
	        }
		}catch(JMSException e){
		}
        
	}
	public void close(){
		try{
			connection.close();
		}catch(JMSException e){
		}
	}
	
	public void sendMessages( String msg) {
        try {
            TextMessage message = session.createTextMessage(msg);
            message.setStringProperty("JMSXGroupID", "Default");
            message.setStringProperty("JMS_SQS_DeduplicationId", deDuplicationId+(seqNum++));
            producer.send(message);
        } catch (JMSException e) {
            System.err.println( "Failed sending message: " + e.getMessage() );
            e.printStackTrace();
        }
    }

}
