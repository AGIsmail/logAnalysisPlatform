#!/bin/bash

#Based on https://github.com/wurstmeister/zookeeper-docker/blob/master/Dockerfile

ZOOKEEPER_VERSION=3.4.9

#Install JDK1.8.0_131
wget www.auto.tuwien.ac.at/~aismail/jdk-8u131-linux-x64.tar.gz --no-check-certificate
tar -zxvf jdk-8u131-linux-x64.tar.gz
mv jdk1.8.0_131/ /usr/java/
export PATH=$PATH:/usr/java/jdk1.8.0_131/bin/
echo "export PATH=$PATH:/usr/java/jdk1.8.0_131/bin/">>/etc/profile.d/path.sh

#Download Zookeeper
wget -q http://mirror.vorboss.net/apache/zookeeper/zookeeper-${ZOOKEEPER_VERSION}/zookeeper-${ZOOKEEPER_VERSION}.tar.gz --no-check-certificate && \
wget -q https://www.apache.org/dist/zookeeper/KEYS --no-check-certificate && \
wget -q https://www.apache.org/dist/zookeeper/zookeeper-${ZOOKEEPER_VERSION}/zookeeper-${ZOOKEEPER_VERSION}.tar.gz.asc --no-check-certificate && \
wget -q https://www.apache.org/dist/zookeeper/zookeeper-${ZOOKEEPER_VERSION}/zookeeper-${ZOOKEEPER_VERSION}.tar.gz.md5 --no-check-certificate

#Verify download
#md5sum -c zookeeper-${ZOOKEEPER_VERSION}.tar.gz.md5 && \
#gpg --import KEYS && \
#gpg --verify zookeeper-${ZOOKEEPER_VERSION}.tar.gz.asc

#Install
tar -xzf zookeeper-${ZOOKEEPER_VERSION}.tar.gz -C /opt

#Configure
mv /opt/zookeeper-${ZOOKEEPER_VERSION}/conf/zoo_sample.cfg /opt/zookeeper-${ZOOKEEPER_VERSION}/conf/zoo.cfg

JAVA_HOME=/usr/java/jdk-8u131-linux-x64.tar.gz
ZK_HOME=/opt/zookeeper-${ZOOKEEPER_VERSION}
sed  -i "s|/tmp/zookeeper|$ZK_HOME/data|g" $ZK_HOME/conf/zoo.cfg; mkdir $ZK_HOME/data

echo "# The number of milliseconds of each tick
tickTime=2000
# The number of ticks that the initial
# synchronization phase can take
initLimit=10
# The number of ticks that can pass between
# sending a request and getting an acknowledgement
syncLimit=5
# the directory where the snapshot is stored.
# do not use /tmp for storage, /tmp here is just
# example sakes.
dataDir=/opt/zookeeper-3.4.9/data
# the port at which the clients will connect
clientPort=2181
# the maximum number of client connections.
# increase this if you need to handle more clients
#maxClientCnxns=60
#
# Be sure to read the maintenance section of the
# administrator guide before turning on autopurge.
#
# http://zookeeper.apache.org/doc/current/zookeeperAdmin.html#sc_maintenance
#
# The number of snapshots to retain in dataDir
#autopurge.snapRetainCount=3
# Purge task interval in hours
# Set to 0 to disable auto purge feature
#autopurge.purgeInterval=1
server.1=128.130.56.90:2888:3888
server.1=128.130.56.40:2888:3888
server.1=128.130.56.71:2888:3888" > /opt/zookeeper-3.4.9/conf/zoo.cfg


echo 'Dont forget to create unique IDs under /opt/zookeeper-3.4.9/data/myid'
echo 'i.e.: for server 1'
echo 'echo "1" > /var/zookeeper/myid'

