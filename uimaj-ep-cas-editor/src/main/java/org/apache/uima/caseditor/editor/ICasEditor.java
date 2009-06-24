package org.apache.uima.caseditor.editor;

import org.apache.uima.cas.CAS;
import org.eclipse.ui.IEditorPart;

/**
 * A Cas Editor is an extension to the {@link IEditorPart} interface and 
 * is responsible to view and edit a {@link CAS} object.
 */
public interface ICasEditor extends IEditorPart{
  ICasDocument getDocument();
}
