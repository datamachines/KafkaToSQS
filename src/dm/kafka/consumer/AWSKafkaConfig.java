package dm.kafka.consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;


import java.io.File;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public class AWSKafkaConfig {
    public static final String DEFAULT_QUEUE_NAME = "SQSJMSClientExampleQueue";
    
    public static final ArrayList<String> DEFAULT_KAFKA_TOPICS = new ArrayList<>();
    
    public static final Region DEFAULT_REGION = Region.getRegion(Regions.US_EAST_2);
    
    public static final String DEFAULT_NA = "NA";
    
    public static final int MAX_BIN_SIZE = 1024*256;
    
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
    public static void ParamsErrorMessage(String prm, String Option){
    	System.err.println(prm+" is missing from the "+Option+" section in the yaml config files");
    }
    public static AWSKafkaConfig parseYaml(String file){
    	ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    	AWSKafkaYaml cfg = null;
    	boolean requiredParmsInYaml = true;
        try {
        	cfg = mapper.readValue(new File(file), AWSKafkaYaml.class);
            System.out.println(ReflectionToStringBuilder.toString(cfg,ToStringStyle.MULTI_LINE_STYLE));
        	Map<String,String> aws = cfg.getAwsParms();
            Map<String,String> kafka = cfg.getKafkaParms();
            Map<String,String> data = cfg.getDataProcessing();
            
            for(String parm: AWSRequireParms){
            	if(aws.get(parm) == null){
            		requiredParmsInYaml = false;
            		ParamsErrorMessage(parm,"awsParms");
            	}
            }
            for(String parm: KafkaRequireParms){
            	if(kafka.get(parm) == null){
            		requiredParmsInYaml = false;
            		ParamsErrorMessage(parm,"kafkaParms");
            	}
            	
            }
            //check conflicting Parameters combinations
            int numEx = 0;
            StringBuilder partErr = new StringBuilder();
            for(String parm: ExclusiveParms){
            	String test = data.get(parm);
            	if(test!=null && test.equals("true")){
            		if(numEx>0){
            			partErr.append(", ");
            		}
            		partErr.append(parm);
            		numEx++;
            	}
            	
            }
            if(numEx>1){
            	requiredParmsInYaml = false;
    			System.err.println(partErr.toString()+" are mutually exclusive.  Only one of them may be set to true");
            }
            
            
            //Check Optional Parameters combinations
            String xAES = data.get("AES");
            String xBinAes = data.get("binAES");
            if((xAES!=null && xAES.equals("true")) ||
            		(xBinAes!=null && xBinAes.equals("true")) 	){
            	String xAesPw = data.get("AESPW");
            	if(xAesPw==null){
            		requiredParmsInYaml = false;
            		ParamsErrorMessage("AESPW","AES or binAES");
            	}else{
            		String checkKeyRet = AESEncrypt.checkKey(xAesPw);
            		if(checkKeyRet != null){
            			requiredParmsInYaml = false;
            			System.err.println("Error with AES Key: "+checkKeyRet);
            		}
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(requiredParmsInYaml)
        	return  new AWSKafkaConfig(cfg);
        else
        	return null;
    }
    enum DataProcessing{NA,Base64,AES};
    enum XmitReplacement{NA,Bin,BinZip,OutTest};
    
    DataProcessing mProcessor = DataProcessing.NA;
    //boolean mUseBin = false;
    XmitReplacement mXmitReplacement = XmitReplacement.NA;
    String AESPassword = null;
    boolean mDisableXmit = false;
    static String[] AWSRequireParms = {"queue","region","credentials"};
    static String[] KafkaRequireParms = {"bootstrap.servers","topics","group.id"};
    static String[] ExclusiveParms = {"binBase64","AES","binAES","binZip","outTest"};
    private AWSKafkaConfig(AWSKafkaYaml cfg){
    	Map<String,String> aws = cfg.getAwsParms();
        Map<String,String> kafka = cfg.getKafkaParms();
        Map<String,String> data = cfg.getDataProcessing();
        
        
        //Require Parameters
        setQueueName(aws.get("queue"));
        cfgRegion(aws.get("region"));
        setCredentialsProvider( new PropertiesFileCredentialsProvider(aws.get("credentials")) );
        
        kafkaServers = kafka.get("bootstrap.servers");
        kafkaTopics =  Arrays.asList(kafka.get("topics").split(","));
        kafkaGrpId = kafka.get("group.id");
        
        //Optional Parameters
        String depParms = aws.get("dedupPrefix");
        if( depParms != null){
        	queueDedupPrefix = depParms;
        }

        
        String sBin = data.get("binSize");
        if(sBin != null){
        	try{
        		mBinSizeBytes = Integer.valueOf(sBin)*1024;
        	}catch(Throwable e){}
        }
        String sBase64 = data.get("binBase64");
        if(sBase64 != null && sBase64.equals("true")){
        	mProcessor = DataProcessing.Base64;
        	mXmitReplacement = XmitReplacement.Bin;
        }
        String disableXmit = aws.get("disableXmit");
        if(disableXmit!= null && disableXmit.equals("true")){
        	mDisableXmit = true;
        }
        
        String xAES = data.get("AES");
        String xBinAes = data.get("binAES");
        if(  (xAES!=null && xAES.equals("true"))  ||
        		(xBinAes!=null && xBinAes.equals("true"))){
        	String xAesPw = data.get("AESPW");
        	if(xAesPw==null){
        		ParamsErrorMessage("AESPW","AES");
        	}else{
        		AESPassword = xAesPw;
	        	mProcessor = DataProcessing.AES;
        	}
        }
        
        if(xBinAes!=null && xBinAes.equals("true")){
        	mXmitReplacement = XmitReplacement.Bin;
        }
        
        String binZip = data.get("binZip");
        if(binZip!= null && binZip.equals("true")){
        	mXmitReplacement = XmitReplacement.BinZip;
        	mProcessor = DataProcessing.NA;
        }
        
        String outTest = data.get("outTest");
        if(outTest!= null && outTest.equals("true")){
        	mXmitReplacement = XmitReplacement.OutTest;
        	mProcessor = DataProcessing.NA;
        }
        
        String binTime = data.get("binTime");
        if(binTime!= null ){
        	try{
        		mBinTimeInMinutes = Integer.valueOf(binTime);
        	}catch(Throwable e){}
        }
        
    }
    public DataTransformer getPre(){
    	DataTransformer mRet = null;
    	switch(mProcessor){
    	case Base64:
    		mRet = new EncodeBase64();
    		break;
    	case AES:
    		mRet = new AESEncrypt(AESPassword);
    		break;
		default:
			mRet = null;
			break;
    	}
    	return mRet;
    }
    
    public DataReplaceXmit getReplaceXmit(DataSink inf){
    	DataReplaceXmit ret = null;
    	switch(mXmitReplacement){
    	case Bin:
    		ret =  new BinData(inf,mBinSizeBytes);
    		break;
    	case BinZip:
    		ret = new BinZipData(inf,mBinSizeBytes,mBinTimeInMinutes);
    		break;
    	case OutTest:
    		ret = new OutTest(inf,mBinSizeBytes);
    		break;
    	default:
    		ret = null;
    		break;
    	}
    	return ret;
    }
    private void cfgRegion(String regionName){
        try {
        	Regions tmpRegions = Regions.fromName(regionName);
            setRegion(Region.getRegion(tmpRegions));
            setRegions(tmpRegions);
        } catch( IllegalArgumentException e ) {
            throw new IllegalArgumentException( "Unrecognized region " + regionName );  
        }
    }
    private AWSKafkaConfig(String args[]) {
    	if(DEFAULT_KAFKA_TOPICS.size()==0){
    		DEFAULT_KAFKA_TOPICS.add("Test");
    	}
        for( int i = 0; i < args.length; ++i ) {
            String arg = args[i];
            if( arg.equals( "--queue" ) ) {//Done
                setQueueName(getParameter(args, i));
                i++;
            } else if( arg.equals( "--region" ) ) {//Done
                String regionName = getParameter(args, i);
                try {
                	Regions tmpRegions = Regions.fromName(regionName);
                    setRegion(Region.getRegion(tmpRegions));
                    setRegions(tmpRegions);
                } catch( IllegalArgumentException e ) {
                    throw new IllegalArgumentException( "Unrecognized region " + regionName );  
                }
                i++;
            } else if( arg.equals( "--credentials" ) ) {//Done
                String credsFile = getParameter(args, i);
                try {
                    setCredentialsProvider( new PropertiesFileCredentialsProvider(credsFile) );
                } catch (AmazonClientException e) {
                    throw new IllegalArgumentException("Error reading credentials from " + credsFile, e );
                }
                i++;
            } 
            else if( arg.equals( "--base64-bin" ) ) {
            	try{
            		mBinSizeBytes = Integer.valueOf(getParameter(args, i))*1024;
            		if(mBinSizeBytes > MAX_BIN_SIZE){
            			mBinSizeBytes = MAX_BIN_SIZE;
            		}
            		if(mBinSizeBytes < 0){
            			mBinSizeBytes = 0;
            		}
            	}catch(Throwable e){
            		mBinSizeBytes = 0;
            	}
                i++;
            } else if( arg.equals( "--dedupPrefix" ) ) {//Done
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
    
    private int mBinTimeInMinutes = 10;
    private int mBinSizeBytes = 1024;
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
    
    public boolean getAwsXmitDisable(){
    	return mDisableXmit;
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