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

class DeliveryDelegator extends NearestDelegator {
    Boolean succeeded;
    Exception e;

    public DeliveryDelegator(Endpoint ep, String topic) {
        super(ep, topic);
        e = null;
        succeeded = null;
    }

    public DeliveryDelegator(String topic) {
        super(null, topic);
        e = null;
        succeeded = null;
    }
    
    public DeliveryDelegator(NearestDelegator nd) {
        super(nd.getEndpoint(), nd.getKeyString());
        succeeded = null;
    }
    
    public boolean isFinished() {
        return succeeded != null;
    }

    public void setSucceeded() {
        succeeded = true;
    }

    public void setFailured(Exception e) {
        succeeded = false;
        this.e = e;
    }

    public String toString() {
        return ("kString=" + kString + ",succeeded=" + succeeded);
    }
}
