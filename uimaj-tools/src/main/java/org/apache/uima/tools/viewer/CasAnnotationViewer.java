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

package org.apache.uima.tools.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.Scrollable;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.BooleanArrayFSImpl;
import org.apache.uima.cas.impl.ByteArrayFSImpl;
import org.apache.uima.cas.impl.DoubleArrayFSImpl;
import org.apache.uima.cas.impl.FloatArrayFSImpl;
import org.apache.uima.cas.impl.IntArrayFSImpl;
import org.apache.uima.cas.impl.LongArrayFSImpl;
import org.apache.uima.cas.impl.ShortArrayFSImpl;
import org.apache.uima.cas.impl.StringArrayFSImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * A Swing component that displays annotations in a text pane with highlighting. There is also a
 * tree view for display details of annotations on which the user clicks. This class extends
 * {@link JPanel} and so can be reused within any Swing application.
 * <p>
 * To launch the viewer, call the {@link #setCAS(CAS)} method with the CAS to be viewed.
 * <p>
 * The viewer is configurable via the following methods:
 * <ul>
 * <li>{@link #setConsistentColors(boolean)} - if set to true (default), the color assigned to any
 * annotation type will be the same across all documents. If set to false, colors may vary across
 * documents.</li>
 * <li>{@link #setDisplayedTypes(String[])} - specifies a set of types that will be highlighted in
 * the viewer's text pane.</li>
 * <li>{@link #setHiddenTypes(String[])} - specifies a set of types that will NOT be highlighted in
 * the viewer's text pane.</li>
 * <li>{@link #setHiddenFeatures(String[])} - specifies a set of features that will never shown in
 * the viewer's annotation details tree.</li>
 * <li>{@link #setHighFrequencyTypes(String[])} - this can be used to specify a set of types that
 * occur frequently. These types will the be assigned the most distinguishable colors.</li>
 * <li>{@link #setInitiallySelectedTypes(String[])} - this can be used to specify a set of types
 * that will initially be selected (i.e. have their checkboxes checked) in the viewer. The default
 * is for all types to be initially selected.</li>
 * <li>{@link #setRightToLeftTextOrientation(boolean)} - switches the text pane from left-to-right
 * (default) to right-to-left mode. This is needed to support languages such as Arabic and Hebrew,
 * which are read from right to left.</li>
 * </ul>
 */
public class CasAnnotationViewer extends JPanel implements ActionListener, MouseListener,
        TreeWillExpandListener, TreeExpansionListener, ItemListener {
  private static final long serialVersionUID = 3559118488371946999L;

  // Mode constants
  private static final short MODE_ANNOTATIONS = 0;

  private static final short MODE_ENTITIES = 1;

  private ArrayList userTypes = null;

  /**
   * @return Returns the userTypes.
   */
  public ArrayList getUserTypes() {
    return userTypes;
  }

  /**
   * @param userTypes
   *          The userTypes to set.
   */
  public void setUserTypes(ArrayList userTypes) {
    this.userTypes = userTypes;
  }

  // colors to use for highlighting annotations
  // (use high brightness for best contrast against black text)
  private static final float BRIGHT = 0.95f;

  private static final Color[] COLORS = new Color[] {
      // low saturation colors are best, so put them first
      Color.getHSBColor(55f / 360, 0.25f, BRIGHT), // butter yellow?
      Color.getHSBColor(0f / 360, 0.25f, BRIGHT), // pink?
      Color.getHSBColor(210f / 360, 0.25f, BRIGHT), // baby blue?
      Color.getHSBColor(120f / 360, 0.25f, BRIGHT), // mint green?
      Color.getHSBColor(290f / 360, 0.25f, BRIGHT), // lavender?
      Color.getHSBColor(30f / 360, 0.25f, BRIGHT), // tangerine?
      Color.getHSBColor(80f / 360, 0.25f, BRIGHT), // celery green?
      Color.getHSBColor(330f / 360, 0.25f, BRIGHT), // light coral?
      Color.getHSBColor(160f / 360, 0.25f, BRIGHT), // aqua?
      Color.getHSBColor(250f / 360, 0.25f, BRIGHT), // light violet?
      // higher saturation colors
      Color.getHSBColor(55f / 360, 0.5f, BRIGHT), Color.getHSBColor(0f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(210f / 360, 0.5f, BRIGHT), Color.getHSBColor(120f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(290f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(30f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(80f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(330f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(160f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(250f / 360, 0.5f, BRIGHT),
      // even higher saturation colors
      Color.getHSBColor(55f / 360, 0.75f, BRIGHT), Color.getHSBColor(0f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(210f / 360, 0.75f, BRIGHT), Color.getHSBColor(120f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(290f / 360, 0.75f, BRIGHT), Color.getHSBColor(30f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(80f / 360, 0.75f, BRIGHT), Color.getHSBColor(330f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(160f / 360, 0.75f, BRIGHT), Color.getHSBColor(250f / 360, 0.75f, BRIGHT) };

  private static String[] DEFAULT_HIDDEN_FEATURES = { "sofa" };

  private Map mTypeNameToColorMap = new HashMap();

  private HashSet noCheckSet = new HashSet();

  private List mHighFrequencyTypes = new ArrayList();

  private Set mDisplayedTypeNames = null;

  private Set mHiddenTypeNames = new HashSet();

  private Set mInitiallySelectedTypeNames = null;

  private Map mTypeToCheckboxMap = new HashMap();

  private Map mEntityToCheckboxMap = new HashMap();

  private CAS mCAS;

  private Type mStringType;

  private Type mFsArrayType;

  private boolean mConsistentColors = true;

  private Set mHiddenFeatureNames = new HashSet();

  private boolean mEntityViewEnabled = false; 

  private short mViewMode = MODE_ANNOTATIONS;

  private boolean mHideUnselectedCheckboxes = false;

  // GUI components
  private JSplitPane horizSplitPane;

  private JSplitPane vertSplitPane;

  private JScrollPane textScrollPane;

  private JTextPane textPane;

  private JPanel legendPanel;

  private JLabel legendLabel;

  private JScrollPane legendScrollPane;

  private JPanel annotationCheckboxPanel;

  private JPanel entityCheckboxPanel;

  private JPanel buttonPanel;

  private JButton selectAllButton;

  private JButton deselectAllButton;

  private JButton showHideUnselectedButton;

  private JTree selectedAnnotationTree;

  private DefaultTreeModel selectedAnnotationTreeModel;

  private JPanel viewModePanel;

  private JRadioButton annotationModeButton;

  private JRadioButton entityModeButton;

  private String[] mBoldfaceKeywords = new String[0];

  private int[] mBoldfaceSpans = new int[0];

  private JPanel sofaSelectionPanel;

  private JComboBox sofaSelectionComboBox;

  private EntityResolver mEntityResolver = new DefaultEntityResolver();

  /**
   * Creates a CAS Annotation Viewer.
   */
  public CasAnnotationViewer() {
    // create a horizonal JSplitPane
    horizSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    horizSplitPane.setResizeWeight(0.6);
    this.setLayout(new BorderLayout());
    this.add(horizSplitPane);

    // create a vertical JSplitPane and add to left of horizSplitPane
    vertSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    vertSplitPane.setResizeWeight(0.8);
    vertSplitPane.setPreferredSize(new Dimension(620, 600));
    vertSplitPane.setMinimumSize(new Dimension(200, 200));
    horizSplitPane.setLeftComponent(vertSplitPane);

    // add JTextPane to top of vertical split pane
    textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setPreferredSize(new Dimension(620, 400));
    textPane.setMinimumSize(new Dimension(200, 100));
    textScrollPane = new JScrollPane(textPane);
    vertSplitPane.setTopComponent(textScrollPane);

    // bottom pane is the legend, with checkboxes
    legendPanel = new JPanel();
    legendPanel.setPreferredSize(new Dimension(620, 200));
    legendPanel.setLayout(new BorderLayout());
    legendLabel = new JLabel("Legend");
    legendPanel.add(legendLabel, BorderLayout.NORTH);
    // checkboxes are contained in a scroll pane
    legendScrollPane = new JScrollPane();
    legendScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    legendPanel.add(legendScrollPane, BorderLayout.CENTER);
    // there are two checkbox panels - one for annotations, one for entities
    annotationCheckboxPanel = new VerticallyScrollablePanel();
    annotationCheckboxPanel.setLayout(new GridLayout(0, 5));
    entityCheckboxPanel = new VerticallyScrollablePanel();
    entityCheckboxPanel.setLayout(new GridLayout(0, 4));
    // add annotation panel first, since that is the default
    legendScrollPane.setViewportView(annotationCheckboxPanel);

    // at very bottom is the Button Panel, with Select All, Deselect All,
    // and Hide/Show Unselected Buttons. It also may show the sofa-selection
    // combo box and/or the Viewer Mode radio group; either are both may
    // be hidden.
    buttonPanel = new JPanel();

    selectAllButton = new JButton("Select All");
    selectAllButton.addActionListener(this);
    buttonPanel.add(selectAllButton);
    deselectAllButton = new JButton("Deselect All");
    deselectAllButton.addActionListener(this);
    buttonPanel.add(deselectAllButton);
    showHideUnselectedButton = new JButton("Hide Unselected");
    showHideUnselectedButton.addActionListener(this);
    buttonPanel.add(showHideUnselectedButton);

    sofaSelectionPanel = new JPanel();
    JLabel sofaSelectionLabel = new JLabel("Sofa:");
    sofaSelectionPanel.add(sofaSelectionLabel);
    sofaSelectionComboBox = new JComboBox();
    sofaSelectionPanel.add(sofaSelectionComboBox);
    sofaSelectionComboBox.addItemListener(this);
    buttonPanel.add(sofaSelectionPanel);

    viewModePanel = new JPanel();
    viewModePanel.add(new JLabel("Mode: "));
    annotationModeButton = new JRadioButton("Annotations");
    annotationModeButton.setSelected(true);
    annotationModeButton.addActionListener(this);
    viewModePanel.add(annotationModeButton);
    entityModeButton = new JRadioButton("Entities");
    entityModeButton.addActionListener(this);
    viewModePanel.add(entityModeButton);
    ButtonGroup group = new ButtonGroup();
    group.add(annotationModeButton);
    group.add(entityModeButton);
    buttonPanel.add(viewModePanel);
    viewModePanel.setVisible(false);
    this.add(buttonPanel, BorderLayout.SOUTH);

    textPane.setMinimumSize(new Dimension(200, 100));
    vertSplitPane.setBottomComponent(legendPanel);

    // right pane has a JTree
    selectedAnnotationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Annotations"));
    selectedAnnotationTree = new JTree(selectedAnnotationTreeModel) {
      private static final long serialVersionUID = -7882967150283952907L;

      public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(230, 500);
      }
    };
    selectedAnnotationTree.setMinimumSize(new Dimension(50, 100));
    selectedAnnotationTree.setScrollsOnExpand(true);
    selectedAnnotationTree.setRootVisible(true);
    selectedAnnotationTree.setCellRenderer(new AnnotationTreeCellRenderer());
    selectedAnnotationTree.addTreeWillExpandListener(this);
    selectedAnnotationTree.addTreeExpansionListener(this);
    JPanel treePanel = new JPanel();
    treePanel.setLayout(new BorderLayout());
    treePanel.add(new JLabel("Click In Text to See Annotation Detail"), BorderLayout.NORTH);

    treePanel.add(new JScrollPane(selectedAnnotationTree), BorderLayout.CENTER);
    horizSplitPane.setRightComponent(treePanel);

    // add mouse listener to update annotation tree
    textPane.addMouseListener(this);

    // initialize hidden feature names map
    mHiddenFeatureNames.addAll(Arrays.asList(DEFAULT_HIDDEN_FEATURES));
  }

  /**
   * @deprecated use the zero-argument constructor and call {@link #setEntityViewEnabled(boolean)}
   */
  public CasAnnotationViewer(boolean aEntityViewEnabled) {
    this();
  }

  /**
   * Set the list of types that occur most frequently. This method assigns the most distinguishable
   * colors to these types.
   * 
   * @param aTypeNames
   *          names of types that are occur frequently. Ideally these should be ordered by
   *          frequency, with the most frequent being first.
   */
  public void setHighFrequencyTypes(String[] aTypeNames) {
    // store these types for later
    mHighFrequencyTypes.clear();
    mHighFrequencyTypes.addAll(Arrays.asList(aTypeNames));
    mTypeNameToColorMap.clear();
    assignColors(mHighFrequencyTypes);
  }

  /**
   * Set the list of types that will be highlighted in the viewer. Types not in this list will not
   * appear in the legend and will never be highlighted. If this method is not called, the default
   * is to show all types in the CAS (except those specifically hidden by a call to
   * {@link #setHiddenTypes(String[])}.
   * 
   * @param aTypeNames
   *          names of types that are to be highlighted. Null indicates that all types in the CAS
   *          should be highlighted.
   */
  public void setDisplayedTypes(String[] aDisplayedTypeNames) {
    if (aDisplayedTypeNames == null) {
      mDisplayedTypeNames = null;
    } else {
      mDisplayedTypeNames = new HashSet();
      mDisplayedTypeNames.addAll(Arrays.asList(aDisplayedTypeNames));
    }
  }

  /**
   * Set the list of types that will NOT be highlighted in the viewer.
   * 
   * @param aTypeNames
   *          names of types that are never to be highlighted.
   */
  public void setHiddenTypes(String[] aTypeNames) {
    mHiddenTypeNames.clear();
    mHiddenTypeNames.addAll(Arrays.asList(aTypeNames));
  }

  /**
   * Configures the initially selected types in the viewer. If not called, all types will be
   * initially selected.
   * 
   * @param aTypeNames
   *          array of fully-qualified names of types to be initially selected
   */
  public void setInitiallySelectedTypes(String[] aTypeNames) {
    mInitiallySelectedTypeNames = new HashSet();
    for (int i = 0; i < aTypeNames.length; i++) {
      mInitiallySelectedTypeNames.add(aTypeNames[i]);
    }
    // apply to existing checkboxes
    Iterator iterator = mTypeToCheckboxMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry entry = (Map.Entry) iterator.next();
      String type = ((Type) entry.getKey()).getName();
      JCheckBox checkbox = (JCheckBox) entry.getValue();
      checkbox.setSelected(typeNamesContains(mInitiallySelectedTypeNames, type));
    }

    // redisplay (if we have a CAS) - this allows this method to be called
    // either before or after displaying the viewer
    if (mCAS != null) {
      display();
    }
  }

  /**
   * Configures the viewer to hide certain features in the annotation deatail pane.
   * 
   * @param aFeatureName
   *          array of (short) feature names to be hidden
   */
  public void setHiddenFeatures(String[] aFeatureNames) {
    mHiddenFeatureNames.clear();
    // add default hidden features
    mHiddenFeatureNames.addAll(Arrays.asList(DEFAULT_HIDDEN_FEATURES));
    // add user-defined hidden features
    mHiddenFeatureNames.addAll(Arrays.asList(aFeatureNames));
  }

  /**
   * Configures whether the viewer will allow the user to switch to "Entity" view, which highlight
   * entities rather than annotations.  Entity mode is typically only useful if the
   * {@link #setEntityResolver(EntityResolver)} method has been called with a user-supplied
   * class that can determine which annotations refer to the same entity.
   * 
   * @param aDisplayEntities
   *          true to enable entity viewing mode, false to allow annotation viewing only.
   *          The default is false.
   */
  public void setEntityViewEnabled(boolean aEnabled) {
    mEntityViewEnabled = aEnabled;
    this.viewModePanel.setVisible(aEnabled);
  }
  
  /**
   * Sets the {@link EntityResolver} to use when the viewer is in entity mode.
   * Entity mode must be turned on using the {@link #setEntityViewEnabled(boolean)} method.
   * @param aEntityResolver user-supplied class that can determine which annotations correspond
   *   to the same entity.
   */
  public void setEntityResolver(EntityResolver aEntityResolver) {
    mEntityResolver = aEntityResolver;
  }

  /**
   * Sets whether colors will be consistent in all documents viewed using this viewer. If set to
   * true, assignments of color to annotation type will persist across documents; if false, colors
   * will be reassigned in each new document. (Note that if high frequency types are set via
   * {@link #setHighFrequencyTypes(String[])}, the colors for those types will always be
   * consistent, regardless of the value passed to this method.
   * 
   * @param aConsistent
   *          true (the default) causes colors to be consistent across documents, false allows them
   *          to vary
   */
  public void setConsistentColors(boolean aConsistent) {
    mConsistentColors = aConsistent;
  }

  /**
   * Sets the text orientation. The default is left-to-right, but needs to be set to right-to-left
   * to properly display some languages, most notably Arabic and Hebrew.
   * 
   * @param aRightToLeft
   *          true to put the viewer in right-to-left mode, false for left-to-right (the default).
   */
  public void setRightToLeftTextOrientation(boolean aRightToLeft) {
    textPane.applyComponentOrientation(aRightToLeft ? ComponentOrientation.RIGHT_TO_LEFT
            : ComponentOrientation.LEFT_TO_RIGHT);
  }

  /**
   * Sets whether unselected (unchecked) checkboxes will be hidden entirely from the legend. This
   * mode makes for a cleaner legend at the expense of making it more difficult to toggle which
   * types are selected. There's also a button in the GUI that lets the user change this setting.
   * 
   * @param aConsistent
   *          true (the default) causes colors to be consistent across documents, false allows them
   *          to vary
   */
  public void setHideUnselectedCheckboxes(boolean aHideUnselected) {
    mHideUnselectedCheckboxes = aHideUnselected;
    display();
  }

  /**
   * Sets the CAS to be viewed. This must be called before {@link #display()}.
   * 
   * @param aCAS
   *          the CSA to be viewed
   */
  public void setCAS(CAS aCAS) {
    mCAS = aCAS;
    mStringType = mCAS.getTypeSystem().getType(CAS.TYPE_NAME_STRING);
    mFsArrayType = mCAS.getTypeSystem().getType(CAS.TYPE_NAME_FS_ARRAY);
    // clear checkbox panel so it will be repopulated
    annotationCheckboxPanel.removeAll();
    entityCheckboxPanel.removeAll();
    mTypeToCheckboxMap.clear();
    mEntityToCheckboxMap.clear();
    // clear selected annotation details tree
    this.updateSelectedAnnotationTree(-1);

    // clear type to color map if color consistency is off
    if (!mConsistentColors) {
      mTypeNameToColorMap.clear();
      // but reassign colors to high frequency types
      assignColors(mHighFrequencyTypes);
    }

    // clear boldface
    mBoldfaceKeywords = new String[0];
    mBoldfaceSpans = new int[0];

    // enable or disable entity view depending on user's choice 
    this.viewModePanel.setVisible(mEntityViewEnabled);

    // Populate sofa combo box with the names of all text Sofas in the CAS
    sofaSelectionComboBox.removeAllItems();
    Iterator sofas = aCAS.getSofaIterator();
    Feature sofaIdFeat = aCAS.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
    boolean nonDefaultSofaFound = false;
    while (sofas.hasNext()) {
      SofaFS sofa = (SofaFS) sofas.next();
      if (sofa.getLocalStringData() != null) {
        String sofaId = sofa.getStringValue(sofaIdFeat);
        if (CAS.NAME_DEFAULT_SOFA.equals(sofaId)) {
          sofaId = "DEFAULT"; // make nicer display
        } else {
          nonDefaultSofaFound = true;
        }
        sofaSelectionComboBox.addItem(sofaId);
        // if this sofa matches the view passed to this method, select it
        CAS viewOfSofa = aCAS.getView(sofa);
        if (viewOfSofa == aCAS) {
          sofaSelectionComboBox.setSelectedIndex(sofaSelectionComboBox.getItemCount() - 1);
        }
      }
    }
    if (sofaSelectionComboBox.getItemCount() == 0) {
      throw new RuntimeException("This CAS contains no document to view.");
    }
    // make sofa selector visible if any text sofa other than the default was found
    sofaSelectionPanel.setVisible(nonDefaultSofaFound);

    // Note that selection of the Sofa from the combo box happens during
    // population, and that triggers the call to display() to display
    // that document and its annotations/entities.
    // display();
  }

  /**
   * Causes the specified words to appear in boldface wherever they occur in the document. This is
   * case-insensitive. Call this method after {@link #setCAS()}. It wil apply only to the current
   * document, and will be reset on the next call to {@link #setCAS()}.
   * 
   * @param aWords
   *          array of words to highlight in boldface.
   */
  public void applyBoldfaceToKeywords(String[] aWords) {
    mBoldfaceKeywords = aWords;
    doBoldface();
  }

  /**
   * Causes the specified spans to appear in boldface. This is case-insensitive. Call this method
   * after {@link #setCAS()}. It wil apply only to the current document, and will be reset on the
   * next call to {@link #setCAS()}.
   * 
   * @param aSpans
   *          spans to appear in boldface (begin1, end1, begin2, end2, ...)
   */
  public void applyBoldfaceToSpans(int[] aSpans) {
    mBoldfaceSpans = aSpans;
    doBoldface();
  }

  /**
   * Configures the viewer appropriately for displaying a hit against an XML fragments query. This
   * does not use a sophisticated algorithm for determining the location of the document that
   * matched the query. Currently all it does is call {@link #setInitiallySelectedTypes(String[])}
   * with the list of types mentioned in the query and {@link #applyBoldfaceToKeyword(String[])} on
   * any keywords mentioned in the query.
   * 
   * @param aQuery
   *          an XML fragments query
   * @param aTypeNamespace
   *          namespace to prepend to the element names in the query in order to form
   *          fully-qualified CAS type names. This is optional; if not specified, type namespaces
   *          are ignored and any type whose local name matches the query will be selected.
   */
  public void configureViewForXmlFragmentsQuery(String aQuery, String aTypeNamespace) {
    // need to parse query and produce type list and keyword list
    List typeList = new ArrayList();
    List keywordList = new ArrayList();

    String delims = "<>+-*\" \t\n";
    StringTokenizer tokenizer = new StringTokenizer(aQuery, delims, true);
    boolean inTag = false;
    while (tokenizer.hasMoreTokens()) {
      String tok = tokenizer.nextToken();
      if ("<".equals(tok)) {
        inTag = true;
      } else if (">".equals(tok) && inTag) {
        inTag = false;
      } else if (delims.indexOf(tok) == -1) // token is not a delimiter
      {
        if (inTag) {
          if (!tok.startsWith("/")) // ignore end tags
          {
            if (tok.endsWith("/")) {
              tok = tok.substring(0, tok.length() - 1); // strip trailing / from empty tags
            }
            typeList.add(aTypeNamespace + '.' + tok);
          }
        } else {
          keywordList.add(tok);
        }
      }
    }

    // System.out.println(typeList);
    // System.out.println(keywordList);

    setInitiallySelectedTypes((String[]) typeList.toArray(new String[0]));
    display();
    applyBoldfaceToKeywords((String[]) keywordList.toArray(new String[0]));
  }

  /**
   * Configures the viewer appropriately for displaying a hit against an XML fragments query. This
   * does not use a sophisticated algorithm for determining the location of the document that
   * matched the query. Currently all it does is call {@link #setInitiallySelectedTypes(String[])}
   * with the list of types mentioned in the query and {@link #applyBoldfaceToKeyword(String[])} on
   * any keywords mentioned in the query.
   * 
   * @param aQuery
   *          an XML fragments query
   */
  public void configureViewForXmlFragmentsQuery(String aQuery) {
    configureViewForXmlFragmentsQuery(aQuery, "*");
  }

  /**
   * Assign initially checked to the specified types, pairing up down the lists
   * 
   * @param aNotChecked
   *          list of types not to be initially checked JMP
   */
  public void assignCheckedFromList(ArrayList aNotChecked) {
    Iterator iterC = aNotChecked.iterator();
    while (iterC.hasNext()) {
      String typeName = (String) iterC.next();
      // assign to list of types not to be initially checked
      noCheckSet.add(typeName);
    }
  }

  /**
   * Assign colors to the specified types, pairing up down the lists
   * 
   * @param aColors
   *          list of colors
   * @param aTypeNames
   *          list of type names JMP
   */
  public void assignColorsFromList(List aColors, ArrayList aTypeNames) {
    // populate mTypeNameToColorMap
    Iterator iter = aTypeNames.iterator();
    Iterator iterC = aColors.iterator();
    while (iter.hasNext()) {
      if (!iterC.hasNext())
        break;
      String typeName = (String) iter.next();
      Color color = (Color) iterC.next();
      // assign background color
      mTypeNameToColorMap.put(typeName, color);
    }

    setUserTypes(aTypeNames);

    // clear checkbox panel so it will be refreshed
    annotationCheckboxPanel.removeAll();
    mTypeToCheckboxMap.clear();
  }

  /**
   * Assign colors to the specified types
   * 
   * @param aTypeNames
   *          list of type names
   */
  private void assignColors(List aTypeNames) {
    // populate mTypeNameToColorMap
    Iterator iter = aTypeNames.iterator();
    while (iter.hasNext()) {
      String typeName = (String) iter.next();
      // assign background color
      Color c = COLORS[mTypeNameToColorMap.size() % COLORS.length];
      mTypeNameToColorMap.put(typeName, c);
    }

    // clear checkbox panel so it will be refreshed
    annotationCheckboxPanel.removeAll();
    mTypeToCheckboxMap.clear();
  }

  /**
   * Creates/updates the display. This is called when setCAS() is called and again each time to
   * user's mode or checkbox selections change.
   */
  private void display() {
    // remember split pane divider location so we can restore it later
    int dividerLoc = vertSplitPane.getDividerLocation();

    // remember caret pos and scroll position
    int caretPos = this.textPane.getCaretPosition();
    int verticalScrollPos = this.textScrollPane.getVerticalScrollBar().getValue();

    // type of display depends on whether we are in annotation or entity mode
    switch (mViewMode) {
      case MODE_ANNOTATIONS:
        displayAnnotations();
        break;
      case MODE_ENTITIES:
        displayEntities();
        break;
    }

    // apply boldface to keywords and spans as indicated by user
    doBoldface();

    // update the label of the Show/Hide Unselected Button
    if (mHideUnselectedCheckboxes) {
      showHideUnselectedButton.setText("Show Unselected");
    } else {
      showHideUnselectedButton.setText("Hide Unselected");
    }

    // reset scroll position
    textPane.setCaretPosition(caretPos);
    textScrollPane.getVerticalScrollBar().setValue(verticalScrollPos);
    textScrollPane.revalidate();

    // reset split pane divider
    vertSplitPane.setDividerLocation(dividerLoc);
  }

  /**
   * Creates the annotation display.
   */
  private void displayAnnotations() {
    // for speed, detach document from text pane before updating
    StyledDocument doc = (StyledDocument) textPane.getDocument();
    Document blank = new DefaultStyledDocument();
    textPane.setDocument(blank);

    // make sure annotationCheckboxPanel is showing
    if (legendScrollPane.getViewport().getView() != annotationCheckboxPanel) {
      legendScrollPane.setViewportView(annotationCheckboxPanel);
    }

    // add text from CAS
    try {
      doc.remove(0, doc.getLength());
      doc.insertString(0, mCAS.getDocumentText(), new SimpleAttributeSet());
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }

    // Iterate over annotations
    FSIterator iter = mCAS.getAnnotationIndex().iterator();
    Hashtable checkBoxes = new Hashtable();
    HashSet checkBoxesDone = new HashSet();
    while (iter.isValid()) {
      AnnotationFS fs = (AnnotationFS) iter.get();
      iter.moveToNext();

      Type type = fs.getType();

      // have we seen this type before?
      JCheckBox checkbox = (JCheckBox) mTypeToCheckboxMap.get(type);
      if (checkbox == null) {
        // check that type should be displayed
        if ((mDisplayedTypeNames == null || typeNamesContains(mDisplayedTypeNames, type.getName()))
                && !typeNamesContains(mHiddenTypeNames, type.getName())) {
          // if mTypeNameToColorMap exists, get color from there
          Color c = (Color) mTypeNameToColorMap.get(type.getName());
          if (c == null) // assign next available color
          {
            c = COLORS[mTypeNameToColorMap.size() % COLORS.length];
            mTypeNameToColorMap.put(type.getName(), c);
          }
          // This next section required until priorities work properly
          // HashSet noCheckSet = new HashSet();
          String noCheckArray[] = {
          // "org.apache.jresporator.PROPER",
          // "DOCSTRUCT_ANNOT_TYPE",
          // "VOCAB_ANNOT_TYPE"
          };
          for (int i = 0; i < noCheckArray.length; i++) {
            noCheckSet.add(noCheckArray[i]);
          }
          // end of section

          // should type be initially selected?
          boolean selected = ((mInitiallySelectedTypeNames == null &&
          // document annotation is not initially selected in default case
                  !CAS.TYPE_NAME_DOCUMENT_ANNOTATION.equals(type.getName()) && !noCheckSet
                  .contains(type.getName()) // priorities JMP
          ) || (mInitiallySelectedTypeNames != null && typeNamesContains(
                  mInitiallySelectedTypeNames, type.getName())));

          // add checkbox
          checkbox = new JCheckBox(type.getShortName(), selected);
          checkbox.setToolTipText(type.getName());
          checkbox.addActionListener(this);
          checkbox.setBackground(c);
          // annotationCheckboxPanel.add(checkbox); do it later JMP
          checkBoxes.put(type.getName(), checkbox);
          checkBoxesDone.add(checkbox);
          // add to (Type, Checkbox) map
          mTypeToCheckboxMap.put(type, checkbox);
        } else {
          // this type is not hidden, skip it
          continue;
        }
      }
      // if checkbox is checked, assign color to text
      if (checkbox.isSelected()) {
        int begin = fs.getBegin();
        int end = fs.getEnd();

        // Be careful of 0-length annotations and annotations that span the
        // entire document. In either of these cases, if we try to set
        // background color, it will set the input text style, which is not
        // what we want.
        if (begin == 0 && end == mCAS.getDocumentText().length()) {
          end--;
        }

        if (begin < end) {
          MutableAttributeSet attrs = new SimpleAttributeSet();
          StyleConstants.setBackground(attrs, checkbox.getBackground());
          doc.setCharacterAttributes(begin, end - begin, attrs, false);
        }
      }
    }

    // now populate panel with checkboxes in order specified in user file. JMP
    ArrayList aTypeNames = getUserTypes();
    if (aTypeNames != null) {
      Iterator iterT = aTypeNames.iterator();
      while (iterT.hasNext()) {
        String typeName = (String) iterT.next();
        JCheckBox cb = (JCheckBox) checkBoxes.get(typeName);
        if (cb != null) {
          annotationCheckboxPanel.add(cb);
          checkBoxesDone.remove(cb);
        }
      }
    }
    // add additional checkboxes in alphabetical order
    LinkedList checkboxes = new LinkedList(checkBoxesDone);
    Collections.sort(checkboxes, new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((JCheckBox) o1).getText().toLowerCase().compareTo(
                ((JCheckBox) o2).getText().toLowerCase());
      }
    });
    Iterator iterC = checkboxes.iterator();
    while (iterC.hasNext()) {
      JCheckBox cb = (JCheckBox) iterC.next();
      annotationCheckboxPanel.add(cb);
    }

    // add/remove checkboxes from display as determined by the
    // mHideUnselectedCheckboxes toggle
    Iterator cbIter = mTypeToCheckboxMap.values().iterator();
    while (cbIter.hasNext()) {
      JCheckBox cb = (JCheckBox) cbIter.next();
      if (mHideUnselectedCheckboxes && !cb.isSelected()) {
        if (cb.getParent() == annotationCheckboxPanel) {
          annotationCheckboxPanel.remove(cb);
        }
      } else if (cb.getParent() != annotationCheckboxPanel) {
        annotationCheckboxPanel.add(cb);
      }
    }

    // reattach document to text pane
    textPane.setDocument(doc);
  }

  /**
   * Creates the entity display.
   */
  private void displayEntities() {
    // for speed, detach document from text pane before updating
    StyledDocument doc = (StyledDocument) textPane.getDocument();
    Document blank = new DefaultStyledDocument();
    textPane.setDocument(blank);

    // make sure entityCheckboxPanel is showing
    if (legendScrollPane.getViewport().getView() != entityCheckboxPanel) {
      legendScrollPane.setViewportView(entityCheckboxPanel);
    }

    // add text from CAS
    try {
      doc.remove(0, doc.getLength());
      doc.insertString(0, mCAS.getDocumentText(), new SimpleAttributeSet());
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }

    // Iterate over EntityAnnotations using JCAS, because the EntityResolver interface
    // uses JCAS as a convenience to the user.
    JCas jcas;
    try {
      // NOTE: for a large type system, this can take a few seconds, which results in a
      // noticeable delay when the user first switches to Entity mode.
      jcas = mCAS.getJCas();
    } catch (CASException e) {
      throw new RuntimeException(e);
    }
    FSIterator iter = jcas.getAnnotationIndex().iterator();
    while (iter.isValid()) {
      Annotation annot = (Annotation) iter.get();
      iter.moveToNext();

      // find out what entity this annotation represents
      EntityResolver.Entity entity = mEntityResolver.getEntity(annot);

      //if not an entity, skip it
      if (entity == null)
        continue;
      
      // have we seen this entity before?
      JCheckBox checkbox = (JCheckBox) mEntityToCheckboxMap.get(entity);
      if (checkbox == null) {
        // assign next available color
        Color c = COLORS[mEntityToCheckboxMap.size() % COLORS.length];
        // add checkbox
        checkbox = new JCheckBox(entity.getCanonicalForm(), true);
        checkbox.setToolTipText(entity.getCanonicalForm());
        checkbox.addActionListener(this);
        checkbox.setBackground(c);
        entityCheckboxPanel.add(checkbox);
        // add to (Entity, Checkbox) map
        mEntityToCheckboxMap.put(entity, checkbox);
      }

      // if checkbox is checked, assign color to text
      if (checkbox.isSelected()) {
        int begin = annot.getBegin();
        int end = annot.getEnd();
        // be careful of 0-length annotation. If we try to set background color when there
        // is no selection, it will set the input text style, which is not what we want.
        if (begin != end) {
          MutableAttributeSet attrs = new SimpleAttributeSet();
          StyleConstants.setBackground(attrs, checkbox.getBackground());
          doc.setCharacterAttributes(begin, end - begin, attrs, false);
        }
      }
    }

    // add/remove checkboxes from display as determined by the
    // mHideUnselectedCheckboxes toggle
    Iterator cbIter = mEntityToCheckboxMap.values().iterator();
    while (cbIter.hasNext()) {
      JCheckBox cb = (JCheckBox) cbIter.next();
      if (mHideUnselectedCheckboxes && !cb.isSelected()) {
        if (cb.getParent() == entityCheckboxPanel) {
          entityCheckboxPanel.remove(cb);
        }
      } else if (cb.getParent() != entityCheckboxPanel) {
        entityCheckboxPanel.add(cb);
      }
    }

    // reattach document to text pane
    textPane.setDocument(doc);
  }

  /**
   * Refreshes the selected annotation tree.
   * 
   * @param aPosition
   *          the currently selected offset into the document. All annotations overlapping this
   *          point will be rendered in the tree.
   */
  private void updateSelectedAnnotationTree(int aPosition) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.selectedAnnotationTreeModel
            .getRoot();
    root.removeAllChildren();
    FSIterator annotIter = this.mCAS.getAnnotationIndex().iterator();
    while (annotIter.isValid()) {
      AnnotationFS annot = (AnnotationFS) annotIter.get();
      // if (getPanePosition(annot.getBegin()) <= aPosition
      // && getPanePosition(annot.getEnd()) > aPosition)
      if (annot.getBegin() <= aPosition && annot.getEnd() > aPosition) {
        JCheckBox checkbox = (JCheckBox) mTypeToCheckboxMap.get(annot.getType());
        if (checkbox != null && checkbox.isSelected()) {
          addAnnotationToTree(annot);
        }
      }
      // else if (getPanePosition(annot.getBegin()) > aPosition)
      else if (annot.getBegin() > aPosition)
        break;
      annotIter.moveToNext();
    }
    this.selectedAnnotationTreeModel.nodeStructureChanged(root);
    // expand first level
    // int row = 0;
    // while (row < this.selectedAnnotationTree.getRowCount())
    // {
    // if (this.selectedAnnotationTree.getPathForRow(row).getPathCount() <= 2)
    // {
    // this.selectedAnnotationTree.expandRow(row);
    // }
    // row++;
    // }

    // hmmm.. how to get scroll pane to resize properly??
    this.selectedAnnotationTree.treeDidChange();
    // this.selectedAnnotationTree.setPreferredSize(this.selectedAnnotationTree.getSize());
    this.selectedAnnotationTree.revalidate();
    this.horizSplitPane.revalidate();
  }

  /**
   * Adds an annotation to the selected annotations tree. Annotations in the tree are grouped by
   * type.
   * 
   * @param aAnnotation
   *          the annotation to add
   */
  protected void addAnnotationToTree(AnnotationFS aAnnotation) {
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.selectedAnnotationTreeModel
            .getRoot();
    // try to find a node for the type
    DefaultMutableTreeNode typeNode = null;
    Enumeration typeNodes = root.children();
    while (typeNodes.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) typeNodes.nextElement();
      if (aAnnotation.getType().equals(((TypeTreeNodeObject) node.getUserObject()).getType())) {
        typeNode = node;
        break;
      }
    }
    if (typeNode == null) {
      typeNode = new DefaultMutableTreeNode(new TypeTreeNodeObject(aAnnotation.getType()));
      root.insert(typeNode, 0);
    }

    // add annotation node
    DefaultMutableTreeNode annotationNode = new DefaultMutableTreeNode(new FsTreeNodeObject(
            aAnnotation, null));
    typeNode.insert(annotationNode, 0);
    // add child nodes for features
    addFeatureTreeNodes(annotationNode, aAnnotation);
  }

  private void addFeatureTreeNodes(DefaultMutableTreeNode aParentNode, FeatureStructure aFS) {
    List aFeatures = aFS.getType().getFeatures();
    Iterator iter = aFeatures.iterator();
    while (iter.hasNext()) {
      Feature feat = (Feature) iter.next();
      String featName = feat.getShortName();
      // skip hidden features
      if (mHiddenFeatureNames.contains(featName)) {
        continue;
      }
      // how we get feature value depends on feature's range type)
      String featVal = "null";
      Type rangeType = feat.getRange();
      String rangeTypeName = rangeType.getName();
      if (mCAS.getTypeSystem().subsumes(mStringType, rangeType)) {
        featVal = aFS.getStringValue(feat);
        if (featVal == null) {
          featVal = "null";
        } else if (featVal.length() > 64) {
          featVal = featVal.substring(0, 64) + "...";
        }
      } else if (rangeType.isPrimitive()) {
        featVal = aFS.getFeatureValueAsString(feat);
      } else if (mCAS.getTypeSystem().subsumes(mFsArrayType, rangeType)) {
        ArrayFS arrayFS = (ArrayFS) aFS.getFeatureValue(feat);
        if (arrayFS != null) {
          // Add featName = FSArray node, then add each array element as a child
          DefaultMutableTreeNode arrayNode = new DefaultMutableTreeNode(featName + " = FSArray");
          for (int i = 0; i < arrayFS.size(); i++) {
            FeatureStructure fsVal = arrayFS.get(i);
            if (fsVal != null) {
              // Add the FS node and a dummy child, so that user can expand it.
              // When user expands it, new nodes for feature values will be created.
              DefaultMutableTreeNode fsValNode = new DefaultMutableTreeNode(new FsTreeNodeObject(
                      fsVal, featName));
              if (!fsVal.getType().getFeatures().isEmpty()) {
                fsValNode.add(new DefaultMutableTreeNode(null));
              }
              arrayNode.add(fsValNode);
            } else {
              arrayNode.add(new DefaultMutableTreeNode("null"));
            }
          }
          aParentNode.add(arrayNode);
          continue;
        }
      } else if (rangeType.isArray()) // primitive array
      {
        String[] vals = null;
        if (CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName)) {
          StringArrayFSImpl arrayFS = (StringArrayFSImpl) aFS.getFeatureValue(feat);
          if (arrayFS != null)
            vals = arrayFS.toArray();
        } else if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName)) {
          IntArrayFSImpl arrayFS = (IntArrayFSImpl) aFS.getFeatureValue(feat);
          if (arrayFS != null)
            vals = arrayFS.toStringArray();
        } else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName)) {
          FloatArrayFSImpl arrayFS = (FloatArrayFSImpl) aFS.getFeatureValue(feat);
          if (arrayFS != null)
            vals = arrayFS.toStringArray();
        } else if (CAS.TYPE_NAME_BOOLEAN_ARRAY.equals(rangeTypeName)) {
          BooleanArrayFSImpl arrayFS = (BooleanArrayFSImpl) aFS.getFeatureValue(feat);
          if (arrayFS != null)
            vals = arrayFS.toStringArray();
        } else if (CAS.TYPE_NAME_BYTE_ARRAY.equals(rangeTypeName)) {
          ByteArrayFSImpl arrayFS = (ByteArrayFSImpl) aFS.getFeatureValue(feat);
          if (arrayFS != null)
            vals = arrayFS.toStringArray();
        } else if (CAS.TYPE_NAME_SHORT_ARRAY.equals(rangeTypeName)) {
          ShortArrayFSImpl arrayFS = (ShortArrayFSImpl) aFS.getFeatureValue(feat);
          if (arrayFS != null)
            vals = arrayFS.toStringArray();
        } else if (CAS.TYPE_NAME_LONG_ARRAY.equals(rangeTypeName)) {
          LongArrayFSImpl arrayFS = (LongArrayFSImpl) aFS.getFeatureValue(feat);
          if (arrayFS != null)
            vals = arrayFS.toStringArray();
        }
        if (CAS.TYPE_NAME_DOUBLE_ARRAY.equals(rangeTypeName)) {
          DoubleArrayFSImpl arrayFS = (DoubleArrayFSImpl) aFS.getFeatureValue(feat);
          if (arrayFS != null)
            vals = arrayFS.toStringArray();
        }
        if (vals == null) {
          featVal = "null";
        } else {
          StringBuffer displayVal = new StringBuffer();
          displayVal.append('[');
          for (int i = 0; i < vals.length - 1; i++) {
            displayVal.append(vals[i]);
            displayVal.append(',');
          }
          if (vals.length > 0) {
            displayVal.append(vals[vals.length - 1]);
          }
          displayVal.append(']');
          featVal = displayVal.toString();
        }
      } else
      // single feature value
      {
        FeatureStructure fsVal = aFS.getFeatureValue(feat);
        if (fsVal != null) {
          // Add the FS node and a dummy child, so that user can expand it.
          // When user expands it, new nodes for feature values will be created.
          DefaultMutableTreeNode fsValNode = new DefaultMutableTreeNode(new FsTreeNodeObject(fsVal,
                  featName));
          if (!fsVal.getType().getFeatures().isEmpty()) {
            fsValNode.add(new DefaultMutableTreeNode(null));
          }
          aParentNode.add(fsValNode);
          continue;
        }
      }
      aParentNode.add(new DefaultMutableTreeNode(featName + " = " + featVal));
    }
  }

  /**
   * Does wildcard matching to determine if a give type name is "contained" in a Set of type names.
   * 
   * @param names
   *          Type names, which may include wildcards (e.g, uima.tt.*)
   * @param name
   *          A type name
   * @return True iff name matches a name in type names
   */
  private boolean typeNamesContains(Set names, String name) {
    if (names.contains(name))
      return true;
    else {
      Iterator namesIterator = names.iterator();
      while (namesIterator.hasNext()) {
        String otherName = (String) namesIterator.next();
        if (otherName.indexOf('*') != -1) {
          if (wildCardMatch(name, otherName)) {
            return true;
          }
        } else {
          if (otherName.equalsIgnoreCase(name)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Helper for {@link #typeNamesContains(HashSet, String)}.
   * 
   * @param s
   *          A litteral string
   * @param pattern
   *          A string that includes one or more *'s as wildcards
   * @return True iff the string matches the pattern.
   */
  private boolean wildCardMatch(String s, String pattern) {
    StringBuffer regexpPatternBuffer = new StringBuffer();
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == '*')
        regexpPatternBuffer.append('.');
      else if (c == '.')
        regexpPatternBuffer.append('\\');
      if (Character.isLetter(c)) {
        regexpPatternBuffer.append('(').append(Character.toLowerCase(c)).append('|').append(
                Character.toUpperCase(c)).append(')');
      } else {
        regexpPatternBuffer.append(c);
      }
    }

    return s.matches(new String(regexpPatternBuffer));
  }

  /**
   * @see java.awt.Component#setSize(Dimension)
   */
  public void setSize(Dimension d) {
    super.setSize(d);
    Insets insets = getInsets();
    Dimension paneSize = new Dimension(d.width - insets.left - insets.right, d.height - insets.top
            - insets.bottom);

    horizSplitPane.setPreferredSize(paneSize);
    horizSplitPane.setSize(paneSize);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == selectAllButton) {
      Iterator cbIter = (mViewMode == MODE_ANNOTATIONS) ? mTypeToCheckboxMap.values().iterator()
              : mEntityToCheckboxMap.values().iterator();
      while (cbIter.hasNext()) {
        ((JCheckBox) cbIter.next()).setSelected(true);
      }
      display();
    } else if (e.getSource() == deselectAllButton) {
      Iterator cbIter = (mViewMode == MODE_ANNOTATIONS) ? mTypeToCheckboxMap.values().iterator()
              : mEntityToCheckboxMap.values().iterator();
      while (cbIter.hasNext()) {
        ((JCheckBox) cbIter.next()).setSelected(false);
      }
      display();
    } else if (e.getSource() == annotationModeButton) {
      mViewMode = MODE_ANNOTATIONS;
      display();
    } else if (e.getSource() == entityModeButton) {
      mViewMode = MODE_ENTITIES;
      display();
      // make sure we clear the annotation tree when we go into entity mode
      // this.updateSelectedAnnotationTree(0);
    } else if (e.getSource() == showHideUnselectedButton) {
      mHideUnselectedCheckboxes = !mHideUnselectedCheckboxes;
      display();
    } else if (e.getSource() instanceof JCheckBox) {
      display();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
   */
  public void mouseClicked(MouseEvent e) {
    if (mViewMode == MODE_ANNOTATIONS) {
      int pos = textPane.viewToModel(e.getPoint());
      this.updateSelectedAnnotationTree(pos);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
   */
  public void mouseEntered(MouseEvent e) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
   */
  public void mouseExited(MouseEvent e) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
   */
  public void mousePressed(MouseEvent e) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
   */
  public void mouseReleased(MouseEvent e) {
  }

  /**
   * Inner class containing data for a tree node representing a FeatureStructure
   */
  static class FsTreeNodeObject {
    public FsTreeNodeObject(FeatureStructure aFS, String aFeatureName) {
      mFS = aFS;
      mFeatureName = aFeatureName;
      mCaption = mFS.getType().getShortName();
      if (mFS instanceof AnnotationFS) {
        String coveredText = ((AnnotationFS) mFS).getCoveredText();
        if (coveredText.length() > 64)
          coveredText = coveredText.substring(0, 64) + "...";
        mCaption += " (\"" + coveredText + "\")";
      }
      if (mFeatureName != null) {
        mCaption = mFeatureName + " = " + mCaption;
      }
    }

    public FeatureStructure getFS() {
      return mFS;
    }

    public String toString() {
      return mCaption;
    }

    private FeatureStructure mFS;

    private String mFeatureName;

    private String mCaption;
  }

  /**
   * Inner class containing data for a tree node representing a Type
   */
  static class TypeTreeNodeObject {
    public TypeTreeNodeObject(Type aType) {
      mType = aType;
    }

    public Type getType() {
      return mType;
    }

    public String toString() {
      return mType.getShortName();
    }

    private Type mType;
  }

  class AnnotationTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = -8661556785397184756L;

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree,
     *      java.lang.Object, boolean, boolean, boolean, int, boolean)
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
            boolean expanded, boolean leaf, int row, boolean aHasFocus) {

      // set background color if this is an Annotation or a Type
      Color background = null;
      if (value instanceof DefaultMutableTreeNode) {
        Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
        Type type = null;
        if (userObj instanceof FsTreeNodeObject) {
          FeatureStructure fs = ((FsTreeNodeObject) userObj).getFS();
          type = fs.getType();
        } else if (userObj instanceof TypeTreeNodeObject) {
          type = ((TypeTreeNodeObject) userObj).getType();
        }
        if (type != null) {
          // look up checkbox to get color
          JCheckBox checkbox = (JCheckBox) mTypeToCheckboxMap.get(type);
          if (checkbox != null) {
            background = checkbox.getBackground();
          }
        }
      }
      this.setBackgroundNonSelectionColor(background);
      this.setBackgroundSelectionColor(background);

      Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
              row, aHasFocus);
      return component;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.TreeWillExpandListener#treeWillCollapse(javax.swing.event.TreeExpansionEvent)
   */
  public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.TreeWillExpandListener#treeWillExpand(javax.swing.event.TreeExpansionEvent)
   */
  public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
    // if FS node is expanded and it has a dummy child, replace with
    // feature value nodes (this is what lets us do infinite tree)
    Object lastPathComponent = event.getPath().getLastPathComponent();
    if (lastPathComponent instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode expandedNode = (DefaultMutableTreeNode) lastPathComponent;
      Object userObj = expandedNode.getUserObject();
      if (userObj instanceof FsTreeNodeObject) {
        TreeNode firstChild = expandedNode.getFirstChild();
        if (firstChild instanceof DefaultMutableTreeNode
                && ((DefaultMutableTreeNode) firstChild).getUserObject() == null) {
          expandedNode.removeAllChildren();
          FeatureStructure fs = ((FsTreeNodeObject) userObj).getFS();
          addFeatureTreeNodes(expandedNode, fs);
          ((JTree) event.getSource()).treeDidChange();
        }

      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.TreeExpansionListener#treeCollapsed(javax.swing.event.TreeExpansionEvent)
   */
  public void treeCollapsed(TreeExpansionEvent event) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.TreeExpansionListener#treeExpanded(javax.swing.event.TreeExpansionEvent)
   */
  public void treeExpanded(TreeExpansionEvent event) {
    // if a Type node is expanded and has only one child,
    // also expand this child (a usability improvement)
    Object lastPathComponent = event.getPath().getLastPathComponent();
    if (lastPathComponent instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode expandedNode = (DefaultMutableTreeNode) lastPathComponent;
      Object userObj = expandedNode.getUserObject();
      if (userObj instanceof TypeTreeNodeObject && expandedNode.getChildCount() == 1) {
        TreePath childPath = event.getPath().pathByAddingChild(expandedNode.getFirstChild());
        ((JTree) event.getSource()).expandPath(childPath);
        ((JTree) event.getSource()).treeDidChange();
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
   */
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource() == sofaSelectionComboBox) {
      // a new sofa was selected. Switch to that view and update display
      String sofaId = (String) e.getItem();
      if ("DEFAULT".equals(sofaId)) {
        mCAS = mCAS.getView(CAS.NAME_DEFAULT_SOFA);
      } else {
        mCAS = mCAS.getView(sofaId);
      }
      display();
    }

  }

  /**
   * Applies boldface as per the mBoldfaceKeywords and mBoldfaceSpans fields.
   */
  private void doBoldface() {
    // Keywords
    if (mBoldfaceKeywords.length > 0) {
      // build regular expression
      StringBuffer regEx = new StringBuffer();
      for (int i = 0; i < mBoldfaceKeywords.length; i++) {
        if (i > 0) {
          regEx.append('|');
        }
        regEx.append("\\b");
        String word = mBoldfaceKeywords[i];
        for (int j = 0; j < word.length(); j++) {
          char c = word.charAt(j);
          if (Character.isLetter(c)) {
            regEx.append('[').append(Character.toLowerCase(c)).append(Character.toUpperCase(c))
                    .append(']');
          } else if (c == '.' || c == '^' || c == '&' || c == '\\' || c == '(' || c == ')') {
            regEx.append('\\').append(c);
          } else {
            regEx.append('c');
          }
        }
        regEx.append("\\b");
      }
      // System.out.println("RegEx: " + regEx);
      Pattern pattern = Pattern.compile(regEx.toString());
      Matcher matcher = pattern.matcher(mCAS.getDocumentText());
      // match
      int pos = 0;
      while (matcher.find(pos)) {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        StyledDocument doc = (StyledDocument) textPane.getDocument();
        doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), attrs, false);

        if (pos == matcher.end()) // infinite loop check
          break;
        else
          pos = matcher.end();
      }
    }
    // Spans
    int docLength = mCAS.getDocumentText().length();
    int len = mBoldfaceSpans.length;
    len -= len % 2; // to avoid ArrayIndexOutOfBoundsException if some numbskull passes in an
    // odd-length array
    int i = 0;
    while (i < len) {
      int begin = mBoldfaceSpans[i];
      int end = mBoldfaceSpans[i + 1];
      if (begin >= 0 && begin <= docLength && end >= 0 && end <= docLength) {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, true);
        StyledDocument doc = (StyledDocument) textPane.getDocument();
        doc.setCharacterAttributes(begin, end - begin, attrs, false);
      }
      i += 2;
    }
  }

  /**
   * Gets the selected annotation tree component.
   * 
   * @return the tree that displays annotation details about annotations overlapping the point where
   *         the user last clicked in the text.
   */
  protected JTree getSelectedAnnotationTree() {
    return this.selectedAnnotationTree;
  }

  /**
   * A panel that is to be placed in a JScrollPane that can only scroll vertically. This panel
   * should have its width track the viewport's width, and increase its height as necessary to
   * display all components.
   * 
   * 
   */
  static class VerticallyScrollablePanel extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1009744410018634511L;

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
     */
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 50;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getScrollableTracksViewportHeight()
     */
    public boolean getScrollableTracksViewportHeight() {
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getScrollableTracksViewportWidth()
     */
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 10;
    }

  }
  
  /**
   * Trivial entity resolver that's applied if the user turns on entity mode without
   * specifying their own entity resolver.  Returns the covered text as the canonical form,
   * and treats annotations with equal covered text as belonging to the same entity.
   */
  static class DefaultEntityResolver implements EntityResolver {

    /* (non-Javadoc)
     * @see org.apache.uima.tools.viewer.EntityResolver#getCanonicalForm(org.apache.uima.jcas.tcas.Annotation)
     */
    public Entity getEntity(final Annotation aAnnotation) {
      return new Entity() {
        
        public String getCanonicalForm() {          
          return aAnnotation.getCoveredText();
        }

        public boolean equals(Object obj) {
          if (obj instanceof Entity) {
            String canon = ((Entity)obj).getCanonicalForm();
            return getCanonicalForm().equals(canon);
          }
          return false;
        }

        public int hashCode() {          
          return getCanonicalForm().hashCode();
        }              
      };              
    }
    
  }
}
