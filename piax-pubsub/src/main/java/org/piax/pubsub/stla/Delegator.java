/*
 * Delegator.java - the publish delegator in STLA
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub.stla;

import java.io.IOException;
import java.io.Serializable;

import org.piax.common.Endpoint;
import org.piax.common.TransportId;
import org.piax.common.subspace.KeyRange;
import org.piax.gtrans.IdConflictException;
import org.piax.gtrans.TransOptions;
import org.piax.gtrans.TransOptions.ResponseType;
import org.piax.gtrans.TransOptions.RetransMode;
import org.piax.gtrans.Transport;
import org.piax.pubsub.MqException;
import org.piax.pubsub.MqMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Delegator<E extends Endpoint>// extends RPCInvoker<DelegatorIf, E>
        implements DelegatorIf {
    private static final Logger logger = LoggerFactory
            .getLogger(Delegator.class);
    static TransportId rpcId = new TransportId("pmqr");

    PeerMqEngine engine;
    
    static public class ControlMessage implements Serializable {
        public int tokenId;
        public String topic;
        public Endpoint source;
        public Serializable content;
        public short reasonCode;
        public ControlMessage(Endpoint source, int tokenId, String topic, Serializable content, short reasonCode) {
            super();
            this.tokenId = tokenId;
            this.topic = topic;
            this.source = source;
            this.content = content;
            this.reasonCode = reasonCode;
        }
        public ControlMessage(Endpoint source, int tokenId, String topic, Serializable content) {
            super();
            this.tokenId = tokenId;
            this.topic = topic;
            this.source = source;
            this.content = content;
            this.reasonCode = (short) -1;
        }
        public ControlMessage(Endpoint source, int tokenId, String topic) {
            super();
            this.tokenId = tokenId;
            this.topic = topic;
            this.source = source;
            this.content = null;
            this.reasonCode = (short) -1;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Delegator(PeerMqEngine engine) throws IdConflictException,
            IOException {
        this.engine = engine;
    }

    @Override
    public void delegate(ControlMessage c) {
        Endpoint sender = c.source;
        int tokenId = c.tokenId;
        String topic = c.topic;
        Serializable message = c.content;
        logger.debug("peer {} delegated topic {}", engine.getPeerId(), topic);
        Transport<Endpoint> trans =((Transport<Endpoint>)engine.getOverlay().getLowerTransport());
        
        trans.setListener(new TransportId("succeeded"), (t, rmsg)->{
            
        });
        
        try {
            RetransMode mode;
            ResponseType type;
            TransOptions mesOpts;
            MqMessage m = (MqMessage) message;
            switch (m.getQos()) {
            case 0:
                type = ResponseType.NO_RESPONSE;
                if (PeerMqDeliveryToken.ACK_INTERVAL < 0) {
                    mode = RetransMode.NONE;
                } else {
                    mode = (tokenId % PeerMqDeliveryToken.ACK_INTERVAL == 0) ? RetransMode.NONE_ACK
                            : RetransMode.NONE;
                }
                mesOpts = new TransOptions(type, mode);
                break;
            default: // 1, 2
                type = ResponseType.AGGREGATE;
                mode = RetransMode.FAST;
                mesOpts = new TransOptions(PeerMqEngine.DELIVERY_TIMEOUT, type,
                        mode);
                break;
            }
            engine.getOverlay().requestAsync(
                    new KeyRange<LATKey>(new LATKey(LATopic.topicMin(topic)),
                            new LATKey(LATopic.topicMax(topic))), (Object) m,
                    (res, ex)->{
                        if (ex != null) {
                            trans.sendAsync(
                                    new TransportId("failed"),
                                    sender,
                                    new ControlMessage(trans.getEndpoint(),
                                            tokenId,
                                            topic,
                                            null, MqException.REASON_CODE_UNEXPECTED_ERROR)
                                    );
                        }
                    }, mesOpts);
            logger.debug("requested topic:" + topic + ", m=" + m + ",on " + engine.getHost() + ":" + engine.getPort());
            trans.sendAsync(
                    new TransportId("delegated"),
                    sender,
                    new ControlMessage(trans.getEndpoint(),
                            tokenId,
                            topic,
                            null, (short) -1)
                    );
        } catch (Exception e) {
            e.printStackTrace();
            trans.sendAsync(
                    new TransportId("failed"),
                    sender,
                    new ControlMessage(trans.getEndpoint(),
                            tokenId,
                            topic,
                            null, MqException.REASON_CODE_UNEXPECTED_ERROR)
                    );
        }
        trans.sendAsync(
                new TransportId("succeeded"),
                sender,
                new ControlMessage(trans.getEndpoint(),
                        tokenId,
                        topic,
                        null, (short)-1)
                );
    }

    @Override
    public void delegated(ControlMessage c) {
        String topic = c.topic;
        logger.debug("peer:" + engine.getPeerId() + " received delegated :"
                + topic);
    }

    @Override
    public void succeeded(ControlMessage c) {
        int tokenId = c.tokenId;
        String topic = c.topic;
            logger.debug("peer:" + engine.getPeerId() + " received succeeded :"
                + topic);
        engine.delegationSucceeded(tokenId, topic);
    }

    @Override
    public void failed(ControlMessage c) {
        logger.debug("peer:" + engine.getPeerId() + " received failed");
    }
}
