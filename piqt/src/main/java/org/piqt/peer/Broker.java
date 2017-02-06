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

import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.MemoryConfig;
import io.moquette.server.config.ResourceLoaderConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.piax.pubsub.MqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Broker {
    private static final Logger logger = LoggerFactory.getLogger(Broker.class
            .getPackage().getName());

    static int port = 1883;

    static Map<Thread, PeerMqEngineMoquette> engineMap = new HashMap<Thread, PeerMqEngineMoquette>();

    private IConfig config = null;
    private Properties properties = null;
    public Server server = null;
    public Observer observer = null;
    PeerMqEngineMoquette engine;

    public Broker(PeerMqEngineMoquette engine, Properties web_config) {
        if (web_config == null) {
            IResourceLoader classpathLoader = new ClasspathResourceLoader();
            config = new ResourceLoaderConfig(classpathLoader);
            //config = new ClasspathConfig();
            config.setProperty("port", String.valueOf(port));
            ++port;
        } else {
            properties = web_config;
        }
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

    public void start(Observer observer) throws MqException {
        if (server == null) {
            server = new Server();
        }
        List<InterceptHandler> lh = new ArrayList<InterceptHandler>();
        try {
            putEngine(engine);
            lh.add(observer);
            if (properties != null) {
                IConfig config = new MemoryConfig(properties);
                server.startServer(config, lh);
            } else {
                server.startServer(config);
            }
            observer.onOpen(server.getSubscriptions());
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
        if (server != null) {
            server.stopServer();
            server = null;
        }
        logger.info("MQTT Broker stopped: " + engine);
    }

}
