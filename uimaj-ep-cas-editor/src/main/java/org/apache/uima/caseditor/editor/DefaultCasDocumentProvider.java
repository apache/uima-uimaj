package org.apache.uima.caseditor.editor;

import org.apache.uima.cas.Type;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.DocumentElement;
import org.apache.uima.caseditor.core.model.INlpElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.part.FileEditorInput;

public class DefaultCasDocumentProvider extends
        org.apache.uima.caseditor.editor.CasDocumentProvider {

  @Override
  protected IDocument createDocument(Object element) throws CoreException {
    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile file = fileInput.getFile();

      INlpElement nlpElement = CasEditorPlugin.getNlpModel().findMember(file);

      if (nlpElement instanceof DocumentElement) {

        try {
          org.apache.uima.caseditor.editor.ICasDocument workingCopy =
                  ((DocumentElement) nlpElement).getDocument(true);

          AnnotationDocument document = new AnnotationDocument();

          document.setDocument(workingCopy);

          elementErrorStatus.remove(element);

          return document;
        } catch (CoreException e) {
          elementErrorStatus.put(element, new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK,
                  "There is a problem with the document: " + e.getMessage(), e));
        }
      } else {
        IStatus status;

        if (nlpElement == null) {
          status = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK,
                  "Document not in a corpus folder!", null);
        } else {
          status = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, "Not a cas document!",
                  null);
        }

        elementErrorStatus.put(element, status);
      }
    }

    return null;
  }

  @Override
  protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document,
          boolean overwrite) throws CoreException {

    fireElementStateChanging(element);

    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile file = fileInput.getFile();

      INlpElement nlpElement =
              org.apache.uima.caseditor.CasEditorPlugin.getNlpModel().findMember(file);

      if (nlpElement instanceof DocumentElement) {
        DocumentElement documentElement = (DocumentElement) nlpElement;

        try {
          documentElement.saveDocument();
        } catch (CoreException e) {
          fireElementStateChangeFailed(element);
          throw e;
        }
      } else {
        fireElementStateChangeFailed(element);
        return;
      }
    }

    fireElementDirtyStateChanged(element, false);
  }

  private INlpElement getNlpElement(Object element) {
    if (element instanceof FileEditorInput) {
      FileEditorInput fileInput = (FileEditorInput) element;

      IFile file = fileInput.getFile();

      return CasEditorPlugin.getNlpModel().findMember(file);
    }

    return null;
  }

  @Override
  protected AnnotationStyle getAnnotationStyle(Object element, Type type) {
    INlpElement nlpElement = getNlpElement(element);

    return nlpElement.getNlpProject().getDotCorpus().getAnnotation(type);
  }

  @Override
  protected EditorAnnotationStatus getEditorAnnotationStatus(Object element) {
    INlpElement nlpElement = getNlpElement(element);

    return nlpElement.getNlpProject().getEditorAnnotationStatus();
  }

  @Override
  protected void setEditorAnnotationStatus(Object element,
          EditorAnnotationStatus editorAnnotationStatus) {
    INlpElement nlpElement = getNlpElement(element);

    nlpElement.getNlpProject().setEditorAnnotationStatus(editorAnnotationStatus);
  }
}
