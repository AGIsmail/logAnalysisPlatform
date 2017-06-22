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
 - [Github link](url=https://github.com/mkobit/docker-nifi)
 - [Docker hub link](url=https://hub.docker.com/r/mkobit/nifi/)

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

References:
 - [Hortonworks Tutorial](url=https://hortonworks.com/hadoop-tutorial/how-to-refine-and-visualize-server-log-data/)
 - [Bryan Bende's blog post: Getting Syslog Events to HBase](url=https://blogs.apache.org/nifi/entry/storing_syslog_events_in_hbase)
 
