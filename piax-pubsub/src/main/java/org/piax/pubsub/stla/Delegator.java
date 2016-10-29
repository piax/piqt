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
import org.piax.gtrans.ChannelTransport;
import org.piax.gtrans.FutureQueue;
import org.piax.gtrans.IdConflictException;
import org.piax.gtrans.RPCInvoker;
import org.piax.gtrans.RemoteValue;
import org.piax.gtrans.TransOptions;
import org.piax.gtrans.TransOptions.ResponseType;
import org.piax.gtrans.TransOptions.RetransMode;
import org.piax.pubsub.MqException;
import org.piax.pubsub.MqMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Delegator<E extends Endpoint> extends RPCInvoker<DelegatorIf, E>
        implements DelegatorIf {
    private static final Logger logger = LoggerFactory
            .getLogger(Delegator.class);
    static TransportId rpcId = new TransportId("pmqr");

    PeerMqEngine engine;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Delegator(PeerMqEngine engine) throws IdConflictException,
            IOException {
        super(rpcId, (ChannelTransport) engine.getOverlay().getLowerTransport());
        this.engine = engine;
    }

    @Override
    public void delegate(Endpoint sender, int tokenId, String topic,
            Serializable message) {
        logger.debug("peer {} delegated topic {}", engine.getPeerId(), topic);
        @SuppressWarnings("unchecked")
        DelegatorIf d = getStub((E) sender);
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

            FutureQueue<?> fq = engine.getOverlay().request(
                    new KeyRange<LATKey>(new LATKey(LATopic.topicMin(topic)),
                            new LATKey(LATopic.topicMax(topic))), (Object) m,
                    mesOpts);
            logger.debug("requested topic:" + topic + ", m=" + m + ",on " + engine.getHost() + ":" + engine.getPort());
            
            d.delegated((Endpoint) trans.getEndpoint(), tokenId, topic);

            for (RemoteValue<?> rv : fq) {
                /*
                 * response is ClusterId ClusterId cid =
                 * (ClusterId)rv.getValue(); if (closest == null ||
                 * closest.distance(cid) <
                 * closest.distance(engine.getClusterId())) { closest = cid; }
                 */
                if ((rv.getException()) != null) {
                    d.failed((Endpoint) trans.getEndpoint(), tokenId, topic,
                            MqException.REASON_CODE_UNEXPECTED_ERROR);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            d.failed((Endpoint) trans.getEndpoint(), tokenId, topic,
                    MqException.REASON_CODE_UNEXPECTED_ERROR);
        }
        d.succeeded((Endpoint) trans.getEndpoint(), tokenId, topic);
    }

    @Override
    public void delegated(Endpoint sender, int tokenId, String topic) {
        logger.debug("peer:" + engine.getPeerId() + " received delegated :"
                + topic);
    }

    @Override
    public void succeeded(Endpoint sender, int tokenId, String topic) {
        logger.debug("peer:" + engine.getPeerId() + " received succeeded :"
                + topic);
        engine.delegationSucceeded(tokenId, topic);
    }

    @Override
    public void failed(Endpoint sender, int tokenId, String topic,
            short reasonCode) {
        logger.debug("peer:" + engine.getPeerId() + " received failed");
    }
}
