/*
 * MqttPiaxConfig.java - Configuration related module.
 * 
 * Copyright (c) 2016 National Institute of Information and Communications
 * Technology, Japan
 *
 * You can redistribute it and/or modify it under either the terms of
 * the AGPLv3 or PIAX binary code license. See the file COPYING
 * included in the PIQT package for more in detail.
 */
package org.piqt.web;

import static org.piqt.peer.Util.isEmpty;
import static org.piqt.peer.Util.isOnlySpace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;
import io.moquette.BrokerConstants;

public class MqttPiaxConfig {
    public static final String KEY_PIAX_SEED_IP_ADDRESS = "piax_seed_ip_address";
    public static final String KEY_PIAX_SEED_PORT = "piax_seed_port";
    public static final String KEY_PIAX_IP_ADDRESS = "piax_ip_address";
    public static final String KEY_PIAX_PORT = "piax_port";
//    public static final String KEY_PIAX_PEER_ID = "piax_peer_id";
    public static final String KEY_PIAX_DOMAIN_NAME = "piax_domain_name";

    public static final String KEY_MQTT_BIND_ADDRESS = "mqtt_bind_address";
    public static final String KEY_MQTT_PORT = "mqtt_port";
    public static final String KEY_MQTT_ALLOW_ANONYMOUS = "mqtt_allow_anonymous";
    public static final String KEY_MQTT_USER_NAME = "mqtt_user_name";
    public static final String KEY_MQTT_USER_PASSWORD = "mqtt_user_password";
    public static final String KEY_MQTT_ACL = "mqtt_acl";
    public static final String KEY_MQTT_PERSISTENT_STORE = "mqtt_persistent_store";

//    public static final String KEY_LOG_LEVEL = "log_level";
//    public static final String KEY_LOG_DESTINATION = "log_destination";

    public static final String KEY_JETTY_HOST = "jetty_host";
    public static final String KEY_JETTY_PORT = "jetty_port";
//    public static final String KEY_JETTY_RESOURCE_BASE = "jetty_resource_base";

    public static final String KEY_UPDATE_INTERVAL = "update_interval";
    public static final String KEY_AUTO_START = "auto_start";
    public static final String KEY_CLEAR_START = "clear_start";

    public static final String[] KEYS = { KEY_PIAX_SEED_IP_ADDRESS,
            KEY_PIAX_SEED_PORT, KEY_PIAX_IP_ADDRESS, KEY_PIAX_PORT,
            KEY_PIAX_DOMAIN_NAME, KEY_MQTT_BIND_ADDRESS,
            KEY_MQTT_PORT, KEY_MQTT_ALLOW_ANONYMOUS, KEY_MQTT_USER_NAME,
            KEY_MQTT_USER_PASSWORD, KEY_MQTT_ACL, KEY_MQTT_PERSISTENT_STORE,
            KEY_JETTY_HOST, KEY_JETTY_PORT, KEY_UPDATE_INTERVAL,
            KEY_AUTO_START, KEY_CLEAR_START };

    public static final String[] INT_KEYS = { KEY_PIAX_SEED_PORT,
            KEY_PIAX_PORT, KEY_MQTT_PORT, KEY_JETTY_PORT };

    HashMap<String, String> p;
    Pattern p1, p2;

    public MqttPiaxConfig(Properties prop) {
        init(prop);
    }

    private void init(Properties prop) {
        p = new HashMap<>();
        p1 = Pattern.compile("^user-[0-9]+$");
        p2 = Pattern.compile("^pass-[0-9]+$");
        for (Object key : prop.keySet()) {
            if (isValidKey((String) key)) {
                p.put((String) key, (String) prop.get(key));
            }
        }
    }

    private boolean isValidKey(String key) {
        for (String k : KEYS) {
            if (key.equals(k)) {
                return true;
            }
        }
        if (p1.matcher(key).find() || p2.matcher(key).find())
            return true;

        return false;
    }

    private boolean isIntegerValue(String key) {
        for (String k : INT_KEYS) {
            if (key.equals(k)) {
                return true;
            }
        }
        return false;
    }

    public void setConfig(LinkedHashMap<String, String> data) {
        p.clear();
        for (Object key : data.keySet()) {
            if (isValidKey((String) key)) {
                p.put((String) key, data.get(key));
            }
        }
    }

    public void setConfig(String key, String value) {
        p.put(key, value);
    }

    private String setDefault(String key) {
        String v;
        if (isIntegerValue(key)) {
            v = "0";
        } else {
            v = "";
        }
        return v;
    }

    public Object get(String key) {
        String v = p.get(key);
        if (v == null) {
            v = setDefault(key);
        }
        if (isIntegerValue(key)) {
            return Integer.valueOf(v);
        } else {
            return v;
        }
    }

    public Properties toMQTTProps() throws IOException {
        Properties ret = new Properties();
        ret.setProperty(BrokerConstants.HOST_PROPERTY_NAME, p.get(KEY_MQTT_BIND_ADDRESS));
        ret.setProperty(BrokerConstants.PORT_PROPERTY_NAME, p.get(KEY_MQTT_PORT));
        ret.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME,
                p.get(KEY_MQTT_PERSISTENT_STORE));
        ret.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME,
                p.get(KEY_MQTT_ALLOW_ANONYMOUS));
        
        // do not open web socket port.
        ret.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, BrokerConstants.DISABLED_PORT_BIND);

        File tmpFile = File.createTempFile("tmp_password_", ".conf");
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile));

        for (Entry<String, String> e : p.entrySet()) {
            String userKey = e.getKey();
            if (p1.matcher(userKey).find()) {
                String name = e.getValue();
                String passKey = userKey.replace("user-", "pass-");
                String pass = p.get(passKey);
                if (isEmpty(name) || isOnlySpace(name) || isEmpty(pass)
                        || isOnlySpace(pass)) {
                    continue;
                }
                bw.write(name + ":" + pass);
                bw.newLine();
            }
        }
        bw.close();
        ret.setProperty(BrokerConstants.PASSWORD_FILE_PROPERTY_NAME, tmpFile.getPath());

        String acls = p.get(KEY_MQTT_ACL);
        if (acls != null && !acls.equals("")) {
            File tmpFile2 = File.createTempFile("tmp_acl_", ".conf");
            BufferedWriter bw2 = new BufferedWriter(new FileWriter(tmpFile2));
            bw2.write(acls);
            bw2.newLine();
            bw2.close();
            ret.setProperty(BrokerConstants.ACL_FILE_PROPERTY_NAME, tmpFile2.getPath());
        }

        ret.setProperty(BrokerConstants.AUTHENTICATOR_CLASS_NAME, "org.piqt.peer.PIQTAuthenticator");
        ret.setProperty(BrokerConstants.AUTHORIZATOR_CLASS_NAME, "org.piqt.peer.PIQTAuthorizator");

        /* The following line is TODO
        ret.setProperty(SSL_PORT_PROPERTY_NAME, "8883");
        ret.setProperty(JKS_PATH_PROPERTY_NAME, "serverkeystore.jks");
        ret.setProperty(KEY_STORE_PASSWORD_PROPERTY_NAME, "xxx");
        ret.setProperty(KEY_MANAGER_PASSWORD_PROPERTY_NAME, "xxx");
        */
        return ret;
    }

    public Properties toProperties() {
        Properties ret = new Properties();

        for (String k : p.keySet()) {
            ret.setProperty(k, p.get(k));
        }
        return ret;
    }
}
