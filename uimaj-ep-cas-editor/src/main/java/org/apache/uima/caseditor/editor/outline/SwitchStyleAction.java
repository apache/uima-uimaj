package org.apache.uima.caseditor.editor.outline;

import org.apache.uima.caseditor.editor.CasEditorError;
import org.eclipse.jface.action.Action;

/**
 * This action triggers the switch of the outline style.
 */
public class SwitchStyleAction extends Action {
	
	private AnnotationOutline outline;
	
	SwitchStyleAction(AnnotationOutline outline) {
		this.outline = outline;
	}
	
	@Override
	public String getText() {
		return "Switch style";
	}
	
	@Override
	public void run() {
		
		if (OutlineStyles.MODE.equals(outline.currentStyle())) {
			outline.switchStyle(OutlineStyles.TYPE);
		}
		else if (OutlineStyles.TYPE.equals(outline.currentStyle())) {
			outline.switchStyle(OutlineStyles.MODE);
		}
		else {
			throw new CasEditorError("Unkown style!");
		}
	}	
}
