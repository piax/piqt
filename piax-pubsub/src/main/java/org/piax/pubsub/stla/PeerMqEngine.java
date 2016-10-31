/*
 * PeerMqEngine.java - An implementation of pub/sub engine.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub.stla;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.piax.common.Destination;
import org.piax.common.Endpoint;
import org.piax.common.PeerId;
import org.piax.common.PeerLocator;
import org.piax.gtrans.FutureQueue;
import org.piax.gtrans.Peer;
import org.piax.gtrans.ReceivedMessage;
import org.piax.gtrans.Transport;
import org.piax.gtrans.ov.Overlay;
import org.piax.gtrans.ov.OverlayListener;
import org.piax.gtrans.ov.OverlayReceivedMessage;
import org.piax.gtrans.ov.szk.Suzaku;
import org.piax.gtrans.raw.udp.UdpLocator;
import org.piax.pubsub.MqCallback;
import org.piax.pubsub.MqDeliveryToken;
import org.piax.pubsub.MqEngine;
import org.piax.pubsub.MqException;
import org.piax.pubsub.MqMessage;
import org.piax.pubsub.MqTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerMqEngine implements MqEngine,
        OverlayListener<Destination, LATKey> {
    private static final Logger logger = LoggerFactory
            .getLogger(PeerMqEngine.class);
    protected Peer peer;
    protected PeerId pid;
    protected PeerLocator seed;
    protected Overlay<Destination, LATKey> o;
    protected MqCallback callback;
    protected Delegator<Endpoint> d;
    protected String host;
    protected int port;

    protected ClusterId clusterId = null;

    protected List<MqTopic> subscribes; // subscribed topics;
    protected List<LATKey> joinedKeys; // joined keys;

    public static int DELIVERY_TIMEOUT = 3000;
    int seqNo;

    public PeerMqEngine(Overlay<Destination, LATKey> overlay)
            throws MqException {
        subscribes = new ArrayList<MqTopic>();
        joinedKeys = new ArrayList<LATKey>();
        o = overlay;
        Transport<?> t = o.getLowerTransport();
        peer = t.getPeer();
        pid = peer.getPeerId();
        try {
            d = new Delegator<Endpoint>(this);
        } catch (Exception e) {
            throw new MqException(e);
        }
        seqNo = 0;
    }

    public PeerMqEngine(String host, int port) throws MqException {
        subscribes = new ArrayList<MqTopic>();
        joinedKeys = new ArrayList<LATKey>();
        this.host = host;
        this.port = port;
        peer = Peer.getInstance(pid = PeerId.newId());
        try {
            o = new Suzaku<Destination, LATKey>(
                    peer.newBaseChannelTransport(new UdpLocator(
                            new InetSocketAddress(host, port))));
            d = new Delegator<Endpoint>(this);
        } catch (Exception e) {
            throw new MqException(e);
        }
    }

    ConcurrentHashMap<Integer, PeerMqDeliveryToken> tokens = new ConcurrentHashMap<Integer, PeerMqDeliveryToken>();

    ConcurrentHashMap<String, TopicDelegator[]> delegateCache = new ConcurrentHashMap<String, TopicDelegator[]>();

    public void delegate(PeerMqDeliveryToken token, Endpoint e, String topic,
            MqMessage message) {
        DelegatorIf dif = d.getStub(e);
        tokens.put(token.seqNo, token);
        dif.delegate(o.getLowerTransport().getEndpoint(), token.seqNo, topic,
                message);
    }
    
    public int getPort() {
        return port;
    }
    
    public String getHost() {
        return host;
    }

    public void foundDelegators(String topic, TopicDelegator[] delegators) {
        delegateCache.put(topic, delegators);
    }

    public TopicDelegator[] getDelegators(String topic) {
        return delegateCache.get(topic);
    }

    public void delegationSucceeded(int tokenId, String topic) {
        PeerMqDeliveryToken token = tokens.get(tokenId);
        if (token == null) {
            logger.info("unregistered delivery succeeded: tokenId={}", tokenId);
        }
        if (token.delegationSucceeded(topic)) {
            tokens.remove(tokenId);
        }
    }

    public void setSeed(String host, int port) {
        seed = new UdpLocator(new InetSocketAddress(host, port));
    }

    public void setClusterId(String clusterId) {
        this.clusterId = new ClusterId(clusterId);
    }

    public ClusterId getClusterId() {
        return this.clusterId;
    }

    public List<LATKey> getJoinedKeys() {
        return joinedKeys;
    }

    synchronized void countUpSeqNo() {
        seqNo++;
    }

    synchronized int nextSeqNo() {
        return seqNo;
    }

    @Override
    public void connect() throws MqException {
        try {
            if (seed == null) {
                throw new MqException(MqException.REASON_SEED_NOT_AVAILABLE);
            }
            o.join(seed);
            o.setListener(this);
        } catch (Exception e) {
            throw new MqException(e);
        }
    }

    @Override
    public void disconnect() throws MqException {
        if (isConnected()) {
            try {
                o.leave();
            } catch (IOException e) {
                new MqException(e);
            }
        }
    }

    @Override
    public void subscribe(String topic) throws MqException {
        MqTopic sTopic = new MqTopic(topic);
        subscribes.add(sTopic);
        try {
            LATopic latk = new LATopic(sTopic.getSubscriberKeyString());
            if (clusterId != null) {
                latk.setClusterId(clusterId);
            }
            LATKey key = new LATKey(latk);
            boolean included = false;
            MqTopic stripped = new MqTopic(key.getKey().getTopic());
            for (LATKey jk : joinedKeys) {
                for (String pk : stripped.getPublisherKeyStrings()) {
                    // ex.
                    // newly joining key: 'sport/tennis/player1'
                    // joined keys: 'sport/tennis'
                    // -> no need to join (included)
                    if (pk.equals(jk.getKey().getTopic())) {
                        included = true;
                    }
                }
            }

            if (!included) {
                // newly joining key: 'sport/tennis'
                // joined key: 'sport/tennis/player1'
                // -> joined key becomes useless
                List<LATKey> uselessKeys = new ArrayList<LATKey>();
                for (LATKey jk : joinedKeys) {
                    MqTopic joined = new MqTopic(jk.getKey().getTopic());
                    for (String jpk : joined.getPublisherKeyStrings()) {
                        if (jpk.equals(key.getKey().getTopic())) {
                            uselessKeys.add(jk);
                        }
                    }
                }
                for (LATKey uKey : uselessKeys) {
                    o.removeKey(uKey);
                    joinedKeys.remove(uKey);
                }
                o.addKey(key);
                joinedKeys.add(key);
            }
        } catch (IOException e) {
            throw new MqException(e);
        }
    }

    @Override
    public void subscribe(String[] topics) throws MqException {
        for (String topic : topics) {
            subscribe(topic);
        }
    }

    @Override
    public void setCallback(MqCallback callback) {
        this.callback = callback;
    }

    @Override
    public boolean isConnected() {
        return o.isJoined();
    }

    @Override
    public void fin() {
        peer.fin();
    }

    @Override
    public void subscribe(String[] topics, int[] qos) throws MqException {
        // XXX qos is ignored at this moment.
        subscribe(topics);
    }

    @Override
    public void unsubscribe(String topic) throws MqException {
        String sKeyStr = null;
        for (Iterator<MqTopic> it = subscribes.iterator(); it.hasNext();) {
            MqTopic t = it.next();
            sKeyStr = t.getSubscriberKeyString();
            if (t.getSpecified().equals(topic)) {
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
                    LATKey sKey = new LATKey(new LATopic(sKeyStr));
                    joinedKeys.remove(sKey);
                    o.removeKey(sKey);
                } catch (IOException e) {
                    throw new MqException(e);
                }
            }
        }
    }

    public Overlay<Destination, LATKey> getOverlay() {
        return o;
    }

    public void setOverlay(Overlay<Destination, LATKey> o) {
        this.o = o;
    }

    @Override
    public void unsubscribe(String[] topics) throws MqException {
        for (String topic : topics) {
            unsubscribe(topic);
        }
    }

    @Override
    public void publish(String topic, byte[] payload, int qos)
            throws MqException {
        MqMessage m = new MqMessage(topic);
        m.setPayload(payload);
        m.setQos(qos);
        publish(m);
    }

    @Override
    public void onReceive(Overlay<Destination, LATKey> ov,
            OverlayReceivedMessage<LATKey> rmsg) {
        logger.debug("onReceive on Transport: trans={} rmsg={}", ov, rmsg);
        try {
            for (MqTopic t : subscribes) {
                MqMessage m = (MqMessage) rmsg.getMessage();
                if (t.matchesToTopic(m.getTopic())) {
                    callback.messageArrived(t, m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public FutureQueue<?> onReceiveRequest(Overlay<Destination, LATKey> ov,
            OverlayReceivedMessage<LATKey> rmsg) {
        logger.debug("onReceiveRequest on Overlay: overlay={} rmsg={} keys={}", ov,
                rmsg, o.getKeys());

        Object msg = rmsg.getMessage();
        if (msg instanceof DelegatorCommand) {// requested by findDelegators.
            DelegatorCommand com = (DelegatorCommand) msg;
            String searchTopic = com.topic;
            boolean noMatch = true;
            for (LATKey key : rmsg.getMatchedKeys()) {
                if (searchTopic.equals(key.getKey().topic)) {
                    noMatch = false;
                }
            }
            if (noMatch) {
                return ov.singletonFutureQueue(null); // the topic did not match. null return. 
            }
            return ov.singletonFutureQueue(o.getLowerTransport().getEndpoint());
        }

        List<String> matched = new ArrayList<String>();
        Throwable th = null;
        try {
            for (MqTopic t : subscribes) {
                MqMessage m = (MqMessage) rmsg.getMessage();
                if (t.matchesToTopic(m.getTopic())) {
                    matched.add(t.getSpecified());
                    callback.messageArrived(t, m);
                }
            }
        } catch (Exception e) {
            th = e;
        }
        return ov.singletonFutureQueue(getPeerId(), th);
    }

    @Override
    public MqDeliveryToken publishAsync(MqMessage m) throws MqException {
        countUpSeqNo();
        PeerMqDeliveryToken t = new PeerMqDeliveryToken(o, m, callback,
                nextSeqNo());
        t.startDelivery(this);
        return t;
    }

    @Override
    public void publish(MqMessage m) throws MqException {
        PeerMqDeliveryToken t = (PeerMqDeliveryToken) publishAsync(m);
        t.waitForCompletion();
    }

    public PeerId getPeerId() {
        return pid;
    }

    @Override
    public void onReceive(Transport<Destination> trans, ReceivedMessage rmsg) {
        logger.debug("onReceive on Transport: trans={} rmsg={}", trans, rmsg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.piax.mq.MqEngine#subscribedTo(java.lang.String)
     */
    @Override
    public boolean subscribedTo(String topic) {
        for (LATKey k : joinedKeys) {
            if (k.getKey().getTopic().equals(topic)) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.piax.mq.MqEngine#subscriptionMatchesTo(java.lang.String)
     */
    @Override
    public boolean subscriptionMatchesTo(String topic) {
        for (MqTopic t : subscribes) {
            try {
                if (t.matchesToTopic(topic)) {
                    return true;
                }
            } catch (MqException e) {
                return false;
            }
        }
        return false;
    }

    public static void main(String args[]) {
        try {
            PeerMqEngine engine = new PeerMqEngine("localhost", 12367);
            engine.setCallback(new MqCallback() {
                @Override
                public void messageArrived(MqTopic subscribedTopic, MqMessage m)
                        throws Exception {
                    System.out.println("received:" + m + " on subscription:"
                            + subscribedTopic.getSpecified() + " for topic:"
                            + m.getTopic());
                }

                @Override
                public void deliveryComplete(MqDeliveryToken token) {
                    System.out.println("delivered:"
                            + token.getMessage().getTopic());
                }
            });
            engine.setSeed("localhost", 12367);
            // engine.setClusterId("cluster.test");
            engine.connect();
            engine.subscribe("sport/tennis/player1");
            System.out.println("joinedKeys=" + engine.getJoinedKeys());
            engine.publish("sport/tennis/player1", "hello1".getBytes(), 0);
            engine.subscribe("#");
            engine.subscribe("+/#");
            engine.subscribe("/+/#");
            engine.subscribe("sport/+");
            System.out.println("joinedKeys=" + engine.getJoinedKeys());
            System.out.println("sleeping 20 sec");
            Thread.sleep(20000);
            engine.publish("sport/tennis", "hello2".getBytes(), 0);
            engine.publish("/sport/tennis", "hello3".getBytes(), 1);
            engine.disconnect();
            engine.fin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
