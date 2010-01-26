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

package org.apache.uima.caseditor.ui.corpusview;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.ui.model.ElementWorkbenchAdapterFactory;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;

/**
 * The corpus explorer is a view which knows how to display and handle the nlp
 * model elements.
 */
public final class CorpusExplorerView extends ViewPart
{
	
	private static class ExtendedBaseWorkbenchContentProvider 
			extends BaseWorkbenchContentProvider {
		
		IAdapterFactory factory = new ElementWorkbenchAdapterFactory();
		
		@Override
		protected IWorkbenchAdapter getAdapter(Object element) {
			
			IWorkbenchAdapter adapter = 
					(IWorkbenchAdapter) factory.getAdapter(element, IWorkbenchAdapter.class);
			
			if (adapter != null) {
				return adapter;
			}
			else {
				return super.getAdapter(element);
			}
		}
	}
	

    /**
     * The ID of the <code>CorpusExplorerView</code>.
     */
    public final static String ID = "org.apache.uima.caseditor.corpusview";

    private TreeViewer mTreeViewer;

    private CorpusExplorerActionGroup mActions;

    private ModelChangeListener mModelChangeListener;

    /**
     * Creates the main control of the current view.
     */
    @Override
    public void createPartControl(Composite parent)
    {
        parent.setLayout(new FillLayout());

        mTreeViewer = new TreeViewer(parent);
        mTreeViewer.setContentProvider(new ExtendedBaseWorkbenchContentProvider());

        mTreeViewer.setLabelProvider(new DecoratingLabelProvider(
                new WorkbenchLabelProvider(), PlatformUI.getWorkbench()
                        .getDecoratorManager().getLabelDecorator()));

        // performance optimization
        mTreeViewer.setUseHashlookup(true); // TODO: change back to true

        initContextMenu();

        mTreeViewer.setInput(CasEditorPlugin.getNlpModel());

        mModelChangeListener = new ModelChangeListener(
                mTreeViewer);

        CasEditorPlugin.getNlpModel().addNlpModelChangeListener(mModelChangeListener);

        mTreeViewer.setSorter(new CorpusSorter());

        mActions = new CorpusExplorerActionGroup(this);

        initListeners();

        getSite().setSelectionProvider(mTreeViewer);

        initDragAndDrop();

        mActions.fillActionBars(getViewSite().getActionBars());

        mActions.setContext(new ActionContext(
                mTreeViewer.getSelection()));

        mActions.updateActionBars();
    }

    /**
     * Initializes the context menu.
     */
    private void initContextMenu()
    {
        MenuManager menuManager = new MenuManager("#PopupMenu");
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                IStructuredSelection selection = (IStructuredSelection) mTreeViewer
                        .getSelection();

                mActions.setContext(new ActionContext(selection));
                mActions.fillContextMenu(manager);
            }
        });

        Menu menu = menuManager.createContextMenu(mTreeViewer.getTree());
        mTreeViewer.getTree().setMenu(menu);
        getSite().registerContextMenu(menuManager, mTreeViewer);
    }

    /**
     * Initializes the listeners.
     */
    private void initListeners()
    {
        mTreeViewer.addDoubleClickListener(new IDoubleClickListener()
        {
            public void doubleClick(DoubleClickEvent event)
            {
                IStructuredSelection selection = (IStructuredSelection) event
                        .getSelection();

                Object selectedElement = selection.getFirstElement();

                if (mTreeViewer.isExpandable(selectedElement))
                {
                    mTreeViewer.setExpandedState(selectedElement, !mTreeViewer
                            .getExpandedState(selectedElement));
                }
            }
        });

        mTreeViewer.addOpenListener(new IOpenListener()
        {
            public void open(OpenEvent event)
            {
                mActions.executeDefaultAction((IStructuredSelection) event
                        .getSelection());
            }
        });

        mTreeViewer.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                mActions.setContext(new ActionContext(event.getSelection()));

                mActions.updateActionBars();
            }
        });

        mTreeViewer.getTree().addKeyListener(new KeyListener(){

			public void keyPressed(KeyEvent e) {
				mActions.handleKeyPressed(e);
			}

			public void keyReleased(KeyEvent e) {
			}});
    }

    /**
     * Initializes the drag and drop stuff. Note: currently disabled, cause drag
     * and drop did not work ...
     */
    private void initDragAndDrop()
    {
//        int ops = DND.DROP_COPY | DND.DROP_MOVE;

        //Transfer[] transfers = new Transfer[]
        //{ LocalSelectionTransfer.getInstance(), ResourceTransfer.getInstance(),
        //        FileTransfer.getInstance(), PluginTransfer.getInstance() };

        //mTreeViewer
        //        .addDragSupport(ops, transfers, new CorpusExplorerDragAdapter(
        //                getSite().getSelectionProvider()));

       // CorpusExplorerDropAdapter adapter = new CorpusExplorerDropAdapter(
       //         mTreeViewer);

        //adapter.setFeedbackEnabled(false);

        // mTreeViewer.addDropSupport(ops | DND.DROP_DEFAULT, transfers, adapter);
    }

    /**
     * Retrieves the <code>TreeViewer</code> of the current corupus explorer.
     *
     * @return the <code>TreeViewer</code>.
     */
    public ISelectionProvider getTreeViewer()
    {
        return mTreeViewer;
    }

    /**
     * Sets the focus to the <code>TreeViewer</code>.
     */
    @Override
    public void setFocus()
    {
        mTreeViewer.getTree().setFocus();
    }

    @Override
    public void dispose()
    {
        super.dispose();

        // TaePlugin.getNLPModel().removeNLPModelChangeListener(
        // mModelChangeListener);
    }
}
