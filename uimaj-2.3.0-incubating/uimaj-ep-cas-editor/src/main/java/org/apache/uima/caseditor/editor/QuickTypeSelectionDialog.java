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

package org.apache.uima.caseditor.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * This is a lightweight popup dialog which creates an annotation of the chosen type.
 */
class QuickTypeSelectionDialog extends PopupDialog {

  private final AnnotationEditor editor;

  private Text filterText;

  private Map<Character, Type> shortcutTypeMap = new HashMap<Character, Type>();

  private Map<Type, Character> typeShortcutMap = new HashMap<Type, Character>();

  /**
   * Initializes the current instance.
   * 
   * @param parent
   * @param editor
   */
  @SuppressWarnings("deprecation")
  QuickTypeSelectionDialog(Shell parent, AnnotationEditor editor) {
    super(parent, PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE, true, true, false, true, null, null);

    this.editor = editor;

    // key shortcuts are assigned automatically to types, the shortcut
    // mapping may change if the type system is modified

    String shortcutsString = "qwertzuiopasdfghjklyxcvbnm1234567890";

    Set<Character> shortcuts = new HashSet<Character>();

    for (int i = 0; i < shortcutsString.length(); i++) {
      shortcuts.add(shortcutsString.charAt(i));
    }

    List<Type> types = new ArrayList<Type>();
    Collections.addAll(types, getTypes());
    Collections.sort(types, new Comparator<Type>() {
      public int compare(Type o1, Type o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    // Try to create mappings with first letter of the type name as shortcut
    for (Iterator<Type> it = types.iterator(); it.hasNext();) {

      Type type = it.next();

      String name = type.getShortName();

      Character candidateChar = Character.toLowerCase(name.charAt(0));

      if (shortcuts.contains(candidateChar)) {
        putShortcut(candidateChar, type);

        shortcuts.remove(candidateChar);
        it.remove();
      }
    }

    // Try to create mappings with second letter of the type name as shortcut
    for (Iterator<Type> it = types.iterator(); it.hasNext();) {

      Type type = it.next();

      String name = type.getShortName();

      if (name.length() > 2) {
        Character candidateChar = Character.toLowerCase(name.charAt(1));

        if (shortcuts.contains(candidateChar)) {
          putShortcut(candidateChar, type);

          shortcuts.remove(candidateChar);
          it.remove();
        }
      }
    }

    // Now assign letters to the remaining types
    for (Iterator<Type> it = types.iterator(); it.hasNext();) {

      if (shortcuts.size() > 0) {

        Character candidateChar = shortcuts.iterator().next();

        putShortcut(candidateChar, it.next());

        shortcuts.remove(candidateChar);
        it.remove();
      }
    }
  }

  private void putShortcut(Character shortcut, Type type) {
    shortcutTypeMap.put(shortcut, type);
    typeShortcutMap.put(type, shortcut);
  }

  private Type[] getTypes() {

    TypeSystem typeSystem = editor.getDocument().getCAS().getTypeSystem();

    List<Type> types =
            typeSystem.getProperlySubsumedTypes(typeSystem.getType(CAS.TYPE_NAME_ANNOTATION));

    return types.toArray(new Type[types.size()]);
  }

  private void annotateAndClose(Type annotationType) {
    if (annotationType != null) {
      Point textSelection = editor.getSelection();

      AnnotationFS annotation =
              editor.getDocument().getCAS().createAnnotation(annotationType, textSelection.x,
                      textSelection.y);

      editor.getDocument().addFeatureStructure(annotation);

      if (annotation.getType().equals(editor.getAnnotationMode())) {
        editor.setAnnotationSelection(annotation);
      }
    }

    QuickTypeSelectionDialog.this.close();
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    // TODO: focus always goes to the text box, but should
    // go to the Tree control, find out why, can SWT.NO_FOCUS be used
    // to fix it ?

    filterText = new Text(composite, SWT.NO_FOCUS);
    filterText.setBackground(parent.getBackground());
    filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final TreeViewer typeTree = new TreeViewer(composite, SWT.SINGLE | SWT.V_SCROLL);
    typeTree.getControl().setLayoutData(
            new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

    typeTree.getControl().setFocus();

    filterText.addKeyListener(new KeyListener() {

      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP) {
          typeTree.getControl().setFocus();

          Tree tree = (Tree) typeTree.getControl();

          if (tree.getItemCount() > 0) {

            tree.setSelection(tree.getItem(0));
          }
        }
      }

      public void keyReleased(KeyEvent e) {
        typeTree.refresh(false);
      }
    });

    typeTree.setContentProvider(new ITreeContentProvider() {

      public Object[] getChildren(Object parentElement) {
        return null;
      }

      public Object getParent(Object element) {
        return null;
      }

      public boolean hasChildren(Object element) {
        return false;
      }

      public Object[] getElements(Object inputElement) {
        return (Type[]) inputElement;
      }

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    });

    typeTree.setFilters(new ViewerFilter[] { new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {

        // check if the string from the filterText is contained in the type name
        Type type = (Type) element;

        return type.getName().contains(filterText.getText());
      }
    } });

    typeTree.setLabelProvider(new ILabelProvider() {

      public Image getImage(Object element) {
        return null;
      }

      public String getText(Object element) {

        Type type = (Type) element;

        Character key = typeShortcutMap.get(type);

        if (typeShortcutMap != null) {
          return "[" + key + "] " + type.getShortName();
        } else {
          return type.getShortName();
        }
      }

      public void addListener(ILabelProviderListener listener) {

      }

      public void dispose() {
      }

      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      public void removeListener(ILabelProviderListener listener) {
      }
    });

    typeTree.getControl().addKeyListener(new KeyListener() {

      public void keyPressed(KeyEvent e) {
        Type type = shortcutTypeMap.get(Character.toLowerCase(e.character));

        if (type != null) {
          annotateAndClose(type);
        }
      }

      public void keyReleased(KeyEvent e) {
      }
    });

    typeTree.getControl().addMouseMoveListener(new MouseMoveListener() {

      public void mouseMove(MouseEvent e) {

        Tree tree = (Tree) typeTree.getControl();

        TreeItem item = tree.getItem(new Point(e.x, e.y));

        if (item != null) {
          tree.setSelection(item);
        }
      }
    });

    // TODO open listener needs a double click, single click should be enough
    // because there is already a selection below the mouse
    typeTree.addOpenListener(new IOpenListener() {

      public void open(OpenEvent event) {
        StructuredSelection selection = (StructuredSelection) event.getSelection();

        annotateAndClose((Type) selection.getFirstElement());
      }
    });

    typeTree.setInput(getTypes());

    ISelection modeSelection = new StructuredSelection(new Object[] { editor.getAnnotationMode() });

    typeTree.setSelection(modeSelection, true);

    return composite;
  }

  @Override
  protected Point getInitialSize() {
    return new Point(250, 300);
  }
}
