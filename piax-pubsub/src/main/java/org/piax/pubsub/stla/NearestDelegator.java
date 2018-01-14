package org.piax.pubsub.stla;

import org.piax.common.Endpoint;

public class NearestDelegator {
    protected Endpoint endpoint;
    final protected String kString;

    public NearestDelegator(Endpoint endpoint, String topic) {
        this.endpoint = endpoint;
        this.kString = topic;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public String getKeyString() {
        return kString;
    }
    
    public String toString() {
        return "delegator:" + (endpoint == null ? "SELF" : endpoint) + "," + kString;
    }
}
