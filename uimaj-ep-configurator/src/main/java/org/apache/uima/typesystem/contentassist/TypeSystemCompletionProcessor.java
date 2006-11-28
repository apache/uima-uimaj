/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/cpl1.0.php
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *
 * This file contains portions which are 
 * derived from the following Eclipse open source files:
 * org/eclipse/jdt/internal/ui/refactoring/contentassist/CUPositionCompletionProcessor.java version 3.0
 * The Eclipse open source
 * is made available under the terms of the Eclipse Public License Version 1.0 ("EPL")
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.apache.uima.typesystem.contentassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.apache.uima.typesystem.ITypeSystemInfo;
import org.apache.uima.typesystem.StringMatcher;
import org.apache.uima.typesystem.TypeSystemInfoLabelProvider;

public class TypeSystemCompletionProcessor implements IContentAssistProcessor,
        ISubjectControlContentAssistProcessor {

  private String fErrorMessage;

  private char[] fProposalAutoActivationSet;

  private TypeSystemCompletionProposalComparator fComparator;

  private ArrayList typeSystemInfoList;

  /**
   * Creates a <code>TypeSystemPositionCompletionProcessor</code>. The completion context must be
   * set via {@link #setCompletionContext(ICompilationUnit,String,String)}.
   * 
   * @param completionRequestor
   *          the completion requestor
   */
  public TypeSystemCompletionProcessor(ArrayList typeSystemInfoList) {
    this.typeSystemInfoList = typeSystemInfoList;

    fComparator = new TypeSystemCompletionProposalComparator();
    fProposalAutoActivationSet = ".".toCharArray();
  }

  /**
   * Computing proposals on a <code>ITextViewer</code> is not supported.
   * 
   * @see #computeCompletionProposals(IContentAssistSubjectControl, int)
   * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
   */
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
    return null;
  }

  /**
   * Computing context information on a <code>ITextViewer</code> is not supported.
   * 
   * @see #computeContextInformation(IContentAssistSubjectControl, int)
   * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
   */
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
   */
  public char[] getCompletionProposalAutoActivationCharacters() {
    return fProposalAutoActivationSet;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
   */
  public char[] getContextInformationAutoActivationCharacters() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
   */
  public String getErrorMessage() {
    // throw new InternalErrorCDE("not implemented, not used");
    return fErrorMessage;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
   */
  public IContextInformationValidator getContextInformationValidator() {
    return null; // no context
  }

  /*
   * @see ISubjectControlContentAssistProcessor#computeContextInformation(IContentAssistSubjectControl,
   *      int)
   */
  public IContextInformation[] computeContextInformation(
          IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
    return null;
  }

  /*
   * @see ISubjectControlContentAssistProcessor#computeCompletionProposals(IContentAssistSubjectControl,
   *      int)
   */
  public ICompletionProposal[] computeCompletionProposals(
          IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
    String input = contentAssistSubjectControl.getDocument().get();
    if (documentOffset == 0)
      return null;
    ICompletionProposal[] proposals = internalComputeCompletionProposals(documentOffset, input);
    Arrays.sort(proposals, fComparator);
    return proposals;
  }

  private ICompletionProposal[] internalComputeCompletionProposals(int documentOffset, String input) {
    try {
      ICompletionProposal[] proposals = searchForProposals(documentOffset, input);
      return proposals;
    } catch (Exception e) {
      return null;
    }
  }

  private ICompletionProposal[] searchForProposals(int documentOffset, String input) {

    ICompletionProposal[] proposals = null;
    ArrayList packageMatchProposalsList = new ArrayList();
    ArrayList fullNameMatchProposalsList = new ArrayList();
    ArrayList classMatchProposalsList = new ArrayList();
    String realInput = null;
    int replacementLength = input.length();
    if (/* input!= null && */documentOffset > 0)
      realInput = input.substring(0, documentOffset);

    if (realInput != null && realInput.length() > 0) {
      StringMatcher fMatcher = new StringMatcher(realInput.trim() + '*', true, false);
      Iterator itr = typeSystemInfoList.iterator();
      while (itr.hasNext()) {
        ITypeSystemInfo tsi = (ITypeSystemInfo) itr.next();
        if (fMatcher.match(tsi.getPackageName())) {
          String replacementString = tsi.getPackageName();
          String displayString = tsi.getPackageName();
          packageMatchProposalsList.add(new CompletionProposal(replacementString, 0,
                  replacementLength, replacementString.length(),
                  TypeSystemInfoLabelProvider.PKG_ICON, displayString, null, null));
        }
        if (fMatcher.match(tsi.getFullName())) {
          String replacementString = tsi.getFullName();
          String displayString = tsi.getName() + " - " + tsi.getPackageName();
          fullNameMatchProposalsList.add(new CompletionProposal(replacementString, 0,
                  replacementLength, replacementString.length(),
                  TypeSystemInfoLabelProvider.CLASS_ICON, displayString, null, null));
        }
        if (fMatcher.match(tsi.getName())) {
          String replacementString = tsi.getFullName();
          String displayString = tsi.getName() + " - " + tsi.getPackageName();
          classMatchProposalsList.add(new CompletionProposal(replacementString, 0,
                  replacementLength, replacementString.length(),
                  TypeSystemInfoLabelProvider.CLASS_ICON, displayString, null, null));
        }
      }
      ICompletionProposal[] packageMatchProposals = (CompletionProposal[]) packageMatchProposalsList
              .toArray(new CompletionProposal[packageMatchProposalsList.size()]);
      ICompletionProposal[] fullNameMatchProposals = (CompletionProposal[]) fullNameMatchProposalsList
              .toArray(new CompletionProposal[fullNameMatchProposalsList.size()]);
      ICompletionProposal[] classMatchProposals = (CompletionProposal[]) classMatchProposalsList
              .toArray(new CompletionProposal[classMatchProposalsList.size()]);

      Arrays.sort(packageMatchProposals, fComparator);
      Arrays.sort(fullNameMatchProposals, fComparator);
      Arrays.sort(classMatchProposals, fComparator);

      proposals = new ICompletionProposal[packageMatchProposals.length
              + fullNameMatchProposals.length + classMatchProposals.length];
      for (int i = 0; i < packageMatchProposals.length; i++)
        proposals[i] = packageMatchProposals[i];
      for (int i = 0; i < fullNameMatchProposals.length; i++)
        proposals[i + packageMatchProposals.length] = fullNameMatchProposals[i];
      for (int i = 0; i < classMatchProposals.length; i++)
        proposals[i + packageMatchProposals.length + fullNameMatchProposals.length] = classMatchProposals[i];
    }
    return proposals;
  }
}
