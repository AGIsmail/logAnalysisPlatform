package com.example.streaming.jobs;


import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer09;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.hadoop.hbase.client.Connection;

import com.example.streaming.jobs.ManagePoolConnections;
import com.example.streaming.jobs.PoolConnectionsFactory;



public class FlinkConsumer {
	
	private static final String columnFamily = "msg";
	
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {      
    
	    // create execution environment
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    
    Properties properties = new Properties();
     properties.setProperty("bootstrap.servers", "128.130.56.90:9092");//“bootstrap.servers” (comma separated list of Kafka brokers)
     properties.setProperty("group.id", "flink_consumer"); //“group.id” the id of the consumer group
     properties.setProperty("auto.offset.reset", "earliest");       // Always read topic from start
   
		// parse user parameters
//		ParameterTool parameterTool = ParameterTool.fromArgs(args);        
	    // create datastream with the data coming from Kafka
        DataStream<String> messageStream = env.addSource(new FlinkKafkaConsumer09<>(
                "jsonSyslog", 
                new SimpleStringSchema(),
                properties));
//                parameterTool.getProperties()));
        
        messageStream.rebalance().map(new MapFunction<String, String>() {
            private static final long serialVersionUID = -6867736771747690202L;
            @Override
            public String map(String message) throws Exception {
            	ManagePoolConnections hb = new ManagePoolConnections(
            			new GenericObjectPool<Connection>(new PoolConnectionsFactory<Object>()));
                hb.writeIntoHBase(columnFamily, "java", message);
                System.out.println("Message written into HBase.");
                return "Kafka dice: " + message;
            }
        }).print();
        // execute enviroment
        try {
            env.execute();
        } catch (Exception ex) {
            Logger.getLogger(FlinkConsumer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
