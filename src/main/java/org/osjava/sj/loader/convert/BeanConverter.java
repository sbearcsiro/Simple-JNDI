/*
 * Copyright (c) 2005, Henri Yandell
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the 
 * following conditions are met:
 * 
 * + Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * 
 * + Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * + Neither the name of Simple-JNDI nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.osjava.sj.loader.convert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Create an object using its empty constructor, then
 * call setXxx for each pseudo property. Only String
 * properties are supported.
 * <p>
 * <pre>
 * Foo.name=Arthur
 * Foo.answer=42
 * Foo.type=com.example.Person
 * Foo.converter=org.osjava.sj.loader.convert.BeanConverter
 * </pre>
 */
public class BeanConverter implements ConverterIF {

    private static Logger LOGGER = LoggerFactory.getLogger(BeanConverter.class);

    public Object convert(Properties properties, String type) {
        String value = properties.getProperty("");

        if (value != null) {
            throw new RuntimeException("Specify the value as a pseudo property as Beans have empty constructors");
        }

        String methodName = null;

        try {
            Class c = Class.forName(type);
            Object bean = c.newInstance();
            Iterator itr = properties.keySet().iterator();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                if ("converter".equals(key) || "type".equals(key)) {
                    continue;
                }
                Object property = properties.get(key);
                if (property instanceof String) {
                    methodName = "set" + Character.toTitleCase(key.charAt(0)) + key.substring(1);
                    Method m = c.getMethod(methodName, String.class);
                    m.invoke(bean, property);
                }
                else if (property instanceof List) {
                    List list = (List) property;
                    int sz = list.size();
                    key = "add" + Character.toTitleCase(key.charAt(0)) + key.substring(1);
                    Method m = c.getMethod(key, Integer.TYPE, String.class);
                    for (int i = 0; i < sz; i++) {
                        Object item = list.get(i);
                        if (item instanceof String) {
                            m.invoke(bean, new Integer(i), item);
                        }
                        else {
                            LOGGER.error("Processing List: properties={} type={} property={} key={} item={}", properties, type, property, key, item);
                            throw new RuntimeException("Only Strings and Lists of String are supported");
                        }
                    }
                }
                else {
                    LOGGER.error("Processing List: properties={} type={} methodName={} property={} key={}", properties, type, methodName, property, key);
                    throw new RuntimeException("Only Strings and Lists of Strings are supported");
                }
            }
            return bean;
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to find class: " + type, e);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find method " + methodName + " on class: " + type, e);
        }
        catch (InstantiationException e) {
            throw new RuntimeException("Unable to instantiate class: " + type, e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to access class: " + type, e);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException("Unable to pass argument to class: " + type, e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to invoke (String) constructor on class: " + type, e);
        }

    }

}
