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

package org.apache.uima.tools.cvd;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.LogManager;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.internal.util.Timer;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.tools.cpm.PerformanceReportDialog;
import org.apache.uima.tools.cvd.control.AboutHandler;
import org.apache.uima.tools.cvd.control.AboutUimaHandler;
import org.apache.uima.tools.cvd.control.AddLanguageHandler;
import org.apache.uima.tools.cvd.control.AnnotatorOpenEventHandler;
import org.apache.uima.tools.cvd.control.AnnotatorRerunEventHandler;
import org.apache.uima.tools.cvd.control.AnnotatorRunCPCEventHandler;
import org.apache.uima.tools.cvd.control.AnnotatorRunOnCasEventHandler;
import org.apache.uima.tools.cvd.control.CaretChangeHandler;
import org.apache.uima.tools.cvd.control.CloseLogViewHandler;
import org.apache.uima.tools.cvd.control.ColorPrefsOpenHandler;
import org.apache.uima.tools.cvd.control.ColorPrefsSaveHandler;
import org.apache.uima.tools.cvd.control.FSTreeSelectionListener;
import org.apache.uima.tools.cvd.control.FileOpenEventHandler;
import org.apache.uima.tools.cvd.control.FileSaveAsEventHandler;
import org.apache.uima.tools.cvd.control.FileSaveEventHandler;
import org.apache.uima.tools.cvd.control.FocusFSAction;
import org.apache.uima.tools.cvd.control.FocusIRAction;
import org.apache.uima.tools.cvd.control.FocusTextAction;
import org.apache.uima.tools.cvd.control.HelpHandler;
import org.apache.uima.tools.cvd.control.IndexPopupListener;
import org.apache.uima.tools.cvd.control.IndexTreeSelectionListener;
import org.apache.uima.tools.cvd.control.LoadRecentDescFileEventHandler;
import org.apache.uima.tools.cvd.control.LoadRecentTextFileEventHandler;
import org.apache.uima.tools.cvd.control.MainFrameClosing;
import org.apache.uima.tools.cvd.control.ManualHandler;
import org.apache.uima.tools.cvd.control.NewTextEventHandler;
import org.apache.uima.tools.cvd.control.PopupHandler;
import org.apache.uima.tools.cvd.control.PopupListener;
import org.apache.uima.tools.cvd.control.RemoveLanguageHandler;
import org.apache.uima.tools.cvd.control.RestoreLangDefaultsHandler;
import org.apache.uima.tools.cvd.control.SetCodePageHandler;
import org.apache.uima.tools.cvd.control.SetDataPathHandler;
import org.apache.uima.tools.cvd.control.SetLanguageHandler;
import org.apache.uima.tools.cvd.control.SetLogConfigHandler;
import org.apache.uima.tools.cvd.control.ShowAnnotatedTextHandler;
import org.apache.uima.tools.cvd.control.ShowAnnotationCustomizerHandler;
import org.apache.uima.tools.cvd.control.ShowTypesystemHandler;
import org.apache.uima.tools.cvd.control.SofaSelectionListener;
import org.apache.uima.tools.cvd.control.SystemExitHandler;
import org.apache.uima.tools.cvd.control.TextChangedListener;
import org.apache.uima.tools.cvd.control.TextContextMenuAction;
import org.apache.uima.tools.cvd.control.TextFocusHandler;
import org.apache.uima.tools.cvd.control.TreeFocusHandler;
import org.apache.uima.tools.cvd.control.TypeSystemFileOpenEventHandler;
import org.apache.uima.tools.cvd.control.UndoMgr;
import org.apache.uima.tools.cvd.control.XCASFileOpenEventHandler;
import org.apache.uima.tools.cvd.control.XCASSaveHandler;
import org.apache.uima.tools.cvd.control.XCASSaveTSHandler;
import org.apache.uima.tools.cvd.control.XmiCasFileOpenHandler;
import org.apache.uima.tools.cvd.control.XmiCasSaveHandler;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.XMLInputSource;

/**
 * Class comment for MainFrame.java goes here.
 * 
 * 
 */
public class MainFrame extends JFrame {

  private static final long serialVersionUID = -1357410768440678886L;

  public static List<Level> logLevels = new ArrayList<Level>(9);
  static {
    logLevels.add(Level.OFF);
    logLevels.add(Level.SEVERE);
    logLevels.add(Level.WARNING);
    logLevels.add(Level.INFO);
    logLevels.add(Level.CONFIG);
    logLevels.add(Level.FINE);
    logLevels.add(Level.FINER);
    logLevels.add(Level.FINEST);
    logLevels.add(Level.ALL);
  }

  private static final String loggerPropertiesFileName = "org/apache/uima/tools/annot_view/Logger.properties";

  private static final String defaultText =
  "Load or edit text here.";

  private static final String titleText = "CAS Visual Debugger (CVD)";

  static final String htmlGrayColor = "<font color=#808080>";

  private static final String indexReposRootLabel = "<html><b>CAS Index Repository</b></html>";

  private static final String noIndexReposLabel = "<html><b>" + htmlGrayColor
      + "CAS Index Repository</b></html>";

  // The content areas.
  private JTextArea textArea;

  private JTree indexTree;

  private JTree fsTree;

  private JPanel statusPanel;

  private JTextField statusBar;

  private JTextField fileStatus;

  private JTextField aeStatus;

  private JTextField caretStatus;

  private Border textTitleBorder;

  // Dirty flag for the editor.
  private boolean isDirty;

  // The scroll panels.
  private JScrollPane textScrollPane;

  private JScrollPane indexTreeScrollPane;

  private JScrollPane fsTreeScrollPane;

  // Menus
  private JMenu fileMenu = null;

  private JMenuItem fileSaveItem = null;

  private JMenu editMenu;

  private JMenuItem undoItem;

  private UndoMgr undoMgr;

  private Action cutAction;

  private Action copyAction;

  private JMenuItem allAnnotationViewerItem;

  private JMenuItem acdItem;

  private JMenuItem tsViewerItem;

  private JMenuItem reRunMenu;

  private JMenuItem runCPCMenu;

  private JMenuItem runOnCasMenuItem;

  private JMenuItem showPerfReportItem;

  private JPopupMenu textPopup;

  private JMenuItem xcasReadItem;

  private JMenuItem xcasWriteItem;

  private JMenuItem xmiCasReadItem;

  private JMenuItem xmiCasWriteItem;

  private JMenuItem typeSystemWriteItem;

  private JMenuItem typeSystemReadItem;

  private JMenu recentTextFileMenu;

  private JMenu recentDescFileMenu;

  // Ini file
  private File iniFile = null;

  // Code pages

  private List<String> codePages = null;

  private String codePage = null;

  private JMenu cpMenu;

  private ButtonGroup cpButtons;

  // Language support
  String languagePrefsList = null;

  // private String defaultLanguagePref = null;
  private List<String> languages = null;

  private JMenu langMenu;

  private ButtonGroup langButtons;

  private static final String LANGUAGE_DEFAULT = "en";

  private String language;

  private static final String defaultLanguages = "de,en,fr,ja,ko-kr,pt-br,zh-cn,zh-tw,x-unspecified";

  private File textFile = null;

  private File fileOpenDir = null;

  private File annotOpenDir = null;

  private File xcasFileOpenDir = null;

  private File colorSettingsDir = null;

  // Selected index
  private String indexLabel = null;

  private FSIndex index = null;

  private boolean isAnnotationIndex = false;

  private CAS cas = null;

  private File aeDescriptorFile = null;

  private AnalysisEngine ae = null;

  private File logFile = null;

  private Logger log = null;

  private File colorSettingFile;

  private static final Color selectionColor = Color.orange;

  private Properties preferences;

  private ProcessTrace lastRunProcessTrace = null;

  public static final String textDirPref = "dir.open.text";

  public static final String aeDirPref = "dir.open.tae";

  public static final String xcasDirPref = "dir.open.xcas";

  public static final String textSizePref = "textArea.size";

  public static final String indexTreeSizePref = "indexTree.size";

  public static final String fsTreeSizePref = "fsTree.size";

  public static final String tsWindowSizePref = "tsWindow.size";

  public static final String annotViewSizePref = "annotViewWindow.size";

  public static final String logViewSizePref = "logViewWindow.size";

  public static final String widthSuffix = ".width";

  public static final String heightSuffix = ".height";

  private static final String colorFilePref = "colors.file";

  private static final String colorDirPref = "colors.dir";

  private static final String cpCurrentPref = "cp.selected";

  private static final String langCurrentPref = "lang.selected";

  private static final String langListPref = "lang.list";

  private static final String textFileListPref = "file.text.list";

  private static final String descFileListPref = "file.desc.list";

  private static final String dataPathPref = "datapath";

  private static final Dimension textDimensionDefault = new Dimension(500, 400);

  private static final Dimension fsTreeDimensionDefault = new Dimension(200, 200);

  private static final Dimension logFileDimensionDefault = new Dimension(500, 600);

  public static final String DEFAULT_STYLE_NAME = "defaultStyle";

  private Map<String, Style> styleMap = new HashMap<String, Style>();

  // For recently used text and descriptor files.
  private static final int maxRecentSize = 8;

  private final RecentFilesList recentTextFiles = new RecentFilesList(maxRecentSize);

  private final List<String> textFileNameList = new ArrayList<String>();

  private final RecentFilesList recentDescFiles = new RecentFilesList(maxRecentSize);

  private final List<String> descFileNameList = new ArrayList<String>();

  // For cursor handling (busy cursor). Is there a better way?
  private List<Component> cursorOwningComponents = new ArrayList<Component>();

  private List<Cursor> cursorCache = null;

  private String dataPathName;

  private JComboBox sofaSelectionComboBox;

  private JPanel sofaSelectionPanel;

  private boolean exitOnClose = true;

  /**
   * Constructor for MainFrame.
   * 
   * @throws HeadlessException -
   */
  public MainFrame(File iniFile) {
    super();
    this.iniFile = iniFile;
    init();
  }

  public void runAE(boolean doCasReset) {
    setStatusbarMessage("Running Annotator.");
    Timer timer = new Timer();
    timer.start();
    if (this.ae == null) {
      JOptionPane.showMessageDialog(this, "No AE loaded.", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    internalRunAE(doCasReset);
    timer.stop();
    setStatusbarMessage("Done running AE " + this.ae.getAnalysisEngineMetaData().getName() + " in "
        + timer.getTimeSpan() + ".");
    updateIndexTree(true);
    this.allAnnotationViewerItem.setEnabled(false);
    this.isDirty = false;
    this.runOnCasMenuItem.setEnabled(true);
  }

  public void runCPC() {
    setStatusbarMessage("Running CollectionProcessComplete.");
    Timer timer = new Timer();
    timer.start();
    if (this.ae == null) {
      JOptionPane.showMessageDialog(this, "No AE loaded.", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    try {
      this.ae.collectionProcessComplete();
    } catch (Exception e) {
      handleException(e);
    }
    this.showPerfReportItem.setEnabled(false);
    timer.stop();
    setStatusbarMessage("Done running CPC on " + this.ae.getAnalysisEngineMetaData().getName() + " in "
        + timer.getTimeSpan() + ".");
    updateIndexTree(true);
    this.allAnnotationViewerItem.setEnabled(false);
    this.isDirty = false;
    this.runOnCasMenuItem.setEnabled(true);
  }

  public void setDataPath(String dataPath) {
    this.dataPathName = dataPath;
  }

  public void loadAEDescriptor(File descriptorFile) {
    addRecentDescFile(descriptorFile);
    setWaitCursor();
    if (descriptorFile.exists() && descriptorFile.isFile()) {
      this.annotOpenDir = descriptorFile.getParentFile();
    }
    Timer time = new Timer();
    time.start();
    boolean success = false;
    try {
      success = setupAE(descriptorFile);
    } catch (Exception e) {
      handleException(e);
    } catch (NoClassDefFoundError e) {
      // We don't want to catch all errors, but some are ok.
      handleException(e);
    }
    time.stop();
    if (!success) {
      setStatusbarMessage("Failed to load AE specifier: " + descriptorFile.getName());
      this.reRunMenu.setText("Run AE");
      setAEStatusMessage();
      resetCursor();
      return;
    }
    if (this.ae != null) {
      this.aeDescriptorFile = descriptorFile;
      String annotName = this.ae.getAnalysisEngineMetaData().getName();
      this.reRunMenu.setText("Run " + annotName);
      this.reRunMenu.setEnabled(true);
      this.runOnCasMenuItem.setText("Run " + annotName + " on CAS");
      setAEStatusMessage();
      setStatusbarMessage("Done loading AE " + annotName + " in " + time.getTimeSpan() + ".");
    }
    // Check for CAS multiplier
    // TODO: properly handle CAS multiplication
    if (this.ae != null) {
      if (this.ae.getAnalysisEngineMetaData().getOperationalProperties().getOutputsNewCASes()) {
        JOptionPane
            .showMessageDialog(
                this,
                "This analysis engine uses a CAS multiplier component.\nCAS multiplication/merging is not currently supported in CVD.\nYou can run the analysis engine, but will not see any results.",
                "Warning: unsupported operation", JOptionPane.WARNING_MESSAGE);
      }
    }
    resetCursor();
  }

  public void handleException(Throwable e) {
    StringBuffer msg = new StringBuffer();
    handleException(e, msg);
  }

  protected void handleException(Throwable e, StringBuffer msg) {
    msg.append(e.getClass().getName() + ": ");
    if (e.getMessage() != null) {
      msg.append(e.getMessage());
    }
    if (this.log != null) {
      if (e instanceof Exception) {
        this.log.log(Level.SEVERE, ((Exception) e).getLocalizedMessage(), e);
      } else {
        this.log.log(Level.SEVERE, e.getMessage(), e);
      }
      msg.append("\nMore detailed information is in the log file.");
    }
    boolean hasAsserts = false;
    // assert(hasAsserts = true);
    if (hasAsserts) {
      e.printStackTrace();
    }
    JOptionPane.showMessageDialog(this, msg.toString(), "Exception", JOptionPane.ERROR_MESSAGE);

  }

  private void showError(String msg) {
    JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
  }

  public void loadFile() {
    try {
      if (this.textFile.exists() && this.textFile.canRead()) {
        String text = null;
        if (this.codePage == null) {
          text = FileUtils.file2String(this.textFile);
        } else {
          text = FileUtils.file2String(this.textFile, this.codePage);
        }
        setTextNoTitle(text);
        setTitle();
        addRecentTextFile(this.textFile);
      } else {
        handleException(new IOException("File does not exist or is not readable: "
            + this.textFile.getAbsolutePath()));
      }
      // Add the loaded file to the recently used files list.
    } catch (UnsupportedEncodingException e) {
      StringBuffer msg = new StringBuffer("Unsupported text encoding (code page): ");
      handleException(e, msg);
    } catch (Exception e) {
      handleException(e);
    }
  }

  public void loadXmiFile(File xmiCasFile) {
    try {
      setXcasFileOpenDir(xmiCasFile.getParentFile());
      Timer time = new Timer();
      time.start();
      SAXParserFactory saxParserFactory = XMLUtils.createSAXParserFactory();
      SAXParser parser = saxParserFactory.newSAXParser();
      XmiCasDeserializer xmiCasDeserializer = new XmiCasDeserializer(getCas().getTypeSystem());
      getCas().reset();
      parser.parse(xmiCasFile, xmiCasDeserializer.getXmiCasHandler(getCas(), true));
      time.stop();
      handleSofas();

      setTitle("XMI CAS");
      updateIndexTree(true);
      setRunOnCasEnabled();
      setEnableCasFileReadingAndWriting();
      setStatusbarMessage("Done loading XMI CAS file in " + time.getTimeSpan() + ".");
    } catch (Exception e) {
      e.printStackTrace();
      handleException(e);
    }
  }


  private static final int getMnemonic(int i) {
    switch (i) {
    case 1:
      return KeyEvent.VK_1;
    case 2:
      return KeyEvent.VK_2;
    case 3:
      return KeyEvent.VK_3;
    case 4:
      return KeyEvent.VK_4;
    case 5:
      return KeyEvent.VK_5;
    case 6:
      return KeyEvent.VK_6;
    case 7:
      return KeyEvent.VK_7;
    case 8:
      return KeyEvent.VK_8;
    case 9:
      return KeyEvent.VK_9;
    default:
      return KeyEvent.VK_0;
    }
  }

  private final JMenuItem createRecentTextFileItem(int num, File file) {
    String fileShortName = file.getName();
    JMenuItem item = new JMenuItem(num + " " + fileShortName, getMnemonic(num));
    item.addActionListener(new LoadRecentTextFileEventHandler(this, file.getAbsolutePath()));
    item.setToolTipText(file.getAbsolutePath());
    return item;
  }

  private void addRecentTextFile(File file) {
    this.recentTextFiles.addFile(file);
    this.recentTextFileMenu.removeAll();
    List<File> textFiles = this.recentTextFiles.getFileList();
    for (int i = 0; i < textFiles.size(); i++) {
      JMenuItem menuItem = createRecentTextFileItem(i + 1, textFiles.get(i));
      this.recentTextFileMenu.add(menuItem);
    }
  }

  private final JMenuItem createRecentDescFileItem(int num, File file) {
    String fileShortName = file.getName();
    JMenuItem item = new JMenuItem(num + " " + fileShortName, getMnemonic(num));
    item.addActionListener(new LoadRecentDescFileEventHandler(this, file.getAbsolutePath()));
    item.setToolTipText(file.getAbsolutePath());
    return item;
  }

  private void addRecentDescFile(File file) {
    this.recentDescFiles.addFile(file);
    this.recentDescFileMenu.removeAll();
    List<File> descFiles = this.recentDescFiles.getFileList();
    for (int i = 0; i < descFiles.size(); i++) {
      JMenuItem menuItem = createRecentDescFileItem(i + 1, descFiles.get(i));
      this.recentDescFileMenu.add(menuItem);
    }
  }

  /**
   * Set the text to be analyzed.
   * 
   * @param text
   *                The text.
   */
  public void setText(String text) {
    this.textFile = null;
    setTextNoTitle(text);
    setTitle();
  }

  /**
   * Load a text file.
   * 
   * @param textFile1
   *                The text file.
   */
  public void loadTextFile(File textFile1) {
    this.textFile = textFile1;
    loadFile();
  }

  // Set the text.
  public void setTextNoTitle(String text) {
    this.textArea.setText(text);
    this.textArea.getCaret().setDot(0);
    this.isDirty = false;
  }

  public void setTitle() {
    StringBuffer buf = new StringBuffer();
    buf.append(titleText);
    if (this.textFile != null) {
      buf.append(": \"");
      buf.append(this.textFile.getAbsolutePath());
      buf.append("*");
      buf.append("\"");
    }
    this.setTitle(buf.toString());
  }

  public boolean saveFile() {
    if (this.textFile.exists() && !this.textFile.canWrite()) {
      showError("File is not writable: " + this.textFile.getAbsolutePath());
      return false;
    }
    final String text = this.textArea.getText();
    FileOutputStream fileOutStream = null;
    try {
      fileOutStream = new FileOutputStream(this.textFile);
    } catch (FileNotFoundException e) {
      handleException(e);
      return false;
    }
    Writer writer = null;
    if (this.codePage == null) {
      writer = new OutputStreamWriter(fileOutStream);
    } else {
      try {
        writer = new OutputStreamWriter(fileOutStream, this.codePage);
      } catch (UnsupportedEncodingException e) {
        handleException(e);
        return false;
      }
    }
    try {
      writer.write(text);
      writer.close();
      this.isDirty = false;
      setTitle();
    } catch (IOException e) {
      handleException(e);
      return false;
    }
    return true;
  }
  
  public boolean confirmOverwrite(File f) {
    if (f.exists()) {
      Object[] options = {"Yes, Overwrite.",
                          "No"};
      int n = JOptionPane.showOptionDialog(this,
          "File " + f.getAbsolutePath() + " exists.\nWould you like to overwrite it?",
          
          "Confirm Overwrite",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE,
          null,
          options,
          options[1]);
      if (n == 1) {
        return false;
      }
    }
    return true;
  }

  private void createTextArea() {
    try {
      this.textArea = new JTextArea();
      this.addCursorOwningComponent(this.textArea);
      Border emptyBorder = BorderFactory.createEmptyBorder(2, 4, 2, 2);
      Border grayLineBordre = BorderFactory.createLineBorder(Color.gray, 1);
      this.textArea.setBorder(BorderFactory.createCompoundBorder(grayLineBordre, emptyBorder));
      this.textArea.setSelectionColor(selectionColor);
      this.textArea.setEditable(true);
      this.textArea.setLineWrap(true);
      this.textArea.setWrapStyleWord(true);
      this.textArea.setText(defaultText);
      this.textArea.addMouseListener(new PopupListener(this));
      // textArea.setFocusable(true);
      this.textArea.addFocusListener(new TextFocusHandler(this));
      this.textArea.getDocument().addDocumentListener(new TextChangedListener(this));
      this.textArea.addCaretListener(new CaretChangeHandler(this));
      this.undoMgr = new UndoMgr(this);
      this.textArea.getDocument().addUndoableEditListener(this.undoMgr);
    } catch (Exception e) {
      handleException(e);
    }
  }

  private void populateEditMenu() {
    this.undoItem = new JMenuItem("Undo");
    this.undoItem.addActionListener(this.undoMgr);
    this.undoItem.setEnabled(false);
    this.undoItem.setMnemonic(KeyEvent.VK_U);
    this.undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK));
    this.editMenu.add(this.undoItem);
    this.editMenu.addSeparator();
    HashMap<Object, Action> actionMap = createEditActionMap();
    // Cut
    this.cutAction = actionMap.get(DefaultEditorKit.cutAction);
    this.cutAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_T));
    this.cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X,
        InputEvent.CTRL_MASK));
    this.cutAction.setEnabled(false);
    JMenuItem cutItem = new JMenuItem(this.cutAction);
    cutItem.setText("Cut");
    this.editMenu.add(cutItem);
    // Copy
    this.copyAction = actionMap.get(DefaultEditorKit.copyAction);
    this.copyAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
    this.copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C,
        InputEvent.CTRL_MASK));
    this.copyAction.setEnabled(false);
    JMenuItem copyItem = new JMenuItem(this.copyAction);
    copyItem.setText("Copy");
    this.editMenu.add(copyItem);
    // Paste
    Action pasteAction = actionMap.get(DefaultEditorKit.pasteAction);
    pasteAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
    pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V,
        InputEvent.CTRL_MASK));
    JMenuItem pasteItem = new JMenuItem(pasteAction);
    pasteItem.setText("Paste");
    this.editMenu.add(pasteItem);
  }

  private HashMap<Object, Action> createEditActionMap() {
    HashMap<Object, Action> map = new HashMap<Object, Action>();
    Action[] ar = this.textArea.getActions();
    for (int i = 0; i < ar.length; i++) {
      Action a = ar[i];
      map.put(a.getValue(Action.NAME), a);
    }
    return map;
  }

  private JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();
    createFileMenu();
    menuBar.add(this.fileMenu);
    menuBar.add(createEditMenu());
    menuBar.add(createRunMenu());
    menuBar.add(createToolsMenu());
    menuBar.add(createHelpMenu());
    return menuBar;
  }

  private JMenu createEditMenu() {
    this.editMenu = new JMenu("Edit");
    this.editMenu.setMnemonic(KeyEvent.VK_E);
    populateEditMenu();
    return this.editMenu;
  }

  private JMenu createHelpMenu() {
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setMnemonic(KeyEvent.VK_H);
    JMenuItem manualItem = new JMenuItem("Manual", KeyEvent.VK_M);
    manualItem.addActionListener(new ManualHandler(this));
    helpMenu.add(manualItem);
    JMenuItem helpInfoItem = new JMenuItem("Help", KeyEvent.VK_H);
    helpInfoItem.addActionListener(new HelpHandler(this));
    helpMenu.add(helpInfoItem);
    helpMenu.addSeparator();
    JMenuItem aboutItem = new JMenuItem("About CVD", KeyEvent.VK_A);
    aboutItem.addActionListener(new AboutHandler(this));
    helpMenu.add(aboutItem);
    JMenuItem aboutUimaItem = new JMenuItem("About UIMA", KeyEvent.VK_U);
    aboutUimaItem.addActionListener(new AboutUimaHandler(this));
    helpMenu.add(aboutUimaItem);
    return helpMenu;
  }

  private void createFileMenu() {
    this.fileMenu = new JMenu("File");

    // Standard text file menu items.
    JMenuItem newTextItem = new JMenuItem("New Text...", KeyEvent.VK_N);
    newTextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
    newTextItem.addActionListener(new NewTextEventHandler(this));
    this.fileMenu.add(newTextItem);
    this.fileMenu.setMnemonic(KeyEvent.VK_F);
    JMenuItem fileOpen = new JMenuItem("Open Text File", KeyEvent.VK_O);
    fileOpen.addActionListener(new FileOpenEventHandler(this));
    fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
    this.fileMenu.add(fileOpen);
    this.fileSaveItem = new JMenuItem("Save Text File", KeyEvent.VK_S);
    this.fileSaveItem.setEnabled(false);
    this.fileSaveItem.addActionListener(new FileSaveEventHandler(this));
    this.fileSaveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
    this.fileMenu.add(this.fileSaveItem);
    JMenuItem fileSaveAsItem = new JMenuItem("Save Text As...", KeyEvent.VK_A);
    fileSaveAsItem.addActionListener(new FileSaveAsEventHandler(this));
    this.fileMenu.add(fileSaveAsItem);
    createCPMenu();
    this.cpMenu.setMnemonic(KeyEvent.VK_P);
    this.fileMenu.add(this.cpMenu);
    this.fileMenu.addSeparator();
    this.recentTextFileMenu = new JMenu("Recently used ...");
    this.recentTextFileMenu.setMnemonic(KeyEvent.VK_U);
    this.fileMenu.add(this.recentTextFileMenu);
    this.fileMenu.addSeparator();

    // Color preferences
    JMenuItem colorPrefsOpenItem = new JMenuItem("Load Color Settings", KeyEvent.VK_L);
    colorPrefsOpenItem.addActionListener(new ColorPrefsOpenHandler(this));
    this.fileMenu.add(colorPrefsOpenItem);
    JMenuItem colorPrefsSaveItem = new JMenuItem("Save Color Settings", KeyEvent.VK_C);
    colorPrefsSaveItem.addActionListener(new ColorPrefsSaveHandler(this));
    this.fileMenu.add(colorPrefsSaveItem);
    this.fileMenu.addSeparator();

    // Reading and writing type system files.
    this.typeSystemReadItem = new JMenuItem("Read Type System File");
    this.typeSystemReadItem.setEnabled(true);
    this.typeSystemReadItem.addActionListener(new TypeSystemFileOpenEventHandler(this));
    this.fileMenu.add(this.typeSystemReadItem);
    this.typeSystemWriteItem = new JMenuItem("Write Type System File");
    this.typeSystemWriteItem.addActionListener(new XCASSaveTSHandler(this));
    this.fileMenu.add(this.typeSystemWriteItem);
    this.fileMenu.addSeparator();

    // Reading and writing XMI CAS files.
    this.xmiCasReadItem = new JMenuItem("Read XMI CAS File");
    this.xmiCasReadItem.addActionListener(new XmiCasFileOpenHandler(this));
    this.fileMenu.add(this.xmiCasReadItem);
    this.xmiCasWriteItem = new JMenuItem("Write XMI CAS File");
    this.xmiCasWriteItem.addActionListener(new XmiCasSaveHandler(this));
    this.fileMenu.add(this.xmiCasWriteItem);
    this.fileMenu.addSeparator();

    // Reading and writing old-style XCAS files.
    this.xcasReadItem = new JMenuItem("Read XCAS File", KeyEvent.VK_R);
    this.xcasReadItem.addActionListener(new XCASFileOpenEventHandler(this));
    this.fileMenu.add(this.xcasReadItem);
    this.xcasWriteItem = new JMenuItem("Write XCAS File", KeyEvent.VK_W);
    this.xcasWriteItem.addActionListener(new XCASSaveHandler(this));
    this.fileMenu.add(this.xcasWriteItem);
    this.fileMenu.addSeparator();
    JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
    exit.addActionListener(new SystemExitHandler(this));
    this.fileMenu.add(exit);

    // Disable menu items that can't be executed yet.
    this.typeSystemWriteItem.setEnabled(false);
    setEnableCasFileReadingAndWriting();
  }

  public final void setEnableCasFileReadingAndWriting() {
    final boolean enable = this.cas != null;
    this.xcasReadItem.setEnabled(enable);
    this.xmiCasReadItem.setEnabled(enable);
    this.xcasWriteItem.setEnabled(enable);
    this.xmiCasWriteItem.setEnabled(enable);
  }

  private final void addCursorOwningComponent(Component comp) {
    this.cursorOwningComponents.add(comp);
  }

  private final void setWaitCursor() {
    this.setEnabled(false);
    this.cursorCache = new ArrayList<Cursor>();
    for (int i = 0; i < this.cursorOwningComponents.size(); i++) {
      Component comp = this.cursorOwningComponents.get(i);
      this.cursorCache.add(comp.getCursor());
      comp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
  }

  public final void resetCursor() {
    if (this.cursorCache == null) {
      return;
    }
    for (int i = 0; i < this.cursorOwningComponents.size(); i++) {
      Component comp = this.cursorOwningComponents.get(i);
      comp.setCursor(this.cursorCache.get(i));
    }
    this.setEnabled(true);
  }

  public void createCodePages() {
    // Get supported encodings from JVM
    Map<String, Charset> charsetMap = Charset.availableCharsets();
    String sysCodePage = Charset.defaultCharset().name();
    
    this.codePages = new ArrayList<String>();

    if (sysCodePage != null) {
      this.codePages.add(sysCodePage);
      if (this.codePage == null) {
        this.codePage = sysCodePage;
      }
    }
    for (String charsetName : charsetMap.keySet()) {
      if (!this.codePages.contains(charsetName)) {
        this.codePages.add(charsetName);
      }
    }
  }

  public void resetTrees() {
    updateIndexTree(false);
  }

  private void createCPMenu() {
    createCodePages();
    this.cpMenu = new AutoFoldingMenu("Code Page", 20);
    resetCPMenu();
  }

  public void resetCPMenu() {
    this.cpMenu.removeAll();
    this.cpButtons = new ButtonGroup();
    JRadioButtonMenuItem item;
    String cp;
    for (int i = 0; i < this.codePages.size(); i++) {
      cp = this.codePages.get(i);
      item = new JRadioButtonMenuItem(cp);
      if (cp.equals(this.codePage)) {
        item.setSelected(true);
      }
      item.addActionListener(new SetCodePageHandler(this));
      this.cpButtons.add(item);
      this.cpMenu.add(item);
    }
    
  }

  public void addLanguage(String language1) {
    this.language = language1;
    if (!this.languages.contains(language1)) {
      this.languages.add(language1);
    }
    resetLangMenu();
  }

  private void createLangMenu() {
    createLanguages();
    this.langMenu = new JMenu("Language");
    resetLangMenu();
  }

  public void resetLangMenu() {
    this.langMenu.removeAll();
    this.langButtons = new ButtonGroup();
    JRadioButtonMenuItem item;
    String lang;
    for (int i = 0; i < this.languages.size(); i++) {
      lang = this.languages.get(i);
      item = new JRadioButtonMenuItem(lang);
      if (lang.equals(this.language)) {
        item.setSelected(true);
      }
      item.addActionListener(new SetLanguageHandler(this));
      this.langButtons.add(item);
      this.langMenu.add(item);
    }
    this.langMenu.addSeparator();
    JMenuItem addLangItem = new JMenuItem("Add language");
    addLangItem.addActionListener(new AddLanguageHandler(this));
    this.langMenu.add(addLangItem);
    JMenu removeMenu = new JMenu("Remove language");
    for (int i = 0; i < this.languages.size(); i++) {
      JMenuItem rmItem = new JMenuItem(this.languages.get(i));
      rmItem.addActionListener(new RemoveLanguageHandler(this));
      removeMenu.add(rmItem);
    }
    this.langMenu.add(removeMenu);
    JMenuItem restoreDefaultsItem = new JMenuItem("Restore defaults");
    restoreDefaultsItem.addActionListener(new RestoreLangDefaultsHandler(this));
    this.langMenu.add(restoreDefaultsItem);
  }

  public void createLanguages() {
    this.languages = new ArrayList<String>();
    if (this.languagePrefsList == null) {
      this.languagePrefsList = defaultLanguages;
    }
    if (this.language == null) {
      this.language = LANGUAGE_DEFAULT;
    }
    StringTokenizer tok = new StringTokenizer(this.languagePrefsList, ",");
    String lang;
    while (tok.hasMoreTokens()) {
      lang = tok.nextToken();
      if (!this.languages.contains(lang)) {
        this.languages.add(lang);
      }
    }
  }

  private JMenu createRunMenu() {
    JMenu runMenu = new JMenu("Run");
    runMenu.setMnemonic(KeyEvent.VK_R);
    JMenuItem runMenuItem = new JMenuItem("Load AE", KeyEvent.VK_L);
    runMenuItem.addActionListener(new AnnotatorOpenEventHandler(this));
    runMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
    runMenu.add(runMenuItem);
    this.reRunMenu = new JMenuItem("Run AE", KeyEvent.VK_R);
    this.reRunMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
    runMenu.add(this.reRunMenu);
    this.reRunMenu.addActionListener(new AnnotatorRerunEventHandler(this));
    this.reRunMenu.setEnabled(false);
    this.runOnCasMenuItem = new JMenuItem("Run AE on CAS", KeyEvent.VK_Y);
    this.runOnCasMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
        ActionEvent.CTRL_MASK));
    runMenu.add(this.runOnCasMenuItem);
    this.runOnCasMenuItem.addActionListener(new AnnotatorRunOnCasEventHandler(this));
    this.runOnCasMenuItem.setEnabled(false);
    this.runCPCMenu = new JMenuItem("Run collectionProcessComplete");
    runMenu.add(this.runCPCMenu);
    this.runCPCMenu.addActionListener(new AnnotatorRunCPCEventHandler(this));
    this.runCPCMenu.setEnabled(false);
    this.showPerfReportItem = new JMenuItem("Performance report");
    this.showPerfReportItem.setEnabled(false);
    this.showPerfReportItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (MainFrame.this.lastRunProcessTrace == null) {
          MainFrame.this.showError("No performance report to show.");
        } else {
          PerformanceReportDialog prd = new PerformanceReportDialog(MainFrame.this);
          prd.displayStats(MainFrame.this.lastRunProcessTrace, 1, "Process trace");
        }
      }
    });
    runMenu.add(this.showPerfReportItem);
    runMenu.addSeparator();
    this.recentDescFileMenu = new JMenu("Recently used ...");
    this.recentDescFileMenu.setMnemonic(KeyEvent.VK_U);
    runMenu.add(this.recentDescFileMenu);
    runMenu.addSeparator();
    createLangMenu();
    runMenu.add(this.langMenu);
    this.langMenu.setMnemonic(KeyEvent.VK_L);
    // runMenu.addSeparator();
    JMenuItem dataPathItem = new JMenuItem("Set data path");
    dataPathItem.addActionListener(new SetDataPathHandler(this));
    dataPathItem.setMnemonic(KeyEvent.VK_S);
    runMenu.addSeparator();
    runMenu.add(dataPathItem);
    return runMenu;
  }

  private JMenu createToolsMenu() {
    JMenu toolsMenu = new JMenu("Tools");
    toolsMenu.setMnemonic(KeyEvent.VK_T);
    this.tsViewerItem = new JMenuItem("View Typesystem", KeyEvent.VK_T);
    this.tsViewerItem.addActionListener(new ShowTypesystemHandler(this));
    this.tsViewerItem.setEnabled(false);
    toolsMenu.add(this.tsViewerItem);
    this.allAnnotationViewerItem = new JMenuItem("Show Selected Annotations", KeyEvent.VK_A);
    this.allAnnotationViewerItem.addActionListener(new ShowAnnotatedTextHandler(this));
    toolsMenu.add(this.allAnnotationViewerItem);
    this.allAnnotationViewerItem.setEnabled(false);
    this.acdItem = new JMenuItem("Customize Annotation Display", KeyEvent.VK_C);
    toolsMenu.add(this.acdItem);
    this.acdItem.setEnabled(false);
    this.acdItem.addActionListener(new ShowAnnotationCustomizerHandler(this));

    JMenu logConfig = new JMenu("Set Log Level");
    ButtonGroup levelGroup = new ButtonGroup();

    // get current log level setting
    String curLogLevel = LogManager.getLogManager().getProperty(".level");

    // create log config menu with available log levels
    Iterator<Level> levelIt = MainFrame.logLevels.iterator();
    while (levelIt.hasNext()) {
      Level level = levelIt.next();
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(level.toString());
      // select current log level
      if (level.toString().equals(curLogLevel)) {
        item.setSelected(true);
      }
      item.addActionListener(new SetLogConfigHandler());
      levelGroup.add(item);
      logConfig.add(item);
    }
    toolsMenu.add(logConfig);

    JMenuItem logViewer = new JMenuItem("View Log File", KeyEvent.VK_L);
    logViewer.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent event) {
        LogFileViewer viewer = new LogFileViewer("Log file: "
            + MainFrame.this.logFile.getAbsolutePath());
        viewer.addWindowListener(new CloseLogViewHandler(MainFrame.this));
        Dimension dim = getDimension(logViewSizePref);
        if (dim == null) {
          dim = logFileDimensionDefault;
        }
        viewer.init(MainFrame.this.logFile, dim);
      }
    });
    toolsMenu.add(logViewer);

    return toolsMenu;
  }

  private void createStatusBar() {
    this.statusPanel = new JPanel();
    // statusPanel.setBorder(BorderFactory.createLineBorder(Color.gray));
    this.statusPanel.setLayout(new BoxLayout(this.statusPanel, BoxLayout.X_AXIS));
    this.statusBar = new JTextField();
    Border innerCompound = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(
        BevelBorder.LOWERED, Color.lightGray, Color.darkGray), BorderFactory.createEmptyBorder(0,
        3, 0, 3));
    Border leftCompoundBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(
        0, 0, 0, 1), innerCompound);
    Border middleCompoundBorder = BorderFactory.createCompoundBorder(BorderFactory
        .createEmptyBorder(0, 1, 0, 1), innerCompound);
    Border rightCompoundBorder = BorderFactory.createCompoundBorder(BorderFactory
        .createEmptyBorder(0, 1, 0, 0), innerCompound);
    // statusBar.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    this.statusBar.setBorder(leftCompoundBorder);
    this.statusBar.setEditable(false);
    this.statusBar.setBackground(this.getBackground());
    this.statusBar.setText("Starting up.");
    this.statusBar.setToolTipText("Status Bar");
    this.statusPanel.add(this.statusBar);
    this.fileStatus = new JTextField();
    // fileStatus.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
    this.fileStatus.setBorder(rightCompoundBorder);
    this.fileStatus.setMaximumSize(new Dimension(500, 25));
    this.fileStatus.setEditable(false);
    // fileStatus.setBackground(new Color(204, 204, 255));
    // fileStatus.setBackground(new Color(164, 147, 255));
    // statusPanel.add(fileStatus);
    this.aeStatus = new JTextField();
    this.aeStatus.setBorder(middleCompoundBorder);
    this.aeStatus.setMaximumSize(new Dimension(500, 25));
    this.aeStatus.setEditable(false);
    this.statusPanel.add(this.aeStatus);
    this.caretStatus = new JTextField();
    this.caretStatus.setBorder(rightCompoundBorder);
    this.caretStatus.setMaximumSize(new Dimension(500, 25));
    // caretStatus.setBackground(new Color(204, 255, 204));
    this.caretStatus.setEditable(false);
    this.caretStatus.setToolTipText("Position of cursor or extent of selection");
    this.statusPanel.add(this.caretStatus);
    // setCaretStatus(0, 0);
    // setFileStatusMessage();
    setAEStatusMessage();
  }

  public void setCaretStatus(final int dot, final int mark) {
    if (dot == mark) {
      this.caretStatus.setText("Cursor: " + dot);
    } else {
      int from, to;
      if (dot < mark) {
        from = dot;
        to = mark;
      } else {
        from = mark;
        to = dot;
      }
      this.caretStatus.setText("Selection: " + from + " - " + to);
    }
    this.statusPanel.revalidate();
    boolean enable = dot != mark;
    this.cutAction.setEnabled(enable);
    this.copyAction.setEnabled(enable);
  }

  public void setFileStatusMessage() {
    Border textBorder;
    if (this.textFile == null) {
      textBorder = BorderFactory.createTitledBorder(this.textTitleBorder, "Text");
      // textBorder.setTitleJustification(TitledBorder.ABOVE_TOP);
    } else {
      textBorder = BorderFactory.createTitledBorder(this.textTitleBorder, this.textFile
          .getAbsolutePath());
    }
    this.textScrollPane.setBorder(textBorder);
    this.textScrollPane.revalidate();
  }

  private void setAEStatusMessage() {
    if (this.ae == null || this.aeDescriptorFile == null) {
      this.aeStatus.setText("(No AE Loaded)");
      this.aeStatus.setToolTipText("No AE descriptor loaded.");
    } else {
      this.aeStatus.setText(this.aeDescriptorFile.getName());
      this.aeStatus.setToolTipText("<html>Currently loaded AE descriptor file:<br>"
          + this.aeDescriptorFile.getAbsolutePath() + "</html>");
    }
    this.statusPanel.revalidate();
  }

  public void setStatusbarMessage(String message) {
    // Date date = new Date();
    Calendar calendar = Calendar.getInstance();
    int time;
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    time = calendar.get(Calendar.HOUR_OF_DAY);
    if (time < 10) {
      buf.append("0");
    }
    buf.append(time);
    buf.append(":");
    time = calendar.get(Calendar.MINUTE);
    if (time < 10) {
      buf.append("0");
    }
    buf.append(time);
    buf.append(":");
    time = calendar.get(Calendar.SECOND);
    if (time < 10) {
      buf.append("0");
    }
    buf.append(time);
    buf.append("]  ");
    buf.append(message);
    this.statusBar.setText(buf.toString());
    this.statusPanel.revalidate();
  }

  private void initializeLogging() {

    // set log file path
    File homeDir = new File(System.getProperty("user.home"));
    this.logFile = new File(homeDir, "uima.log");

    // delete file if it exists
    this.logFile.delete();

    // initialize log framework
    LogManager logManager = LogManager.getLogManager();
    try {
      InputStream ins = ClassLoader.getSystemResourceAsStream(loggerPropertiesFileName);
      // Try the current class loader if system one cannot find the file
      if (ins == null) {
    	ins = this.getClass().getClassLoader().getResourceAsStream(loggerPropertiesFileName);
      }
      if (ins != null) {
    	logManager.readConfiguration(ins);
      } else {
    	System.out.println("WARNING: failed to load "+loggerPropertiesFileName);
      }
    } catch (SecurityException e) {
      handleException(e);
      return;
    } catch (IOException e) {
      handleException(e);
      return;
    }

    // get UIMA framework logger
    this.log = UIMAFramework.getLogger();
  }

  private void init() {
    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        // Do nothing.
      }
    });
    initializeLogging();
    this.addCursorOwningComponent(this);
    this.addWindowListener(new MainFrameClosing(this));
    // runConfigs = new ArrayList();
    createTextArea();
    this.setTitle(titleText);
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
    Border grayLineBorder = BorderFactory.createLineBorder(Color.gray, 1);
    splitPane.setBorder(emptyBorder);
    final int dividerSize = 5;
    splitPane.setDividerSize(dividerSize);
    JPanel contentPane = new JPanel(new BorderLayout());
    contentPane.add(splitPane, BorderLayout.CENTER);
    contentPane.setBorder(emptyBorder);
    createStatusBar();
    contentPane.add(this.statusPanel, BorderLayout.SOUTH);
    contentPane.setOpaque(true);

    this.setContentPane(contentPane);
    initIRTree();
    this.indexTree.addMouseListener(new IndexPopupListener(this));

    // add combobox to select the view
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BorderLayout());
    this.sofaSelectionPanel = new JPanel();
    this.sofaSelectionComboBox = new JComboBox();
    this.sofaSelectionComboBox.addItem(CAS.NAME_DEFAULT_SOFA);
    this.sofaSelectionPanel.add(new JLabel("Select View:"));
    this.sofaSelectionPanel.add(this.sofaSelectionComboBox);
    leftPanel.add(this.sofaSelectionPanel, BorderLayout.NORTH);
    this.sofaSelectionPanel.setVisible(false);
    this.sofaSelectionComboBox.addItemListener(new SofaSelectionListener(this));
    this.sofaSelectionComboBox
        .setToolTipText("This CAS has multiple Views. Select the View to display.");

    JSplitPane treePairPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    treePairPane.setDividerSize(dividerSize);
    this.indexTreeScrollPane = new JScrollPane(this.indexTree);
    this.indexTreeScrollPane.setBorder(grayLineBorder);
    leftPanel.add(treePairPane);
    splitPane.setLeftComponent(leftPanel);
    // splitPane.setLeftComponent(treePairPane);
    treePairPane.setBorder(BorderFactory.createTitledBorder(grayLineBorder, "Analysis Results"));
    treePairPane.setLeftComponent(this.indexTreeScrollPane);
    initFSTree();
    this.fsTreeScrollPane = new JScrollPane(this.fsTree);
    this.fsTreeScrollPane.setBorder(grayLineBorder);
    treePairPane.setRightComponent(this.fsTreeScrollPane);
    // TitledBorder analysisResultBorder =
    // BorderFactory.createTitledBorder(emptyBorder, "Analysis Results");
    // treePairPane.setBorder(analysisResultBorder);
    this.textScrollPane = new JScrollPane(this.textArea);
    this.textScrollPane.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
    // JPanel textAreaPanel = new JPanel();
    this.textTitleBorder = grayLineBorder;
    setFileStatusMessage();
    splitPane.setRightComponent(this.textScrollPane);
    try {
      loadProgramPreferences();
    } catch (IOException e) {
      handleException(e);
    }
    // Create menus after loading preferences to get code pages.
    this.setJMenuBar(createMenuBar());
    Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
    defaultStyle = StyleContext.getDefaultStyleContext().addStyle("defaultAnnot", defaultStyle);
    StyleConstants.setBackground(defaultStyle, selectionColor);
    this.styleMap.put(CAS.TYPE_NAME_ANNOTATION, defaultStyle);
    this.textPopup = new JPopupMenu();
    this.fsTree.addFocusListener(new TreeFocusHandler(this.fsTree));
    this.indexTree.addFocusListener(new TreeFocusHandler(this.indexTree));
    initKeyMap();
    // Does not work in Java 1.3
    // initFocusTraversalPolicy();
    initFileLists();
    setStatusbarMessage("Ready.");
  }

  private final void initFileLists() {
    int numFiles = this.textFileNameList.size();
    int max = numFiles < maxRecentSize ? numFiles : maxRecentSize;
    for (int i = 0; i < max; i++) {
      File file = new File(this.textFileNameList.get(i));
      this.recentTextFileMenu.add(createRecentTextFileItem(i + 1, file));
      this.recentTextFiles.appendFile(file);
    }
    numFiles = this.descFileNameList.size();
    max = numFiles < maxRecentSize ? numFiles : maxRecentSize;
    for (int i = 0; i < max; i++) {
      File file = new File(this.descFileNameList.get(i));
      this.recentDescFileMenu.add(createRecentDescFileItem(i + 1, file));
      this.recentDescFiles.appendFile(file);
    }
  }

  protected boolean setupAE(File aeFile) {
    try {
      ResourceManager rsrcMgr = null;
      if (this.dataPathName != null) {
        try {
          rsrcMgr = UIMAFramework.newDefaultResourceManager();
          rsrcMgr.setDataPath(this.dataPathName);
        } catch (MalformedURLException e) {
          StringBuffer msg = new StringBuffer();
          msg.append("Error setting data path in AE,\n");
          msg.append("data path contains invalid URL or file descriptor.\n");
          msg.append("You can still run the AE if it doesn't rely on the data path.\n");
          msg.append("Please correct the data path in the \"Run->Set data path\" menu.\n");
          handleException(e, msg);
        }
      }

      // Destroy old AE.
      if (this.ae != null) {
        destroyAe();
        this.acdItem.setEnabled(false);
        setEnableCasFileReadingAndWriting();
        this.tsViewerItem.setEnabled(false);
        this.reRunMenu.setEnabled(false);
        this.runCPCMenu.setEnabled(false);
        this.runOnCasMenuItem.setEnabled(false);
      }

      // get Resource Specifier from XML file
      XMLInputSource in = new XMLInputSource(aeFile);
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

      // for debugging, output the Resource Specifier
      // System.out.println(specifier);

      // create Analysis Engine here
      if (rsrcMgr == null) {
        this.ae = UIMAFramework.produceAnalysisEngine(specifier);
      } else {
        this.ae = UIMAFramework.produceAnalysisEngine(specifier, rsrcMgr, null);
      }
      this.cas = this.ae.newCAS();
      initCas();
      this.acdItem.setEnabled(true);
      this.tsViewerItem.setEnabled(true);
      this.typeSystemWriteItem.setEnabled(true);
      setEnableCasFileReadingAndWriting();
      this.reRunMenu.setEnabled(true);
      this.runCPCMenu.setEnabled(true);

      // reset sofa combo box with just the initial view
      // this.disableSofaListener = true;
      this.sofaSelectionComboBox.removeAllItems();
      this.sofaSelectionComboBox.addItem(CAS.NAME_DEFAULT_SOFA);
      this.sofaSelectionPanel.setVisible(false);
      // this.disableSofaListener = false;
      MainFrame.this.updateIndexTree(true);
    } catch (Exception e) {
      handleException(e);
      return false;
    }
    return true;
  }

  private final void initCas() {
    this.cas.setDocumentLanguage(this.language);
    this.cas.setDocumentText(this.textArea.getText());
  }

  protected void internalRunAE(boolean doCasReset) {
    try {
      if (doCasReset) {
        // Change to Initial view
        this.cas = this.cas.getView(CAS.NAME_DEFAULT_SOFA);
        this.cas.reset();
        initCas();
        // this.disableSofaListener = true;
        this.sofaSelectionComboBox.setSelectedIndex(0);
      }
      this.lastRunProcessTrace = this.ae.process(this.cas);
      this.showPerfReportItem.setEnabled(true);
      this.log.log(Level.INFO, "Process trace of AE run:\n" + this.lastRunProcessTrace.toString());
      // Update sofacombobox here
      // this.disableSofaListener = true;
      int currentViewID = this.sofaSelectionComboBox.getSelectedIndex();
      this.sofaSelectionComboBox.removeAllItems();
      this.sofaSelectionComboBox.addItem(CAS.NAME_DEFAULT_SOFA);
      Iterator<?> sofas = ((CASImpl) MainFrame.this.cas).getBaseCAS().getSofaIterator();
      Feature sofaIdFeat = MainFrame.this.cas.getTypeSystem().getFeatureByFullName(
          CAS.FEATURE_FULL_NAME_SOFAID);
      boolean nonDefaultSofaFound = false;
      while (sofas.hasNext()) {
        SofaFS sofa = (SofaFS) sofas.next();
        String sofaId = sofa.getStringValue(sofaIdFeat);
        if (!CAS.NAME_DEFAULT_SOFA.equals(sofaId)) {
          this.sofaSelectionComboBox.addItem(sofaId);
          nonDefaultSofaFound = true;
        }
      }
      this.sofaSelectionComboBox.setSelectedIndex(currentViewID);
      // make sofa selector visible if any text sofa other than the
      // default was found
      this.sofaSelectionPanel.setVisible(nonDefaultSofaFound);
    } catch (Exception e) {
      handleException(e);
    } catch (Error e) {
      StringBuffer buf = new StringBuffer();
      buf.append("A severe error has occured:\n");
      handleException(e, buf);
      throw e;
    }
  }

  private void initIRTree() {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(noIndexReposLabel);
    DefaultTreeModel model = new DefaultTreeModel(root);
    this.indexTree = new JTree(model);
    this.indexTree.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
    // Only one node can be selected at any one time.
    this.indexTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    this.indexTree.addTreeSelectionListener(new IndexTreeSelectionListener(this));
    // No icons.
    DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    cellRenderer.setLeafIcon(null);
    // cellRenderer.setIcon(null);
    cellRenderer.setClosedIcon(null);
    cellRenderer.setOpenIcon(null);
    this.indexTree.setCellRenderer(cellRenderer);
  }

  private void initFSTree() {
    FSTreeModel treeModel = new FSTreeModel();
    this.fsTree = new JTree(treeModel);
    this.fsTree.addMouseListener(new StringFsPopupEventAdapter());
    this.fsTree.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
    this.fsTree.setLargeModel(true);
    // Only one node can be selected at any one time.
    this.fsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    this.fsTree.addTreeSelectionListener(new FSTreeSelectionListener(this));
    DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    cellRenderer.setLeafIcon(null);
    // cellRenderer.setIcon(null);
    cellRenderer.setClosedIcon(null);
    cellRenderer.setOpenIcon(null);
    this.fsTree.setCellRenderer(cellRenderer);
  }

  private void deleteFSTree() {
    ((FSTreeModel) this.fsTree.getModel()).reset();
  }

  @SuppressWarnings("unchecked")
  public void updateIndexTree(boolean useCAS) {
    deleteFSTree();
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.indexTree.getModel().getRoot();
    if (useCAS) {
      root.setUserObject(indexReposRootLabel);
    } else {
      root.setUserObject(noIndexReposLabel);
    }
    root.removeAllChildren();
    if (this.cas != null && useCAS) {
      FSIndexRepository ir = this.cas.getIndexRepository();
      Iterator<String> it = ir.getLabels();
      while (it.hasNext()) {
        String label = it.next();
        FSIndex index1 = ir.getIndex(label);
        IndexTreeNode nodeObj = new IndexTreeNode(label, index1.getType(), index1.size());
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeObj);
        root.add(node);
        node.add(createTypeTree(index1.getType(), this.cas.getTypeSystem(), label, ir));
      }
    }
    DefaultTreeModel model = (DefaultTreeModel) this.indexTree.getModel();
    // 1.3 workaround
    TreeModelListener[] listeners = org.apache.uima.tools.cvd.tsview.MainFrame
        .getTreeModelListeners(model);
    // TreeModelListener[] listeners = model.getTreeModelListeners();
    // System.out.println("Number of tree model listeners: " +
    // listeners.length);
    Object[] path = new Object[1];
    path[0] = root;
    TreeModelEvent event = new TreeModelEvent(root, path);
    for (int i = 0; i < listeners.length; i++) {
      listeners[i].treeStructureChanged(event);
    }
  }

  public void updateFSTree(String indexName, FSIndex index1) {
    FSTreeModel treeModel = (FSTreeModel) this.fsTree.getModel();
    treeModel.update(indexName, index1, this.cas);
  }

  private ArrayList<FSNode> getAnnotationsAtPos(int pos, List<FSNode> annots) {
    ArrayList<FSNode> res = new ArrayList<FSNode>();
    FSNode annot;
    final int max = annots.size();
    for (int i = 0; i < max; i++) {
      annot = annots.get(i);
      if (annot.getStart() > pos) {
        break;
      }
      if (annot.getEnd() >= pos) {
        res.add(annot);
      }
    }
    return res;
  }

  private DefaultMutableTreeNode createTypeTree(org.apache.uima.cas.Type type, TypeSystem ts,
          String label, FSIndexRepository ir) {
    int size = ir.getIndex(label, type).size();
    TypeTreeNode typeNode = new TypeTreeNode(type, label, size);
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(typeNode);
    // UIMA-2565 - Clash btw. cas.Type and Window.Type on JDK 7
    // also on method parameter "type" 
    List<org.apache.uima.cas.Type> types = ts.getDirectSubtypes(type);
    final int max = types.size();
    for (int i = 0; i < max; i++) {
      if (ir.getIndex(label, types.get(i)) == null) {
        continue;
      }
      DefaultMutableTreeNode child = createTypeTree(types.get(i), ts, label, ir);
      node.add(child);
    }
    return node;
  }

  private void loadProgramPreferences() throws IOException {
    if (this.iniFile == null) {
      File home = new File(System.getProperty("user.home"));
      this.iniFile = new File(home, "annotViewer.pref");
    }
    if (this.iniFile.exists() && this.iniFile.isFile() && this.iniFile.canRead()) {
      FileInputStream in = new FileInputStream(this.iniFile);
      this.preferences = new Properties();
      this.preferences.load(in);
      String fileOpenDirName = this.preferences.getProperty(textDirPref);
      if (fileOpenDirName != null) {
        this.fileOpenDir = new File(fileOpenDirName);
      }
      String aeOpenDirName = this.preferences.getProperty(aeDirPref);
      if (aeOpenDirName != null) {
        this.annotOpenDir = new File(aeOpenDirName);
      }
      String xcasOpenDirName = this.preferences.getProperty(xcasDirPref);
      if (xcasOpenDirName != null) {
        this.xcasFileOpenDir = new File(xcasOpenDirName);
      }
      String colorFileName = this.preferences.getProperty(colorFilePref);
      if (colorFileName != null) {
        this.colorSettingFile = new File(colorFileName);
        try {
          loadColorPreferences(this.colorSettingFile);
        } catch (IOException e) {
          handleException(e);
        }
      }
      String colorDirName = this.preferences.getProperty(colorDirPref);
      if (colorDirName != null) {
        this.colorSettingsDir = new File(colorDirName);
      }
      this.codePage = this.preferences.getProperty(cpCurrentPref);
      this.language = this.preferences.getProperty(langCurrentPref);
      this.languagePrefsList = this.preferences.getProperty(langListPref);
      this.dataPathName = this.preferences.getProperty(dataPathPref);
    }
    if (this.preferences == null) {
      this.textScrollPane.setPreferredSize(textDimensionDefault);
      this.fsTree.setPreferredSize(fsTreeDimensionDefault);
    } else {
      setPreferredSize(this.textScrollPane, textSizePref);
      setPreferredSize(this.indexTreeScrollPane, indexTreeSizePref);
      setPreferredSize(this.fsTreeScrollPane, fsTreeSizePref);
    }
    if (this.preferences != null) {
      List<String> list = stringToArrayList(this.preferences.getProperty(textFileListPref, ""));
      for (int i = 0; i < list.size(); i++) {
        this.textFileNameList.add(list.get(i));
      }
      list = stringToArrayList(this.preferences.getProperty(descFileListPref, ""));
      for (int i = 0; i < list.size(); i++) {
        this.descFileNameList.add(list.get(i));
      }
    }
    // System.out.println("Home dir: " + System.getProperty("user.home"));
    if (this.preferences == null) {
      this.preferences = new Properties();
    }
  }

  public void setPreferredSize(JComponent comp, String propPrefix) {
    // assert(comp != null);
    comp.setPreferredSize(getDimension(propPrefix));
  }

  public Dimension getDimension(String propPrefix) {
    if (this.preferences == null) {
      return null;
    }
    final String width = this.preferences.getProperty(propPrefix + widthSuffix);
    final String height = this.preferences.getProperty(propPrefix + heightSuffix);
    if (height == null || width == null) {
      return null;
    }
    double x = 0.0;
    double y = 0.0;
    try {
      x = Double.parseDouble(width);
      y = Double.parseDouble(height);
    } catch (NumberFormatException e) {
      handleException(e);
      return null;
    }
    Dimension d = new Dimension();
    d.setSize(x, y);
    return d;
  }

  private static final String stringListToString(List<String> list) {
    if (list.size() < 1) {
      return "";
    }
    StringBuffer buf = new StringBuffer();
    buf.append(list.get(0));
    for (int i = 1; i < list.size(); i++) {
      buf.append(',');
      buf.append(list.get(i));
    }
    return buf.toString();
  }

  private static final List<String> stringToArrayList(String s) {
    List<String> list = new ArrayList<String>();
    if (s.length() > 0) {
      StringTokenizer tok = new StringTokenizer(s, ",");
      while (tok.hasMoreTokens()) {
        list.add(tok.nextToken());
      }
    }
    return list;
  }

  public void saveProgramPreferences() throws IOException {
    // File open dialog preferences.
    if (this.preferences == null) {
      this.preferences = new Properties();
    }
    if (this.fileOpenDir != null) {
      this.preferences.setProperty(textDirPref, this.fileOpenDir.getAbsolutePath());
    }
    if (this.annotOpenDir != null) {
      this.preferences.setProperty(aeDirPref, this.annotOpenDir.getAbsolutePath());
    }
    if (this.xcasFileOpenDir != null) {
      this.preferences.setProperty(xcasDirPref, this.xcasFileOpenDir.getAbsolutePath());
    }
    // Window size preferences.
    Dimension d = this.textScrollPane.getSize();
    this.preferences.setProperty(textSizePref + widthSuffix, Double.toString(d.getWidth()));
    this.preferences.setProperty(textSizePref + heightSuffix, Double.toString(d.getHeight()));
    d = this.indexTreeScrollPane.getSize();
    this.preferences.setProperty(indexTreeSizePref + widthSuffix, Double.toString(d.getWidth()));
    this.preferences.setProperty(indexTreeSizePref + heightSuffix, Double.toString(d.getHeight()));
    d = this.fsTreeScrollPane.getSize();
    this.preferences.setProperty(fsTreeSizePref + widthSuffix, Double.toString(d.getWidth()));
    this.preferences.setProperty(fsTreeSizePref + heightSuffix, Double.toString(d.getHeight()));
    if (this.dataPathName != null) {
      this.preferences.setProperty(dataPathPref, this.dataPathName);
    }
    if (this.colorSettingFile != null) {
      this.preferences.setProperty(colorFilePref, this.colorSettingFile.getAbsolutePath());
    }
    if (this.colorSettingsDir != null) {
      this.preferences.setProperty(colorDirPref, this.colorSettingsDir.getAbsolutePath());
    }
    if (this.codePage != null) {
      this.preferences.setProperty(cpCurrentPref, this.codePage);
    }
    if (this.language != null) {
      this.preferences.setProperty(langCurrentPref, this.language);
    }
    if (this.languages != null && this.languages.size() > 0) {
      StringBuffer buf = new StringBuffer();
      buf.append(this.languages.get(0));
      for (int i = 1; i < this.languages.size(); i++) {
        buf.append(",");
        buf.append(this.languages.get(i));
      }
      this.preferences.setProperty(langListPref, buf.toString());
    }
    this.preferences.setProperty(textFileListPref, stringListToString(this.recentTextFiles
        .toStringList()));
    this.preferences.setProperty(descFileListPref, stringListToString(this.recentDescFiles
        .toStringList()));
    // Write out preferences to file.
    FileOutputStream out = new FileOutputStream(this.iniFile);
    this.preferences.store(out, "Automatically generated preferences file for Annotation Viewer");
  }

  public void saveColorPreferences(File file) throws IOException {
    Properties prefs1 = new Properties();
    Iterator<String> it = this.styleMap.keySet().iterator();
    String type;
    Style style;
    Color fg, bg;
    while (it.hasNext()) {
      type = it.next();
      style = this.styleMap.get(type);
      fg = StyleConstants.getForeground(style);
      bg = StyleConstants.getBackground(style);
      prefs1.setProperty(type, Integer.toString(fg.getRGB()) + "+" + Integer.toString(bg.getRGB()));
    }
    FileOutputStream out = new FileOutputStream(file);
    prefs1.store(out, "Color preferences for annotation viewer.");
  }

  public void loadColorPreferences(File file) throws IOException {
    Style parent = this.styleMap.get(CAS.TYPE_NAME_ANNOTATION);
    StyleContext sc = StyleContext.getDefaultStyleContext();
    Properties prefs1 = new Properties();
    FileInputStream in = new FileInputStream(file);
    prefs1.load(in);
    String typeName, value;
    Style style;
    Color color;
    int pos;
    Iterator<?> it = prefs1.keySet().iterator();
    while (it.hasNext()) {
      typeName = (String) it.next();
      value = prefs1.getProperty(typeName);
      style = sc.addStyle(typeName, parent);
      pos = value.indexOf('+');
      if (pos <= 0) {
        continue;
      }
      // Set foreground.
      color = new Color(Integer.parseInt(value.substring(0, pos)));
      StyleConstants.setForeground(style, color);
      // Set background.
      color = new Color(Integer.parseInt(value.substring(pos + 1, value.length())));
      StyleConstants.setBackground(style, color);
      this.styleMap.put(typeName, style);
    }
  }

  private void initKeyMap() {
    // Create a key map for focussing the index repository tree panel.
    Action focusIRAction = new FocusIRAction(this);
    String focusIRActionName = "focusIRAction";
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
        KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK), focusIRActionName);
    getRootPane().getActionMap().put(focusIRActionName, focusIRAction);
    // Create a key map for focussing the FS tree panel.
    Action focusFSAction = new FocusFSAction(this);
    String focusFSActionName = "focusFSAction";
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
        KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), focusFSActionName);
    getRootPane().getActionMap().put(focusFSActionName, focusFSAction);
    // Create a key map for focussing the text area.
    Action focusTextAction = new FocusTextAction(this);
    String focusTextActionName = "focusTextAction";
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
        KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK), focusTextActionName);
    getRootPane().getActionMap().put(focusTextActionName, focusTextAction);
    // Create a key map for bringing up the text area context menu.
    Action textContextAction = new TextContextMenuAction(this);
    String textContextActionName = "textContextAction";
    this.textArea.getInputMap(JComponent.WHEN_FOCUSED).put(
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_MASK), textContextActionName);
    this.textArea.getActionMap().put(textContextActionName, textContextAction);
  }

  @SuppressWarnings("unchecked")
  public void showTextPopup(int x, int y) {
    final int pos = this.textArea.getCaretPosition();
    this.textPopup.removeAll();
    JMenuItem item = new JMenuItem("Position: " + pos);
    item.setEnabled(false);
    this.textPopup.add(item);
    FSNode posAnnot;
    if (this.isAnnotationIndex) {
      List<FSNode> annots = ((FSTreeModel) this.fsTree.getModel()).getFSs();
      ArrayList<FSNode> selAnnots = getAnnotationsAtPos(pos, annots);
      for (int i = 0; i < selAnnots.size(); i++) {
        posAnnot = selAnnots.get(i);
        item = new JMenuItem("[" + posAnnot.getArrayPos() + "] = " + posAnnot.getType().getName());
        item.addActionListener(new PopupHandler(this, posAnnot.getArrayPos()));
        this.textPopup.add(item);
      }
    }
    this.textPopup.show(this.textArea, x, y);
  }

  public JTree getIndexTree() {
    return this.indexTree;
  }

  public JTree getFsTree() {
    return this.fsTree;
  }

  public JTextArea getTextArea() {
    return this.textArea;
  }

  public CAS getCas() {
    return this.cas;
  }

  public boolean isDirty() {
    return this.isDirty;
  }

  public void setDirty(boolean isDirty) {
    this.isDirty = isDirty;
  }

  public Properties getPreferences() {
    return this.preferences;
  }

  public String getIndexLabel() {
    return this.indexLabel;
  }

  public FSIndex getIndex() {
    return this.index;
  }

  public Map<String, Style> getStyleMap() {
    return this.styleMap;
  }

  public AnalysisEngine getAe() {
    return this.ae;
  }

  public void setIndexLabel(String indexLabel) {
    this.indexLabel = indexLabel;
  }

  public boolean isAnnotationIndex() {
    return this.isAnnotationIndex;
  }

  public void setAnnotationIndex(boolean isAnnotationIndex) {
    this.isAnnotationIndex = isAnnotationIndex;
  }

  public void setIndex(FSIndex index) {
    this.index = index;
  }

  public void setAllAnnotationViewerItemEnable(boolean enabled) {
    this.allAnnotationViewerItem.setEnabled(enabled);
  }

  public File getFileOpenDir() {
    return this.fileOpenDir;
  }

  public void setFileOpenDir(File fileOpenDir) {
    this.fileOpenDir = fileOpenDir;
  }

  public File getTextFile() {
    return this.textFile;
  }

  public void setTextFile(File textFile) {
    this.textFile = textFile;
  }

  public void setSaveTextFileEnable(boolean enabled) {
    this.fileSaveItem.setEnabled(enabled);
  }

  public UndoMgr getUndoMgr() {
    return this.undoMgr;
  }

  public void setUndoEnabled(boolean enabled) {
    this.undoItem.setEnabled(enabled);
  }

  public File getXcasFileOpenDir() {
    return this.xcasFileOpenDir;
  }

  public void setXcasFileOpenDir(File xcasFileOpenDir) {
    this.xcasFileOpenDir = xcasFileOpenDir;
  }

  public void setCas(CAS cas) {
    this.cas = cas;
  }

  public void setRunOnCasEnabled() {
    // Enable the "Run on CAS" menu item when we have both an AE and a CAS.
    this.runOnCasMenuItem.setEnabled(this.ae != null && this.cas != null);
  }

  public void destroyAe() {
    this.cas = null;
    if (this.ae != null) {
      this.ae.destroy();
      this.ae = null;
    }
  }

  public void setRerunEnabled(boolean enabled) {
    this.reRunMenu.setEnabled(enabled);
    this.runCPCMenu.setEnabled(enabled);
  }

  public void setTypeSystemViewerEnabled(boolean enabled) {
    this.tsViewerItem.setEnabled(enabled);
  }

  public File getColorSettingsDir() {
    return this.colorSettingsDir;
  }

  public void setColorSettingsDir(File colorSettingsDir) {
    this.colorSettingsDir = colorSettingsDir;
  }

  public File getColorSettingFile() {
    return this.colorSettingFile;
  }

  public void setColorSettingFile(File colorSettingFile) {
    this.colorSettingFile = colorSettingFile;
  }

  public File getAnnotOpenDir() {
    return this.annotOpenDir;
  }

  public void setAnnotOpenDir(File annotOpenDir) {
    this.annotOpenDir = annotOpenDir;
  }

  public String getDataPathName() {
    return this.dataPathName;
  }

  public void setDataPathName(String dataPathName) {
    this.dataPathName = dataPathName;
  }

  public String getCodePage() {
    return this.codePage;
  }

  public void setCodePage(String codePage) {
    this.codePage = codePage;
  }

  public List<String> getCodePages() {
    return this.codePages;
  }

  public String getLanguage() {
    return this.language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public List<String> getLanguages() {
    return this.languages;
  }

  public String getLanguagePrefsList() {
    return this.languagePrefsList;
  }

  public void setLanguagePrefsList(String languagePrefsList) {
    this.languagePrefsList = languagePrefsList;
  }

  
  public void handleSofas() {
    // Populate sofa combo box with the names of all text
    // Sofas in the CAS
    String currentView = (String) this.sofaSelectionComboBox.getSelectedItem();
    this.sofaSelectionComboBox.removeAllItems();
    this.sofaSelectionComboBox.addItem(CAS.NAME_DEFAULT_SOFA);
    Iterator<?> sofas = ((CASImpl) getCas()).getBaseCAS().getSofaIterator();
    Feature sofaIdFeat = getCas().getTypeSystem()
        .getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
    boolean nonDefaultSofaFound = false;
    while (sofas.hasNext()) {
      SofaFS sofa = (SofaFS) sofas.next();
      String sofaId = sofa.getStringValue(sofaIdFeat);
      if (!CAS.NAME_DEFAULT_SOFA.equals(sofaId)) {
        this.sofaSelectionComboBox.addItem(sofaId);
        nonDefaultSofaFound = true;
      }
    }
    // reuse last selected view if found in new CAS
    int newIndex = 0;
    String newView = CAS.NAME_DEFAULT_SOFA;
    for (int i = 0; i < this.sofaSelectionComboBox.getItemCount(); i++) {
      if (currentView.equals(this.sofaSelectionComboBox.getItemAt(i))) {
        newIndex = i;
        newView = currentView;
        break;
      }
    }
    // make sofa selector visible if any text sofa other
    // than the default was found
    this.sofaSelectionPanel.setVisible(nonDefaultSofaFound);
    setCas(getCas().getView(newView));

    this.sofaSelectionComboBox.setSelectedIndex(newIndex);
    String text = getCas().getDocumentText();
    if (text == null) {
      text = getCas().getSofaDataURI();
      if (text != null) {
        text = "SofaURI = " + text;
      } else {
        if (getCas().getSofaDataArray() != null) {
          text = "Sofa array with mime type = " + getCas().getSofa().getSofaMime();
        }
      }
    }
    setText(text);
    if (text == null) {
      getTextArea().repaint();
    }
  }

  public boolean isExitOnClose() {
    return this.exitOnClose;
  }

  /**
   * Set exit-on-close behavior. Normally, CVD will shut down the JVM it's running in when it's main
   * window is being closed. Calling <code>setExitOnClose(false)</code> prevents that. It is then
   * the caller's task to shut down the JVM.
   * 
   * @param exitOnClose
   */
  public void setExitOnClose(boolean exitOnClose) {
    this.exitOnClose = exitOnClose;
  }

}
