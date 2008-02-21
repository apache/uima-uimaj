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

package org.apache.uima.taeconfigurator.files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.apache.uima.taeconfigurator.CDEpropertyPage;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.Messages;
import org.apache.uima.taeconfigurator.TAEConfiguratorPlugin;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.taeconfigurator.editors.ui.dialogs.ResourcePickerDialog;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class MultiResourceSelectionDialog extends ResourcePickerDialog {
  
  private static class CandidateAndSource implements Comparable<CandidateAndSource> {
    String candidate;
    String source;
    
    CandidateAndSource(String aCandidate, String aSource) {
      candidate = aCandidate;
      source = aSource;
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((candidate == null) ? 0 : candidate.hashCode());
      result = prime * result + ((source == null) ? 0 : source.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      final CandidateAndSource other = (CandidateAndSource) obj;
      if (candidate == null) {
        if (other.candidate != null)
          return false;
      } else if (!candidate.equals(other.candidate))
        return false;
      if (source == null) {
        if (other.source != null)
          return false;
      } else if (!source.equals(other.source))
        return false;
      return true;
    }

    public int compareTo(CandidateAndSource o) {
      int r = o.candidate.compareTo(this.candidate);
      if (r == 0) {
        return o.source.compareTo(this.source);
      }
      return r;
    }
  }

  private Button browseButton; // for browsing the file system

  private Button importByNameUI;

  private Button importByLocationUI;

  public boolean isImportByName;

  protected MultiPageEditor editor;

  public MultiResourceSelectionDialog(Shell parentShell, IAdaptable rootElement, String message,
          IPath aExcludeDescriptor, MultiPageEditor aEditor) {
    super(parentShell);
    editor = aEditor;
    String importByStickySetting = CDEpropertyPage.getImportByDefault(editor.getProject());
    isImportByName = (importByStickySetting.equals("name")) ? true : false;  

    /*
    super(parentShell, rootElement, message);
    editor = aEditor;
    setTitle(Messages.getString("ResourceSelectionDialog.title")); //$NON-NLS-1$

    if (message != null)
      setMessage(message);
    else
      setMessage(Messages.getString("ResourceSelectionDialog.message")); //$NON-NLS-1$
    setShellStyle(getShellStyle() | SWT.RESIZE);
 */
  }

  protected Control createDialogArea(Composite parent) {
    // page group
    Composite composite = (Composite) super.createDialogArea(parent);
    FormToolkit factory = new FormToolkit(TAEConfiguratorPlugin.getDefault().getFormColors(
            parent.getDisplay()));
    Label label = new Label(composite, SWT.WRAP /* SWT.CENTER */);
    label.setText(Messages.getString("MultiResourceSelectionDialog.Or")); //$NON-NLS-1$
    browseButton = factory.createButton(composite, Messages
            .getString("MultiResourceSelectionDialog.BrowseFileSys"), //$NON-NLS-1$
            SWT.PUSH);
    browseButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    browseButton.pack(false);
    browseButton.addListener(SWT.MouseUp, new Listener() {
      public void handleEvent(Event event) {
        FileDialog dialog = new FileDialog(getShell(), /* SWT.OPEN | */
        SWT.MULTI);
        String[] extensions = { Messages.getString("MultiResourceSelectionDialog.starDotXml") }; //$NON-NLS-1$
        dialog.setFilterExtensions(extensions);
        String sStartDir = TAEConfiguratorPlugin.getWorkspace().getRoot().getLocation()
                .toOSString();
        dialog.setFilterPath(sStartDir);
        String file = dialog.open();

        if (file != null && !file.equals("")) { //$NON-NLS-1$
          // close();
          okPressed();
          ArrayList list = new ArrayList();
          IPath iPath = new Path(file);
          list.add(iPath);
          localSetResult(list);
        }
      }

    });

    new Label(composite, SWT.NONE).setText("");
    importByNameUI = new Button(composite, SWT.RADIO);
    importByNameUI.setText("Import by Name");
    importByNameUI
            .setToolTipText("Importing by name looks up the name on the datapath, and if not found there, on the classpath.");

    importByLocationUI = new Button(composite, SWT.RADIO);
    importByLocationUI.setText("Import By Location");
    importByLocationUI.setToolTipText("Importing by location requires a relative or absolute URL");

    String importByStickySetting = CDEpropertyPage.getImportByDefault(editor.getProject());
    if (importByStickySetting.equals("location")) {
      importByNameUI.setSelection(false);
      importByLocationUI.setSelection(true);
    } else {
      importByNameUI.setSelection(true);
      importByLocationUI.setSelection(false);
    }
    if (importByNameUI.getSelection()) {
      setupResourcesByName();
    }
    importByLocationUI.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        isImportByName = false;
        CDEpropertyPage.setImportByDefault(editor.getProject(), "location");

        MultiResourceSelectionDialog.this.setupResourcesByLocation();
        browseButton.setEnabled(true);
      }   
    });
    
    importByNameUI.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        isImportByName = true;
        CDEpropertyPage.setImportByDefault(editor.getProject(), "name");
        MultiResourceSelectionDialog.this.setupResourcesByName();
      }   
    });

    return composite;
  }

 
  
  @Override
  protected void setupResourcesByLocation() {
    if (! isImportByName) {
      super.setupResourcesByLocation();
    }
  }

  private void setupResourcesByName() {
    resourcesUI.removeAll();
    resourcesUI.removeListener(SWT.Expand, this); // remove to prevent
                                                  // triggering while setting up
    resourcesUI.removeListener(SWT.Selection, this); // remove to prevent
                                                      // triggering while
                                                      // setting up
    resourcesUI.setHeaderVisible(true);    
    
    resourcesUIc1.setWidth(400);
    resourcesUIc1.setText("by-name xml resource");
    resourcesUIc2.setWidth(400);
    resourcesUIc2.setText("source of by-name resource");
   
    CandidateAndSource [] candidates = computeByNameCandidates();
    for (CandidateAndSource c : candidates) {
      TreeItem item = new TreeItem(resourcesUI, SWT.NULL, 0);     
      item.setText(new String [] {c.candidate, c.source});
      item.setData(c.candidate);
    }
    resourcesUI.addListener(SWT.Selection, this);
    browseButton.setEnabled(false);
  }
  
  public void copyValuesFromGUI() {
    if (resourcesUI.getSelectionCount() > 0) {
      if (importByLocationUI.getSelection()) {
        super.copyValuesFromGUI();
        return;
      }
      result = new Object[] {resourcesUI.getSelection()[0].getData()};
    }
  }
  
  // some caching - for jars with timestamps
  private CandidateAndSource [] computeByNameCandidates() {
    String cp;
    try {
      cp = editor.getFilteredProjectClassPath(false);
    } catch (CoreException e) {
      throw new InternalErrorCDE("unhandled CoreException while getting classpaths to populate by-location list", e);
    }
    String [] cps = cp.split(";");
    List<CandidateAndSource> candidates = new ArrayList<CandidateAndSource>(100);
    
    for (String jarOrDir : cps) {
      if (jarOrDir.toLowerCase().endsWith(".jar")) {
        addJarCandidates(jarOrDir, candidates, jarOrDir);
      } else {
        addClassCandidates(new File(jarOrDir), candidates, "", jarOrDir);
      }
    }
    CandidateAndSource [] result = candidates.toArray(new CandidateAndSource[candidates.size()]);
    Arrays.sort(result);
/*  test - verify all resources can be loaded    
    ResourceManager rm = editor.createResourceManager();
    for (String r : result) {
      try {
        Object resource = rm.getResource(r);
        assert resource != null;
      } catch (ResourceAccessException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
*/
    return result;   
  }
  
  private FilenameFilter onlyXml = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      return name.endsWith(".xml");
    }
  };
  
  private FileFilter onlyDir = new FileFilter() {
    public boolean accept(File pathname) {
      return pathname.isDirectory();
    }
  };
  
  private void addClassCandidates(File dir, List<CandidateAndSource> candidates, String prefix, String source) {
    if (null == dir) {
      return;
    }
    String[] xmlFileNames = dir.list(onlyXml);
    if (null == xmlFileNames) {
      return;
    }
    for (String xmlFileName : xmlFileNames) {
      candidates.add(new CandidateAndSource(prefix + xmlFileName, source));
    }
    File[] subdirs = dir.listFiles(onlyDir);
    if (null == subdirs) {
      return;
    }
 
    for (File subdir : subdirs) {
      String nextPrefix = prefix + subdir.getName() + "/"; 
      addClassCandidates(subdir, candidates, nextPrefix, source);
    }
  }
  
  private static Map<String, Long> sawJar = new TreeMap<String, Long>();
  private static Map<String, List<CandidateAndSource>> cachedCs = new TreeMap<String, List<CandidateAndSource>>();
  
  private void addJarCandidates(String jarPath, List<CandidateAndSource> candidates, String source) {
    Long fileLastModified = (new File(jarPath)).lastModified();
    Long lastModified = sawJar.get(jarPath);   
    List<CandidateAndSource> css = cachedCs.get(jarPath);
    if (fileLastModified <= 0 || 
        lastModified == null || 
        lastModified.longValue() != fileLastModified ||
        null == css) {
      JarInputStream jarIn;
      css = new ArrayList<CandidateAndSource>();
      try {
        jarIn = new JarInputStream(new BufferedInputStream(new FileInputStream(jarPath)));
      } catch (FileNotFoundException e) {
        return;
      } catch (IOException e) {
        throw new InternalErrorCDE(
            "unhandled IOException while reading Jar in classpath to populate by-location list", e);
      }
      ZipEntry entry;
      try {
        while (null != (entry = jarIn.getNextEntry())) {
          String name = entry.getName();
          if (name.startsWith("META-INF")) {
            continue;
          }
          if (name.endsWith(".xml")) {
            css.add(new CandidateAndSource(name, source));
          }
        }
      } catch (IOException e) {
        throw new InternalErrorCDE(
            "unhandled IOException while getting next Jar Entry to populate by-location list", e);
      }
      sawJar.put(jarPath, fileLastModified);
      cachedCs.put(jarPath, css);
    }
    candidates.addAll(css);
  }

  // This is to avoid synthetic access method warning
  protected void localSetResult(ArrayList list) {
    setResult(list);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  public void enableOK() {
    okButton.setEnabled(false);  // preset
    if (0 < resourcesUI.getSelectionCount()) {
      if (importByLocationUI.getSelection()) {
        if (resourcesUI.getSelection()[0].getData() instanceof IFile) {
          okButton.setEnabled(true);
        }
      } else { // import by name
        if (resourcesUI.getSelection()[0] != resourcesUI.getItems()[0]) {
          okButton.setEnabled(true);
        }
      }
    }
  }

}
