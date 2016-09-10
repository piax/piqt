/*
 * PIQT version of Moquette Server.
 */
package org.piqt.peer;

import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.DefaultMoquetteSslContextCreator;
import io.moquette.server.ServerAcceptor;
import io.moquette.server.config.IConfig;
import io.moquette.server.netty.NettyAcceptor;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.BrokerInterceptor;
import io.moquette.spi.impl.ProtocolProcessor;
import io.moquette.spi.impl.security.ACLFileParser;
import io.moquette.spi.impl.security.AcceptAllAuthenticator;
import io.moquette.spi.impl.security.DenyAllAuthorizator;
import io.moquette.spi.impl.security.FileAuthenticator;
import io.moquette.spi.impl.security.PermitAllAuthorizator;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.persistence.MapDBPersistentStore;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.moquette.spi.security.ISslContextCreator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private ServerAcceptor m_acceptor;

    private volatile boolean m_initialized;

    // from ProtocolProcessorBootstrapper
    private SubscriptionsStore subscriptions;

    private MapDBPersistentStore m_mapStorage;

    private ProtocolProcessor m_processor;

    private BrokerInterceptor m_interceptor;
    
    private SessionsStoreHandler m_sshandler;

    public ProtocolProcessor init(IConfig props, List<? extends InterceptHandler> embeddedObservers,
            SessionsStoreHandler sshandler,IAuthenticator authenticator, IAuthorizator authorizator, Server server) {

        m_processor = new ProtocolProcessor();
        subscriptions = new SubscriptionsStore();
        m_mapStorage = new MapDBPersistentStore(props);
        m_mapStorage.initStore();
        IMessagesStore messagesStore = m_mapStorage.messagesStore();
        ISessionsStore sessionsStore = m_mapStorage.sessionsStore(messagesStore);
        sshandler.onOpen(sessionsStore);

        List<InterceptHandler> observers = new ArrayList<>(embeddedObservers);
        String interceptorClassName = props.getProperty(BrokerConstants.INTERCEPT_HANDLER_PROPERTY_NAME);
        if (interceptorClassName != null && !interceptorClassName.isEmpty()) {
            try {
                InterceptHandler handler;
                try {
                    final Constructor<? extends InterceptHandler> constructor = Class.forName(interceptorClassName).asSubclass(InterceptHandler.class).getConstructor(Server.class);
                    handler = constructor.newInstance(server);
                } catch (NoSuchMethodException nsme){
                    handler = Class.forName(interceptorClassName).asSubclass(InterceptHandler.class).newInstance();
                }
                observers.add(handler);
            } catch (Throwable ex) {
                LOG.error("Can't load the intercept handler {}", ex);
            }
        }
        m_interceptor = new BrokerInterceptor(observers);

        subscriptions.init(sessionsStore);

        String configPath = System.getProperty("moquette.path", null);
        String authenticatorClassName = props.getProperty(BrokerConstants.AUTHENTICATOR_CLASS_NAME, "");

        if (!authenticatorClassName.isEmpty()) {
            authenticator = (IAuthenticator)loadClass(authenticatorClassName, IAuthenticator.class, props);
            LOG.info("Loaded custom authenticator {}", authenticatorClassName);
        }

        if (authenticator == null) {
            String passwdPath = props.getProperty(BrokerConstants.PASSWORD_FILE_PROPERTY_NAME, "");
            if (passwdPath.isEmpty()) {
                authenticator = new AcceptAllAuthenticator();
            } else {
                authenticator = new FileAuthenticator(configPath, passwdPath);
            }
        }

        String authorizatorClassName = props.getProperty(BrokerConstants.AUTHORIZATOR_CLASS_NAME, "");
        if (!authorizatorClassName.isEmpty()) {
            authorizator = (IAuthorizator)loadClass(authorizatorClassName, IAuthorizator.class, props);
            LOG.info("Loaded custom authorizator {}", authorizatorClassName);
        }

        if (authorizator == null) {
            String aclFilePath = props.getProperty(BrokerConstants.ACL_FILE_PROPERTY_NAME, "");
            if (aclFilePath != null && !aclFilePath.isEmpty()) {
                authorizator = new DenyAllAuthorizator();
                File aclFile = new File(configPath, aclFilePath);
                try {
                    authorizator = ACLFileParser.parse(aclFile);
                } catch (ParseException pex) {
                    LOG.error(String.format("Format error in parsing acl file %s", aclFile), pex);
                }
                LOG.info("Using acl file defined at path {}", aclFilePath);
            } else {
                authorizator = new PermitAllAuthorizator();
                LOG.info("Starting without ACL definition");
            }
        }
        boolean allowAnonymous = Boolean.parseBoolean(props.getProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, "true"));
        boolean allowZeroByteClientId = Boolean.parseBoolean(props.getProperty(BrokerConstants.ALLOW_ZERO_BYTE_CLIENT_ID_PROPERTY_NAME, "false"));
        m_processor.init(subscriptions, messagesStore, sessionsStore, authenticator, allowAnonymous, allowZeroByteClientId, authorizator, m_interceptor, props.getProperty(BrokerConstants.PORT_PROPERTY_NAME));
        return m_processor;
    }

    private Object loadClass(String className, Class<?> cls, IConfig props) {
        Object instance = null;
        try {
            Class<?> clazz = Class.forName(className);

            // check if method getInstance exists
            Method method = clazz.getMethod("getInstance", new Class[] {});
            try {
                instance = method.invoke(null, new Object[] {});
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException ex) {
                LOG.error(null, ex);
                throw new RuntimeException("Cannot call method "+ className +".getInstance", ex);
            }
        }
        catch (NoSuchMethodException nsmex) {
            try {
                // check if constructor with IConfig parameter exists
                instance = this.getClass().getClassLoader()
                        .loadClass(className)
                        .asSubclass(cls)
                        .getConstructor(IConfig.class).newInstance(props);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                LOG.error(null, ex);
                throw new RuntimeException("Cannot load custom authenticator class " + className, ex);
            } catch (NoSuchMethodException | InvocationTargetException e) {
                try {
                    // fallback to default constructor
                    instance = this.getClass().getClassLoader()
                            .loadClass(className)
                            .asSubclass(cls)
                            .newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                    LOG.error(null, ex);
                    throw new RuntimeException("Cannot load custom authenticator class " + className, ex);
                }
            }
        } catch (ClassNotFoundException ex) {
            LOG.error(null, ex);
            throw new RuntimeException("Class " + className + " not found", ex);
        } catch (SecurityException ex) {
            LOG.error(null, ex);
            throw new RuntimeException("Cannot call method "+ className +".getInstance", ex);
        }
        return instance;
    }

    public void startServer(IConfig config) throws IOException {
        startServer(config, null, null);
    }
    
    /**
     * Starts Moquette with config provided by an implementation of IConfig class and with the
     * set of InterceptHandler and SessionsStoreHandler.
     * */
    public void startServer(IConfig config, List<? extends InterceptHandler> handlers,
            SessionsStoreHandler ssh) throws IOException {
        startServer(config, handlers, null, null, null, ssh);
    }

    public void startServer(IConfig config, List<? extends InterceptHandler> handlers,
                            ISslContextCreator sslCtxCreator, IAuthenticator authenticator,
                            IAuthorizator authorizator,
                            SessionsStoreHandler ssh) throws IOException {
        if (handlers == null) {
            handlers = Collections.emptyList();
        }
        m_sshandler = ssh;
        final String handlerProp = System.getProperty(BrokerConstants.INTERCEPT_HANDLER_PROPERTY_NAME);
        if (handlerProp != null) {
            config.setProperty(BrokerConstants.INTERCEPT_HANDLER_PROPERTY_NAME, handlerProp);
        }

        LOG.info("Persistent store file: {}", config.getProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME));

        final ProtocolProcessor processor = init(config, handlers, m_sshandler, authenticator, authorizator, this);

        if (sslCtxCreator == null) {
            sslCtxCreator = new DefaultMoquetteSslContextCreator(config);
        }

        m_acceptor = new NettyAcceptor();
        m_acceptor.initialize(processor, config, sslCtxCreator);
        m_processor = processor;
        m_initialized = true;
    }

    /**
     * Use the broker to publish a message. It's intended for embedding applications.
     * It can be used only after the server is correctly started with startServer.
     *
     * @param msg the message to forward.
     * @throws IllegalStateException if the server is not yet started
     * */
    public void internalPublish(PublishMessage msg) {
        if (!m_initialized) {
            throw new IllegalStateException("Can't publish on a server is not yet started");
        }
        m_processor.internalPublish(msg);
    }
    
    public void stopServer() {
        LOG.info("Server stopping...");
        m_acceptor.close();
        m_mapStorage.close();
        m_initialized = false;
        LOG.info("Server stopped");
    }

    /**
     * SPI method used by Broker embedded applications to add intercept handlers.
     * */
    public boolean addInterceptHandler(InterceptHandler interceptHandler) {
        if (!m_initialized) {
            throw new IllegalStateException("Can't register interceptors on a server that is not yet started");
        }
        return m_processor.addInterceptHandler(interceptHandler);
    }

    /**
     * SPI method used by Broker embedded applications to remove intercept handlers.
     * */
    public boolean removeInterceptHandler(InterceptHandler interceptHandler) {
        if (!m_initialized) {
            throw new IllegalStateException("Can't deregister interceptors from a server that is not yet started");
        }
        return m_processor.removeInterceptHandler(interceptHandler);
    }

}
