/* Generated By:JJTree: Do not edit this line. AstFunction.java */

package org.apache.el.parser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.el.ELException;
import javax.el.FunctionMapper;

import org.apache.el.lang.EvaluationContext;
import org.apache.el.util.MessageFactory;


/**
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: dpatil $
 */
public final class AstFunction extends SimpleNode {

    protected String localName = "";

    protected String prefix = "";

    public AstFunction(int id) {
        super(id);
    }

    public String getLocalName() {
        return localName;
    }

    public String getOutputName() {
        if (this.prefix == null) {
            return this.localName;
        } else {
            return this.prefix + ":" + this.localName;
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public Class getType(EvaluationContext ctx)
            throws ELException {
        
        FunctionMapper fnMapper = ctx.getFunctionMapper();
        
        // quickly validate again for this request
        if (fnMapper == null) {
            throw new ELException(MessageFactory.get("error.fnMapper.null"));
        }
        Method m = fnMapper.resolveFunction(this.prefix, this.localName);
        if (m == null) {
            throw new ELException(MessageFactory.get("error.fnMapper.method",
                    this.getOutputName()));
        }
        return m.getReturnType();
    }

    public Object getValue(EvaluationContext ctx)
            throws ELException {
        
        FunctionMapper fnMapper = ctx.getFunctionMapper();
        
        // quickly validate again for this request
        if (fnMapper == null) {
            throw new ELException(MessageFactory.get("error.fnMapper.null"));
        }
        Method m = fnMapper.resolveFunction(this.prefix, this.localName);
        if (m == null) {
            throw new ELException(MessageFactory.get("error.fnMapper.method",
                    this.getOutputName()));
        }

        Class[] paramTypes = m.getParameterTypes();
        Object[] params = null;
        Object result = null;
        int numParams = this.jjtGetNumChildren();
        if (numParams > 0) {
            params = new Object[numParams];
            try {
                for (int i = 0; i < numParams; i++) {
                    params[i] = this.children[i].getValue(ctx);
                    params[i] = coerceToType(params[i], paramTypes[i]);
                }
            } catch (ELException ele) {
                throw new ELException(MessageFactory.get("error.function", this
                        .getOutputName()), ele);
            }
        }
        try {
            result = m.invoke(null, params);
        } catch (IllegalAccessException iae) {
            throw new ELException(MessageFactory.get("error.function", this
                    .getOutputName()), iae);
        } catch (InvocationTargetException ite) {
            throw new ELException(MessageFactory.get("error.function", this
                    .getOutputName()), ite.getCause());
        }
        return result;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    
    public String toString()
    {
        return ELParserTreeConstants.jjtNodeName[id] + "[" + this.getOutputName() + "]";
    }
}
