package com.indago.labeleditor.plugin.actions;

import com.indago.labeleditor.core.controller.LabelEditorActions;
import com.indago.labeleditor.core.controller.LabelEditorController;
import com.indago.labeleditor.core.model.LabelEditorModel;
import com.indago.labeleditor.core.model.tagging.LabelEditorTag;
import com.indago.labeleditor.core.view.LabelEditorView;
import net.imglib2.roi.labeling.LabelingType;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SelectionActions<L> implements LabelEditorActions {

	protected final LabelEditorModel<L> model;
	protected final LabelEditorView<L> renderer;
	private final LabelEditorController<L> actionManager;
	protected LabelingType<L> currentLabels;
	protected int currentSegment = -1;

	public SelectionActions(LabelEditorModel<L> model, LabelEditorView<L> renderer, LabelEditorController<L> actionManager) {
		this.model = model;
		this.renderer = renderer;
		this.actionManager = actionManager;
	}

	protected synchronized void handleMouseMove(MouseEvent e) {
		LabelingType<L> labels = actionManager.viewer().getLabelsAtMousePosition(e, model);
		int intIndex;
		try {
			intIndex = labels.getIndex().getInteger();
		} catch(ArrayIndexOutOfBoundsException exc) {
			//TODO pause model listeners
			defocusAll();
			//TODO resume model listeners
			return;
		}
		if(intIndex == currentSegment) return;
		currentSegment = intIndex;
//		new Thread(() -> {
			//TODO pause model listeners
			defocusAll();
			currentLabels = labels;
			labels.forEach(this::focus);
			//TODO resume model listeners
//		}).start();
	}

	protected void handleClick() {
		//TODO pause model listeners
		if (noLabelsAtMousePosition()) {
			deselectAll();
		} else {
			selectFirst(currentLabels);
		}
		//TODO resume model listeners
	}

	protected void handleShiftClick() {
		//TODO pause model listeners
		if (!noLabelsAtMousePosition()) {
			addFirstToSelection(currentLabels);
		}
		//TODO resume model listeners
	}

	protected boolean noLabelsAtMousePosition() {
		return currentLabels == null || currentLabels.size() == 0;
	}

	protected void handleWheelRotation(double direction, boolean isHorizontal) {
		if(noLabelsAtMousePosition()) return;
		if(!anySelected(currentLabels)) {
			//TODO pause model listeners
			selectFirst(currentLabels);
			//TODO resume model listeners
		}
		if ( !isHorizontal ) {
			//TODO pause model listeners
			if (direction > 0)
				selectNext(currentLabels);
			else
				selectPrevious(currentLabels);
			//TODO resume model listeners
		}
	}

	protected void selectFirst(LabelingType<L> currentLabels) {
		L label = getFirst(currentLabels);
		if(model.tagging().getTags(label).contains(LabelEditorTag.SELECTED)) return;
		deselectAll();
		select(label);
	}

	protected void addFirstToSelection(LabelingType<L> currentLabels) {
		L label = getFirst(currentLabels);
		if(model.tagging().getTags(label).contains(LabelEditorTag.SELECTED)) return;
		select(label);
	}

	protected L getFirst(LabelingType<L> currentLabels) {
		List<L> orderedLabels = new ArrayList<>(currentLabels);
		orderedLabels.sort(model.getLabelComparator());
		return orderedLabels.get(0);
	}

	protected boolean isSelected(L label) {
		return model.tagging().getTags(label).contains(LabelEditorTag.SELECTED);
	}

	protected boolean anySelected(LabelingType<L> labels) {
		return labels.stream().anyMatch(label -> model.tagging().getTags(label).contains(LabelEditorTag.SELECTED));
	}

	protected void select(L label) {
		model.tagging().addTag(LabelEditorTag.SELECTED, label);
	}

	protected void selectPrevious(LabelingType<L> labels) {
		List<L> reverseLabels = new ArrayList<>(labels);
		Collections.reverse(reverseLabels);
		selectNext(reverseLabels);
	}

	protected void selectNext(Collection<L> labels) {
		boolean foundSelected = false;
		for (Iterator<L> iterator = labels.iterator(); iterator.hasNext(); ) {
			L label = iterator.next();
			if (isSelected(label)) {
				foundSelected = true;
				if(iterator.hasNext()) {
					deselect(label);
				}
			} else {
				if (foundSelected) {
					select(label);
					return;
				}
			}
		}
	}

	protected void deselect(L label) {
		model.tagging().removeTag(LabelEditorTag.SELECTED, label);
	}

	protected void deselectAll() {
		model.tagging().removeTag(LabelEditorTag.SELECTED);
	}

	protected void defocusAll() {
		model.tagging().removeTag(LabelEditorTag.MOUSE_OVER);
		currentLabels = null;
	}

	protected void focus(L label) {
		model.tagging().addTag(LabelEditorTag.MOUSE_OVER, label);
	}
}