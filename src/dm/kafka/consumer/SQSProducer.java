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

public class SQSProducer implements DataSink{
	 
	Session session;
	MessageProducer producer;
	SQSConnection connection;
	AWSKafkaConfig config;
	int seqNum = 1;
	String deDuplicationId = "NA";
	
	//StringBuilder messageBuffer = new StringBuilder();
	DataTransformer mPreProcessor = null;
	DataReplaceXmit mXmitReplacement = null;
	//BinData binObj = null;
	boolean mXmitDisable = false;
	public void init(AWSKafkaConfig config){
		
		this.config = config;
		deDuplicationId  = config.getDeDeupPrefix();
		
		mXmitDisable = config.getAwsXmitDisable();
		
		SQSConnectionFactory connectionFactory = new SQSConnectionFactory(
		        new ProviderConfiguration(),
		        AmazonSQSClientBuilder.standard()
		                .withRegion(config.getRegions())
		                .withCredentials(config.getCredentialsProvider())
		        );
		try{ 
			
			mPreProcessor = config.getPre();
			mXmitReplacement = config.getReplaceXmit(this);
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
		//ceanup, transmit any remaining data
		try{
			if(mPreProcessor!= null){
				mPreProcessor.close();
			}
			if(mXmitReplacement != null){
				mXmitReplacement.close();
			}
			connection.close();
		}catch(JMSException e){
		}
		
		System.out.println("Exit called on SQSProducer");
	}
	
	public void sendMessages( String orgMsg) {
		String msg = orgMsg;
		if(mPreProcessor!= null){
			msg = mPreProcessor.alter(orgMsg);
		}
		
		
    	if(mXmitReplacement == null){
    		xmitData(msg);
    	}else{
    		mXmitReplacement.send(msg);
    	}  
    }

	
	@Override
	public void xmitData(String data) {
		try {
			if(mXmitDisable == false){
				TextMessage message = session.createTextMessage(data);
		        message.setStringProperty("JMSXGroupID", "Default");
		        message.setStringProperty("JMS_SQS_DeduplicationId", deDuplicationId+(seqNum++));
		        producer.send(message);
			}
		} catch (JMSException e) {
	        System.err.println( "Failed sending message: " + e.getMessage() );
	        e.printStackTrace();
	    }
		
	}

}
