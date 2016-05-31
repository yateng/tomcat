package org.apache.catalina.startup;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.catalina.security.SecurityClassLoad;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Bootstrap第一个功能是引导Catalina,Bootstrap构造一个class loader来加载Catalina的内部类（所有在catalina.home中的jar文件）
 * 第二个功能是启动container。
 * 实现catalina的内部类和系统的class path以及应用程序中的class要区分开不能相互访问的目的。
 *
 */
public final class Bootstrap {

    private static Log log = LogFactory.getLog(Bootstrap.class);

    protected static final String CATALINA_HOME_TOKEN = "${catalina.home}";

    protected static final String CATALINA_BASE_TOKEN = "${catalina.base}";

    private static Bootstrap daemon = null;

    private Object catalinaDaemon = null;

    protected ClassLoader commonLoader = null;

    protected ClassLoader catalinaLoader = null;

    protected ClassLoader sharedLoader = null;

    /** 
     * 启动入口
     */
    public static void main(String args[]) {
        if (daemon == null) {
            daemon = new Bootstrap();
            try {
                daemon.init();
            } catch (Throwable t) {
                t.printStackTrace();
                return;
            }
        }

        try {
            String command = "start";
            if (args.length > 0) {
                command = args[args.length - 1];
            }
            if (command.equals("startd")) {
                args[0] = "start";
                daemon.load(args);
                daemon.start();
            } else if (command.equals("stopd")) {
                args[0] = "stop";
                daemon.stop();
            } else if (command.equals("start")) {
                daemon.setAwait(true);
                daemon.load(args);
                daemon.start();
            } else if (command.equals("stop")) {
                daemon.stopServer(args);
            } else {
                log.warn("Bootstrap: command \"" + command + "\" does not exist.");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     *
     *  commonLoader             
     *  |
     *  |---------------catalinaLoader
     *  |
     *  |---------------sharedLoader
     *  |               |
     *  |               |------------webAppClassLoaderA
     *  |               |
     *  |               |------------webAppClassLoaderB
     *  |
     * 初始化classLoader，这里分别创建了3个classLoader[common,catalina,sharedLoader]
     * 其中common没有父亲，另外两个的父亲是common
     */
    private void initClassLoaders() {
        try {
            commonLoader = createClassLoader("common", null);
            if (commonLoader == null) {
                // no config file, default to this loader - we might be in a 'single' env.
                commonLoader = this.getClass().getClassLoader();
            }
            catalinaLoader = createClassLoader("server", commonLoader); // java.net.URLClassLoader 
            sharedLoader = createClassLoader("shared", commonLoader);   // java.net.URLClassLoader  
        } catch (Throwable t) {
            log.error("Class loader creation threw exception", t);
            System.exit(1);
        }
    }

    private ClassLoader createClassLoader(String name, ClassLoader parent) throws Exception {
        // 从conf目录下的catalina.properties文件中读取common.loader类加载器（tomcat中默认是读取lib目录下）
        // ${catalina.home}/lib,${catalina.home}/lib/*.jar
        String value = CatalinaProperties.getProperty(name + ".loader"); 
        if ((value == null) || (value.equals("")))
            return parent;

        // repository:英文翻译是仓库的意思
        ArrayList<String> repositoryLocations = new ArrayList<String>();
        ArrayList<Integer> repositoryTypes = new ArrayList<Integer>();
        int i;

        StringTokenizer tokenizer = new StringTokenizer(value, ",");
        while (tokenizer.hasMoreElements()) {
            String repository = tokenizer.nextToken();
            // Local repository
            boolean replace = false;
            String before = repository;
            while ((i = repository.indexOf(CATALINA_HOME_TOKEN)) >= 0) {
                replace = true;
                if (i > 0) {
                    repository = repository.substring(0, i) + getCatalinaHome() + repository.substring(i + CATALINA_HOME_TOKEN.length());
                } else {
                    repository = getCatalinaHome() + repository.substring(CATALINA_HOME_TOKEN.length());
                }
            }
            while ((i = repository.indexOf(CATALINA_BASE_TOKEN)) >= 0) {
                replace = true;
                if (i > 0) {
                    repository = repository.substring(0, i) + getCatalinaBase() + repository.substring(i + CATALINA_BASE_TOKEN.length());
                } else {
                    repository = getCatalinaBase() + repository.substring(CATALINA_BASE_TOKEN.length());
                }
            }
            if (replace && log.isDebugEnabled()) {
                log.debug("Expanded " + before + " to " + replace);
            }

            // Check for a JAR URL repository
            try {
                // 当解析jar时，才生效;
                @SuppressWarnings("unused")
                URL url = new URL(repository);
                repositoryLocations.add(repository);
                repositoryTypes.add(ClassLoaderFactory.IS_URL);
                continue;
            } catch (MalformedURLException e) {
            }

            if (repository.endsWith("*.jar")) {
                repository = repository.substring(0, repository.length() - "*.jar".length());
                repositoryLocations.add(repository);
                repositoryTypes.add(ClassLoaderFactory.IS_GLOB);
            } else if (repository.endsWith(".jar")) {
                repositoryLocations.add(repository);
                repositoryTypes.add(ClassLoaderFactory.IS_JAR);
            } else {
                repositoryLocations.add(repository);
                repositoryTypes.add(ClassLoaderFactory.IS_DIR);
            }
        }

        String[] locations = (String[]) repositoryLocations.toArray(new String[0]);
        Integer[] types = (Integer[]) repositoryTypes.toArray(new Integer[0]);

        ClassLoader classLoader = ClassLoaderFactory.createClassLoader(locations, types, parent);

        // Retrieving MBean server
        MBeanServer mBeanServer = null;
        if (MBeanServerFactory.findMBeanServer(null).size() > 0) {
            mBeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
        } else {
            mBeanServer = MBeanServerFactory.createMBeanServer();
        }

        // Register the server classloader
        ObjectName objectName = new ObjectName("Catalina:type=ServerClassLoader,name=" + name);
        mBeanServer.registerMBean(classLoader, objectName);

        return classLoader;

    }

    private void init() throws Exception {

        // Set Catalina path
        setCatalinaHome(); // D:\workspace\tomcat
        setCatalinaBase(); // D:\workspace\tomcat

        initClassLoaders(); // 先初始化classLoader，包括common，catalina以及shared

        Thread.currentThread().setContextClassLoader(catalinaLoader); // 设置当前线程classLoader

        SecurityClassLoad.securityClassLoad(catalinaLoader);// 安全classLoader？

        // Load our startup class and call its process() method
        if (log.isDebugEnabled()) {
            log.debug("Loading startup class");
        }
        // 这里加载catalina类型是用catalinaloader来加载的
        Class<?> startupClass = catalinaLoader.loadClass("org.apache.catalina.startup.Catalina");
        // 创建org.apache.catalina.startup.Catalina对象
        Object startupInstance = startupClass.newInstance();

        // Set the shared extensions class loader
        if (log.isDebugEnabled()) {
            log.debug("Setting startup class properties");
        }
        String methodName = "setParentClassLoader";
        Class<?> paramTypes[] = new Class[1];
        paramTypes[0] = Class.forName("java.lang.ClassLoader");
        Object paramValues[] = new Object[1];
        paramValues[0] = sharedLoader;
        Method method = startupInstance.getClass().getMethod(methodName, paramTypes);
        // 调用刚刚创建的org.apache.catalina.startup.Catalina对象的setParentClassLoader设置classLoader，shareloader
        method.invoke(startupInstance, paramValues);

        catalinaDaemon = startupInstance;// 将这个启动的实例保存起来，这里引用的是catalina的类型的对象

    }

    private void load(String[] arguments) throws Exception {
        // 调用容器的load方法
        String methodName = "load";
        Object param[];
        Class<?> paramTypes[];
        if (arguments == null || arguments.length == 0) {
            paramTypes = null;
            param = null;
        } else {
            paramTypes = new Class[1];
            paramTypes[0] = arguments.getClass();
            param = new Object[1];
            param[0] = arguments;
        }
        Method method = catalinaDaemon.getClass().getMethod(methodName, paramTypes);
        if (log.isDebugEnabled()) {
            log.debug("Calling startup class " + method);
        }
        method.invoke(catalinaDaemon, param);

    }

    public void init(String[] arguments) throws Exception {

        init();
        load(arguments);

    }

    public void start() throws Exception {
        // 再次检查，查看是否已经初始化环境变量，路径，类加载器等
        if (catalinaDaemon == null) {
            init();
        }

        Method method = catalinaDaemon.getClass().getMethod("start", (Class[]) null);
        method.invoke(catalinaDaemon, (Object[]) null);

    }

    public void stop() throws Exception {

        Method method = catalinaDaemon.getClass().getMethod("stop", (Class[]) null);
        method.invoke(catalinaDaemon, (Object[]) null);

    }

    public void stopServer() throws Exception {

        Method method = catalinaDaemon.getClass().getMethod("stopServer", (Class[]) null);
        method.invoke(catalinaDaemon, (Object[]) null);

    }

    public void stopServer(String[] arguments) throws Exception {

        Object param[];
        Class<?> paramTypes[];
        if (arguments == null || arguments.length == 0) {
            paramTypes = null;
            param = null;
        } else {
            paramTypes = new Class[1];
            paramTypes[0] = arguments.getClass();
            param = new Object[1];
            param[0] = arguments;
        }
        Method method = catalinaDaemon.getClass().getMethod("stopServer", paramTypes);
        method.invoke(catalinaDaemon, param);

    }

    public void setAwait(boolean await) throws Exception {

        Class<?> paramTypes[] = new Class[1];
        paramTypes[0] = Boolean.TYPE;
        Object paramValues[] = new Object[1];
        paramValues[0] = new Boolean(await);
        Method method = catalinaDaemon.getClass().getMethod("setAwait", paramTypes);
        method.invoke(catalinaDaemon, paramValues);

    }

    public boolean getAwait() throws Exception {
        Class<?> paramTypes[] = new Class[0];
        Object paramValues[] = new Object[0];
        Method method = catalinaDaemon.getClass().getMethod("getAwait", paramTypes);
        Boolean b = (Boolean) method.invoke(catalinaDaemon, paramValues);
        return b.booleanValue();
    }

    public void destroy() {
    }

    public void setCatalinaHome(String s) {
        System.setProperty("catalina.home", s);
    }

    public void setCatalinaBase(String s) {
        System.setProperty("catalina.base", s);
    }

    private void setCatalinaBase() {
        if (System.getProperty("catalina.base") != null) {
            return;
        }
        if (System.getProperty("catalina.home") != null) {
            System.setProperty("catalina.base", System.getProperty("catalina.home"));
        } else {
            System.setProperty("catalina.base", System.getProperty("user.dir"));
        }
    }

    private void setCatalinaHome() {
        if (System.getProperty("catalina.home") != null) {
            return;
        }
        File bootstrapJar = new File(System.getProperty("user.dir"), "bootstrap.jar");
        if (bootstrapJar.exists()) {
            try {
                System.setProperty("catalina.home", (new File(System.getProperty("user.dir"), "..")).getCanonicalPath());
            } catch (Exception e) {
                // Ignore
                System.setProperty("catalina.home", System.getProperty("user.dir"));
            }
        } else {
            System.setProperty("catalina.home", System.getProperty("user.dir"));
        }
    }

    public static String getCatalinaHome() {
        return System.getProperty("catalina.home", System.getProperty("user.dir"));
    }

    public static String getCatalinaBase() {
        return System.getProperty("catalina.base", getCatalinaHome());
    }
}
