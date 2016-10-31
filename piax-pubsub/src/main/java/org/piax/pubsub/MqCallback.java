/*
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.piax.pubsub;

/**
 * Enables an application to be notified when asynchronous events related to the
 * client occur. Classes implementing this interface can be registered on both
 * types of client: {@link MqEngine#setCallback(MqCallback)} and
 * {@link MqEngine#setCallback(MqCallback)}
 */
public interface MqCallback {

    /**
     * This method is called when a message arrives.
     *
     * @param subscribedTopic
     *            the subscribed topic filter which the message was received.
     * @param m
     *            message the actual message.
     * @throws Exception
     *             if an error has occurred, and the client should be shut down.
     */
    public void messageArrived(MqTopic subscribedTopic, MqMessage m)
            throws Exception;

    /**
     * Called when delivery for a message has been completed, and all
     * acknowledgments have been received. For QoS 0 messages it is called once
     * the message has been handed to the network for delivery. For QoS 1 it is
     * called when PUBACK is received and for QoS 2 when PUBCOMP is received.
     * The token will be the same token as that returned when the message was
     * published.
     *
     * @param token
     *            the delivery token associated with the message.
     */
    public void deliveryComplete(MqDeliveryToken token);

}
