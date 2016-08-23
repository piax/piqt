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

import org.piqt.MqMessage;

public class MqMessageMoquette extends MqMessage {

    /**
	 * 
	 */
    private static final long serialVersionUID = -957783061937044610L;
    String peerId;

    public MqMessageMoquette(String topic) {
        super(topic);
    }

    public MqMessageMoquette(String topic, String peerId) {
        super(topic);
        this.peerId = peerId;
    }

    public String getPeerId() {
        return peerId;
    }
}
