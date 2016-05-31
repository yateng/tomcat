package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;

public class ContextRuleSet extends RuleSetBase {

    protected String prefix = null;

    protected boolean create = true;

    public ContextRuleSet() {

        this("");

    }

    public ContextRuleSet(String prefix) {

        super();
        this.namespaceURI = null;
        this.prefix = prefix;

    }

    public ContextRuleSet(String prefix, boolean create) {

        super();
        this.namespaceURI = null;
        this.prefix = prefix;
        this.create = create;

    }

    public void addRuleInstances(Digester digester) {

        if (create) {
            digester.addObjectCreate(prefix + "Context", "org.apache.catalina.core.StandardContext", "className");
            digester.addSetProperties(prefix + "Context");
        } else {
            digester.addRule(prefix + "Context", new SetContextPropertiesRule());
        }

        if (create) {
            digester.addRule(prefix + "Context", new LifecycleListenerRule("org.apache.catalina.startup.ContextConfig", "configClass"));
            digester.addSetNext(prefix + "Context", "addChild", "org.apache.catalina.Container");
        }
        digester.addCallMethod(prefix + "Context/InstanceListener", "addInstanceListener", 0);

        digester.addObjectCreate(prefix + "Context/Listener", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Context/Listener");
        digester.addSetNext(prefix + "Context/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");

        digester.addObjectCreate(prefix + "Context/Loader", "org.apache.catalina.loader.WebappLoader", "className");
        digester.addSetProperties(prefix + "Context/Loader");
        digester.addSetNext(prefix + "Context/Loader", "setLoader", "org.apache.catalina.Loader");

        digester.addObjectCreate(prefix + "Context/Manager", "org.apache.catalina.session.StandardManager", "className");
        digester.addSetProperties(prefix + "Context/Manager");
        digester.addSetNext(prefix + "Context/Manager", "setManager", "org.apache.catalina.Manager");

        digester.addObjectCreate(prefix + "Context/Manager/Store", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Context/Manager/Store");
        digester.addSetNext(prefix + "Context/Manager/Store", "setStore", "org.apache.catalina.Store");

        digester.addObjectCreate(prefix + "Context/Parameter", "org.apache.catalina.deploy.ApplicationParameter");
        digester.addSetProperties(prefix + "Context/Parameter");
        digester.addSetNext(prefix + "Context/Parameter", "addApplicationParameter", "org.apache.catalina.deploy.ApplicationParameter");

        digester.addObjectCreate(prefix + "Context/Realm", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Context/Realm");
        digester.addSetNext(prefix + "Context/Realm", "setRealm", "org.apache.catalina.Realm");

        digester.addObjectCreate(prefix + "Context/Resources", "org.apache.naming.resources.FileDirContext", "className");
        digester.addSetProperties(prefix + "Context/Resources");
        digester.addSetNext(prefix + "Context/Resources", "setResources", "javax.naming.directory.DirContext");

        digester.addObjectCreate(prefix + "Context/ResourceLink", "org.apache.catalina.deploy.ContextResourceLink");
        digester.addSetProperties(prefix + "Context/ResourceLink");
        digester.addRule(prefix + "Context/ResourceLink", new SetNextNamingRule("addResourceLink", "org.apache.catalina.deploy.ContextResourceLink"));

        digester.addObjectCreate(prefix + "Context/Valve", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Context/Valve");
        digester.addSetNext(prefix + "Context/Valve", "addValve", "org.apache.catalina.Valve");

        digester.addCallMethod(prefix + "Context/WatchedResource", "addWatchedResource", 0);

        digester.addCallMethod(prefix + "Context/WrapperLifecycle", "addWrapperLifecycle", 0);

        digester.addCallMethod(prefix + "Context/WrapperListener", "addWrapperListener", 0);

    }
}
