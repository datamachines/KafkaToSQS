# Introduction

![KafkaToSQS](https://raw.githubusercontent.com/datamachines/KafkaToSQS/master/KafkaToSQS.png)

KafkaToSQS facilitates mirroring an internal Kafka topic to AWS SQS where it can be safely consumed by third parties or accessed by globally distributed Internet of Things or similar architectures.   

[Apache Kafka](https://kafka.apache.org/) is an open-source stream processing platform developed by the Apache Software Foundation written in Scala and Java. The project aims to provide a unified, high-throughput, low-latency platform for handling real-time data feeds.

[Amazon Simple Queue Service (SQS)](https://aws.amazon.com/sqs/) is a proprietary cloud based fully managed message queuing service used to decouple and scale microservices, distributed systems, and serverless applications. SQS makes it possible to decouple and coordinate the components of a cloud application. SQS standard queues offer maximum throughput, best-effort ordering, and at-least-once delivery. SQS FIFO queues are designed to guarantee that messages are processed exactly once, in the exact order that they are sent, with limited throughput.

Building applications from individual components that each perform a discrete function improves scalability and reliability, and is best practice design for modern applications. Kafka is used widely as a central component to stream data processing architectures. However, it is insufficient when collaborating with external organizations or sharing data across the wider Internet. Kafka has no mechanism for restricting access, providing service guarantees, or protecting itself from intentional or unintentional misuse which can cause downtime. It is also difficult to provide and manage access to kafka systems across network perimeter boundaries due to it's distributed nature. SQS is a pay-for-what-you-use publicly hosted message queue offered as an Amazon Web Service which has access controls and provides a widely accessible point for streaming data collaboration. KafkaToSQS facilitates mirroring a Kafka topic to SQS where it can be consumed by third parties or accessed safely by globally distributed Internet of Things or similar architectures.   

# Build Project
```
mvn clean install -U
mvn package
```

# Usage
```
cd  ./target --
java -jar kafkaConsumer-0.0.1-SNAPSHOT-jar-with-dependencies.jar ../config.yaml
```



## config.yaml Parameters
```
awsParms:
  queue: The name of the AWS queue that will accept data from our app
  region: The AWS region that the AWS queue was created in
  credentials: file with AWS key and secret for accessing account that queue was created in (credential.properties.template is a format  example)
  dedupPrefix: String use by AWS message framework to prevent duplicate messages (necessary for FIFO queue).  A default "NA" prefix is used if one is not supplied.  This string should be different if more than one instance of this app is used simultaneously with the same AWS Queue
  
kafkaParms:
  bootstrap.servers: comma separated list of IP:PORT of Kafka brokers
  group.id: group id for Kafka consumer - any string will work
  topics: comma separated list of Kafka topic(s) that app will listen for 
  
dataProcessing:
  binSize: Range[1 - 256] Optional parameter that sets the aggregate message size to an AWS queue if binBase64 or binAES is set to true. Parameters has units of KB.  The default is 1024 bytes if this is not set/present.
  binTime: Range[1....N] Optional parameter that sets the maximum time in minutes that bin/aggregated messages are store before it is transmitted to the AWS Queue.  The time starts counting down the first time a message is bin/aggregated.  Parameter has units of minutes.  The default is 10 minutes if this is not set/present.
  binBase64: Base64 encode message and aggregate messages before transmission to an AWS queue. The messages will be aggregated until the aggregate message size is equal to value in binSize or appending the current message will cause the aggregate message to be larger than binSize. 
  binAES: AES encrypted messages and aggregate them for transmission to AWS queue. The aggregation is the same as in binBase64.
  AES: AES encrypted messages before transmitting data to an AWS queue
  AESPW: AES encryption key - needs to be 16,24 or 32 characters - This parameter needs to be set if binAES or AES is set to true
  binZip: Each message is a zip entry in a zip binary. The zip binary is sent to the AWS Queue in base64 encoding.
```
### config.yaml  Parameter constraints and notes
Only one of the following parameter may be set to true at the same time(all are can be false): 
	- binBase64
	- binAES
	- AES
	- binZip

binSize has a max value of 256.  This parameters has units of 1KB

### Example configurations
#### Example 1
```
awsParms:
  queue: stdQueue
  region: us-east-2
  credentials: ../credential.properties
  dedupPrefix: sor
kafkaParms:
  bootstrap.servers: localhost:9092
  group.id: testGrp1
  topics: test1,test2,test3
dataProcessing:
  outTest: false
  binSize: 256
  binTime:  60
  binZip: true
  binBase64: false
  binAES: false
  AES: false
  AESPW: 1234567890123456
```
#### Example 2
```
awsParms:
  queue: data.fifo
  region: us-east-1
  credentials: ../credential.properties
kafkaParms:
  bootstrap.servers: localhost:9092,10.1.22.21:9092,10.1.22.26:9097,10.1.22.125:9092
  group.id: testGrp1
  topics: test1,test2,test3
dataProcessing:
  outTest: false
  binSize: 100
  binTime:  60
  binZip: true
  binBase64: false
  binAES: true
  AES: false
  AESPW: 1234567890123456
```

## Base64 Encoding and Aggregations
Commas are use to separate different Kafka messages when --base64-bin (base64 encoding and aggregation) option is set for transmitting data to an AWS Queue.

### Original Messages
Message 1 = "MESSAGE1"

Message 2 = "message2"

### Base64
Message 1 = "TUVTU0FHRTE="

Message 2 = "bWVzc2FnZTI="

### Transmitted Message to AWS Queue
Transmitted Message Body to AWS Queue = "TUVTU0FHRTE=,bWVzc2FnZTI="

## AES encoding
```
output of AES encryption: = Base64 of IV : Base64 of cipher text  (A:B)
IV = 16 bytes (random value) which will then be base64 encoded = A
original message, AES encrypted with password(16,24 or 32 characters) and IV.  The encrypted message is then base64 encoded = B
```

### code support for decryption 
```
dm.kafka.consumer.ExternalDecryptUtil method
1. static public String[] unBin(String queueData)
	- This method will take any bin/aggregated queue message and break them into individual message and return them in an String array. Use the decrypt method to decrypt each message in the return array.
2. String decrypt(String data)
	- Decrypts the String (format  A:B - A and B are Base64 encoded.  A is IV and B is encrypted/cipher text).  This method takes the encoding that is done by the encrypted encoding done by this application and decodes it.
	- Use the init(cipherkey) method to set the AES encryption key before using decrypt
```

## BinZip encoding
```
Each kafka message is store as a zip entry in binary object/file.  
The  binary zip object is base64 encoded and then transmitted to the
AWS Queue.

```


## Issue:
### libcrypto.so
Can't find libcrypto.so

### fix
sudo apt-get install libssl-dev

