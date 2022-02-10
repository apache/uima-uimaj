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

package org.apache.uima.tools.util.gui;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;

import org.apache.uima.tools.images.Images;

/**
 * The Class TransportControlPanel.
 */
public class TransportControlPanel extends JPanel implements ActionListener {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -2176626241850938032L;

  /** The Constant PLAY_BUTTON. */
  static public final int PLAY_BUTTON = 0;

  /** The Constant PAUSE_BUTTON. */
  static public final int PAUSE_BUTTON = 1;

  /** The Constant STOP_BUTTON. */
  static public final int STOP_BUTTON = 2;

  /** The play button. */
  private ToggleButton playButton;

  /** The pause button. */
  private ToggleButton pauseButton;

  /** The stop button. */
  private ToggleButton stopButton;

  /** The invisible button. */
  private JToggleButton invisibleButton; // Used as part of ButtonGroup to give

  // visual effect of no button selected
  /** The last button selected. */
  // if Stop is pressed.
  private ToggleButton lastButtonSelected;

  /** The allow stop. */
  private boolean allowStop = true;

  /** The control listener. */
  final TransportControlListener controlListener;

  /**
   * Instantiates a new transport control panel.
   *
   * @param controlListener
   *          the control listener
   */
  public TransportControlPanel(TransportControlListener controlListener) {
    this.controlListener = controlListener;
    ButtonGroup buttonGroup = new ButtonGroup();

    setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

    FlowLayout flowLayout = (FlowLayout) getLayout();
    flowLayout.setHgap(10);

    playButton = new ToggleButton(Images.getImageIcon(Images.PLAY));
    add(playButton);
    buttonGroup.add(playButton);
    playButton.addActionListener(this);

    pauseButton = new ToggleButton(Images.getImageIcon(Images.PAUSE));
    add(pauseButton);
    buttonGroup.add(pauseButton);
    pauseButton.addActionListener(this);

    stopButton = new ToggleButton(Images.getImageIcon(Images.STOP));
    add(stopButton);
    buttonGroup.add(stopButton);
    stopButton.addActionListener(this);

    invisibleButton = new JToggleButton("");
    buttonGroup.add(invisibleButton);

    // Until 'Play' is pressed, then pause and stop are disabled:

    pauseButton.setEnabled(false);
    stopButton.setEnabled(false);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == playButton && lastButtonSelected != playButton) {
      playButton.setEnabled(false);
      pauseButton.setEnabled(true);
      stopButton.setEnabled(true);

      controlListener.controlStarted();

      lastButtonSelected = playButton;
    } else if (source == pauseButton) {
      if (lastButtonSelected != pauseButton)
        pause();
      else {
        controlListener.controlResumed();
        playButton.setSelected(true);
        playButton.setEnabled(true);
        stopButton.setEnabled(true);

        lastButtonSelected = playButton;
      }
    } else if (source == stopButton) {
      if (allowStop)
        stop();
      else {
        playButton.setSelected(true);

        lastButtonSelected = playButton;
      }
    }
  }

  /**
   * Pause.
   */
  private void pause() {
    playButton.setEnabled(false);
    stopButton.setEnabled(true);
    controlListener.controlPaused();

    lastButtonSelected = pauseButton;
  }

  /**
   * Perform pause.
   */
  public void performPause() {
    pause();
  }

  /**
   * Stop.
   */
  public void stop() {
    stop(true);
  }

  /**
   * Stop.
   *
   * @param invokeListener
   *          the invoke listener
   */
  private void stop(boolean invokeListener) {
    if (invokeListener)
      controlListener.controlStopped();

    invisibleButton.setSelected(true);

    pauseButton.setEnabled(false);
    stopButton.setEnabled(false);
    playButton.setEnabled(true);

    lastButtonSelected = null;
  }

  /**
   * Reset.
   */
  public void reset() {
    stop(false);
  }

  /**
   * Sets the button tooltip text.
   *
   * @param buttonIndex
   *          the button index
   * @param tooltip
   *          the tooltip
   */
  public void setButtonTooltipText(int buttonIndex, String tooltip) {
    if (buttonIndex == PLAY_BUTTON)
      playButton.setToolTipText(tooltip);
    else if (buttonIndex == PAUSE_BUTTON)
      pauseButton.setToolTipText(tooltip);
    else if (buttonIndex == STOP_BUTTON)
      stopButton.setToolTipText(tooltip);
  }

  /**
   * Gets the button.
   *
   * @param buttonIndex
   *          the button index
   * @return the button
   */
  public AbstractButton getButton(int buttonIndex) {
    if (buttonIndex == PLAY_BUTTON)
      return playButton;
    else if (buttonIndex == PAUSE_BUTTON)
      return pauseButton;
    else if (buttonIndex == STOP_BUTTON)
      return stopButton;
    else
      return null;
  }

  /**
   * Sets the allow stop.
   *
   * @param allowStop
   *          the new allow stop
   */
  public void setAllowStop(boolean allowStop) {
    this.allowStop = allowStop;
  }

  /**
   * The Class ToggleButton.
   */
  static class ToggleButton extends JToggleButton {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 334131406404068987L;

    /**
     * Instantiates a new toggle button.
     *
     * @param imageIcon
     *          the image icon
     */
    public ToggleButton(ImageIcon imageIcon) {
      super(imageIcon);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.JComponent#getInsets()
     */
    @Override
    public Insets getInsets() {
      return new Insets(2, 2, 2, 2);
    }
  }
}
