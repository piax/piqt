/*
 * MqToken.java - An interface of token.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub;

/**
 * Provides a mechanism for tracking the completion of an asynchronous task.
 *
 * A MessageToken is used to track the state of the operation. An application
 * can use the token to wait for an operation to complete. A token is passed to
 * callbacks once the operation completes and provides context linking it to the
 * original request. A token is associated with a single operation.
 * <p>
 * An action is in progress until either:
 * </p>
 * <ul>
 * <li>isComplete() returns true or
 * <li>getException() is not null.
 * </ul>
 * 
 *
 */
public interface MqToken {

    /**
     * Blocks the current thread until the action this token is associated with
     * has completed.
     *
     * @throws MqException
     *             if there was a problem with the action associated with the
     *             token.
     * @see #waitForCompletion(long)
     */
    public void waitForCompletion() throws MqException;

    /**
     * Blocks the current thread until the action this token is associated with
     * has completed.
     * <p>
     * The timeout specifies the maximum time it will block for. If the action
     * completes before the timeout then control returns immediately, if not it
     * will block until the timeout expires.
     * </p>
     * <p>
     * If the action being tracked fails or the timeout expires an exception
     * will be thrown. In the event of a timeout the action may complete after
     * timeout.
     * </p>
     *
     * @param timeout
     *            the maximum amount of time to wait for, in milliseconds.
     * @throws MqException
     *             if there was a problem with the action associated with the
     *             token.
     */
    public void waitForCompletion(long timeout) throws MqException;

    /**
     * Returns whether or not the action has finished.
     * <p>
     * True will be returned both in the case where the action finished
     * successfully and in the case where it failed. If the action failed
     * {@link #getException()} will be non null.
     * </p>
     * @return true if completed.
     */
    public boolean isComplete();

    /**
     * Returns an exception providing more detail if an operation failed.
     * <p>
     * While an action in in progress and when an action completes successfully
     * null will be returned. Certain errors like timeout or shutting down will
     * not set the exception as the action has not failed or completed at that
     * time
     * </p>
     * 
     * @return exception may return an exception if the operation failed. Null
     *         will be returned while action is in progress and if action
     *         completes successfully.
     */
    public MqException getException();

    /**
     * Register a listener to be notified when an action completes.
     * <p>
     * Once a listener is registered it will be invoked when the action the
     * token is associated with either succeeds or fails.
     * </p>
     * 
     * @param listener
     *            to be invoked once the action completes
     */
    public void setActionCallback(MqActionListener listener);

    /**
     * Return the async listener for this token.
     * 
     * @return listener that is set on the token or null if a listener is not
     *         registered.
     */
    public MqActionListener getActionCallback();

    /**
     * Returns the topic string(s) for the action being tracked by this token.
     * If the action has not been initiated or the action has not topic
     * associated with it such as connect then null will be returned.
     *
     * @return the topic string(s) for the subscribe being tracked by this token
     *         or null
     */
    public String[] getTopics();

    /**
     * Store some context associated with an action.
     * <p>
     * Allows the caller of an action to store some context that can be accessed
     * from within the ActionListener associated with the action. This can be
     * useful when the same ActionListener is associated with multiple actions
     * </p>
     * 
     * @param userContext
     *            to associate with an action
     */
    public void setUserContext(Object userContext);

    /**
     * Retrieve the context associated with an action.
     * <p>
     * Allows the ActionListener associated with an action to retrieve any
     * context that was associated with the action when the action was invoked.
     * If not context was provided null is returned.
     * </p>
     * 
     * @return Object context associated with an action or null if there is
     *         none.
     */
    public Object getUserContext();

    /**
     * Returns the message ID of the message that is associated with the token.
     * A message id of zero will be returned for tokens associated with connect,
     * disconnect and ping operations as there can only ever be one of these
     * outstanding at a time. For other operations the message id flowed over
     * the network.
     * @return the message ID.
     */
    public int getMessageId();

}
