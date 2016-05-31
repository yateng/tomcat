package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;

public class EngineRuleSet extends RuleSetBase {

    protected String prefix = null;

    public EngineRuleSet() {

        this("");

    }

    public EngineRuleSet(String prefix) {

        super();
        this.namespaceURI = null;
        this.prefix = prefix;

    }

    public void addRuleInstances(Digester digester) {
        // prefix = Server/Service/
        digester.addObjectCreate(prefix + "Engine", "org.apache.catalina.core.StandardEngine", "className");
        digester.addSetProperties(prefix + "Engine");
        digester.addRule(prefix + "Engine", new LifecycleListenerRule("org.apache.catalina.startup.EngineConfig", "engineConfigClass"));
        digester.addSetNext(prefix + "Engine", "setContainer", "org.apache.catalina.Container");

        // Cluster configuration start
        digester.addObjectCreate(prefix + "Engine/Cluster", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Engine/Cluster");
        digester.addSetNext(prefix + "Engine/Cluster", "setCluster", "org.apache.catalina.Cluster");
        // Cluster configuration end

        digester.addObjectCreate(prefix + "Engine/Listener", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Engine/Listener");
        digester.addSetNext(prefix + "Engine/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");

        digester.addObjectCreate(prefix + "Engine/Logger", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Engine/Logger");
        digester.addSetNext(prefix + "Engine/Logger", "setLogger", "org.apache.catalina.Logger");

        digester.addObjectCreate(prefix + "Engine/Realm", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Engine/Realm");
        digester.addSetNext(prefix + "Engine/Realm", "setRealm", "org.apache.catalina.Realm");

        digester.addObjectCreate(prefix + "Engine/Valve", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Engine/Valve");
        digester.addSetNext(prefix + "Engine/Valve", "addValve", "org.apache.catalina.Valve");

    }

}
