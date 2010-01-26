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

package org.apache.uima.caseditor.editor.util;

/**
 * This class is able to transfer a {@link org.apache.uima.cas.FeatureStructure}
 * objects.
 */
public class FeatureStructureTransfer extends ObjectTransfer
{
    private static FeatureStructureTransfer sFeatureStructureTransfer;

    /**
     * Initializes a new instance.
     *
     * Note: Use {@link #getInstance() } to get a instance of
     * the FeatureStructureTransfer, singleton pattern.
     */
    private FeatureStructureTransfer()
    {
        super("FeatureStructureTransfer");
    }

    /**
     * Returns the singleton instance of the FeatureStructureTransfer.
     *
     * @return the only FeatureStructureTransfer object
     */
    public static FeatureStructureTransfer getInstance()
    {
        if (sFeatureStructureTransfer == null)
        {
            sFeatureStructureTransfer = new FeatureStructureTransfer();
        }

        return sFeatureStructureTransfer;
    }
}