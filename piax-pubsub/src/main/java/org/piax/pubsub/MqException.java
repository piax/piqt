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
 * Thrown if an error occurs communicating with the server.
 */
public class MqException extends Exception {
    private static final long serialVersionUID = 300L;

    public static final short REASON_CODE_FAILED_AUTHENTICATION = 0x04;
    /** Not authorized to perform the requested operation */
    public static final short REASON_CODE_NOT_AUTHORIZED = 0x05;

    /** An unexpected error has occurred. */
    public static final short REASON_CODE_UNEXPECTED_ERROR = 0x06;

    public static final short REASON_CODE_INVALID_TOKEN = 0x07;
    public static final short REASON_SEED_NOT_AVAILABLE = 0x08;

    private int reasonCode;
    private Throwable cause;

    /**
     * Constructs a new <code>MqttException</code> with the specified code as
     * the underlying reason.
     * 
     * @param reasonCode
     *            the reason code for the exception.
     */
    public MqException(int reasonCode) {
        super();
        this.reasonCode = reasonCode;
    }

    /**
     * Constructs a new <code>MqttException</code> with the specified
     * <code>Throwable</code> as the underlying reason.
     * 
     * @param cause
     *            the underlying cause of the exception.
     */
    public MqException(Throwable cause) {
        super();
        this.reasonCode = REASON_CODE_UNEXPECTED_ERROR;
        this.cause = cause;
    }

    /**
     * Constructs a new <code>MqttException</code> with the specified
     * <code>Throwable</code> as the underlying reason.
     * 
     * @param reason
     *            the reason code for the exception.
     * @param cause
     *            the underlying cause of the exception.
     */
    public MqException(int reason, Throwable cause) {
        super();
        this.reasonCode = reason;
        this.cause = cause;
    }

    /**
     * Returns the reason code for this exception.
     * 
     * @return the code representing the reason for this exception.
     */
    public int getReasonCode() {
        return reasonCode;
    }

    /**
     * Returns the underlying cause of this exception, if available.
     * 
     * @return the Throwable that was the root cause of this exception, which
     *         may be <code>null</code>.
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns a <code>String</code> representation of this exception.
     * 
     * @return a <code>String</code> representation of this exception.
     */
    public String toString() {
        String result = getMessage() + " (" + reasonCode + ")";
        if (cause != null) {
            result = result + " - " + cause.toString();
        }
        return result;
    }
}
