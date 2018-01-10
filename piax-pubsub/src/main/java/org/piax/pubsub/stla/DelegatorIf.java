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

import org.piax.pubsub.stla.Delegator.ControlMessage;

public interface DelegatorIf {
    void delegate(ControlMessage c);
    void delegated(ControlMessage c);
    void succeeded(ControlMessage c);
    void failed(ControlMessage c);
}
