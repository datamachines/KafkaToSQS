package dm.kafka.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class AWSKafkaConfig {
    public static final String DEFAULT_QUEUE_NAME = "SQSJMSClientExampleQueue";
    
    public static final ArrayList<String> DEFAULT_KAFKA_TOPICS = new ArrayList<>();
    
    public static final Region DEFAULT_REGION = Region.getRegion(Regions.US_EAST_2);
    
    public static final String DEFAULT_NA = "NA";
    
    private static String getParameter( String args[], int i ) {
        if( i + 1 >= args.length ) {
            throw new IllegalArgumentException( "Missing parameter for " + args[i] );
        }
        return args[i+1];
    }
    
    /**
     * Parse the command line and return the resulting config. If the config parsing fails
     * print the error and the usage message and then call System.exit
     * 
     * @param app the app to use when printing the usage string
     * @param args the command line arguments
     * @return the parsed config
     */
    public static AWSKafkaConfig parseConfig(String app, String args[]) {
        try {
            return new AWSKafkaConfig(args);
        } catch (IllegalArgumentException e) {
            System.err.println( "ERROR: " + e.getMessage() );
            System.err.println();
            System.err.println( "Usage: " + app + " [--queue <queue>] [--region <region>] [--credentials <credentials>] [--bootstrap.servers <IP:Port>] [--group.id <id>] [--topics <topics>]");
            System.err.println( "Usage: " + app + " --queue dmTestQueue.fifo --region us-east-2 --credentials credential.properties --bootstrap.servers localhost:9092 --group.id testGrp1 --topics test1,test2,test3");
            System.exit(-1);
            return null;
        }
    }
    
    private AWSKafkaConfig(String args[]) {
    	if(DEFAULT_KAFKA_TOPICS.size()==0){
    		DEFAULT_KAFKA_TOPICS.add("Test");
    	}
        for( int i = 0; i < args.length; ++i ) {
            String arg = args[i];
            if( arg.equals( "--queue" ) ) {
                setQueueName(getParameter(args, i));
                i++;
            } else if( arg.equals( "--region" ) ) {
                String regionName = getParameter(args, i);
                try {
                	Regions tmpRegions = Regions.fromName(regionName);
                    setRegion(Region.getRegion(tmpRegions));
                    setRegions(tmpRegions);
                } catch( IllegalArgumentException e ) {
                    throw new IllegalArgumentException( "Unrecognized region " + regionName );  
                }
                i++;
            } else if( arg.equals( "--credentials" ) ) {
                String credsFile = getParameter(args, i);
                try {
                    setCredentialsProvider( new PropertiesFileCredentialsProvider(credsFile) );
                } catch (AmazonClientException e) {
                    throw new IllegalArgumentException("Error reading credentials from " + credsFile, e );
                }
                i++;
            } else if( arg.equals( "--dedupPrefix" ) ) {
            	queueDedupPrefix = getParameter(args, i);
                i++;
            } else if( arg.equals( "--bootstrap.servers" ) ) {
            	kafkaServers = getParameter(args, i);
                i++;
            } else if( arg.equals( "--topics" ) ) {
            	String topicStr = getParameter(args, i);
            	kafkaTopics =  Arrays.asList(topicStr.split(","));
                i++;
            } else if( arg.equals( "--group.id" ) ) {
            	kafkaGrpId = getParameter(args, i);
                i++;
            } else {
                throw new IllegalArgumentException("Unrecognized option " + arg);
            }
        }
    }
    
    private String queueDedupPrefix = DEFAULT_NA;
    private List<String> kafkaTopics = DEFAULT_KAFKA_TOPICS;
    private String kafkaServers = DEFAULT_NA;
    private String kafkaGrpId = DEFAULT_NA;
    private String queueName = DEFAULT_QUEUE_NAME;
    private Region region = DEFAULT_REGION;
    private Regions regions = Regions.US_EAST_2;
    private AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
    
    public String testRst(){
    	return "queue:"+queueName+"  region:"+regions+" bootstrap.servers:"+kafkaServers+" group.id:"+kafkaGrpId;
    }
    
    
    public String getKafkaServers(){
    	return kafkaServers;
    }
    public String getKafkaGrpId(){
    	return kafkaGrpId;
    }
    public List<String> getKafkaTopics(){
    	return kafkaTopics;
    }
    
    public String getQueueName() {
        return queueName;
    }
    
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
    
    public String getDeDeupPrefix(){
    	return queueDedupPrefix;
    }
    
    public Region getRegion() {
        return region;
    }
    
    public void setRegion(Region region) {
        this.region = region;
    }
    
    public Regions getRegions(){ return regions;}
    
    public void setRegions(Regions regions){ this.regions = regions; }
 
    public AWSCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }
    
    public void setCredentialsProvider(AWSCredentialsProvider credentialsProvider) {
        // Make sure they're usable first
        credentialsProvider.getCredentials();
        this.credentialsProvider = credentialsProvider;
    }
}