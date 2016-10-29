package org.piqt.peer;

import io.moquette.spi.impl.subscriptions.Subscription;

import java.util.List;

public interface SessionsStoreHandler {
    void onOpen(List<Subscription> subscriptions);
    void onClose();
}
