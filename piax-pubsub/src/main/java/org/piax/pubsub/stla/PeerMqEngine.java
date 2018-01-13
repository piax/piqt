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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.piax.common.Destination;
import org.piax.common.Endpoint;
import org.piax.common.PeerId;
import org.piax.common.TransportId;
import org.piax.gtrans.ReceivedMessage;
import org.piax.gtrans.Transport;
import org.piax.gtrans.ov.Overlay;
import org.piax.gtrans.ov.OverlayListener;
import org.piax.gtrans.ov.OverlayReceivedMessage;
import org.piax.gtrans.ov.suzaku.Suzaku;
import org.piax.pubsub.MqCallback;
import org.piax.pubsub.MqDeliveryToken;
import org.piax.pubsub.MqEngine;
import org.piax.pubsub.MqException;
import org.piax.pubsub.MqMessage;
import org.piax.pubsub.MqTopic;
import org.piax.pubsub.stla.Delegator.ControlMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerMqEngine implements MqEngine,
        OverlayListener<Destination, LATKey>, Closeable {
    private static final Logger logger = LoggerFactory
            .getLogger(PeerMqEngine.class);
    protected PeerId pid;
    protected String seed;
    protected Overlay<Destination, LATKey> o;
    protected MqCallback callback;
    protected Delegator<Endpoint> d;
    protected String host;
    protected int port;

    protected ClusterId clusterId = null;

    protected List<MqTopic> subscribes; // subscribed topics;
    protected List<LATKey> joinedKeys; // joined keys;

    public static int DELIVERY_TIMEOUT = 3000;
    public static String NETTY_TYPE = "tcp";
    int seqNo;

    ConcurrentHashMap<Integer, PeerMqDeliveryToken> tokens = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, NearestDelegator> delegateCache = new ConcurrentHashMap<>();

    public PeerMqEngine(Overlay<Destination, LATKey> overlay)
            throws MqException {
        subscribes = new ArrayList<MqTopic>();
        joinedKeys = new ArrayList<LATKey>();
        o = overlay;
        pid = o.getPeerId();
        try {
            d = new Delegator<Endpoint>(this);
        } catch (Exception e) {
            throw new MqException(e);
        }
        seqNo = 0;
    }

    @SuppressWarnings("unchecked")
    public PeerMqEngine(String host, int port) throws MqException {
        subscribes = new ArrayList<MqTopic>();
        joinedKeys = new ArrayList<LATKey>();
        this.host = host;
        this.port = port;
        try {
            o = new Suzaku<>("id:*:" + NETTY_TYPE + ":" + host + ":" + port);
            d = new Delegator<>(this);
        } catch (Exception e) {
            throw new MqException(e);
        }
        pid = o.getLowerTransport().getPeerId();
        Transport<Endpoint> t = ((Transport<Endpoint>)o.getLowerTransport());
        t.setListener(new TransportId("delegate"), (trans, mes) -> {
            ControlMessage c = (ControlMessage)mes.getMessage();
            logger.debug("delegating: content={}", c.content);
            d.delegate(c);
        });
        t.setListener(new TransportId("delegated"), (trans, mes) -> {
            ControlMessage c = (ControlMessage)mes.getMessage();
            d.delegated(c);
        });
        t.setListener(new TransportId("failed"), (trans, mes) -> {
            ControlMessage c = (ControlMessage)mes.getMessage();
            d.failed(c);
        });
        t.setListener(new TransportId("succeeded"), (trans, mes) -> {
            ControlMessage c = (ControlMessage)mes.getMessage();
            d.succeeded(c);
        });
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> delegate(PeerMqDeliveryToken token, Endpoint e, String topic, MqMessage message) {
        return ((Transport<Endpoint>)o.getLowerTransport()).sendAsync(new TransportId("delegate"), e, 
                new ControlMessage(getEndpoint(),
                        token.seqNo, topic, message, (short) -1));
    }

    public Endpoint getEndpoint() {
        return o.getLowerTransport().getEndpoint();
    }
    
    public int getPort() {
        return port;
    }
    
    public String getHost() {
        return host;
    }

    public void foundDelegator(String topic, DeliveryDelegator delegator) {
        delegateCache.put(topic, delegator);
    }

    public NearestDelegator getNearestDelegator(String topic) {
        return delegateCache.get(topic);
    }
    
    public void removeDelegator(String topic) {
        delegateCache.remove(topic);
    }

    public void delegationSucceeded(int tokenId, String kString) {
        PeerMqDeliveryToken token = tokens.get(tokenId);
        if (token == null) {
            logger.info("unregistered delivery succeeded: tokenId={}", tokenId);
            return;
        }
        logger.debug("found token:" + token);
        token.delegationSucceeded(kString);
        
        //tokens.remove(tokenId);
    }
    
    public void delegationRetry(int tokenId, String kString) {
        logger.debug("delivery for '{}' retrying on {}", kString, getPeerId());
        PeerMqDeliveryToken token = tokens.get(tokenId);
        if (token == null) {
            logger.info("unregistered delivery retried: tokenId={}", tokenId);
            return;
        }
        // Temporarily remove the delivery delegator and re-generate.
        delegateCache.remove(kString);
        try {
            DeliveryDelegator d = new DeliveryDelegator(kString);
            // XXX concurrent modification.  
            token.replaceExistingDeliveryDelegator(d);
            token.findDelegatorAndDeliver(d, this, kString);
        } catch (MqException e) {
            
        }
    }

    public void delegationFailed(int tokenId, String kString, Exception ex) {
        PeerMqDeliveryToken token = tokens.get(tokenId);
        if (token == null) {
            logger.info("unregistered delivery failed: tokenId={}", tokenId);
            return;
        }
        // Remove the delivery delegator. start from find next time.
        delegateCache.remove(kString);
        logger.info("delegation failed for key:{} by exception ", kString, ex);
    }

    public void setSeed(String host, int port) {
        seed = NETTY_TYPE + ":" + host + ":" + port;
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
    
    public boolean isJoined(String kString) {
        for (LATKey k : joinedKeys) {
            if (k.key.getTopic().equals(kString)) {
                return true;
            }
        }
        return false;
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
            e.printStackTrace();
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
    public void close() {
        fin();
    }
    
    @Override
    public void fin() {
        o.fin();
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
    public Object onReceiveRequest(Overlay<Destination, LATKey> ov,
            OverlayReceivedMessage<LATKey> rmsg) {
        Object msg = rmsg.getMessage();
        if (msg instanceof ControlMessage) {// requested by findDelegators.
            ControlMessage c = (ControlMessage) msg;
            String searchTopic = c.kString;
            boolean noMatch = true;
            for (LATKey key : rmsg.getMatchedKeys()) {
                if (searchTopic.equals(key.getKey().topic)) {
                    logger.debug("searchTopic found: '{}'", searchTopic);
                    noMatch = false;
                }
            }
            if (noMatch) {
                // XXX appropriate delegator cannot be found if there is no engine lower than a LATKey.
                // ex.if t1.id2 and t2.id4 are joined,
                // a query from t2.id3 matches to t1.id2.
                // then, we need to forward it to the next neighbor tobic (TODO).
                // the current implementation returns null.
                
                return null; // the topic did not match. null return. 
            }
            
            d.delegate(c); // when completed, succeeded is send to sender 
            logger.debug("received control message: {} on {}", c, getPeerId());
            return getEndpoint();
        }

        MqMessage m = (MqMessage) rmsg.getMessage();
        try {
            for (MqTopic t : subscribes) {
                if (t.matchesToTopic(m.getTopic())) {
                    callback.messageArrived(t, m);
                }
            }
        }
        catch (Exception e) { // XXX how to propargate to the sender?
            e.printStackTrace();
        }
        logger.debug("received message: {} on {}", m, getPeerId());
        return getPeerId();
    }

    @Override
    public MqDeliveryToken publishAsync(MqMessage m) throws MqException {
        countUpSeqNo();
        PeerMqDeliveryToken t = new PeerMqDeliveryToken(o, m, callback,
                nextSeqNo());
        tokens.put(t.seqNo, t);
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
