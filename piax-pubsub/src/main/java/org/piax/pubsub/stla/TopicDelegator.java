/*
 * TopicDelegator.java - A delegator entry for a topic.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub.stla;

import org.piax.common.Endpoint;

class TopicDelegator {
    public Endpoint endpoint;
    public String topic;
    public ClusterId cid;
    public boolean succeeded;

    public TopicDelegator(Endpoint endpoint, String topic) {
        this.endpoint = endpoint;
        this.topic = topic;
        this.cid = null;
        this.succeeded = false;
    }

    public TopicDelegator(Endpoint endpoint, String topic, ClusterId cid) {
        this.endpoint = endpoint;
        this.topic = topic;
        this.cid = cid;
        this.succeeded = false;
    }
}
