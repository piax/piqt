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
import org.piax.common.Option.BooleanOption;
import org.piax.common.TransportId;
import org.piax.common.subspace.KeyRange;
import org.piax.gtrans.IdConflictException;
import org.piax.gtrans.RequestTransport.Response;
import org.piax.gtrans.TransOptions;
import org.piax.gtrans.TransOptions.ResponseType;
import org.piax.gtrans.TransOptions.RetransMode;
import org.piax.gtrans.Transport;
import org.piax.pubsub.MqException;
import org.piax.pubsub.MqMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Delegator<E extends Endpoint> {
    private static final Logger logger = LoggerFactory
            .getLogger(Delegator.class);
    static TransportId rpcId = new TransportId("pmqr");
    final PeerMqEngine engine;
    BooleanOption FOLLOW_DELEGATOR_SUBSCRIPTION = new BooleanOption(true, "-follow-delegator-sub");  
    
    static public class ControlMessage implements Serializable {
        public int tokenId;
        public String kString;
        public Endpoint source;
        public Serializable content;
        public short reasonCode;
        public ControlMessage(Endpoint source, int tokenId, String kString, Serializable content, short reasonCode) {
            super();
            this.tokenId = tokenId;
            this.kString = kString;
            this.source = source;
            this.content = content;
            this.reasonCode = reasonCode;
        }
        public ControlMessage(Endpoint source, int tokenId, String kString, short reasonCode) {
            super();
            this.tokenId = tokenId;
            this.kString = kString;
            this.source = source;
            this.reasonCode = reasonCode;
        }
        public ControlMessage(Endpoint source, int tokenId, String kString, Serializable content) {
            super();
            this.tokenId = tokenId;
            this.kString = kString;
            this.source = source;
            this.content = content;
            this.reasonCode = (short) -1;
        }
        public ControlMessage(Endpoint source, int tokenId, String kString) {
            super();
            this.tokenId = tokenId;
            this.kString = kString;
            this.source = source;
            this.content = null;
            this.reasonCode = (short) -1;
        }
    }

    public Delegator(PeerMqEngine engine) throws IdConflictException,
            IOException {
        this.engine = engine;
    }

    /*
     * deliver messages to the kString region from the local engine.
     * the result is notified to the sender of controlmessage.
     * 
     * @see org.piax.pubsub.stla.DelegatorIf#delegate(org.piax.pubsub.stla.Delegator.ControlMessage)
     */
    @SuppressWarnings("unchecked")
    public void delegate(ControlMessage c) {
        Endpoint sender = c.source;
        int tokenId = c.tokenId;
        String kString = c.kString;
        Serializable message = c.content;
        logger.debug("peer {} starting dissemination for kString:{}", engine.getPeerId(), kString);
        Transport<Endpoint> trans =((Transport<Endpoint>)engine.getOverlay().getLowerTransport());
        
        if (FOLLOW_DELEGATOR_SUBSCRIPTION.value()) {
            if (!engine.isJoined(kString)) { // Not joined anymore
                logger.debug("delegated but not joined to {} anymore", kString);
                trans.sendAsync(
                        new TransportId("failed"),
                        sender,
                        new ControlMessage(trans.getEndpoint(),
                                tokenId,
                                kString,
                                null, MqException.REASON_NOT_SUBSCRIBED)
                        );
                return;
            }
        }
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
                    new KeyRange<LATKey>(new LATKey(LATopic.topicMin(kString)),
                            new LATKey(LATopic.topicMax(kString))), (Object) m,
                    (res, ex)->{
                        logger.debug("dissemination requestAsync received: {}", res);
                        if (Response.EOR.equals(res)) {
                            logger.debug("EOR. dissemination for requestAsync for kString:{} qos={} finished:", kString, m.getQos());
                            trans.sendAsync(
                                    new TransportId("succeeded"),
                                    sender,
                                    new ControlMessage(trans.getEndpoint(),
                                            tokenId,
                                            kString)
                                    );
                        }
                        else if (ex != null) {
                            logger.debug("requestAsync for kString:{} failed:", kString, ex);
                            trans.sendAsync(
                                    new TransportId("failed"),
                                    sender,
                                    new ControlMessage(trans.getEndpoint(),
                                            tokenId,
                                            kString,
                                            null, MqException.REASON_CODE_UNEXPECTED_ERROR)
                                    );
                        }
                    }, mesOpts);
            logger.debug("requested topic:" + kString + ", m=" + m + ",on " + engine.getHost() + ":" + engine.getPort());
            trans.sendAsync(
                    new TransportId("delegated"),
                    sender,
                    new ControlMessage(trans.getEndpoint(),
                            tokenId,
                            kString)
                    );
        } catch (Exception e) {
            e.printStackTrace();
            trans.sendAsync(
                    new TransportId("failed"),
                    sender,
                    new ControlMessage(trans.getEndpoint(),
                            tokenId,
                            kString,
                            null, MqException.REASON_CODE_UNEXPECTED_ERROR)
                    );
        }
        
    }

    public void delegated(ControlMessage c) {
        String topic = c.kString;
        logger.debug("peer:" + engine.getPeerId() + " received delegated :"
                + topic);
    }

    public void succeeded(ControlMessage c) {
        int tokenId = c.tokenId;
        String topic = c.kString;
            logger.debug("peer:" + engine.getPeerId() + " received succeeded :"
                + topic);
        engine.delegationSucceeded(tokenId, topic);
    }

    public void failed(ControlMessage c) {
        String kString = c.kString;
        // if recoverable, let the delivery token to retry for delivering the kString.
        if (c.reasonCode == MqException.REASON_NOT_SUBSCRIBED) { // subscription of delegator changed.
            int tokenId = c.tokenId;
            engine.delegationRetry(c.tokenId, kString);
            return;
        }
        // unrecoverable error. just remove and mark as finished. 
        engine.removeDelegator(kString);
        engine.delegationFailed(c.tokenId, kString, new MqException(c.reasonCode));
        
        logger.debug("peer:" + engine.getPeerId() + " received failed");
    }
}
