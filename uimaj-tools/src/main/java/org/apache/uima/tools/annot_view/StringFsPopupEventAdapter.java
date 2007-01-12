package org.apache.uima.tools.annot_view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

public class StringFsPopupEventAdapter extends MouseAdapter {

  private static class ShowStringHandler implements ActionListener {

    private String string;

    private ShowStringHandler(String s) {
      super();
      this.string = s;
    }

    public void actionPerformed(ActionEvent e) {
      JFrame frame = new JFrame("Full string value");
      JTextArea textArea = new JTextArea(this.string);
      textArea.setEditable(false);
      JScrollPane scrollPane = new JScrollPane(textArea);
      frame.setContentPane(scrollPane);
      frame.pack();
      frame.setVisible(true);
    }

  }

  public StringFsPopupEventAdapter() {
    super();
  }

  public void mousePressed(MouseEvent e) {
    showPopupMaybe(e);
  }

  public void mouseReleased(MouseEvent e) {
    showPopupMaybe(e);
  }

  private void showPopupMaybe(MouseEvent e) {
    if (e.isPopupTrigger()) {
      Object o = e.getSource();
      if (o instanceof javax.swing.JTree) {
	JTree tree = (JTree) o;
	TreePath path = tree.getPathForLocation(e.getX(), e.getY());
	Object leafComponent = path.getLastPathComponent();
	if (leafComponent instanceof FSNode) {
	  FSNode node = (FSNode) leafComponent;
	  if (node.getNodeClass() == FSNode.STRING_FS && node.isShortenedString()) {
	    JPopupMenu menu = new JPopupMenu();
	    JMenuItem showStringItem = new JMenuItem("Show full string");
	    showStringItem.addActionListener(new ShowStringHandler(node.getFullString()));
	    menu.add(showStringItem);
	    menu.show(e.getComponent(), e.getX(), e.getY());
	  }
	}
      }
    }
  }

}
