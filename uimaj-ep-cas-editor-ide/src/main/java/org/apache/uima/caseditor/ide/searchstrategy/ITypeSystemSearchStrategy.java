package org.apache.uima.caseditor.ide.searchstrategy;

import org.eclipse.core.resources.IFile;

public interface ITypeSystemSearchStrategy {

  IFile findTypeSystem(IFile casFile);

}
