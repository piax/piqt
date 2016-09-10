package org.piqt.peer;

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.spi.impl.security.ACLFileParser;
import io.moquette.spi.security.IAuthorizator;

import java.io.File;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PIQTAuthorizator implements IAuthorizator {
    private static final Logger logger = LoggerFactory.getLogger(PIQTAuthorizator.class
            .getPackage().getName());
    IAuthorizator authorizator = null;

    public PIQTAuthorizator(IConfig config) {
        String path = config.getProperty(BrokerConstants.ACL_FILE_PROPERTY_NAME);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    authorizator = ACLFileParser.parse(file);
                } catch (ParseException e) {
                    logger.warn("Error in ACL file: " + e);
                }
                file.delete();
                logger.debug("Temporary ACL file " + file.getPath() + " removed.");
            }
        }
    }
    
    @Override
    public boolean canWrite(String topic, String user, String client) {
        if (authorizator != null) {
            return authorizator.canWrite(topic, user, client);
        }
        return true;
    }

    @Override
    public boolean canRead(String topic, String user, String client) {
        if (authorizator != null) {
            return authorizator.canRead(topic, user, client);
        }
        return true;
    }

}
