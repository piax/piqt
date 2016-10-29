/*
 * MqActionListener.java - An interface for action listener.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub;

/**
 * Implementors of this interface will be notified when an asynchronous action
 * completes.
 * 
 * <p>
 * A listener is registered on an MqttToken and a token is associated with an
 * action like connect or publish. When used with tokens on the MqttAsyncClient
 * the listener will be called back on the MQTT client's thread. The listener
 * will be informed if the action succeeds or fails. It is important that the
 * listener returns control quickly otherwise the operation of the MQTT client
 * will be stalled.
 * </p>
 */
public interface MqActionListener {
    /**
     * This method is invoked when an action has completed successfully.
     * 
     * @param asyncActionToken
     *            associated with the action that has completed
     */
    public void onSuccess(MqToken asyncActionToken);

    /**
     * This method is invoked when an action fails. If a client is disconnected
     * while an action is in progress onFailure will be called. For connections
     * that use cleanSession set to false, any QoS 1 and 2 messages that are in
     * the process of being delivered will be delivered to the requested quality
     * of service next time the client connects.
     * 
     * @param asyncActionToken
     *            associated with the action that has failed
     * @param exception
     *            the exception of the failure.
     */
    public void onFailure(MqToken asyncActionToken, Throwable exception);
}
