package org.apache.catalina.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.core.StandardServer;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

public class Catalina extends Embedded {

    protected String configFile = "conf/server.xml";

    protected ClassLoader parentClassLoader = Catalina.class.getClassLoader();

    protected Server server = null;

    protected boolean starting = false;

    protected boolean stopping = false;

    protected boolean useShutdownHook = true;

    protected Thread shutdownHook = null;

    public void setConfig(String file) {
        configFile = file;
    }

    public void setConfigFile(String file) {
        configFile = file;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setUseShutdownHook(boolean useShutdownHook) {
        this.useShutdownHook = useShutdownHook;
    }

    public boolean getUseShutdownHook() {
        return useShutdownHook;
    }

    public void setParentClassLoader(ClassLoader parentClassLoader) {

        this.parentClassLoader = parentClassLoader;

    }

    public void setServer(Server server) {
        this.server = server;
    }

    public static void main(String args[]) {
        (new Catalina()).process(args);
    }

    public void process(String args[]) {

        setAwait(true);
        setCatalinaHome();
        setCatalinaBase();
        try {
            if (arguments(args)) {
                if (starting) {
                    load(args);
                    start();
                } else if (stopping) {
                    stopServer();
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    protected boolean arguments(String args[]) {

        boolean isConfig = false;

        if (args.length < 1) {
            usage();
            return (false);
        }

        for (int i = 0; i < args.length; i++) {
            if (isConfig) {
                configFile = args[i];
                isConfig = false;
            } else if (args[i].equals("-config")) {
                isConfig = true;
            } else if (args[i].equals("-nonaming")) {
                setUseNaming(false);
            } else if (args[i].equals("-help")) {
                usage();
                return (false);
            } else if (args[i].equals("start")) {
                starting = true;
                stopping = false;
            } else if (args[i].equals("stop")) {
                starting = false;
                stopping = true;
            } else {
                usage();
                return (false);
            }
        }

        return (true);

    }

    protected File configFile() {

        File file = new File(configFile);
        // 如果当前server.xml是相对路径，则在从catalina.base中取
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("catalina.base"), configFile);
        }
        return file;
    }

    /**
     * Create and configure the Digester we will be using for startup.
     */
    protected Digester createStartDigester() {
        long t1 = System.currentTimeMillis();
        Digester digester = new Digester(); // 这个对象用于解析处理xml文件
        digester.setValidating(false);      // 不进行XML与相应的DTD的合法性验证
        digester.setClassLoader(StandardServer.class.getClassLoader()); // 设置类加载起

        /*
         * ===================================1============================================
         * digester.addObjectCreate(String pattern, String className, String attributeName)
         * pattern--匹配的节点
         * className--该节点对应的默认实体类
         * attributeName--如果该节点有className属性,用className的值替换默认实体类
         * Digester匹配到Server节点，如果Server节点没有className属性，将创建org.apache.catalina.core.StandardServer对象；如果Server节点有className属性，将创建指定的(className属性的值)对象
         * ===================================2============================================
         * digester.addSetProperties(String pattern)
         * 将指定节点的属性映射到对象
         * ===================================3============================================
         * Digester.addSetNext(String pattern, String methodName, String paramType)
         * pattern--匹配的节点
         * methodName--调用父节点的方法
         * paramType--父节点的方法接收的参数类型
         */
        digester.addObjectCreate("Server", "org.apache.catalina.core.StandardServer", "className"); 
        digester.addSetProperties("Server"); // 将指定节点的属性映射到对象，即将Server节点的port,shutdown 属性映射到StandardServer.java
        digester.addSetNext("Server", "setServer", "org.apache.catalina.Server"); // Digester匹配到Server节点，将调用Catalina(Server的父节点)的setServer方法，参数为Server对象

        digester.addObjectCreate("Server/GlobalNamingResources", "org.apache.catalina.deploy.NamingResources");
        digester.addSetProperties("Server/GlobalNamingResources");
        digester.addSetNext("Server/GlobalNamingResources", "setGlobalNamingResources", "org.apache.catalina.deploy.NamingResources");

        // MUST be specified in the element
        digester.addObjectCreate("Server/Listener", null, "className");
        digester.addSetProperties("Server/Listener");
        digester.addSetNext("Server/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");

        digester.addObjectCreate("Server/Service", "org.apache.catalina.core.StandardService", "className");
        digester.addSetProperties("Server/Service");
        digester.addSetNext("Server/Service", "addService", "org.apache.catalina.Service");

        // MUST be specified in the element
        digester.addObjectCreate("Server/Service/Listener", null, "className");
        digester.addSetProperties("Server/Service/Listener");
        digester.addSetNext("Server/Service/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");

        digester.addRule("Server/Service/Connector", new ConnectorCreateRule());
        digester.addRule("Server/Service/Connector", new SetAllPropertiesRule());
        digester.addSetNext("Server/Service/Connector", "addConnector", "org.apache.catalina.connector.Connector");

        // MUST be specified in the element
        digester.addObjectCreate("Server/Service/Connector/Listener", null, "className");
        digester.addSetProperties("Server/Service/Connector/Listener");
        digester.addSetNext("Server/Service/Connector/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");

        // Add RuleSets for nested elements
        digester.addRuleSet(new NamingRuleSet("Server/GlobalNamingResources/"));
        digester.addRuleSet(new EngineRuleSet("Server/Service/"));
        digester.addRuleSet(new HostRuleSet("Server/Service/Engine/"));
        digester.addRuleSet(new ContextRuleSet("Server/Service/Engine/Host/"));
        digester.addRuleSet(ClusterRuleSetFactory.getClusterRuleSet("Server/Service/Engine/Host/Cluster/"));
        digester.addRuleSet(new NamingRuleSet("Server/Service/Engine/Host/Context/"));

        // When the 'engine' is found, set the parentClassLoader.
        digester.addRule("Server/Service/Engine", new SetParentClassLoaderRule(parentClassLoader));
        digester.addRuleSet(ClusterRuleSetFactory.getClusterRuleSet("Server/Service/Engine/Cluster/"));

        long t2 = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Digester for server.xml created " + (t2 - t1));
        }
        return (digester);

    }

    protected Digester createStopDigester() {

        // Initialize the digester
        Digester digester = new Digester();

        // Configure the rules we need for shutting down
        digester.addObjectCreate("Server", "org.apache.catalina.core.StandardServer", "className");
        digester.addSetProperties("Server");
        digester.addSetNext("Server", "setServer", "org.apache.catalina.Server");

        return (digester);

    }

    public void stopServer() {
        stopServer(null);
    }

    public void stopServer(String[] arguments) {

        if (arguments != null) {
            arguments(arguments);
        }

        if (server == null) {
            // Create and execute our Digester
            Digester digester = createStopDigester();
            digester.setClassLoader(Thread.currentThread().getContextClassLoader());
            File file = configFile();
            try {
                InputSource is = new InputSource("file://" + file.getAbsolutePath());
                FileInputStream fis = new FileInputStream(file);
                is.setByteStream(fis);
                digester.push(this);
                digester.parse(is);
                fis.close();
            } catch (Exception e) {
                log.error("Catalina.stop: ", e);
                System.exit(1);
            }
        }

        // Stop the existing server
        try {
            Socket socket = new Socket("127.0.0.1", server.getPort());
            OutputStream stream = socket.getOutputStream();
            String shutdown = server.getShutdown();
            for (int i = 0; i < shutdown.length(); i++)
                stream.write(shutdown.charAt(i));
            stream.flush();
            stream.close();
            socket.close();
        } catch (IOException e) {
            log.error("Catalina.stop: ", e);
            System.exit(1);
        }

    }

    public void setCatalinaBase() {
        initDirs();
    }

    public void setCatalinaHome() {
        initDirs();
    }

    public void load() {

        // 初始化catalina.home，和catalina.base环境变量路径
        initDirs();

        initNaming();

        Digester digester = createStartDigester();
        long t1 = System.currentTimeMillis();

        InputSource inputSource = null;
        InputStream inputStream = null;
        File file = null;
        try {
            file = configFile();
            inputStream = new FileInputStream(file);
            inputSource = new InputSource("file://" + file.getAbsolutePath());
        } catch (Exception e) {
        }
        if (inputStream == null) {
            try {
                inputStream = getClass().getClassLoader().getResourceAsStream(getConfigFile());
                inputSource = new InputSource(getClass().getClassLoader().getResource(getConfigFile()).toString());
            } catch (Exception e) {
            }
        }

        // This should be included in catalina.jar
        // Alternative: don't bother with xml, just create it manually.
        if (inputStream == null) {
            try {
                inputStream = getClass().getClassLoader().getResourceAsStream("server-embed.xml");
                inputSource = new InputSource(getClass().getClassLoader().getResource("server-embed.xml").toString());
            } catch (Exception e) {
                ;
            }
        }

        if ((inputStream == null) && (file != null)) {
            log.warn("Can't load server.xml from " + file.getAbsolutePath());
            return;
        }

        try {
            inputSource.setByteStream(inputStream);
            digester.push(this);// 将当前对象放到对象堆的最顶层
            digester.parse(inputSource); // 真正实例化server.xml中的标签生成对象.用sax来解析xml
            inputStream.close();
        } catch (Exception e) {
            log.warn("Catalina.start using " + getConfigFile() + ": ", e);
            return;
        }

        // 替换System.out和System.err的使用自定义的PrintStream
        initStreams();

        // Start the new server
        if (server instanceof Lifecycle) {
            try {
                server.initialize();
            } catch (LifecycleException e) {
                log.error("Catalina.start", e);
            }
        }

        long t2 = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            log.info("Initialization processed in " + (t2 - t1) + " ms");
        }

    }

    public void load(String args[]) {

        try {
            if (arguments(args)) {
                load();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public void create() {

    }

    public void destroy() {

    }

    public void start() {

        if (server == null) {
            load();
        }

        long t1 = System.currentTimeMillis();

        // Start the new server
        if (server instanceof Lifecycle) {
            try {
                ((Lifecycle) server).start();
            } catch (LifecycleException e) {
                log.error("Catalina.start: ", e);
            }
        }

        long t2 = System.currentTimeMillis();
        if (log.isInfoEnabled())
            log.info("Server startup in " + (t2 - t1) + " ms");

        try {
            // Register shutdown hook
            if (useShutdownHook) {
                if (shutdownHook == null) {
                    shutdownHook = new CatalinaShutdownHook();
                }
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            }
        } catch (Throwable t) {
            // This will fail on JDK 1.2. Ignoring, as Tomcat can run
            // fine without the shutdown hook.
        }

        if (await) {
            await();
            stop();
        }

    }

    public void stop() {

        try {
            // Remove the ShutdownHook first so that server.stop()
            // doesn't get invoked twice
            if (useShutdownHook) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        } catch (Throwable t) {
            // This will fail on JDK 1.2. Ignoring, as Tomcat can run
            // fine without the shutdown hook.
        }

        // Shut down the server
        if (server instanceof Lifecycle) {
            try {
                ((Lifecycle) server).stop();
            } catch (LifecycleException e) {
                log.error("Catalina.stop", e);
            }
        }

    }

    public void await() {

        server.await();

    }

    protected void usage() {

        System.out.println("usage: java org.apache.catalina.startup.Catalina" + " [ -config {pathname} ]" + " [ -nonaming ] { start | stop }");

    }

    protected class CatalinaShutdownHook extends Thread {
        public void run() {
            if (server != null) {
                Catalina.this.stop();
            }
        }
    }

    private static org.apache.juli.logging.Log log = org.apache.juli.logging.LogFactory.getLog(Catalina.class);

}

final class SetParentClassLoaderRule extends Rule {

    public SetParentClassLoaderRule(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    ClassLoader parentClassLoader = null;

    public void begin(String namespace, String name, Attributes attributes) throws Exception {

        if (digester.getLogger().isDebugEnabled())
            digester.getLogger().debug("Setting parent class loader");

        Container top = (Container) digester.peek();
        top.setParentClassLoader(parentClassLoader);
    }
}
