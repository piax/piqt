/*
 * DelegatorCommand.java - An implementation of delegator command.
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub.stla;

import java.io.Serializable;

public class DelegatorCommand implements Serializable {
    /**
	 * 
	 */
    private static final long serialVersionUID = -8118232028344200034L;
    public String command;
    public String topic;

    public DelegatorCommand(String command, String topic) {
        this.command = command;
        this.topic = topic;
    }
}
