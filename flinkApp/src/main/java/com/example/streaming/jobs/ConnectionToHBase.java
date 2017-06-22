package com.example.streaming.jobs;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

/**
 * 
 * @author sistemas
 * Configuration Hbase Connection
 */
public class ConnectionToHBase {

    //instancia del patrón singleton
	private static ConnectionToHBase hbaseConexion = new ConnectionToHBase();
	
	//definición de variables
	private static String hbaseZookeeperQuorum="128.130.56.90,128.130.56.40,128.130.56.71";
    private static String hbaseZookeeperClientPort="2181";
    private static String zookeeper_znode_parent="/hbase";

    //creamos el constructor vacío del patrón singleton
    private ConnectionToHBase(){ }
    
    public static ConnectionToHBase getInstance(){
    	return hbaseConexion;
    }
    
    //creamos la conexión
	public static Connection createConnection() throws IOException{
		 // Instantiating Configuration class
        Configuration config = HBaseConfiguration.create();           
        config.set("hbase.zookeeper.quorum", hbaseZookeeperQuorum);
        config.set("hbase.zookeeper.property.clientPort", hbaseZookeeperClientPort);        
        config.set("zookeeper.znode.parent", zookeeper_znode_parent);
        Connection c = ConnectionFactory.createConnection(config);
        return c;
	}

}
