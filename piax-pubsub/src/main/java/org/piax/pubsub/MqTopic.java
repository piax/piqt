/*
 * MqTopic.java - A topic parser
 * 
 * Copyright (c) 2016 PIAX development team
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piax.pubsub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MqTopic {
    private List<Object> topicPath;
    private String subscriberKey = null; // cache

    enum Wildcard {
        SINGLE, MULTI
    }

    private String specified = "";

    public String getSpecified() {
        return specified;
    }

    public void setSpecified(String specified) {
        this.specified = specified;
    }

    public MqTopic(String topic) throws MqException {
        if (topic == null) {// || topic.length() == 0) {
            throw new MqException(MqException.REASON_CODE_INVALID_TOKEN);
        }
        specified = topic;
        parseTopic(topic);
    }

    public List<Object> getTopicPath() {
        return topicPath;
    }

    private void parseTopic(String topic) throws MqException {
        topicPath = new ArrayList<Object>();
        String paths[] = topic.split("/");
        boolean multiWild = false;
        for (String path : paths) {
            if (multiWild) {
                throw new MqException(MqException.REASON_CODE_INVALID_TOKEN);
            }
            if ("+".equals(path)) {
                topicPath.add(Wildcard.SINGLE);
            } else if ("#".equals(path)) {
                topicPath.add(Wildcard.MULTI);
                multiWild = true;
            } else {
                if (path.contains("+") || path.contains("#"))
                    throw new MqException(MqException.REASON_CODE_INVALID_TOKEN);
                else
                    topicPath.add(path);
            }
        }
    }

    public String[] getPublisherKeyStrings() throws MqException {
        // naive method:
        // sport => "" and "sport"
        // /sport => "" and "/sport"
        // sport/tennis/player => "", "sport", "sport/tennis" and
        // "sport/tennis/player"
        List<String> keys = new ArrayList<String>();
        String keyStr = "";
        keys.add(keyStr);
        for (Object path : topicPath) {
            if (!(path instanceof String)) {
                throw new MqException(MqException.REASON_CODE_INVALID_TOKEN);
            }
            keyStr += path;
            if (((String) path).length() != 0) {
                keys.add(keyStr);
            }
            keyStr += "/";
        }
        return keys.toArray(new String[0]);
    }

    public String getSubscriberKeyString() {
        // + => ""
        // # => ""
        // abc => "abc"
        // +/+/+ => ""
        // abc/# => "abc"
        // /finance/+ => "/finance"
        // sport/tennis => "sport/tennis"
        // sport/tennis/+ => "sport/tennis"
        // sport/+/player1 => "sport"
        if (subscriberKey == null) {
            String keyStr = "";
            int i = 0;
            while (i < topicPath.size()) {
                Object path = topicPath.get(i);
                if (path instanceof String) {
                    keyStr += path;
                } else {
                    break;
                }
                if (topicPath.size() > i + 1
                        && (topicPath.get(i + 1) instanceof String)) {
                    keyStr += "/";
                }
                i++;
            }
            subscriberKey = keyStr;
            return keyStr;
        }
        return subscriberKey;
    }

    public boolean matchesToTopic(String topic) throws MqException {
        List<Object> targetPath = new MqTopic(topic).getTopicPath();
        int i = 0;
        while (i < targetPath.size()) {
            Object tPath = targetPath.get(i);
            if (topicPath.size() > i) {
                Object fPath = topicPath.get(i);
                if (!(tPath instanceof String)) {
                    throw new MqException(MqException.REASON_CODE_INVALID_TOKEN);
                }
                if (fPath.equals(Wildcard.SINGLE) || fPath.equals(tPath)) {
                    // OK
                    i++;
                    continue;
                }
                if (fPath.equals(Wildcard.MULTI)) {
                    return true;
                }
                return false;
            } else {
                return false;
            }
        }
        return topicPath.size() == i || (topicPath.get(i) == Wildcard.MULTI);
    }

    public String toString() {
        String ret = "";
        for (Object n : topicPath) {
            ret += "[" + n + "]";
        }
        return ret;
    }

    public static void main(String args[]) {
        try {
            new MqTopic("sport/#").matchesToTopic("sport");
            System.out.println(new MqTopic("+"));
            System.out.println(new MqTopic("+/tennis/#"));
            System.out.println(new MqTopic("sport/+/player1"));
            System.out.println(new MqTopic("/finance"));
            System.out.println(Arrays.toString(new MqTopic("sport")
                    .getPublisherKeyStrings()));
            System.out.println(Arrays.toString(new MqTopic("/sport")
                    .getPublisherKeyStrings()));
            System.out.println(Arrays.toString(new MqTopic("sport/tennis")
                    .getPublisherKeyStrings()));
            System.out.println(Arrays.toString(new MqTopic(
                    "/sport/tennis/player1").getPublisherKeyStrings()));
            System.out.println(Arrays.toString(new MqTopic(
                    "sport/tennis/player1").getPublisherKeyStrings()));
            System.out.println("'"
                    + new MqTopic("+/tennis/#").getSubscriberKeyString() + "'");
            System.out
                    .println("'"
                            + new MqTopic("/+/tennis/#")
                                    .getSubscriberKeyString() + "'");
            System.out.println("'"
                    + new MqTopic("sport/+/tennis/#").getSubscriberKeyString()
                    + "'");
            System.out.println("'"
                    + new MqTopic("/sport/+/tennis/#").getSubscriberKeyString()
                    + "'");
            System.out.println("'"
                    + new MqTopic("sport/tennis").getSubscriberKeyString()
                    + "'");
            System.out.println("'"
                    + new MqTopic("sport/tennis/+").getSubscriberKeyString()
                    + "'");
            System.out.println("'"
                    + new MqTopic("/sport/tennis/#").getSubscriberKeyString()
                    + "'");
            System.out.println("true?"
                    + ((new MqTopic("/sport/tennis/#"))
                            .matchesToTopic("/sport/tennis/player")));
            System.out.println("true?"
                    + ((new MqTopic("/sport/tennis/#"))
                            .matchesToTopic("/sport/tennis")));
            System.out.println("true?"
                    + ((new MqTopic("/sport/tennis/+"))
                            .matchesToTopic("/sport/tennis/player")));
            System.out.println("false?"
                    + ((new MqTopic("/sport/+"))
                            .matchesToTopic("/sport/tennis/player")));
            System.out.println("true?"
                    + ((new MqTopic("sport/tennis"))
                            .matchesToTopic("sport/tennis")));
            System.out.println("true?"
                    + ((new MqTopic("sport/#")).matchesToTopic("sport")));
            System.out.println("true?"
                    + ((new MqTopic("#")).matchesToTopic("sport")));
            System.out.println("true?"
                    + ((new MqTopic("/#")).matchesToTopic("/sport")));
            System.out.println("true?"
                    + ((new MqTopic("#")).matchesToTopic("/sport")));
            System.out.println("true?"
                    + ((new MqTopic("#"))
                            .matchesToTopic("/sport/tennis/player")));
            System.out.println("true?"
                    + ((new MqTopic("+/#"))
                            .matchesToTopic("/sport/tennis/player")));
            System.out.println("false?"
                    + ((new MqTopic("+")).matchesToTopic("/sport")));
            System.out.println("true?"
                    + (new MqTopic("sport/#").matchesToTopic("sport")));
        } catch (MqException e) {
            e.printStackTrace();
        }
        try {
            System.out.println(new MqTopic("sport+"));
        } catch (MqException e) {
            System.out.println("sport+ is an error"); // reaches here.
        }
        try {
            System.out.println(new MqTopic("/#/+"));
        } catch (MqException e) {
            System.out.println("/#/+ is an error"); // reaches here.
        }

    }
}
