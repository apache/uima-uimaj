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

package org.apache.uima.caseditor.ui.property;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.uima.caseditor.core.model.DotCorpusElement;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;

/**
 * TODO: add javdoc here
 */
public class DotCorpusPreferenceStore implements IPersistentPreferenceStore
{
    /**
     * This is the config key, should only be used combined with the
     * <code>DotCorpusPreferenceStore</code>
     */
    public enum Key
    {
        /**
         * Name of the type system file
         */
        TYPE_SYSTEM_FILE,

        /**
         * The tagger configuration folder
         */
        TAGGER_CONFIG_FOLDER,

        /**
         * The corpus folders
         */
        CORPUS_FOLDERS,

        /**
         * Line length hint for the editor.
         */
        EDITOR_LINE_LENGTH_HINT;
    }

    private DotCorpusElement mDotCorpusElement;


    /**
     * Initializes new instance with a <code>DotCorpus</code> config object.
     *
     * @param dotCorpusElement
     */
    public DotCorpusPreferenceStore(DotCorpusElement dotCorpusElement)
    {
        mDotCorpusElement = dotCorpusElement;
    }

    /**
     * @return always false
     */
    public boolean needsSaving()
    {
        return false;
    }

    /**
     * Writes the DotCorpus to the file system.
     *
     * @throws IOException -
     *             if writing fails
     */
    public void save() throws IOException
    {
        try
        {
            mDotCorpusElement.serialize();
        }
        catch (CoreException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Not implemented
     */
    public void addPropertyChangeListener(IPropertyChangeListener listener)
    {
        // currently not implemented, no need for this method
    }

    /**
     * Not implemented
     */
    public void removePropertyChangeListener(IPropertyChangeListener listener)
    {
        // currently not implemented, no need for this method
    }

    /**
     * Not implemented
     */
    public void firePropertyChangeEvent(String name, Object oldValue,
            Object newValue)
    {
        // currently not implemented, no need for this method
    }

    /**
     * Not implemented.
     *
     * @return always false
     */
    public boolean contains(String name)
    {
        return false;
    }

    /**
     * Not implemented
     */
    public boolean isDefault(String name)
    {
        return false;
    }

    /**
     * Not implemented
     */
    public void setToDefault(String name)
    {
        // currently not implemented, no need for this method
    }

    // boolean

    /**
     * Not implemented
     */
    public boolean getDefaultBoolean(String name)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public void setDefault(String name, boolean value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public boolean getBoolean(String name)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public void setValue(String name, boolean value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    // int

    /**
     * Not implemented
     */
    public int getDefaultInt(String name)
    {
        return getInt(name);
    }

    /**
     * Not implemented
     */
    public void setDefault(String name, int value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Retrieves the following parameters:
     * + editor line length hint
     */
    public int getInt(String name)
    {
        if (Key.EDITOR_LINE_LENGTH_HINT.equals(Key.valueOf(name)))
        {
            return mDotCorpusElement.getEditorLineLengthHint();
        }
        else
        {
            throw new IllegalArgumentException("Unkown name: " + name);
        }
    }

    /**
     * Sets the following parameters:
     * + editor line length hint
     */
    public void setValue(String name, int value)
    {
        if (Key.EDITOR_LINE_LENGTH_HINT.equals(Key.valueOf(name)))
        {
            mDotCorpusElement.setEditorLineLengthHint(value);
        }
        else
        {
            throw new IllegalArgumentException("Unkown name: " + name);
        }
    }

    // long

    /**
     * Not implemented
     */
    public long getDefaultLong(String name)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public void setDefault(String name, long value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public long getLong(String name)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public void setValue(String name, long value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    // float

    /**
     * Not implemented
     */
    public float getDefaultFloat(String name)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public void setDefault(String name, float value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public float getFloat(String name)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public void setValue(String name, float value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    // double

    /**
     * Not implemented
     */
    public double getDefaultDouble(String name)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public void setDefault(String name, double value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public double getDouble(String name)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Not implemented
     */
    public void setValue(String name, double value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    // string

    /**
     * @return always null
     */
    public String getDefaultString(String name)
    {
        return getString(name);
    }

    /**
     * Not implemented
     */
    public void setDefault(String name, String defaultObject)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }

    /**
     * Retrieves the config value for the given name.
     *
     * @param name
     *            the config key
     */
    public String getString(String name)
    {
        Key key = Key.valueOf(name);

        if (Key.TAGGER_CONFIG_FOLDER.equals(key))
        {
          StringBuilder pathStringBuilder = new StringBuilder();

          for (IFolder folderName : mDotCorpusElement
                  .getCasProcessorFolders())
          {
              pathStringBuilder.append(folderName.getName());
              pathStringBuilder.append(File.pathSeparator);
          }

          return pathStringBuilder.toString();
        }
        else if (Key.TYPE_SYSTEM_FILE.equals(key))
        {
            return mDotCorpusElement.getTypeSystemFile() != null ? mDotCorpusElement
                    .getTypeSystemFile().getName() : "";
        }
        else if (Key.CORPUS_FOLDERS.equals(key))
        {
            StringBuilder pathStringBuilder = new StringBuilder();

            for (IFolder folderName : mDotCorpusElement
                    .getCorpusFolderNameList())
            {
                pathStringBuilder.append(folderName.getName());
                pathStringBuilder.append(File.pathSeparator);
            }

            return pathStringBuilder.toString();
        }
        else
        {
            throw new IllegalArgumentException("Provided key is unkown!");
        }
    }

    /**
     * Sets the config value for the given config key.
     */
    public void setValue(String name, String value)
    {
        Key key = Key.valueOf(name);

        if (Key.TAGGER_CONFIG_FOLDER.equals(key))
        {
            StringTokenizer tokenizer = new StringTokenizer(value,
                    File.pathSeparator);

            // delete all corpus folders
            for (IFolder folder : mDotCorpusElement.getCasProcessorFolders())
            {
              mDotCorpusElement.removeCasProcessorFolder(folder.getName());
            }

            while (tokenizer.hasMoreTokens())
            {
                mDotCorpusElement.addCasProcessorFolder(tokenizer.nextToken());
            }
        }
        else if (Key.TYPE_SYSTEM_FILE.equals(key))
        {
            if (value.length() != 0)
            {
                mDotCorpusElement.setTypeSystemFilename(value);
            }
            else
            {
                mDotCorpusElement.setTypeSystemFilename(null);
            }
        }
        else if (Key.CORPUS_FOLDERS.equals(key))
        {
            StringTokenizer tokenizer = new StringTokenizer(value,
                    File.pathSeparator);

            // delete all corpus folders
            for (IFolder corpus : mDotCorpusElement.getCorpusFolderNameList())
            {
                mDotCorpusElement.removeCorpusFolder(corpus);
            }

            while (tokenizer.hasMoreTokens())
            {
                mDotCorpusElement.addCorpusFolder(tokenizer.nextToken());
            }
        }
    }

    /**
     * Not implemented
     */
    public void putValue(String name, String value)
    {
        throw new IllegalArgumentException("Not expected to be used!");
    }
}