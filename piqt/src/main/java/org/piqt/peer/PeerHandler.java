package org.piqt.peer;

import org.piax.pubsub.MqMessage;

public interface PeerHandler {
    void onReceive(MqMessage msg);
    void onSend(MqMessage msg);
}
