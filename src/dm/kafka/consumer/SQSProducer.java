package dm.kafka.consumer;


import java.io.UnsupportedEncodingException;
import java.util.Base64;
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
	static final int MAX_MSG_BOBY_SIZE = 1024*256 - 100;//reserving 100 characters for 
	Session session;
	MessageProducer producer;
	SQSConnection connection;
	AWSKafkaConfig config;
	int seqNum = 1;
	String deDuplicationId = "NA";
	int binSize = 0;
	StringBuilder messageBuffer = new StringBuilder();
	public void init(AWSKafkaConfig config){
		
		this.config = config;
		deDuplicationId  = config.getDeDeupPrefix();
		binSize = config.getBinSize();
		if(binSize>MAX_MSG_BOBY_SIZE){
			binSize = MAX_MSG_BOBY_SIZE;
		}
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

	        // Create an Amazon SQS queue if it does not already exist
	        if (!client.queueExists(config.getQueueName())) {
	            Map<String, String> attributes = new HashMap<String, String>();
	            attributes.put(config.getQueueName(), "true");
	            attributes.put("ContentBasedDeduplication", "true");
	            client.createQueue(new CreateQueueRequest().withQueueName(config.getQueueName()).withAttributes(attributes));
	        }
		}catch(JMSException e){
		}
        
	}
	public void close(){
		try{
			SendRemainingMsg();
			connection.close();
		}catch(JMSException e){
		}
	}
	
	public void sendMessages( String msg) {
    	if(binSize < 1024){//minimum bin size is 1K - don't aggregate if anything less
    		sendSQSMsg(msg);
    	}else{
    		sendBatchMsg(msg);
    	}  
    }
	private void SendRemainingMsg(){
		if(messageBuffer.length()>0){
			sendSQSMsg(messageBuffer.toString());
			messageBuffer.setLength(0);//clear buffer
		}
	}
	private void sendBatchMsg(String rawmsg){
		String msg;
		try {
			msg = Base64.getEncoder().encodeToString(rawmsg.getBytes("utf-8"));
		
			int newMesSize = messageBuffer.length()+msg.length()+2;//2 - 1 for ',' message separator and 1 more charactor for NULL terminated String
			if(newMesSize > binSize){//send out
				if(messageBuffer.length() > 0){
					sendSQSMsg(messageBuffer.toString());
					messageBuffer.setLength(0);//clear buffer
				}
				messageBuffer.append(msg);
				if(messageBuffer.length() >= binSize){//Send our message if it is >= max message size
					sendSQSMsg(messageBuffer.toString());
					messageBuffer.setLength(0);//clear buffer
				}
			}else if (newMesSize == binSize){
				messageBuffer.append(',');
				messageBuffer.append(msg);
				sendSQSMsg(messageBuffer.toString());
				messageBuffer.setLength(0);//clear buffer
			}else{//aggregating messages into 1 big Queue message
				if(messageBuffer.length() > 0){
					messageBuffer.append(',');
				}
				messageBuffer.append(msg);
			}
		
		} catch (UnsupportedEncodingException e) {
			System.err.println( "Failed to econde message: " + e.getMessage() );
		}
	}
	
	private void sendSQSMsg(String msg){
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
