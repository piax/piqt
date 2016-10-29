/*
 * LATopic.java - The topic of STLA
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub.stla;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LATopic implements Comparable<Object>, Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 3846105094656925646L;
	String topic;
    ClusterId cluster;
    String id;
    Const cedge;
    Const edge;
    
    public static boolean SORT_BY_CLUSTER = true;
    
    enum Const {NONE, MIN, MAX, CMIN, CMAX};
    
    public LATopic(String topic) {
        this.topic = topic;
        this.cluster = new ClusterId("");
        this.id = "";
        this.cedge = Const.NONE;
        this.edge = Const.NONE;
    }
    
    public LATopic topicMin() {
    		LATopic t = new LATopic(this.topic);
        t.cluster = new ClusterId("");
        t.cedge = Const.CMAX;
        t.edge = Const.MIN;
        return t;
    }
    
    public LATopic topicMax() {
    		LATopic t = new LATopic(this.topic);
        t.cluster = new ClusterId("");
        t.cedge = Const.CMIN;
        t.edge = Const.MAX;
        return t;
    }
    
    static public LATopic topicMin(String topic) {
    	LATopic t = new LATopic(topic);
        t.cluster = new ClusterId("");
        t.cedge = Const.CMAX;
        t.edge = Const.MIN;
        return t;
    }
    static public LATopic topicMax(String topic) {
        LATopic t = new LATopic(topic);
        t.cluster = new ClusterId("");
        t.cedge = Const.CMIN;
        t.edge = Const.MAX;
        return t;
    }
    
    static public LATopic clusterMin(LATopic orig) {
        LATopic t = new LATopic(orig.topic);
        t.cluster = new ClusterId("");
        t.edge = orig.edge;
        t.id = orig.id;
        t.cedge = Const.CMIN;
        return t;
    }
    
    static public LATopic clusterMax(LATopic orig) {
        LATopic t = new LATopic(orig.topic);
        t.cluster = new ClusterId("");
        t.edge = orig.edge;
        t.id = orig.id;
        t.cedge = Const.CMAX;
        return t;
    }

    public boolean clusterDiffers(LATopic topic) {
        if (topic == null) return false;
        if (this.cluster != null && this.cluster.equals(topic.cluster)) {
            return false;
        }
        return true;
    }
    
    public boolean clusterIncluded(List<LATopic> list) {
        for (LATopic t : list) {
            if (clusterDiffers(t)) {
                return false;
            }
        }
        return true;
    }
    
    public void setClusterId(ClusterId cluster) {
        this.cluster = cluster;
    }
    
    public ClusterId getClusterId() {
        return this.cluster;
    }
    public boolean isMin() {
        return (this.edge == Const.MIN);
    }
    public boolean isMax() {
        return (this.edge == Const.MAX);
    }
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTopic() {
        return topic;
    }

	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	@Override
    public boolean equals(Object o) {
		LATopic arg = (LATopic)o;
		return topic.equals(arg.topic);
	}

	@Override
    public int compareTo(Object o) {
		if (!(o instanceof LATopic)) {
			return -1;
		}
		LATopic arg = (LATopic)o;
		int ret = topic.compareTo(arg.topic);
		if (ret != 0) {
			return ret;
		}
		else {
            switch (edge) {
            case MIN:
                if (arg.edge == Const.MIN) {
                    return 0;
                }
                return -1;
            case MAX:
                if (arg.edge == Const.MAX) {
                    return 0;
                }
                return 1;
            default:
                // not reached.
            }
            switch (cedge) {
            case CMIN:
                if (SORT_BY_CLUSTER) {
                    if (cluster.equals(arg.cluster)) {
                        if (arg.cedge == Const.CMIN) {
                            return 0;
                        }
                        return -1;
                    }
                }
                // continue to default:
            case CMAX:
                if (SORT_BY_CLUSTER) {
                    if (cluster.equals(arg.cluster)) {
                        if (arg.cedge == Const.CMAX) {
                            return 0;
                        }
                        return 1;
                    }
                }
                // continue
            default:
                switch (arg.edge) {
                case MIN:
                    if (edge == Const.MIN) {
                        return 0;
                    }
                    return 1;
                case MAX:
                    if (edge == Const.MAX) {
                        return 0;
                    }
                    return -1;
                default:
                    // not reached here.
                }
                switch (arg.cedge) {
                case CMIN:
                    if (SORT_BY_CLUSTER) {
                        if (cluster.equals(arg.cluster)) {
                            if (cedge == Const.CMIN) {
                                return 0;
                            }
                            return 1;
                        }
                    }
                    // continue to default:
                case CMAX:
                    if (SORT_BY_CLUSTER) {
                        if (cluster.equals(arg.cluster)) {
                            if (cedge == Const.CMAX) {
                                return 0;
                            }
                            return -1;
                        }
                    }
                    // continue to default:
                default:
                    if (SORT_BY_CLUSTER) {
                        ret = cluster.compareTo(arg.cluster);
                        if (ret != 0) {
                            return ret;                    
                        }
                    }
                    return id.compareTo(arg.id);
                }
            }
        }
    }
    
    String edgeToString(Const c) {
        switch (c) {
        case MIN:
            return "MIN";
        case MAX:
            return "MAX";
        case CMIN:
            return "CMIN";
        case CMAX:
            return "CMAX";
        case NONE:
            return "";
        }
        return "??";
    }
    
    public String toString() {
        if (edge == Const.NONE && cedge == Const.NONE) {
            return "(" + topic + "|" + cluster + "|" + id + ")";
        }
        else {
            return "(" + topic + "|" + (cluster == null? "" : cluster) + "|" + edgeToString(cedge) + "/" + edgeToString(edge) + ")";
        }
    }

}
