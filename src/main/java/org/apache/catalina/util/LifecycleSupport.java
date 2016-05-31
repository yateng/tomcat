package org.apache.catalina.util;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

/**
 * Support class to assist in firing LifecycleEvent notifications to
 * registered LifecycleListeners.
 * 生命周期管理的实用类，提供对观察者的添加，删除及通知观察者的方法
 * 
 * @author Craig R. McClanahan
 * @version $Id: LifecycleSupport.java 302726 2004-02-27 14:59:07Z yoavs $
 */

public final class LifecycleSupport {

    public LifecycleSupport(Lifecycle lifecycle) {
        super();
        this.lifecycle = lifecycle;
    }

    private Lifecycle lifecycle = null;

    private LifecycleListener listeners[] = new LifecycleListener[0];

    public LifecycleListener[] findLifecycleListeners() {
        return listeners;
    }

    /**
     * 添加一个监听者
     */
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (listeners) {
            LifecycleListener results[] = new LifecycleListener[listeners.length + 1];
            for (int i = 0; i < listeners.length; i++) {
                results[i] = listeners[i];
            }
            results[listeners.length] = listener;
            listeners = results;
        }
    }

    public void fireLifecycleEvent(String type, Object data) {

        LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
        LifecycleListener interested[] = null;
        synchronized (listeners) {
            interested = (LifecycleListener[]) listeners.clone();
        }
        for (int i = 0; i < interested.length; i++) {
            interested[i].lifecycleEvent(event);
        }
    }

    /**
     * 从当前监听器集合中移除监听者
     * 
     */
    public void removeLifecycleListener(LifecycleListener listener) {

        synchronized (listeners) {
            int n = -1;
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i] == listener) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;
            LifecycleListener results[] = new LifecycleListener[listeners.length - 1];
            int j = 0;
            for (int i = 0; i < listeners.length; i++) {
                if (i != n) {
                    results[j++] = listeners[i];
                }
            }
            listeners = results;
        }
    }
}
