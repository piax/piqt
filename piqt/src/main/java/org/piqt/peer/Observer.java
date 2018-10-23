package org.piqt.peer;

import static org.piqt.peer.Util.*;

import java.util.List;

import org.piax.pubsub.MqException;
import org.piax.pubsub.MqMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptDisconnectMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.interception.messages.InterceptSubscribeMessage;
import io.moquette.interception.messages.InterceptUnsubscribeMessage;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.netty.buffer.ByteBuf;

public class Observer implements InterceptHandler, PeerHandler, SessionsStoreHandler {

    private static final Logger logger = LoggerFactory.getLogger(Observer.class
            .getPackage().getName());

    // PeerMqEngine engine;
    PeerMqEngineMoquette engine;
    Statistics stats;

    public Observer(PeerMqEngineMoquette engine) {
        super();
        Thread th = Thread.currentThread();
        logger.debug("PeerMqObserver thread=" + th);
        this.engine = engine;
        stats = new Statistics();
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
        logger.info("clientID=" + msg.getClientID());
        stats.up(msg.getClientID());
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
        logger.info("clientID=" + msg.getClientID());
        stats.down(msg.getClientID());
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        logger.info("topic=" + msg.getTopicName() + " qos=" + msg.getQos());
        ByteBuf buf = msg.getPayload();
        byte[] bytes;
        int length = buf.readableBytes();
        if (buf.hasArray()) { // if the buffer has array, use it directly.
            bytes = buf.array();
        } else {
            bytes = new byte[length];
            buf.getBytes(buf.readerIndex(), bytes);
        }
        try {
            engine.publish(msg.getTopicName(), msg.getClientID(), bytes, 
                    msg.getQos().value(), msg.isRetainFlag());
            stats.publishedMessages++;
        } catch (MqException e) {
            logger.error("Failed to publish." + newline + stackTraceStr(e));
        }
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {
        logger.info("topic=" + msg.getTopicFilter() + " qos="
                + msg.getRequestedQos());
        try {
            if (!engine.subscribedTo(msg.getTopicFilter())) { // only if the engine has not subscribed yet.
                engine.subscribe(msg.getTopicFilter());
            }
        } catch (MqException e) {
            logger.error("Failed to subscribe." + newline + stackTraceStr(e));
        }
        stats.subscribe(msg.getClientID(), msg.getTopicFilter(),
                msg.getRequestedQos().value(), true);
    }
    
    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
        logger.info("topic=" + msg.getTopicFilter());
        stats.unsubscribe(msg.getClientID(), msg.getTopicFilter());
        if (!stats.subscribed(msg.getTopicFilter())) {
            // no other client subscribed to the same topic.
            engine.unsubscribe(msg.getTopicFilter());
        }
        
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage amsg) {
        stats.transferedMessages++;
    }
    
    public Statistics getStatistics() {
        return stats;
    }

    @Override
    public void onReceive(MqMessage msg) {
        stats.receivedMessagesFromPIAX++;
    }

    @Override
    public void onSend(MqMessage msg) {
        // Nothing to do.
    }

    @Override
    public void onOpen(List<Subscription> subscriptions) {
        subscriptions.stream().forEach(s -> {
            stats.subscribe(s.getClientId(), s.getTopicFilter().toString(), s.getRequestedQos().value(), false);
        });
    }

    @Override
    public void onClose() {
        // Nothing to do
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {
        logger.info("clientID=" + msg.getClientID());
        stats.down(msg.getClientID());
    }

    @Override
    public String getID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        // TODO Auto-generated method stub
        return null;
    }

}
