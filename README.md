# Introduction

![KafkaToSQS](https://raw.githubusercontent.com/datamachines/KafkaToSQS/master/KafkaToSQS.png)

KafkaToSQS facilitates mirroring an internal Kafka topic to AWS SQS where it can be safely consumed by third parties or accessed safely by globally distributed Internet of Things or similar architectures.   

[Apache Kafka](https://kafka.apache.org/) is an open-source stream processing platform developed by the Apache Software Foundation written in Scala and Java. The project aims to provide a unified, high-throughput, low-latency platform for handling real-time data feeds.

[Amazon Simple Queue Service (SQS)](https://aws.amazon.com/sqs/) is a proprietary cloud based fully managed message queuing service used to decouple and scale microservices, distributed systems, and serverless applications. SQS makes it possible to decouple and coordinate the components of a cloud application. SQS standard queues offer maximum throughput, best-effort ordering, and at-least-once delivery. SQS FIFO queues are designed to guarantee that messages are processed exactly once, in the exact order that they are sent, with limited throughput.

Building applications from individual components that each perform a discrete function improves scalability and reliability, and is best practice design for modern applications. Kafka is used widely as a central component to stream data processing architectures. However, it is insufficient when collaborating with external organizations or sharing data across the wider Internet. Kafka has no mechanism for restricting access, providing service guarantees, or protecting itself from intentional or unintentional misuse which can cause downtime. It is also difficult to provide and manage access to kafka systems across network perimeter boundaries due to it's distributed nature. SQS is a pay-for-what-you-use publicly hosted message queue offered as an Amazon Web Service which has access controls and provides a widely accessible point for streaming data collaboration. KafkaToSQS facilitates mirroring a Kafka topic to SQS where it can be consumed by third parties or accessed safely by globally distributed Internet of Things or similar architectures.   

# Build Project
```
mvn clean install -U
mvn package
```

# Usage

cd  ./target --
java -jar kafkaConsumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar --queue <queueName> --region <AWS Queue Instance Region> --credentials <file with AWS credential> --bootstrap.servers <IP:Port (1...N, comma separated list)> --group.id <any string ID> --topics  <topic names (1...N, comma separated list)> --dedupPrefix <any string>

## AWS Parameters
--queue       :  The name of the AWS queue that will accept data from our app
--region      :  The AWS region that the AWS queue was created in
--credentials :  file with AWS key and secret for accessing account that queue was created in (credential.properties.template is a format  example) 
--dedupPrefix :  String use by AWS message framework to prevent duplicate messages (necessary for FIFO queue).  A default "NA" prefix is used if one is not supplied.  This string should be different if more than one instance of this app is used simultaneously with the same AWS Queue 

## Kafka Parameters
--bootstrap.servers : comma separated list of IP:PORT of Kafka brokers
--group.id          : group id for Kafka consumer - any string will work
--topics            : comma separated list of Kafka topic(s) that app will listen for

## Examples
java -jar kafkaConsumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar --queue dmTestQueue.fifo --region us-east-2 --credentials ../credential.properties --bootstrap.servers localhost:9092,10.2.10.55:9092,10.2.10.2:9099 --group.id testGrp1 --topics test1,test2,test3 --dedupPrefix fiveByFive

java -jar kafkaConsumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar --queue stdQueue --region us-east-2 --credentials ../credential.properties --bootstrap.servers 10.2.10.2:9099 --group.id testGrp1 --topics DSRA 
