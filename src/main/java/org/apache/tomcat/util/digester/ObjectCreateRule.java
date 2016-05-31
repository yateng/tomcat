/*
 * $Id: ObjectCreateRule.java 299475 2004-06-26 17:41:32Z remm $
 * Copyright 2001-2004 The Apache Software Foundation.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomcat.util.digester;

import org.xml.sax.Attributes;

/**
 * Rule implementation that creates a new object and pushes it
 * onto the object stack. When the element is complete, the
 * object will be popped
 */

public class ObjectCreateRule extends Rule {

    public ObjectCreateRule(Digester digester, String className) {
        this(className);
    }

    public ObjectCreateRule(Digester digester, Class clazz) {
        this(clazz);
    }

    public ObjectCreateRule(Digester digester, String className, String attributeName) {
        this(className, attributeName);
    }

    public ObjectCreateRule(Digester digester, String attributeName, Class clazz) {
        this(attributeName, clazz);
    }

    public ObjectCreateRule(String className) {
        this(className, (String) null);
    }

    public ObjectCreateRule(Class clazz) {
        this(clazz.getName(), (String) null);
    }

    public ObjectCreateRule(String className, String attributeName) {
        this.className = className;
        this.attributeName = attributeName;
    }

    public ObjectCreateRule(String attributeName, Class clazz) {
        this(clazz.getName(), attributeName);
    }

    protected String attributeName = null;

    protected String className = null;

    public void begin(Attributes attributes) throws Exception {
        // Identify the name of the class to instantiate
        String realClassName = className;
        if (attributeName != null) {
            String value = attributes.getValue(attributeName);
            if (value != null) {
                realClassName = value;
            }
        }
        if (digester.log.isDebugEnabled()) {
            digester.log.debug("[ObjectCreateRule]{" + digester.match + "}New " + realClassName);
        }

        // Instantiate the new object and push it on the context stack
        Class clazz = digester.getClassLoader().loadClass(realClassName);
        Object instance = clazz.newInstance();
        digester.push(instance);
    }

    public void end() throws Exception {
        Object top = digester.pop();
        if (digester.log.isDebugEnabled()) {
            digester.log.debug("[ObjectCreateRule]{" + digester.match + "} Pop " + top.getClass().getName());
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ObjectCreateRule[");
        sb.append("className=");
        sb.append(className);
        sb.append(", attributeName=");
        sb.append(attributeName);
        sb.append("]");
        return (sb.toString());
    }
}
