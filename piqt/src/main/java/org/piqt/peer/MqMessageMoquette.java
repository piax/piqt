/*
 * MqMessageMoquette.java - A message implementation for moquette.
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.peer;

import org.piax.common.PeerId;
import org.piax.pubsub.MqMessage;

public class MqMessageMoquette extends MqMessage {

    /**
	 * 
	 */
    private static final long serialVersionUID = -957783061937044610L;
    PeerId peerId;
    String clientId;

    public MqMessageMoquette(String topic) {
        super(topic);
    }

    public MqMessageMoquette(String topic, PeerId peerId, String clientId) {
        super(topic);
        this.peerId = peerId;
        this.clientId = clientId;
    }

    public PeerId getPeerId() {
        return peerId;
    }
    
    public String getClientId() {
        return clientId;
    }
}
