/*
 * BrokerTest.java -- tests using MQTT clients. 
 * 
 * Copyright (c) 2017 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.Test;

public class BrokerTest {

    static MqttClientPersistence s_dataStore;
    
    class MyMqttCallback implements MqttCallback {
        public AtomicInteger receivedCount;
        public MyMqttCallback() {
            receivedCount = new AtomicInteger(0);
        }
        
        public int getCount() {
            return receivedCount.get();
        }
        
        @Override
        public void connectionLost(Throwable cause) {
        }

        @Override
        public void messageArrived(String topic, MqttMessage message)
                throws Exception {
            System.out.println("received=" + topic + ",mes=" + new String(message.getPayload()));
            receivedCount.incrementAndGet();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }
    
    private IMqttClient client(int port, String clientId, String persistentFile, MyMqttCallback m_callback) throws Exception {
        IMqttClient m_client;

        String tmpDir = System.getProperty("java.io.tmpdir");
        s_dataStore = new MqttDefaultFilePersistence(tmpDir);
        
        m_client = new MqttClient("tcp://localhost:" + port, clientId, s_dataStore);
        m_client.setCallback(m_callback);
        m_client.connect();
        return m_client;
    }

    private void subscribe(IMqttClient m_client) throws Exception {
        m_client.subscribe("topic-1", 1);
    }
    
    private void publish(IMqttClient m_client) throws Exception {
        MqttMessage mes = new MqttMessage();
        mes.setId(1);
        mes.setPayload("hello".getBytes());
        mes.setQos(1);
        m_client.publish("topic-1", mes);
    }
    
    private void close(IMqttClient m_client) throws Exception {
        if (m_client != null && m_client.isConnected()) {
            m_client.disconnect();
        }
    }

    public static void cleanPersistenceFile(String fileName) {
        File dbFile = new File(fileName);
        if (dbFile.exists()) {
            dbFile.delete();
            new File(fileName + ".p").delete();
            new File(fileName + ".t").delete();
        }
    }
    
    @Test
    public void multiPubSubTest() throws Exception {
        String tmpDir = System.getProperty("java.io.tmpdir");
        
        File tmpFile1 = new File(tmpDir, "c1");
        String persistentFile1 = tmpFile1.getAbsolutePath();
        TstBroker sb1 = new TstBroker(10000, 10000, 10883, persistentFile1);
        
        File tmpFile2 = new File(tmpDir, "c2");
        String persistentFile2 = tmpFile2.getAbsolutePath();
        TstBroker sb2 = new TstBroker(10001, 10000, 10884, persistentFile2);
        IMqttClient c1 = null, c2 = null;
        try {
            sb1.start();
            sb2.start();
            Thread.sleep(1000);
            MyMqttCallback mmc1 = new MyMqttCallback();
            c1 = client(10883, "c1", persistentFile1, mmc1);
            MyMqttCallback mmc2 = new MyMqttCallback();
            c2 = client(10884, "c2", persistentFile1, mmc2);
            subscribe(c2);
            Thread.sleep(1000);
            publish(c1);
            Thread.sleep(1000);
            System.out.println(mmc2.getCount());
            assertTrue(mmc2.getCount() == 1);
            
            System.out.println(mmc1.getCount());
            assertTrue(mmc1.getCount() == 0);
        }
        finally {
            close(c1);
            close(c2);
            sb1.fin();
            sb2.fin();
            cleanPersistenceFile(persistentFile1);
            cleanPersistenceFile(persistentFile2);
        }
    }
    
    @Test
    public void singlePubSubTest() throws Exception {
        String tmpDir = System.getProperty("java.io.tmpdir");
        
        File tmpFile1 = new File(tmpDir, "c1");
        String persistentFile1 = tmpFile1.getAbsolutePath();
        TstBroker sb1 = new TstBroker(10000, 10000, 10883, persistentFile1);
        IMqttClient c1 = null;
        try {
            sb1.start();
            Thread.sleep(1000);
            MyMqttCallback mmc = new MyMqttCallback();
            c1 = client(10883, "c1", persistentFile1, mmc);
            subscribe(c1);
            Thread.sleep(1000);
            publish(c1);
            Thread.sleep(1000);
            System.out.println(mmc.getCount());
            assertTrue(mmc.getCount() == 1);
        }
        finally {
            close(c1);
            sb1.fin();
            cleanPersistenceFile(persistentFile1);
        }
    }
}