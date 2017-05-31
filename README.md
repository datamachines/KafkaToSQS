Build Project:

mvn clean install -U
mvn package

Usage:

cd  ./target --
java -jar kafkaConsumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar --queue <queueName> --region <AWS Queue Instance Region> --credentials <file with AWS credential> --bootstrap.servers <IP:Port (1...N, comma separated list)> --group.id <any string ID> --topics  <topic names (1...N, comma separated list)> --dedupPrefix <any string>

AWS Parameters:
--queue       :  The name of the AWS queue that will accept data from our app
--region      :  The AWS region that the AWS queue was created in
--credentials :  file with AWS key and secret for accessing account that queue was created in (credential.properties.template is a format  example) 
--dedupPrefix :  String use by AWS message framework to prevent duplicate messages (necessary for FIFO queue).  A default "NA" prefix is used if one is not supplied.  This string should be different if more than one instance of this app is used simultaneously with the same AWS Queue 

Kafka Parameters:
--bootstrap.servers : comma separated list of IP:PORT of Kafka brokers
--group.id          : group id for Kafka consumer - any string will work
--topics            : comma separated list of Kafka topic(s) that app will listen for

examples:
java -jar kafkaConsumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar --queue dmTestQueue.fifo --region us-east-2 --credentials ../credential.properties --bootstrap.servers localhost:9092,10.2.10.55:9092,10.2.10.2:9099 --group.id testGrp1 --topics test1,test2,test3 --dedupPrefix fiveByFive

java -jar kafkaConsumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar --queue stdQueue --region us-east-2 --credentials ../credential.properties --bootstrap.servers 10.2.10.2:9099 --group.id testGrp1 --topics DSRA 