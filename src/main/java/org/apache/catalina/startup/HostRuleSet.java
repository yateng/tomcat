package org.apache.catalina.startup;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSetBase;

/**
 * <p>
 * <strong>RuleSet</strong> for processing the contents of a Host definition element. This <code>RuleSet</code> does NOT include any rules for nested Context or
 * DefaultContext elements, which should be added via instances of <code>ContextRuleSet</code>.
 * </p>
 * 
 * @author Craig R. McClanahan
 * @version $Revision: 302984 $ $Date: 2004-06-26 19:41:33 +0200 (sam., 26 juin 2004) $
 */

public class HostRuleSet extends RuleSetBase {

    protected String prefix = null;

    public HostRuleSet() {

        this("");

    }

    public HostRuleSet(String prefix) {

        super();
        this.namespaceURI = null;
        this.prefix = prefix;

    }

    public void addRuleInstances(Digester digester) {
        //prefix:Server/Service/Engine/
        digester.addObjectCreate(prefix + "Host", "org.apache.catalina.core.StandardHost", "className");
        digester.addSetProperties(prefix + "Host");
        digester.addRule(prefix + "Host", new CopyParentClassLoaderRule());
        digester.addRule(prefix + "Host", new LifecycleListenerRule("org.apache.catalina.startup.HostConfig", "hostConfigClass"));
        digester.addSetNext(prefix + "Host", "addChild", "org.apache.catalina.Container");

        digester.addCallMethod(prefix + "Host/Alias", "addAlias", 0);

        // Cluster configuration start
        digester.addObjectCreate(prefix + "Host/Cluster", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Host/Cluster");
        digester.addSetNext(prefix + "Host/Cluster", "setCluster", "org.apache.catalina.Cluster");
        // Cluster configuration end

        digester.addObjectCreate(prefix + "Host/Listener", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Host/Listener");
        digester.addSetNext(prefix + "Host/Listener", "addLifecycleListener", "org.apache.catalina.LifecycleListener");

        digester.addObjectCreate(prefix + "Host/Realm", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Host/Realm");
        digester.addSetNext(prefix + "Host/Realm", "setRealm", "org.apache.catalina.Realm");

        digester.addObjectCreate(prefix + "Host/Valve", null, "className");// MUST be specified in the element
        digester.addSetProperties(prefix + "Host/Valve");
        digester.addSetNext(prefix + "Host/Valve", "addValve", "org.apache.catalina.Valve");

    }

}
