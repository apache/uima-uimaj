package org.apache.uima.caseditor.editor.searchStrategy;

import org.eclipse.core.resources.IFile;

public interface ITypeSystemSearchStrategy {

  IFile findTypeSystem(IFile casFile);

}
