/*
 * SingleBroker.java
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.test;

import static org.piqt.web.MqttPiaxConfig.KEY_MQTT_ALLOW_ANONYMOUS;
import static org.piqt.web.MqttPiaxConfig.KEY_MQTT_BIND_ADDRESS;
import static org.piqt.web.MqttPiaxConfig.KEY_MQTT_PERSISTENT_STORE;
import static org.piqt.web.MqttPiaxConfig.KEY_MQTT_PORT;
import static org.piqt.web.MqttPiaxConfig.KEY_PIAX_IP_ADDRESS;
import static org.piqt.web.MqttPiaxConfig.KEY_PIAX_PORT;
import static org.piqt.web.MqttPiaxConfig.KEY_PIAX_SEED_IP_ADDRESS;
import static org.piqt.web.MqttPiaxConfig.KEY_PIAX_SEED_PORT;

import java.util.Properties;

import org.piax.common.Destination;
import org.piax.gtrans.Peer;
import org.piax.gtrans.ov.ddll.NodeMonitor;
import org.piax.gtrans.ov.ring.MessagingFramework;
import org.piax.gtrans.ov.ring.rq.RQManager;
import org.piax.gtrans.ov.szk.Suzaku;
import org.piax.gtrans.raw.udp.UdpLocator;
import org.piax.gtrans.util.ThroughTransport;
import org.piax.pubsub.MqException;
import org.piax.pubsub.stla.LATKey;
import org.piax.pubsub.stla.PeerMqDeliveryToken;
import org.piqt.peer.PeerMqEngineMoquette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBroker {

    static Logger logger = LoggerFactory.getLogger(OnePeerMoquette.class);

    static String configFile;
    Properties prop;
    Peer p;
    PeerMqEngineMoquette e;

    String host = "localhost";

    public TestBroker(int port, int seedPort, int mqttPort, String persistentStore) {
        try {
            e = new PeerMqEngineMoquette(host, port, toMQTTProps(mqttPort, persistentStore));
        } catch (MqException e1) {
            e1.printStackTrace();
        }
        e.setSeed(host, seedPort);
        e.setClusterId("");
    }

    public void start() {
        PeerMqDeliveryToken.USE_DELEGATE = false;
        try {
            e.connect();
        } catch (MqException e1) {
            e1.printStackTrace();
        }
    }

    public void fin() throws MqException {
        e.disconnect();
        e.fin();
    }

    private Properties toMQTTProps(int mqttPort, String persistentStore) {
        Properties ret = new Properties();
        ret.setProperty("host", host);
        ret.setProperty("port", "" + mqttPort);
        ret.setProperty("persistent_store", persistentStore);
        ret.setProperty("allow_anonymous", "true");
        ret.setProperty("password_file", "");
        return ret;
    }
}
