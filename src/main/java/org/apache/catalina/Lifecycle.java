package org.apache.catalina;

/**
 * <td>抽象主题（Observable）:定义了管理观察者的添加，删除和通知方法。
 * 抽象观察者（Observer）:定义了主题发生变化时，具体观察者必须执行的方法。
 * 具体主题(Container):对观察者进行管理，并在自身发生变化时，通知观察者。
 * 具体观察者(ContainerConfig):实现了当主题发生变化时，应该执行的动作。
 * 
 * Lifecycle:相当于抽象主题角色，所有的容器类与组件实现类都实现了这个接口。如StandardContext
 * LifecycleListener:相当于抽象观察者角色,具体的实现类有ContextConfig, HostConfig, EngineConfig类，它们在容器启动时与停止时触发。
 * LifecycleEvent:生命周期事件，对主题与发生的事件进行封装。
 * LifecycleSupport:生命周期管理的实用类，提供对观察者的添加，删除及通知观察者的方法。
 * LifecycleException:生命周期异常类。
 * 
 * @author Craig R. McClanahan
 * @version $Revision: 303352 $ $Date: 2004-10-05 19:12:52 +0200 (mar., 05 oct. 2004) $
 */

public interface Lifecycle {

    public static final String INIT_EVENT = "init";

    public static final String START_EVENT = "start";

    public static final String BEFORE_START_EVENT = "before_start";

    public static final String AFTER_START_EVENT = "after_start";

    public static final String STOP_EVENT = "stop";

    public static final String BEFORE_STOP_EVENT = "before_stop";

    public static final String AFTER_STOP_EVENT = "after_stop";

    public static final String DESTROY_EVENT = "destroy";

    public static final String PERIODIC_EVENT = "periodic";

    public void addLifecycleListener(LifecycleListener listener);

    public LifecycleListener[] findLifecycleListeners();

    public void removeLifecycleListener(LifecycleListener listener);

    public void start() throws LifecycleException;

    public void stop() throws LifecycleException;
}
