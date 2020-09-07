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
package org.apache.uima.fit.testing.junit;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Provides a {@link CAS} object which is automatically reset before the test.
 */
public final class CasRule
    extends TestWatcher
{
    private final CAS cas;

    /**
     * Provides a CAS with an auto-detected type system.
     */
    public CasRule()
    {
        try {
            cas = CasFactory.createCas();
        }
        catch (UIMAException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Provides a CAS with the specified type system.
     * 
     * @param aTypeSystemDescription
     *            the type system used to initialize the CAS.
     */
    public CasRule(TypeSystemDescription aTypeSystemDescription)
    {
        try {
            cas = CasFactory.createCas(aTypeSystemDescription);
        }
        catch (UIMAException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the CAS object managed by this rule.
     */
    public CAS get()
    {
        return cas;
    }

    @Override
    protected void starting(Description description)
    {
        cas.reset();
    }
}