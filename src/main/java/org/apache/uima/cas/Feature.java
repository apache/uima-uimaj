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

package org.apache.uima.cas;

/**
 * The interface that describes features in the type system.
 * 
 * <p>
 * <a name="names">Feature short or base names</a> are <a
 * href="./Type.html#identifiers"> type system identifiers</a>. The (fully)
 * qualified name of a feature is the <a href="./Type.html#names">name</a> of
 * the type it is defined on, followed by a colon, followed by the its short
 * name. For example, the qualified name of the Annotation begin feature is
 * <code>uima.tcas.Annotation:begin</code>.
 * 
 * 
 */
public interface Feature extends Comparable {

    /**
     * Get the domain type for this feature.
     * 
     * @return The domain type. This can not be <code>null</code>.
     */
    Type getDomain();

    /**
     * Get the range type for this feature.
     * 
     * @return The range type. This can not be <code>null</code>.
     */
    Type getRange();

    /**
     * Get the <a href="#names">qualified name</a> for this feature.
     * 
     * @return The name.
     */
    String getName();

    /**
     * Get the <a href="#names">unqualified, short name</a> of this feature.
     * 
     * @return The short name.
     */
    String getShortName();

    /**
     * Checks if there can be multiple references to values of this feature.
     * There can only be a single reference to a value of a feature if the value
     * type is primitive, or if the feature is array valued and has been
     * declared in the type system to not allow multiple references.
     * 
     * @return <code>true</code> iff the value type of this feature is
     *         primitive, or if it's an array valued feature and has been
     *         declared not to allow multiple references.
     */
    boolean isMultipleReferencesAllowed();

}
