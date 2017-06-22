# Log Analysis Platform (LAP)

This system receives logs using the syslog protocol for analysis.
It is composed of the following 5 (or 6) services:
 - Apache Nifi: Data ingestion
 - Apache Kafka: MOM
 - Apache Flink: Analysis
 - Apache HBase: Persistence
 - Apache Hadoop HDFS & YARN: Supporting Flink and HBase
 - Apache Phoenix: For Zeppelin to have an SQL interpreter for HBase
 - Apache Zeppelin/Project Jypiter: Notebooks

This project applies the above services using a mixture of docker containers and VMs.

NOTICE: Use at your own risk.

# Apache NiFi
The Apache NiFi service uses the mkobit/nifi docker image:
 - [Github link](https://github.com/mkobit/docker-nifi)
 - [Docker hub link](https://hub.docker.com/r/mkobit/nifi/)

You can pull the image and run it using the commands bellow. 
You will need to expose port 8080 for NiFi's UI and another port (in this example port 20100) for NiFi's syslog listening processor. 
If Nifi does not automatically start then execute the nifi.sh script:
```sh
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
Simply run the installZk.sh bash script on any machine you want to have ZooKeeper on.
Don't forget to install Java first, to update the IP addresses for the ZooKeeper servers in the script, and to follow the echoed notes at the end of the shell script (related to zookeeper server IDs).

Given the number of services that will be using ZooKeeper, it is recommended that you start a quorum (minimum of 3 servers).
# Apache Kafka 
The Apache Kafka service is based on the wurstmeister/kafka image. The github page is [here](https://github.com/wurstmeister/kafka-docker).
This repo modifies wurstmeister's docker compose file to use an external ZooKeeper service instead of the bundled wurstmeister/zookeeper image.

Before running Kafka, update the IP addresses, ports and topics in the docker-compose-single-broker.yml and/or docker-compose.yml files.

Issue this command to docker-compose to run a single broker:
```sh
user@VM# docker-compose -f docker-compose-single-broker.yml up -d
```

Now is a good time to test your services. 
On Nifi: Start the appropriate processors up till the PublishKafka processor
In the Kafka docker:
```sh
user@VM:~/# docker exec -itu 0 kafkadocker_kafka_1 /bin/bash
bash-4.3# ./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic jsonSyslog --from-beginning
{"body":"user: test","version":"","hostname":"test-pc","protocol":"TCP","port":"20100","sender":"/128.130.60.90","id":"14911842000_29cf9c66-fe45-4ff0-b496-b953a64843af","timestamp":"Jun 22 14:10:42"}
```
Next comes the analysis service.

# Apache Hadoop
Download hadoop from a mirror site.
```sh
user@VM:~/ wget http://www-eu.apache.org/dist/hadoop/common/hadoop-2.8.0/hadoop-2.8.0.tar.gz
user@VM:~/ tar -zxvf hadoop-2.8.0.tar.gz 
user@VM:~/ mv hadoop-2.8.0 /etc/hadoop
```
Set the JAVA_HOME variable in hadoop-env.sh (e.g.  "export JAVA_HOME=/usr/java/jdk1.8.0_131"):
```sh
user@VM:~/ cd /etc/hadoop/etc/hadoop && nano hadoop-env.sh 
```

Place the hadoop configuration xml files in /etc/hadoop/etc/hadoop. Update the configurations first as you see fit.

Setup passphraseless ssh:
```sh
user@VM:~/ ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa
user@VM:~/ cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
user@VM:~/ chmod 0600 ~/.ssh/authorized_keys
```

Format the namenode, start the dfs, and start yarn:
```sh
bin/hdfs namenode -format
sbin/start-dfs.sh
sbin/start-yarn.sh
```

# Apache HBase & Apache Phoenix
Download HBase from a mirror site:
```sh
wget http://www-eu.apache.org/dist/hbase/stable/hbase-1.2.6-bin.tar.gz
tar zxvf hbase-1.2.6-bin.tar.gz 
mv hbase-1.2.6 /etc/hbase
```

Place the hbase xml conf files provided in /etc/hbase/conf. Update the configurations first as you see fit.
Set hbase java home
```sh
user@VM:/etc/hbase/conf# nano hbase-env.sh 
export JAVA_HOME=/usr/java/jdk1.8.0_131
```

Set the hbase rootdir to the FQDN of the host in hbase-site.xml
```xml
        <property>
                <name>hbase.rootdir</name>
                <value>hdfs://hbase.site.io/hbase</value>
        </property>
```

If you'll be using Apache Zeppelin, you'll need to install Apache Phoenix.
Download Apache phoenix for your hbase version
```sh
user@VM:/etc/hbase/bin# ./hbase version
HBase 1.2.6
Source code repository file:///home/busbey/projects/hbase/hbase-assembly/target/hbase-1.2.6 revision=Unknown
Compiled by busbey on Mon May 29 02:25:32 CDT 2017
From source with checksum 7e8ce83a648e252758e9dae1fbe779c9
user@VM:~/ wget http://www-eu.apache.org/dist/phoenix/apache-phoenix-4.10.0-HBase-1.2/bin/apache-phoenix-4.10.0-HBase-1.2-bin.tar.gz
```
Copy phoenix files to hbase lib folder:
```sh
user@VM:~/apache-phoenix-4.10.0-HBase-1.2-bin# cp *.jar /etc/hbase/lib/
```
Start hbase
```sh
./hbase master start &
./hbase regionserver start &
```
# Apache Flink
Download Flink:
```sh
wget http://www-us.apache.org/dist/flink/flink-1.3.0/flink-1.3.0-bin-hadoop27-scala_2.11.tgz
$ tar xzf flink-*.tgz   # Unpack the downloaded archive
$ cd flink-0.8.1
```
Configure flink task slots in flink-conf.yaml (e.g., taskmanager.numberOfTaskSlots: 5):
```
user@VM:~/flink-1.3.0/conf$ nano flink-conf.yaml
```
Start flink locally
```sh
$ bin/start-local.sh    # Start Flink locally
```
# Flink Application
The Flink application is based on source code from [here](https://github.com/JosuHM/FlinkConsumerFromKafkaToHbaseWithPoolConnections).
It receives the logs from the Kafka stream and deposits it in an HBase table.

Edit the IP address for ZooKeeper and Kafka, and the topic, etc. in the application src files.
You can compile the application using 
```sh
mvn clean package
``` 
Upload the compiled JAR under the target folder to the Flink UI accessible via port 8081.
Choose "Submit New Job", select the uploaded JAR file, set the Entry Class variable to "com.example.streaming.jobs.FlinkConsumer" and start the application by hitting the green "Submit" button.

# Apache Zeppelin
The Zeppelin service uses [this docker image](https://hub.docker.com/r/epahomov/docker-zeppelin/) (github [link](https://github.com/epahomov/docker-zeppelin)).
```sh
docker pull epahomov/docker-zeppelin
docker run -d -p 8089:8080 -p 7077:7077 -p 4040:4040 epahomov/docker-zeppelin
```

Access the Zeppelin UI via port 8089.
Configure the jdbc phoenix interpreter by setting these three values. Replace the IP for ZooKeeper and the hbase parent node appropriately.
```
phoenix.url 	jdbc:phoenix:128.130.56.90:2181:/hbase
phoenix.user 	phoenixuser
psql.driver 	org.postgresql.Driver 
```

# Jupyter
Nothing special required beyond the required steps under Jupyter's [install instructions](http://jupyter.org/install.html)




