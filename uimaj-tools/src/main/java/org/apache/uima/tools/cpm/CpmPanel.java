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
package org.apache.uima.tools.cpm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCasProcessors;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeCollectionReaderCasInitializer;
import org.apache.uima.collection.metadata.CpeCollectionReaderIterator;
import org.apache.uima.collection.metadata.CpeComponentDescriptor;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.tools.util.gui.FileChooserBugWorkarounds;
import org.apache.uima.tools.util.gui.FileSelector;
import org.apache.uima.tools.util.gui.FileSelectorListener;
import org.apache.uima.tools.util.gui.TransportControlListener;
import org.apache.uima.tools.util.gui.TransportControlPanel;
import org.apache.uima.tools.util.gui.XMLFileFilter;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.UriUtils;
import org.apache.uima.util.XMLInputSource;

public class CpmPanel extends JPanel implements ActionListener, FileSelectorListener,
        TabClosedListener, TransportControlListener {
  private static final long serialVersionUID = -5096300176103368922L;

  public static final String HELP_MESSAGE = "Instructions for using UIMA Collection Processing Engine Configurator:\n\n"
          + "Select a Collection Reader descriptor using the Browse button in the topmost panel.\n\n"
          + "On the Analyis Engines panel and the CAS Consumers panel, use the Add button to select Analysis Engine (AE) \n"
          + "and CAS Consumer descriptors.\n\n"
          + "Press the Play button to start collection processing.\n"
          + "A progress bar in the lower left corner of the window will indicate the processing progress.\n"
          + "When running, you may use the Pause or Stop button to pause or stop the processing.\n\n"
          + "The File menu contains options for opening and saving CPE descriptors.\n\n"
          + "The View menu contains an option to display the CAS Initializer panel.  CAS Initializers are deprecated \n"
          + "since UIMA version 2.0, but are still supported by this tool.";

  private static final String PREFS_CPE_DESCRIPTOR_FILE = "cpeDescriptorFile";
  
  private static final String PREFS_SAVE_USING_IMPORTS ="saveUsingImports";

  private JMenuItem openCpeDescMenuItem;

  private JMenuItem saveCpeDescMenuItem;

  private JMenuItem refreshMenuItem;

  private JMenuItem clearAllMenuItem;

  private JSplitPane mainSplitPane;

  private JSplitPane readerInitializerSplitPane;

  private ResetableMetaDataPanel collectionReaderPanel;

  private TitledBorder collectionReaderTitledBorder;

  private ResetableMetaDataPanel casInitializerPanel;

  private TitledBorder casInitializerTitledBorder;

  private FileSelector readerFileSelector;

  private FileSelector casInitializerFileSelector;

  private JPanel aeMainPanel;

  private JTabbedPaneWithCloseIcons aeTabbedPane;

  private JButton moveAeRightButton;

  private JButton moveAeLeftButton;

  private JButton addAeButton;

  private TitledBorder aeTitledBorder;

  private JPanel consumersPanel;

  private TitledBorder consumerTitledBorder;

  private JButton addConsumerButton;

  private JButton moveConsumerRightButton;

  private JButton moveConsumerLeftButton;

  private JTabbedPaneWithCloseIcons consumerTabbedPane;

  private Vector aeSpecifiers = new Vector();

  private Vector consumerSpecifiers = new Vector();

  // private static LogDialog logDialog;
  private JProgressBar progressBar;

  private TransportControlPanel transportControlPanel;

  private AbstractButton startButton;

  private AbstractButton stopButton;

  private JLabel statusLabel;

  private Timer progressTimer;

  private int elapsedTime;

  private Timer performanceQueryTimer;

  private CollectionReaderDescription collectionReaderDesc;

  private CasInitializerDescription casInitializerDesc;

  private CollectionProcessingEngine mCPE;

  private boolean indeterminateProgressPause;

  private JFileChooser aeFileChooser;

  private JFileChooser consumerFileChooser;

  private JFileChooser openSaveFileChooser;

  private File fileChooserRootDir;

  private long collectionReaderLastFileSyncTimestamp;

  private long casInitializerLastFileSyncTimestamp;

  private long lastFileSyncUserPromptTime;

  /** Stores user preferences */
  private Preferences prefs = Preferences.userRoot().node("org/apache/uima/tools/CPE_GUI");

  private boolean mShuttingDown;

  private boolean mPaused;

  private boolean selectedComponentsChanged = false;

  private JMenuItem viewCasInitializerPanelMenuItem;

  private CpeDescription currentCpeDesc = createEmptyCpeDescription();
  
  private final ResourceManager defaultResourceManager = UIMAFramework.newDefaultResourceManager();
  
  private boolean saveUsingImports = true;

  private JCheckBoxMenuItem saveUsingImportMenuItem;

  public CpmPanel() {
    super();

    // The following is VERY flaky:
    // Don't 'try' this at home:
    /*
     * try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception
     * e) { }
     */

    String fileChooserRootDirPath = System.getProperty("uima.file_chooser_root");
    if (fileChooserRootDirPath == null) {
      fileChooserRootDirPath = System.getProperty("user.dir");
    }
    fileChooserRootDir = new File(fileChooserRootDirPath);

    // setResizable(false);
    this.setLayout(new BorderLayout());

    mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    mainSplitPane.setResizeWeight(0.25);
    JSplitPane bottomSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    bottomSplitPane.setResizeWeight(0.5);
    mainSplitPane.setBottomComponent(bottomSplitPane);
    readerInitializerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    readerInitializerSplitPane.setResizeWeight(0.5);

    collectionReaderPanel = new ResetableMetaDataPanel(2);
    // Initialized in readPreferences

    Border bevelBorder = BorderFactory.createRaisedBevelBorder();
    collectionReaderTitledBorder = BorderFactory.createTitledBorder(bevelBorder,
            "Collection Reader");
    collectionReaderPanel.setBorder(collectionReaderTitledBorder);

    JScrollPane collectionReaderScrollPane = new JScrollPane(collectionReaderPanel);
    readerInitializerSplitPane.setLeftComponent(collectionReaderScrollPane);

    casInitializerPanel = new ResetableMetaDataPanel(2);

    bevelBorder = BorderFactory.createRaisedBevelBorder();
    casInitializerTitledBorder = BorderFactory.createTitledBorder(bevelBorder, "CAS Initializer");
    casInitializerPanel.setBorder(casInitializerTitledBorder);

    readerInitializerSplitPane.setRightComponent(casInitializerPanel);

    mainSplitPane.setTopComponent(readerInitializerSplitPane);

    // AE Panel
    aeMainPanel = new JPanel();
    aeMainPanel.setLayout(new BorderLayout());
    aeTitledBorder = BorderFactory.createTitledBorder(bevelBorder, "Analysis Engines");
    aeMainPanel.setBorder(aeTitledBorder);

    addAeButton = new JButton("Add...");
    addAeButton.addActionListener(this);

    moveAeLeftButton = new JButton("<<");
    moveAeLeftButton.addActionListener(this);

    moveAeRightButton = new JButton(">>");
    moveAeRightButton.addActionListener(this);

    JPanel addAePanel = new JPanel();
    addAePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    addAePanel.add(addAeButton);
    addAePanel.add(moveAeLeftButton);
    addAePanel.add(moveAeRightButton);

    aeMainPanel.add(addAePanel, BorderLayout.NORTH);

    aeTabbedPane = new JTabbedPaneWithCloseIcons();
    aeTabbedPane.addTabClosedListener(this);

    aeMainPanel.add(aeTabbedPane, BorderLayout.CENTER);

    JScrollPane aeScrollPane = new JScrollPane(aeMainPanel);
    bottomSplitPane.setTopComponent(aeScrollPane);

    // Consumers panel
    consumersPanel = new JPanel();
    consumersPanel.setLayout(new BorderLayout());
    consumerTitledBorder = BorderFactory.createTitledBorder(bevelBorder, "CAS Consumers");
    consumersPanel.setBorder(consumerTitledBorder);

    addConsumerButton = new JButton("Add...");
    addConsumerButton.addActionListener(this);

    moveConsumerLeftButton = new JButton("<<");
    moveConsumerLeftButton.addActionListener(this);

    moveConsumerRightButton = new JButton(">>");
    moveConsumerRightButton.addActionListener(this);

    JPanel addConsumerPanel = new JPanel();
    addConsumerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    addConsumerPanel.add(addConsumerButton);
    addConsumerPanel.add(moveConsumerLeftButton);
    addConsumerPanel.add(moveConsumerRightButton);

    consumersPanel.add(addConsumerPanel, BorderLayout.NORTH);

    consumerTabbedPane = new JTabbedPaneWithCloseIcons();
    consumerTabbedPane.addTabClosedListener(this);

    consumersPanel.add(consumerTabbedPane, BorderLayout.CENTER);

    JScrollPane consumerScrollPane = new JScrollPane(consumersPanel);
    bottomSplitPane.setBottomComponent(consumerScrollPane);

    // logDialog = new LogDialog();

    this.add(mainSplitPane, BorderLayout.CENTER);

    JPanel southernPanel = new JPanel();
    southernPanel.setLayout(new BorderLayout());

    JPanel southernCentralPanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridwidth = 3;
    gbc.gridy = 0;
    gbc.insets = new Insets(4, 4, 4, 4);

    southernCentralPanel.setLayout(gbl);

    progressBar = new JProgressBar();
    gbc.gridx = 0;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    southernCentralPanel.add(progressBar, gbc);

    transportControlPanel = new TransportControlPanel(this);
    transportControlPanel.setButtonTooltipText(TransportControlPanel.PLAY_BUTTON,
            "Run Collection Processing");
    transportControlPanel.setButtonTooltipText(TransportControlPanel.PAUSE_BUTTON,
            "Pause Collection Processing");
    transportControlPanel.setButtonTooltipText(TransportControlPanel.STOP_BUTTON,
            "Stop Collection Processing");

    gbc.gridx = 1;
    gbc.weightx = 0.0;
    gbc.anchor = GridBagConstraints.CENTER;

    southernCentralPanel.add(transportControlPanel, gbc);
    southernPanel.add(southernCentralPanel, BorderLayout.CENTER);

    statusLabel = new JLabel("Initialized", SwingConstants.LEFT);
    southernPanel.add(statusLabel, BorderLayout.SOUTH);

    startButton = transportControlPanel.getButton(TransportControlPanel.PLAY_BUTTON);
    startButton.addActionListener(this);
    stopButton = transportControlPanel.getButton(TransportControlPanel.STOP_BUTTON);
    stopButton.addActionListener(this);

    this.add(southernPanel, BorderLayout.SOUTH);

    progressTimer = new Timer(1000, this);

    performanceQueryTimer = new Timer(2000, this);

    initFileChoosers();

    //create file selectors (used to populate CR and CI panels later) 
    readerFileSelector = new FileSelector(null, "Collection Reader Descriptor",
            JFileChooser.FILES_ONLY, fileChooserRootDir, new XMLFileFilter());
    readerFileSelector.addFileSelectorListener(this, collectionReaderPanel);

    casInitializerFileSelector = new FileSelector(null, "CAS Initializer Descriptor",
            JFileChooser.FILES_ONLY, fileChooserRootDir, new XMLFileFilter());
    casInitializerFileSelector.addFileSelectorListener(this, casInitializerPanel);

    // initialize empty CollectionReader and CAS Initializer panels
    try {
      populateCollectionReaderPanel(null);
      populateCasInitializerPanel(null);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // CAS initializer panel is initially hidden since it is deprecated
    setCasInitializerPanelVisible(false);
    
    // read preferences (loads last opened CPE descriptor)
    if (System.getProperty("uima.noprefs") == null) {
      readPreferences();
    }
  }

  /**
   * Initialize the file choosers. This is called initially from the constructor but can be called
   * again to reset the file choosers to their default state.
   */
  private void initFileChoosers() {
    // set up AE file chooser to point to directory containing last
    // selected AE
    aeFileChooser = new JFileChooser(fileChooserRootDir);
    aeFileChooser.setDialogTitle("Add Analysis Engine");
    aeFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    aeFileChooser.addChoosableFileFilter(new XMLFileFilter());
    File aeDescDir = fileChooserRootDir;
    if (aeSpecifiers.size() > 0) {
      File lastAeFile = new File((String) aeSpecifiers.get(aeSpecifiers.size() - 1));
      aeDescDir = lastAeFile.getParentFile();
    }
    if (aeDescDir.exists()) {
      FileChooserBugWorkarounds.setCurrentDirectory(aeFileChooser, aeDescDir);
    }

    // set up CAS consumer file chooser to point to directory containing last
    // selected consumer
    consumerFileChooser = new JFileChooser(fileChooserRootDir);
    consumerFileChooser.setDialogTitle("Add CAS Consumer");
    consumerFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    consumerFileChooser.addChoosableFileFilter(new XMLFileFilter());
    File consumerDescDir = fileChooserRootDir;
    if (consumerSpecifiers.size() > 0) {
      File lastConsumerFile = new File((String) consumerSpecifiers
              .get(consumerSpecifiers.size() - 1));
      consumerDescDir = lastConsumerFile.getParentFile();
    }
    if (consumerDescDir.exists()) {
      FileChooserBugWorkarounds.setCurrentDirectory(consumerFileChooser, consumerDescDir);
    }

    this.openSaveFileChooser = new JFileChooser(fileChooserRootDir);
    openSaveFileChooser.setFileFilter(new XMLFileFilter());
  }

  private Frame getParentFrame() {
    Component parent = this.getParent();
    while (parent != null && !(parent instanceof Frame)) {
      parent = parent.getParent();
    }
    return (Frame) parent;
  }

  /**
   * Creates JMenuItems that should be added to the File menu
   * 
   * @return a List of JMenuItems
   */
  public List createFileMenuItems() {
    List menuItemList = new ArrayList();

    openCpeDescMenuItem = new JMenuItem("Open CPE Descriptor");
    openCpeDescMenuItem.addActionListener(this);
    menuItemList.add(openCpeDescMenuItem);

    saveCpeDescMenuItem = new JMenuItem("Save CPE Descriptor");
    saveCpeDescMenuItem.addActionListener(this);
    menuItemList.add(saveCpeDescMenuItem);

    refreshMenuItem = new JMenuItem("Refresh Descriptors from File System");
    refreshMenuItem.addActionListener(this);
    menuItemList.add(refreshMenuItem);

    JMenu saveOptionsSubmenu = new JMenu ("Save Options");
    saveUsingImportMenuItem = new JCheckBoxMenuItem("Use <import>");
    saveUsingImportMenuItem.addActionListener(this);
    saveUsingImportMenuItem.setSelected(saveUsingImports);
    saveOptionsSubmenu.add(saveUsingImportMenuItem);
    menuItemList.add(saveOptionsSubmenu);
        
    clearAllMenuItem = new JMenuItem("Clear All");
    clearAllMenuItem.addActionListener(this);
    menuItemList.add(clearAllMenuItem);

    return menuItemList;
  }

  /**
   * Creates JMenuItems that should be added to the View menu
   * 
   * @return a List of JMenuItems
   */
  public List createViewMenuItems() {
    List menuItemList = new ArrayList();

    viewCasInitializerPanelMenuItem = new JCheckBoxMenuItem("CAS Initializer Panel");
    viewCasInitializerPanelMenuItem.setSelected(casInitializerPanel.isVisible());
    viewCasInitializerPanelMenuItem.addActionListener(this);
    menuItemList.add(viewCasInitializerPanelMenuItem);

    return menuItemList;
  }
  
  private void setCasInitializerPanelVisible(boolean visible) {
    casInitializerPanel.setVisible(visible);
    if (viewCasInitializerPanelMenuItem != null) {
      viewCasInitializerPanelMenuItem.setSelected(visible);
    }
    if (visible) {
      readerInitializerSplitPane.setDividerLocation(0.5);
    }
  }

  private void readPreferences() {
    String cpeDescFileString = prefs.get(PREFS_CPE_DESCRIPTOR_FILE, null);
    if (cpeDescFileString != null) {
      File cpeDescFile = new File(cpeDescFileString);
      if (cpeDescFile.exists()) {
        openSaveFileChooser.setSelectedFile(cpeDescFile);
        try {
          openCpeDescriptor(cpeDescFile);
        } catch (Exception e) {
          System.err.println("Error loading last known CPE Descriptor " + cpeDescFileString);
          e.printStackTrace();
        }
      }
    }
    String saveUsingImportsString = prefs.get(PREFS_SAVE_USING_IMPORTS, "true");
    setSaveUsingImports("true".equalsIgnoreCase(saveUsingImportsString));
  }

  private void setSaveUsingImports(boolean b) {
    saveUsingImports = b;
    if (saveUsingImportMenuItem != null)
      saveUsingImportMenuItem.setSelected(b);
    prefs.put(PREFS_SAVE_USING_IMPORTS, Boolean.toString(b));
  }

  private void startProcessing() {
    // Check that Collection Reader is selected
    if (collectionReaderDesc == null) {
      JOptionPane.showMessageDialog(CpmPanel.this, "No Collection Reader has been selected",
              "Error", JOptionPane.ERROR_MESSAGE);
      transportControlPanel.reset();
      resetScreen();
      return;
    }

    try {
      // update the parameter overrides according to GUI settings
      updateCpeDescriptionParameterOverrides();

      // intantiate CPE
      mCPE = null;  // to allow GC of previous ae's that may
                    // hold onto lots of memory e.g. OpenNLP
      mCPE = UIMAFramework.produceCollectionProcessingEngine(currentCpeDesc);

      // attach callback listener
      StatusCallbackListenerImpl statCbL = new StatusCallbackListenerImpl();
      mCPE.addStatusCallbackListener(statCbL);

      // start processing
      mCPE.process();
    } catch (Exception ex) {
      resetScreen();
      displayError(ex);
    }

  }

  /**
   * Updates the configuration parameter settings in this.currentCpeDesc to match the current state
   * of the GUI.
   */
  private void updateCpeDescriptionParameterOverrides() throws Exception {
    // first check for descriptors out of sync with filesystem
    checkForOutOfSyncFiles();

    // Collection Reader
    if (readerFileSelector.getSelected().length() > 0) {
      if (collectionReaderPanel.isModified()) {
        CasProcessorConfigurationParameterSettings crSettings = CpeDescriptorFactory
                .produceCasProcessorConfigurationParameterSettings();
        currentCpeDesc.getAllCollectionCollectionReaders()[0]
                .setConfigurationParameterSettings(crSettings);
        createParameterOverrides(crSettings, collectionReaderPanel);
      } else {
        currentCpeDesc.getAllCollectionCollectionReaders()[0]
                .setConfigurationParameterSettings(null);
      }
    }

    // CAS Initializer
    if (casInitializerFileSelector.getSelected().length() > 0) {
      if (casInitializerPanel.isModified()) {
        CasProcessorConfigurationParameterSettings casIniSettings = CpeDescriptorFactory
                .produceCasProcessorConfigurationParameterSettings();
        currentCpeDesc.getAllCollectionCollectionReaders()[0].getCasInitializer()
                .setConfigurationParameterSettings(casIniSettings);
        createParameterOverrides(casIniSettings, casInitializerPanel);
      } else {
        currentCpeDesc.getAllCollectionCollectionReaders()[0].getCasInitializer()
                .setConfigurationParameterSettings(null);
      }
    }
    // Analysis Engines
    for (int i = 0; i < aeSpecifiers.size(); i++) {
      CpeCasProcessor casProc = currentCpeDesc.getCpeCasProcessors().getCpeCasProcessor(i);
      AnalysisEnginePanel aePanel = (AnalysisEnginePanel) aeTabbedPane.getComponentAt(i);
      if (aePanel.isModified()) {
        CasProcessorConfigurationParameterSettings settings = CpeDescriptorFactory
                .produceCasProcessorConfigurationParameterSettings();
        casProc.setConfigurationParameterSettings(settings);
        createParameterOverrides(settings, aePanel);
      } else {
        casProc.setConfigurationParameterSettings(null);
      }
    }
    // CAS Consumers
    for (int i = 0; i < consumerSpecifiers.size(); i++) {
      CpeCasProcessor casProc = currentCpeDesc.getCpeCasProcessors().getCpeCasProcessor(
              aeSpecifiers.size() + i);
      ConsumerPanel consumerPanel = (ConsumerPanel) consumerTabbedPane.getComponentAt(i);
      if (consumerPanel.isModified()) {
        CasProcessorConfigurationParameterSettings settings = CpeDescriptorFactory
                .produceCasProcessorConfigurationParameterSettings();
        casProc.setConfigurationParameterSettings(settings);
        createParameterOverrides(settings, consumerPanel);
      } else {
        casProc.setConfigurationParameterSettings(null);
      }
    }
  }

  /**
   * Called by createCpeDescription to add configuration parameter overrides to the CpeDescription
   * being constructed, based on the user's changes in the GUI.
   * 
   * @param aSettings
   *          the CasProcessorConfigurationParameterSettings element that will be modified
   * @param aPanel
   *          the GUI panel representing settings for the CAS Processor
   * @param aClearDirty
   *          whether to clear the dirty bit of each field. This should be set to true when this
   *          method is called during the act of saving the CPE descriptor.
   */
  private void createParameterOverrides(CasProcessorConfigurationParameterSettings aSettings,
          MetaDataPanel aPanel) throws CpeDescriptorException {
    List values = aPanel.getValues();
    Iterator iterator = values.iterator();
    while (iterator.hasNext()) {
      ConfigField configField = (ConfigField) iterator.next();
      if (configField.isModified()) {
        String name = configField.getParameterName();
        Object value = configField.getFieldValue();

        if (value != null) {
          aSettings.setParameterValue(name, value);
        }
      }
    }
  }

  /**
   * Marks all fields as not dirty. To be called when CPE descriptor is opened or saved.
   */
  private void clearDirty() {
    collectionReaderPanel.clearDirty();
    casInitializerPanel.clearDirty();
    for (int i = 0; i < aeTabbedPane.getTabCount(); i++) {
      ((MetaDataPanel) aeTabbedPane.getComponentAt(i)).clearDirty();
    }
    for (int i = 0; i < consumerTabbedPane.getTabCount(); i++) {
      ((MetaDataPanel) consumerTabbedPane.getComponentAt(i)).clearDirty();
    }
    selectedComponentsChanged = false;
  }

  public void actionPerformed(ActionEvent ev) {
    Object source = ev.getSource();

    if (source == progressTimer) {
      elapsedTime++;
      // menuBarLabel.setText(ElapsedTimeFormatter.format(elapsedTime));

      displayProgress();
    } else if (source == performanceQueryTimer && mCPE != null) {
      // printStats();
    } else if (source == startButton) {
      statusLabel.setText("Initializing");
      // logDialog.clear();
      progressBar.setValue(0);

      final Thread worker = new Thread() {
        public void run() {
          startProcessing();
        }
      };
      worker.start();
    } else if (source == addAeButton) {
      int rv = aeFileChooser.showOpenDialog(addAeButton);

      if (rv == JFileChooser.APPROVE_OPTION) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        File file = aeFileChooser.getSelectedFile();

        // Create AE panel on the tabbed pane for this AE specifier file

        try {
          addAE(file.getPath());
        } catch (Exception e) {
          displayError(e);
        }

        int lastTabIndex = aeTabbedPane.getTabCount() - 1;
        aeTabbedPane.setSelectedIndex(lastTabIndex);

        setCursor(Cursor.getDefaultCursor());
      }
    } else if (source == moveAeLeftButton) {
      int index = aeTabbedPane.getSelectedIndex();
      if (index > 0) {
        // update CPE descriptor
        try {
          CpeCasProcessor casProcToMove = currentCpeDesc.getCpeCasProcessors().getCpeCasProcessor(
                  index);
          currentCpeDesc.getCpeCasProcessors().removeCpeCasProcessor(index);
          currentCpeDesc.getCpeCasProcessors().addCpeCasProcessor(casProcToMove, index - 1);
        } catch (CpeDescriptorException e) {
          displayError(e);
          return;
        }
        // update GUI
        aeTabbedPane.moveTab(index, index - 1);
        aeTabbedPane.setSelectedIndex(index - 1);
        Object specifierToMove = aeSpecifiers.remove(index);
        aeSpecifiers.add(index - 1, specifierToMove);
      }
    } else if (source == moveAeRightButton) {
      int index = aeTabbedPane.getSelectedIndex();
      if (index > -1 && index < aeTabbedPane.getTabCount() - 1) {
        // update CPE descriptor
        try {
          CpeCasProcessor casProcToMove = currentCpeDesc.getCpeCasProcessors().getCpeCasProcessor(
                  index);
          currentCpeDesc.getCpeCasProcessors().removeCpeCasProcessor(index);
          currentCpeDesc.getCpeCasProcessors().addCpeCasProcessor(casProcToMove, index + 1);
        } catch (CpeDescriptorException e) {
          displayError(e);
          return;
        }
        // update GUI
        aeTabbedPane.moveTab(index, index + 1);
        aeTabbedPane.setSelectedIndex(index + 1);
        Object specifierToMove = aeSpecifiers.remove(index);
        aeSpecifiers.add(index + 1, specifierToMove);
      }
    } else if (source == addConsumerButton) {
      int rv = consumerFileChooser.showOpenDialog(addConsumerButton);

      if (rv == JFileChooser.APPROVE_OPTION) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        File file = consumerFileChooser.getSelectedFile();

        // Create consumer panel on the tabbed pane
        // for this consumer specifier file:

        try {
          addConsumer(file.getPath());
        } catch (Exception e) {
          displayError(e);
        }

        int lastTabIndex = consumerTabbedPane.getTabCount() - 1;
        consumerTabbedPane.setSelectedIndex(lastTabIndex);

        setCursor(Cursor.getDefaultCursor());
      }
    } else if (source == moveConsumerLeftButton) {
      int index = consumerTabbedPane.getSelectedIndex();
      if (index > 0) {
        // update CPE descriptor
        try {
          int absIndex = aeSpecifiers.size() + index;
          CpeCasProcessor casProcToMove = currentCpeDesc.getCpeCasProcessors().getCpeCasProcessor(
                  absIndex);
          currentCpeDesc.getCpeCasProcessors().removeCpeCasProcessor(absIndex);
          currentCpeDesc.getCpeCasProcessors().addCpeCasProcessor(casProcToMove, absIndex - 1);
        } catch (CpeDescriptorException e) {
          displayError(e);
          return;
        }
        // update GUI
        consumerTabbedPane.moveTab(index, index - 1);
        consumerTabbedPane.setSelectedIndex(index - 1);
        Object specifierToMove = consumerSpecifiers.remove(index);
        consumerSpecifiers.add(index - 1, specifierToMove);
      }
    } else if (source == moveConsumerRightButton) {
      int index = consumerTabbedPane.getSelectedIndex();
      if (index > -1 && index < consumerTabbedPane.getTabCount() - 1) {
        // update CPE descriptor
        try {
          int absIndex = aeSpecifiers.size() + index;
          CpeCasProcessor casProcToMove = currentCpeDesc.getCpeCasProcessors().getCpeCasProcessor(
                  absIndex);
          currentCpeDesc.getCpeCasProcessors().removeCpeCasProcessor(absIndex);
          currentCpeDesc.getCpeCasProcessors().addCpeCasProcessor(casProcToMove, absIndex + 1);
        } catch (CpeDescriptorException e) {
          displayError(e);
          return;
        }
        // update GUI
        consumerTabbedPane.moveTab(index, index + 1);
        consumerTabbedPane.setSelectedIndex(index + 1);
        Object specifierToMove = consumerSpecifiers.remove(index);
        consumerSpecifiers.add(index + 1, specifierToMove);
      }
    } else if (source == saveCpeDescMenuItem) {
      saveCpeDescriptor();
    } else if (source == openCpeDescMenuItem) {
      int returnVal = this.openSaveFileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File f = this.openSaveFileChooser.getSelectedFile();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
          openCpeDescriptor(f);
        } catch (Exception e) {
          displayError(e);
        } finally {
          setCursor(Cursor.getDefaultCursor());
        }
      }
    } else if (source == refreshMenuItem) {
      refreshOutOfSyncFiles();
    } else if (source == clearAllMenuItem) {
      clearAll();
    } else if (source == viewCasInitializerPanelMenuItem) {
      setCasInitializerPanelVisible(!casInitializerPanel.isVisible());
    } else if (source == saveUsingImportMenuItem) {
      setSaveUsingImports(!saveUsingImports);      
    } 
  }

  /**
   * Resets the GUI to an empty state.
   */
  private void clearAll() {
    currentCpeDesc = createEmptyCpeDescription();
    collectionReaderPanel.reset();
    readerFileSelector.setSelected("");
    casInitializerPanel.reset();
    casInitializerFileSelector.setSelected("");
    removeAllAEsAndConsumers();
    initFileChoosers();
    collectionReaderPanel.validate();
    casInitializerPanel.validate();
    aeMainPanel.validate();
    consumersPanel.validate();
  }

  private CpeDescription createEmptyCpeDescription() {
    CpeDescription cpeDesc = CpeDescriptorFactory.produceDescriptor();
    // We use CAS pool size default of 3
    try {
      CpeCasProcessors cpeCasProcs = CpeDescriptorFactory.produceCasProcessors();
      cpeDesc.setCpeCasProcessors(cpeCasProcs);
      cpeCasProcs.setPoolSize(3);
    } catch (CpeDescriptorException e) {
      e.printStackTrace(); // this should never happen
    }
    return cpeDesc;
  }

  /**
   * Prompt user for file to save CPE Descriptor to, and do the save.
   */
  private void saveCpeDescriptor() {
    int returnVal = this.openSaveFileChooser.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File f = this.openSaveFileChooser.getSelectedFile();
      // if .xml filter was seleted, add .xml extension if user did not specify an extension
      if (this.openSaveFileChooser.getFileFilter() instanceof XMLFileFilter
              && f.getAbsolutePath().indexOf('.') == -1) {
        f = new File(f.getAbsolutePath() + ".xml");
      }
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      try {
        doSaveCpeDescriptor(f);
        prefs.put(PREFS_CPE_DESCRIPTOR_FILE, f.getAbsolutePath());
      } catch (Exception e) {
        displayError(e);
      } finally {
        setCursor(Cursor.getDefaultCursor());
      }
    }
  }

  private void doSaveCpeDescriptor(File aFile) throws Exception {
    // update the parameter overrides according to GUI settings
    updateCpeDescriptionParameterOverrides();
    
    if (saveUsingImports) {
      //replace <include> with <import>, and update relative import paths
      updateImports(aFile);
    }

    // save descriptor
    OutputStream out = null;
    try {
      File parentFile = aFile.getParentFile();
      if (parentFile != null) {
        parentFile.mkdirs();
      }
      out = new FileOutputStream(aFile);
      currentCpeDesc.toXML(out);
    } finally {
      if (out != null) {
        out.close();
      }
    }

    //mark descriptor with new location, for later import resolution
    currentCpeDesc.setSourceUrl(aFile.toURI().toURL());
    
    clearDirty();
  }

  /**
   * @param file
   */
  private void updateImports(File cpeDescSaveFile) throws Exception {
    CpeCollectionReader[] readers = currentCpeDesc.getAllCollectionCollectionReaders();
    if (readers != null) {
      for (int i = 0; i < readers.length; i++) {
        updateImport(readers[i].getDescriptor(), cpeDescSaveFile);
        if (readers[i].getCasInitializer() != null) {
          updateImport(readers[i].getCasInitializer().getDescriptor(), cpeDescSaveFile);
        }
      }
    }
    CpeCasProcessor[] casProcs = currentCpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();
    for (int i = 0; i < casProcs.length; i++) {
      updateImport(casProcs[i].getCpeComponentDescriptor(), cpeDescSaveFile);
    }
    
  }

  /**
   * @param descriptor
   * @param cpeDescSaveFile
   */
  private void updateImport(CpeComponentDescriptor descriptor, File cpeDescSaveFile) throws Exception {
    //don't touch import by name
    if (descriptor.getImport() != null && descriptor.getImport().getName() != null)
      return;
    
    //for include or import by location, get the absolute URL of the descriptor
    URL descUrl = descriptor.findAbsoluteUrl(defaultResourceManager);
    
    //don't touch URLs with protocol other than file:
    if ("file".equals(descUrl.getProtocol())) {
      File descFile = urlToFile(descUrl);
      //try to find relative path from cpeDescSaveFile to descFile
      String relPath = FileUtils.findRelativePath(descFile, cpeDescSaveFile.getParentFile());
      if (relPath != null) {
        //update CPE descriptor
        descriptor.setInclude(null);
        Import newImport = UIMAFramework.getResourceSpecifierFactory().createImport();
        newImport.setLocation(relPath);
        descriptor.setImport(newImport);
      }
    }    
  }

  /**
   * Utility method for convertion a URL to a File name, taking care of
   * proper escaping.
   * @param url a URL
   * @return File corresponding to that URL
   */
  private File urlToFile(URL url) throws URISyntaxException {
    return new File(UriUtils.quote(url));
  }

  private void displayProgress() {
    if (mCPE != null) {
      try {
        Progress progress[] = mCPE.getProgress();
        if (progress != null && progress.length > 0) {
          int FILE_ENTITY_PROGRESS_INDEX = -1;

          if (FILE_ENTITY_PROGRESS_INDEX == -1) {
            for (int i = 0; i < progress.length; i++) {
              if (progress[i].getUnit().equals(Progress.ENTITIES)) {
                FILE_ENTITY_PROGRESS_INDEX = i;
                break;
              }
            }
          }

          if (FILE_ENTITY_PROGRESS_INDEX >= 0) {  // uima-1086
            int value = (int) progress[FILE_ENTITY_PROGRESS_INDEX].getCompleted();
            progressBar.setValue(value);

            if (progressBar.isIndeterminate())
              statusLabel.setText("Processed " + value);
            else
              statusLabel.setText("Processed " + value + " of " + progressBar.getMaximum());
          }
        }
      } catch (Exception e) {
        displayError(e);
      }
    }
  }

  /** Ask user to confirm exist. Return true if they confirm, false if not. */
  public boolean confirmExit() {
    mShuttingDown = true;
    // ask for confirm if CPM is processing
    if (mCPE != null && mCPE.isProcessing()) {
      int rv = JOptionPane.showConfirmDialog(this,
              "Collection Processing is currently running.  Do you wish to abort?", "Exit",
              JOptionPane.YES_NO_OPTION);
      if (rv == JOptionPane.NO_OPTION) {
        mShuttingDown = false;
        return false;
      } else {
        mCPE.stop();
        resetScreen();
      }
    }

    // ask for confirm if configuration settings have been modified
    try {
      if (isDirty()) {
        int rv = JOptionPane.showConfirmDialog(this, "Configuration settings have been modified. "
                + "Would you like to save the CPE descriptor?", "Exit",
                JOptionPane.YES_NO_CANCEL_OPTION);

        if (rv == JOptionPane.CANCEL_OPTION) {
          mShuttingDown = false;
          return false;
        }
        if (rv == JOptionPane.YES_OPTION) {
          saveCpeDescriptor();
        }
      }

      // exit
      return true;
    } catch (Exception e) {
      displayError(e);
      return true;
    }
  }

  public void checkForOutOfSyncFiles() {
    StringBuffer componentNames = new StringBuffer();

    if (collectionReaderDesc != null) {
      File readerSpecifierFile = new File(readerFileSelector.getSelected());
      if (readerSpecifierFile.lastModified() > this.collectionReaderLastFileSyncTimestamp
              && readerSpecifierFile.lastModified() > this.lastFileSyncUserPromptTime) {
        componentNames.append(collectionReaderDesc.getMetaData().getName()).append('\n');
      }
    }

    if (casInitializerDesc != null) {
      File casInitializerSpecifierFile = new File(casInitializerFileSelector.getSelected());
      if (casInitializerSpecifierFile.lastModified() > this.casInitializerLastFileSyncTimestamp
              && casInitializerSpecifierFile.lastModified() > this.lastFileSyncUserPromptTime) {
        componentNames.append(casInitializerDesc.getMetaData().getName()).append('\n');
      }
    }

    int nrTabs = aeTabbedPane.getTabCount();
    for (int i = 0; i < nrTabs; i++) {
      AnalysisEnginePanel aePanel = (AnalysisEnginePanel) aeTabbedPane.getComponentAt(i);
      if (aePanel.hasFileChanged(this.lastFileSyncUserPromptTime)) {
        componentNames.append(aeTabbedPane.getTitleAt(i)).append('\n');
      }
    }

    nrTabs = consumerTabbedPane.getTabCount();
    for (int i = 0; i < nrTabs; i++) {
      ConsumerPanel consumerPanel = (ConsumerPanel) consumerTabbedPane.getComponentAt(i);
      if (consumerPanel.hasFileChanged(this.lastFileSyncUserPromptTime)) {
        componentNames.append(consumerTabbedPane.getTitleAt(i)).append('\n');
      }
    }

    if (componentNames.length() > 0) {
      int rv = JOptionPane.showConfirmDialog(this,
              "The following descriptor(s) have changed on the file system:\n"
                      + componentNames.toString() + "\n\nDo you want to refresh them?",
              "Descriptors Changed On File System", JOptionPane.YES_NO_OPTION);
      if (rv == JOptionPane.YES_OPTION) {
        refreshOutOfSyncFiles();
      }
      // record this time so that we don't re-prompt user continuously
      this.lastFileSyncUserPromptTime = System.currentTimeMillis();
    }
  }

  public void refreshOutOfSyncFiles() {
    if (collectionReaderDesc != null) {
      File readerSpecifierFile = new File(readerFileSelector.getSelected());
      if (readerSpecifierFile.lastModified() > this.collectionReaderLastFileSyncTimestamp) {
        try {
          populateCollectionReaderPanel(currentCpeDesc.getAllCollectionCollectionReaders()[0]
                  .getCollectionIterator());
        } catch (Exception e) {
          displayError(e);
        }
      }
    }

    if (casInitializerDesc != null) {
      File casInitializerSpecifierFile = new File(casInitializerFileSelector.getSelected());
      if (casInitializerSpecifierFile.lastModified() > this.casInitializerLastFileSyncTimestamp) {
        try {
          populateCasInitializerPanel(currentCpeDesc.getAllCollectionCollectionReaders()[0]
                  .getCasInitializer());
        } catch (Exception e) {
          displayError(e);
        }
      }
    }

    int nrTabs = aeTabbedPane.getTabCount();
    for (int i = 0; i < nrTabs; i++) {
      AnalysisEnginePanel aePanel = (AnalysisEnginePanel) aeTabbedPane.getComponentAt(i);
      if (aePanel.hasFileChanged(0)) {
        try {
          aePanel.refreshFromFile();
        } catch (Exception e) {
          displayError(e);
        }
      }
    }

    nrTabs = consumerTabbedPane.getTabCount();
    for (int i = 0; i < nrTabs; i++) {
      ConsumerPanel consumerPanel = (ConsumerPanel) consumerTabbedPane.getComponentAt(i);
      if (consumerPanel.hasFileChanged(0)) {
        try {
          consumerPanel.refreshFromFile();
        } catch (Exception e) {
          displayError(e);
        }
      }
    }
  }

  /**
   * Displays an error message to the user.
   * 
   * @param aErrorString
   *          error message to display
   */
  public void displayError(String aErrorString) {
    // word-wrap long mesages
    StringBuffer buf = new StringBuffer(aErrorString.length());
    final int CHARS_PER_LINE = 80;
    int charCount = 0;
    StringTokenizer tokenizer = new StringTokenizer(aErrorString, " \n", true);

    while (tokenizer.hasMoreTokens()) {
      String tok = tokenizer.nextToken();

      if (tok.equals("\n")) {
        buf.append("\n");
        charCount = 0;
      } else if ((charCount > 0) && ((charCount + tok.length()) > CHARS_PER_LINE)) {
        buf.append("\n").append(tok);
        charCount = tok.length();
      } else {
        buf.append(tok);
        charCount += tok.length();
      }
    }

    JOptionPane.showMessageDialog(this, buf.toString(), "Error", JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Displays an error message to the user.
   * 
   * @param aThrowable
   *          Throwable whose message is to be displayed.
   */
  public void displayError(Throwable aThrowable) {
    aThrowable.printStackTrace();

    String message = aThrowable.toString();

    // For UIMAExceptions or UIMARuntimeExceptions, add cause info.
    // We have to go through this nonsense to support Java 1.3.
    // In 1.4 all exceptions can have a cause, so this wouldn't involve
    // all of this typecasting.
    while ((aThrowable instanceof UIMAException) || (aThrowable instanceof UIMARuntimeException)) {
      if (aThrowable instanceof UIMAException) {
        aThrowable = ((UIMAException) aThrowable).getCause();
      } else if (aThrowable instanceof UIMARuntimeException) {
        aThrowable = ((UIMARuntimeException) aThrowable).getCause();
      }

      if (aThrowable != null) {
        message += ("\nCausedBy: " + aThrowable.toString());
      }
    }

    displayError(message);
  }

  private boolean populateCollectionReaderPanel(CpeCollectionReaderIterator cpeColRdr)
          throws InvalidXMLException, IOException, ResourceConfigurationException {
    try {
      URL specifierUrl = null;
      String specifierFile = null;
      if (cpeColRdr != null) {
        specifierUrl = cpeColRdr.getDescriptor().findAbsoluteUrl(defaultResourceManager);
        //CPE GUI only supports file URLs
        if (!"file".equals(specifierUrl.getProtocol())) {
          displayError("Could not load descriptor from URL " + specifierUrl.toString() + 
                  ".  CPE Configurator only supports file: URLs");
          return false;
        }  
        try {
          specifierFile = urlToFile(specifierUrl).toString();
        } catch (URISyntaxException e) {
          displayError(e);
          return false;
        }
      }

      if (collectionReaderPanel.getNrComponents() == 0) {
        collectionReaderPanel.add(new JLabel("Descriptor:"));
        collectionReaderPanel.add(readerFileSelector);
        readerFileSelector.setSelected(specifierFile);
      } else {
        collectionReaderPanel.reset();
      }

      if (specifierFile == null || specifierFile.length() == 0) {
        // no CollectionReader selected (allowed, as interemediate state)
        collectionReaderDesc = null;
        return true;
      }

      if (specifierFile != null && specifierFile.length() > 0) {
        File f = new File(specifierFile);
        if (!f.exists()) {
          String errorMsg = "Descriptor file " + f.getAbsolutePath() + " does not exist";
          JOptionPane.showMessageDialog(this, errorMsg, null, JOptionPane.ERROR_MESSAGE);
          transportControlPanel.reset();
          resetScreen();
          return false;
        } else {
          collectionReaderLastFileSyncTimestamp = f.lastModified();
          XMLInputSource readerInputSource = new XMLInputSource(f);
          collectionReaderDesc = UIMAFramework.getXMLParser().parseCollectionReaderDescription(
                  readerInputSource);
          collectionReaderPanel.populate(collectionReaderDesc.getMetaData(), cpeColRdr
                  .getConfigurationParameterSettings());
        }
      }
      return true;
    } finally {
      collectionReaderPanel.validate();
      collectionReaderPanel.validate();
      mainSplitPane.validate();
    }
  }

  private boolean populateCasInitializerPanel(CpeCollectionReaderCasInitializer cpeCasIni)
          throws InvalidXMLException, IOException, ResourceConfigurationException {
    try {
      URL specifierUrl = null;
      String specifierFile = null;
      if (cpeCasIni != null) {
        specifierUrl = cpeCasIni.getDescriptor().findAbsoluteUrl(defaultResourceManager);
        //CPE GUI only supports file URLs
        if (!"file".equals(specifierUrl.getProtocol())) {
          displayError("Could not load descriptor from URL " + specifierUrl.toString() + 
                  ".  CPE Configurator only supports file: URLs");
          return false;
        }  
        try {
          specifierFile = urlToFile(specifierUrl).toString();
        } catch (URISyntaxException e) {
          displayError(e);
          return false;
        }
      }

      if (casInitializerPanel.getNrComponents() == 0) {
        casInitializerPanel.add(new JLabel("Descriptor:"));

        casInitializerPanel.add(casInitializerFileSelector);
        casInitializerFileSelector.setSelected(specifierFile);
      } else {
        casInitializerPanel.reset();
      }

      if (specifierFile == null || specifierFile.length() == 0) {
        // no CAS initializer selected - this is OK
        casInitializerDesc = null;
        return true;
      }

      // a CAS initializer is selected, so make sure the panel is made visible
      setCasInitializerPanelVisible(true);

      File f = new File(specifierFile);
      if (!f.exists()) {
        String errorMsg = "Descriptor file " + f.getAbsolutePath() + " does not exist";
        JOptionPane.showMessageDialog(this, errorMsg, null, JOptionPane.ERROR_MESSAGE);
        transportControlPanel.reset();
        resetScreen();
        return false;
      } else {
        casInitializerLastFileSyncTimestamp = f.lastModified();
        XMLInputSource casIniInputSource = new XMLInputSource(f);
        casInitializerDesc = UIMAFramework.getXMLParser().parseCasInitializerDescription(
                casIniInputSource);

        casInitializerPanel.populate(casInitializerDesc.getMetaData(), cpeCasIni
                .getConfigurationParameterSettings());
        return true;
      }
    } finally {
      casInitializerPanel.validate();
      mainSplitPane.validate();
    }
  }

  private void addAE(String aeSpecifierFile) throws CpeDescriptorException, InvalidXMLException,
          IOException, ResourceConfigurationException {
    String tempAeName = new File(aeSpecifierFile).getName(); // overriden later
    CpeCasProcessor casProc = CpeDescriptorFactory.produceCasProcessor(tempAeName);
    casProc.setDescriptor(aeSpecifierFile);
    casProc.setBatchSize(10000);
    casProc.getErrorHandling().getErrorRateThreshold().setMaxErrorCount(0);

    // add to pipeline as last AE but before CAS Consumers
    currentCpeDesc.addCasProcessor(aeTabbedPane.getTabCount(), casProc);

    // update GUI
    addAE(casProc);
  }

  private boolean addAE(CpeCasProcessor cpeCasProc) throws CpeDescriptorException,
          InvalidXMLException, IOException, ResourceConfigurationException {
    URL aeSpecifierUrl = cpeCasProc.getCpeComponentDescriptor().findAbsoluteUrl(defaultResourceManager);
    //CPE GUI only supports file URLs
    if (!"file".equals(aeSpecifierUrl.getProtocol())) {
      displayError("Could not load descriptor from URL " + aeSpecifierUrl.toString() + 
              ".  CPE Configurator only supports file: URLs");
      return false;
    }  
    File f;
    try {
      f = urlToFile(aeSpecifierUrl);
    } catch (URISyntaxException e) {
      displayError(e);
      return false;
    }
    long fileModStamp = f.lastModified(); // get mod stamp before parsing, to prevent race condition
    XMLInputSource aeInputSource = new XMLInputSource(aeSpecifierUrl);
    ResourceSpecifier aeSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(
            aeInputSource);

    AnalysisEnginePanel aePanel = new AnalysisEnginePanel(aeSpecifier, f, fileModStamp);
    String tabName;
    if (aeSpecifier instanceof AnalysisEngineDescription) {
      AnalysisEngineDescription aeDescription = (AnalysisEngineDescription) aeSpecifier;
      ResourceMetaData md = aeDescription.getMetaData();
      aePanel.populate(md, cpeCasProc.getConfigurationParameterSettings());
      tabName = md.getName();
    } else {
      tabName = f.getName();
    }

    tabName = makeUniqueCasProcessorName(tabName);
    cpeCasProc.setName(tabName);
    aeTabbedPane.addTab(tabName, aePanel);
    aeSpecifiers.add(f.getAbsolutePath());

    selectedComponentsChanged = true;
    return true;
  }

  private void addConsumer(String consumerSpecifierFile) throws CpeDescriptorException,
          InvalidXMLException, IOException, ResourceConfigurationException {
    String tempName = new File(consumerSpecifierFile).getName(); // overriden later
    CpeCasProcessor casProc = CpeDescriptorFactory.produceCasProcessor(tempName);
    casProc.setDescriptor(consumerSpecifierFile);
    casProc.setBatchSize(10000);
    casProc.getErrorHandling().getErrorRateThreshold().setMaxErrorCount(0);

    // add to pipeline as last CAS Processor
    currentCpeDesc.addCasProcessor(casProc);
    // update GUI
    addConsumer(casProc);
  }

  private boolean addConsumer(CpeCasProcessor cpeCasProc) throws CpeDescriptorException,
          InvalidXMLException, IOException, ResourceConfigurationException {
    URL consumerSpecifierUrl = cpeCasProc.getCpeComponentDescriptor().findAbsoluteUrl(
            defaultResourceManager);
    //CPE GUI only supports file URLs
    if (!"file".equals(consumerSpecifierUrl.getProtocol())) {
      displayError("Could not load descriptor from URL " + consumerSpecifierUrl.toString() + 
              ".  CPE Configurator only supports file: URLs");
      return false;
    }  
    File f;
    try {
      f = urlToFile(consumerSpecifierUrl);
    } catch (URISyntaxException e) {
      displayError(e);
      return false;
    }

    long fileModStamp = f.lastModified(); // get mod stamp before parsing, to prevent race condition
    XMLInputSource consumerInputSource = new XMLInputSource(consumerSpecifierUrl);
    ResourceSpecifier casConsumerSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(
            consumerInputSource);
    ConsumerPanel consumerPanel = new ConsumerPanel(casConsumerSpecifier, f, fileModStamp);

    String tabName;
    if (casConsumerSpecifier instanceof CasConsumerDescription) {
      ResourceMetaData md = ((CasConsumerDescription) casConsumerSpecifier)
              .getCasConsumerMetaData();
      consumerPanel.populate(md, cpeCasProc.getConfigurationParameterSettings());
      tabName = md.getName();
    } else {
      tabName = f.getName();
    }

    tabName = makeUniqueCasProcessorName(tabName);
    cpeCasProc.setName(tabName);
    consumerTabbedPane.addTab(tabName, consumerPanel);
    consumerSpecifiers.add(f.getAbsolutePath());
    selectedComponentsChanged = true;
    return true;
  }

  private String makeUniqueCasProcessorName(String baseName) {
    if (aeTabbedPane.indexOfTab(baseName) == -1 && consumerTabbedPane.indexOfTab(baseName) == -1)
      return baseName;

    int num = 2;
    while (true) {
      String name = baseName + " " + num;
      if (aeTabbedPane.indexOfTab(name) == -1 && consumerTabbedPane.indexOfTab(name) == -1)
        return name;
      num++;
    }
  }

  private void removeAllAEsAndConsumers() {
    aeTabbedPane.removeAll();
    aeSpecifiers.clear();

    consumerTabbedPane.removeAll();
    consumerSpecifiers.clear();
  }

  // FileSelectorListener:
  public boolean fileSelected(JComponent source, String fileString) {

    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    boolean rv = false;

    try {
      if (source == collectionReaderPanel) {
        if (fileString != null && fileString.length() > 0) {
          // [UIMA-2841]
          currentCpeDesc.setAllCollectionCollectionReaders(null);
          currentCpeDesc.addCollectionReader(fileString);
          CpeCollectionReader[] readers = currentCpeDesc.getAllCollectionCollectionReaders();
          
//          CpeCollectionReader[] readers = currentCpeDesc.getAllCollectionCollectionReaders();
//          if (readers.length == 0) {
//            currentCpeDesc.addCollectionReader(fileString);
//            readers = currentCpeDesc.getAllCollectionCollectionReaders();
//          } else {
//            readers[0].getCollectionIterator().getDescriptor().getInclude().set(fileString);
//            readers[0].getCollectionIterator().setConfigurationParameterSettings(null);
//          }

          rv = populateCollectionReaderPanel(readers[0].getCollectionIterator());
        } else {
          // no Collection Reader selected - allowed as intermeidate state
          currentCpeDesc.setAllCollectionCollectionReaders(new CpeCollectionReader[0]);
        }
        selectedComponentsChanged = true;
      } else if (source == casInitializerPanel) {
        if (fileString != null && fileString.length() > 0) {
          // [UIMA-2841]
          currentCpeDesc.setAllCollectionCollectionReaders(null);
          currentCpeDesc.addCasInitializer(fileString);
          CpeCollectionReader[] readers = currentCpeDesc.getAllCollectionCollectionReaders();
          
//          CpeCollectionReader[] readers = currentCpeDesc.getAllCollectionCollectionReaders();
//          if (readers.length == 0 || readers[0].getCasInitializer() == null) {
//            currentCpeDesc.addCasInitializer(fileString);
//            readers = currentCpeDesc.getAllCollectionCollectionReaders();
//          } else {
//            readers[0].getCasInitializer().getDescriptor().getInclude().set(fileString);
//            // clear config settings
//            readers[0].getCasInitializer().setConfigurationParameterSettings(null);
//          }
          rv = populateCasInitializerPanel(readers[0].getCasInitializer());
        } else {
          // no CAS initializer selected - OK
          CpeCollectionReader[] readers = currentCpeDesc.getAllCollectionCollectionReaders();
          if (readers.length > 0) {
            readers[0].removeCasInitializer();
          }
          rv = populateCasInitializerPanel(null);
        }
        selectedComponentsChanged = true;
      }
    } catch (Exception e) {
      displayError(e);
      rv = false;
    }

    setCursor(Cursor.getDefaultCursor());

    return rv;
  }

  // TabClosedListener:
  public void tabClosed(JTabbedPaneWithCloseIcons source, int tabPos) {
    try {
      if (source == consumerTabbedPane) {
        currentCpeDesc.getCpeCasProcessors().removeCpeCasProcessor(aeSpecifiers.size() + tabPos);
        consumerSpecifiers.remove(tabPos);
      } else if (source == aeTabbedPane) {
        currentCpeDesc.getCpeCasProcessors().removeCpeCasProcessor(tabPos);
        aeSpecifiers.remove(tabPos);
      }
      selectedComponentsChanged = true;
    } catch (CpeDescriptorException e) {
      displayError(e);
    }
  }

  public void controlStarted() {
    statusLabel.setText("Initializing");
    progressBar.setIndeterminate(true);
    setFrameEnabled(false);
  }

  public void controlPaused() {
    mPaused = true;
    statusLabel.setText("Paused");

    indeterminateProgressPause = progressBar.isIndeterminate();
    if (indeterminateProgressPause)
      progressBar.setIndeterminate(false);

    if (!mCPE.isPaused())
      mCPE.pause();

    progressTimer.stop();
    performanceQueryTimer.stop();

    PerformanceReportDialog perfReportDlg = new PerformanceReportDialog(CpmPanel.this
            .getParentFrame());
    perfReportDlg.displayStats(mCPE.getPerformanceReport(), progressBar.getValue(),
            "Processing is paused.");
  }

  public void controlResumed() {
    mPaused = false;
    statusLabel.setText("Resumed");
    if (indeterminateProgressPause)
      progressBar.setIndeterminate(true);

    mCPE.resume();

    progressTimer.restart();
    performanceQueryTimer.restart();
  }

  public void controlStopped() {
    try {
      mCPE.stop();
    } catch (UIMA_IllegalStateException e) {
      // already stopped - OK
    }
  }

  private void resetScreen() {
    progressTimer.stop();
    performanceQueryTimer.stop();
    transportControlPanel.reset();
    setFrameEnabled(true);

    progressBar.setIndeterminate(false);
    progressBar.setValue(0);

    elapsedTime = 0;

    statusLabel.setText("");
  }

  /**
   * Called to lock the GUI while processing is occurring. We don't actually disable the JFrame,
   * because we don't want to disable the stop/pause buttons. Instead we disable all of the controls
   * that the user shouldn't mess with while processing is occurring.
   * 
   * @param onOff
   *          true to enable, false to disable
   */
  private void setFrameEnabled(boolean onOff) {
    Color titleColor = (onOff ? Color.black : Color.gray);

    // int nrMenuItems = menuBar.getMenuCount();
    // for (int i = 0; i < (nrMenuItems - 2); i++)
    // menuBar.getMenu(i).setEnabled(onOff);

    readerInitializerSplitPane.setEnabled(onOff);
    collectionReaderPanel.setEnabled(onOff);
    collectionReaderTitledBorder.setTitleColor(titleColor);
    casInitializerPanel.setEnabled(onOff);
    casInitializerTitledBorder.setTitleColor(titleColor);
    aeMainPanel.setEnabled(onOff);
    addAeButton.setEnabled(onOff);
    moveAeRightButton.setEnabled(onOff);
    moveAeLeftButton.setEnabled(onOff);
    aeTitledBorder.setTitleColor(titleColor);
    aeTabbedPane.setEnabled(onOff);

    // Cycle through each of the AE panels:
    int nrTabs = aeTabbedPane.getTabCount();
    for (int i = 0; i < nrTabs; i++) {
      AnalysisEnginePanel aePanel = (AnalysisEnginePanel) aeTabbedPane.getComponentAt(i);
      aePanel.setEnabled(onOff);
    }

    consumersPanel.setEnabled(onOff);
    consumerTitledBorder.setTitleColor(titleColor);
    addConsumerButton.setEnabled(onOff);
    moveConsumerRightButton.setEnabled(onOff);
    moveConsumerLeftButton.setEnabled(onOff);

    consumerTabbedPane.setEnabled(onOff);

    // Cycle through each of the consumer panels:
    nrTabs = consumerTabbedPane.getTabCount();
    for (int i = 0; i < nrTabs; i++) {
      ConsumerPanel consumerPanel = (ConsumerPanel) consumerTabbedPane.getComponentAt(i);
      consumerPanel.setEnabled(onOff);
    }

    // Cursor cursor = (onOff ? Cursor.getDefaultCursor() : Cursor
    // .getPredefinedCursor(Cursor.WAIT_CURSOR));
    // setCursor(cursor);
    if (openCpeDescMenuItem != null)
      openCpeDescMenuItem.setEnabled(onOff);
    if (saveCpeDescMenuItem != null)
      saveCpeDescMenuItem.setEnabled(onOff);
    if (refreshMenuItem != null)
      refreshMenuItem.setEnabled(onOff);
    if (clearAllMenuItem != null)
      clearAllMenuItem.setEnabled(onOff);
  }

  public void onCompletion() {
    try {
      // statusLabel.setText("Completed (" + statusLabel.getText() + ")");
      setFrameEnabled(true);
      transportControlPanel.reset();
      displayProgress();
      progressBar.setIndeterminate(false);

      elapsedTime = 0;
      progressTimer.stop();

      performanceQueryTimer.stop();

      PerformanceReportDialog perfReportDlg = new PerformanceReportDialog(this.getParentFrame());
      perfReportDlg.displayStats(mCPE.getPerformanceReport(), progressBar.getValue(),
              "Processing completed successfully.");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void printStats() {
    // logDialog.write( mCPM.getPerformanceReport().toString());
  }

  /**
   * Returns whether the GUI is dirty; that is, whether configuration settings have been modified
   * since the last save.
   * 
   * @return whether the GUI is dirty
   */
  private boolean isDirty() {
    // have components been added or removed?
    if (selectedComponentsChanged)
      return true;

    // has configuration of any component changed?
    if (collectionReaderPanel.isDirty() || casInitializerPanel.isDirty()) {
      return true;
    }
    for (int i = 0; i < aeTabbedPane.getTabCount(); i++) {
      MetaDataPanel panel = (MetaDataPanel) aeTabbedPane.getComponentAt(i);
      if (panel.isDirty())
        return true;
    }
    for (int i = 0; i < consumerTabbedPane.getTabCount(); i++) {
      MetaDataPanel panel = (MetaDataPanel) consumerTabbedPane.getComponentAt(i);
      if (panel.isDirty())
        return true;
    }

    return false;
  }

  private void openCpeDescriptor(File aFile) throws InvalidXMLException, IOException,
          CpeDescriptorException, ResourceConfigurationException {
    // parse
    currentCpeDesc = UIMAFramework.getXMLParser().parseCpeDescription(new XMLInputSource(aFile));

    // update GUI
    // Collection Reader
    CpeCollectionReader[] collRdrs = currentCpeDesc.getAllCollectionCollectionReaders(); // more
                                                                                          // than
                                                                                          // one??
    CpeCollectionReader collRdr = null;
    if (collRdrs != null && collRdrs.length > 0) {
      collRdr = collRdrs[0];
      collectionReaderPanel.clearAll();
      populateCollectionReaderPanel(collRdr.getCollectionIterator());
    } else {
      collectionReaderPanel.reset();
    }
    // CAS Initializer
    CpeCollectionReaderCasInitializer casIni = null;
    if (collRdr != null) {
      casIni = collRdr.getCasInitializer();
    }
    if (casIni != null) {
      casInitializerPanel.clearAll();
      populateCasInitializerPanel(casIni);
    } else {
      casInitializerPanel.reset();
    }

    // CAS Processors
    int numFailed = 0;
    removeAllAEsAndConsumers();
    CpeCasProcessor[] casProcs = currentCpeDesc.getCpeCasProcessors().getAllCpeCasProcessors();
    for (int i = 0; i < casProcs.length; i++) {
      boolean success = true;
      try {
        URL specifierUrl = casProcs[i].getCpeComponentDescriptor().findAbsoluteUrl(defaultResourceManager);
        ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(
                new XMLInputSource(specifierUrl));
        if (isCasConsumerSpecifier(specifier)) {
          success = addConsumer(casProcs[i]);
        } else {
          success = addAE(casProcs[i]);
        }
      }
      catch(Exception e) {
        System.err.println("Error loading CPE Descriptor " + aFile.getPath());
        e.printStackTrace();        
        success= false;
      }
      if (!success) {
        currentCpeDesc.getCpeCasProcessors().removeCpeCasProcessor(i - numFailed);
        numFailed++;
      }
    }    
    
    prefs.put(PREFS_CPE_DESCRIPTOR_FILE, aFile.getAbsolutePath());

    // nothing should be dirty when we first open
    clearDirty();
  }

  /**
   * @param specifier
   */
  private boolean isCasConsumerSpecifier(ResourceSpecifier specifier) {
    if (specifier instanceof CasConsumerDescription) {
      return true;
    } else if (specifier instanceof URISpecifier) {
      URISpecifier uriSpec = (URISpecifier) specifier;
      return URISpecifier.RESOURCE_TYPE_CAS_CONSUMER.equals(uriSpec.getResourceType());
    } else
      return false;
  }

  class StatusCallbackListenerImpl implements StatusCallbackListener {
    public void initializationComplete() {
      // init progress bar
      int nrFiles = -1;
      Progress progress[] = mCPE.getProgress();
      if (progress != null) {
        for (int i = 0; i < progress.length; i++) {
          if (progress[i].getUnit().equals(Progress.ENTITIES)) {
            nrFiles = (int) progress[i].getTotal();
            break;
          }
        }
      }
      if (nrFiles != -1) {
        progressBar.setMaximum(nrFiles);
        progressBar.setIndeterminate(false);
      } else
        progressBar.setIndeterminate(true);

      progressBar.setValue(0);

      // start progress timer which will update the progress bar
      progressTimer.start();
    }

    public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
      // report an error if it occurred
      if (aStatus.isException()) {
        List ex = aStatus.getExceptions();
        displayError((Throwable) ex.get(0));
      }
    }

    public void batchProcessComplete() {
    }

    public void collectionProcessComplete() {
      onCompletion();
    }

    public void paused() {
      System.out.println("StatusCallbackListenerImpl::paused()");
    }

    public void resumed() {
      System.out.println("StatusCallbackListenerImpl::resumed");
    }

    public void aborted() {
      if (!mShuttingDown && !mPaused) {
        PerformanceReportDialog perfReportDlg = new PerformanceReportDialog(CpmPanel.this
                .getParentFrame());
        perfReportDlg.displayStats(mCPE.getPerformanceReport(), progressBar.getValue(),
                "Processing aborted.");
      }
      resetScreen();
      mPaused = false;
    }
  }

  /**
   * MetaDataPanel used for Collection Reader &amp; AE selection and configuration. Adds a reset method
   * to clear out related components when specifier file selection changes.
   */
  static class ResetableMetaDataPanel extends MetaDataPanel {
    private static final long serialVersionUID = -4573780175511175666L;

    public ResetableMetaDataPanel() {
      super();
    }

    public ResetableMetaDataPanel(int nrColumns) {
      super(nrColumns);
    }

    /**
     * Clears all but the first 2 components from the panel.
     */
    public void reset() {
      Component components[] = gridBagPanel.getComponents();
      if (components.length > 2) {
        for (int i = (components.length - 1); i >= 2; i--)
          gridBagPanel.remove(i);

        componentIndex = 2;
      }
      fieldsList.clear();
    }
  }

}
