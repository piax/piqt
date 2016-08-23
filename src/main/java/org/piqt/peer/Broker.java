/*
 * Broker.java - a broker implementation.
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.peer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.moquette.server.Server;
import org.eclipse.moquette.server.config.ClasspathConfig;
import org.eclipse.moquette.server.config.IConfig;
import org.piqt.MqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Broker {
    private static final Logger logger = LoggerFactory.getLogger(Broker.class
            .getPackage().getName());

    static int port = 1883;

    static Map<Thread, PeerMqEngineMoquette> engineMap = new HashMap<Thread, PeerMqEngineMoquette>();

    private IConfig config = null;
    private Properties properties = null;
    private Server mqttBroker = null;
    PeerMqEngineMoquette engine;

    public Broker(PeerMqEngineMoquette engine, Properties web_config) {
        if (web_config == null) {
            config = new ClasspathConfig();
            config.setProperty("websocket_port", "");
            config.setProperty("port", String.valueOf(port));
            ++port;
        } else {
            properties = web_config;
            properties.setProperty("websocket_port", "");
        }
        System.setProperty("intercept.handler", "org.piqt.peer.Observer");
        this.engine = engine;
    }

    synchronized static void putEngine(PeerMqEngineMoquette e) {
        Thread th = Thread.currentThread();
        engineMap.put(th, e);
    }

    synchronized static PeerMqEngineMoquette getEngine() {
        return engineMap.get(Thread.currentThread());
    }

    synchronized static void removeEngine() {
        engineMap.remove(Thread.currentThread());
    }

    public void start() throws MqException {
        if (mqttBroker == null) {
            mqttBroker = new Server();
        }
        try {
            putEngine(engine);
            if (properties != null) {
                mqttBroker.startServer(properties);
            } else {
                mqttBroker.startServer(config);
            }
        } catch (IOException e) {
            logger.error("MQTT Broker not started: " + engine);
            throw new MqException(e);
        } finally {
            removeEngine();
        }
        logger.info("MQTT Broker started: " + engine);
    }

    public void stop() {
        logger.info("MQTT Broker Stopping: " + engine + " th = "
                + Thread.currentThread());
        if (mqttBroker != null) {
            mqttBroker.stopServer();
            mqttBroker = null;
        }
        logger.info("MQTT Broker stopped: " + engine);
    }

}
