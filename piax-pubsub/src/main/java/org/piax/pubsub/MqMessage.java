/*
 * MqMessage.java - A message implementation.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub;

import java.io.Serializable;

public class MqMessage implements Serializable {
    /**
	 * 
	 */
    private static final long serialVersionUID = -4783439070342174271L;
    private String topic;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    private boolean mutable = true;
    private byte[] payload;
    private int qos = 1;
    private boolean retained = false;
    private boolean dup = false;

    public static void validateQos(int qos) {
        if ((qos < 0) || (qos > 2)) {
            throw new IllegalArgumentException();
        }
    }

    public MqMessage(String topic) {
        setTopic(topic);
        setPayload(new byte[] {});
    }

    public MqMessage(String topic, byte[] payload) {
        setTopic(topic);
        setPayload(payload);
    }

    public byte[] getPayload() {
        return payload;
    }

    public void clearPayload() {
        checkMutable();
        this.payload = new byte[] {};
    }

    public void setPayload(byte[] payload) {
        checkMutable();
        if (payload == null) {
            throw new NullPointerException();
        }
        this.payload = payload;
    }

    public boolean isRetained() {
        return retained;
    }

    public void setRetained(boolean retained) {
        checkMutable();
        this.retained = retained;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        checkMutable();
        validateQos(qos);
        this.qos = qos;
    }

    public String toString() {
        return new String(payload);
    }

    protected void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

    protected void checkMutable() throws IllegalStateException {
        if (!mutable) {
            throw new IllegalStateException();
        }
    }

    protected void setDuplicate(boolean dup) {
        this.dup = dup;
    }

    public boolean isDuplicate() {
        return this.dup;
    }
}
