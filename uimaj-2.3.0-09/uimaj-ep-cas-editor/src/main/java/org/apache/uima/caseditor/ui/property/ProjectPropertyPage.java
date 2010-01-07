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

import java.io.IOException;

import org.apache.uima.caseditor.core.model.INlpElement;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;


/**
 * TODO: add javadoc here
 */
public class ProjectPropertyPage extends NlpProjectFieldEditorPage
{
    private DotCorpusPreferenceStore mDotCorpusPropertyStore;

    private FileFieldEditor mTypeSystemFile;

    private FolderPathEditor mCasProcessorFolders;

    private FolderPathEditor mCorpusFolders;

    private IntegerFieldEditor mEditorLineLengthHint;

    /**
     * Initialize a new instance.
     */
    public ProjectPropertyPage()
    {
        super(FieldEditorPreferencePage.GRID);
    }

    @Override
    protected void createFieldEditors()
    {
        NlpProject nlpProject = ((INlpElement) getElement()).getNlpProject();

        mDotCorpusPropertyStore = new DotCorpusPreferenceStore(nlpProject
                .getDotCorpus());

        Composite parent = getFieldEditorParent();

        // uima config folder
        mCasProcessorFolders = new FolderPathEditor(
                DotCorpusPreferenceStore.Key.TAGGER_CONFIG_FOLDER.name(),
                "Processor Folders", "Processor folder selection", "Select the processor folder",
                parent, nlpProject);
        mCasProcessorFolders.setPreferenceStore(mDotCorpusPropertyStore);
        addField(mCasProcessorFolders);

        // corpus folder
        mCorpusFolders = new FolderPathEditor(
                DotCorpusPreferenceStore.Key.CORPUS_FOLDERS.name(),
                "Corpus Folders", "Corpus folder selection", "Select the corpus folder",
                parent, nlpProject);

        mCorpusFolders.setPreferenceStore(mDotCorpusPropertyStore);
        addField(mCorpusFolders);

        // type system file
        mTypeSystemFile = new FileFieldEditor(
                DotCorpusPreferenceStore.Key.TYPE_SYSTEM_FILE.name(), "Typesystem",
                "Typesystem file selection", "Select the typesystem file", parent, nlpProject);
        mTypeSystemFile.setChangeButtonText("Browse...");
        mTypeSystemFile.setPreferenceStore(mDotCorpusPropertyStore);
        addField(mTypeSystemFile);

        // editor line length hint
        mEditorLineLengthHint = new IntegerFieldEditor(
                DotCorpusPreferenceStore.Key.EDITOR_LINE_LENGTH_HINT.name(),
                "Line Length Hint", parent);
        mEditorLineLengthHint.setPreferenceStore(mDotCorpusPropertyStore);
        addField(mEditorLineLengthHint);
    }

    @Override
    public boolean performOk()
    {
        mTypeSystemFile.store();
        mCorpusFolders.store();
        mCasProcessorFolders.store();
        mEditorLineLengthHint.store();

        try
        {
            mDotCorpusPropertyStore.save();
        }
        catch (IOException e)
        {
            // TODO: show error message with save error
            MessageDialog.openError(getShell(), "Unable to save settings!",
                    "Unable to save settings:" + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    protected void performDefaults()
    {
        mTypeSystemFile.loadDefault();
        mCorpusFolders.loadDefault();
        mCasProcessorFolders.loadDefault();
        mEditorLineLengthHint.loadDefault();
    }
}