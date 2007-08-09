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
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;

/**
 * Mock-up of dialog for reporting performance stats.
 * 
 */
public class PerformanceReportDialog extends JDialog {
  private static final long serialVersionUID = 7747258424181047062L;

  private JLabel statusLabel = new JLabel("Processing completed successfully.");

  private JLabel docsProcessedLabel = new JLabel("Documents Processed: 0");

  private JLabel totalTimeLabel = new JLabel("Total Time: 0.0 seconds");

  private JTree tree = new JTree();

  private Map mEventTypeMap;

  /**
   * @throws java.awt.HeadlessException
   */
  public PerformanceReportDialog(Frame aFrame) throws HeadlessException {
    super(aFrame, true);
    this.setTitle("Performance Report");
    this.getContentPane().setLayout(new BorderLayout());
    JPanel messagePanel = new JPanel();
    this.getContentPane().add(messagePanel, BorderLayout.NORTH);

    messagePanel.setLayout(new GridLayout(3, 1));
    messagePanel.add(statusLabel);
    messagePanel.add(docsProcessedLabel);
    messagePanel.add(totalTimeLabel);

    this.getContentPane().add(tree, BorderLayout.CENTER);

    this.pack();
    this.setSize(400, 400);

    // map from event types to their display names
    mEventTypeMap = new HashMap();
    mEventTypeMap.put(ProcessTraceEvent.ANALYSIS_ENGINE, "TAE");
    mEventTypeMap.put(ProcessTraceEvent.ANALYSIS, "Annotator");
    mEventTypeMap.put("CAS_PROCESSOR", "CAS Consumer");

  }

  public void displayStats(ProcessTrace aProcessTrace, int aNumDocsProcessed, String aStatusMessage) {
    statusLabel.setText(aStatusMessage);
    docsProcessedLabel.setText("Documents Processed: " + aNumDocsProcessed);

    // count total time
    long totalTime = 0;
    Iterator it = aProcessTrace.getEvents().iterator();
    while (it.hasNext()) {
      ProcessTraceEvent event = (ProcessTraceEvent) it.next();
      totalTime += event.getDuration();
    }
    double totalTimeSeconds = (double) totalTime / 1000;
    totalTimeLabel.setText("Total Time: " + totalTimeSeconds + " seconds");

    // create root tree node
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("100% (" + totalTime
            + "ms) - Collection Processing Engine");
    // build tree
    it = aProcessTrace.getEvents().iterator();
    while (it.hasNext()) {
      ProcessTraceEvent event = (ProcessTraceEvent) it.next();
      buildEventTree(event, root, totalTime);
    }
    tree.setModel(new DefaultTreeModel(root));

    this.setVisible(true);
  }

  public void buildEventTree(ProcessTraceEvent aEvent, DefaultMutableTreeNode aParentNode,
          long aTotalTime) {
    final DecimalFormat pctFmt = new DecimalFormat("##.##%");
    long duration = aEvent.getDuration();
    double pct;
    if (aTotalTime != 0) {
      pct = ((double) duration) / aTotalTime;
    } else {
      pct = 0;
    }
    String pctStr = pctFmt.format(pct);

    String type = (String) mEventTypeMap.get(aEvent.getType());
    if (type == null) {
      type = aEvent.getType();
    }

    DefaultMutableTreeNode node = new DefaultMutableTreeNode(pctStr + " (" + duration + "ms) - "
            + aEvent.getComponentName() + " (" + type + ")");
    aParentNode.add(node);
    Iterator it = aEvent.getSubEvents().iterator();
    while (it.hasNext()) {
      ProcessTraceEvent event = (ProcessTraceEvent) it.next();
      buildEventTree(event, node, aTotalTime);
    }
  }
}
