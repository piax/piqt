/*
 * PeerMqEngineMoquette.java - A pub/sub engine with moquette implementation.
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.peer;

import static org.piqt.peer.Util.newline;
import static org.piqt.peer.Util.stackTraceStr;
import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.spi.impl.ProtocolProcessor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.piax.pubsub.MqCallback;
import org.piax.pubsub.MqDeliveryToken;
import org.piax.pubsub.MqException;
import org.piax.pubsub.MqMessage;
import org.piax.pubsub.MqTopic;
import org.piax.pubsub.stla.LATKey;
import org.piax.pubsub.stla.LATopic;
import org.piax.pubsub.stla.PeerMqEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerMqEngineMoquette extends PeerMqEngine {
    private static final Logger logger = LoggerFactory
            .getLogger(PeerMqEngineMoquette.class.getPackage().getName());
    Broker moquette;
    ProtocolProcessor pp;
    //PeerId peerId;
    Observer observer;
    
    public PeerMqEngineMoquette(String host, int port,
            Properties config) throws MqException {
        super(host, port);
        moquette = new Broker(this, config);
        //peerId = overlay.getPeerId();
        observer = new Observer(this);
        
        setCallback(new MqCallback() {
            @Override
            public void deliveryComplete(MqDeliveryToken arg0) {
                logger.debug("Launcher deliveryComplete: topic="
                        + arg0.getTopics());
            }

            @Override
            public void messageArrived(MqTopic t, MqMessage m) {
                byte[] body = m.getPayload();
                String msg = null;
                try {
                    msg = new String(body, "UTF-8");
                } catch (UnsupportedEncodingException e1) {
                    String msg2 = "Exception caused by debugging codes.";
                    String detail = stackTraceStr(e1);
                    logger.debug(msg2 + newline + detail);
                }
                logger.debug("Launcher messageArrived: topic=" + m.getTopic()
                        + " msg=" + msg);
                write(m);
            }
        });
    }

    public PeerMqEngineMoquette(String host, int port) throws MqException {
        super(host, port);

        Properties properties = new Properties();
        properties.setProperty("host", host);
        properties.setProperty("port", String.valueOf(port));
        moquette = new Broker(this, properties);
    }

    boolean moquette_started = false;

    public void connect() throws MqException {
        super.connect();
        try {
            moquette.start(observer);
        } catch (Exception e) {
            super.disconnect();
            throw new MqException(e);
        }
        moquette_started = true;
    }

    public void disconnect() throws MqException {
        super.disconnect();
        if (moquette_started) {
            moquette.stop();
            moquette_started = false;
        }
    }

    public void publish(String topic, String clientId, byte[] payload, int qos, boolean retain)
            throws MqException {
        MqMessage m = new MqMessageMoquette(topic, peer.getPeerId(), clientId);
        m.setPayload(payload);
        m.setQos(qos);
        m.setRetained(retain);
        publish(m);
    }

    public void write(MqMessage m) {
        String c = null;
        if (m instanceof MqMessageMoquette) {
            MqMessageMoquette msg = (MqMessageMoquette) m;
            c = msg.getClientId();
            if (msg.getPeerId().equals(peer.getPeerId())) {
                return;
            }
        }
        if (c != null) {
            PublishMessage msg = new PublishMessage();
            msg.setRetainFlag(m.isRetained());
            msg.setTopicName(m.getTopic());
            msg.setQos(AbstractMessage.QOSType.valueOf((byte) m.getQos()));
            msg.setPayload(ByteBuffer.wrap(m.getPayload()));
            msg.setLocal(false);
            msg.setClientId(c);
            moquette.server.internalPublish(msg);
            observer.onReceive(m);
        }
    }

    public void unsubscribe(String topic) {
        MqTopic removeCandidate = null;
        int found = 0;
        for (MqTopic e : subscribes) {
            if (e.getSpecified().equals(topic)) {
                if (removeCandidate == null)
                    removeCandidate = e;
                found++;
            }
        }
        if (removeCandidate != null) {
            logger.debug("removeCandidate: " + removeCandidate.getSpecified()
                    + " " + found + " found.");
            if (found > 1) {
                subscribes.remove(removeCandidate);
            } else {
                // 1つだけの場合はオーバレイから削除
                String sKeyStr = null;
                for (Iterator<MqTopic> it = subscribes.iterator(); it.hasNext();) {
                    MqTopic t = it.next();
                    if (t.getSpecified().equals(topic)) {
                        sKeyStr = t.getSubscriberKeyString();
                        it.remove();
                    }
                }
                if (sKeyStr != null) {
                    boolean anotherExists = false;
                    for (MqTopic t : subscribes) {
                        if (sKeyStr.equals(t.getSubscriberKeyString())) {
                            anotherExists = true;
                        }
                    }
                    if (!anotherExists) {
                        try {
                            logger.info("remove key from overlay. key="
                                    + sKeyStr);
                            LATKey sKey = new LATKey(new LATopic(sKeyStr));
                            joinedKeys.remove(sKey);
                            o.removeKey(sKey);
                        } catch (IOException e) {
                            logger.error("Failed to Suzaku.removeKey()."
                                    + newline + stackTraceStr(e));
                        }
                    }
                }
            }
        }
    }

    public void notifyDeletedTopics(Set<String> deletedTopics) {
        if (deletedTopics.size() > 0) {
            logger.debug("remove candidate topic=" + deletedTopics);
        }
        for (String t : deletedTopics) {
            unsubscribe(t);
        }
    }

    public String getStatistics() {
        Statistics stat = observer.getStatistics();
        return stat.dump();
    }

}
