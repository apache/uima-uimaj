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

package org.apache.uima.taeconfigurator.editors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_impl;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.impl.AnalysisEngineMetaData_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.internal.util.Class_TCCL;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.jcas.jcasgenp.MergerImpl;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceServiceSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.taeconfigurator.CDEpropertyPage;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.point.IUimaEditorExtension;
import org.apache.uima.taeconfigurator.editors.point.IUimaMultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.AggregatePage;
import org.apache.uima.taeconfigurator.editors.ui.CapabilityPage;
import org.apache.uima.taeconfigurator.editors.ui.HeaderPage;
import org.apache.uima.taeconfigurator.editors.ui.IndexesPage;
import org.apache.uima.taeconfigurator.editors.ui.OverviewPage;
import org.apache.uima.taeconfigurator.editors.ui.ParameterPage;
import org.apache.uima.taeconfigurator.editors.ui.ResourcesPage;
import org.apache.uima.taeconfigurator.editors.ui.SettingsPage;
import org.apache.uima.taeconfigurator.editors.ui.TypePage;
import org.apache.uima.taeconfigurator.editors.ui.Utility;
import org.apache.uima.taeconfigurator.editors.xml.XMLEditor;
import org.apache.uima.taeconfigurator.files.ContextForPartDialog;
import org.apache.uima.taeconfigurator.model.AllTypes;
import org.apache.uima.taeconfigurator.model.DefinedTypesWithSupers;
import org.apache.uima.taeconfigurator.model.DescriptorTCAS;
import org.apache.uima.tools.jcasgen.IError;
import org.apache.uima.tools.jcasgen.Jg;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.apache.uima.util.XMLizable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.Bundle;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Main class implementing the multi page editor. In Eclipse 3, we extend FormEditor, which extends
 * in turn MultiPageEditorPart.
 * 
 * Life cycle: Create: An instance of this class is created each time the editor is started on a new
 * resource. Base multipage editor createPartControl calls createPages; FormEditor's impl of
 * createPages calls createToolkit, then calls addPages. FormEditor has field holding the toolkit
 * This class overrides createToolkit to re-use the colors in the plugin. * Dispose: dispose is
 * called. FormEditor impl of dispose disposes of the toolkit.
 * 
 * Superclass (FormEditor) holds vector of pages toolkit (disposed of in FormEditor dispose method)
 * SuperSuperclass (MultiPageEditorPart) holds array of editors (we only have 1 "editor" - the xml
 * source editor - the rest are views into that model / data).
 * 
 * Stale = model (on disk, saved) is ahead of widgets Dirty = widgets are ahead of model
 * &lt;&lt;&lt; NOT USED HERE
 * 
 * Each page of the multipage editor has its own class. ownclass -%gt; HeaderPage -%gt; FormPage
 * (impl IFormPage) has instance of PageForm -%gt; ManagedForm ManagedForm (impl IManagedForm): has
 * instance of ScrolledForm has subparts (IFormPart - which live on the scrolled form) A part can be
 * a section. A part can implement IPartSelectionListener to get selectionChanged(IFormPart,
 * ISelection) calls. initialize() call propagated to all parts. dispose() call propagated to all
 * parts. refresh() propagated to all parts (if part.isStale()) (Not Used) commit() propagated to
 * all parts (if part.isDirty()) setInput() propagated to all parts setFocus() propagated to 1st
 * part (not used) isDirty() propagated to all parts, is true if any are true isStale() propagated
 * to all parts, is true if any are true reflow() delegated to the contained ScrolledForm (not used)
 * fireSelectionChanged(IFormPart, ISelection) - can be used to notify other parts that implement
 * IPartSelectionListener about selection changes
 * 
 * Each page has one or more sections. sectionSpecific -&gt; (AbstractTableSection) -&gt;
 * AbstractSection -&gt; SectionPart -&gt; AbstractFormPart (impl IFormPart, see above)
 * 
 * AbstractFormPart holds back ref to managed form, a dirty and stale bit. Stale = model is ahead of
 * widgets (Not used) Dirty = widgets are ahead of model Stale brought into sync by 'refresh'
 * method. Part notifies containing ManagedForm when Stale/Dirty changes in the part; Part
 * responsible for removing listeners from event providers. IFormPart can receive form input
 * SectionPart adds listeners for expansionStateChang(ed)(ing) expansionStateChanged calls reflow on
 * wrapped form Note: the forms framework Dirty mechanism and the "commit" methods are not used. In
 * its place, the handlers directly update the model, rather than marking Dirty and letting someone
 * call commit.
 */
public class MultiPageEditor extends FormEditor implements IUimaMultiPageEditor {

  // ******************************
  /** The initial size type collections. */
  // * Tuning Parameters
  public final int INITIAL_SIZE_TYPE_COLLECTIONS = 20;

  /** The initial size feature collections. */
  public final int INITIAL_SIZE_FEATURE_COLLECTIONS = 40;

  /** The preserve comments. */
  public final boolean PRESERVE_COMMENTS = true;

  // ******************************

  // ***********************************************************
  // M O D E L
  // the following are only populated based on what type
  /** The ae description. */
  // of descriptor is being edited
  private AnalysisEngineDescription aeDescription = null;

  /** The type system description. */
  private TypeSystemDescription typeSystemDescription = null;

  /** The merged type system description. */
  private TypeSystemDescription mergedTypeSystemDescription = null;

  /** The merged types adding features. */
  private Map<String, Set<String>> mergedTypesAddingFeatures = new TreeMap<>();

  /** The imported type system description. */
  private TypeSystemDescription importedTypeSystemDescription = null;

  /** The xml infoset. */
  private Node xmlInfoset = null; // captures comments and ignorableWhitespace
  /**
   * Key = unique ID of included AE in aggregate Value = AnalysisEngineSpecification or URISpecifier
   * if remote This value is obtained from aeDescription.getDelegateAnalysisEngineSpecifiers() for
   * aggregates, and is cached so we don't need to repeatedly resolve it, with checks for invalid
   * xml exceptions.
   */
  private Map resolvedDelegates = new HashMap();

  // fully resolved (imports) and merged index collection
  // resolve with mergeDelegateAnalysisEngineFsIndexCollections
  /** The merged fs index collection. */
  // (This works also for primitives)
  private FsIndexCollection mergedFsIndexCollection;

  /** The imported fs index collection. */
  private FsIndexCollection importedFsIndexCollection;

  // fully resolved (imports) and merged type priorities
  // resolve with mergeDelegateAnalysisEngineTypePriorities
  // (This works also for primitives)
  // This collects all the type priority lists into one list, after
  /** The merged type priorities. */
  // resolving imports.
  private TypePriorities mergedTypePriorities;

  /** The imported type priorities. */
  private TypePriorities importedTypePriorities;

  // fully resolved (imports) ResourceManagerConfiguration
  // This collects all the External Resources and bindings into 2 list,
  // resolving imports. The resulting list may have
  // overridden bindings
  /** The resolved external resources and bindings. */
  // unused external resources (not bound)
  private ResourceManagerConfiguration resolvedExternalResourcesAndBindings;

  // private ResourceManagerConfiguration importedExternalResourcesAndBindings;

  /** The resolved flow controller declaration. */
  private FlowControllerDeclaration resolvedFlowControllerDeclaration;

  /** The collection reader description. */
  private CollectionReaderDescription collectionReaderDescription;

  /** The cas initializer description. */
  private CasInitializerDescription casInitializerDescription;

  /** The cas consumer description. */
  private CasConsumerDescription casConsumerDescription;

  /** The flow controller description. */
  private FlowControllerDescription flowControllerDescription;

  // values computed when first needed
  /** The descriptor CAS. */
  // all use common markStale()
  public DescriptorTCAS descriptorCAS;

  /** The all types. */
  public AllTypes allTypes;

  /** The defined types with supers. */
  public DefinedTypesWithSupers definedTypesWithSupers;

  // ****************************************
  // * Model parts not part of the descriptor
  /** The file. */
  // ****************************************
  private IFile file; // file being edited

  /** The file needing context. */
  private IFile fileNeedingContext;

  // ***********************************************************
  // End of M O D E L
  // ***********************************************************

  /*
   * Each page is an instance of a particular class. These instances are created each time a new
   * instance of the editor opens.
   */

  /** The source index. */
  protected int sourceIndex = -1;

  /** The overview index. */
  protected int overviewIndex = -1;

  /** The aggregate index. */
  private int aggregateIndex = -1;

  /** The parameter index. */
  private int parameterIndex = -1;

  /** The settings index. */
  private int settingsIndex = -1;

  /** The type index. */
  protected int typeIndex = -1;

  /** The capability index. */
  protected int capabilityIndex = -1;

  /** The indexes index. */
  protected int indexesIndex = -1;

  /** The resources index. */
  protected int resourcesIndex = -1;

  /** The overview page. */
  protected OverviewPage overviewPage = null;

  /** The aggregate page. */
  private AggregatePage aggregatePage = null;

  /** The parameter page. */
  private ParameterPage parameterPage = null;

  /** The settings page. */
  private SettingsPage settingsPage = null;

  /** The type page. */
  protected TypePage typePage = null;

  /** The capability page. */
  protected CapabilityPage capabilityPage = null;

  /** The indexes page. */
  protected IndexesPage indexesPage = null;

  /** The resources page. */
  protected ResourcesPage resourcesPage = null;

  /** The source text editor. */
  protected XMLEditor sourceTextEditor;

  /** The m b is inited. */
  private boolean m_bIsInited = false;

  /** The is bad XML. */
  protected boolean isBadXML = true;

  /** The source changed. */
  public boolean sourceChanged = true;

  /** The file dirty. */
  private boolean fileDirty; // can only be set dirty once inited

  /** The dirty type name hash. */
  private HashSet dirtyTypeNameHash; // for generating .java

  // type files upon saving (this has a problem if user edited xml
  // directly...)

  /** The m n save as status. */
  public int m_nSaveAsStatus = SAVE_AS_NOT_IN_PROGRESS;

  /** The Constant SAVE_AS_NOT_IN_PROGRESS. */
  public static final int SAVE_AS_NOT_IN_PROGRESS = -1;

  /** The Constant SAVE_AS_STARTED. */
  public static final int SAVE_AS_STARTED = -2;

  /** The Constant SAVE_AS_CANCELLED. */
  public static final int SAVE_AS_CANCELLED = -3;

  /** The Constant SAVE_AS_CONFIRMED. */
  public static final int SAVE_AS_CONFIRMED = -4;

  /** The opening context. */
  private boolean openingContext = false;

  /** The is context loaded. */
  private boolean isContextLoaded = false;

  /**
   * Gets the checks if is context loaded.
   *
   * @return the checks if is context loaded
   */
  public boolean getIsContextLoaded() {
    return isContextLoaded;
  }

  /** The limit J cas gen to project scope. */
  private boolean limitJCasGenToProjectScope = MultiPageEditorContributor
          .getLimitJCasGenToProjectScope();

  /**
   * Gets the limit J cas gen to project scope.
   *
   * @return the limit J cas gen to project scope
   */
  public boolean getLimitJCasGenToProjectScope() {
    return limitJCasGenToProjectScope;
  }

  /**
   * Sets the limit J cas gen to project scope.
   *
   * @param v
   *          the new limit J cas gen to project scope
   */
  public void setLimitJCasGenToProjectScope(boolean v) {
    limitJCasGenToProjectScope = v;
  }

  /** Descriptor Types. */

  private int descriptorType = 0;

  /**
   * Gets the descriptor type.
   *
   * @return the descriptor type
   */
  public int getDescriptorType() {
    return descriptorType;
  }

  /** The Constant DESCRIPTOR_AE. */
  public static final int DESCRIPTOR_AE = 1;

  /** The Constant DESCRIPTOR_TYPESYSTEM. */
  public static final int DESCRIPTOR_TYPESYSTEM = 1 << 1;

  /** The Constant DESCRIPTOR_INDEX. */
  public static final int DESCRIPTOR_INDEX = 1 << 2;

  /** The Constant DESCRIPTOR_TYPEPRIORITY. */
  public static final int DESCRIPTOR_TYPEPRIORITY = 1 << 3;

  /** The Constant DESCRIPTOR_EXTRESANDBINDINGS. */
  public static final int DESCRIPTOR_EXTRESANDBINDINGS = 1 << 4;

  /** The Constant DESCRIPTOR_COLLECTIONREADER. */
  public static final int DESCRIPTOR_COLLECTIONREADER = 1 << 5;

  /** The Constant DESCRIPTOR_CASINITIALIZER. */
  public static final int DESCRIPTOR_CASINITIALIZER = 1 << 6;

  /** The Constant DESCRIPTOR_CASCONSUMER. */
  public static final int DESCRIPTOR_CASCONSUMER = 1 << 7;

  /** The Constant DESCRIPTOR_FLOWCONTROLLER. */
  public static final int DESCRIPTOR_FLOWCONTROLLER = 1 << 8;

  /**
   * Descriptor type string.
   *
   * @param pDescriptorType
   *          the descriptor type
   * @return the string
   */
  public String descriptorTypeString(int pDescriptorType) {
    String r;
    switch (pDescriptorType) {
      case DESCRIPTOR_AE:
        r = Messages.getString("MultiPageEditor.0"); //$NON-NLS-1$
        break;
      case DESCRIPTOR_TYPESYSTEM:
        r = Messages.getString("MultiPageEditor.1"); //$NON-NLS-1$
        break;
      case DESCRIPTOR_INDEX:
        r = Messages.getString("MultiPageEditor.2"); //$NON-NLS-1$
        break;
      case DESCRIPTOR_TYPEPRIORITY:
        r = Messages.getString("MultiPageEditor.3"); //$NON-NLS-1$
        break;
      case DESCRIPTOR_EXTRESANDBINDINGS:
        r = Messages.getString("MultiPageEditor.4"); //$NON-NLS-1$
        break;
      case DESCRIPTOR_COLLECTIONREADER:
        r = Messages.getString("MultiPageEditor.5"); //$NON-NLS-1$
        break;
      case DESCRIPTOR_CASINITIALIZER:
        r = Messages.getString("MultiPageEditor.6"); //$NON-NLS-1$
        break;
      case DESCRIPTOR_CASCONSUMER:
        r = Messages.getString("MultiPageEditor.7"); //$NON-NLS-1$
        break;
      case DESCRIPTOR_FLOWCONTROLLER:
        r = "Flow Controller";
        break;
      default:
        throw new InternalErrorCDE(Messages.getString("MultiPageEditor.8")); //$NON-NLS-1$
    }
    return r + Messages.getString("MultiPageEditor.9"); //$NON-NLS-1$
  }

  /**
   * Descriptor type string.
   *
   * @return the string
   */
  public String descriptorTypeString() {
    return descriptorTypeString(descriptorType);
  }

  /**
   * Checks if is ae descriptor.
   *
   * @return true, if is ae descriptor
   */
  public boolean isAeDescriptor() {
    return 0 != (descriptorType & DESCRIPTOR_AE);
  }

  /**
   * Checks if is type system descriptor.
   *
   * @return true, if is type system descriptor
   */
  public boolean isTypeSystemDescriptor() {
    return 0 != (descriptorType & DESCRIPTOR_TYPESYSTEM);
  }

  /**
   * Checks if is fs index collection.
   *
   * @return true, if is fs index collection
   */
  public boolean isFsIndexCollection() {
    return 0 != (descriptorType & DESCRIPTOR_INDEX);
  }

  /**
   * Checks if is type priority descriptor.
   *
   * @return true, if is type priority descriptor
   */
  public boolean isTypePriorityDescriptor() {
    return 0 != (descriptorType & DESCRIPTOR_TYPEPRIORITY);
  }

  /**
   * Checks if is ext res and bindings descriptor.
   *
   * @return true, if is ext res and bindings descriptor
   */
  public boolean isExtResAndBindingsDescriptor() {
    return 0 != (descriptorType & DESCRIPTOR_EXTRESANDBINDINGS);
  }

  /**
   * Checks if is collection reader descriptor.
   *
   * @return true, if is collection reader descriptor
   */
  public boolean isCollectionReaderDescriptor() {
    return 0 != (descriptorType & DESCRIPTOR_COLLECTIONREADER);
  }

  /**
   * Checks if is cas initializer descriptor.
   *
   * @return true, if is cas initializer descriptor
   */
  public boolean isCasInitializerDescriptor() {
    return 0 != (descriptorType & DESCRIPTOR_CASINITIALIZER);
  }

  /**
   * Checks if is cas consumer descriptor.
   *
   * @return true, if is cas consumer descriptor
   */
  public boolean isCasConsumerDescriptor() {
    return 0 != (descriptorType & DESCRIPTOR_CASCONSUMER);
  }

  /**
   * Checks if is flow controller descriptor.
   *
   * @return true, if is flow controller descriptor
   */
  public boolean isFlowControllerDescriptor() {
    return 0 != (descriptorType & DESCRIPTOR_FLOWCONTROLLER);
  }

  /**
   * Checks if is local processing descriptor.
   *
   * @return true, if is local processing descriptor
   */
  public boolean isLocalProcessingDescriptor() {
    return 0 != (descriptorType & (DESCRIPTOR_AE | DESCRIPTOR_COLLECTIONREADER
            | DESCRIPTOR_CASINITIALIZER | DESCRIPTOR_CASCONSUMER | DESCRIPTOR_FLOWCONTROLLER));
  }

  /**
   * Checks if is primitive.
   *
   * @return true, if is primitive
   */
  public boolean isPrimitive() {
    return isLocalProcessingDescriptor() && aeDescription.isPrimitive();
  }

  /**
   * Checks if is aggregate.
   *
   * @return true, if is aggregate
   */
  public boolean isAggregate() {
    return isAeDescriptor() && (!aeDescription.isPrimitive());
  }

  /** The m type priorities backup. */
  private TypePriorities m_typePrioritiesBackup;

  /** The fade color. */
  private Color fadeColor;

  /** The is reverting index. */
  private boolean isRevertingIndex;

  /** The is page change recursion. */
  protected boolean isPageChangeRecursion = false;

  /** The Constant typeDescriptionArray0. */
  public static final TypeDescription[] typeDescriptionArray0 = new TypeDescription[0];

  /** The type systems to merge. */
  private List typeSystemsToMerge;

  /** The type priorities to merge. */
  private List typePrioritiesToMerge;

  /** The fs indexes to merge. */
  private List fsIndexesToMerge;

  /** The failed remotes. */
  private Map failedRemotes = new TreeMap();

  /** The failed remotes already known. */
  private Set failedRemotesAlreadyKnown = new TreeSet();

  /** The external editor configurations. */
  private static List<IConfigurationElement> externalEditorConfigurations = null;

  /** The current editor. */
  private IUimaMultiPageEditor currentEditor; // can be CDE or another editor

  /**
   * Instantiates a new multi page editor.
   */
  public MultiPageEditor() {
    currentEditor = this; // default
    initCDE(); // specific for CDE
  }

  /**
   * 
   * Note: Try to move these codes out of constructor MultiPageEditor(). Too much of impacts. Put it
   * back into constructor MultiPageEditor()
   */
  private void initCDE() {
    // Model initialization
    fileDirty = false;
    dirtyTypeNameHash = new HashSet();
    descriptorCAS = new DescriptorTCAS(this);
    allTypes = new AllTypes(this);
    definedTypesWithSupers = new DefinedTypesWithSupers(this);

    // reasonable initial values
    aeDescription = UIMAFramework.getResourceSpecifierFactory().createAnalysisEngineDescription();
    typeSystemDescription = null;
    importedTypeSystemDescription = null;
    mergedTypeSystemDescription = null;
    mergedFsIndexCollection = aeDescription.getAnalysisEngineMetaData().getFsIndexCollection();
    resolvedExternalResourcesAndBindings = aeDescription.getResourceManagerConfiguration();
    resolvedFlowControllerDeclaration = aeDescription.getFlowControllerDeclaration();
    mergedTypePriorities = aeDescription.getAnalysisEngineMetaData().getTypePriorities();
  }

  /** The Constant EXTENSION_TAG_CLASS_ATTRIB. */
  private static final String EXTENSION_TAG_CLASS_ATTRIB = "class";

  /**
   * Gets the required editor.
   *
   * @param parsedResult
   *          the parsed result
   * @return the required editor
   */
  private IUimaEditorExtension getRequiredEditor(XMLizable parsedResult) {
    return getRequiredEditor(null, parsedResult.getClass().getName());
  }

  /**
   * Gets the required editor.
   *
   * @param topElementName
   *          the top element name
   * @return the required editor
   */
  private IUimaEditorExtension getRequiredEditor(String topElementName) {
    return getRequiredEditor(topElementName, null);
  }

  // returns null if no matching editor found
  /**
   * Gets the required editor.
   *
   * @param topElementName
   *          the top element name
   * @param parsedResultClassName
   *          the parsed result class name
   * @return the required editor
   */
  // otherwise instantiates a new editor
  private IUimaEditorExtension getRequiredEditor(String topElementName,
          String parsedResultClassName) {
    IUimaEditorExtension editor;
    // load external editor configurations if not already loaded
    if (null == externalEditorConfigurations) {
      getExternalEditorConfigurations();
    }

    for (IConfigurationElement xeditor : externalEditorConfigurations) {
      for (IConfigurationElement canEdit : xeditor.getChildren()) {
        String elementName = canEdit.getAttribute("elementName");
        String parseResultName = canEdit.getAttribute("internalParseClass");
        if (((null != topElementName) && topElementName.equals(elementName))
                || ((null != parsedResultClassName)
                        && parsedResultClassName.equals(parseResultName))) {
          try {
            editor = (IUimaEditorExtension) xeditor
                    .createExecutableExtension(EXTENSION_TAG_CLASS_ATTRIB);
          } catch (CoreException e) {
            Utility.popMessage("Unexpected Exception",
                    "While trying to load an editor extension" + getMessagesToRootCause(e),
                    MessageDialog.ERROR);
            return null;
          }
          editor.init();
          return editor;
        }
      }
    }
    return null;
  }

  /** The Constant EXTENSION_POINT_ID. */
  private static final String EXTENSION_POINT_ID = "externalEditor";

  // load all of the external editor xml data
  /**
   * Gets the external editor configurations.
   *
   * @return the external editor configurations
   */
  // (but don't load the actual editors, yet)
  private void getExternalEditorConfigurations() {
    // Get extension point from Registry
    IExtensionPoint point = Platform.getExtensionRegistry()
            .getExtensionPoint(TAEConfiguratorPlugin.pluginId, EXTENSION_POINT_ID);

    externalEditorConfigurations = new ArrayList<>();

    // check: Any <extension> tags for our extension-point?
    if (point != null) {
      for (IExtension extension : point.getExtensions()) {
        Bundle b = Platform.getBundle(extension.getContributor().getName());
        if (b == null) {
          Utility.popMessage("Problem with Editor Extension", "Editor '"
                  + extension.getContributor().getName()
                  + "' is present, but can't be loaded, probably because of unsatisfied dependencies\n",
                  MessageDialog.ERROR);
          continue;
        }
        for (IConfigurationElement ces : extension.getConfigurationElements()) {
          externalEditorConfigurations.add(ces);
        }
      }
    } else {
      // Error - no such extension point
      Utility.popMessage("Internal Error", "CDE's extension point is missing", MessageDialog.ERROR);
    }
  }

  // *************************************************************************
  // Expose "protected" methods and methods from Super
  // *************************************************************************

  /**
   * @param site
   *          the site
   * @param editorInput
   *          the editor input
   * @throws PartInitException
   *           the part init exception
   */
  public void initSuper(IEditorSite site, IEditorInput editorInput) throws PartInitException {
    super.init(site, editorInput);
  }

  /**
   * Gets the current page super.
   *
   * @return the current page super
   */
  public int getCurrentPageSuper() {
    return getCurrentPage();
  }

  /**
   * Sets the part name super.
   *
   * @param partName
   *          the new part name super
   */
  public void setPartNameSuper(String partName) {
    super.setPartName(partName);
  }

  /**
   * Sets the page text super.
   *
   * @param pageIndex
   *          the page index
   * @param text
   *          the text
   */
  public void setPageTextSuper(int pageIndex, String text) {
    super.setPageText(pageIndex, text);
  }

  /**
   * Page change super.
   *
   * @param newPageIndex
   *          the new page index
   */
  public void pageChangeSuper(int newPageIndex) {
    super.pageChange(newPageIndex);
  }

  /**
   * Sets the active page super.
   *
   * @param pageIndex
   *          the new active page super
   */
  public void setActivePageSuper(int pageIndex) {
    super.setActivePage(pageIndex);
  }

  /**
   * Fire property change super.
   *
   * @param propertyId
   *          the property id
   */
  public void firePropertyChangeSuper(final int propertyId) {
    super.firePropertyChange(propertyId);
  }

  /**
   * Sets the input super.
   *
   * @param input
   *          the new input super
   */
  public void setInputSuper(IEditorInput input) {
    super.setInput(input);
  }

  // XML source editor is opened by CDE when the source is "initially" invalid.
  /**
   * Gets the source editor.
   *
   * @return the source editor
   */
  // Called by DDE when the source becomes valid and it is DD.
  public XMLEditor getSourceEditor() {
    return sourceTextEditor;
  }

  /**
   * ************************************************************************.
   *
   * @param display
   *          the display
   * @return the form toolkit
   */

  /**
   * override the createToolkit method in FormEditor - to use a shared colors resource.
   * 
   * This method is called by the FormEditor's createPages() method which will in turn call the
   * addPages method below. The toolkit ref is stored in the FormEditor object, and can be retrieved
   * by getToolkit().
   * 
   */

  @Override
  protected FormToolkit createToolkit(Display display) {
    return new FormToolkit(TAEConfiguratorPlugin.getDefault().getFormColors(display));
  }

  /**
   * Adds the page and set tab title.
   *
   * @param page
   *          the page
   * @param keyTabTitle
   *          the key tab title
   * @return the int
   * @throws PartInitException
   *           the part init exception
   */
  /*
   * Two forms of addPage - one for non-source-editors, and one for source-editor
   */
  protected int addPageAndSetTabTitle(HeaderPage page, String keyTabTitle)
          throws PartInitException {
    int pageIndex = addPage(page);
    // set the text on the tab used to select the page in the multipage editor
    setPageText(pageIndex, keyTabTitle);
    return pageIndex;
  }

  /**
   * Adds the page and set tab title.
   *
   * @param page
   *          the page
   * @param input
   *          the input
   * @param keyTabTitle
   *          the key tab title
   * @return the int
   * @throws PartInitException
   *           the part init exception
   */
  protected int addPageAndSetTabTitle(IEditorPart page, IEditorInput input, String keyTabTitle)
          throws PartInitException {
    int pageIndex = addPage(page, input);
    // set the text on the tab used to select the page in the multipage editor
    setPageText(pageIndex, keyTabTitle);
    return pageIndex;
  }

  /*
   * In general, 3 kinds of pages can be added. 1) an editor (IEditorPart, IEditorInput) 2) (lazy)
   * an IFormPage (extends IEditorPart) - has a managedForm, can wrap an editor 3) (lazy) a SWT
   * Control (Not Used)
   * 
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
   */
  @Override
  protected void addPages() {
    currentEditor.addPagesForCurrentEditor();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.point.IUimaMultiPageEditor#addPagesForCurrentEditor()
   */
  @Override
  public void addPagesForCurrentEditor() {
    boolean allPages = isLocalProcessingDescriptor();
    try {
      overviewIndex = addPageAndSetTabTitle(overviewPage = new OverviewPage(this),
              Messages.getString("MultiPageEditor.overviewTab")); //$NON-NLS-1$

      if (allPages) {
        if (isAeDescriptor()) {
          aggregateIndex = addPageAndSetTabTitle(aggregatePage = new AggregatePage(this),
                  Messages.getString("MultiPageEditor.aggregateTab")); //$NON-NLS-1$
        }
        parameterIndex = addPageAndSetTabTitle(parameterPage = new ParameterPage(this),
                Messages.getString("MultiPageEditor.parameterTab")); //$NON-NLS-1$
        settingsIndex = addPageAndSetTabTitle(settingsPage = new SettingsPage(this),
                Messages.getString("MultiPageEditor.settingsTab")); //$NON-NLS-1$
      }

      if (allPages || isTypeSystemDescriptor()) {
        typeIndex = addPageAndSetTabTitle(typePage = new TypePage(this),
                Messages.getString("MultiPageEditor.typeTab")); //$NON-NLS-1$
      }

      if (allPages) {
        capabilityIndex = addPageAndSetTabTitle(capabilityPage = new CapabilityPage(this),
                Messages.getString("MultiPageEditor.capabilityTab")); //$NON-NLS-1$
      }

      if (allPages || isTypePriorityDescriptor() || isFsIndexCollection()) {
        indexesIndex = addPageAndSetTabTitle(indexesPage = new IndexesPage(this),
                Messages.getString("MultiPageEditor.indexesTab")); //$NON-NLS-1$
      }

      if (allPages || isExtResAndBindingsDescriptor()) {
        resourcesIndex = addPageAndSetTabTitle(resourcesPage = new ResourcesPage(this),
                Messages.getString("MultiPageEditor.resourcesTab")); //$NON-NLS-1$
      }

      sourceIndex = addPageAndSetTabTitle(sourceTextEditor = new XMLEditor(this), getEditorInput(),
              Messages.getString("MultiPageEditor.sourceTab")); //$NON-NLS-1$

    } catch (PartInitException e) {
      e.printStackTrace(); // TODO fix this
    }
    if (isBadXML) {
      pageChange(sourceIndex);
    }
  }

  /**
   * Jcas gen.
   *
   * @param monitor
   *          the monitor
   */
  public void jcasGen(IProgressMonitor monitor) {
    if (MultiPageEditorContributor.getAutoJCasGen()) {
      doJCasGenChkSrc(monitor);
    }
  }

  /**
   * Do J cas gen chk src.
   *
   * @param monitor
   *          the monitor
   */
  public void doJCasGenChkSrc(IProgressMonitor monitor) {
    if (isSourceFolderValid()) {
      doJCasGen(monitor);
    }
  }

  /**
   * Checks if is source folder valid.
   *
   * @return true, if is source folder valid
   */
  public boolean isSourceFolderValid() {
    IResource folder = getPrimarySourceFolder();
    if (folder == null) {
      String msg = Messages.getString("MultiPageEditor.noSrcNoJCas"); //$NON-NLS-1$
      Utility.popMessage(Messages.getString("MultiPageEditor.noSrcDir"), msg, MessageDialog.ERROR); //$NON-NLS-1$
      return false;
    }
    return true;
  }

  /**
   * Sync source before saving to file.
   *
   * @return true, if successful
   */
  private boolean syncSourceBeforeSavingToFile() {
    boolean modelOK = true;
    if (getCurrentPage() != sourceIndex) {
      validateIndexes();
      updateSourceFromModel();
    } else { // have to check if there are dirty types
      modelOK = validateSource();
    }
    if (modelOK && isLocalProcessingDescriptor()) {
      return isValidAE(aeDescription);
    }
    return modelOK;
  }

  /**
   * Checks if is valid AE.
   *
   * @param aAe
   *          the a ae
   * @return true, if is valid AE
   */
  public boolean isValidAE(AnalysisEngineDescription aAe) {
    AbstractSection.setVnsHostAndPort(aAe);
    // copy Ae into real descriptors if needed
    getTrueDescriptor();
    // use clones because validation modifies (imports get imported)
    if (isCollectionReaderDescriptor()) {
      CollectionReaderDescription collRdr = (CollectionReaderDescription) collectionReaderDescription
              .clone();
      try {
        collRdr.doFullValidation(createResourceManager());
      } catch (Throwable e) { // all these are Throwable to catch errors like
        // UnsupportedClassVersionError, which happens if the annotator
        // class is compiled for Java 5.0, but the CDE is running Java 1.4.2
        Utility.popMessage(Messages.getString("MultiPageEditor.failedCollRdrValidation"), //$NON-NLS-1$
                Messages.getString("MultiPageEditor.failedCollRdrValidationMsg") + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                        + getMessagesToRootCause(e), MessageDialog.ERROR);
        return false;
      }
    } else if (isCasInitializerDescriptor()) {
      CasInitializerDescription casInit = (CasInitializerDescription) casInitializerDescription
              .clone();
      try {
        casInit.doFullValidation(createResourceManager());
      } catch (Throwable e) {
        Utility.popMessage(Messages.getString("MultiPageEditor.failedCasInitValidation"), //$NON-NLS-1$
                Messages.getString("MultiPageEditor.failedCasInitValidationMsg") + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                        + getMessagesToRootCause(e), MessageDialog.ERROR);
        return false;
      }
    } else if (isCasConsumerDescriptor()) {
      CasConsumerDescription casCons = (CasConsumerDescription) casConsumerDescription.clone();
      try {
        casCons.doFullValidation(createResourceManager());
      } catch (Throwable e) {
        Utility.popMessage(Messages.getString("MultiPageEditor.failedCasConsValidation"), //$NON-NLS-1$
                Messages.getString("MultiPageEditor.failedCasConsValidationMsg") + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                        + getMessagesToRootCause(e), MessageDialog.ERROR);
        return false;
      }

    } else if (isFlowControllerDescriptor()) {
      FlowControllerDescription fc = (FlowControllerDescription) flowControllerDescription.clone();
      try {
        fc.doFullValidation(createResourceManager());
      } catch (Throwable e) {
        Utility.popMessage("Error in Flow Controller Descriptor",
                "The Descriptor is invalid for the following reason:" + "\n"
                        + getMessagesToRootCause(e),
                MessageDialog.ERROR);
        return false;
      }
    } else {
      AnalysisEngineDescription ae = (AnalysisEngineDescription) aAe.clone();

      // speedup = replace typeSystem with resolved imports version
      if (ae.isPrimitive()) {
        TypeSystemDescription tsd = getMergedTypeSystemDescription();
        if (null != tsd) {
          tsd = (TypeSystemDescription) tsd.clone();
        }
        ae.getAnalysisEngineMetaData().setTypeSystem(tsd);
      }
      ae.getAnalysisEngineMetaData().setFsIndexCollection(getMergedFsIndexCollection());
      ae.getAnalysisEngineMetaData().setTypePriorities(getMergedTypePriorities());
      try {
        ae.doFullValidation(createResourceManager());
      } catch (Throwable e) {
        Utility.popMessage(Messages.getString("MultiPageEditor.failedAeValidation"), //$NON-NLS-1$
                Messages.getString("MultiPageEditor.failedAeValidationMsg") + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                        + getMessagesToRootCause(e), MessageDialog.ERROR);
        return false;
      }
    }
    return true;
  }

  /**
   * Saves the multi-page editor's document.
   *
   * @param monitor
   *          the monitor
   */
  @Override
  public void doSave(IProgressMonitor monitor) {
    currentEditor.doSaveForCurrentEditor(monitor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.point.IUimaMultiPageEditor#doSaveForCurrentEditor(org.
   * eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void doSaveForCurrentEditor(IProgressMonitor monitor) {
    boolean modelOK = syncSourceBeforeSavingToFile();
    sourceTextEditor.doSave(monitor);
    finishSave(monitor, modelOK);
  }

  /**
   * Finish save.
   *
   * @param monitor
   *          the monitor
   * @param modelOK
   *          the model OK
   */
  private void finishSave(IProgressMonitor monitor, boolean modelOK) {
    if (modelOK) {
      if (dirtyTypeNameHash.size() > 0) {
        jcasGen(monitor);
      }
      dirtyTypeNameHash.clear();
    }
    fileDirty = false;
    firePropertyChange(ISaveablePart.PROP_DIRTY);

  }

  /**
   * Saves the multi-page editor's document as another file. Updates this multi-page editor's input
   * to correspond to the nested editor's.
   * 
   * This is not implemented correctly: filename isn't switched to new filename, etc.
   */
  @Override
  public void doSaveAs() {
    currentEditor.doSaveAsForCurrentEditor();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.point.IUimaMultiPageEditor#doSaveAsForCurrentEditor()
   */
  @Override
  public void doSaveAsForCurrentEditor() {
    boolean modelOK = syncSourceBeforeSavingToFile();
    setSaveAsStatus(SAVE_AS_STARTED);
    sourceTextEditor.doSaveAs();

    if (m_nSaveAsStatus == SAVE_AS_CANCELLED) {
      m_nSaveAsStatus = SAVE_AS_NOT_IN_PROGRESS;
      return;
    }
    // should only do if editorInput is new
    FileEditorInput newEditorInput = (FileEditorInput) sourceTextEditor.getEditorInput();

    // if(old)
    setInput(newEditorInput);
    firePropertyChange(PROP_INPUT);
    // setTitle(newEditorInput.getFile().getName());
    setPartName(newEditorInput.getFile().getName());
    // this next does NOT seem to change the overall page title

    firePropertyChange(PROP_TITLE);
    finishSave(null, modelOK);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.editor.FormEditor#isDirty()
   */
  @Override
  public boolean isDirty() {
    return fileDirty;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.part.EditorPart#isSaveOnCloseNeeded()
   */
  @Override
  public boolean isSaveOnCloseNeeded() {
    return fileDirty;
  }

  /**
   * Sets the file dirty.
   */
  public void setFileDirty() {
    if (m_bIsInited) {
      fileDirty = true;
      // next is key
      this.firePropertyChange(ISaveablePart.PROP_DIRTY);
    }
  }

  /**
   * Sets the file dirty flag.
   *
   * @param value
   *          the new file dirty flag
   */
  // Called by External Editor extensions when doSave or doSaveAs is called
  public void setFileDirtyFlag(boolean value) {
    fileDirty = value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.IEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
    XMLInputSource input;

    if (!(editorInput instanceof IFileEditorInput)) {
      throw new PartInitException(Messages.getString("MultiPageEditor.invalidInputClass")); //$NON-NLS-1$
    }
    fileNeedingContext = file = ((IFileEditorInput) editorInput).getFile();
    String filePathName = file.getLocation().toOSString();

    try {
      input = new XMLInputSource(filePathName);
    } catch (IOException e) {
      String m = Messages.getFormattedString("MultiPageEditor.IOError", //$NON-NLS-1$
              new String[] { AbstractSection.maybeShortenFileName(filePathName) })
              + Messages.getString("MultiPageEditor.10") + getMessagesToRootCause(e); //$NON-NLS-1$
      // skip showing a message because the partInitException
      // shows it
      throw new PartInitException(m);
    }

    super.init(site, editorInput); // to allow other editors to get site and editorInput

    // leaves isBadXML set, if it can't parse but isn't throwing
    isContextLoaded = false;
    try {
      parseSource(input, filePathName, PRESERVE_COMMENTS);
    } catch (MultilevelCancel e) {
      throw new PartInitException("Operation Cancelled");
    }

    isContextLoaded = true;

    // super.init(site, editorInput);
    setPartName(editorInput.getName());
    setContentDescription(editorInput.getName());
    // setContentDescription(1 line summary); TODO

    m_bIsInited = true;
  }

  /** The extension editor. */
  private IUimaEditorExtension extensionEditor;

  /**
   * Parses the source.
   *
   * @param input
   *          the input
   * @param filePathName
   *          the file path name
   * @param preserveComments
   *          the preserve comments
   * @throws PartInitException
   *           the part init exception
   */
  private void parseSource(XMLInputSource input, String filePathName, boolean preserveComments)
          throws PartInitException {
    extensionEditor = null;
    parseSourceInner(input, filePathName, preserveComments);
  }

  /**
   * Parses the source inner.
   *
   * @param input
   *          the input
   * @param filePathName
   *          the file path name
   * @param preserveComments
   *          the preserve comments
   * @throws PartInitException
   *           the part init exception
   */
  private void parseSourceInner(XMLInputSource input, String filePathName, boolean preserveComments)
          throws PartInitException {
    XMLizable inputDescription = null;
    try {
      inputDescription = AbstractSection.parseDescriptor(input, preserveComments);
      if (inputDescription instanceof AnalysisEngineDescription) {
        validateDescriptorType(DESCRIPTOR_AE);
        setAeDescription((AnalysisEngineDescription) inputDescription);
      } else if (inputDescription instanceof TypeSystemDescription) {
        validateDescriptorType(DESCRIPTOR_TYPESYSTEM);
        setTypeSystemDescription((TypeSystemDescription) inputDescription);
      } else if (inputDescription instanceof TypePriorities) {
        validateDescriptorType(DESCRIPTOR_TYPEPRIORITY);
        setTypePriorities((TypePriorities) inputDescription);
      } else if (inputDescription instanceof FsIndexCollection) {
        validateDescriptorType(DESCRIPTOR_INDEX);
        setFsIndexCollection((FsIndexCollection) inputDescription);
      } else if (inputDescription instanceof ResourceManagerConfiguration) {
        validateDescriptorType(DESCRIPTOR_EXTRESANDBINDINGS);
        setExtResAndBindings((ResourceManagerConfiguration) inputDescription);
      } else if (inputDescription instanceof CollectionReaderDescription) {
        validateDescriptorType(DESCRIPTOR_COLLECTIONREADER);
        setCollectionReaderDescription((CollectionReaderDescription) inputDescription);
      } else if (inputDescription instanceof CasInitializerDescription) {
        validateDescriptorType(DESCRIPTOR_CASINITIALIZER);
        setCasInitializerDescription((CasInitializerDescription) inputDescription);
      } else if (inputDescription instanceof CasConsumerDescription) {
        validateDescriptorType(DESCRIPTOR_CASCONSUMER);
        setCasConsumerDescription((CasConsumerDescription) inputDescription);
      } else if (inputDescription instanceof FlowControllerDescription) {
        validateDescriptorType(DESCRIPTOR_FLOWCONTROLLER);
        setFlowControllerDescription((FlowControllerDescription) inputDescription);
      } else {
        if (null == extensionEditor) {
          extensionEditor = getRequiredEditor(inputDescription);
        }
        if (null == extensionEditor) {
          throw new PartInitException(
                  Messages.getFormattedString("MultiPageEditor.unrecognizedDescType", //$NON-NLS-1$
                          new String[] { AbstractSection.maybeShortenFileName(filePathName) })
                          + Messages.getString("MultiPageEditor.11")); //$NON-NLS-1$
        } else {
          extensionEditor.activateEditor(getEditorSite(), getEditorInput(), this, inputDescription);
          currentEditor = (IUimaMultiPageEditor) extensionEditor;
        }
      }
      isBadXML = false;
    } catch (InvalidXMLException e) {
      if (InvalidXMLException.INVALID_DESCRIPTOR_FILE.equals(e.getMessageKey())) {
        Throwable cause = e.getCause();
        if ((cause instanceof InvalidXMLException)
                && InvalidXMLException.UNKNOWN_ELEMENT
                        .equals(((InvalidXMLException) cause).getMessageKey())
                && (null == extensionEditor)) {
          // try loading extension editors and reparsing
          extensionEditor = getRequiredEditor(
                  (String) ((InvalidXMLException) cause).getArguments()[0]);
          if (null != extensionEditor) {
            // the act of finding the right editor calls that editors init() method
            // which could install another parse target result, which would make this
            // exception go away - so try reparsing.

            try {
              parseSourceInner(new XMLInputSource(input.getURL()), filePathName, preserveComments);
            } catch (IOException e1) {
              Utility.popMessage("Internal Error",
                      "While parsing input for extension editor: " + getMessagesToRootCause(e1),
                      MessageDialog.ERROR);
              throw new InternalErrorCDE(e1);
            }
            return;
          }
        }
      }
      e.printStackTrace();
      Utility.popMessage(Messages.getString("MultiPageEditor.XMLerrorInDescriptorTitle"), //$NON-NLS-1$
              Messages.getString("MultiPageEditor.XMLerrorInDescriptor") + "\n" //$NON-NLS-1$ //$NON-NLS-2$
                      + getMessagesToRootCause(e), MessageDialog.ERROR);

    } catch (ResourceInitializationException e) {
      // occurs if bad xml
      // leave isBadXML flag set to true
      Utility.popMessage(Messages.getString("MultiPageEditor.errorInDescTitle"), //$NON-NLS-1$
              Messages.getString("MultiPageEditor.errorInDesc") + "\n" + getMessagesToRootCause(e), //$NON-NLS-1$ //$NON-NLS-2$
              MessageDialog.ERROR);
    }
  }

  /**
   * Validate descriptor type.
   *
   * @param newDescriptorType
   *          the new descriptor type
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void validateDescriptorType(int newDescriptorType)
          throws ResourceInitializationException {
    if (0 != descriptorType && !openingContext && ((descriptorType & newDescriptorType) == 0)) {
      throw new ResourceInitializationException(Messages.getString("MultiPageEditor.12"), //$NON-NLS-1$
              Messages.getString("MultiPageEditor.13"), //$NON-NLS-1$
              new String[] { descriptorTypeString(), descriptorTypeString(newDescriptorType) });
    }
    if (!openingContext) {
      descriptorType = newDescriptorType;
    }
  }

  /**
   * Create a resource manager that has a class loader that will search the compiled output of the
   * current project, in addition to the plug-in's classpath.
   * 
   * We create a new resource manager every time it's needed to pick up any changes the user may
   * have made to any classes that could have been loaded.
   * 
   * @return a resource manager that has a class loader that will search the compiled output of the
   *         current project, in addition to the plug-in's classpath
   */
  public ResourceManager createResourceManager() {
    // long time = System.currentTimeMillis();
    ResourceManager rm = createResourceManager(null);
    // System.out.println("CreateResourceManager: " + (System.currentTimeMillis() - time));
    return rm;
  }

  /** The cached R mclass path. */
  private String cachedRMclassPath = null;

  /** The cached R mcl. */
  private SoftReference<UIMAClassLoader> cachedRMcl = new SoftReference<>(null);

  /**
   * Creates the resource manager.
   *
   * @param classPath
   *          the class path
   * @return the resource manager
   */
  public ResourceManager createResourceManager(String classPath) {
    ResourceManager resourceManager = UIMAFramework.newDefaultResourceManager();

    try {
      if (null == classPath) {
        classPath = getProjectClassPath();
      }
      String dataPath = CDEpropertyPage.getDataPath(getProject());

      // first try to get the value of the class loader from the last (cached)
      // value - should succeed frequently because the class loader is only dependent
      // on the value of the classpath

      UIMAClassLoader uimaCL = null;
      if (cachedRMclassPath != null && cachedRMclassPath.equals(classPath)) {
        uimaCL = cachedRMcl.get();
      }

      if (uimaCL != null) {
        ((ResourceManager_impl) resourceManager).setExtensionClassPath(uimaCL, true);
      } else {
        // first arg in next is the parent of the class loader. Make it be the
        // uima framework's class loader (not this class's class loader)
        // so the validation tests work properly (that test isAssignableFrom)
        resourceManager.setExtensionClassPath(Class_TCCL.get_parent_cl(), // UIMAFramework.class.getClassLoader(),
                classPath, true);
        cachedRMclassPath = classPath;
        cachedRMcl = new SoftReference<>(
                (UIMAClassLoader) resourceManager.getExtensionClassLoader());
      }

      // in any case, set the data path
      resourceManager.setDataPath(dataPath);
    } catch (MalformedURLException e1) {
      throw new InternalErrorCDE(Messages.getString("MultiPageEditor.14"), e1); //$NON-NLS-1$
    } catch (CoreException e1) {
      throw new InternalErrorCDE(Messages.getString("MultiPageEditor.15"), e1); //$NON-NLS-1$
    }
    return resourceManager;
  }

  /*
   * (non-Javadoc) Method declared on IEditorPart.
   */
  @Override
  public boolean isSaveAsAllowed() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.forms.editor.FormEditor#pageChange(int)
   */
  @Override
  protected void pageChange(int newPageIndex) {
    currentEditor.pageChangeForCurrentEditor(newPageIndex);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.taeconfigurator.editors.point.IUimaMultiPageEditor#pageChangeForCurrentEditor(
   * int)
   */
  @Override
  public void pageChangeForCurrentEditor(int newPageIndex) {
    if (isPageChangeRecursion)
      return;
    isRevertingIndex = false;
    int oldPageIndex = getCurrentPage();

    if (oldPageIndex != -1) {
      if (oldPageIndex == sourceIndex) {
        if (!validateSource()) {
          setActivePageWhileBlockingRecursion(sourceIndex);
          return;
        }
      } else if (oldPageIndex == indexesIndex &&
      // could be the same page if users chose to
      // edit current descriptor when validateIndexes detected a
      // bad type priorities set
              newPageIndex != indexesIndex) {
        if (!validateIndexes()) {
          return;
        } else if (newPageIndex != indexesIndex) {
          saveGoodVersionOfTypePriorities();
        }
      }
    }

    super.pageChange(newPageIndex);

    Object newPage = pages.get(newPageIndex);
    if (newPage instanceof HeaderPage) {
      // ((HeaderPage)newPage).getManagedForm().refresh(); //super.pageChange does this
      if (newPage instanceof IndexesPage && oldPageIndex != indexesIndex) {
        saveGoodVersionOfTypePriorities();
      }
    } else if (newPageIndex == sourceIndex) {
      if (!isBadXML) {
        updateSourceFromModel();
      } else {
        setActivePageWhileBlockingRecursion(sourceIndex);
      }
      // set sourceChanged if badXML to redo error notification if nothing changed
      // in case XML was bad
      sourceChanged = (isBadXML || isRevertingIndex) ? true : false;
    }
  }

  /**
   * Sets the active page while blocking recursion.
   *
   * @param sourceIndex
   *          the new active page while blocking recursion
   */
  protected void setActivePageWhileBlockingRecursion(int sourceIndex) {
    try {
      isPageChangeRecursion = true;
      // next call needed to be done but wasn't prior to
      // Eclipse 3.2
      // In Eclipse 3.2 they fixed this, but call this now
      // calls pageChange, and makes a recursive loop
      // We break that loop here.
      setActivePage(sourceIndex); // isn't being done otherwise?
    } finally {
      isPageChangeRecursion = false;
    }
  }

  /**
   * Save good version of type priorities.
   */
  private void saveGoodVersionOfTypePriorities() {
    TypePriorities tp = getAeDescription().getAnalysisEngineMetaData().getTypePriorities();
    m_typePrioritiesBackup = (null == tp) ? null : (TypePriorities) tp.clone();
  }

  /**
   * Revert to last valid.
   *
   * @param msg
   *          the msg
   * @param msgDetails
   *          the msg details
   * @return true, if successful
   */
  private boolean revertToLastValid(String msg, String msgDetails) {
    String[] buttonLabels = new String[2];
    buttonLabels[0] = Messages.getString("MultiPageEditor.revertToLastValid"); //$NON-NLS-1$
    buttonLabels[1] = Messages.getString("MultiPageEditor.EditExisting"); //$NON-NLS-1$
    MessageDialog dialog = new MessageDialog(getEditorSite().getShell(), msg, null, msgDetails,
            MessageDialog.WARNING, buttonLabels, 0);
    dialog.open();
    // next line depends on return code for button 1 (which is 1)
    // and CANCEL code both being == 1
    return dialog.getReturnCode() == 0;
  }

  /**
   * Called when switching off of the indexes page Goal is to validate indexes by making a CAS - as
   * a side effect it does index validation.
   * 
   * We do this without changing the typeSystemDescription
   * 
   * @return is valid state
   */
  private boolean validateIndexes() {
    CAS localCAS = descriptorCAS.get();
    TypePriorities savedMergedTypePriorities = getMergedTypePriorities();
    FsIndexCollection savedFsIndexCollection = getMergedFsIndexCollection();
    try {
      setMergedFsIndexCollection();
      setMergedTypePriorities();
      descriptorCAS.validate();
    } catch (Exception ex) {
      descriptorCAS.set(localCAS);
      if (!revertToLastValid(Messages.getString("MultiPageEditor.indexDefProblemTitle"), //$NON-NLS-1$
              Messages.getString("MultiPageEditor.indexDefProblem") + //$NON-NLS-1$
                      getMessagesToRootCause(ex))) {
        // currentIndex = -1; //irrelevent, but not sourceIndex
        super.setActivePage(indexesIndex);
        // currentIndex = indexesIndex;
        return false;
      } else {
        getAeDescription().getAnalysisEngineMetaData().setTypePriorities(m_typePrioritiesBackup);
        setMergedTypePriorities(savedMergedTypePriorities);
        setMergedFsIndexCollection(savedFsIndexCollection);
        isRevertingIndex = true;
        return true;
      }
    }
    return true;
  }

  /**
   * Gets the char set.
   *
   * @param text
   *          the text
   * @return the char set
   */
  public String getCharSet(String text) {
    final String key = Messages.getString("MultiPageEditor.16"); //$NON-NLS-1$
    int i = text.indexOf(key);
    if (i == -1) {
      return Messages.getString("MultiPageEditor.17"); //$NON-NLS-1$
    }
    i += key.length();
    int end = text.indexOf(Messages.getString("MultiPageEditor.18"), i); //$NON-NLS-1$
    return text.substring(i, end);
  }

  /**
   * Validate source.
   *
   * @return true, if successful
   */
  private boolean validateSource() {
    if (!sourceChanged)
      return true;

    isBadXML = true; // preset
    IDocument doc = sourceTextEditor.getDocumentProvider()
            .getDocument(sourceTextEditor.getEditorInput());
    String text = doc.get();
    InputStream is;
    try {
      is = new ByteArrayInputStream(text.getBytes(getCharSet(text)));
    } catch (UnsupportedEncodingException e2) {
      Utility.popMessage(Messages.getString("MultiPageEditor.19"), //$NON-NLS-1$
              getMessagesToRootCause(e2), MessageDialog.ERROR);
      super.setActivePage(sourceIndex);
      return false;
    }

    String filePathName = getFile().getLocation().toString();
    XMLInputSource input = new XMLInputSource(is, new File(filePathName));

    AnalysisEngineDescription oldAe = aeDescription;
    TypeSystemDescription oldTsdWithResolvedImports = mergedTypeSystemDescription;

    try {
      parseSource(input, filePathName, true); // sets isBadXML to false if OK
    } catch (PartInitException e1) { // if user switched the kind of descriptor
      Utility.popMessage(Messages.getString("MultiPageEditor.20"), //$NON-NLS-1$
              getMessagesToRootCause(e1), MessageDialog.ERROR);
      super.setActivePage(sourceIndex);
      return false;
    }

    if (isBadXML) {
      return false;
    }

    if (isPrimitive()) {
      checkForNewlyDirtyTypes(oldTsdWithResolvedImports);
    }

    checkForNewlyStaleSections(oldAe.getAnalysisEngineMetaData(),
            aeDescription.getAnalysisEngineMetaData());
    return true;
  }

  /**
   * Mark all pages stale.
   */
  public void markAllPagesStale() {
    checkForNewlyStaleSections(null, null);
  }

  /**
   * Check for newly stale sections.
   *
   * @param previous
   *          the previous
   * @param current
   *          the current
   */
  private void checkForNewlyStaleSections(MetaDataObject previous, MetaDataObject current) {

    // AnalysisEngineMetaData previous,
    // AnalysisEngineMetaData current

    // some day can implement code to see what's affected
    // for now, mark everything as stale
    // index tests during development - some pages not done
    if (overviewIndex >= 0) {
      ((HeaderPage) pages.get(overviewIndex)).markStale();
    }
    if (aggregateIndex >= 0) {
      ((HeaderPage) pages.get(aggregateIndex)).markStale();
    }
    if (parameterIndex >= 0) {
      ((HeaderPage) pages.get(parameterIndex)).markStale();
    }
    if (settingsIndex >= 0) {
      ((HeaderPage) pages.get(settingsIndex)).markStale();
    }
    if (typeIndex >= 0) {
      ((HeaderPage) pages.get(typeIndex)).markStale();
    }
    if (capabilityIndex >= 0) {
      ((HeaderPage) pages.get(capabilityIndex)).markStale();
    }
    if (indexesIndex >= 0) {
      ((HeaderPage) pages.get(indexesIndex)).markStale();
    }
    if (resourcesIndex >= 0) {
      ((HeaderPage) pages.get(resourcesIndex)).markStale();
    }
  }

  /**
   * Check for newly dirty types.
   *
   * @param oldTsd
   *          the old tsd
   */
  private void checkForNewlyDirtyTypes(TypeSystemDescription oldTsd) {

    // an array of TypeDescription objects (not CAS), including imported ones
    TypeDescription[] oldTypes = (null == oldTsd || null == oldTsd.getTypes())
            ? new TypeDescription[0]
            : oldTsd.getTypes();
    HashMap oldTypeHash = new HashMap(oldTypes.length);

    for (int i = 0, length = oldTypes.length; i < length; i++) {
      TypeDescription oldType = oldTypes[i];
      oldTypeHash.put(oldType.getName(), oldType);
    }

    TypeDescription[] newTypes = mergedTypeSystemDescription.getTypes();
    for (int i = 0; i < newTypes.length; i++) {
      TypeDescription newType = newTypes[i];
      TypeDescription oldType = (TypeDescription) oldTypeHash.get(newType.getName());

      if (newType.equals(oldType)) {
        oldTypeHash.remove(oldType.getName());
      } else {
        addDirtyTypeName(newType.getName());
        if (oldType != null) {
          oldTypeHash.remove(oldType.getName());
        }
      }
    }

    Set deletedTypes = oldTypeHash.keySet();
    Iterator deletedTypeIterator = deletedTypes.iterator();
    while (deletedTypeIterator.hasNext()) {
      removeDirtyTypeName((String) deletedTypeIterator.next());
    }

  }

  /**
   * Gets the true descriptor.
   *
   * @return the true descriptor
   */
  /*
   * This returns the true descriptor, accounting for the "trick" when we put CPM descriptors in the
   * AE descriptor. As a side effect, it updates the CPM descriptors
   */
  private XMLizable getTrueDescriptor() {
    XMLizable thing;
    if (isAeDescriptor()) {
      thing = aeDescription;
    } else if (isTypeSystemDescriptor()) {
      thing = typeSystemDescription;
    } else if (isTypePriorityDescriptor()) {
      thing = aeDescription.getAnalysisEngineMetaData().getTypePriorities();
    } else if (isExtResAndBindingsDescriptor()) {
      thing = aeDescription.getResourceManagerConfiguration();
    } else if (isFsIndexCollection()) {
      thing = aeDescription.getAnalysisEngineMetaData().getFsIndexCollection();
    } else if (isCollectionReaderDescriptor()) {
      thing = collectionReaderDescription;
      linkLocalProcessingDescriptorsFromAe(collectionReaderDescription);
    } else if (isCasInitializerDescriptor()) {
      thing = casInitializerDescription;
      linkLocalProcessingDescriptorsFromAe(casInitializerDescription);
    } else if (isCasConsumerDescriptor()) {
      thing = casConsumerDescription;
      linkLocalProcessingDescriptorsFromAe(casConsumerDescription);
    } else if (isFlowControllerDescriptor()) {
      thing = flowControllerDescription;
      linkLocalProcessingDescriptorsFromAe(flowControllerDescription);
    } else {
      throw new InternalErrorCDE(Messages.getString("MultiPageEditor.21")); //$NON-NLS-1$
    }
    return thing;
  }

  /**
   * Pretty print model.
   *
   * @return the string
   */
  public String prettyPrintModel() {
    StringWriter writer = new StringWriter();
    String parsedText = null;
    try {
      XMLSerializer xmlSerializer = new XMLSerializer(true);
      xmlSerializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
              Integer.valueOf(MultiPageEditorContributor.getXMLindent()).toString());
      xmlSerializer.setIndent(true);
      xmlSerializer.setWriter(writer);
      ContentHandler contentHandler = xmlSerializer.getContentHandler();
      contentHandler.startDocument();
      XMLizable trueDescriptor = getTrueDescriptor();
      if (trueDescriptor instanceof AnalysisEngineDescription) {
        AnalysisEngineDescription aed = (AnalysisEngineDescription) trueDescriptor;
        aed.toXML(contentHandler, true, true);
      } else {
        trueDescriptor.toXML(contentHandler, true);
      }
      contentHandler.endDocument();
      writer.close();
      parsedText = writer.toString();

    } catch (SAXException e) {
      throw new InternalErrorCDE(Messages.getString("MultiPageEditor.22"), e); //$NON-NLS-1$
    } catch (IOException e) {
      throw new InternalErrorCDE(Messages.getString("MultiPageEditor.23"), e); //$NON-NLS-1$
    }
    return parsedText;
  }

  /**
   * Update source from model.
   */
  public void updateSourceFromModel() {
    sourceTextEditor.setIgnoreTextEvent(true);
    IDocument doc = sourceTextEditor.getDocumentProvider()
            .getDocument(sourceTextEditor.getEditorInput());
    doc.set(prettyPrintModel());
    sourceTextEditor.setIgnoreTextEvent(false);
  }

  /**
   * Gets the ae description.
   *
   * @return the ae description
   */
  public AnalysisEngineDescription getAeDescription() {
    return aeDescription;
  }

  /**
   * Sets the ae description.
   *
   * @param aAnalysisEngineDescription
   *          the new ae description
   * @throws ResourceInitializationException
   *           -
   */
  public void setAeDescription(AnalysisEngineDescription aAnalysisEngineDescription)
          throws ResourceInitializationException {
    if (null == aAnalysisEngineDescription) {
      throw new InternalErrorCDE(Messages.getString("MultiPageEditor.24")); //$NON-NLS-1$
    }
    aeDescription = aAnalysisEngineDescription;

    try {
      // we do this to keep resolvedDelegates update-able
      // The value from getDeletageAESpecs is an unmodifiable hash map
      resolvedDelegates
              .putAll(aeDescription.getDelegateAnalysisEngineSpecifiers(createResourceManager()));
    } catch (InvalidXMLException e) {
      throw new ResourceInitializationException(e);
    }
    // get the metadata once, because it can be expensive to do
    AnalysisEngineMetaData md = aeDescription.getAnalysisEngineMetaData();

    // These come before setTypeSystemDescription call because that call
    // invokes tcas validate, which uses the merged values for speedup
    // Here we set them to values that won't cause errors. They're set to actual values below.
    mergedFsIndexCollection = md.getFsIndexCollection();
    mergedTypePriorities = md.getTypePriorities();
    resolvedExternalResourcesAndBindings = aeDescription.getResourceManagerConfiguration();
    resolvedFlowControllerDeclaration = aeDescription.getFlowControllerDeclaration();

    setTypeSystemDescription(aeDescription.isPrimitive() ? md.getTypeSystem() : null);
    // aggregates have null type system descriptors.
    // If passed in one that isn't null, make it null.

    // These come after setTypeSystemDescription call, even though
    // that call invokeds tcas validate, which uses the merged values for speedup
    // Therefore, merged values have to be set to proper ideas first.
    setMergedFsIndexCollection();
    setImportedFsIndexCollection();
    setMergedTypePriorities();
    setImportedTypePriorities();
    try {
      setResolvedExternalResourcesAndBindings();
      // setImportedExternalResourcesAndBindings();
    } catch (InvalidXMLException e1) {
      throw new ResourceInitializationException(e1);
    }
    try {
      setResolvedFlowControllerDeclaration();
    } catch (InvalidXMLException e1) {
      throw new ResourceInitializationException(e1);
    }
  }

  // note that this also updates merged type system
  /**
   * Sets the type system description.
   *
   * @param typeSystemDescription
   *          the new type system description
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  // Also called for aggregate TAEs
  public void setTypeSystemDescription(TypeSystemDescription typeSystemDescription)
          throws ResourceInitializationException {
    boolean doValidation = true;

    this.typeSystemDescription = typeSystemDescription;

    // This could be a tsd associated with a primitive TAE descriptor, or
    // it could be a tsd from a tsd
    if (typeSystemDescription == null) {
      if (!isAggregate()) {
        this.typeSystemDescription = UIMAFramework.getResourceSpecifierFactory()
                .createTypeSystemDescription();
        doValidation = false; // speed up by 1/3 second
      }
    }

    setMergedTypeSystemDescription();

    // setImportedTypeSystemDescription(); // done in above call

    if (aeDescription == null) {
      aeDescription = UIMAFramework.getResourceSpecifierFactory().createAnalysisEngineDescription();
    }
    aeDescription.getAnalysisEngineMetaData().setTypeSystem(this.typeSystemDescription);

    if (doValidation) {
      descriptorCAS.validate();
    }
  }

  // **************************************************************
  // * From taeDescriptor back into the Collection part descriptors
  /**
   * Link local processing descriptors from ae.
   *
   * @param d
   *          the d
   */
  // **************************************************************
  private void linkLocalProcessingDescriptorsFromAe(CollectionReaderDescription d) {
    d.setImplementationName(aeDescription.getAnnotatorImplementationName());
    d.setFrameworkImplementation(aeDescription.getFrameworkImplementation());
    linkCommonCollectionDescriptorsFromAe(d);
  }

  /**
   * Link local processing descriptors from ae.
   *
   * @param d
   *          the d
   */
  private void linkLocalProcessingDescriptorsFromAe(CasInitializerDescription d) {
    d.setImplementationName(aeDescription.getAnnotatorImplementationName());
    d.setFrameworkImplementation(aeDescription.getFrameworkImplementation());
    linkCommonCollectionDescriptorsFromAe(d);
  }

  /**
   * Link local processing descriptors from ae.
   *
   * @param d
   *          the d
   */
  private void linkLocalProcessingDescriptorsFromAe(CasConsumerDescription d) {
    d.setImplementationName(aeDescription.getAnnotatorImplementationName());
    d.setFrameworkImplementation(aeDescription.getFrameworkImplementation());
    linkCommonCollectionDescriptorsFromAe(d);
  }

  /**
   * Link local processing descriptors from ae.
   *
   * @param d
   *          the d
   */
  private void linkLocalProcessingDescriptorsFromAe(FlowControllerDescription d) {
    d.setImplementationName(aeDescription.getAnnotatorImplementationName());
    d.setFrameworkImplementation(aeDescription.getFrameworkImplementation());
    linkCommonCollectionDescriptorsFromAe(d);
  }

  /**
   * Link common collection descriptors from ae.
   *
   * @param r
   *          the r
   */
  private void linkCommonCollectionDescriptorsFromAe(ResourceCreationSpecifier r) {
    r.setExternalResourceDependencies(aeDescription.getExternalResourceDependencies());
    r.setMetaData(convertFromAeMetaData((AnalysisEngineMetaData) aeDescription.getMetaData()));
    r.setResourceManagerConfiguration(aeDescription.getResourceManagerConfiguration());
  }

  // *********************************************************
  // * From Collection Part Descriptors into the taeDescriptor
  // *********************************************************

  /**
   * Creates the and link local processing descriptors to ae.
   *
   * @param d
   *          the d
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void createAndLinkLocalProcessingDescriptorsToAe(CollectionReaderDescription d)
          throws ResourceInitializationException {
    aeDescription = UIMAFramework.getResourceSpecifierFactory().createAnalysisEngineDescription();
    aeDescription.setAnnotatorImplementationName(d.getImplementationName());
    aeDescription.setFrameworkImplementation(d.getFrameworkImplementation());
    linkLocalProcessingDescriptorsToAe(d);
  }

  /**
   * Creates the and link local processing descriptors to ae.
   *
   * @param d
   *          the d
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void createAndLinkLocalProcessingDescriptorsToAe(CasInitializerDescription d)
          throws ResourceInitializationException {
    aeDescription = UIMAFramework.getResourceSpecifierFactory().createAnalysisEngineDescription();
    aeDescription.setAnnotatorImplementationName(d.getImplementationName());
    aeDescription.setFrameworkImplementation(d.getFrameworkImplementation());
    linkLocalProcessingDescriptorsToAe(d);
  }

  /**
   * Creates the and link local processing descriptors to ae.
   *
   * @param d
   *          the d
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void createAndLinkLocalProcessingDescriptorsToAe(CasConsumerDescription d)
          throws ResourceInitializationException {
    aeDescription = UIMAFramework.getResourceSpecifierFactory().createAnalysisEngineDescription();
    aeDescription.setAnnotatorImplementationName(d.getImplementationName());
    aeDescription.setFrameworkImplementation(d.getFrameworkImplementation());
    linkLocalProcessingDescriptorsToAe(d);
  }

  /**
   * Creates the and link local processing descriptors to ae.
   *
   * @param d
   *          the d
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void createAndLinkLocalProcessingDescriptorsToAe(FlowControllerDescription d)
          throws ResourceInitializationException {
    aeDescription = UIMAFramework.getResourceSpecifierFactory().createAnalysisEngineDescription();
    aeDescription.setAnnotatorImplementationName(d.getImplementationName());
    aeDescription.setFrameworkImplementation(d.getFrameworkImplementation());
    linkLocalProcessingDescriptorsToAe(d);
  }

  /**
   * Link local processing descriptors to ae.
   *
   * @param r
   *          the r
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void linkLocalProcessingDescriptorsToAe(ResourceCreationSpecifier r)
          throws ResourceInitializationException {
    aeDescription.setExternalResourceDependencies(r.getExternalResourceDependencies());
    aeDescription.setMetaData(convertToAeMetaData(r.getMetaData()));
    aeDescription.setPrimitive(true);
    aeDescription.setResourceManagerConfiguration(r.getResourceManagerConfiguration());
    setAeDescription(aeDescription);
  }

  /**
   * Convert to ae meta data.
   *
   * @param r
   *          the r
   * @return the analysis engine meta data
   */
  private AnalysisEngineMetaData convertToAeMetaData(ResourceMetaData r) {
    ProcessingResourceMetaData p = (ProcessingResourceMetaData) r;
    AnalysisEngineMetaData d = UIMAFramework.getResourceSpecifierFactory()
            .createAnalysisEngineMetaData();
    d.setCapabilities(p.getCapabilities());
    d.setConfigurationParameterDeclarations(p.getConfigurationParameterDeclarations());
    d.setConfigurationParameterSettings(p.getConfigurationParameterSettings());
    d.setCopyright(p.getCopyright());
    d.setDescription(p.getDescription());
    d.setFsIndexCollection(p.getFsIndexCollection());
    d.setName(p.getName());
    d.setTypePriorities(p.getTypePriorities());
    d.setTypeSystem(p.getTypeSystem());
    d.setVendor(p.getVendor());
    d.setVersion(p.getVersion());
    d.setOperationalProperties(p.getOperationalProperties());
    ((AnalysisEngineMetaData_impl) d).setInfoset(((MetaDataObject_impl) r).getInfoset());
    return d;
  }

  /**
   * Convert from ae meta data.
   *
   * @param p
   *          the p
   * @return the processing resource meta data
   */
  private ProcessingResourceMetaData convertFromAeMetaData(AnalysisEngineMetaData p) {
    ProcessingResourceMetaData d = UIMAFramework.getResourceSpecifierFactory()
            .createProcessingResourceMetaData();
    d.setCapabilities(p.getCapabilities());
    d.setConfigurationParameterDeclarations(p.getConfigurationParameterDeclarations());
    d.setConfigurationParameterSettings(p.getConfigurationParameterSettings());
    d.setCopyright(p.getCopyright());
    d.setDescription(p.getDescription());
    d.setFsIndexCollection(p.getFsIndexCollection());
    d.setName(p.getName());
    d.setTypePriorities(p.getTypePriorities());
    d.setTypeSystem(p.getTypeSystem());
    d.setVendor(p.getVendor());
    d.setVersion(p.getVersion());
    d.setOperationalProperties(p.getOperationalProperties());
    ((MetaDataObject_impl) d).setInfoset(((MetaDataObject_impl) p).getInfoset());
    return d;
  }

  /**
   * Sets the collection reader description.
   *
   * @param d
   *          the new collection reader description
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void setCollectionReaderDescription(CollectionReaderDescription d)
          throws ResourceInitializationException {
    collectionReaderDescription = d;
    createAndLinkLocalProcessingDescriptorsToAe(d);
  }

  /**
   * Sets the cas initializer description.
   *
   * @param d
   *          the new cas initializer description
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void setCasInitializerDescription(CasInitializerDescription d)
          throws ResourceInitializationException {
    casInitializerDescription = d;
    createAndLinkLocalProcessingDescriptorsToAe(d);
  }

  /**
   * Sets the cas consumer description.
   *
   * @param d
   *          the new cas consumer description
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void setCasConsumerDescription(CasConsumerDescription d)
          throws ResourceInitializationException {
    casConsumerDescription = d;
    createAndLinkLocalProcessingDescriptorsToAe(d);
  }

  /**
   * Sets the flow controller description.
   *
   * @param d
   *          the new flow controller description
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void setFlowControllerDescription(FlowControllerDescription d)
          throws ResourceInitializationException {
    flowControllerDescription = d;
    createAndLinkLocalProcessingDescriptorsToAe(d);
  }

  /**
   * Sets the type priorities.
   *
   * @param typePriorities
   *          the new type priorities
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void setTypePriorities(TypePriorities typePriorities)
          throws ResourceInitializationException {
    loadContext(typePriorities);
    aeDescription.getAnalysisEngineMetaData().setTypePriorities(typePriorities);
    setMergedTypePriorities();
    setImportedTypePriorities();
    descriptorCAS.validate();
  }

  /**
   * The Class MultilevelCancel.
   */
  private static class MultilevelCancel extends RuntimeException {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;
  }

  /**
   * Load context.
   *
   * @param thing
   *          the thing
   */
  private void loadContext(XMLizable thing) {
    // try to load a context that has the types
    if (isContextLoaded) {
      return;
    }
    String contextFile = null;
    XMLInputSource input = null;
    aeDescription = null;
    openingContext = true;
    try {
      try {
        contextFile = fileNeedingContext.getPersistentProperty(new QualifiedName(
                AbstractSection.PLUGIN_ID, AbstractSection.IMPORTABLE_PART_CONTEXT));
      } catch (CoreException e) {
        throw new InternalErrorCDE("unexpected exception", e);
      }
      ContextForPartDialog dialog = new ContextForPartDialog(
              PlatformUI.getWorkbench().getDisplay().getShells()[0], // ok in Eclipse 3.0
              getFile().getProject().getParent(), thing, getFile().getLocation(), this,
              contextFile);
      dialog.setTitle("File specifying context for editing importable part");
      if (dialog.open() == Window.CANCEL) {
        throw new MultilevelCancel();
      }

      contextFile = dialog.contextPath;

      if (null == contextFile) {
        Utility.popMessage("Context Info",
                "A context is required to edit this part.  However no context was supplied.  Editing will be cancelled",
                MessageDialog.INFORMATION);
        throw new MultilevelCancel();
      } else {
        try {
          input = new XMLInputSource(contextFile);
        } catch (IOException e) {
          showContextLoadFailureMessage(e, contextFile);
          throw new MultilevelCancel();
        }
        if (null != input) {
          try {
            parseSource(input, contextFile, !PRESERVE_COMMENTS);
          } catch (PartInitException e) {
            showContextLoadFailureMessage(e, contextFile);
            throw new MultilevelCancel();
          }
        }
      }
    } finally {
      openingContext = false;
    }
    if (null == aeDescription) {
      aeDescription = UIMAFramework.getResourceSpecifierFactory().createAnalysisEngineDescription();
    } else {
      try {
        file.setPersistentProperty(new QualifiedName(AbstractSection.PLUGIN_ID,
                AbstractSection.IMPORTABLE_PART_CONTEXT), contextFile);
      } catch (CoreException e) {
        Utility.popMessage("Unexpected Exception",
                "While loading Context" + getMessagesToRootCause(e), MessageDialog.ERROR);
        throw new InternalErrorCDE("Unexpected Exception:" + getMessagesToRootCause(e), e);
      }
    }
  }

  /**
   * Sets the fs index collection.
   *
   * @param indexCollection
   *          the new fs index collection
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void setFsIndexCollection(FsIndexCollection indexCollection)
          throws ResourceInitializationException {
    loadContext(indexCollection);
    aeDescription.getAnalysisEngineMetaData().setFsIndexCollection(indexCollection);
    setMergedFsIndexCollection();
    setImportedFsIndexCollection();
    descriptorCAS.validate();
  }

  /**
   * Show context load failure message.
   *
   * @param e
   *          the e
   * @param contextFile
   *          the context file
   */
  private void showContextLoadFailureMessage(Exception e, String contextFile) {
    String m = Messages.getFormattedString("MultiPageEditor.IOError", //$NON-NLS-1$
            new String[] { AbstractSection.maybeShortenFileName(contextFile) })
            + Messages.getString("MultiPageEditor.10") + getMessagesToRootCause(e); //$NON-NLS-1$
    Utility.popMessage("Cannot load context",
            m + "\nCannot load the context file for this importable part due to an I/O exception"
                    + " - proceeding without context",
            MessageDialog.WARNING);
  }

  /**
   * Only called when editing a resources/bindings descriptor.
   *
   * @param rb
   *          the new ext res and bindings
   * @throws ResourceInitializationException
   *           -
   */
  private void setExtResAndBindings(ResourceManagerConfiguration rb)
          throws ResourceInitializationException {
    loadContext(rb);
    aeDescription.setResourceManagerConfiguration(rb);
    try {
      setResolvedExternalResourcesAndBindings();
      // setImportedExternalResourcesAndBindings();
    } catch (InvalidXMLException e) {
      throw new ResourceInitializationException(e);
    }
    descriptorCAS.validate();
  }

  /**
   * Gets the absolute path from import.
   *
   * @param importItem
   *          the import item
   * @return the absolute path from import
   */
  public String getAbsolutePathFromImport(Import importItem) {
    // getAbsoluteURLfromImport may return a bundleresource style url
    return new File(getAbsoluteURLfromImport(importItem).getPath()).getPath();
  }

  /**
   * Gets the absolute UR lfrom import.
   *
   * @param importItem
   *          the import item
   * @return the absolute UR lfrom import
   */
  private URL getAbsoluteURLfromImport(Import importItem) {
    try {
      // if by location, it's relative to the descriptor.
      return Platform.asLocalURL(importItem.findAbsoluteUrl(createResourceManager()));
    } catch (InvalidXMLException ex) {
      ex.printStackTrace();
    } catch (IOException e) {
    }
    return null;
  }

  /**
   * Gets the aggregate page.
   *
   * @return the aggregate page
   */
  public AggregatePage getAggregatePage() {
    return aggregatePage;
  }

  /**
   * Gets the overview page.
   *
   * @return the overview page
   */
  public OverviewPage getOverviewPage() {
    return overviewPage;
  }

  /**
   * Gets the parameter page.
   *
   * @return the parameter page
   */
  public ParameterPage getParameterPage() {
    return parameterPage;
  }

  /**
   * Gets the type page.
   *
   * @return the type page
   */
  public TypePage getTypePage() {
    return typePage;
  }

  /**
   * Gets the capability page.
   *
   * @return the capability page
   */
  public CapabilityPage getCapabilityPage() {
    return capabilityPage;
  }

  /**
   * Gets the indexes page.
   *
   * @return the indexes page
   */
  public IndexesPage getIndexesPage() {
    return indexesPage;
  }

  /**
   * Gets the resources page.
   *
   * @return the resources page
   */
  public ResourcesPage getResourcesPage() {
    return resourcesPage;
  }

  /**
   * Gets the XML editor page.
   *
   * @return the XML editor page
   */
  public XMLEditor getXMLEditorPage() {
    return sourceTextEditor;
  }

  /**
   * Gets the settings page.
   *
   * @return the settings page
   */
  public SettingsPage getSettingsPage() {
    return settingsPage;
  }

  /**
   * Gets the file.
   *
   * @return current file being edited
   */
  public IFile getFile() {
    return file;
  }

  /**
   * Gets the resolved delegates.
   *
   * @return the resolved delegates
   */
  public Map getResolvedDelegates() {
    return resolvedDelegates;
  }

  /**
   * gets the Hash Map of resolved AE delegates Clones the description first because the getting
   * updates it in some cases.
   *
   * @param aed
   *          the aed
   * @return the Map of resolved AE delegates
   */
  public Map getDelegateAEdescriptions(AnalysisEngineDescription aed) {
    Map result = new HashMap();
    AnalysisEngineDescription aedClone = (AnalysisEngineDescription) ((AnalysisEngineDescription_impl) aed)
            .clone();
    try {
      result = aedClone.getDelegateAnalysisEngineSpecifiers(createResourceManager());
    } catch (InvalidXMLException e) {

    }
    return result;
  }

  /**
   * Mark T cas dirty.
   */
  public void markTCasDirty() {
    descriptorCAS.markDirty();
    allTypes.markDirty();
    definedTypesWithSupers.markDirty();
  }

  /**
   * Gets the current view.
   *
   * @return the current view
   */
  public CAS getCurrentView() {
    return descriptorCAS.get();
  }

  /**
   * Gets the project.
   *
   * @return the project
   */
  public IProject getProject() {
    IFile iFile = getFile();
    if (null == iFile) {
      // call
      return null;
    }
    return getFile().getProject();
  }

  /**
   * Gets the descriptor directory.
   *
   * @return the descriptor directory
   */
  public String getDescriptorDirectory() {
    String sDir = file.getParent().getLocation().toString();
    if (sDir.charAt(sDir.length() - 1) != '/') {
      sDir += '/';
    }
    return sDir;
  }

  /**
   * Gets the descriptor relative path.
   *
   * @param aFullOrRelativePath
   *          the a full or relative path
   * @return the descriptor relative path
   */
  public String getDescriptorRelativePath(String aFullOrRelativePath) {
    String sEditorFileFullPath = getFile().getLocation().toString();
    String sFullOrRelativePath = aFullOrRelativePath.replace('\\', '/');

    // first, if not in workspace, or if a relative path, not a full path, return path
    String sWorkspacePath = TAEConfiguratorPlugin.getWorkspace().getRoot().getLocation().toString();
    if (sFullOrRelativePath.indexOf(sWorkspacePath) == -1) {
      return sFullOrRelativePath;
    }

    String sFullPath = sFullOrRelativePath; // rename the var to its semantics

    String commonPrefix = getCommonParentFolder(sEditorFileFullPath, sFullPath);
    if (commonPrefix.length() < 2 || commonPrefix.indexOf(':') == commonPrefix.length() - 2) {
      return sFullPath;
    }

    // now count extra slashes to determine how many ..'s are needed
    int nCountBackDirs = 0;
    String sRelativePath = ""; //$NON-NLS-1$
    for (int i = commonPrefix.length(); i < sEditorFileFullPath.length(); i++) {
      if (sEditorFileFullPath.charAt(i) == '/') {
        sRelativePath += "../"; //$NON-NLS-1$
        nCountBackDirs++;
      }
    }
    sRelativePath += sFullPath.substring(commonPrefix.length());
    return sRelativePath;
  }

  /**
   * Gets the common parent folder.
   *
   * @param sFile1
   *          the s file 1
   * @param sFile2
   *          the s file 2
   * @return the common parent folder
   */
  private static String getCommonParentFolder(String sFile1, String sFile2) {
    if (sFile1 == null || sFile2 == null) {
      return ""; //$NON-NLS-1$
    }

    int maxLength = (sFile1.length() <= sFile2.length() ? sFile1.length() : sFile2.length());
    int commonPrefixLength = 0;
    for (int i = 0; i < maxLength; i++) {
      if (sFile1.charAt(i) != sFile2.charAt(i) || (i == maxLength - 1)) { // catch files which have
        // same prefix
        for (int j = i; j >= 0; j--) {
          if (sFile1.charAt(j) == '/' || sFile1.charAt(j) == '\\') {
            commonPrefixLength = j + 1;
            break;
          }
        }
        break;
      }
    }

    return sFile1.substring(0, commonPrefixLength);
  }

  /**
   * Checks if is file in workspace.
   *
   * @param aFileRelPath
   *          the a file rel path
   * @return true, if is file in workspace
   */
  public boolean isFileInWorkspace(String aFileRelPath) {
    Object fileOrIFile = getIFileOrFile(aFileRelPath);
    return (fileOrIFile instanceof IFile && ((IFile) fileOrIFile).exists());
  }

  /**
   * Gets the full path from descriptor relative path.
   *
   * @param aDescRelPath
   *          the a desc rel path
   * @return the full path from descriptor relative path
   */
  public String getFullPathFromDescriptorRelativePath(String aDescRelPath) {

    if (aDescRelPath.indexOf(':') > 0) { // indicates already an absolute path on Windows, at least
      return aDescRelPath.replace('\\', '/');
    }

    String sEditorFileFullPath = getFile().getLocation().toString();
    String sDescRelPath = aDescRelPath.replace('\\', '/');

    int nCountDirsToBackup = 0;
    int nNextFindLoc = -1;
    int nLastFindLoc = -1;
    while (true) {
      nLastFindLoc = nNextFindLoc;
      nNextFindLoc = sDescRelPath.indexOf("../", nNextFindLoc + 1); //$NON-NLS-1$
      if (nNextFindLoc > -1) {
        nCountDirsToBackup++;
      } else {
        break;
      }
    }
    String sFinalFragment = ""; //$NON-NLS-1$
    if (nCountDirsToBackup > 0) {
      sFinalFragment = sDescRelPath.substring(nLastFindLoc + 3);
    }

    if (nCountDirsToBackup == 0) {
      int nEditorFileLastSlash = sEditorFileFullPath.lastIndexOf('/');
      String sEditorFileDirectory = sEditorFileFullPath.substring(0, nEditorFileLastSlash + 1);
      return sEditorFileDirectory + sDescRelPath;
    }

    int nSubDirCount = 0;
    for (int i = 0; i < sEditorFileFullPath.length(); i++) {
      if (sEditorFileFullPath.charAt(i) == '/') {
        nSubDirCount++;
      }
    }
    int[] subDirMarkerLocs = new int[nSubDirCount];
    int j = 0;
    for (int i = 0; i < sEditorFileFullPath.length(); i++) {
      if (sEditorFileFullPath.charAt(i) == '/') {
        subDirMarkerLocs[j++] = i;
      }
    }

    if (nCountDirsToBackup > nSubDirCount) {
      return null;
    }

    return sEditorFileFullPath.substring(0,
            subDirMarkerLocs[nSubDirCount - nCountDirsToBackup - 1] + 1) + sFinalFragment;
  }

  /**
   * Open.
   *
   * @param fileToOpen
   *          the file to open
   */
  public void open(IFile fileToOpen) {
    final IFile ffile = fileToOpen;
    Shell shell = new Shell();
    shell.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
          page.openEditor(new FileEditorInput(ffile), "taeconfigurator.editors.MultiPageEditor"); //$NON-NLS-1$
        } catch (PartInitException e) {
          throw new InternalErrorCDE("unexpected exception");
        }
      }
    });
  }

  /**
   * Open text editor.
   *
   * @param fileToOpen
   *          the file to open
   */
  public void openTextEditor(IFile fileToOpen) {
    final IFile ffile = fileToOpen;
    Shell shell = new Shell();
    shell.getDisplay().asyncExec(new Runnable() {
      @Override
      public void run() {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
          page.openEditor(new FileEditorInput(ffile), "org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
        } catch (PartInitException e) {
          throw new InternalErrorCDE("unexpected exception");
        }
      }
    });
  }

  /**
   * Gets the i file or file.
   *
   * @param relOrAbsPath
   *          the rel or abs path
   * @return the i file or file
   */
  public Object getIFileOrFile(String relOrAbsPath) {
    String sFileFullPath = getFullPathFromDescriptorRelativePath(relOrAbsPath);
    String sWorkspacePath = TAEConfiguratorPlugin.getWorkspace().getRoot().getLocation().toString();

    boolean bHasWorkspacePath = (sFileFullPath.indexOf(sWorkspacePath) > -1);
    if (bHasWorkspacePath) {
      Path path = new Path(sFileFullPath);
      return TAEConfiguratorPlugin.getWorkspace().getRoot().getFileForLocation(path);
    }
    return new File(sFileFullPath);
  }

  /**
   * Open.
   *
   * @param fullPath
   *          the full path
   */
  public void open(String fullPath) {
    Path path = new Path(fullPath);
    IFile fileToOpen = TAEConfiguratorPlugin.getWorkspace().getRoot().getFileForLocation(path);
    open(fileToOpen);
  }

  /**
   * Open text editor.
   *
   * @param fullPath
   *          the full path
   */
  public void openTextEditor(String fullPath) {
    Path path = new Path(fullPath);
    IFile fileToOpen = TAEConfiguratorPlugin.getWorkspace().getRoot().getFileForLocation(path);
    openTextEditor(fileToOpen);
  }

  /**
   * Adds the dirty type name.
   *
   * @param typeName
   *          the type name
   */
  public void addDirtyTypeName(String typeName) {
    dirtyTypeNameHash.add(typeName);
    markTypeModelDirty();
  }

  /**
   * Mark type model dirty.
   */
  private void markTypeModelDirty() {
    allTypes.markDirty();
    descriptorCAS.markDirty();
    definedTypesWithSupers.markDirty();
  }

  /**
   * Removes the dirty type name.
   *
   * @param typeName
   *          the type name
   */
  public void removeDirtyTypeName(String typeName) {
    dirtyTypeNameHash.remove(typeName);
    markTypeModelDirty();
  }

  /**
   * Do J cas gen.
   *
   * @param monitor
   *          the monitor
   */
  public void doJCasGen(IProgressMonitor monitor) {
    if (0 < mergedTypesAddingFeatures.size()) {
      if (Window.CANCEL == Utility.popOkCancel("Type feature merging extended features",
              "Before generating the JCas classes for the CAS types, please note that "
                      + "the following types were generated by merging different type descriptors, "
                      + "where the resulting number of features is larger than that of the components. "
                      + "Although the resulting generated JCas classes are correct, "
                      + "doing this kind of merging makes reuse of this component more difficult."
                      + makeMergeMessage(mergedTypesAddingFeatures)
                      + "\n   Press OK to generate the JCas classes anyway, or cancel to skip generating the JCas classes.",
              MessageDialog.WARNING)) {
        return;
      }
    }
    final JCasGenThrower jCasGenThrower = new JCasGenThrower();

    try {

      final IWorkspace workspace = ResourcesPlugin.getWorkspace();
      final Jg jg = new Jg();
      final TypeDescription[] types = mergedTypeSystemDescription.getTypes();
      final String outputDirectory = getPrimarySourceFolder().getLocation().toOSString();
      final String inputFile = file.getLocation().toOSString(); // path to descriptor file
      IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
        @Override
        public void run(IProgressMonitor progressMonitor) {
          try {
            jg.mainForCde(new MergerImpl(), new JCasGenProgressMonitor(progressMonitor),
                    jCasGenThrower, inputFile, outputDirectory, types, (CASImpl) getCurrentView(),
                    getProject().getLocation().toString(), // https://issues.apache.org/jira/browse/UIMA-5715
                    // getLocationURI().getPath(), // on linux/mars, was returning
                    // /default/project.name etc
                    limitJCasGenToProjectScope, mergedTypesAddingFeatures);
          } catch (IOException e) {
            Utility.popMessage(Messages.getString("MultiPageEditor.25"), //$NON-NLS-1$
                    Messages.getString("MultiPageEditor.26") //$NON-NLS-1$
                            + getMessagesToRootCause(e),
                    MessageDialog.ERROR);
          }
        }
      };
      workspace.run(runnable, monitor);
      getPrimarySourceFolder().refreshLocal(IResource.DEPTH_INFINITE, null);

      String jcasMsg = jCasGenThrower.getMessage();
      if (null != jcasMsg && jcasMsg.length() > 0) {
        Utility.popMessage(Messages.getString("MultiPageEditor.JCasGenErrorTitle"), //$NON-NLS-1$
                Messages.getFormattedString("MultiPageEditor.jcasGenErr", //$NON-NLS-1$
                        new String[] { jcasMsg }),
                MessageDialog.ERROR);
        System.out.println(jcasMsg);
      }
    } catch (Exception ex) {
      Utility.popMessage(Messages.getString("MultiPageEditor.JCasGenErrorTitle"), //$NON-NLS-1$
              Messages.getFormattedString("MultiPageEditor.jcasGenErr", //$NON-NLS-1$
                      new String[] { jCasGenThrower.getMessage() }),
              MessageDialog.ERROR);
      ex.printStackTrace();
    }
  }

  /**
   * Make merge message.
   *
   * @param m
   *          the m
   * @return the string
   */
  // message: TypeName = ".....", URLs defining this type = "xxxx", "xxxx", ....
  private String makeMergeMessage(Map m) {
    StringBuffer sb = new StringBuffer();
    for (Iterator it = m.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry) it.next();
      String typeName = (String) entry.getKey();
      sb.append("\n  ");
      sb.append("TypeName having merged features = ").append(typeName)
              .append("\n    URLs defining this type =");
      Set urls = (Set) entry.getValue();
      boolean afterFirst = false;
      for (Iterator itUrls = urls.iterator(); itUrls.hasNext();) {
        if (afterFirst) {
          sb.append(",\n        ");
        } else {
          sb.append("\n        ");
        }
        afterFirst = true;
        String url = (String) itUrls.next();
        sb.append('"').append(url).append('"');
      }
    }
    return sb.toString();
  }

  /** The Constant PATH_SEPARATOR. */
  final public static String PATH_SEPARATOR = System.getProperty("path.separator"); //$NON-NLS-1$

  /** The cached stamp. */
  private long cachedStamp = -1;

  /** The cached class path. */
  private String cachedClassPath = null;

  /**
   * Gets the project class path.
   *
   * @return the project class path
   * @throws CoreException
   *           the core exception
   */
  public String getProjectClassPath() throws CoreException {
    return getFilteredProjectClassPath(true);
  }

  /**
   * Gets the filtered project class path.
   *
   * @param filterCoreResources
   *          the filter core resources
   * @return the filtered project class path
   * @throws CoreException
   *           the core exception
   */
  public String getFilteredProjectClassPath(boolean filterCoreResources) throws CoreException {
    IProject project = getProject();

    if (null == project || !project.isNatureEnabled("org.eclipse.jdt.core.javanature")) { //$NON-NLS-1$
      return ""; //$NON-NLS-1$
    }
    IJavaProject javaProj = JavaCore.create(project);
    IProject projectRoot = javaProj.getProject();

    IResource classFileResource = projectRoot.findMember(".classpath"); //$NON-NLS-1$
    long stamp = classFileResource.getModificationStamp();
    if (stamp == cachedStamp && filterCoreResources) {
      return cachedClassPath;
    }

    StringBuffer result = new StringBuffer(1000);

    String[] classPaths = JavaRuntime.computeDefaultRuntimeClassPath(javaProj);

    for (int i = 0; i < classPaths.length; i++) {
      String classPath = classPaths[i];
      if (filterCoreResources) {
        URLClassLoader checker = null;
        try {
          // ignore this entry if it is the Java JVM path
          checker = new URLClassLoader(new URL[] { new File(classPath).toURL() });

        } catch (MalformedURLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        if (null == checker ||
        // || null != checker.findResource("java/lang/Object.class")
        // //$NON-NLS-1$
                null != checker.findResource("org/apache/uima/impl/UIMAFramework_impl.class")) { //$NON-NLS-1$
          continue;
        }
      }
      if (result.length() > 0) {
        result = result.append(PATH_SEPARATOR);
      }
      result = result.append(classPath);
    }
    if (filterCoreResources) {
      cachedStamp = stamp;
      cachedClassPath = result.toString();
      return cachedClassPath;
    }
    return result.toString();

  }

  /**
   * Gets the primary source folder.
   *
   * @return the primary source folder
   */
  public IResource getPrimarySourceFolder() {
    IProject project = getProject();
    try {
      if (!project.isNatureEnabled("org.eclipse.jdt.core.javanature")) { //$NON-NLS-1$
        return null;
      }
      IJavaProject javaProj = JavaCore.create(project);
      IPackageFragmentRoot[] frs = javaProj.getPackageFragmentRoots();
      for (int i = 0; i < frs.length; i++) {
        frs[i].open(null);
        IResource resource = frs[i].getResource(); // first folder resource will always be first
        // source folder
        if (resource instanceof IFolder || resource instanceof IProject) {
          return resource;
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return null;
  }

  /**
   * Sets the save as status.
   *
   * @param nStatus
   *          the new save as status
   */
  public void setSaveAsStatus(int nStatus) {
    m_nSaveAsStatus = nStatus;
  }

  /**
   * Gets the type system description.
   *
   * @return the type system description
   */
  public TypeSystemDescription getTypeSystemDescription() {
    return aeDescription.getAnalysisEngineMetaData().getTypeSystem();
  }

  /**
   * Gets the type priorities.
   *
   * @return the type priorities
   */
  public TypePriorities getTypePriorities() {
    return aeDescription.getAnalysisEngineMetaData().getTypePriorities();
  }

  /**
   * Gets the fs index collection.
   *
   * @return the fs index collection
   */
  public FsIndexCollection getFsIndexCollection() {
    return aeDescription.getAnalysisEngineMetaData().getFsIndexCollection();
  }

  /**
   * Gets the ext res and bindings.
   *
   * @return the ext res and bindings
   */
  public ResourceManagerConfiguration getExtResAndBindings() {
    return aeDescription.getResourceManagerConfiguration();
  }

  /** The Constant VALIDATE_INPUTS. */
  private static final boolean VALIDATE_INPUTS = true;

  /**
   * Validate inputs.
   *
   * @param typeNameHash
   *          the type name hash
   * @return true, if successful
   */
  // returns true if no inputs were removed, false otherwise
  public boolean validateInputs(Map typeNameHash) {
    return validateIOs(VALIDATE_INPUTS, typeNameHash);
  }

  /**
   * Validate outputs.
   *
   * @param typeNameHash
   *          the type name hash
   * @return true, if successful
   */
  // returns true if no outputs were removed, false otherwise
  public boolean validateOutputs(Map typeNameHash) {
    return validateIOs(!VALIDATE_INPUTS, typeNameHash);
  }

  /**
   * Validate I os.
   *
   * @param isValidateInputs
   *          the is validate inputs
   * @param typeNameHash
   *          the type name hash
   * @return true, if successful
   */
  public boolean validateIOs(boolean isValidateInputs, Map typeNameHash) {
    boolean bRes = true;

    if (aeDescription != null) {
      Capability[] capabilities = aeDescription.getAnalysisEngineMetaData().getCapabilities();
      if (capabilities == null || capabilities.length == 0) {
        return true;
      }

      TypeOrFeature[] oldIOs = (isValidateInputs) ? capabilities[0].getInputs()
              : capabilities[0].getOutputs();
      Vector validIOs = new Vector();
      for (int i = 0; i < oldIOs.length; i++) {
        String typeName;
        int nColonLoc = oldIOs[i].getName().indexOf(':');
        if (nColonLoc == -1) {
          typeName = oldIOs[i].getName();
        } else {
          typeName = oldIOs[i].getName().substring(0, nColonLoc);
        }
        if (typeNameHash.containsKey(typeName)) {
          validIOs.addElement(oldIOs[i]);
        } else {
          bRes = false;
        }
      }

      if (!bRes) {
        TypeOrFeature[] newIOs = new TypeOrFeature[validIOs.size()];
        for (int i = 0; i < newIOs.length; i++) {
          newIOs[i] = (TypeOrFeature) validIOs.elementAt(i);
        }

        if (isValidateInputs) {
          capabilities[0].setInputs(newIOs);
        } else {
          capabilities[0].setOutputs(newIOs);
        }
      }
    }

    return bRes;
  }

  /**
   * Validate type priorities.
   *
   * @param typeNameHash
   *          the type name hash
   * @return true, if successful
   */
  // returns true if no type priorities were modified, false otherwise
  public boolean validateTypePriorities(Map typeNameHash) {
    boolean bRes = true;

    TypePriorities priorities = aeDescription.getAnalysisEngineMetaData().getTypePriorities();
    if (priorities != null) {
      TypePriorityList[] priorityLists = priorities.getPriorityLists();
      if (priorityLists != null) {
        for (int i = 0; i < priorityLists.length; i++) {
          String[] typeNames = priorityLists[i].getTypes();
          if (typeNames != null) {
            int nCountNewTypeNames = 0;
            for (int j = 0; j < typeNames.length; j++) {
              if (typeNameHash.containsKey(typeNames[j])) {
                nCountNewTypeNames++;
              }
            }
            if (nCountNewTypeNames < typeNames.length) {
              bRes = false;
              String[] newTypeNames = new String[nCountNewTypeNames];
              for (int j = 0, k = 0; j < typeNames.length; j++) {
                if (typeNameHash.containsKey(typeNames[j])) {
                  newTypeNames[k++] = typeNames[j];
                }
              }
              priorityLists[i].setTypes(newTypeNames);
            }
          }
        }
      }
    }

    return bRes;
  }

  /** The Constant previewSize. */
  private static final int previewSize = 1024 * 16;

  /**
   * Used by code to get lists of delegate components by input/output type specs.
   *
   * @param iFile
   *          the i file
   * @param componentHeaders
   *          the component headers
   * @return the delegate resource specifier
   */
  public static ResourceSpecifier getDelegateResourceSpecifier(IFile iFile,
          String[] componentHeaders) {
    if (!iFile.getName().toLowerCase().endsWith(".xml")) { //$NON-NLS-1$
      return null;
    }
    // make a quick assesment of whether file is a TAE
    // looking in the first part of the file, but 1024 isn't big enough
    // because initial comment blocks like the apache license are that big
    // Do 16 K
    char[] acBuffer = new char[previewSize];
    FileReader fileReader = null;
    int nCharsRead = 0;
    try {
      // FileReader is FileInputStream using "default" char-encoding
      fileReader = new FileReader(iFile.getLocation().toString());
      while (true) {
        int tempCharsRead = fileReader.read(acBuffer, nCharsRead, previewSize - nCharsRead);
        if (-1 == tempCharsRead) {
          break;
        }
        nCharsRead = nCharsRead + tempCharsRead;
        if (nCharsRead >= previewSize) {
          break;
        }
      }
    } catch (FileNotFoundException e) {
      return null;
    } catch (IOException e) {
      return null;
    } finally {
      if (null != fileReader) {
        try {
          fileReader.close();
        } catch (IOException e1) {
        }
      }
    }
    if (-1 == nCharsRead) {
      return null;
    }
    String sBuffer = (new String(acBuffer, 0, nCharsRead)).toLowerCase();
    for (int i = 0; i < componentHeaders.length; i++) {
      if (-1 != sBuffer.indexOf(componentHeaders[i])) {
        break;
      }
      if (i == (componentHeaders.length - 1)) {
        return null;
      }
    }

    try {
      XMLInputSource input = new XMLInputSource(iFile.getLocation().toFile());
      XMLizable inputDescription = AbstractSection.parseDescriptor(input);
      if (inputDescription instanceof AnalysisEngineDescription
              || inputDescription instanceof CasConsumerDescription
              || inputDescription instanceof FlowControllerDescription) {
        return (ResourceCreationSpecifier) inputDescription;
      } else if (inputDescription instanceof ResourceServiceSpecifier) {
        return (ResourceSpecifier) inputDescription;
      }
      return null;
    } catch (IOException e) {
      return null;
    } catch (InvalidXMLException e) {
      return null;
    }
  }

  // **************************************************
  // * Getting exception messages down to root
  /**
   * Gets the messages to root cause.
   *
   * @param e
   *          the e
   * @return the messages to root cause
   */
  // **************************************************
  public String getMessagesToRootCause(Throwable e) {
    boolean wantStackTrace = false;
    StringBuffer b = new StringBuffer(200);
    String messagePart = e.getMessage();

    // messages for noClassDef found and NPE don't say what the problem was,
    // so always include the exception class also

    formatMessageWithClass(e, b, messagePart);
    if (null == messagePart) {
      wantStackTrace = true;
    }
    // if (null == messagePart) {
    // b.append(e.getClass().getName());
    // wantStackTrace = true;
    // } else
    // b.append(messagePart);
    Throwable cur = e;
    Throwable next;

    while (null != (next = cur.getCause())) {
      String message = next.getMessage();
      wantStackTrace = false; // only do stack trace if last item has no message
      if (null == message) {
        b.append(next.getClass().getName());
        wantStackTrace = true;
      }
      if (null != message && !message.equals(messagePart)) {
        b.append(Messages.getString("MultiPageEditor.causedBy"));
        formatMessageWithClass(next, b, message);
        messagePart = message;
      }
      cur = next;
    }
    if (wantStackTrace) {
      ByteArrayOutputStream ba = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(ba);
      cur.printStackTrace(ps);
      ps.flush();
      b.append(ba.toString());
      ps.close();
    }
    return b.toString();
  }

  /**
   * Format message with class.
   *
   * @param e
   *          the e
   * @param b
   *          the b
   * @param messagePart
   *          the message part
   */
  private void formatMessageWithClass(Throwable e, StringBuffer b, String messagePart) {
    String name = e.getClass().getName();
    // because this is a message for ordinary users, and
    // because the exceptions are more easily readable without their package prefixes,
    // remove the package prefix from the displayed name
    int lastDot = name.lastIndexOf('.');
    if (lastDot >= 0) {
      name = name.substring(lastDot + 1);
    }
    b.append(name);
    if (null != messagePart) {
      b.append(": ").append(messagePart);
    }
  }

  /**
   * The Class JCasGenProgressMonitor.
   */
  public static class JCasGenProgressMonitor
          implements org.apache.uima.tools.jcasgen.IProgressMonitor {

    /** The m progress monitor. */
    IProgressMonitor m_progressMonitor;

    /**
     * Instantiates a new j cas gen progress monitor.
     *
     * @param progressMonitor
     *          the progress monitor
     */
    public JCasGenProgressMonitor(IProgressMonitor progressMonitor) {
      m_progressMonitor = progressMonitor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.jcas.jcasgen_gen.IProgressMonitor#done()
     */
    @Override
    public void done() {
      m_progressMonitor.done();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.jcas.jcasgen_gen.IProgressMonitor#beginTask(java.lang.String, int)
     */
    @Override
    public void beginTask(String name, int totalWorked) {
      m_progressMonitor.beginTask(name, totalWorked);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.jcas.jcasgen_gen.IProgressMonitor#subTask(java.lang.String)
     */
    @Override
    public void subTask(String name) {
      m_progressMonitor.subTask(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.jcas.jcasgen_gen.IProgressMonitor#worked(int)
     */
    @Override
    public void worked(int work) {
      m_progressMonitor.worked(work);
    }

  }

  /**
   * The Class JCasGenThrower.
   */
  public static class JCasGenThrower implements IError {

    /** The log levels. */
    private Level logLevels[] = { Level.INFO, Level.WARNING, Level.SEVERE };

    /** The m message. */
    private String m_message = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.uima.jcas.jcasgen_gen.IError#newError(int, java.lang.String)
     */
    @Override
    public void newError(int severity, String message, Exception ex) {
      Logger log = UIMAFramework.getLogger();
      log.log(logLevels[severity], "JCasGen: " + message); //$NON-NLS-1$
      System.out.println(Messages.getString("MultiPageEditor.JCasGenErr") //$NON-NLS-1$
              + message);
      if (null != ex) {
        ex.printStackTrace();
      }
      if (IError.WARN < severity) {
        m_message = message;
        throw new Jg.ErrorExit();
      }
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
      return m_message;
    }
  }

  /**
   * Gets the fade color.
   *
   * @return the fade color
   */
  public Color getFadeColor() {
    if (null == fadeColor) {
      // COLOR_WIDGET_DARK_SHADOW is the same as black on SUSE KDE
      fadeColor = getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
    }
    return fadeColor;
  }

  // **********************
  // * Merged type system
  /**
   * Sets the merged type system description.
   *
   * @param saved
   *          the new merged type system description
   */
  // **********************
  public void setMergedTypeSystemDescription(TypeSystemDescription saved) {
    mergedTypeSystemDescription = saved;
  }

  /**
   * Sets the imported type system description.
   *
   * @param saved
   *          the new imported type system description
   */
  public void setImportedTypeSystemDescription(TypeSystemDescription saved) {
    importedTypeSystemDescription = saved;
  }

  /**
   * Sets the imported type system description.
   *
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void setImportedTypeSystemDescription() throws ResourceInitializationException {
    Collection tsdc = new ArrayList(1);
    TypeSystemDescription tsd = typeSystemDescription;
    if (null != tsd) {
      tsd = (TypeSystemDescription) tsd.clone();
      tsd.setTypes(typeDescriptionArray0);
    }
    tsdc.clear();
    tsdc.add(tsd);
    importedTypeSystemDescription = CasCreationUtils.mergeTypeSystems(tsdc,
            createResourceManager());
  }

  /**
   * Sets the merged type system description.
   *
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  public void setMergedTypeSystemDescription() throws ResourceInitializationException {
    mergedTypesAddingFeatures.clear();
    if (isAggregate()) {
      mergedTypeSystemDescription = mergeDelegateAnalysisEngineTypeSystems(
              (AnalysisEngineDescription) aeDescription.clone(), createResourceManager(),
              mergedTypesAddingFeatures);
    } else {
      if (null == typeSystemDescription) {
        mergedTypeSystemDescription = null;
      } else {
        ResourceManager resourceManager = createResourceManager();
        Collection tsdc = new ArrayList(1);
        tsdc.add(typeSystemDescription.clone());
        // System.out.println("mergingTypeSystem 2"); //$NON-NLS-1$
        // long time = System.currentTimeMillis();
        mergedTypeSystemDescription = CasCreationUtils.mergeTypeSystems(tsdc, resourceManager,
                mergedTypesAddingFeatures);
        // System.out.println("Finished mergingTypeSystem 2; time= " + //$NON-NLS-1$
        // (System.currentTimeMillis() - time));
        setImportedTypeSystemDescription();
      }
    }
  }

  /**
   * Gets the merged type system description.
   *
   * @return the merged type system description
   */
  public TypeSystemDescription getMergedTypeSystemDescription() {
    return mergedTypeSystemDescription;
  }

  /**
   * Gets the imported type system desription.
   *
   * @return the imported type system desription
   */
  public TypeSystemDescription getImportedTypeSystemDesription() {
    return importedTypeSystemDescription;
  }

  /**
   * Sets the merged fs index collection.
   *
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  public void setMergedFsIndexCollection() throws ResourceInitializationException {
    mergedFsIndexCollection = mergeDelegateAnalysisEngineFsIndexCollections(
            (AnalysisEngineDescription) aeDescription.clone(), createResourceManager());
  }

  /**
   * Sets the merged fs index collection.
   *
   * @param saved
   *          the new merged fs index collection
   */
  public void setMergedFsIndexCollection(FsIndexCollection saved) {
    mergedFsIndexCollection = saved;
  }

  /**
   * Gets the merged fs index collection.
   *
   * @return the merged fs index collection
   */
  public FsIndexCollection getMergedFsIndexCollection() {
    return mergedFsIndexCollection;
  }

  /**
   * Sets the merged type priorities.
   *
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  // full merge - including locally defined and imported ones
  public void setMergedTypePriorities() throws ResourceInitializationException {

    mergedTypePriorities = mergeDelegateAnalysisEngineTypePriorities(
            (AnalysisEngineDescription) aeDescription.clone(), createResourceManager());
  }

  /**
   * Sets the merged type priorities.
   *
   * @param saved
   *          the new merged type priorities
   */
  public void setMergedTypePriorities(TypePriorities saved) {
    mergedTypePriorities = saved;
  }

  /**
   * Gets the merged type priorities.
   *
   * @return the merged type priorities
   */
  public TypePriorities getMergedTypePriorities() {
    return mergedTypePriorities;
  }

  /**
   * Sets the resolved flow controller declaration.
   *
   * @throws InvalidXMLException
   *           the invalid XML exception
   */
  public void setResolvedFlowControllerDeclaration() throws InvalidXMLException {
    FlowControllerDeclaration fcDecl = aeDescription.getFlowControllerDeclaration();
    if (null != fcDecl) {
      resolvedFlowControllerDeclaration = (FlowControllerDeclaration) fcDecl.clone();
      resolvedFlowControllerDeclaration.resolveImports(createResourceManager());
    } else {
      resolvedFlowControllerDeclaration = null;
    }
  }

  /**
   * Gets the resolved flow controller declaration.
   *
   * @return the resolved flow controller declaration
   */
  public FlowControllerDeclaration getResolvedFlowControllerDeclaration() {
    return resolvedFlowControllerDeclaration;
  }

  /**
   * A Merge method doesn't "fit". merging isn't done over aggregates for these. Instead, the
   * outer-most one "wins".
   * 
   * But: resolving does fit. So we name this differently
   * 
   * @throws InvalidXMLException
   *           -
   */
  public void setResolvedExternalResourcesAndBindings() throws InvalidXMLException {
    AnalysisEngineDescription clonedAe = (AnalysisEngineDescription) aeDescription.clone();
    ResourceManagerConfiguration rmc = clonedAe.getResourceManagerConfiguration();
    if (null != rmc) {
      rmc.resolveImports(createResourceManager());
    }
    resolvedExternalResourcesAndBindings = rmc;
  }

  /**
   * Sets the resolved external resources and bindings.
   *
   * @param saved
   *          the new resolved external resources and bindings
   */
  public void setResolvedExternalResourcesAndBindings(ResourceManagerConfiguration saved) {
    resolvedExternalResourcesAndBindings = saved;
  }

  /**
   * Gets the resolved external resources and bindings.
   *
   * @return the resolved external resources and bindings
   */
  public ResourceManagerConfiguration getResolvedExternalResourcesAndBindings() {
    return resolvedExternalResourcesAndBindings;
  }

  /**
   * Sets the imported fs index collection.
   *
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void setImportedFsIndexCollection() throws ResourceInitializationException {
    AnalysisEngineDescription localAe = (AnalysisEngineDescription) aeDescription.clone();
    localAe.getAnalysisEngineMetaData().setFsIndexCollection(null);
    importedFsIndexCollection = CasCreationUtils
            .mergeDelegateAnalysisEngineFsIndexCollections(localAe, createResourceManager());
  }

  /**
   * Gets the imported fs index collection.
   *
   * @return the imported fs index collection
   */
  public FsIndexCollection getImportedFsIndexCollection() {
    return importedFsIndexCollection;
  }

  // this is all the type priorities, except those locally defined
  // used to distinguish between locally defined and imported ones
  /**
   * Sets the imported type priorities.
   *
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  // (only locally defined ones can be edited)
  private void setImportedTypePriorities() throws ResourceInitializationException {
    AnalysisEngineDescription localAe = (AnalysisEngineDescription) aeDescription.clone();
    localAe.getAnalysisEngineMetaData().setTypePriorities(null);
    importedTypePriorities = mergeDelegateAnalysisEngineTypePriorities(localAe,
            createResourceManager());
  }

  /**
   * Gets the imported type priorities.
   *
   * @return the imported type priorities
   */
  public TypePriorities getImportedTypePriorities() {
    return importedTypePriorities;
  }

  // private void setImportedExternalResourcesAndBindings() throws ResourceInitializationException {
  // ResourceManagerConfiguration_impl rmc = ((ResourceManagerConfiguration_impl)
  // aeDescription.getResourceManagerConfiguration());
  // if (null != rmc) {
  // rmc = (ResourceManagerConfiguration_impl) rmc.clone();
  // rmc.setExternalResourceBindings(null);
  // rmc.setExternalResources(null);
  // try {
  // rmc.resolveImports(createResourceManager());
  // } catch (InvalidXMLException e) {
  // throw new ResourceInitializationException(e);
  // }
  // }
  // importedExternalResourcesAndBindings = rmc;
  // }

  // public ResourceManagerConfiguration getImportedExternalResourcesAndBindings() {
  // return importedExternalResourcesAndBindings;
  // }

  /**
   * Gets the source page editor.
   *
   * @return the source page editor
   */
  public ITextEditor getSourcePageEditor() {
    if (getCurrentPage() == sourceIndex) {
      return sourceTextEditor;
    } else {
      return null;
    }
  }

  /** The java project. */
  private IJavaProject javaProject = null;

  /**
   * Gets the java project.
   *
   * @return the java project
   */
  public IJavaProject getJavaProject() {
    if (null == javaProject && null != file) {
      javaProject = JavaCore.create(file.getProject());
    }
    return javaProject;
  }

  /**
   * Gets the type from project.
   *
   * @param typename
   *          the typename
   * @return the type from project
   */
  public IType getTypeFromProject(String typename) {
    IJavaProject jp = getJavaProject();
    if (null != jp)
      try {
        return jp.findType(typename);
      } catch (JavaModelException e) {
        Utility.popMessage("Unexpected Exception",
                MessageFormat.format(
                        "Unexpected exception while getting type information for type ''{0}''. {1}",
                        new Object[] { typename, getMessagesToRootCause(e) }),
                MessageDialog.ERROR);
        throw new InternalErrorCDE("unexpected exception", e);
      }
    return null;
  }

  /** The analysis component I type. */
  private IType analysisComponentIType = null;

  /** The base annotator I type. */
  private IType baseAnnotatorIType = null;

  /** The collection reader I type. */
  private IType collectionReaderIType = null;

  /** The cas initializer I type. */
  private IType casInitializerIType = null;

  /** The cas consumer I type. */
  private IType casConsumerIType = null;

  /** The flow controller I type. */
  private IType flowControllerIType = null;

  /**
   * Gets the analysis component I type.
   *
   * @return the analysis component I type
   */
  public IType getAnalysisComponentIType() {
    if (null == analysisComponentIType) {
      analysisComponentIType = getTypeFromProject(
              "org.apache.uima.analysis_component.AnalysisComponent");
    }
    return analysisComponentIType;
  }

  /**
   * Gets the base annotator I type.
   *
   * @return the base annotator I type
   */
  public IType getBaseAnnotatorIType() {
    if (null == baseAnnotatorIType) {
      baseAnnotatorIType = getTypeFromProject(
              "org.apache.uima.analysis_engine.annotator.BaseAnnotator");
    }
    return baseAnnotatorIType;
  }

  /**
   * Gets the collection reader I type.
   *
   * @return the collection reader I type
   */
  public IType getCollectionReaderIType() {
    if (null == collectionReaderIType) {
      collectionReaderIType = getTypeFromProject("org.apache.uima.collection.CollectionReader");
    }
    return collectionReaderIType;
  }

  /**
   * Gets the cas initializer I type.
   *
   * @return the cas initializer I type
   */
  public IType getCasInitializerIType() {
    if (null == casInitializerIType) {
      casInitializerIType = getTypeFromProject("org.apache.uima.collection.CasInitializer");
    }
    return casInitializerIType;
  }

  /**
   * Gets the cas consumer I type.
   *
   * @return the cas consumer I type
   */
  public IType getCasConsumerIType() {
    if (null == casConsumerIType) {
      casConsumerIType = getTypeFromProject("org.apache.uima.collection.CasConsumer");
    }
    return casConsumerIType;
  }

  /**
   * Gets the flow controller I type.
   *
   * @return the flow controller I type
   */
  public IType getFlowControllerIType() {
    if (null == flowControllerIType) {
      flowControllerIType = getTypeFromProject("org.apache.uima.flow.FlowController");
    }
    return flowControllerIType;
  }

  /**
   * The Class CombinedHierarchyScope.
   */
  private static class CombinedHierarchyScope implements IJavaSearchScope {

    /** The sub scopes. */
    private IJavaSearchScope[] subScopes = new IJavaSearchScope[5];

    /** The nbr scopes. */
    private int nbrScopes = 0;

    /**
     * Gets the scopes.
     *
     * @return the scopes
     */
    public IJavaSearchScope[] getScopes() {
      return subScopes;
    }

    /**
     * Adds the scope.
     *
     * @param newScope
     *          the new scope
     */
    public void addScope(IJavaSearchScope newScope) {
      subScopes[nbrScopes++] = newScope;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.core.search.IJavaSearchScope#encloses(java.lang.String)
     */
    @Override
    public boolean encloses(String resourcePath) {
      for (int i = 0; i < nbrScopes; i++) {
        if (subScopes[i].encloses(resourcePath)) {
          return true;
        }
      }
      if (!resourcePath.startsWith("C:\\p\\j")) {
        System.out.println(MessageFormat.format(" FALSE encloses resourcepath: ''{0}''",
                new Object[] { resourcePath }));
      }
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.core.search.IJavaSearchScope#encloses(org.eclipse.jdt.core.IJavaElement)
     */
    @Override
    public boolean encloses(IJavaElement element) {

      for (int i = 0; i < nbrScopes; i++) {
        if (subScopes[i].encloses(element)) {
          return true;
        }
      }
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.core.search.IJavaSearchScope#enclosingProjectsAndJars()
     */
    @Override
    public IPath[] enclosingProjectsAndJars() {
      ArrayList result = new ArrayList(10);
      for (int i = 0; i < nbrScopes; i++) {
        IPath[] pjs = subScopes[i].enclosingProjectsAndJars();
        if (null != pjs) {
          for (int j = 0; j < pjs.length; j++) {
            if (!result.contains(pjs[j])) {
              result.add(pjs[j]);
            }
          }
        }
      }
      return (IPath[]) result.toArray(new IPath[result.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.core.search.IJavaSearchScope#includesBinaries()
     */
    @Override
    public boolean includesBinaries() {
      // TODO Auto-generated method stub
      return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.core.search.IJavaSearchScope#includesClasspaths()
     */
    @Override
    public boolean includesClasspaths() {
      // TODO Auto-generated method stub
      return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.core.search.IJavaSearchScope#setIncludesBinaries(boolean)
     */
    @Override
    public void setIncludesBinaries(boolean includesBinaries) {
      // implements interface method
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.core.search.IJavaSearchScope#setIncludesClasspaths(boolean)
     */
    @Override
    public void setIncludesClasspaths(boolean includesClasspaths) {
      // implements interface method
    }
  }

  /**
   * Gets the search scope for descriptor type.
   *
   * @return the search scope for descriptor type
   */
  public IJavaSearchScope getSearchScopeForDescriptorType() {
    try {
      switch (descriptorType) {
        case DESCRIPTOR_AE:
          CombinedHierarchyScope scope = new CombinedHierarchyScope();
          scope.addScope(SearchEngine.createHierarchyScope(getAnalysisComponentIType()));
          scope.addScope(SearchEngine.createHierarchyScope(getBaseAnnotatorIType()));
          scope.addScope(SearchEngine.createHierarchyScope(getCollectionReaderIType()));
          scope.addScope(SearchEngine.createHierarchyScope(getCasConsumerIType()));
          return scope;
        case DESCRIPTOR_CASCONSUMER:
          return SearchEngine.createHierarchyScope(getCasConsumerIType());
        case DESCRIPTOR_CASINITIALIZER:
          return SearchEngine.createHierarchyScope(getCasInitializerIType());
        case DESCRIPTOR_COLLECTIONREADER:
          return SearchEngine.createHierarchyScope(getCollectionReaderIType());
        case DESCRIPTOR_FLOWCONTROLLER:
          return SearchEngine.createHierarchyScope(getFlowControllerIType());
      }
    } catch (JavaModelException e) {
      throw new InternalErrorCDE("unexpected exception", e);
    }
    return null;
  }

  /**
   * Merge delegate analysis engine type systems.
   *
   * @param aAeDescription
   *          the a ae description
   * @param aResourceManager
   *          the a resource manager
   * @param aOutputMergedTypes
   *          the a output merged types
   * @return the type system description
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private TypeSystemDescription mergeDelegateAnalysisEngineTypeSystems(
          AnalysisEngineDescription aAeDescription, ResourceManager aResourceManager,
          Map aOutputMergedTypes) throws ResourceInitializationException {

    getMergeInput(aAeDescription, aResourceManager);
    return CasCreationUtils.mergeTypeSystems(typeSystemsToMerge, aResourceManager,
            aOutputMergedTypes);
  }

  /**
   * Merge delegate analysis engine type priorities.
   *
   * @param aAeDescription
   *          the a ae description
   * @param aResourceManager
   *          the a resource manager
   * @return the type priorities
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private TypePriorities mergeDelegateAnalysisEngineTypePriorities(
          AnalysisEngineDescription aAeDescription, ResourceManager aResourceManager)
          throws ResourceInitializationException {

    getMergeInput(aAeDescription, aResourceManager);
    return CasCreationUtils.mergeTypePriorities(typePrioritiesToMerge, aResourceManager);
  }

  /**
   * Merge delegate analysis engine fs index collections.
   *
   * @param aAeDescription
   *          the a ae description
   * @param aResourceManager
   *          the a resource manager
   * @return the fs index collection
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private FsIndexCollection mergeDelegateAnalysisEngineFsIndexCollections(
          AnalysisEngineDescription aAeDescription, ResourceManager aResourceManager)
          throws ResourceInitializationException {

    getMergeInput(aAeDescription, aResourceManager);
    return CasCreationUtils.mergeFsIndexes(fsIndexesToMerge, aResourceManager);
  }

  /**
   * Creates the cas.
   *
   * @param aAeDescription
   *          the a ae description
   * @param aPerformanceTuningSettings
   *          the a performance tuning settings
   * @param aResourceManager
   *          the a resource manager
   * @return the cas
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  public CAS createCas(AnalysisEngineDescription aAeDescription,
          Properties aPerformanceTuningSettings, ResourceManager aResourceManager)
          throws ResourceInitializationException {

    getMergeInput(aAeDescription, aResourceManager);

    // merge
    TypeSystemDescription aggTypeDesc = CasCreationUtils.mergeTypeSystems(typeSystemsToMerge,
            aResourceManager);
    TypePriorities aggTypePriorities = CasCreationUtils.mergeTypePriorities(typePrioritiesToMerge,
            aResourceManager);
    FsIndexCollection aggIndexColl = CasCreationUtils.mergeFsIndexes(fsIndexesToMerge,
            aResourceManager);

    return CasCreationUtils.createCas(aggTypeDesc, aggTypePriorities, aggIndexColl.getFsIndexes(),
            aPerformanceTuningSettings, aResourceManager);
  }

  /**
   * Gets the merge input.
   *
   * @param aAggregateDescription
   *          the a aggregate description
   * @param aResourceManager
   *          the a resource manager
   * @return the merge input
   * @throws ResourceInitializationException
   *           the resource initialization exception
   */
  private void getMergeInput(AnalysisEngineDescription aAggregateDescription,
          ResourceManager aResourceManager) throws ResourceInitializationException {

    // expand the aggregate AE description into the individual delegates
    ArrayList l = new ArrayList();
    l.add(aAggregateDescription);
    List mdList = CasCreationUtils.getMetaDataList(l, aResourceManager, failedRemotes);

    maybeShowRemoteFailure();

    // extract type systems and merge
    typeSystemsToMerge = new ArrayList();
    typePrioritiesToMerge = new ArrayList();
    fsIndexesToMerge = new ArrayList();
    Iterator it = mdList.iterator();
    while (it.hasNext()) {
      ProcessingResourceMetaData md = (ProcessingResourceMetaData) it.next();
      if (md.getTypeSystem() != null) {
        typeSystemsToMerge.add(md.getTypeSystem());
      }
      if (md.getTypePriorities() != null) {
        typePrioritiesToMerge.add(md.getTypePriorities());
      }
      if (md.getFsIndexCollection() != null) {
        fsIndexesToMerge.add(md.getFsIndexCollection());
      }
    }
  }

  /**
   * Maybe show remote failure.
   */
  private void maybeShowRemoteFailure() {
    if (failedRemotes.size() == 0) {
      return;
    }

    List names = new ArrayList();
    List exceptions = new ArrayList();

    for (Iterator it = failedRemotes.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry) it.next();
      String component = (String) entry.getKey();

      if (failedRemotesAlreadyKnown.contains(component)) {
        continue;
      }

      failedRemotesAlreadyKnown.add(component);
      names.add(component);
      exceptions.add(entry.getValue());
    }

    if (names.size() == 0) {
      return;
    }

    StringBuffer sb = new StringBuffer(100);
    for (int i = 0; i < names.size(); i++) {
      sb.append("Component key-name(s): ").append(names.get(i)).append(": ")
              .append(getMessagesToRootCause((Exception) exceptions.get(i)))
              .append("\n---------------\n");
    }

    Utility.popMessage("Remotes Unavailable", "Note: This message is only shown once.\n\n"
            + "Some Remote components (see error message below) could not be accessed.\n"
            + "This is not an error; perhaps the remote components are not currently running.\n\n"
            + "WARNING: The Types, Type Priorities, and Indexes created by \"merging\"\n"
            + "information from the imported remote types may be incomplete\n"
            + "(because the editor can't read this information at the moment).\n"
            + "However, this doesn't affect the editing operations; you can continue\n"
            + "(but with perhaps less complete error checking in the editor,\n"
            + "and JCasGen, if used, may be missing some type information that\n"
            + "would have come from the remote components, had they been available.\n\n" + sb,
            MessageDialog.WARNING);
  }

}
