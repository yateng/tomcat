package org.apache.catalina.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.modeler.Registry;

/**
 * 
 * （1）sever对象是service对象的容器;
 * （2）service对象是connector对象和container对象的容器，同一个service对象可以包含多个connector对象，但是只能包含一个container对象;
 * （3）container对象是属于自包含的，自己又可以包含多个子container对象;
 * （4）同时container对象包含一个pipeline对象，pipeline对象可以理解为是valve对象的容器。
 * 
 * @author Craig R. McClanahan
 */

public class StandardService implements Lifecycle, Service, MBeanRegistration {

    private static Log log = LogFactory.getLog(StandardService.class);

    private static final String info = "org.apache.catalina.core.StandardService/1.0";

    private String name = null; // server的名称

    private LifecycleSupport lifecycle = new LifecycleSupport(this);

    private static final StringManager sm = StringManager.getManager(Constants.Package);// 提示信息

    private Server server = null;

    private boolean started = false; // 当前组件是否已经启动

    protected PropertyChangeSupport support = new PropertyChangeSupport(this);

    protected Connector connectors[] = new Connector[0]; // 连接器对象

    protected Container container = null; // 容器

    protected boolean initialized = false; // 是否已经初始化

    public Container getContainer() {
        return (this.container);
    }

    public void setContainer(Container container) {
        Container oldContainer = this.container;
        if ((oldContainer != null) && (oldContainer instanceof Engine)) {
            ((Engine) oldContainer).setService(null);
        }
        this.container = container;
        if ((this.container != null) && (this.container instanceof Engine)) {
            ((Engine) this.container).setService(this);
        }
        if (started && (this.container != null) && (this.container instanceof Lifecycle)) {
            try {
                ((Lifecycle) this.container).start();
            } catch (LifecycleException e) {
                ;
            }
        }
        synchronized (connectors) {
            for (int i = 0; i < connectors.length; i++)
                connectors[i].setContainer(this.container);
        }
        if (started && (oldContainer != null) && (oldContainer instanceof Lifecycle)) {
            try {
                ((Lifecycle) oldContainer).stop();
            } catch (LifecycleException e) {
                ;
            }
        }

        // Report this property change to interested listeners
        support.firePropertyChange("container", oldContainer, this.container);

    }

    public ObjectName getContainerName() {
        if (container instanceof ContainerBase) {
            return ((ContainerBase) container).getJmxName();
        }
        return null;
    }

    public String getInfo() {
        return (info);
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Server getServer() {
        return (this.server);
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void addConnector(Connector connector) {

        synchronized (connectors) {
            connector.setContainer(this.container);
            connector.setService(this);
            Connector results[] = new Connector[connectors.length + 1];
            System.arraycopy(connectors, 0, results, 0, connectors.length);
            results[connectors.length] = connector;
            connectors = results;

            if (initialized) {
                try {
                    connector.initialize();
                } catch (LifecycleException e) {
                    log.error("Connector.initialize", e);
                }
            }

            if (started && (connector instanceof Lifecycle)) {
                try {
                    ((Lifecycle) connector).start();
                } catch (LifecycleException e) {
                    log.error("Connector.start", e);
                }
            }

            // Report this property change to interested listeners
            support.firePropertyChange("connector", null, connector);
        }

    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public Connector[] findConnectors() {
        return (connectors);
    }

    public void removeConnector(Connector connector) {
        synchronized (connectors) {
            int j = -1;
            for (int i = 0; i < connectors.length; i++) {
                if (connector == connectors[i]) {
                    j = i;
                    break;
                }
            }
            if (j < 0) {
                return;
            }
            if (started && (connectors[j] instanceof Lifecycle)) {
                try {
                    ((Lifecycle) connectors[j]).stop();
                } catch (LifecycleException e) {
                    log.error("Connector.stop", e);
                }
            }
            connectors[j].setContainer(null);
            connector.setService(null);
            int k = 0;
            Connector results[] = new Connector[connectors.length - 1];
            for (int i = 0; i < connectors.length; i++) {
                if (i != j)
                    results[k++] = connectors[i];
            }
            connectors = results;

            // Report this property change to interested listeners
            support.firePropertyChange("connector", connector, null);
        }

    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public String toString() {

        StringBuffer sb = new StringBuffer("StandardService[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

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
    
    public void initialize() throws LifecycleException {
        // Service shouldn't be used with embeded, so it doesn't matter
        if (initialized) {
            if (log.isInfoEnabled()) {
                log.info(sm.getString("standardService.initialize.initialized"));
            }
            return;
        }
        initialized = true;

        if (oname == null) {
            try {
                // Hack - Server should be deprecated...
                Container engine = this.getContainer();
                domain = engine.getName();
                oname = new ObjectName(domain + ":type=Service,serviceName=" + name);
                this.controller = oname;
                Registry.getRegistry(null, null).registerComponent(this, oname, null);
            } catch (Exception e) {
                log.error(sm.getString("standardService.register.failed", domain), e);
            }

        }
        if (server == null) {
            ServerFactory.getServer().addService(this);
        }

        // Initialize our defined Connectors
        synchronized (connectors) {
            for (int i = 0; i < connectors.length; i++) {
                connectors[i].initialize();
            }
        }
    }

    public void start() throws LifecycleException {

        if (log.isInfoEnabled() && started) {
            log.info(sm.getString("standardService.start.started"));
        }

        // 在初始化时，该值已经被修改成true了
        if (!initialized) {
            init();
        }

        // 这里的listener为空。
        lifecycle.fireLifecycleEvent(BEFORE_START_EVENT, null);
        if (log.isInfoEnabled()) {
            log.info(sm.getString("standardService.start.name", this.name));
        }
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        /********************** 启动容器和启动连接器并监听发送的请求-start ********************************/
        if (container != null) {
            synchronized (container) {
                if (container instanceof Lifecycle) {
                    
                    ((Lifecycle) container).start();/*StandardEngine*/
                }
            }
        }

        synchronized (connectors) {
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i] instanceof Lifecycle) {
                    ((Lifecycle) connectors[i]).start();
                }
            }
        }
        /********************** 启动容器和启动链接器并监听发送的请求-end ********************************/

        lifecycle.fireLifecycleEvent(AFTER_START_EVENT, null);
    }

    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            return;
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(BEFORE_STOP_EVENT, null);

        // Stop our defined Connectors first
        synchronized (connectors) {
            for (int i = 0; i < connectors.length; i++) {
                connectors[i].pause();
            }
        }

        // Heuristic: Sleep for a while to ensure pause of the connector
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }

        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        if (log.isInfoEnabled())
            log.info(sm.getString("standardService.stop.name", this.name));
        started = false;

        // Stop our defined Container second
        if (container != null) {
            synchronized (container) {
                if (container instanceof Lifecycle) {
                    ((Lifecycle) container).stop();
                }
            }
        }
        // pero -- Why container stop first? KeepAlive connetions can send request!
        // Stop our defined Connectors first
        synchronized (connectors) {
            for (int i = 0; i < connectors.length; i++) {
                if (connectors[i] instanceof Lifecycle)
                    ((Lifecycle) connectors[i]).stop();
            }
        }

        if (oname == controller) {
            // we registered ourself on init().
            // That should be the typical case - this object is just for
            // backward compat, nobody should bother to load it explicitely
            Registry.getRegistry(null, null).unregisterComponent(oname);
        }

        // Notify our interested LifecycleListeners
        lifecycle.fireLifecycleEvent(AFTER_STOP_EVENT, null);

    }

    public void destroy() throws LifecycleException {
        if (started)
            stop();
    }

    public void init() {
        try {
            initialize();
        } catch (Throwable t) {
            log.error(sm.getString("standardService.initialize.failed", domain), t);
        }
    }

    protected String type;

    protected String domain;

    protected String suffix;

    protected ObjectName oname;

    protected ObjectName controller;

    protected MBeanServer mserver;

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
    }

}
