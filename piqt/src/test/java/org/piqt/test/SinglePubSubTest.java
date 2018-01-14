/*
 * SinglePubSubTest.java - A test.
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.test;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;
import org.piax.ayame.ov.suzaku.SuzakuStrategy;
import org.piax.common.Destination;
import org.piax.gtrans.ov.suzaku.Suzaku;
import org.piax.pubsub.MqCallback;
import org.piax.pubsub.MqDeliveryToken;
import org.piax.pubsub.MqMessage;
import org.piax.pubsub.MqTopic;
import org.piax.pubsub.stla.ClusterId;
import org.piax.pubsub.stla.LATKey;
import org.piax.pubsub.stla.PeerMqDeliveryToken;
import org.piax.pubsub.stla.PeerMqEngine;
import org.piqt.peer.PeerMqEngineMoquette;

import io.moquette.server.Server;
import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;

public class SinglePubSubTest {
    int numOfPeers = 2;
    static int startPort = 12360;
    static int count = 0;
    static final int FLEVELS = 4;

    synchronized public static void countUp() {
        count++;
    }

    synchronized public static int count() {
        return count;
    }

    synchronized public static void clearCount() {
        count = 0;
    }

    @Test
    public void PublishTest() throws Exception {
        System.out.println("@@@ PublishTest");
        // PeerMqDeliveryToken.USE_DELEGATE = false;
        // System.out.println("--- NO DELEGATE ---");
        // runTest2(0, 0);
        count = 0;
        PeerMqDeliveryToken.USE_DELEGATE.set(false);;
        //System.out.println("--- USE DELEGATE ---");
        runTest2(0, 0);
        count = 0;
        numOfPeers = 2;
        PeerMqDeliveryToken.USE_DELEGATE.set(true);
        //System.out.println("--- USE DELEGATE ---");
        runTest2(0, 0);
        
        count = 0;
        numOfPeers = 8;
        PeerMqDeliveryToken.USE_DELEGATE.set(true);
        //System.out.println("--- USE DELEGATE ---");
        runTest2(0, 0);

        // System.out.println("--- USE DELEGATE ---");
        // PeerMqDeliveryToken.USE_DELEGATE = true;
        // runTest(0, 0);
        // // warm up hot spot
        // System.out.println("---start---");
        // System.out.println("--- NO DELEGATE ---");
        // PeerMqDeliveryToken.USE_DELEGATE = false;
        // runTest(0, 0);
        // System.out.println("--- USE DELEGATE ---");
        // PeerMqDeliveryToken.USE_DELEGATE = true;
        // runTest(0, 0);

    }

    // @Test
    public void MoquetteTest() throws Exception {
        //IConfig classPathConfig = new ClasspathConfig();
        IResourceLoader classpathLoader = new ClasspathResourceLoader();
        IConfig classPathConfig  = new ResourceLoaderConfig(classpathLoader);
        Server mqttBroker = new Server();
        mqttBroker.startServer(classPathConfig);
        System.out.println("Broker started");
//        int c = System.in.read();
        System.out.println("Stopping broker");
        mqttBroker.stopServer();
        System.out.println("Broker stopped");
    }

    public void runTest2(int qos, int failureLevel) throws Exception {
        PeerMqEngine e[] = new PeerMqEngine[numOfPeers];

        int port = startPort;
        for (int i = 0; i < numOfPeers; i++) {
            ClusterId cid;
            if (i < (numOfPeers / 3)) {
                cid = new ClusterId("jp.isp1.dc1");
            } else if (i < (numOfPeers * 2 / 3)) {
                cid = new ClusterId("jp.isp1.dc2");
            } else {
                cid = new ClusterId("jp.isp1.dc3");
            }
            int myPort = port;
            Properties properties = new Properties();
            properties.setProperty("host", "localhost");
            properties.setProperty("port", String.valueOf(++port)); // 12361
            e[i] = new PeerMqEngineMoquette("localhost", myPort, properties); // 12360
            port++; // 12362
            e[i].setSeed("localhost", startPort);
            e[i].setClusterId(cid.toString());
            e[i].setCallback(new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    countUp();
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                }
            });
            e[i].connect();
        }

        e[1].subscribe("sport/tennis/+");
        Thread.sleep(100);
        e[0].publish("sport/tennis/player1", String.valueOf(e[0].getPort()).getBytes(), qos);

        for (int i = 0; i < numOfPeers; i++) {
            e[i].disconnect();
            e[i].fin();
        }
        assertTrue(count == 1);
    }

    @SuppressWarnings("unchecked")
    public void runTest(int qos, int failureLevel) throws Exception {
        PeerMqEngine e[] = new PeerMqEngine[numOfPeers];
        Suzaku<Destination, LATKey> szk[] = new Suzaku[numOfPeers];
        int port = startPort;

        for (int i = 0; i < numOfPeers; i++) {
            ClusterId cid;
            if (i < (numOfPeers / 3)) {
                cid = new ClusterId("jp.isp1.dc1");
            } else if (i < (numOfPeers * 2 / 3)) {
                cid = new ClusterId("jp.isp1.dc2");
            } else {
                cid = new ClusterId("jp.isp1.dc3");
            }
            szk[i] = new Suzaku<>("id:*:tcp:localhost" + (port++));
            e[i] = new PeerMqEngine(szk[i]);
            e[i].setSeed("localhost", startPort);
            e[i].setClusterId(cid.toString());
            e[i].setCallback(new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    System.out.println("@@@ messageArrived");
                    byte[] body = m.getPayload();
                    String msg = new String(body, "UTF-8");
                    System.out.println("time="
                            + (System.currentTimeMillis() - Long
                                    .parseLong(new String(body))) + " msg="
                            + msg);
                    countUp();
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                    System.out.println("@@@ deliveryComplete");
                }
            });
            e[i].connect();
            System.out.println("e" + i + " " + cid);
            // Thread.sleep(100);
        }

        e[1].subscribe("sport/tennis/+");
        System.out.println("@@@ e1 subscribed");

        System.out.println("start sleep 3 sec");
        Thread.sleep(3000);
        SuzakuStrategy.UPDATE_FINGER_PERIOD.set(5 * 1000);

        // List<Object> failures = new ArrayList<Object>();
        // for (int i = 1; i * 10 < numOfPeers; i++) {
        // if (failureLevel >= 1) {
        // fs[i * 10].setErrorRate(100);
        // fs[i * 10].upsetTransport();
        // failures.add("" + (i * 10));
        // }
        // if (failureLevel >= 2) {
        // fs[(i * 10) + 2].setErrorRate(100);
        // fs[(i * 10) + 2].upsetTransport();
        // failures.add("" + (i * 10 + 2));
        // }
        // if (failureLevel >= 3) {
        // fs[(i * 10) + 1].setErrorRate(100);
        // fs[(i * 10) + 1].upsetTransport();
        // failures.add("" + (i * 10 + 1));
        // }
        // }
        // System.out.println("failures=" + failures);
        // System.out.println("running qos=" + qos + ", failureLevel=" +
        // failureLevel);
        System.out.println("@@@ qos=" + qos);
        // for (int times = 1; times <= 10; times++) {
        // clearCount();
        // for (int i = 0; i < numOfPeers; i++) {
        // c[i].clearCounter();
        // }
        // long start = System.currentTimeMillis();
        // e[0].publish("sport/tennis/player1", ("" + start).getBytes(), qos);
        // Thread.sleep(1000);
        // System.out.println("c " + times + " " + count());
        // int traffic = 0;
        // for (int i = 0; i < numOfPeers; i++) {
        // traffic += c[i].getCounter();
        // }
        // System.out.println("t " + times + " " + traffic);
        // }
        long start = System.currentTimeMillis();
        e[0].publish("sport/tennis/player1", ("" + start).getBytes(), qos);
        System.out.println("@@@ e0 published");
        Thread.sleep(3000);

        // for (int i = 1; i < numOfPeers; i++) {
        // fs[i].setErrorRate(0);
        // }
        System.out.println("running fin");
        for (int i = 0; i < numOfPeers; i++) {
            e[i].disconnect();
            e[i].fin();
        }
        System.out.println("end.");
    }
}
