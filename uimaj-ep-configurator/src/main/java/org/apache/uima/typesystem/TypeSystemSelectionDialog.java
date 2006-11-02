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
 * org/eclipse/jdt/internal/ui/dialogs/TypeSelectionDialog.java version 3.0
 * The Eclipse open source
 * is made available under the terms of the Eclipse Public License Version 1.0 ("EPL")
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.apache.uima.typesystem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredList;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;


/**
 * A dialog to select a type from a list of types.
 */
public class TypeSystemSelectionDialog extends TwoPaneElementSelector {

	private static class TypeFilterMatcher implements FilteredList.FilterMatcher {

		private static final char END_SYMBOL= '<';
		private static final char ANY_STRING= '*';

		private StringMatcher fMatcher;
		private StringMatcher fQualifierMatcher;
		
		/*
		 * @see FilteredList.FilterMatcher#setFilter(String, boolean)
		 */
		public void setFilter(String pattern, boolean ignoreCase, boolean igoreWildCards) {
			if (pattern == null || pattern.trim().length() == 0) pattern = "*";
			int qualifierIndex= pattern.lastIndexOf("."); //$NON-NLS-1$

			// type			
			if (qualifierIndex == -1) {
				fQualifierMatcher= null;
				fMatcher= new StringMatcher(adjustPattern(pattern), ignoreCase, igoreWildCards);
				
			// qualified type
			} else {
				fQualifierMatcher= new StringMatcher(pattern.substring(0, qualifierIndex), ignoreCase, igoreWildCards);
				fMatcher= new StringMatcher(adjustPattern(pattern.substring(qualifierIndex + 1)), ignoreCase, igoreWildCards);
			}
		}

		/*
		 * @see FilteredList.FilterMatcher#match(Object)
		 */
		public boolean match(Object element) {
			if (!(element instanceof ITypeSystemInfo))
				return false;

			ITypeSystemInfo type= (ITypeSystemInfo) element;

			if (!fMatcher.match(type.getName()))
				return false;

			if (fQualifierMatcher == null)
				return true;

			return fQualifierMatcher.match(type.getPackageName());
		}
		
		private String adjustPattern(String pattern) {
			int length= pattern.length();
			if (length > 0) {
				switch (pattern.charAt(length - 1)) {
					case END_SYMBOL:
						pattern= pattern.substring(0, length - 1);
						break;
					case ANY_STRING:
						break;
					default:
						pattern= pattern + ANY_STRING;
				}
			}
			return pattern;
		}
	}
	
	private static boolean isLowerCase(char c) {
		return c == Character.toLowerCase(c);
	}
	
	/*
	 * A string comparator which is aware of obfuscated code
	 * (type names starting with lower case characters).
	 */
	private static class StringComparator implements Comparator {
	    public int compare(Object left, Object right) {
	     	String leftString= (String) left;
	     	String rightString= (String) right;  		     	
	     	if (isLowerCase(leftString.charAt(0)) &&
	     		!isLowerCase(rightString.charAt(0)))
	     		return +1;

	     	if (isLowerCase(rightString.charAt(0)) &&
	     		!isLowerCase(leftString.charAt(0)))
	     		return -1;
	     	
			int result= leftString.compareToIgnoreCase(rightString);			
			if (result == 0)
				result= leftString.compareTo(rightString);

			return result;
	    }
	}

	final ArrayList typeList;
	
	/**
	 * Constructs a type selection dialog.
	 * @param parent  the parent shell.
	 */
	public TypeSystemSelectionDialog(Shell parent, ArrayList itemList) {
		super(parent, new TypeSystemInfoLabelProvider(TypeSystemInfoLabelProvider.SHOW_TYPE_ONLY),
			new TypeSystemInfoLabelProvider(TypeSystemInfoLabelProvider.SHOW_TYPE_CONTAINER_ONLY + TypeSystemInfoLabelProvider.SHOW_ROOT_POSTFIX));
		
		typeList = itemList;
		
		setUpperListLabel(TypeSystemUIMessages.getString("TypeSystem.SelectionDialog.matchingTypesLabel")); //$NON-NLS-1$
		setLowerListLabel(TypeSystemUIMessages.getString("TypeSystem.SelectionDialog.qualifierLabel")); //$NON-NLS-1$
		
		setMatchEmptyString(true);	
		setTitle(TypeSystemUIMessages.getString("TypeSystem.SelectionDialog.title")); //$NON-NLS-1$
		setMessage(TypeSystemUIMessages.getString("TypeSystem.SelectionDialog.message")); //$NON-NLS-1$
	}

	/*
	 * @see AbstractElementListSelectionDialog#createFilteredList(Composite)
	 */
 	protected FilteredList createFilteredList(Composite parent) {
 		FilteredList list= super.createFilteredList(parent);
 		
		fFilteredList.setFilterMatcher(new TypeFilterMatcher());
		fFilteredList.setComparator(new StringComparator());
		fFilteredList.setFilter("*");
		
		return list;
	}
	
	/*
	 * @see org.eclipse.jface.window.Window#open()
	 */
	public int open() {
			
		if (typeList.isEmpty()) {
			String title= TypeSystemUIMessages.getString("TypeSystem.SelectionDialog.notypes.title"); //$NON-NLS-1$
			String message= TypeSystemUIMessages.getString("TypeSystem.SelectionDialog.notypes.message"); //$NON-NLS-1$
			MessageDialog.openInformation(getShell(), title, message);
			return CANCEL;
		}
			
		ITypeSystemInfo[] typeRefs= (ITypeSystemInfo[])typeList.toArray(new ITypeSystemInfo[typeList.size()]);
		setElements(typeRefs);
		
		return super.open();
	}
	
	/*
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
	 */
	protected void computeResult() {
		ITypeSystemInfo ref= (ITypeSystemInfo) getLowerSelectedElement();

		if (ref == null)
			return;
		try {
			ITypeSystemInfo type = ref;
				List result= new ArrayList(1);
				result.add(type);
				setResult(result);
		} catch (Throwable e) {
			String title= TypeSystemUIMessages.getString("TypeSystem.SelectionDialog.errorTitle"); //$NON-NLS-1$
			String message= TypeSystemUIMessages.getString("TypeSystem.SelectionDialog.errorMessage"); //$NON-NLS-1$
			MessageDialog.openError(getShell(), title, message);
			setResult(null);
		}
	}
	
}
