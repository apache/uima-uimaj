/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.uima.it.pear_with_typesystem;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.internal.util.UIMAClassLoader;

public abstract class TestAnnotator_ImplBase
    extends JCasAnnotator_ImplBase
{
    protected void assertClassIsLocal(Class<?> aClazz)
    {
        if (!(getClass().getClassLoader() instanceof UIMAClassLoader)) {
            throw new RuntimeException(
                    "Expecting " + getClass().getClassLoader() + " to be a " + UIMAClassLoader.class
                            + " but it is not. Looks like we are not running as a PEAR.");
        }

        if (aClazz.getClassLoader() != getClass().getClassLoader()) {
            throw new RuntimeException("Expecting " + aClazz + " and " + getClass()
                    + " to be loaded by the same (PEAR) classloader.");
        }
    }

    protected void assertClassIsGlobal(String aClazz)
    {
        try {
            var clazz = getClass().getClassLoader().loadClass(aClazz);
            assertClassIsGlobal(clazz);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Expecting " + aClazz
                    + " to be loaded by a global classloader but it seems not to be accessible at all",
                    e);
        }
    }

    protected void assertClassIsGlobal(Class<?> aClazz)
    {
        if (aClazz.getClassLoader() instanceof UIMAClassLoader) {
            throw new RuntimeException("Expecting " + aClazz
                    + " to be loaded by a global classloader but it seems to have been loaded in a PEAR.");
        }
    }

    protected void assertClassNotAccessible(String aClass)
    {
        var failed = false;
        try {
            getClass().getClassLoader().loadClass(aClass);
        }
        catch (ClassNotFoundException e) {
            failed = true;
        }
        if (!failed) {
            throw new RuntimeException("Could load class [" + aClass + "] from ["
                    + getClass().getClassLoader() + "] but should not have been able to");
        }
    }

    protected void assertException(Class<? extends Throwable> aExpected, Runnable aCode)
    {
        try {
            aCode.run();
        }
        catch (Throwable e) {
            if (aExpected.isAssignableFrom(e.getClass())) {
                return;
            }

            throw new RuntimeException("Thrown exception [" + e.getClass().getName()
                    + "] does not match expected type [" + aExpected.getName() + "]");
        }

        throw new RuntimeException(
                "Expected exception [" + aExpected.getName() + "] but none was thrown");
    }

    protected void assertTrue(boolean aCondition)
    {
        if (!aCondition) {
            throw new RuntimeException("Expected condition to be TRUE but was not.");
        }
    }

    protected void assertFalse(boolean aCondition)
    {
        if (aCondition) {
            throw new RuntimeException("Expected condition to be FALSE but was not.");
        }
    }
}
