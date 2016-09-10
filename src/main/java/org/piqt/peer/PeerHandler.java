package org.piqt.peer;

import org.piqt.MqMessage;

public interface PeerHandler {
    void onReceive(MqMessage msg);
    void onSend(MqMessage msg);
}
