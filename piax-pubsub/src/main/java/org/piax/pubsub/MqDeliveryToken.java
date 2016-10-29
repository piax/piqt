/*
 * MqDeliveryToken.java - An interface of delivery token.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub;

/**
 * Provides a mechanism for tracking the delivery of a message.
 * 
 * <p>
 * A subclass of MessageToken that allows the delivery of a message to be
 * tracked. Unlike instances of MessageToken delivery tokens can be used across
 * connection and client restarts. This enables the delivery of a messages to be
 * tracked after failures. There are two approaches
 * </p>
 * <p>
 * An action is in progress until either:
 * </p>
 * <ul>
 * <li>isComplete() returns true or
 * <li>getException() is not null. If a client shuts down before delivery is
 * complete. an exception is returned. As long as the Java Runtime is not
 * stopped a delivery token is valid across a connection disconnect and
 * reconnect. In the event the client is shut down the getPendingDeliveryTokens
 * method can be used once the client is restarted to obtain a list of delivery
 * tokens for inflight messages.
 * </ul>
 * 
 */

public interface MqDeliveryToken extends MqToken {
    /**
     * Returns the message associated with this token.
     * <p>
     * Until the message has been delivered, the message being delivered will be
     * returned. Once the message has been delivered <code>null</code> will be
     * returned.
     * 
     * @return the message associated with this token or null if already
     *         delivered.
     */
    public MqMessage getMessage();
}
