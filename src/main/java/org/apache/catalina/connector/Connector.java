package org.apache.catalina.connector;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Service;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.coyote.Adapter;
import org.apache.coyote.ProtocolHandler;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.http.mapper.Mapper;
import org.apache.tomcat.util.modeler.Registry;

public class Connector implements Lifecycle, MBeanRegistration {

    private static Log log = LogFactory.getLog(Connector.class);

    public Connector() throws Exception {
        this(null);
    }

    public Connector(String protocol) throws Exception {
        setProtocol(protocol);
        // Instantiate protocol handler
        try {
            Class<?> clazz = Class.forName(protocolHandlerClassName);
            this.protocolHandler = (ProtocolHandler) clazz.newInstance();
        } catch (Exception e) {
            log.error(sm.getString("coyoteConnector.protocolHandlerInstantiationFailed", e));
        }
    }

    protected Service service = null;

    protected boolean allowTrace = false;

    protected Container container = null;

    protected boolean emptySessionPath = false;

    protected boolean enableLookups = false;

    protected boolean xpoweredBy = false;

    protected static final String info = "org.apache.catalina.connector.Connector/2.1";

    protected LifecycleSupport lifecycle = new LifecycleSupport(this);

    protected int port = 0;

    protected String proxyName = null;

    protected int proxyPort = 0;

    protected int redirectPort = 443;

    protected String scheme = "http";

    protected boolean secure = false;

    protected StringManager sm = StringManager.getManager(Constants.Package);

    protected int maxPostSize = 2 * 1024 * 1024;

    protected int maxSavePostSize = 4 * 1024;

    protected boolean initialized = false;

    protected boolean started = false;

    protected boolean stopped = false;

    protected boolean useIPVHosts = false;

    protected Thread thread = null;

    protected String protocolHandlerClassName = "org.apache.coyote.http11.Http11Protocol";

    protected ProtocolHandler protocolHandler = null;

    protected Adapter adapter = null;

    protected Mapper mapper = new Mapper();

    protected MapperListener mapperListener = new MapperListener(mapper);

    protected String URIEncoding = null;

    protected boolean useBodyEncodingForURI = false;

    protected static HashMap<String, String> replacements = new HashMap<String, String>();
    static {
        replacements.put("acceptCount", "backlog");
        replacements.put("connectionLinger", "soLinger");
        replacements.put("connectionTimeout", "soTimeout");
        replacements.put("connectionUploadTimeout", "timeout");
        replacements.put("clientAuth", "clientauth");
        replacements.put("keystoreFile", "keystore");
        replacements.put("randomFile", "randomfile");
        replacements.put("rootFile", "rootfile");
        replacements.put("keystorePass", "keypass");
        replacements.put("keystoreType", "keytype");
        replacements.put("sslProtocol", "protocol");
        replacements.put("sslProtocols", "protocols");
    }

    public Object getProperty(String name) {
        String repl = name;
        if (replacements.get(name) != null) {
            repl = (String) replacements.get(name);
        }
        return IntrospectionUtils.getProperty(protocolHandler, repl);
    }

    public void setProperty(String name, String value) {
        String repl = name;
        if (replacements.get(name) != null) {
            repl = (String) replacements.get(name);
        }
        IntrospectionUtils.setProperty(protocolHandler, repl, value);
    }

    public Object getAttribute(String name) {
        return getProperty(name);
    }

    public void setAttribute(String name, Object value) {
        setProperty(name, String.valueOf(value));
    }

    public void removeProperty(String name) {
        // protocolHandler.removeAttribute(name);
    }

    public Service getService() {
        return (this.service);
    }

    public void setService(Service service) {
        this.service = service;
    }

    public boolean getAllowTrace() {
        return (this.allowTrace);
    }

    public void setAllowTrace(boolean allowTrace) {
        this.allowTrace = allowTrace;
        setProperty("allowTrace", String.valueOf(allowTrace));

    }

    public boolean isAvailable() {
        return (started);
    }

    /**
     * @deprecated
     */
    public int getBufferSize() {
        return 2048;
    }

    /**
     * @deprecated
     */
    public void setBufferSize(int bufferSize) {
    }

    public Container getContainer() {
        if (container == null) {
            // Lazy - maybe it was added later
            findContainer();
        }
        return (container);
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public boolean getEmptySessionPath() {
        return (this.emptySessionPath);
    }

    public void setEmptySessionPath(boolean emptySessionPath) {
        this.emptySessionPath = emptySessionPath;
        setProperty("emptySessionPath", String.valueOf(emptySessionPath));

    }

    public boolean getEnableLookups() {
        return (this.enableLookups);
    }

    public void setEnableLookups(boolean enableLookups) {
        this.enableLookups = enableLookups;
        setProperty("enableLookups", String.valueOf(enableLookups));
    }

    public String getInfo() {
        return (info);
    }

    public Mapper getMapper() {
        return (mapper);
    }

    public int getMaxPostSize() {
        return (maxPostSize);
    }

    public void setMaxPostSize(int maxPostSize) {
        this.maxPostSize = maxPostSize;
    }

    public int getMaxSavePostSize() {
        return (maxSavePostSize);
    }

    public void setMaxSavePostSize(int maxSavePostSize) {
        this.maxSavePostSize = maxSavePostSize;
        setProperty("maxSavePostSize", String.valueOf(maxSavePostSize));
    }

    public int getPort() {
        return (this.port);
    }

    public void setPort(int port) {
        this.port = port;
        setProperty("port", String.valueOf(port));
    }

    public String getProtocol() {
        if ("org.apache.coyote.http11.Http11Protocol".equals(getProtocolHandlerClassName())
                || "org.apache.coyote.http11.Http11AprProtocol".equals(getProtocolHandlerClassName())) {
            return "HTTP/1.1";
        } else if ("org.apache.jk.server.JkCoyoteHandler".equals(getProtocolHandlerClassName())
                || "org.apache.coyote.ajp.AjpAprProtocol".equals(getProtocolHandlerClassName())) {
            return "AJP/1.3";
        }
        return getProtocolHandlerClassName();
    }

    public void setProtocol(String protocol) {
        // Test APR support
        boolean apr = false;
        try {
            String methodName = "initialize";
            Class<?> paramTypes[] = new Class[1];
            paramTypes[0] = String.class;
            Object paramValues[] = new Object[1];
            paramValues[0] = null;
            Method method = Class.forName("org.apache.tomcat.jni.Library").getMethod(methodName, paramTypes);
            method.invoke(null, paramValues);
            apr = true;
        } catch (Throwable t) {
            // Ignore
        }

        if (apr) {
            if ("HTTP/1.1".equals(protocol)) {
                setProtocolHandlerClassName("org.apache.coyote.http11.Http11AprProtocol");
            } else if ("AJP/1.3".equals(protocol)) {
                setProtocolHandlerClassName("org.apache.coyote.ajp.AjpAprProtocol");
            } else if (protocol != null) {
                setProtocolHandlerClassName(protocol);
            } else {
                setProtocolHandlerClassName("org.apache.coyote.http11.Http11AprProtocol");
            }
        } else {
            if ("HTTP/1.1".equals(protocol)) {
                setProtocolHandlerClassName("org.apache.coyote.http11.Http11Protocol");
            } else if ("AJP/1.3".equals(protocol)) {
                setProtocolHandlerClassName("org.apache.jk.server.JkCoyoteHandler");
            } else if (protocol != null) {
                setProtocolHandlerClassName(protocol);
            }
        }
    }

    public String getProtocolHandlerClassName() {
        return (this.protocolHandlerClassName);
    }

    public void setProtocolHandlerClassName(String protocolHandlerClassName) {
        this.protocolHandlerClassName = protocolHandlerClassName;
    }

    public ProtocolHandler getProtocolHandler() {
        return (this.protocolHandler);
    }

    public String getProxyName() {
        return (this.proxyName);
    }

    public void setProxyName(String proxyName) {
        if (proxyName != null && proxyName.length() > 0) {
            this.proxyName = proxyName;
            setProperty("proxyName", proxyName);
        } else {
            this.proxyName = null;
            removeProperty("proxyName");
        }
    }

    public int getProxyPort() {
        return (this.proxyPort);
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        setProperty("proxyPort", String.valueOf(proxyPort));
    }

    public int getRedirectPort() {
        return (this.redirectPort);
    }

    public void setRedirectPort(int redirectPort) {
        this.redirectPort = redirectPort;
        setProperty("redirectPort", String.valueOf(redirectPort));
    }

    public String getScheme() {
        return (this.scheme);
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public boolean getSecure() {
        return (this.secure);
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
        setProperty("secure", Boolean.toString(secure));
    }

    public String getURIEncoding() {
        return (this.URIEncoding);
    }

    public void setURIEncoding(String URIEncoding) {
        this.URIEncoding = URIEncoding;
        setProperty("uRIEncoding", URIEncoding);
    }

    public boolean getUseBodyEncodingForURI() {
        return (this.useBodyEncodingForURI);
    }

    public void setUseBodyEncodingForURI(boolean useBodyEncodingForURI) {
        this.useBodyEncodingForURI = useBodyEncodingForURI;
        setProperty("useBodyEncodingForURI", String.valueOf(useBodyEncodingForURI));
    }

    public boolean getXpoweredBy() {
        return xpoweredBy;
    }

    public void setXpoweredBy(boolean xpoweredBy) {
        this.xpoweredBy = xpoweredBy;
        setProperty("xpoweredBy", String.valueOf(xpoweredBy));
    }

    public void setUseIPVHosts(boolean useIPVHosts) {
        this.useIPVHosts = useIPVHosts;
        setProperty("useIPVHosts", String.valueOf(useIPVHosts));
    }

    public boolean getUseIPVHosts() {
        return useIPVHosts;
    }

    public Request createRequest() {
        Request request = new Request();
        request.setConnector(this);
        return (request);

    }

    public Response createResponse() {
        Response response = new Response();
        response.setConnector(this);
        return (response);
    }

    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    @SuppressWarnings("deprecation")
    protected ObjectName createObjectName(String domain, String type) throws MalformedObjectNameException {
        String encodedAddr = null;
        if (getProperty("address") != null) {
            encodedAddr = URLEncoder.encode(getProperty("address").toString());
        }
        String addSuffix = (getProperty("address") == null) ? "" : ",address=" + encodedAddr;
        ObjectName _oname = new ObjectName(domain + ":type=" + type + ",port=" + getPort() + addSuffix);
        return _oname;
    }

    public void initialize() throws LifecycleException {
        if (initialized) {
            if (log.isInfoEnabled())
                log.info(sm.getString("coyoteConnector.alreadyInitialized"));
            return;
        }

        this.initialized = true;

        if (oname == null && (container instanceof StandardEngine)) {
            try {
                // we are loaded directly, via API - and no name was given to us
                StandardEngine cb = (StandardEngine) container;
                oname = createObjectName(cb.getName(), "Connector");
                Registry.getRegistry(null, null).registerComponent(this, oname, null);
                controller = oname;
            } catch (Exception e) {
                log.error("Error registering connector ", e);
            }
            if (log.isDebugEnabled())
                log.debug("Creating name for connector " + oname);
        }

        adapter = new CoyoteAdapter(this);
        // 根据protocol来设置处理器protocolHandler(在构造方法中设置)
        protocolHandler.setAdapter(adapter);

        IntrospectionUtils.setProperty(protocolHandler, "jkHome", System.getProperty("catalina.base"));

        try {
            // 根据不同协议，初始化协议处理器
            protocolHandler.init();
        } catch (Exception e) {
            throw new LifecycleException(sm.getString("coyoteConnector.protocolHandlerInitializationFailed", e));
        }
    }

    public void pause() throws LifecycleException {
        try {
            protocolHandler.pause();
        } catch (Exception e) {
            log.error(sm.getString("coyoteConnector.protocolHandlerPauseFailed"), e);
        }
    }

    public void resume() throws LifecycleException {
        try {
            protocolHandler.resume();
        } catch (Exception e) {
            log.error(sm.getString("coyoteConnector.protocolHandlerResumeFailed"), e);
        }
    }

    public void start() throws LifecycleException {
        if (!initialized) {
            initialize();
        }

        if (started) {
            if (log.isInfoEnabled()) {
                log.info(sm.getString("coyoteConnector.alreadyStarted"));
            }
            return;
        }
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // We can't register earlier - the JMX registration of this happens in Server.start callback
        if (this.oname != null) {
            // We are registred - register the adapter as well.
            try {
                Registry.getRegistry(null, null).registerComponent(protocolHandler, createObjectName(this.domain, "ProtocolHandler"), null);
            } catch (Exception ex) {
                log.error(sm.getString("coyoteConnector.protocolRegistrationFailed"), ex);
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info(sm.getString("coyoteConnector.cannotRegisterProtocol"));
            }
        }

        try {
            protocolHandler.start();
        } catch (Exception e) {
            String errPrefix = "";
            if (this.service != null) {
                errPrefix += "service.getName(): \"" + this.service.getName() + "\"; ";
            }
            throw new LifecycleException(errPrefix + " " + sm.getString("coyoteConnector.protocolHandlerStartFailed", e));
        }

        if (this.domain != null) {
            mapperListener.setDomain(domain);
            // mapperListener.setEngine( service.getContainer().getName() );
            mapperListener.init();
            try {
                ObjectName mapperOname = createObjectName(this.domain, "Mapper");
                if (log.isDebugEnabled()) {
                    log.debug(sm.getString("coyoteConnector.MapperRegistration", mapperOname));
                }
                Registry.getRegistry(null, null).registerComponent(mapper, mapperOname, "Mapper");
            } catch (Exception ex) {
                log.error(sm.getString("coyoteConnector.protocolRegistrationFailed"), ex);
            }
        }
    }

    public void stop() throws LifecycleException {
        // Validate and update our current state
        if (!started) {
            log.error(sm.getString("coyoteConnector.notStarted"));
            return;

        }
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        try {
            mapperListener.destroy();
            Registry.getRegistry(null, null).unregisterComponent(createObjectName(this.domain, "Mapper"));
            Registry.getRegistry(null, null).unregisterComponent(createObjectName(this.domain, "ProtocolHandler"));
        } catch (MalformedObjectNameException e) {
            log.error(sm.getString("coyoteConnector.protocolUnregistrationFailed"), e);
        }
        try {
            protocolHandler.destroy();
        } catch (Exception e) {
            throw new LifecycleException(sm.getString("coyoteConnector.protocolHandlerDestroyFailed", e));
        }

    }

    // -------------------- JMX registration --------------------
    protected String domain;

    protected ObjectName oname;

    protected MBeanServer mserver;

    ObjectName controller;

    public ObjectName getController() {
        return controller;
    }

    public void setController(ObjectName controller) {
        this.controller = controller;
    }

    public ObjectName getObjectName() {
        return oname;
    }

    public String getDomain() {
        return domain;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        oname = name;
        mserver = server;
        domain = name.getDomain();
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
        try {
            if (started) {
                stop();
            }
        } catch (Throwable t) {
            log.error("Unregistering - can't stop", t);
        }
    }

    protected void findContainer() {
        try {
            // Register to the service
            ObjectName parentName = new ObjectName(domain + ":" + "type=Service");

            if (log.isDebugEnabled())
                log.debug("Adding to " + parentName);
            if (mserver.isRegistered(parentName)) {
                mserver.invoke(parentName, "addConnector", new Object[] { this }, new String[] { "org.apache.catalina.connector.Connector" });
                // As a side effect we'll get the container field set
                // Also initialize will be called
                // return;
            }
            // Go directly to the Engine
            // initialize(); - is called by addConnector
            ObjectName engName = new ObjectName(domain + ":" + "type=Engine");
            if (mserver.isRegistered(engName)) {
                Object obj = mserver.getAttribute(engName, "managedResource");
                if (log.isDebugEnabled())
                    log.debug("Found engine " + obj + " " + obj.getClass());
                container = (Container) obj;

                // Internal initialize - we now have the Engine
                initialize();

                if (log.isDebugEnabled())
                    log.debug("Initialized");
                // As a side effect we'll get the container field set
                // Also initialize will be called
                return;
            }
        } catch (Exception ex) {
            log.error("Error finding container " + ex);
        }
    }

    public void init() throws Exception {

        if (this.getService() != null) {
            if (log.isDebugEnabled())
                log.debug("Already configured");
            return;
        }
        if (container == null) {
            findContainer();
        }
    }

    public void destroy() throws Exception {
        if (oname != null && controller == oname) {
            if (log.isDebugEnabled())
                log.debug("Unregister itself " + oname);
            Registry.getRegistry(null, null).unregisterComponent(oname);
        }
        if (getService() == null)
            return;
        getService().removeConnector(this);
    }

}
