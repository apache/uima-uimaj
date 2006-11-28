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

package org.apache.uima.taeconfigurator.editors.xml;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.apache.uima.taeconfigurator.editors.MultiPageEditorContributor;

public class XMLConfiguration extends SourceViewerConfiguration {
  private XMLDoubleClickStrategy doubleClickStrategy;

  private XMLTagScanner tagScanner;

  private XMLScanner scanner;

  private ColorManager colorManager;

  public XMLConfiguration(ColorManager colorManager) {
    this.colorManager = colorManager;
  }

  public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    return new String[] { IDocument.DEFAULT_CONTENT_TYPE, XMLPartitionScanner.XML_COMMENT,
        XMLPartitionScanner.XML_TAG };
  }

  public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer,
          String contentType) {
    if (doubleClickStrategy == null)
      doubleClickStrategy = new XMLDoubleClickStrategy();
    return doubleClickStrategy;
  }

  protected XMLScanner getXMLScanner() {
    if (scanner == null) {
      scanner = new XMLScanner(colorManager);
      scanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager
              .getColor(IXMLColorConstants.DEFAULT))));
    }
    return scanner;
  }

  protected XMLTagScanner getXMLTagScanner() {
    if (tagScanner == null) {
      tagScanner = new XMLTagScanner(colorManager);
      tagScanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager
              .getColor(IXMLColorConstants.TAG))));
    }
    return tagScanner;
  }

  public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
    PresentationReconciler reconciler = new PresentationReconciler();

    DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getXMLTagScanner());
    reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
    reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

    dr = new DefaultDamagerRepairer(getXMLScanner());
    reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
    reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

    NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(
            colorManager.getColor(IXMLColorConstants.XML_COMMENT)));
    reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
    reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

    return reconciler;
  }

  // these 2 functions don't seem to control indent
  public int getTabWidth(ISourceViewer sourceViewer) {
    return MultiPageEditorContributor.getXMLindent();
  }

  public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
    StringBuffer spaces = new StringBuffer(4);
    int indent = getTabWidth(null);
    for (int i = 0; i < indent; i++)
      spaces.append(' ');
    return new String[] { "\t", spaces.toString() /* , "" */}; //$NON-NLS-1$ //$NON-NLS-2$ 
  }

}
