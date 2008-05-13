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

package org.apache.uima.taeconfigurator.editors.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

/*
 * This class forwards to TypesWithNameSpaces
 * Purpose: provide content assist under Eclipse 3.2
 * (The other class is set up to provide browse assist under 3.1 or higher Eclipse)
 */

public class TypesWithNameSpaces32 implements IContentProposalProvider {

  final private SortedMap sortedNames;
  
  public TypesWithNameSpaces32(TypesWithNameSpaces aBase) {
    sortedNames = aBase.sortedNames;
  }
  
  private CasTypeProposal [] proposalArray = null;
  
  public static class CasTypeProposal 
      implements IContentProposal, Comparable {
    private final String labelForm;
    private final String fullName;
    private final String compareKey;
    /* (non-Javadoc)
     * @see org.eclipse.jface.fieldassist.IContentProposal#getContent()
     */
    CasTypeProposal(String aCompareKey, String shortName, String nameSpace) {
      fullName = (null == nameSpace || "".equals(nameSpace))? shortName : nameSpace + "." + shortName;
      labelForm = (null == nameSpace || "".equals(nameSpace))? shortName : shortName + " - " + nameSpace;
      compareKey = aCompareKey.toLowerCase();
    }
       
    public String getContent() {
      return fullName;
    }
    /* (non-Javadoc)
     * @see org.eclipse.jface.fieldassist.IContentProposal#getCursorPosition()
     */
    public int getCursorPosition() {
      return fullName.length();
    }
    /* (non-Javadoc)
     * @see org.eclipse.jface.fieldassist.IContentProposal#getDescription()
     */
    public String getDescription() {
      return null;
    }
    /* (non-Javadoc)
     * @see org.eclipse.jface.fieldassist.IContentProposal#getLabel()
     */
    public String getLabel() {
      if (labelForm.toLowerCase().startsWith(compareKey))
        return labelForm;
      else
        return fullName;
    }
    
    public String getCompareKey() {
      return compareKey;
    }
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object arg0) throws ClassCastException {
        final CasTypeProposal c = (CasTypeProposal) arg0;
        return this.compareKey.compareTo(c.getCompareKey()); 
    }
  }
 
  public void createProposalArray() {
    List r = new ArrayList(sortedNames.size()*2);
    
    // item a.b.c.name creates 2 entries in the suggestions:
    //   compare key: a.b.c.name  label:  name - a.b.c    content: a.b.c.name
    //   compare key: name        label:  name - a.b.c    content: a.b.c.name 
    
    for (Iterator it = sortedNames.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry)it.next();
      Set nameSpaces = (Set)entry.getValue();
      for (Iterator nsi = nameSpaces.iterator(); nsi.hasNext();) {
        String nameSpace = (String)nsi.next();
        String shortName = (String)entry.getKey();
        r.add(new CasTypeProposal(shortName, shortName, nameSpace));
        if (null != nameSpace) {
          r.add(new CasTypeProposal(nameSpace + "." + shortName, shortName, nameSpace));
        }
      }
    }
    proposalArray = (CasTypeProposal[])r.toArray(new CasTypeProposal[r.size()]);
    Arrays.sort(proposalArray);
  }
  
  public CasTypeProposal [] getProposalArray() {
    return proposalArray;
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.fieldassist.IContentProposalProvider#getProposals(java.lang.String, int)
   */
  public IContentProposal[] getProposals(String contents, int position) {
    if (null == proposalArray)
      createProposalArray();
    String keyString = contents.substring(0, position).toLowerCase();
    CasTypeProposal key = new CasTypeProposal(keyString, null, null); 
    int i = Arrays.binarySearch(proposalArray, key);
    
    if (i < 0) {
      i = Math.abs(i + 1);
    }
    List rl = new ArrayList(proposalArray.length - i);
    for (; i < proposalArray.length; i++) {
      if (proposalArray[i].getCompareKey().startsWith(keyString)) {
        rl.add(proposalArray[i]);
      } else
        break;
    }
    return (CasTypeProposal[])rl.toArray(new CasTypeProposal[rl.size()]);
  } 
}
