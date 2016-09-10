package org.piqt.peer;

import io.moquette.spi.ISessionsStore;

public interface SessionsStoreHandler {
    void onOpen(ISessionsStore store);
    void onClose(ISessionsStore store);
}
