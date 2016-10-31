package org.piqt.peer;

import io.moquette.parser.proto.messages.AbstractMessage.QOSType;

import java.util.ArrayList;
import java.util.List;

import net.arnx.jsonic.JSON;

public class Statistics {
    public class Subscription {
        public String topicFilter;
        public QOSType qos;
        public Subscription(String topicFilter, QOSType qos) {
            this.topicFilter = topicFilter;
            this.qos = qos;
        }
    }
    public class ClientStatistics {
        public String clientID;
        public List<Subscription> subscriptions;
        public boolean isActive;
        public boolean subscribed(String topicFilter) {
            for (Subscription sub : subscriptions) {
                if (sub.topicFilter.equals(topicFilter)) {
                    return true;
                }
            }
            return false;
        }
    }
    public long storedTopics;
    public long storedMessages;
    public long transferedMessages;
    public long publishedMessages;
    public long receivedMessagesFromPIAX;
    public List<ClientStatistics> clients;

    public Statistics() {
        init();
    }

    public Statistics(long st, long sm, long tm, long pm, long rmfp) {
        storedTopics = st;
        storedMessages = sm;
        transferedMessages = tm;
        publishedMessages = pm;
        receivedMessagesFromPIAX = rmfp;
        clients = new ArrayList<ClientStatistics>();
    }

    private void init() {
        storedTopics = 0;
        storedMessages = 0;
        transferedMessages = 0;
        publishedMessages = 0;
        receivedMessagesFromPIAX = 0;
        clients = new ArrayList<ClientStatistics>();
    }
    
    public boolean subscribed(String topicFilter) {
        for (ClientStatistics cs : clients) {
            for (Subscription sub : cs.subscriptions) {
                if (sub.topicFilter.equals(topicFilter)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public void subscribe(String clientId, String topicFilter, QOSType qos, boolean isActive) {
        boolean found = false;
        for (ClientStatistics cs : clients) {
            if (cs.clientID.equals(clientId)) {
                // XXX new qos is ignored
                if (!cs.subscribed(topicFilter)) {
                    cs.subscriptions.add(new Subscription(topicFilter, qos));
                }
                found = true;
            }
        }
        if (!found) {
            ClientStatistics cs = new ClientStatistics();
            cs.clientID = clientId;
            cs.subscriptions = new ArrayList<Subscription>();
            cs.subscriptions.add(new Subscription(topicFilter, qos));
            cs.isActive = isActive;
            clients.add(cs);
        }
        
    }
    
    public void unsubscribe(String clientId, String topicFilter) {
        for (ClientStatistics cs : clients) {
            if (cs.clientID.equals(clientId)) {
                cs.subscriptions.remove(topicFilter);
                return;
            }
        }
    }
    
    public void up(String clientId) {
        for (ClientStatistics cs : clients) {
            if (cs.clientID.equals(clientId)) {
                cs.isActive = true;
                return;
            }
        }
    }

    public void down(String clientId) {
        for (ClientStatistics cs : clients) {
            if (cs.clientID.equals(clientId)) {
                cs.isActive = false;
                return;
            }
        }
    }
    
    public String dump() {
        // stored topics
        int count = 0;
        for (ClientStatistics cs : clients) {
            count += cs.subscriptions.size();
        }
        storedTopics = count;
        
        String ret = JSON.encode(this);
        return ret;
    }
    
    static public void main(String[] args) {
        Statistics stats = new Statistics();
        stats.subscribe("x", "/a/b", QOSType.LEAST_ONE, true);
        System.out.println(stats.subscribed("/a/b"));
        System.out.println(stats.subscribed("a/b"));
        System.out.println(stats.dump());
    }

}