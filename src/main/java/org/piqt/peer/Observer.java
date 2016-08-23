package org.piqt.peer;

import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.eclipse.moquette.interception.InterceptHandler;
import org.eclipse.moquette.interception.messages.InterceptConnectMessage;
import org.eclipse.moquette.interception.messages.InterceptDisconnectMessage;
import org.eclipse.moquette.interception.messages.InterceptPublishMessage;
import org.eclipse.moquette.interception.messages.InterceptSubscribeMessage;
import org.eclipse.moquette.interception.messages.InterceptUnsubscribeMessage;
import org.eclipse.moquette.spi.impl.ProtocolProcessor;
import org.piqt.MqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.piqt.peer.Util.*;

public class Observer implements InterceptHandler {

    private static final Logger logger = LoggerFactory.getLogger(Observer.class
            .getPackage().getName());

    // PeerMqEngine engine;
    PeerMqEngineMoquette engine;

    public Observer() {
        super();
        Thread th = Thread.currentThread();
        logger.debug("PeerMqObserver thread=" + th);
        engine = Broker.getEngine();
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
        logger.info("clientID=" + msg.getClientID());
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
        logger.info("clientID=" + msg.getClientID());
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        logger.info("topic=" + msg.getTopicName() + " qos=" + msg.getQos());
        byte[] byteArray = new byte[msg.getPayload().remaining()];
        msg.getPayload().get(byteArray);
        String payload = "";
        try {
            payload = new String(byteArray, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.debug("Failed to convert byte[] -> String. " + newline
                    + stackTraceStr(e));
        }
        logger.debug("msg=" + payload);
        try {
            engine.publish(msg.getTopicName(), byteArray, msg.getQos()
                    .byteValue(), msg.isRetainFlag());
        } catch (MqException e) {
            logger.error("Failed to publish." + newline + stackTraceStr(e));
        }

    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {
        logger.info("topic=" + msg.getTopicFilter() + " qos="
                + msg.getRequestedQos());
        try {
            engine.subscribe(msg.getTopicFilter());
        } catch (MqException e) {
            logger.error("Failed to subscribe." + newline + stackTraceStr(e));
        }
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
        logger.info("topic=" + msg.getTopicFilter());
        engine.unsubscribe(msg.getTopicFilter());
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg,
            Set<String> deletedTopics) {
        logger.info("clientID=" + msg.getClientID());
        engine.notifyDeletedTopics(deletedTopics);

    }

    @Override
    public void onInit(ProtocolProcessor pp) {
        logger.trace("onInit");
        engine.notifyInit(pp);
    }

}
