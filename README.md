# Log Analysis Platform (LAP)

This system receives logs using the syslog protocol for analysis.
It is composed of the following 5 (or 6) services:
 - Apache Nifi: Data ingestion
 - Apache Kafka: MOM
 - Apache Flink: Analysis
 - Apache HBase: Persistence
 - Apache Zeppelin/Project Jypiter: Notebooks

This project applies the above services using a mixture of docker containers and VMs.
# Apache NiFi
The Apache NiFi service uses the mkobit/nifi docker image:
 - [Github link](https://github.com/mkobit/docker-nifi)
 - [Docker hub link](https://hub.docker.com/r/mkobit/nifi/)

You can pull the image and run it using the commands bellow. 
You will need to expose port 8080 for NiFi's UI and another port (in this example port 20100) for NiFi's syslog listening processor. 
If Nifi does not automatically start then execute the nifi.sh script:
```
user@VM:~/ docker pull mkobit/nifi
user@VM:~/ docker run -it -p 8080-8081:8080-8081 -p 20100:20100 mkobit/nifi /bin/bash 
nifi@3261f89ad489:/opt/nifi/bin$ ./nifi.sh start
```
Once you can access the NiFi UI, upload the nifiTemplate.xml file to Nifi.
The template contains:
 - Log Generator and Data Enrichment process groups: These process groups create example syslog messages.
 - ListenSyslog processor: Configured to listen for TCP syslog messages on port 20100.
 - UpdateAttribute processor: Creates a row id for HBase and renames attributes
 - AttributesToJSON processor: Creates a JSON doc from the attributes 
 - PutHBaseJSON processor: Persists the JSON doc in HBase
 - PublishKafka processor: Publishes the JSON doc as a message on Kafka
 
The PutHbaseJSON processor is optional and will need a HBase client controller configured to point to HBase's ZooKeeper quorum to function.

You will need to update the processors with the appropriate IP's and port numbers. e.g., the PublishKafka processor's broker IP and port setting.

References:
 - [Hortonworks Tutorial](https://hortonworks.com/hadoop-tutorial/how-to-refine-and-visualize-server-log-data/)
 - [Bryan Bende's blog post: Getting Syslog Events to HBase](https://blogs.apache.org/nifi/entry/storing_syslog_events_in_hbase)

# Apache ZooKeeper

Since several services from here on out will depend on a functioning ZooKeeper ensemble, it is a good idea to setup ZooKeeper now before proceeding with the other services.



# Apache Kafka 
The Apache Kafka service is based on the wurstmeister/kafka image. The github page is [here](https://github.com/wurstmeister/kafka-docker).
This repo modifies wurstmeister's docker compose file to use an external ZooKeeper service instead of the bundled wurstmeister/zookeeper image.

Before running Kafka, update the IP addresses, ports and topics in the docker-compose-single-broker.yml and/or docker-compose.yml files.

Issue this command to docker-compose to run a single broker:
```
user@VM# docker-compose -f docker-compose-single-broker.yml up -d
```

Now is a good time to test your 


