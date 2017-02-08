package org.piqt.test;

/*
 * Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

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
        TestBroker sb1 = new TestBroker(10000, 10000, 10883, persistentFile1);
        
        File tmpFile2 = new File(tmpDir, "c2");
        String persistentFile2 = tmpFile2.getAbsolutePath();
        TestBroker sb2 = new TestBroker(10001, 10000, 10884, persistentFile2);

        try {
            sb1.start();
            sb2.start();
            Thread.sleep(1000);
            MyMqttCallback mmc1 = new MyMqttCallback();
            IMqttClient c1 = client(10883, "c1", persistentFile1, mmc1);
            MyMqttCallback mmc2 = new MyMqttCallback();
            IMqttClient c2 = client(10884, "c2", persistentFile1, mmc2);
            subscribe(c2);
            publish(c1);
            Thread.sleep(1000);

            System.out.println(mmc2.getCount());
            assertTrue(mmc2.getCount() == 1);
            
            System.out.println(mmc1.getCount());
            assertTrue(mmc1.getCount() == 0);
            
            close(c1);
            close(c2);
            sb1.fin();
            sb2.fin();
        }
        finally {
            cleanPersistenceFile(persistentFile1);
            cleanPersistenceFile(persistentFile2);
        }
    }
    
    @Test
    public void singlePubSubTest() throws Exception {
        String tmpDir = System.getProperty("java.io.tmpdir");
        
        File tmpFile1 = new File(tmpDir, "c1");
        String persistentFile1 = tmpFile1.getAbsolutePath();
        TestBroker sb1 = new TestBroker(10000, 10000, 10883, persistentFile1);
        
        /*File tmpFile2 = new File(tmpDir, "c2");
        String persistentFile2 = tmpFile2.getAbsolutePath();
        SingleBroker sb2 = new SingleBroker(10001, 10000, 10884, persistentFile2);
         */
        try {
            sb1.start();
            // sb2.start();
            Thread.sleep(1000);
            MyMqttCallback mmc = new MyMqttCallback();
            IMqttClient c1 = client(10883, "c1", persistentFile1, mmc);
            subscribe(c1);
            publish(c1);
            Thread.sleep(1000);
            System.out.println(mmc.getCount());
            assertTrue(mmc.getCount() == 1);
            close(c1);
            sb1.fin();
        }
        finally {
            cleanPersistenceFile(persistentFile1);
            //cleanPersistenceFile(persistentFile2);
        }
    }
}