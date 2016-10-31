/*
 * MqEngine.java - An interface of pub/sub engine.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub;

/**
 * Enables an application to communicate by pub/sub.
 * <p>
 * This interface allows applications to utilize all features of the MQTT
 * version 3.1 specification including:
 * </p>
 * <ul>
 * <li>connect
 * <li>publish
 * <li>subscribe
 * <li>unsubscribe
 * <li>disconnect
 * </ul>
 * 
 */
public interface MqEngine {
    /**
     * Connects to the system.
     * 
     * @throws MqException
     *             for some problems
     */
    void connect() throws MqException;

    /**
     * Disconnects from the system.
     * <p>
     * An attempt is made to quiesce the client allowing outstanding work to
     * complete before disconnecting. It will wait for a maximum of 30 seconds
     * for work to quiesce before disconnecting. This method must not be called
     * from inside {@link MqCallback} methods.
     * </p>
     * @throws MqException thrown when there is an error.
     */
    void disconnect() throws MqException;

    /**
     * Subscribe to a topic, which may include wildcards using a QoS of 1.
     *
     * @see #subscribe(String[], int[])
     *
     * @param topic
     *            the topic to subscribe to, which can include wildcards.
     * @throws MqException
     *             if there was an error registering the subscription.
     */
    void subscribe(String topic) throws MqException;

    /**
     * Subscribes to a one or more topics, which may include wildcards using a
     * QoS of 1.
     *
     * @see #subscribe(String[], int[])
     *
     * @param topics
     *            an array of topics to subscribe to, which can include wildcards.
     * @throws MqException
     *             if there was an error registering the subscription.
     */
    void subscribe(String[] topics) throws MqException;

    /**
     * Subscribes to multiple topics, each of which may include wildcards.
     * <p>
     * The {@link #setCallback(MqCallback)} method should be called before
     * this method, otherwise any received messages will be discarded.
     * </p>
     * <p>
     * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to true
     * when when connecting to the server then the subscription remains in place
     * until either:
     * </p>
     * <ul>
     * <li>The client disconnects</li>
     * <li>An unsubscribe method is called to un-subscribe the topic</li>
     * </ul>
     * 
     * <p>
     * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to false
     * when when connecting to the server then the subscription remains in place
     * until either:
     * </p>
     * <ul>
     * <li>An unsubscribe method is called to unsubscribe the topic</li>
     * <li>The client connects with cleanSession set to true</li>
     * </ul>
     * <p>
     * With cleanSession set to false the MQTT server will store messages
     * on behalf of the client when the client is not connected. The next time
     * the client connects with the <b>same client ID</b> the server will
     * deliver the stored messages to the client.
     * </p>
     *
     * <p>
     * The "topic filter" string used when subscribing may contain special
     * characters, which allow you to subscribe to multiple topics at once.
     * </p>
     * <p>
     * The topic level separator is used to introduce structure into the topic,
     * and can therefore be specified within the topic for that purpose. The
     * multi-level wildcard and single-level wildcard can be used for
     * subscriptions, but they cannot be used within a topic by the publisher of
     * a message.
     * </p>
     * <dl>
     * <dt>Topic level separator</dt>
     * <dd>The forward slash (/) is used to separate each level within a topic
     * tree and provide a hierarchical structure to the topic space. The use of
     * the topic level separator is significant when the two wildcard characters
     * are encountered in topics specified by subscribers.</dd>
     * <dt>Multi-level wildcard</dt>
     * <dd>The number sign (#) is a wildcard character that matches any number of
     * levels within a topic. For example, if you subscribe to <span><span
     * class="filepath">finance/stock/ibm/#</span></span>, you receive messages
     * on these topics:
     * <pre>
     * finance/stock/ibm<br>   finance/stock/ibm/closingprice<br>   finance/stock/ibm/currentprice
     * </pre>
     * <p>
     * The multi-level wildcard can represent zero or more levels. Therefore,
     * <em>finance/#</em> can also match the singular <em>finance</em>, where
     * <em>#</em> represents zero levels. The topic level separator is
     * meaningless in this context, because there are no levels to separate.
     * </p>
     * <p>
     * The <span>multi-level</span> wildcard can be specified only on its own or
     * next to the topic level separator character. Therefore, <em>#</em> and
     * <em>finance/#</em> are both valid, but <em>finance#</em> is not valid.
     * <span>The multi-level wildcard must be the last character used within the
     * topic tree. For example, <em>finance/#</em> is valid but
     * <em>finance/#/closingprice</em> is not valid.</span>
     * </p>
     * </dd>
     *
     * <dt>Single-level wildcard</dt>
     * <dd>
     * <p>
     * The plus sign (+) is a wildcard character that matches only one topic
     * level. For example, <em>finance/stock/+</em> matches
     * <em>finance/stock/ibm</em> and <em>finance/stock/xyz</em>, but not
     * <em>finance/stock/ibm/closingprice</em>. Also, because the single-level
     * wildcard matches only a single level, <em>finance/+</em> does not match
     * <em>finance</em>.
     * </p>
     *
     * <p>
     * Use the single-level wildcard at any level in the topic tree, and in
     * conjunction with the multilevel wildcard. Specify the single-level
     * wildcard next to the topic level separator, except when it is specified
     * on its own. Therefore, <em>+</em> and <em>finance/+</em> are both valid,
     * but <em>finance+</em> is not valid. <span>The single-level wildcard can
     * be used at the end of the topic tree or within the topic tree. For
     * example, <em>finance/+</em> and <em>finance/+/ibm</em> are both
     * valid.</span>
     * </p>
     * </dd>
     * </dl>
     *
     * <p>
     * This is a blocking method that returns once subscribe completes
     * </p>
     *
     * @param topics
     *            one or more topics to subscribe to, which can include
     *            wildcards.
     * @param qos
     *            the maximum quality of service to subscribe each topic at.
     *            Messages published at a lower quality of service will be
     *            received at the published QoS. Messages published at a higher
     *            quality of service will be received using the QoS specified on
     *            the subscribe.
     * @throws MqException
     *             if there was an error registering the subscription.
     * @throws IllegalArgumentException
     *             if the two supplied arrays are not the same size.
     */
    void subscribe(String[] topics, int[] qos) throws MqException;

    boolean subscribedTo(String topic);

    boolean subscriptionMatchesTo(String topic);

    /**
     * Requests the server unsubscribe the client from a topic.
     *
     * @see #unsubscribe(String[])
     * @param topic
     *            the topic to unsubscribe from. It must match a topicFilter
     *            specified on the subscribe.
     * @throws MqException
     *             if there was an error unregistering the subscription.
     */
    void unsubscribe(String topic) throws MqException;

    /**
     * Requests unsubscribe from one or more topics.
     * <p>
     * Unsubcribing is the opposite of subscribing. When the server receives the
     * unsubscribe request it looks to see if it can find a subscription for the
     * client and then removes it. After this point the server will send no more
     * messages to the client for this subscription.
     * </p>
     * <p>
     * The topic(s) specified on the unsubscribe must match the topic(s)
     * specified in the original subscribe request for the subscribe to succeed
     * </p>
     *
     * <p>
     * This is a blocking method that returns once unsubscribe completes
     * </p>
     *
     * @param topics
     *            one or more topics to unsubscribe from. Each topicFilter must
     *            match one specified on a subscribe
     * @throws MqException
     *             if there was an error unregistering the subscription.
     */
    void unsubscribe(String[] topics) throws MqException;

    /**
     * Publishes a message to a topic on the server and return once it is
     * delivered.
     * <p>
     * This is a convenience method, which will create a new {@link MqMessage}
     * object with a byte array payload and the specified QoS, and then publish
     * it. All other values in the message will be set to the defaults.
     * </p>
     *
     * @param topic
     *            to deliver the message to, for example "finance/stock/ibm".
     * @param payload
     *            the byte array to use as the payload
     * @param qos
     *            the Quality of Service to deliver the message at. Valid values
     *            are 0, 1 or 2.
     * @throws MqException
     *             for errors encountered while publishing the message. For
     *             instance client not connected.
     * @see MqMessage#setQos(int)
     * @see MqMessage#setRetained(boolean)
     */
    void publish(String topic, byte[] payload, int qos) throws MqException;

    /**
     * Publishes a message to a topic on the server.
     * <p>
     * Delivers a message to the server at the requested quality of service and
     * returns control once the message has been delivered. In the event the
     * connection fails or the client stops, any messages that are in the
     * process of being delivered will be delivered once a connection is
     * re-established to the server on condition that:
     * </p>
     * <ul>
     * <li>The connection is re-established with the same clientID
     * <li>The original connection was made with (@link
     * MqttConnectOptions#setCleanSession(boolean)} set to false
     * <li>The connection is re-established with (@link
     * MqttConnectOptions#setCleanSession(boolean)} set to false
     * </ul>
     * 
     * <p>
     * In the event that the connection breaks or the client stops it is still
     * possible to determine when the delivery of the message completes. Prior
     * to re-establishing the connection to the server:
     * </p>
     * <ul>
     * <li>Register a {@link #setCallback(MqCallback)} callback on the client
     * and the delivery complete callback will be notified once a delivery of a
     * message completes.
     * </ul>
     *
     * <p>
     * When building an application, the design of the topic tree should take
     * into account the following principles of topic name syntax and semantics:
     * </p>
     *
     * <ul>
     * <li>A topic must be at least one character long.</li>
     * <li>Topic names are case sensitive. For example, <em>ACCOUNTS</em> and
     * <em>Accounts</em> are two different topics.</li>
     * <li>Topic names can include the space character. For example,
     * <em>Accounts
     * 	payable</em> is a valid topic.</li>
     * <li>A leading "/" creates a distinct topic. For example,
     * <em>/finance</em> is different from <em>finance</em>. <em>/finance</em>
     * matches "+/+" and "/+", but not "+".</li>
     * <li>Do not include the null character (Unicode<em>\x0000</em>) in any topic.</li>
     * </ul>
     *
     * <p>
     * The following principles apply to the construction and content of a topic
     * tree:
     * </p>
     *
     * <ul>
     * <li>The length is limited to 64k but within that there are no limits to
     * the number of levels in a topic tree.</li>
     * <li>There can be any number of root nodes; that is, there can be any
     * number of topic trees.</li>
     * </ul>
     *
     * <p>
     * This is a blocking method that returns once publish completes
     * </p>
     *
     * @param m
     *            to delivery to the server
     * @throws MqException
     *             for errors encountered while publishing the message. For
     *             instance client not joined to the network.
     */
    void publish(MqMessage m) throws MqException;

    MqDeliveryToken publishAsync(MqMessage m) throws MqException;

    /**
     * Sets the callback listener to use for events that happen asynchronously.
     * <p>
     * There are a number of events that listener will be notified about. These
     * include
     * </p>
     * <ul>
     * <li>A new message has arrived and is ready to be processed</li>
     * <li>The connection to the server has been lost</li>
     * <li>Delivery of a message to the server has completed.</li>
     * </ul>
     * 
     * <p>
     * Other events that track the progress of an individual operation such as
     * connect and subscribe can be tracked using the {@link MqToken} passed
     * to the operation
     * </p>
     * 
     * @see MqCallback
     * @param callback
     *            the class to callback when for events related to the client
     */
    void setCallback(MqCallback callback);

    /**
     * Determines if this client is currently connected to the server.
     *
     * @return <code>true</code> if connected, <code>false</code> otherwise.
     */
    boolean isConnected();

    /**
     * Finalize the engine. Releases all resource associated with the engine.
     * After the engine has been finalized, it cannot be reused. For instance,
     * attempts to join will fail.
     * 
     * @throws MqException
     *             if the engine is not disconnected.
     */
    void fin() throws MqException;
}
