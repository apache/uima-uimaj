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
import org.apache.uima.tools.viewer.EntityResolver.Entity;

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
public class CasAnnotationViewer extends JPanel {
  private static final long serialVersionUID = 3559118488371946999L;

  // Mode constants
  private static final short MODE_ANNOTATIONS = 0;
  private static final short MODE_ENTITIES = 1;

  private static String[] DEFAULT_HIDDEN_FEATURES = { "sofa" };
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
      Color.getHSBColor(55f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(0f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(210f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(120f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(290f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(30f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(80f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(330f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(160f / 360, 0.5f, BRIGHT),
      Color.getHSBColor(250f / 360, 0.5f, BRIGHT),
      // even higher saturation colors
      Color.getHSBColor(55f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(0f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(210f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(120f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(290f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(30f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(80f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(330f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(160f / 360, 0.75f, BRIGHT),
      Color.getHSBColor(250f / 360, 0.75f, BRIGHT) };

  private CAS cas;
  private Type stringType;
  private Type fsArrayType;
  private boolean useConsistentColors = true;
  private List<String> highFrequencyTypes = new ArrayList<String>();
  private String[] boldFaceKeyWords = new String[0];
  private int[] boldFaceSpans = new int[0];
  private Set<String> hiddenFeatureNames = new HashSet<String>();
  private Set<String> hiddenTypeNames = new HashSet<String>();
  private Set<String> displayedTypeNames = null;
  private Set<String> initiallySelectedTypeNames = null;
  private boolean hideUnselectedCheckboxes = false;
  private List<String> userTypes = null;
  private Set<String> typesNotChecked = new HashSet<String>();
  private Map<String, Color> typeColorMap = new HashMap<String, Color>();
  private EntityResolver mEntityResolver = new DefaultEntityResolver();

  private boolean entityViewEnabled = false; 
  private short viewMode = MODE_ANNOTATIONS;
  // GUI components
  private Map<Type, JCheckBox> typeToCheckBoxMap = new HashMap<Type, JCheckBox>();
  private Map<Entity, JCheckBox> entityToCheckBoxMap = new HashMap<Entity, JCheckBox>();
  private JSplitPane horizontalSplitPane;
  private JSplitPane verticalSplitPane;
  private JTextPane textPane;
  private JScrollPane textScrollPane;
  private JScrollPane legendScrollPane;
  private JPanel annotationCheckboxPanel;
  private JPanel entityCheckboxPanel;
  private JButton selectAllButton;
  private JButton deselectAllButton;
  private JButton showHideUnselectedButton;
  private JTree selectedAnnotationTree;
  private DefaultTreeModel selectedAnnotationTreeModel;
  private JPanel viewModePanel;
  private JRadioButton annotationModeButton;
  private JRadioButton entityModeButton;
  private JPanel sofaSelectionPanel;
  @SuppressWarnings("rawtypes")
  private JComboBox sofaSelectionComboBox;

  /**
   * Creates a CAS Annotation Viewer.
   */
  public CasAnnotationViewer() {
    this.setLayout(new BorderLayout());
    // create a horizonal JSplitPane
    this.createHorizontalSplitPane();
    this.add(this.horizontalSplitPane, BorderLayout.CENTER);
    // at very bottom is the Button Panel, with Select All, Deselect All,
    // and Hide/Show Unselected Buttons. It also may show the sofa-selection
    // combo box and/or the Viewer Mode radio group; either are both may
    // be hidden.
    this.add(this.createControlPanel(), BorderLayout.SOUTH);

    // initialize hidden feature names map
    this.hiddenFeatureNames.addAll(Arrays.asList(DEFAULT_HIDDEN_FEATURES));
  }

  private JPanel createControlPanel() {
    JPanel buttonPanel = new JPanel();
    this.createSelectAllButton();
    buttonPanel.add(this.selectAllButton);
    this.createDeselectAllButton();
    buttonPanel.add(this.deselectAllButton);
    this.createShowHidenUnselectedButton();
    buttonPanel.add(this.showHideUnselectedButton);
    this.createSofaSelectionPanel();
    buttonPanel.add(this.sofaSelectionPanel);
    this.createViewModePanel();
    buttonPanel.add(this.viewModePanel);

    return buttonPanel;
  }

  private void createViewModePanel() {
    this.viewModePanel = new JPanel();
    this.viewModePanel.add(new JLabel("Mode: "));
    this.createAnnotationModeButton();
    this.viewModePanel.add(this.annotationModeButton);
    this.createEntityModeButton();
    this.viewModePanel.add(this.entityModeButton);
    ButtonGroup group = new ButtonGroup();
    group.add(this.annotationModeButton);
    group.add(this.entityModeButton);
    this.viewModePanel.setVisible(false);
  }

  private void createEntityModeButton() {
    this.entityModeButton = new JRadioButton("Entities");
    this.entityModeButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
          viewMode = MODE_ENTITIES;
          display();
        }
    });
  }

  private void createAnnotationModeButton() {
    this.annotationModeButton = new JRadioButton("Annotations");
    this.annotationModeButton.setSelected(true);
    this.annotationModeButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
          viewMode = MODE_ANNOTATIONS;
          display();
		}
    });
  }

  private void createSofaSelectionPanel() {
    this.sofaSelectionPanel = new JPanel();
    JLabel sofaSelectionLabel = new JLabel("Sofa:");
    this.sofaSelectionPanel.add(sofaSelectionLabel);
    this.createSofaSelectionComboBox();
    this.sofaSelectionPanel.add(this.sofaSelectionComboBox);
  }

  @SuppressWarnings("rawtypes")
  private void createSofaSelectionComboBox() {
    this.sofaSelectionComboBox = new JComboBox();
    this.sofaSelectionComboBox.addItemListener(new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
          if (e.getSource() == sofaSelectionComboBox) {
            // a new sofa was selected. Switch to that view and update display
            String sofaId = (String) e.getItem();
            if ("DEFAULT".equals(sofaId)) {
              cas = cas.getView(CAS.NAME_DEFAULT_SOFA);
            } else {
              cas = cas.getView(sofaId);
            }
            display();
          }
        }
    });
  }

  private void createShowHidenUnselectedButton() {
    this.showHideUnselectedButton = new JButton("Hide Unselected");
    this.showHideUnselectedButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
          hideUnselectedCheckboxes = !hideUnselectedCheckboxes;
          display();
		}
    });
  }

  private void createDeselectAllButton() {
    this.deselectAllButton = new JButton("Deselect All");
    this.deselectAllButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
          Iterator<JCheckBox> cbIter = (viewMode == MODE_ANNOTATIONS) ? typeToCheckBoxMap.values().iterator()
              : entityToCheckBoxMap.values().iterator();
          while (cbIter.hasNext()) {
            cbIter.next().setSelected(false);
          }
          display();
        }
    });
  }

  private void createSelectAllButton() {
    this.selectAllButton = new JButton("Select All");
    this.selectAllButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
          Iterator<JCheckBox> cbIter = (viewMode == MODE_ANNOTATIONS) ? typeToCheckBoxMap.values().iterator()
		              : entityToCheckBoxMap.values().iterator();
          while (cbIter.hasNext()) {
            cbIter.next().setSelected(true);
          }
          display();
        }
    });
  }

  private void createHorizontalSplitPane() {
    this.horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	this.horizontalSplitPane.setResizeWeight(0.6);
    // create a vertical JSplitPane and add to left of horizSplitPane
	this.createVerticalSplitPane();
    this.horizontalSplitPane.setLeftComponent(this.verticalSplitPane);
    // right pane has a JTree
    this.horizontalSplitPane.setRightComponent(this.createTreePanel());
  }

  private JPanel createTreePanel() {
    JPanel treePanel = new JPanel();
    treePanel.setLayout(new BorderLayout());
    treePanel.add(new JLabel("Click In Text to See Annotation Detail"), BorderLayout.NORTH);
    this.createSelectedAnnotationTree();
    treePanel.add(new JScrollPane(this.selectedAnnotationTree), BorderLayout.CENTER);
	return treePanel;
  }

  private void createSelectedAnnotationTree() {
    this.selectedAnnotationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Annotations"));
    this.selectedAnnotationTree = new JTree(selectedAnnotationTreeModel) {
      private static final long serialVersionUID = -7882967150283952907L;

      public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(230, 500);
      }
    };
    this.selectedAnnotationTree.setMinimumSize(new Dimension(50, 100));
    this.selectedAnnotationTree.setScrollsOnExpand(true);
    this.selectedAnnotationTree.setRootVisible(true);
    this.selectedAnnotationTree.setCellRenderer(new AnnotationTreeCellRenderer());
    this.selectedAnnotationTree.addTreeWillExpandListener(new TreeWillExpandListener() {
		@Override
		public void treeWillExpand(TreeExpansionEvent event)
				throws ExpandVetoException {
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
                FeatureStructure fs = ((FsTreeNodeObject) userObj).getFeatureStructure();
                addFeatureTreeNodes(expandedNode, fs);
                ((JTree) event.getSource()).treeDidChange();
              }
            }
          }
		}

		@Override
		public void treeWillCollapse(TreeExpansionEvent event)
				throws ExpandVetoException {
		}    	
    });
    this.selectedAnnotationTree.addTreeExpansionListener(new TreeExpansionListener() {
		@Override
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

		@Override
		public void treeCollapsed(TreeExpansionEvent event) {
		}    	
    });
  }

  private void createVerticalSplitPane() {
    this.verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    this.verticalSplitPane.setResizeWeight(0.8);
    this.verticalSplitPane.setPreferredSize(new Dimension(620, 600));
    this.verticalSplitPane.setMinimumSize(new Dimension(200, 200));

    // add JTextPane to top of vertical split pane
    this.createTextScrollPane();
    this.verticalSplitPane.setTopComponent(this.textScrollPane);
    // bottom pane is the legend, with checkboxes
    this.verticalSplitPane.setBottomComponent(this.createLegendPanel());
  }

  private JPanel createLegendPanel() {
    JPanel legendPanel = new JPanel();
    legendPanel.setPreferredSize(new Dimension(620, 200));
    legendPanel.setLayout(new BorderLayout());
    JLabel legendLabel = new JLabel("Legend");
    legendPanel.add(legendLabel, BorderLayout.NORTH);

    // checkboxes are contained in a scroll pane
    this.createLegendScrollPane();
    legendPanel.add(this.legendScrollPane, BorderLayout.CENTER);

    return legendPanel;
  }

  private void createLegendScrollPane() {
    this.legendScrollPane = new JScrollPane();
    this.legendScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    // there are two checkbox panels - one for annotations, one for entities
    this.createAnnotationCheckBoxPanel();
    this.createEntityCheckBoxPanel();
    // add annotation panel first, since that is the default
    this.legendScrollPane.setViewportView(this.annotationCheckboxPanel);
  }

  private void createEntityCheckBoxPanel() {
    this.entityCheckboxPanel = new VerticallyScrollablePanel();
    this.entityCheckboxPanel.setLayout(new GridLayout(0, 4));
  }

  private void createAnnotationCheckBoxPanel() {
    this.annotationCheckboxPanel = new VerticallyScrollablePanel();
    this.annotationCheckboxPanel.setLayout(new GridLayout(0, 5));
  }

  private void createTextScrollPane() {
    this.textPane = new JTextPane();
    this.textPane.setEditable(false);
    this.textPane.setPreferredSize(new Dimension(620, 400));
    this.textPane.setMinimumSize(new Dimension(200, 100));
    // add mouse listener to update annotation tree
    this.textPane.addMouseListener(new MouseListener() {
		@Override
		public void mouseClicked(MouseEvent e) {
          if (viewMode == MODE_ANNOTATIONS) {
            int pos = textPane.viewToModel(e.getPoint());
            updateSelectedAnnotationTree(pos);
          }
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}    	
    });
    this.textScrollPane = new JScrollPane(this.textPane);
  }

  /**
   * @deprecated use the zero-argument constructor and call {@link #setEntityViewEnabled(boolean)}
   */
  @Deprecated
  public CasAnnotationViewer(boolean aEntityViewEnabled) {
    this();
  }
  
  /**
   * @return Returns the userTypes.
   */
  public List<String> getUserTypes() {
    return userTypes;
  }

  /**
   * @param userTypes
   *          The userTypes to set.
   */
  public void setUserTypes(List<String> userTypes) {
    this.userTypes = userTypes;
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
    highFrequencyTypes.clear();
    highFrequencyTypes.addAll(Arrays.asList(aTypeNames));
    typeColorMap.clear();
    assignColors(highFrequencyTypes);
  }

  /**
   * Set the list of types that will be highlighted in the viewer. Types not in this list will not
   * appear in the legend and will never be highlighted. If this method is not called, the default
   * is to show all types in the CAS (except those specifically hidden by a call to
   * {@link #setHiddenTypes(String[])}.
   * 
   * @param aDisplayedTypeNames
   *          names of types that are to be highlighted. Null indicates that all types in the CAS
   *          should be highlighted.
   */
  public void setDisplayedTypes(String[] aDisplayedTypeNames) {
    if (aDisplayedTypeNames == null) {
      displayedTypeNames = null;
    } else {
      displayedTypeNames = new HashSet<String>();
      displayedTypeNames.addAll(Arrays.asList(aDisplayedTypeNames));
    }
  }

  /**
   * Set the list of types that will NOT be highlighted in the viewer.
   * 
   * @param aTypeNames
   *          names of types that are never to be highlighted.
   */
  public void setHiddenTypes(String[] aTypeNames) {
    hiddenTypeNames.clear();
    hiddenTypeNames.addAll(Arrays.asList(aTypeNames));
  }

  /**
   * Configures the initially selected types in the viewer. If not called, all types will be
   * initially selected.
   * 
   * @param aTypeNames
   *          array of fully-qualified names of types to be initially selected
   */
  public void setInitiallySelectedTypes(String[] aTypeNames) {
    initiallySelectedTypeNames = new HashSet<String>();
    for (int i = 0; i < aTypeNames.length; i++) {
      initiallySelectedTypeNames.add(aTypeNames[i]);
    }
    // apply to existing checkboxes
    Iterator<Map.Entry<Type, JCheckBox>> iterator = typeToCheckBoxMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Type, JCheckBox> entry = iterator.next();
      String type = ((Type) entry.getKey()).getName();
      JCheckBox checkbox = (JCheckBox) entry.getValue();
      checkbox.setSelected(typeNamesContains(initiallySelectedTypeNames, type));
    }

    // redisplay (if we have a CAS) - this allows this method to be called
    // either before or after displaying the viewer
    if (cas != null) {
      display();
    }
  }

  /**
   * Configures the viewer to hide certain features in the annotation deatail pane.
   * 
   * @param aFeatureNames
   *          array of (short) feature names to be hidden
   */
  public void setHiddenFeatures(String[] aFeatureNames) {
    hiddenFeatureNames.clear();
    // add default hidden features
    hiddenFeatureNames.addAll(Arrays.asList(DEFAULT_HIDDEN_FEATURES));
    // add user-defined hidden features
    hiddenFeatureNames.addAll(Arrays.asList(aFeatureNames));
  }

  /**
   * Configures whether the viewer will allow the user to switch to "Entity" view, which highlight
   * entities rather than annotations.  Entity mode is typically only useful if the
   * {@link #setEntityResolver(EntityResolver)} method has been called with a user-supplied
   * class that can determine which annotations refer to the same entity.
   * 
   * @param aEnabled
   *          true to enable entity viewing mode, false to allow annotation viewing only.
   *          The default is false.
   */
  public void setEntityViewEnabled(boolean aEnabled) {
    entityViewEnabled = aEnabled;
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
    useConsistentColors = aConsistent;
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
   * @param aHideUnselected
   */
  public void setHideUnselectedCheckboxes(boolean aHideUnselected) {
    hideUnselectedCheckboxes = aHideUnselected;
    display();
  }

  /**
   * Sets the CAS to be viewed. This must be called before {@link #display()}.
   * 
   * @param aCAS
   *          the CSA to be viewed
   */
  @SuppressWarnings("unchecked")
  public void setCAS(CAS aCAS) {
    cas = aCAS;
    stringType = cas.getTypeSystem().getType(CAS.TYPE_NAME_STRING);
    fsArrayType = cas.getTypeSystem().getType(CAS.TYPE_NAME_FS_ARRAY);
    // clear checkbox panel so it will be repopulated
    annotationCheckboxPanel.removeAll();
    entityCheckboxPanel.removeAll();
    typeToCheckBoxMap.clear();
    entityToCheckBoxMap.clear();
    // clear selected annotation details tree
    this.updateSelectedAnnotationTree(-1);

    // clear type to color map if color consistency is off
    if (!useConsistentColors) {
      typeColorMap.clear();
      // but reassign colors to high frequency types
      assignColors(highFrequencyTypes);
    }

    // clear boldface
    boldFaceKeyWords = new String[0];
    boldFaceSpans = new int[0];

    // enable or disable entity view depending on user's choice 
    this.viewModePanel.setVisible(entityViewEnabled);

    // Populate sofa combo box with the names of all text Sofas in the CAS
    sofaSelectionComboBox.removeAllItems();
    Iterator<SofaFS> sofas = aCAS.getSofaIterator();
    Feature sofaIdFeat = aCAS.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
    boolean nonDefaultSofaFound = false;
    while (sofas.hasNext()) {
      SofaFS sofa = sofas.next();
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
   * case-insensitive. Call this method after {@link #setCAS(CAS)}. It wil apply only to the current
   * document, and will be reset on the next call to {@link #setCAS(CAS)}.
   * 
   * @param aWords
   *          array of words to highlight in boldface.
   */
  public void applyBoldfaceToKeywords(String[] aWords) {
    boldFaceKeyWords = aWords;
    doBoldface();
  }

  /**
   * Causes the specified spans to appear in boldface. This is case-insensitive. Call this method
   * after {@link #setCAS(CAS)}. It wil apply only to the current document, and will be reset on the
   * next call to {@link #setCAS(CAS)}.
   * 
   * @param aSpans
   *          spans to appear in boldface (begin1, end1, begin2, end2, ...)
   */
  public void applyBoldfaceToSpans(int[] aSpans) {
    boldFaceSpans = aSpans;
    doBoldface();
  }

  /**
   * Configures the viewer appropriately for displaying a hit against an XML fragments query. This
   * does not use a sophisticated algorithm for determining the location of the document that
   * matched the query. Currently all it does is call {@link #setInitiallySelectedTypes(String[])}
   * with the list of types mentioned in the query and {@link #applyBoldfaceToKeywords(String[])} on
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
    List<String> typeList = new ArrayList<String>();
    List<String> keywordList = new ArrayList<String>();

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
   * with the list of types mentioned in the query and {@link #applyBoldfaceToKeywords(String[])} on
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
  public void assignCheckedFromList(List<String> aNotChecked) {
    Iterator<String> iterC = aNotChecked.iterator();
    while (iterC.hasNext()) {
      String typeName = iterC.next();
      // assign to list of types not to be initially checked
      typesNotChecked.add(typeName);
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
  public void assignColorsFromList(List<Color> aColors, List<String> aTypeNames) {
    // populate mTypeNameToColorMap
    Iterator<String> iter = aTypeNames.iterator();
    Iterator<Color> iterC = aColors.iterator();
    while (iter.hasNext()) {
      if (!iterC.hasNext())
        break;
      String typeName = (String) iter.next();
      Color color = (Color) iterC.next();
      // assign background color
      typeColorMap.put(typeName, color);
    }

    setUserTypes(aTypeNames);

    // clear checkbox panel so it will be refreshed
    annotationCheckboxPanel.removeAll();
    typeToCheckBoxMap.clear();
  }

  /**
   * Assign colors to the specified types
   * 
   * @param aTypeNames
   *          list of type names
   */
  private void assignColors(List<String> aTypeNames) {
    // populate mTypeNameToColorMap
    Iterator<String> iter = aTypeNames.iterator();
    while (iter.hasNext()) {
      String typeName = iter.next();
      // assign background color
      Color c = COLORS[typeColorMap.size() % COLORS.length];
      typeColorMap.put(typeName, c);
    }

    // clear checkbox panel so it will be refreshed
    annotationCheckboxPanel.removeAll();
    typeToCheckBoxMap.clear();
  }

  /**
   * Creates/updates the display. This is called when setCAS() is called and again each time to
   * user's mode or checkbox selections change.
   */
  private void display() {
    // remember split pane divider location so we can restore it later
    int dividerLoc = verticalSplitPane.getDividerLocation();

    // remember caret pos and scroll position
    int caretPos = this.textPane.getCaretPosition();
    int verticalScrollPos = this.textScrollPane.getVerticalScrollBar().getValue();

    // type of display depends on whether we are in annotation or entity mode
    switch (viewMode) {
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
    if (hideUnselectedCheckboxes) {
      showHideUnselectedButton.setText("Show Unselected");
    } else {
      showHideUnselectedButton.setText("Hide Unselected");
    }

    // reset scroll position
    textPane.setCaretPosition(caretPos);
    textScrollPane.getVerticalScrollBar().setValue(verticalScrollPos);
    textScrollPane.revalidate();

    // reset split pane divider
    verticalSplitPane.setDividerLocation(dividerLoc);
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
      doc.insertString(0, cas.getDocumentText(), new SimpleAttributeSet());
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }

    // Iterate over annotations
    FSIterator<AnnotationFS> iter = cas.getAnnotationIndex().iterator();
    Hashtable<String, JCheckBox> checkBoxes = new Hashtable<String, JCheckBox>();
    Set<JCheckBox> checkBoxesDone = new HashSet<JCheckBox>();
    while (iter.isValid()) {
      AnnotationFS fs = iter.get();
      iter.moveToNext();

      Type type = fs.getType();

      // have we seen this type before?
      JCheckBox checkbox = (JCheckBox) typeToCheckBoxMap.get(type);
      if (checkbox == null) {
        // check that type should be displayed
        if ((displayedTypeNames == null || typeNamesContains(displayedTypeNames, type.getName()))
                && !typeNamesContains(hiddenTypeNames, type.getName())) {
          // if mTypeNameToColorMap exists, get color from there
          Color c = (Color) typeColorMap.get(type.getName());
          if (c == null) // assign next available color
          {
            c = COLORS[typeColorMap.size() % COLORS.length];
            typeColorMap.put(type.getName(), c);
          }
          // This next section required until priorities work properly
          // HashSet noCheckSet = new HashSet();
          String noCheckArray[] = {
          // "org.apache.jresporator.PROPER",
          // "DOCSTRUCT_ANNOT_TYPE",
          // "VOCAB_ANNOT_TYPE"
          };
          for (int i = 0; i < noCheckArray.length; i++) {
            typesNotChecked.add(noCheckArray[i]);
          }
          // end of section

          // should type be initially selected?
          boolean selected = ((initiallySelectedTypeNames == null &&
          // document annotation is not initially selected in default case
                  !CAS.TYPE_NAME_DOCUMENT_ANNOTATION.equals(type.getName()) && !typesNotChecked
                  .contains(type.getName()) // priorities JMP
          ) || (initiallySelectedTypeNames != null && typeNamesContains(
                  initiallySelectedTypeNames, type.getName())));

          // add checkbox
          checkbox = new JCheckBox(type.getShortName(), selected);
          checkbox.setToolTipText(type.getName());
          checkbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
              display();
			}        	  
          });
          checkbox.setBackground(c);
          // annotationCheckboxPanel.add(checkbox); do it later JMP
          checkBoxes.put(type.getName(), checkbox);
          checkBoxesDone.add(checkbox);
          // add to (Type, Checkbox) map
          typeToCheckBoxMap.put(type, checkbox);
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
        if (begin == 0 && end == cas.getDocumentText().length()) {
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
    List<String> aTypeNames = getUserTypes();
    if (aTypeNames != null) {
      Iterator<String> iterT = aTypeNames.iterator();
      while (iterT.hasNext()) {
        String typeName = iterT.next();
        JCheckBox cb = (JCheckBox) checkBoxes.get(typeName);
        if (cb != null) {
          annotationCheckboxPanel.add(cb);
          checkBoxesDone.remove(cb);
        }
      }
    }
    // add additional checkboxes in alphabetical order
    List<JCheckBox> checkboxes = new LinkedList<JCheckBox>(checkBoxesDone);
    Collections.sort(checkboxes, new Comparator<JCheckBox>() {
      public int compare(JCheckBox o1, JCheckBox o2) {
        return o1.getText().toLowerCase().compareTo(
                 o2.getText().toLowerCase());
      }
    });
    Iterator<JCheckBox> iterC = checkboxes.iterator();
    while (iterC.hasNext()) {
      JCheckBox cb = iterC.next();
      annotationCheckboxPanel.add(cb);
    }

    // add/remove checkboxes from display as determined by the
    // mHideUnselectedCheckboxes toggle
    Iterator<JCheckBox> cbIter = typeToCheckBoxMap.values().iterator();
    while (cbIter.hasNext()) {
      JCheckBox cb = cbIter.next();
      if (hideUnselectedCheckboxes && !cb.isSelected()) {
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
      doc.insertString(0, cas.getDocumentText(), new SimpleAttributeSet());
    } catch (BadLocationException e) {
      throw new RuntimeException(e);
    }

    // Iterate over EntityAnnotations using JCAS, because the EntityResolver interface
    // uses JCAS as a convenience to the user.
    JCas jcas;
    try {
      // NOTE: for a large type system, this can take a few seconds, which results in a
      // noticeable delay when the user first switches to Entity mode.
      jcas = cas.getJCas();
    } catch (CASException e) {
      throw new RuntimeException(e);
    }
    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    while (iter.isValid()) {
      Annotation annot = iter.get();
      iter.moveToNext();

      // find out what entity this annotation represents
      EntityResolver.Entity entity = mEntityResolver.getEntity(annot);

      //if not an entity, skip it
      if (entity == null)
        continue;
      
      // have we seen this entity before?
      JCheckBox checkbox = entityToCheckBoxMap.get(entity);
      if (checkbox == null) {
        // assign next available color
        Color c = COLORS[entityToCheckBoxMap.size() % COLORS.length];
        // add checkbox
        checkbox = new JCheckBox(entity.getCanonicalForm(), true);
        checkbox.setToolTipText(entity.getCanonicalForm());
        checkbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
              display();
			}
        });
        checkbox.setBackground(c);
        entityCheckboxPanel.add(checkbox);
        // add to (Entity, Checkbox) map
        entityToCheckBoxMap.put(entity, checkbox);
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
    Iterator<JCheckBox> cbIter = entityToCheckBoxMap.values().iterator();
    while (cbIter.hasNext()) {
      JCheckBox cb = cbIter.next();
      if (hideUnselectedCheckboxes && !cb.isSelected()) {
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
    FSIterator<AnnotationFS> annotIter = this.cas.getAnnotationIndex().iterator();
    while (annotIter.isValid()) {
      AnnotationFS annot = annotIter.get();
      // if (getPanePosition(annot.getBegin()) <= aPosition
      // && getPanePosition(annot.getEnd()) > aPosition)
      if (annot.getBegin() <= aPosition && annot.getEnd() > aPosition) {
        JCheckBox checkbox = typeToCheckBoxMap.get(annot.getType());
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
    this.horizontalSplitPane.revalidate();
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
    for (int i = 0; i < root.getChildCount(); i++) {
    	DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
    	if (aAnnotation.getType().equals(((TypeTreeNodeObject) child.getUserObject()).getType())) {
    		typeNode = child;
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
    List<Feature> aFeatures = aFS.getType().getFeatures();
    Iterator<Feature> iter = aFeatures.iterator();
    while (iter.hasNext()) {
      Feature feat = (Feature) iter.next();
      String featName = feat.getShortName();
      // skip hidden features
      if (hiddenFeatureNames.contains(featName)) {
        continue;
      }
      // how we get feature value depends on feature's range type)
      String featVal = "null";
      Type rangeType = feat.getRange();
      String rangeTypeName = rangeType.getName();
      if (cas.getTypeSystem().subsumes(stringType, rangeType)) {
        featVal = aFS.getStringValue(feat);
        if (featVal == null) {
          featVal = "null";
        } else if (featVal.length() > 64) {
          featVal = featVal.substring(0, 64) + "...";
        }
      } else if (rangeType.isPrimitive()) {
        featVal = aFS.getFeatureValueAsString(feat);
      } else if (cas.getTypeSystem().subsumes(fsArrayType, rangeType)) {
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
  private boolean typeNamesContains(Set<String> names, String name) {
    if (names.contains(name))
      return true;
    else {
      Iterator<String> namesIterator = names.iterator();
      while (namesIterator.hasNext()) {
        String otherName = namesIterator.next();
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

    horizontalSplitPane.setPreferredSize(paneSize);
    horizontalSplitPane.setSize(paneSize);
  }

  /**
   * Applies boldface as per the mBoldfaceKeywords and mBoldfaceSpans fields.
   */
  private void doBoldface() {
    // Keywords
    if (boldFaceKeyWords.length > 0) {
      // build regular expression
      StringBuffer regEx = new StringBuffer();
      for (int i = 0; i < boldFaceKeyWords.length; i++) {
        if (i > 0) {
          regEx.append('|');
        }
        regEx.append("\\b");
        String word = boldFaceKeyWords[i];
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
      Matcher matcher = pattern.matcher(cas.getDocumentText());
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
    int docLength = cas.getDocumentText().length();
    int len = boldFaceSpans.length;
    len -= len % 2; // to avoid ArrayIndexOutOfBoundsException if some numbskull passes in an
    // odd-length array
    int i = 0;
    while (i < len) {
      int begin = boldFaceSpans[i];
      int end = boldFaceSpans[i + 1];
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
   * Trivial entity resolver that's applied if the user turns on entity mode without
   * specifying their own entity resolver.  Returns the covered text as the canonical form,
   * and treats annotations with equal covered text as belonging to the same entity.
   */
  public class DefaultEntityResolver implements EntityResolver {

    /* (non-Javadoc)
     * @see org.apache.uima.tools.viewer.EntityResolver#getCanonicalForm(org.apache.uima.jcas.tcas.Annotation)
     */
    public Entity getEntity(final Annotation inAnnotation) {
      return new Entity() {
        
        public String getCanonicalForm() {
          return inAnnotation.getCoveredText();
        }

        @Override
        public boolean equals(Object inObject) {
          if (inObject instanceof Entity) {
            String canon = ((Entity)inObject).getCanonicalForm();
            return canon != null && canon.equals(this.getCanonicalForm());
          }
          return false;
        }

        @Override
        public int hashCode() {
          return this.getCanonicalForm().hashCode();
        }
      };
    }
  }

  /**
   * Inner class containing data for a tree node representing a Type
   */
  private static class TypeTreeNodeObject {
    private Type type;

    public TypeTreeNodeObject(Type inType) {
      this.type = inType;
    }

    public Type getType() {
      return this.type;
    }

    @Override
    public String toString() {
      return this.type.getShortName();
    }
  }

  /**
   * Inner class containing data for a tree node representing a FeatureStructure
   */
  private static class FsTreeNodeObject {
    private FeatureStructure featureStructure;
    private String featureName;
    private String caption;

    public FsTreeNodeObject(FeatureStructure inFeatureStructure, String inFeatureName) {
      this.featureStructure = inFeatureStructure;
      this.featureName = inFeatureName;
      this.caption = this.featureStructure.getType().getShortName();
      if (this.featureStructure instanceof AnnotationFS) {
        String coveredText = ((AnnotationFS) this.featureStructure).getCoveredText();
        if (coveredText.length() > 64)
          coveredText = coveredText.substring(0, 64) + "...";
        this.caption += " (\"" + coveredText + "\")";
      }
      if (this.featureName != null) {
        this.caption = this.featureName + " = " + this.caption;
      }
    }

    public FeatureStructure getFeatureStructure() {
      return this.featureStructure;
    }

    @Override
    public String toString() {
      return this.caption;
    }
  }

  private class AnnotationTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = -8661556785397184756L;

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree,
     *      java.lang.Object, boolean, boolean, boolean, int, boolean)
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
      // set background color if this is an Annotation or a Type
      Color background = null;
      if (value instanceof DefaultMutableTreeNode) {
        Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
        Type type = null;
        if (userObj instanceof FsTreeNodeObject) {
          FeatureStructure fs = ((FsTreeNodeObject) userObj).getFeatureStructure();
          type = fs.getType();
        } else if (userObj instanceof TypeTreeNodeObject) {
          type = ((TypeTreeNodeObject) userObj).getType();
        }
        if (type != null) {
          // look up checkbox to get color
          JCheckBox checkbox = typeToCheckBoxMap.get(type);
          if (checkbox != null) {
            background = checkbox.getBackground();
          }
        }
      }
      this.setBackgroundNonSelectionColor(background);
      this.setBackgroundSelectionColor(background);

      Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
              row, hasFocus);
      return component;
    }
  }

  /**
   * A panel that is to be placed in a JScrollPane that can only scroll vertically. This panel
   * should have its width track the viewport's width, and increase its height as necessary to
   * display all components.
   * 
   * 
   */
  private static class VerticallyScrollablePanel extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1009744410018634511L;

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
     */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return this.getPreferredSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
     */
    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 50;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getScrollableTracksViewportHeight()
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getScrollableTracksViewportWidth()
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
     */
    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
      return 10;
    }
  }
}
