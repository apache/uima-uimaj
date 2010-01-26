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

import java.util.Arrays;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

/**
 * This class is able to transfer an {@link Object} object. The object gets
 * saved and only an Id is transfered.
 */
public abstract class ObjectTransfer extends ByteArrayTransfer
{
    private IDGenerator mIdGenerator = IDGenerator.getInstance();

    private String mTransferName;

    private int mTransferID;

    private byte[] mCurrentID;

    private Object mObject;

    /**
     * Initializes a new instance with a name.
     *
     * @param name - the name of current instance.
     */
    protected ObjectTransfer(String name)
    {
        mTransferName = name;

        mTransferID = registerType(mTransferName);
    }

    @Override
    protected void javaToNative(Object object, TransferData transferData)
    {
        mCurrentID = mIdGenerator.nextUniqueID();

        mObject = object;

        if (transferData != null)
        {
            super.javaToNative(mCurrentID, transferData);
        }
    }

    @Override
    protected Object nativeToJava(TransferData transferData)
    {
        byte bytes[] = (byte[]) super.nativeToJava(transferData);

        return Arrays.equals(mCurrentID, bytes) ? mObject : null;
    }

    @Override
    protected int[] getTypeIds()
    {
        return new int[]
            {
                mTransferID
            };
    }

    @Override
    protected String[] getTypeNames()
    {
        return new String[]
                {
                    mTransferName
                };
    }
}