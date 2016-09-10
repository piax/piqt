package org.piqt.peer;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.spi.impl.security.FileAuthenticator;
import io.moquette.spi.security.IAuthenticator;

public class PIQTAuthenticator implements IAuthenticator {
    private static final Logger logger = LoggerFactory.getLogger(PIQTAuthenticator.class
            .getPackage().getName());
    FileAuthenticator fa;
    
    public PIQTAuthenticator(IConfig config) {
        String path = config.getProperty(BrokerConstants.PASSWORD_FILE_PROPERTY_NAME);
        fa = new FileAuthenticator(null, path);
        File file = new File(path);
        file.delete();
        logger.debug("Temporary ACL file " + file.getPath() + " removed.");
    }

    @Override
    public boolean checkValid(String clientId, String username, byte[] password) {
        return fa.checkValid(clientId, username, password);
    }
}
