/*
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

package org.apache.uima.caseditor.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIntConstraint;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.editor.action.DeleteFeatureStructureAction;
import org.apache.uima.caseditor.editor.action.LowerLeftAnnotationSideAction;
import org.apache.uima.caseditor.editor.action.LowerRightAnnotationSideAction;
import org.apache.uima.caseditor.editor.action.WideLeftAnnotationSideAction;
import org.apache.uima.caseditor.editor.action.WideRightAnnotationSideAction;
import org.apache.uima.caseditor.editor.annotation.DrawingStyle;
import org.apache.uima.caseditor.editor.annotation.EclipseAnnotationPeer;
import org.apache.uima.caseditor.editor.context.AnnotationEditingControlCreator;
import org.apache.uima.caseditor.editor.contextmenu.IModeMenuListener;
import org.apache.uima.caseditor.editor.contextmenu.IShowAnnotationsListener;
import org.apache.uima.caseditor.editor.contextmenu.ModeMenu;
import org.apache.uima.caseditor.editor.contextmenu.ShowAnnotationsMenu;
import org.apache.uima.caseditor.editor.outline.AnnotationOutline;
import org.apache.uima.caseditor.editor.outline.OutlinePageBook;
import org.apache.uima.caseditor.editor.util.AnnotationComparator;
import org.apache.uima.caseditor.editor.util.AnnotationSelection;
import org.apache.uima.caseditor.editor.util.FeatureStructureTransfer;
import org.apache.uima.caseditor.editor.util.Span;
import org.apache.uima.caseditor.editor.util.StrictTypeConstraint;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationPainter;
import org.eclipse.jface.text.source.AnnotationPainter.IDrawingStrategy;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBookView;
import org.eclipse.ui.texteditor.IStatusField;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.ui.texteditor.StatusTextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * An editor to annotate text.
 */
public final class AnnotationEditor extends StatusTextEditor implements ICasEditor, ISelectionListener {
  
  private abstract class AbstractAnnotateAction extends Action
      implements ISelectionChangedListener {
  
    public void selectionChanged(SelectionChangedEvent event) {
      setEnabled(AnnotationEditor.this.getSelection().y - 
          AnnotationEditor.this.getSelection().x > 0);
    }
  }
  
  /**
   * This action annotates the selected text with a defined tag.
   */
  private class QuickAnnotateAction extends AbstractAnnotateAction {
    
    private static final String ID = "QuickAnnotate";
    
    private StyledText mTextWidget;

    /**
     * Initializes a new instance.
     *
     * @param textWidget
     */
    QuickAnnotateAction(StyledText textWidget) {
      mTextWidget = textWidget;
    }

    /**
     * Executes this action, adds an annotation of the marked span.
     */
    @Override
    public void run() {
      if (isSomethingSelected()) {
        Point selection = mTextWidget.getSelectionRange();

        // get old annotations of current type for this area
        // if there is something ... the delete them and add
        Collection<AnnotationFS> oldAnnotations = getAnnotation(getDocument().getCAS(),
            getAnnotationMode(), new Span(selection.x, selection.y));

        if (!oldAnnotations.isEmpty()) {
          getDocument().removeFeatureStructures(oldAnnotations);
        }

        int start = selection.x;
        int end = start + selection.y;

        AnnotationFS annotation = getDocument().getCAS().createAnnotation(getAnnotationMode(),
                start, end);

        getDocument().addFeatureStructure(annotation);

        setAnnotationSelection(annotation);
      }
    }

    ICasDocument getDocument() {
      return AnnotationEditor.this.getDocument();
    }
  }

  private class SmartAnnotateAction extends AbstractAnnotateAction {

    private static final String ID = "Annotate";
    
    @Override
    public void run() {

      if (isSomethingSelected()) {

        QuickTypeSelectionDialog typeDialog =
          new QuickTypeSelectionDialog(Display.getCurrent().getActiveShell(),
          AnnotationEditor.this);

        typeDialog.open();
      }
    }
  }

  /**
   * Shows the annotation editing context for the active annotation.
   */
  private class ShowAnnotationContextEditAction extends Action {
    private InformationPresenter mPresenter;

    /**
     * Initializes a new instance.
     */
    ShowAnnotationContextEditAction() {
      mPresenter = new InformationPresenter(new AnnotationEditingControlCreator());

      mPresenter.setInformationProvider(new AnnotationInformationProvider(AnnotationEditor.this),
              org.eclipse.jface.text.IDocument.DEFAULT_CONTENT_TYPE);
      mPresenter.setDocumentPartitioning(org.eclipse.jface.text.IDocument.DEFAULT_CONTENT_TYPE);
      mPresenter.install(getSourceViewer());
    }

    /**
     * Executes this action, shows context information.
     */
    @Override
    public void run() {
      mPresenter.showInformation();

      // the information presenter closes ... if
      // the subject control sends a keyDown event

      // this action is triggered with
      // a keyEvent send by the subject control

      // inside this handler the presenter gets installed on
      // the subject control during that the presenter registers
      // a key listener on the subject control

      // the presenter closes now because the keyEvent to trigger
      // this action is also sent to the presenter

      // to avoid this behavior this action is reposted
      // to the END of the ui thread queue

      // this does not happen if for the action is no key explicit
      // configured with setActivationKey(...)

      // TODO:
      // This does not work for mac or linux
    }
  }

  /**
   * This <code>IDocumentChangeListener</code> is responsible to synchronize annotation in the
   * document with the annotations in eclipse.
   */
  private class DocumentListener extends AbstractAnnotationDocumentListener {
    
    /**
     * Adds a collection of annotations.
     *
     * @param annotations
     */
    @Override
    public void addedAnnotation(Collection<AnnotationFS> annotations) {
      IAnnotationModelExtension annotationModel = (IAnnotationModelExtension) getDocumentProvider().getAnnotationModel(getEditorInput());
      
      Map<Annotation, Position> addAnnotationMap = new HashMap<Annotation, Position>();
      
      for (AnnotationFS annotation : annotations) {
        addAnnotationMap.put(new EclipseAnnotationPeer(annotation), new Position(annotation.getBegin(), 
                annotation.getEnd() - annotation.getBegin()));
      }
      
      annotationModel.replaceAnnotations(null, addAnnotationMap);
    }

    /**
     * Removes a collection of annotations.
     *
     * @param deletedAnnotations
     */
    @Override
    public void removedAnnotation(Collection<AnnotationFS> deletedAnnotations) {

      if (getSite().getPage().getActivePart() == AnnotationEditor.this) {
        mFeatureStructureSelectionProvider.clearSelection();
      } else {
        mFeatureStructureSelectionProvider.clearSelectionSilently();
      }

      highlight(0, 0); // TODO: only if removed annotation was selected

      IAnnotationModelExtension annotationModel = (IAnnotationModelExtension) getDocumentProvider().getAnnotationModel(getEditorInput());
      
      Annotation removeAnnotations[] = new Annotation[deletedAnnotations.size()];
      int removeAnnotationsIndex = 0;
      for (AnnotationFS annotation : deletedAnnotations) {
        removeAnnotations[removeAnnotationsIndex++] = new EclipseAnnotationPeer(annotation);
      }
      
      annotationModel.replaceAnnotations(removeAnnotations, null);
    }

    /**
     *
     * @param annotations
     */
    @Override
    public void updatedAnnotation(Collection<AnnotationFS> annotations) {
      
      IAnnotationModelExtension annotationModel = (IAnnotationModelExtension) getDocumentProvider().getAnnotationModel(getEditorInput());
      
      for (AnnotationFS annotation : annotations) {
        annotationModel.modifyAnnotationPosition(new EclipseAnnotationPeer(annotation), 
                new Position(annotation.getBegin(), annotation.getEnd() - annotation.getBegin()));
      }
      
      selectionChanged(getSite().getPage().getActivePart(), mFeatureStructureSelectionProvider.getSelection());
    }

    public void changed() {
      mFeatureStructureSelectionProvider.clearSelection();

      syncAnnotations();
    }
    
    public void viewChanged(String oldViewName, String newViewName) {
      // TODO: Currently do nothing ... 
    }
  }

  /**
   * Sometimes the wrong annotation is selected ... ????
   */
  private class FeatureStructureDragListener implements DragSourceListener {
    private boolean mIsActive;

    private AnnotationFS mCandidate;

    FeatureStructureDragListener(final StyledText textWidget) {
      textWidget.addKeyListener(new KeyListener() {
        public void keyPressed(KeyEvent e) {
          if (e.keyCode == SWT.ALT) {
            mIsActive = true;

            textWidget.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND));
          }
        }

        public void keyReleased(KeyEvent e) {
          if (e.stateMask == SWT.ALT) {
            mIsActive = false;
            textWidget.setCursor(null);
          }
        }
      });

      textWidget.addMouseMoveListener(new MouseMoveListener() {

        public void mouseMove(MouseEvent e) {
          if (mIsActive) {
            // try to get the position inside the text

            int offset;

            // Workaround: It is possible check that is text under the cursor ?
            try {
              offset = textWidget.getOffsetAtLocation(new Point(e.x, e.y));
            }
            catch (IllegalArgumentException e2) {
              return;
            }

            Map<Integer, AnnotationFS> view = getView(getAnnotationMode());

            mCandidate = view.get(offset);

            if (mCandidate != null) {
              textWidget.setSelectionRange(mCandidate.getBegin(), mCandidate.getEnd()
                      - mCandidate.getBegin());
            }
          }
        }

      });
    }

    public void dragStart(DragSourceEvent event) {
      if (mIsActive) {
        event.doit = mCandidate != null;
      } else {
        event.doit = false;
      }
    }

    public void dragSetData(DragSourceEvent event) {
      event.data = mCandidate;
    }

    public void dragFinished(DragSourceEvent event) {
    }
  }

  private class AnnotationAccess implements IAnnotationAccess, IAnnotationAccessExtension {

    public Object getType(Annotation annotation) {
      return null;
    }

    public boolean isMultiLine(Annotation annotation) {
      return false;
    }

    public boolean isTemporary(Annotation annotation) {
      return false;
    }

    public int getLayer(Annotation annotation) {

      if (annotation instanceof EclipseAnnotationPeer) {

        EclipseAnnotationPeer eclipseAnnotation = (EclipseAnnotationPeer) annotation;

        // ask document provider for this
        // getAnnotation must be added to cas document provider

        AnnotationStyle style = getAnnotationStyle(
            eclipseAnnotation.getAnnotationFS().getType());

        return style.getLayer();
      }
      else {
        return 0;
      }
    }

    public Object[] getSupertypes(Object annotationType) {
      return new Object[0];
    }

    public String getTypeLabel(Annotation annotation) {
      return null;
    }

    public boolean isPaintable(Annotation annotation) {
      assert false : "Should never be called";

      return false;
    }

    /**
     * This implementation imitates the behavior without the
     * {@link IAnnotationAccessExtension}.
     */
    public boolean isSubtype(Object annotationType, Object potentialSupertype) {

      Type type = getDocument().getCAS().getTypeSystem().getType((String) annotationType);

      return mShowAnnotationsMenu.getSelectedTypes().contains(type) ||
          getAnnotationMode().equals(type);
    }

    public void paint(Annotation annotation, GC gc, Canvas canvas, Rectangle bounds) {
      assert false : "Should never be called";
    }
  }

  // TODO: Move to external class
  static class CasViewMenu extends ContributionItem {
    
    private AnnotationEditor casEditor;
    
    public CasViewMenu(AnnotationEditor casEditor) {
      this.casEditor = casEditor;
    } 
    
    @Override
    public void fill(Menu parentMenu, int index) {
      
      CAS cas = casEditor.getDocument().getCAS();
      
      for (Iterator<CAS> it = cas.getViewIterator(); it.hasNext(); ) {
        
        CAS casView = it.next();
        final String viewName = casView.getViewName();
        
        final MenuItem actionItem = new MenuItem(parentMenu, SWT.CHECK);
        actionItem.setText(viewName);
        
        // TODO: Disable non-text views, check mime-type
        try {
          actionItem.setEnabled(cas.getDocumentText() != null);
        } catch (Throwable t) {
          // TODO: Not nice, discuss better solution on ml
          actionItem.setEnabled(false); 
        }
        
        // TODO: Add support for non text views, editor has
        //       to display some error message
        
        if (cas.getViewName().equals(viewName))
            actionItem.setSelection(true);
        
        // TODO: move this to an action
        actionItem.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            // Trigger only if view is really changed
            // TODO: Move this check to the document itself ...
            if(!casEditor.getDocument().getCAS().getViewName().equals(viewName)) {
                casEditor.showView(viewName);
            }
          }
        });
      }
    }
  }
  
  private Type mAnnotationMode;

  /**
   * The outline page belonging to this editor.
   */
  private OutlinePageBook mOutlinePage = new OutlinePageBook();

  private Set<IAnnotationEditorModifyListener> mEditorListener = new HashSet<IAnnotationEditorModifyListener>();

  private ListenerList mEditorInputListener = new ListenerList();
  
  /**
   * TODO: Do we really need this position variable ?
   */
  private int mCursorPosition;

  boolean mIsSomethingHighlighted = false;

  private StyleRange mCurrentStyleRange;

  private FeatureStructureSelectionProvider mFeatureStructureSelectionProvider;

  private AnnotationPainter mPainter;

  private ShowAnnotationsMenu mShowAnnotationsMenu;

  private DocumentListener mAnnotationSynchronizer;

  private AnnotationStyleChangeListener mAnnotationStyleListener;
  
  private Collection<Type> shownAnnotationTypes = new HashSet<Type>();
  
  private IPropertyChangeListener preferenceStoreChangeListener;
  
  private CasDocumentProvider casDocumentProvider;

  /**
   * Creates an new AnnotationEditor object.
   */
  public AnnotationEditor() {
  }

  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
    casDocumentProvider =
            CasDocumentProviderFactory.instance().getDocumentProvider(input);
    
    setDocumentProvider(new TextDocumentProvider(casDocumentProvider));

    super.init(site, input);
  }
  
  public CasDocumentProvider getCasDocumentProvider() {
    return casDocumentProvider;
  }
  
  /**
   * Retrieves the tooltip of the title.
   *
   * @return string of tooltip
   */
  @Override
  public String getTitleToolTip() {
    if (getEditorInput() == null) {
      return super.getTitleToolTip();
    }
    ICasDocument document = getDocument();
    if(document == null) {
      return super.getTitleToolTip();
    }
    String typeSystemText = document.getTypeSystemText();
    String toolTipText = getEditorInput().getToolTipText();
    if (typeSystemText != null) {
      return toolTipText + " (" + typeSystemText + ")";
    } else {
      return toolTipText;
    }
  }
  
  /**
   * Retrieves annotation editor adapters.
   *
   * @param adapter
   * @return an adapter or null
   */
  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {

    if (IContentOutlinePage.class.equals(adapter) && getDocument() != null) {
      return mOutlinePage;
    }
    else if (CAS.class.equals(adapter) && getDocument() != null) {
      return getDocument().getCAS();
    }
    else {
      return super.getAdapter(adapter);
    }

  }

  @Override
  protected ISourceViewer createSourceViewer(Composite parent,
          org.eclipse.jface.text.source.IVerticalRuler ruler, int styles) {
    SourceViewer sourceViewer = new SourceViewer(parent, ruler, styles);

    sourceViewer.setEditable(false);

    mPainter = new AnnotationPainter(sourceViewer, new AnnotationAccess());

    sourceViewer.addPainter(mPainter);
    
    return sourceViewer;
  }

  private void setTextSize(int newSize) {
    Font font = getSourceViewer().getTextWidget().getFont();
    
    if (font.getFontData().length > 0) {
      FontData fd = font.getFontData()[0];
      getSourceViewer().getTextWidget().setFont(new Font(font.getDevice(),
              fd.getName(), newSize, fd.getStyle()));
    }
  }
  
  /**
   * Configures the editor.
   * 
   * @param parent
   */
  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);

    /*
     * this is a workaround for the quickdiff assertion if nothing was changed, how to do this
     * better ? is this the right way ?
     */
    showChangeInformation(false);

    // UIMA-2167:
    // The Annotation Editor needs to listen to changes for the caret position,
    // the new eclipse 3.5 API allows to register a listener which reports
    // that
    //
    // Until this can be used we improvise by registering a couple of listeners which
    // compute a caret offset change based on events which could change it, 
    // that are key and mouse listeners.
    
    getSourceViewer().getTextWidget().addKeyListener(new KeyListener() {
      public void keyPressed(KeyEvent e) {
        int newCaretOffset = getSourceViewer().getTextWidget().getCaretOffset();

        if (newCaretOffset != mCursorPosition) {
          mCursorPosition = newCaretOffset;
          refreshSelection();
        }
      }

      public void keyReleased(KeyEvent e) {
        // not implemented
      }

    });

    getSourceViewer().getTextWidget().addMouseListener(new MouseListener() {
      public void mouseDown(MouseEvent e) {
        int newCaretOffset = getSourceViewer().getTextWidget().getCaretOffset();

        if (newCaretOffset != mCursorPosition) {
          mCursorPosition = newCaretOffset;
          refreshSelection();
        }
      }

      public void mouseDoubleClick(MouseEvent e) {
        // not needed
      }

      public void mouseUp(MouseEvent e) {
        int newCaretOffset = getSourceViewer().getTextWidget().getCaretOffset();
        
        if (newCaretOffset != mCursorPosition) {
          mCursorPosition = newCaretOffset;
          refreshSelection();
        }        
      }

    });

    
    // UIMA-2242:
    // When a single character is selected with a double click the selection
    // changes but the caret does not.
    // A press on enter now fails to create an annotation.
    
    // UIMA-2247:
    // Changed again to ensure that also selection from the find dialog
    // can be detected
    getSourceViewer().getSelectionProvider().addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        mFeatureStructureSelectionProvider.setSelection(new AnnotationEditorSelection((ITextSelection) event.getSelection(), 
                new StructuredSelection(ModelFeatureStructure.create(getDocument(), getSelectedAnnotations()))));
      }
    });
    
    DragSource dragSource = new DragSource(getSourceViewer().getTextWidget(), DND.DROP_COPY);

    Transfer[] types = new Transfer[] { FeatureStructureTransfer.getInstance() };

    dragSource.setTransfer(types);

    dragSource.addDragListener(new FeatureStructureDragListener(getSourceViewer().getTextWidget()));

    getSourceViewer().getTextWidget().setEditable(false);
    getSourceViewer().setEditable(false);

    getSite().setSelectionProvider(mFeatureStructureSelectionProvider);
    
      
    // Retrieve font size from preference store, default is 15
    IPreferenceStore prefStore = CasEditorPlugin.getDefault().getPreferenceStore();
    int textSize = prefStore.getInt(AnnotationEditorPreferenceConstants.ANNOTATION_EDITOR_TEXT_SIZE);
    
    if (textSize > 0) {
      setTextSize(textSize);
    }
    
    preferenceStoreChangeListener = (new IPropertyChangeListener() {
      
      public void propertyChange(PropertyChangeEvent event) {
        if (AnnotationEditorPreferenceConstants.ANNOTATION_EDITOR_TEXT_SIZE.equals(event.getProperty())) {
          Integer textSize = (Integer) event.getNewValue();
          
          if (textSize != null && textSize > 0) {
            setTextSize(textSize);
          }
        }
      }
    });
    
    prefStore.addPropertyChangeListener(preferenceStoreChangeListener);
    
    // Note: In case the CAS could not be created the editor will be initialized with
    // a null document (getDocument() == null). Depending on the error it might be 
    // possible to recover and the editor input will be set again. The createPartControl
    // method might be called with a document or without one, both cases must be supported.
    
    // Perform the document dependent initialization 
    initiallySynchronizeUI();
  }

  private void refreshSelection() {
    mFeatureStructureSelectionProvider.setSelection(getDocument(), getSelectedAnnotations());
  }

  /**
   * Checks if the current instance is editable.
   *
   * @return false
   */
  @Override
  public boolean isEditable() {
    return false;
  }

  

  /**
   * Used to inform about input changes
   */
  @Override
  protected void handleElementContentReplaced() {
    super.handleElementContentReplaced();
    setInput(getEditorInput());
  }
  
  @Override
  protected void doSetInput(final IEditorInput input) throws CoreException {
    final IEditorInput oldInput = getEditorInput();
    final ICasDocument oldDocument = getDocument();
    
    // Unregister the editor listeners on the old input
    // TODO: Should we make methods to encapsulate the register/unregister code?
    if (oldDocument != null) {
      oldDocument.removeChangeListener(mAnnotationSynchronizer);
      mAnnotationSynchronizer = null;
      
      getCasDocumentProvider().getTypeSystemPreferenceStore(getEditorInput()).
              removePropertyChangeListener(mAnnotationStyleListener);
      mAnnotationStyleListener = null;
    }

    super.doSetInput(input);
    
    if (CasEditorPlugin.getDefault().
            getAndClearShowMigrationDialogFlag()) {
      getSite().getShell().getDisplay().asyncExec(new Runnable() {
          public void run() {
              MessageDialog.openInformation(getSite().getShell(),"Cas Editor Project Removed",
                      "The Cas Editor Project support was removed from this release and" +
                      " your existing Cas Editor Projects have been migrated. If you still want to use the " +
                      "Cas Editor Project support please install the Cas Editor 2.3.1 again.\n\n" + 
                      "The Analysis Engine and Cas Consumer run support was also removed and " +
                      "is replaced by the new Analysis Engine Launch plugin.");
          }
      });
    }
    
    if (getDocument() != null) {
      
      AnnotationOutline outline = new AnnotationOutline(this);
      mOutlinePage.setCASViewPage(outline);

      shownAnnotationTypes.clear();
      
      // Synchronize shown types with the editor
      String shownTypesString = getCasDocumentProvider().getSessionPreferenceStore(input).getString("LastShownTypes");
      
      String[] shownTypes = shownTypesString.split(";");
      
      for (String shownType : shownTypes) {
        
        Type type = getDocument().getType(shownType);
        
        // Types can be deleted from the type system but still be marked 
        // as shown in the .dotCorpus file, in that case the type
        // name cannot be mapped to a type and should be ignored.
        
        if (type != null)
          shownAnnotationTypes.add(type);
      }
      
      if (getSourceViewer() != null) {
        
        // This branch is usually only executed when the 
        // input was updated because it could not be opened the
        // first time trough an error e.g. no type system available
        //
        // Compared to the usual code branch createPartControl was
        // already called without a document, which means
        // that the state between the UI and the document
        // must be synchronized now

        initiallySynchronizeUI();

        // Views which fetch content from the editors document still believe
        // the editor document is not available.
        // Send a partBroughtToTop event to the views to update their content
        // on this currently active editor.
        IWorkbenchPage page = getSite().getWorkbenchWindow().getActivePage();
        
        for (IWorkbenchPart view : page.getViews()) {
          if (view instanceof PageBookView) {
            ((PageBookView) view).partBroughtToTop(getEditorSite().getPart());
          }
        }
      }
    }
    else {
      // Makes the outline book show the not available message page
      mOutlinePage.setCASViewPage(null);
    }
    
    final ICasDocument newDocument = getDocument();
    
    if (mEditorInputListener != null) {
      
      // Iterate over the existing listeners, listeners might be removed
      // or added during notification, since views are created or destroyed
      // when this event is fired.
      
      for (Object listener : mEditorInputListener.getListeners()) {
        
        final ICasEditorInputListener inputListener = 
                (ICasEditorInputListener) listener;
        
        SafeRunner.run(new SafeRunnable() {
          public void run() {
            inputListener.casDocumentChanged(oldInput, oldDocument, input, newDocument);
          }
        });
      }
    }
  }

  // The editor status support is abused to display different control than the
  // text control when displaying text is not possible, e.g. because the sofa
  // is not a text sofa or not set at all
  @Override
  protected boolean isErrorStatus(IStatus status) {
    return super.isErrorStatus(status) || getDocument().getCAS().getDocumentText() == null;
  }
  
  /**
   * Initialized the UI from the freshly set document.
   * 
   * Note: Does nothing if getDoucment() return null.
   */
  private void initiallySynchronizeUI() {
    if (getDocument() != null) {
      
      mShowAnnotationsMenu = new ShowAnnotationsMenu(
              getDocument().getCAS().getTypeSystem(), shownAnnotationTypes);
      mShowAnnotationsMenu.addListener(new IShowAnnotationsListener() {

        public void selectionChanged(Collection<Type> selection) {
          // compute change sets and apply changes:

          Collection<Type> notShownAnymore = new HashSet<Type>(shownAnnotationTypes);
          notShownAnymore.removeAll(selection);
          for (Type type : notShownAnymore) {
            showAnnotationType(type, false);
          }

          Collection<Type> newShownTypes = new HashSet<Type>(selection);
          newShownTypes.removeAll(shownAnnotationTypes);
          for (Iterator<Type> iterator = newShownTypes.iterator(); iterator.hasNext();) {
            Type type = iterator.next();
            showAnnotationType(type, true);
          }
          
          // Repaint after annotations are changed
          mPainter.paint(IPainter.CONFIGURATION);
          
          setEditorSessionPreferences();
          
          if (mEditorListener != null) {
            for (IAnnotationEditorModifyListener listener : mEditorListener) 
              listener.showAnnotationsChanged(selection);
          }
        }
      });
      
      IPreferenceStore sessionPreferences =
          getCasDocumentProvider().getSessionPreferenceStore(getEditorInput());
      
      // TODO: Define constants for these settings!
      
      Type annotationModeType = getDocument().getType(
          sessionPreferences.getString("LastUsedModeType"));
      
      // Note: The type in the configuration might not exist anymore, or there is
      // no type stored yet, in these cases the built-in uima.tcas.Annotation type is used.
      // Not setting the annotation type will cause NPEs after the editor opened.
      
      if (annotationModeType == null) {
    	  annotationModeType = getDocument().getType(CAS.TYPE_NAME_ANNOTATION);
      }
      
      setAnnotationMode(annotationModeType);
      
      String lastActiveViewName = sessionPreferences.getString("LastActiveCasViewName");
      
      try {
        // TODO: Missing compatibility check!!!
        getDocument().getCAS().getView(lastActiveViewName);
        showView(lastActiveViewName);
      }
      catch (CASRuntimeException e) {
        // ignore, view is not available
        // Note: Using exceptions for control flow is very bad practice
        // TODO: Is there a way to check which views are available?!
        //       Maybe we should iterate over all available views, and then
        //       check if it is available!
        showView(CAS.NAME_DEFAULT_SOFA);
      }
    }
  }
  
  @Override
  protected void editorContextMenuAboutToShow(IMenuManager menu) {
    super.editorContextMenuAboutToShow(menu);

    // Add Annotate action
    menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
    menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getAction(SmartAnnotateAction.ID));
    menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getAction(QuickAnnotateAction.ID));
    menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getAction(ITextEditorActionDefinitionIds.DELETE));
    
    TypeSystem typeSytem = getDocument().getCAS().getTypeSystem();

    // mode menu
    MenuManager modeMenuManager = new MenuManager("Mode");
    menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, modeMenuManager);

    ModeMenu modeMenu = new ModeMenu(typeSytem, this);
    modeMenu.addListener(new IModeMenuListener(){

    public void modeChanged(Type newMode) {
      IAction actionToExecute = new ChangeModeAction(newMode, newMode.getShortName(),
              AnnotationEditor.this);

      actionToExecute.run();
    }});

    modeMenuManager.add(modeMenu);

    // annotation menu
    MenuManager showAnnotationMenu = new MenuManager("Show Annotations");
    menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, showAnnotationMenu);
    showAnnotationMenu.add(mShowAnnotationsMenu);
    
    // view menu
    MenuManager casViewMenuManager = new MenuManager("CAS Views");
    menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, casViewMenuManager);
    CasViewMenu casViewMenu = new CasViewMenu(this);
    casViewMenuManager.add(casViewMenu);
  }

  /**
   * Updates the status line.
   */
  private void updateStatusLineModeItem() {
    // TODO: refactore this
    IStatusField statusField = getStatusField(AnnotationEditorActionContributor.ID);

    // can be null directly after doSetInput()
    if (statusField != null) {
      statusField.setText(getAnnotationMode().getShortName());
    }
  }

  /**
   * Returns the current <code>AnnotationDocument</code> of this editor.
   *
   * @return current <code>AnnotationDocument</code>
   */
  public ICasDocument getDocument() {
    return (ICasDocument) getDocumentProvider().getDocument(getEditorInput());
  }

  public void reopenEditorWithNewTypeSystem() {
    setInput(getEditorInput());
  }
  
  /**
   * Returns the current annotation type.
   *
   * @return - current annotation type
   */
  public Type getAnnotationMode() {
    return mAnnotationMode;
  }

  /**
   * Sets the new annotation type.
   *
   * @param type
   */
  public void setAnnotationMode(Type type) {
    // TODO: check if this type is a subtype of Annotation

    if (type == null) {
      throw new IllegalArgumentException("type must not be null!");
    }

    mAnnotationMode = type;

    highlight(0, 0);

    setEditorSessionPreferences();

    updateStatusLineModeItem();

    syncAnnotationTypes();

    fireAnnotationTypeChanged(getAnnotationMode());

    mShowAnnotationsMenu.setEditorAnnotationMode(type);
  }

  public Collection<Type> getShownAnnotationTypes() {
    return Collections.unmodifiableCollection(shownAnnotationTypes);
  }
  
  public void setShownAnnotationType(Type type, boolean isShown) {
    mShowAnnotationsMenu.setSelectedType(type, isShown);
  }

  public void setShownAnnotationTypes(Collection<Type> types) {
    mShowAnnotationsMenu.setSelectedTypes(types);
  }

  /**
   * @param type
   */
  private void fireAnnotationTypeChanged(Type type) {
    if (mEditorListener != null) {
      for (IAnnotationEditorModifyListener listener : mEditorListener)
        listener.annotationModeChanged(type);
    }
  }

  /**
   * Retrieves an <code>AnnotationStyle</code> from the underlying storage.
   *
   * @param type
   */
  public AnnotationStyle getAnnotationStyle(Type type) {
    if (type == null)
      throw new IllegalArgumentException("type parameter must not be null!");
    
    IPreferenceStore prefStore = getCasDocumentProvider().
            getTypeSystemPreferenceStore(getEditorInput());
    
    return AnnotationStyle.getAnnotationStyleFromStore(prefStore, type.getName());
  }
  

  /**
   * Sets an annotation style.
   * 
   * Note: Internal usage only!
   * 
   * @param style
   */
  // TODO: Disk must be accessed for every changed annotation style
  // add a second method which can take all changed styles
  public void setAnnotationStyle(AnnotationStyle style) {
    IPreferenceStore prefStore = getCasDocumentProvider().getTypeSystemPreferenceStore(getEditorInput());
    AnnotationStyle.putAnnotatationStyleToStore(prefStore, style);
    
    getCasDocumentProvider().saveTypeSystemPreferenceStore(getEditorInput());
  }
  
  /**
   * Set the shown annotation status of a type.
   * 
   * @param type
   * @param isVisible if true the type is shown, if false the type
   * it not shown
   */
  private void showAnnotationType(Type type, boolean isVisible) {
    AnnotationStyle style = getAnnotationStyle(type);
    
    if (isVisible) {
      
      IDrawingStrategy strategy = DrawingStyle.createStrategy(style);
      
      // It might not be possible to create the drawing strategy trough
      // configuration errors, in this case the drawing strategy will be ignored
      if (strategy != null) {
        
        if (style.getStyle().equals(AnnotationStyle.Style.TAG)) {
          getSourceViewer().getTextWidget().setLineSpacing(13);
        }
        
        mPainter.addDrawingStrategy(type.getName(), strategy);
        mPainter.addAnnotationType(type.getName(), type.getName());
        java.awt.Color color = style.getColor();
        mPainter.setAnnotationTypeColor(type.getName(), new Color(null, color.getRed(),
                color.getGreen(), color.getBlue()));
      }
      
      shownAnnotationTypes.add(type);
    }
    else {
      mPainter.removeAnnotationType(type.getName());
      shownAnnotationTypes.remove(type);
      
      if (style.getStyle().equals(AnnotationStyle.Style.TAG)) {
        // TODO: only if no more TAG styles are active
        // How to figure that out ?!
        boolean isKeepLineSpacing = false;
        
        for(Type shownType : shownAnnotationTypes) {
          AnnotationStyle potentialTagStyle = getAnnotationStyle(shownType);
          
          if (AnnotationStyle.Style.TAG.equals(potentialTagStyle.getStyle())) {
            isKeepLineSpacing = true;
            break;
          }
        }
        
        if (!isKeepLineSpacing)
          getSourceViewer().getTextWidget().setLineSpacing(0);
      }
    }
  }

  /**
   * Synchronizes all annotations with the eclipse annotation painter.
   */
  public void syncAnnotationTypes() {

    mPainter.removeAllAnnotationTypes();
    getSourceViewer().getTextWidget().setLineSpacing(0);
    
    for (Type displayType : mShowAnnotationsMenu.getSelectedTypes()) {
      showAnnotationType(displayType, true);
    }

    if (!mShowAnnotationsMenu.getSelectedTypes().contains(getAnnotationMode())) {
      showAnnotationType(getAnnotationMode(), true);
    }

    mPainter.paint(IPainter.CONFIGURATION);
  }

  private void removeAllAnnotations() {
    // Remove all annotation from the model
    IAnnotationModel annotationModel = getDocumentProvider().getAnnotationModel(getEditorInput());
    ((IAnnotationModelExtension) annotationModel).removeAllAnnotations();
  }
  
  private void syncAnnotations() {
    
    removeAllAnnotations();
    
    // Remove all annotation from the model
    IAnnotationModel annotationModel = getDocumentProvider().getAnnotationModel(getEditorInput());
    ((IAnnotationModelExtension) annotationModel).removeAllAnnotations();
    
    // Add all annotation to the model
    // copy annotations into annotation model
    final Iterator<AnnotationFS> mAnnotations = getDocument().getCAS().getAnnotationIndex()
            .iterator();
    
    // TODO: Build first a map, and then pass all annotations at once
    Map annotationsToAdd = new HashMap();
    
    while (mAnnotations.hasNext()) {
      AnnotationFS annotationFS = mAnnotations.next();
      annotationsToAdd.put(new EclipseAnnotationPeer(annotationFS), new Position(
              annotationFS.getBegin(), annotationFS.getEnd() - annotationFS.getBegin()));
    }
    
    ((IAnnotationModelExtension) annotationModel).replaceAnnotations(null, annotationsToAdd);
  }
  
  /**
   * @param listener
   */
  public void addAnnotationListener(IAnnotationEditorModifyListener listener) {
    mEditorListener.add(listener);
  }
  
  public void removeAnnotationListener(IAnnotationEditorModifyListener listener) {
    mEditorListener.remove(listener);
  }

  public void addCasEditorInputListener(ICasEditorInputListener listener) {
    mEditorInputListener.add(listener);
  }
  
  public void removeCasEditorInputListener(ICasEditorInputListener listener) {
    mEditorInputListener.remove(listener);
  }
  
  
  /**
   * Returns the selection.
   *
   * @return - the selection
   */
  public Point getSelection() {
    return getSourceViewer().getTextWidget().getSelection();
  }

  /**
   * Highlights the given range in the editor.
   *
   * @param start
   * @param length
   */
  private void highlight(int start, int length) {
    ISourceViewer sourceViewer = getSourceViewer();

    assert sourceViewer != null;

    StyledText text = sourceViewer.getTextWidget();

    if (mCurrentStyleRange != null) {
      // reset current style range
      StyleRange resetedStyleRange = new StyleRange(mCurrentStyleRange.start,
              mCurrentStyleRange.length, null, null);

      text.setStyleRange(resetedStyleRange);
      mCurrentStyleRange = null;
    }

    if (length != 0) {
      mCurrentStyleRange = new StyleRange(start, length, text.getSelectionForeground(), text
              .getSelectionBackground());

      text.setStyleRange(mCurrentStyleRange);
    }
  }

  /**
   * Retrieves the currently selected annotation.
   *
   * TODO: make this private ??? clients can use selections for this ...
   *
   * @return the selected annotations or an empty list
   */
  public List<AnnotationFS> getSelectedAnnotations() {
    List<AnnotationFS> selection = new ArrayList<AnnotationFS>();

    if (isSomethingSelected()) {
      Point selectedText = getSourceViewer().getTextWidget().getSelectionRange();

      Span selecectedSpan = new Span(selectedText.x, selectedText.y);

      Collection<AnnotationFS> selectedAnnotations = getAnnotation(getDocument().getCAS(),
              getAnnotationMode(), selecectedSpan);

      for (AnnotationFS annotation : selectedAnnotations) {
        selection.add(annotation);
      }

      Collections.sort(selection, new AnnotationComparator());
    } else {
      Map<Integer, AnnotationFS> view = getView(getAnnotationMode());

      AnnotationFS annotation = view.get(getSourceViewer().getTextWidget().getCaretOffset());

      if (annotation == null) {
        annotation = view.get(mCursorPosition - 1);
      }

      if (annotation != null) {
        selection.add(annotation);
      }
    }

    return selection;
  }
  
  /**
   * Returns the caret position relative to the start of the text.
   * 
   * @return the caret position relative to the start of the text
   */
  public int getCaretOffset() {
    return getSourceViewer().getTextWidget().getCaretOffset();
  }
  
  public void showView(String viewName) {
    
    // TODO: Check if set view is compatible .. if not display some message!
    
    // TODO: Consider to clear selection if this is called in the
    // selectionChanged method, is that the right place?!
    
    // Clear all old selections, references FSes of current view
    mFeatureStructureSelectionProvider.clearSelection();
    
    // Move the caret before the first char, otherwise it
    // might be placed on an index which is out of bounds in
    // the changed text
    if (getSourceViewer().getTextWidget().getText().length() > 0)
      getSourceViewer().getTextWidget().setCaretOffset(0);
    
    // De-highlight the text in the editor, because the highlight
    // method has to remember the highlighted text area 
    // After the text changed the offsets might be out of bound
    highlight(0, 0);
    
    // Remove all editor annotations
    // Changing the text with annotations might fail, because the
    // bounds might be invalid in the new text
    removeAllAnnotations();
    
    // Change the view in the input document
    // TODO: Add support for this to the interface
    ((AnnotationDocument) getDocument()).switchView(viewName);
    
    // Retrieve the new (changed) text document and refresh the source viewer
    getSourceViewer().setDocument((AnnotationDocument) getDocument(),
            getDocumentProvider().getAnnotationModel(getEditorInput())); 
    getSourceViewer().invalidateTextPresentation();
    
    // All annotations will be synchronized in the document listener
    
    // Last opened view should be remembered, in case a new editor is opened
    setEditorSessionPreferences();
    
    // Check if CAS view is compatible, only if compatible the listeners
    // to update the annotations in the editor can be registered
    // and the annotations can be synchronized
    if (!isErrorStatus(getCasDocumentProvider().getStatus(getEditorInput()))) {
      
      // Synchronize all annotation from the document with
      // the editor
      syncAnnotations();
      
      // Note: If a change from a compatible view to a compatible view
      //       occurs there is no need be register the listeners again
      
      // Register listener to synchronize annotations between the
      // editor and the document in case the annotations
      // change e.g. updated in a view
      if (mAnnotationSynchronizer == null) {
        mAnnotationSynchronizer = new DocumentListener();
        getDocument().addChangeListener(mAnnotationSynchronizer);
      }
      
      // Register listener to synchronize annotation styles
      // between multiple open annotation editors
      if (mAnnotationStyleListener == null) {
        mAnnotationStyleListener = new AnnotationStyleChangeListener() {
          
          public void annotationStylesChanged(Collection<AnnotationStyle> styles) {
            // TODO: Only sync changed types
            syncAnnotationTypes();
          }
        };
        
        getCasDocumentProvider().getTypeSystemPreferenceStore(getEditorInput()).
            addPropertyChangeListener(mAnnotationStyleListener);
      }
      
      getSite().getPage().addSelectionListener(this);
    }
    else {
      // if not null ... then unregister ... listener
      if (mAnnotationSynchronizer != null) {
        getDocument().removeChangeListener(mAnnotationSynchronizer);
        mAnnotationSynchronizer = null;
      }
      
      if (mAnnotationStyleListener != null) {
        getCasDocumentProvider().getTypeSystemPreferenceStore(getEditorInput()).
            removePropertyChangeListener(mAnnotationStyleListener);
        mAnnotationStyleListener = null;
      }
      
      getSite().getPage().removeSelectionListener(this);
    }
    
    // According to error status toggle between text editor or
    // status page
    updatePartControl(getEditorInput());
  }
  
  /**
   * Text is not editable, cause of the nature of the annotation editor. This does not mean, that
   * the annotations are not editable.
   *
   * @return false
   */
  @Override
  public boolean isEditorInputModifiable() {
    return false;
  }

  /**
   * Notifies the current instance about selection changes in the workbench.
   *
   * @param part
   * @param selection
   */
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    if (selection instanceof StructuredSelection) {
      
      AnnotationSelection annotations = new AnnotationSelection((StructuredSelection) selection);

      // only process these selection if the annotations belong
      // to the current editor instance
      if (getSite().getPage().getActiveEditor() == this) {
        
        if (!annotations.isEmpty()) {
          highlight(annotations.getFirst().getBegin(), annotations.getLast().getEnd()
                  - annotations.getFirst().getBegin());
  
          // move caret to new position when selected outside of the editor
          if (AnnotationEditor.this != part) {
            
            // Note: The caret cannot be placed between line delimiters
            // See bug UIMA-1470
            int newCaretOffset = annotations.getLast().getEnd();
            String text = getSourceViewer().getTextWidget().getText();
            
            if (newCaretOffset > 0 && newCaretOffset < text.length()) {
              char beforeCaret = text.charAt(newCaretOffset -1);
              char afterCaret = text.charAt(newCaretOffset);
              
              final int cr = 0x0D;
              final int lf = 0x0A;
              if (beforeCaret == cr && afterCaret == lf) {
                // In case the caret offset is in the middle
                // of a multiple-char line delimiter place caret
                // before
                newCaretOffset = newCaretOffset -1;
              }
            }
            
            // check bounds, if out of text do nothing
            getSourceViewer().getTextWidget().setCaretOffset(newCaretOffset);
            getSourceViewer().revealRange(newCaretOffset, 0);
            
            mFeatureStructureSelectionProvider.setSelection(selection);
          }
        }
        else {
          // Nothing selected, clear annotation selection
          highlight(0, 0);
        }
      }
    }
  }

  private boolean isSomethingSelected() {
    // TODO: sometimes we get a NPE here ...
    // getSourceViewer() returns null here ... but why ?
    return getSourceViewer().getTextWidget().getSelectionCount() != 0;
  }
  
  /**
   * Set the session data which should be used to initalize the next Cas Editor which
   * is opened.
   */
  private void setEditorSessionPreferences() {
    
    // TODO: Define constants with prefix for these settings ... so they don't conflict with other plugins!
    
    IPreferenceStore sessionStore = getCasDocumentProvider().
            getSessionPreferenceStore(getEditorInput());
    
    sessionStore.setValue("LastActiveCasViewName", getDocument().getCAS().getViewName());
    sessionStore.setValue("LastUsedModeType", getAnnotationMode().getName());
    
    StringBuilder shownTypesString = new StringBuilder();
    
    for (Type shownType : getShownAnnotationTypes()) {
      shownTypesString.append(shownType.getName());
      shownTypesString.append(";");
    }
    
    sessionStore.setValue("LastShownTypes", shownTypesString.toString());
  }

  /**
   * Creates custom annotation actions:
   *
   * Annotate Action
   * Smart Annotate Action
   * Delete Annotations Action
   * Find Annotate Action
   */
  @Override
  protected void createActions() {

    super.createActions();

    mFeatureStructureSelectionProvider = new FeatureStructureSelectionProvider();
    getSite().setSelectionProvider(mFeatureStructureSelectionProvider);

    // create annotate action
    QuickAnnotateAction quickAnnotateAction = new QuickAnnotateAction(getSourceViewer().getTextWidget());
    quickAnnotateAction.setActionDefinitionId(QuickAnnotateAction.ID);
    quickAnnotateAction.setText("Quick Annotate");
    setAction(QuickAnnotateAction.ID, quickAnnotateAction);
    getSite().getSelectionProvider().addSelectionChangedListener(quickAnnotateAction);
    
    SmartAnnotateAction smartAnnotateAction = new SmartAnnotateAction();
    smartAnnotateAction.setActionDefinitionId(SmartAnnotateAction.ID);
    smartAnnotateAction.setText("Annotate");
    setAction(SmartAnnotateAction.ID, smartAnnotateAction);
    getSite().getSelectionProvider().addSelectionChangedListener(smartAnnotateAction);

    // create delete action
    DeleteFeatureStructureAction deleteAnnotationAction = new DeleteFeatureStructureAction(
            this);
    deleteAnnotationAction.setText("Delete Annotation");
    getSite().getSelectionProvider().addSelectionChangedListener(deleteAnnotationAction);

    deleteAnnotationAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.DELETE);

    setAction(IWorkbenchActionDefinitionIds.DELETE, deleteAnnotationAction);
    setActionActivationCode(IWorkbenchActionDefinitionIds.DELETE, (char) 0, SWT.CR, SWT.NONE);

    // create show annotation context editing action
    ShowAnnotationContextEditAction annotationContextEditAction =
            new ShowAnnotationContextEditAction();

    annotationContextEditAction.setActionDefinitionId(ITextEditorActionDefinitionIds.QUICK_ASSIST);
    setAction(ITextEditorActionDefinitionIds.QUICK_ASSIST, annotationContextEditAction);

    // Create find annotate action
    FindAnnotateAction findAnnotateAction = new FindAnnotateAction(this, getSourceViewer().getFindReplaceTarget());
    findAnnotateAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_REPLACE);
    setAction(ITextEditorActionConstants.FIND, findAnnotateAction);
    
    // Lower left side of annotation action
    LowerLeftAnnotationSideAction lowerLeftAnnotationSideAction = new LowerLeftAnnotationSideAction(this);
    lowerLeftAnnotationSideAction.setActionDefinitionId(LowerLeftAnnotationSideAction.ID);
    setAction(LowerLeftAnnotationSideAction.ID, lowerLeftAnnotationSideAction);
    getSite().getSelectionProvider().addSelectionChangedListener(lowerLeftAnnotationSideAction);
    
    // Wide left side of annotation action
    WideLeftAnnotationSideAction wideLeftAnnotationSide = new WideLeftAnnotationSideAction(this);
    wideLeftAnnotationSide.setActionDefinitionId(WideLeftAnnotationSideAction.ID);
    setAction(WideLeftAnnotationSideAction.ID, wideLeftAnnotationSide);
    getSite().getSelectionProvider().addSelectionChangedListener(wideLeftAnnotationSide);
    
    // Lower right side of annotation
    LowerRightAnnotationSideAction lowerRightAnnotationSideAction = new LowerRightAnnotationSideAction(this);
    lowerRightAnnotationSideAction.setActionDefinitionId(LowerRightAnnotationSideAction.ID);
    setAction(LowerRightAnnotationSideAction.ID, lowerRightAnnotationSideAction);
    getSite().getSelectionProvider().addSelectionChangedListener(lowerRightAnnotationSideAction);
    
    // Wide right side of annotation
    WideRightAnnotationSideAction wideRightAnnotationSideAction = new WideRightAnnotationSideAction(this);
    wideRightAnnotationSideAction.setActionDefinitionId(WideRightAnnotationSideAction.ID);
    setAction(WideRightAnnotationSideAction.ID, wideRightAnnotationSideAction);
    getSite().getSelectionProvider().addSelectionChangedListener(wideRightAnnotationSideAction);
  }

  @Override
  public void dispose() {
    // remove selection listener
    getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);

    getSite().getPage().removeSelectionListener(this);

    ICasDocument document = getDocument();

    if (document != null) {
      document.removeChangeListener(mAnnotationSynchronizer);
    }

    CasDocumentProvider provider = getCasDocumentProvider();
    
    if (provider != null) {
      IPreferenceStore store = provider.getTypeSystemPreferenceStore(getEditorInput());
      if (store != null)
        store.removePropertyChangeListener(mAnnotationStyleListener);
    }
    
    if (preferenceStoreChangeListener != null)
      CasEditorPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(preferenceStoreChangeListener);
    
    super.dispose();
  }

  void setAnnotationSelection(AnnotationFS annotation) {
    mFeatureStructureSelectionProvider.setSelection(getDocument(), annotation);
  }

  /**
   * Creates a list of all {@link AnnotationEditor} which are currently opened.
   */
  public static AnnotationEditor[] getAnnotationEditors() {

    ArrayList<AnnotationEditor> dirtyParts = new ArrayList<AnnotationEditor>();
    IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow element : windows) {
      IWorkbenchPage pages[] = element.getPages();
      for (IWorkbenchPage page : pages) {
        IEditorReference[] references = page.getEditorReferences();

        for (IEditorReference reference : references) {

          IEditorPart part = reference.getEditor(false);

          if (part instanceof AnnotationEditor) {
            AnnotationEditor editor = (AnnotationEditor) part;
            dirtyParts.add(editor);
          }
        }
      }
    }

    return dirtyParts.toArray(new AnnotationEditor[dirtyParts.size()]);
  }
  
  @Override
  protected Control createStatusControl(Composite parent, IStatus status) {

    // Type System is missing in non Cas Editor Project case
    if (status.getCode() == CasDocumentProvider.TYPE_SYSTEM_NOT_AVAILABLE_STATUS_CODE
            && getEditorInput() !=null) {
      // TODO: Is the null check on the editor input necessary ?!
      
      // Show a form to select a type system in the document provider,
      // afterwards the form calls reopenEditorWithNewTypesytem to reopen
      // the editor on the input
      return getCasDocumentProvider().createTypeSystemSelectorForm(this, parent, status);
    }
    else if (status.getCode() == IStatus.OK) {
      
      // TODO: Figure out which page should be shown
      // TODO: Implement pages ...
      // TODO: Each page needs an ability to switch the view back to something else ...
      
      if (getDocument() != null && getDocument().getCAS().getDocumentText() == null) {
        
        // TODO: Also display the current view name ...
        
        Composite noTextComposite = new Composite(parent, SWT.NONE);
        noTextComposite.setLayout(new GridLayout(1, false));
        Label noTextLabel = new Label(noTextComposite, SWT.NONE);
        noTextLabel.setText("Text sofa is not set!");
        
        
        Label switchToView = new Label(noTextComposite, SWT.NONE);
        switchToView.setText("Choose a view to switch to:");
        
        final Combo viewSelectionCombo = new Combo(noTextComposite, SWT.READ_ONLY);
        
        List<String> viewNames = new ArrayList<String>();
        
        for (Iterator<CAS> it = getDocument().getCAS().getViewIterator(); it.hasNext(); ) {
          viewNames.add(it.next().getViewName());
        }
        
        viewSelectionCombo.setItems(viewNames.toArray(new String[viewNames.size()]));
        
        // Preselect default view, will always be there
        viewSelectionCombo.select(0);
        
        Button switchView = new Button(noTextComposite, SWT.PUSH);
        switchView.setText("Switch");
        
        // TODO: Add a combo to select view ...
        
        switchView.addSelectionListener(new SelectionListener() {
          
          public void widgetSelected(SelectionEvent e) {
            // TODO; Switch to selected view in combo ...
            showView(viewSelectionCombo.getText());
          }
          
          public void widgetDefaultSelected(SelectionEvent e) {
          }
        });
        
        return noTextComposite;
      }
      
      return super.createStatusControl(parent, status);
    }
    else {
      return super.createStatusControl(parent, status);
    }
  }
  
  /**
   * Retrieves the annotations in the given span.
   */
  static Collection<AnnotationFS> getAnnotation(CAS cas, Type type, Span span) {
    ConstraintFactory cf = cas.getConstraintFactory();

    Type annotationType = cas.getAnnotationType();

    FeaturePath beginPath = cas.createFeaturePath();
    beginPath.addFeature(annotationType.getFeatureByBaseName("begin"));
    FSIntConstraint beginConstraint = cf.createIntConstraint();
    beginConstraint.geq(span.getStart());

    FSMatchConstraint embeddedBegin = cf.embedConstraint(beginPath, beginConstraint);

    FeaturePath endPath = cas.createFeaturePath();
    endPath.addFeature(annotationType.getFeatureByBaseName("end"));
    FSIntConstraint endConstraint = cf.createIntConstraint();
    endConstraint.leq(span.getEnd());

    FSMatchConstraint embeddedEnd = cf.embedConstraint(endPath, endConstraint);

    FSMatchConstraint strictType = new StrictTypeConstraint(type);

    FSMatchConstraint annotatioInSpanConstraint = cf.and(embeddedBegin, embeddedEnd);

    FSMatchConstraint annotationInSpanAndStrictTypeConstraint =
            cf.and(annotatioInSpanConstraint, strictType);

    FSIndex<AnnotationFS> allAnnotations = cas.getAnnotationIndex(type);

    FSIterator<AnnotationFS> annotationInsideSpanIndex =
            cas.createFilteredIterator(allAnnotations.iterator(),
            annotationInSpanAndStrictTypeConstraint);

    return DocumentUimaImpl.fsIteratorToCollection(annotationInsideSpanIndex);
  }
  
  /**
   * Retrieves the view map.
   */
  private Map<Integer, AnnotationFS> getView(Type annotationType) {
    Collection<AnnotationFS> annotations = getDocument().getAnnotations(annotationType);

    HashMap<Integer, AnnotationFS> viewMap = new HashMap<Integer, AnnotationFS>();

    for (AnnotationFS annotation : annotations) {
      for (int i = annotation.getBegin(); i <= annotation.getEnd() - 1; i++) {
        viewMap.put(i, annotation);
      }
    }

    return Collections.unmodifiableMap(viewMap);
  }
}
