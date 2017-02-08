/*
 * LATKey.java - PIAX version of LATK.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub.stla;

import org.piax.common.wrapper.WrappedComparableKeyImpl;

public class LATKey extends WrappedComparableKeyImpl<LATopic> {
    private static final long serialVersionUID = 3617209391617791819L;
    LATopic key;
    
    public LATKey(LATopic key) {
        super(key);
        this.key = key;
    }
    
    @Override
    public int hashCode() {
        return (key.getTopic() + key.getClusterId()).hashCode();
    }
}
