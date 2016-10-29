/*
 * DelegatorIf.java - An interface for RPC on PIAX.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub.stla;

import java.io.Serializable;

import org.piax.common.Endpoint;
import org.piax.gtrans.RPCIf;
import org.piax.gtrans.RemoteCallable;
import org.piax.gtrans.RemoteCallable.Type;

/*
 * RPC interface for Delegator.  
 */
public interface DelegatorIf extends RPCIf {
    @RemoteCallable(Type.ONEWAY)
    void delegate(Endpoint sender, int tokenId, String topic,
            Serializable message);

    // responses
    @RemoteCallable(Type.ONEWAY)
    void delegated(Endpoint sender, int tokenId, String topic);

    @RemoteCallable(Type.ONEWAY)
    void succeeded(Endpoint sender, int tokenId, String topic);

    @RemoteCallable(Type.ONEWAY)
    void failed(Endpoint sender, int tokenId, String topic, short reasonCode);
}
