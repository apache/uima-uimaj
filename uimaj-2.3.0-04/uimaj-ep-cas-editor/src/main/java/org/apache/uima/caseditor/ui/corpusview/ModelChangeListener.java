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

import java.util.LinkedList;

import org.apache.uima.caseditor.core.model.INlpElement;
import org.apache.uima.caseditor.core.model.delta.INlpElementDelta;
import org.apache.uima.caseditor.core.model.delta.INlpModelChangeListener;
import org.apache.uima.caseditor.core.model.delta.INlpModelDeltaVisitor;
import org.apache.uima.caseditor.core.model.delta.Kind;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

/**
 * The ModelChangeListener listens for changes on the nlp
 * model and updates the workspace tree.
 */
class ModelChangeListener implements
        INlpModelChangeListener
{
    private TreeViewer mTreeViewer;

    /**
     * Initializes a new instance.
     *
     * @param treeViewer
     */
    public ModelChangeListener(TreeViewer treeViewer)
    {
        mTreeViewer = treeViewer;
    }

    public void resourceChanged(INlpElementDelta delta)
    {
        final LinkedList<Object> updated = new LinkedList<Object>();

        final LinkedList<Object> added = new LinkedList<Object>();

        final LinkedList<Object> removed = new LinkedList<Object>();


        // INlpElementDelta childs[] = delta.getAffectedChildren();

        INlpModelDeltaVisitor visitor = new INlpModelDeltaVisitor()
        {
            public boolean visit(INlpElementDelta delta)
            {
              if (delta.getResource().getName().equals(".corpus")) {
                return true;
              }

                //if (delta.getKind() == IResourceDelta.OPEN
                //        || delta.getKind() == IResourceDelta.CONTENT)

                if(delta.getKind().equals(Kind.CHANGED))
                {
                    if (delta.isNlpElement())
                    {
                        updated.add(delta.getNlpElement());
                    }
                    else
                    {
                        updated.add(delta.getResource());
                    }
                }
                else if (delta.getKind().equals(Kind.ADDED))
                {
                    if (delta.isNlpElement())
                    {
                        added.add(delta.getNlpElement());
                    }
                    else
                    {
                        added.add(delta.getResource());
                    }
                }
                else if (delta.getKind() == Kind.REMOVED)
                {
                    if (delta.isNlpElement())
                    {
                        removed.add(delta.getNlpElement());
                    }
                    else
                    {
                        removed.add(delta.getResource());
                    }
                }

                return true;
            }
        };

        delta.accept(visitor);

        Display display = mTreeViewer.getControl().getDisplay();

        if (!display.isDisposed())
        {
            display.asyncExec(new Runnable()
            {
                public void run()
                {
                    if (mTreeViewer.getControl().isDisposed())
                    {
                        return;
                    }

                    mTreeViewer.remove(removed.toArray());

                    // TODO: add multiple elements at a time
                    // TODO: refactor
                    for (Object add : added)
                    {
                        ITreeContentProvider contentProvider =
                                (ITreeContentProvider)
                                mTreeViewer.getContentProvider();

                        // seems to make problems
                        mTreeViewer.add(
                                contentProvider.getParent(add),
                                add);
                    }
                }
            });
        }
    }

    /**
     * Refreshes the viewer.
     */
    public void refresh(final INlpElement element)
    {
        Display display = mTreeViewer.getControl().getDisplay();

        display.asyncExec(new Runnable()
        {

            public void run()
            {
                mTreeViewer.refresh(element);
            }
        });
    }
}
