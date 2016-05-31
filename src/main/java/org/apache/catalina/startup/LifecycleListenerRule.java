package org.apache.catalina.startup;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.tomcat.util.digester.Rule;
import org.xml.sax.Attributes;

public class LifecycleListenerRule extends Rule {

    public LifecycleListenerRule(String listenerClass, String attributeName) {
        this.listenerClass = listenerClass;
        this.attributeName = attributeName;
    }

    private String attributeName;

    /**
     * The name of the <code>LifecycleListener</code> implementation class.
     */
    private String listenerClass;

    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        // Instantiate a new LifecyleListener implementation object
        String className = listenerClass;
        if (attributeName != null) {
            String value = attributes.getValue(attributeName);
            if (value != null)
                className = value;
        }
        Class clazz = Class.forName(className);
        LifecycleListener listener = (LifecycleListener) clazz.newInstance();

        // Add this LifecycleListener to our associated component
        Lifecycle lifecycle = (Lifecycle) digester.peek();
        lifecycle.addLifecycleListener(listener);
    }
}