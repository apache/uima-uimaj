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


import org.apache.uima.cas.CAS;
import org.apache.uima.caseditor.core.model.DocumentElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * This is the <code>DocumentElement</code> property page.
 * It shows information about the selected document.
 */
public class DocumentPropertyPage extends PropertyPage
{
    private Text mLanguageText;

    private CAS mCAS;

    @Override
    protected Control createContents(Composite parent)
    {
        DocumentElement document = (DocumentElement) getElement();

        try {
			mCAS = document.getDocument(false).getCAS();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        Composite base = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.horizontalSpacing = 10;
        base.setLayout(layout);

        Label charNumberLabel = new Label(base, SWT.NONE);
        charNumberLabel.setText("Characters:");

        // char number
        Label charNumberValue = new Label(base, SWT.NONE);
        charNumberValue.setText(Integer.toString(mCAS.getDocumentText()
                .length()));

        // tokens
        /*
        Label tokenNumberLabel = new Label(base, SWT.NONE);
        tokenNumberLabel.setText("Tokens:");

        Label tokenNumberValueLabel = new Label(base, SWT.NONE);

        Type tokenType = mTCAS.getTypeSystem().getType(
                "com.calcucare.nlp.Token");

        int numberOfTokens = mTCAS.getAnnotationIndex(tokenType).size();

        tokenNumberValueLabel.setText(Integer.toString(numberOfTokens));
        */

        // sentences
        /*
        Label sentenceNumberLabel = new Label(base, SWT.NONE);
        sentenceNumberLabel.setText("Sentences:");

        Label sentenceNumberValueLabel = new Label(base, SWT.NONE);

        Type sentenceType = mTCAS.getTypeSystem().getType(
                "com.calcucare.nlp.Sentence");

        int numberOfSentences = mTCAS.getAnnotationIndex(sentenceType).size();

        sentenceNumberValueLabel.setText(Integer.toString(numberOfSentences));
        */

        // document language
        Label languageLabel = new Label(base, SWT.NONE);
        languageLabel.setText("Language:");

        mLanguageText = new Text(base, SWT.BORDER);
        mLanguageText.setText(mCAS.getDocumentLanguage());

        return base;
    }

    @Override
    public boolean performOk()
    {
        mCAS.setDocumentLanguage(mLanguageText.getText());

        return super.performOk();
    }
}