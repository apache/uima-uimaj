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
 * org/eclipse/jdt/internal/ui/text/java/JavaCompletionProposalComparator.java version 3.0
 * The Eclipse open source
 * is made available under the terms of the Eclipse Public License Version 1.0 ("EPL")
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.apache.uima.typesystem.contentassist;

import java.util.Comparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateProposal;

public class TypeSystemCompletionProposalComparator implements Comparator {

  private static TypeSystemCompletionProposalComparator fgInstance = new TypeSystemCompletionProposalComparator();

  public static TypeSystemCompletionProposalComparator getInstance() {
    return fgInstance;
  }

  private boolean fOrderAlphabetically;

  /**
   * Constructor for CompletionProposalComparator.
   */
  public TypeSystemCompletionProposalComparator() {
    fOrderAlphabetically = false;
  }

  public void setOrderAlphabetically(boolean orderAlphabetically) {
    fOrderAlphabetically = orderAlphabetically;
  }

  /*
   * (non-Javadoc)
   * 
   * @see Comparator#compare(Object, Object)
   */
  public int compare(Object o1, Object o2) {
    ICompletionProposal p1 = (ICompletionProposal) o1;
    ICompletionProposal p2 = (ICompletionProposal) o2;

    if (!fOrderAlphabetically) {
      int r1 = getRelevance(p1);
      int r2 = getRelevance(p2);
      int relevanceDif = r2 - r1;
      if (relevanceDif != 0) {
        return relevanceDif;
      }
    }
    return p1.getDisplayString().compareToIgnoreCase(p2.getDisplayString());
  }

  private int getRelevance(ICompletionProposal obj) {
    if (obj instanceof TemplateProposal) {
      TemplateProposal tp = (TemplateProposal) obj;
      return tp.getRelevance();
    }
    return 0;
  }

}