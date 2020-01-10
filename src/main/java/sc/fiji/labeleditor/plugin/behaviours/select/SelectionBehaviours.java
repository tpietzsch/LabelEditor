package sc.fiji.labeleditor.plugin.behaviours.select;

import sc.fiji.labeleditor.core.controller.LabelEditorBehaviours;
import sc.fiji.labeleditor.core.controller.LabelEditorController;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.core.model.tagging.LabelEditorTag;
import sc.fiji.labeleditor.core.view.LabelEditorView;
import net.imglib2.roi.labeling.LabelingType;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SelectionBehaviours<L> implements LabelEditorBehaviours<L> {

	@Parameter
	CommandService commandService;

	protected LabelEditorModel<L> model;
	protected LabelEditorController<L> controller;

	protected static final String TOGGLE_LABEL_SELECTION_NAME = "LABELEDITOR_TOGGLELABELSELECTION";
	protected static final String TOGGLE_LABEL_SELECTION_TRIGGERS = "shift scroll";
	protected static final String SELECT_FIRST_LABEL_NAME = "LABELEDITOR_SELECTFIRSTLABEL";
	protected static final String SELECT_FIRST_LABEL_TRIGGERS = "button1";
	protected static final String ADD_LABEL_TO_SELECTION_NAME = "LABELEDITOR_ADDLABELTOSELECTION";
	protected static final String ADD_LABEL_TO_SELECTION_TRIGGERS = "shift button1";
	protected static final String SELECT_ALL_LABELS_NAME = "LABELEDITOR_SELECTALL";
	protected static final String SELECT_ALL_LABELS_TRIGGERS = "ctrl A";

	@Override
	public void init(LabelEditorModel<L> model, LabelEditorView<L> view, LabelEditorController<L> controller) {
		this.model = model;
		this.controller = controller;
	}

	@Override
	public void install(Behaviours behaviours, Component panel) {

		behaviours.behaviour(getToggleLabelSelectionBehaviour(), TOGGLE_LABEL_SELECTION_NAME, TOGGLE_LABEL_SELECTION_TRIGGERS);
		behaviours.behaviour(getSelectFirstLabelBehaviour(), SELECT_FIRST_LABEL_NAME, SELECT_FIRST_LABEL_TRIGGERS);
		behaviours.behaviour(getAddFirstLabelToSelectionBehaviour(), ADD_LABEL_TO_SELECTION_NAME, ADD_LABEL_TO_SELECTION_TRIGGERS);
		behaviours.behaviour(getSelectAllBehaviour(), SELECT_ALL_LABELS_NAME, SELECT_ALL_LABELS_TRIGGERS);

	}

	private Behaviour getSelectAllBehaviour() {
		return (ClickBehaviour) (arg0, arg1) -> {
			model.tagging().pauseListeners();
			selectAll();
			model.tagging().resumeListeners();
		};
	}

	protected Behaviour getAddFirstLabelToSelectionBehaviour() {
		return (ClickBehaviour) (arg0, arg1) -> {
			model.tagging().pauseListeners();
			addFirstLabelToSelection(arg0, arg1);
			model.tagging().resumeListeners();
		};
	}

	protected Behaviour getSelectFirstLabelBehaviour() {
		return (ClickBehaviour) (arg0, arg1) -> {
			model.tagging().pauseListeners();
			selectFirstLabel(arg0, arg1);
			model.tagging().resumeListeners();
		};
	}

	protected Behaviour getToggleLabelSelectionBehaviour() {
		return (ScrollBehaviour) (wheelRotation, isHorizontal, x, y) -> {
			if(!isHorizontal) {
				model.tagging().pauseListeners();
				toggleLabelSelection(wheelRotation > 0, x, y);
				model.tagging().resumeListeners();
			}};
	}

	public void selectAll() {
		controller.labelSetInScope().forEach(this::select);
	}

	protected void selectFirstLabel(int x, int y) {
		LabelingType<L> labels = controller.interfaceInstance().findLabelsAtMousePosition(x, y, model);
		if (foundLabels(labels)) {
			selectFirst(labels);
		} else {
			deselectAll();
		}
	}

	private boolean foundLabels(LabelingType<L> labels) {
		return labels != null && labels.size() > 0;
	}

	protected void addFirstLabelToSelection(int x, int y) {
		LabelingType<L> labels = controller.interfaceInstance().findLabelsAtMousePosition(x, y, model);
		if (foundLabels(labels)) {
			toggleSelectionOfFirst(labels);
		}
	}

	protected void toggleLabelSelection(boolean forwardDirection, int x, int y) {
		LabelingType<L> labels = controller.interfaceInstance().findLabelsAtMousePosition(x, y, model);
		if(!foundLabels(labels)) return;
		if(!anySelected(labels)) {
			selectFirst(labels);
			return;
		}
		if (forwardDirection)
			selectNext(labels);
		else
			selectPrevious(labels);
	}

	protected void selectFirst(LabelingType<L> labels) {
		L label = getFirst(labels);
		if(model.tagging().getTags(label).contains(LabelEditorTag.SELECTED)) return;
		deselectAll();
		select(label);
	}

	protected void toggleSelectionOfFirst(LabelingType<L> labels) {
		L label = getFirst(labels);
		if(model.tagging().getTags(label).contains(LabelEditorTag.SELECTED)) {
			deselect(label);
		} else {
			select(label);
		}
	}

	protected L getFirst(LabelingType<L> labels) {
		if(labels.size() == 0) return null;
		List<L> orderedLabels = new ArrayList<>(labels);
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
		model.tagging().addTagToLabel(LabelEditorTag.SELECTED, label);
		model.tagging().removeTagFromLabel(LabelEditorTag.FOCUS, label);
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
		model.tagging().removeTagFromLabel(LabelEditorTag.SELECTED, label);
	}

	public void deselectAll() {
		controller.labelSetInScope().forEach(label -> model.tagging().removeTagFromLabel(LabelEditorTag.SELECTED, label));
	}

	public void invertSelection() {
		Set<L> all = new HashSet(controller.labelSetInScope());
		Set<L> selected = model.tagging().filterLabelsWithTag(all, LabelEditorTag.SELECTED);
		all.removeAll(selected);
		all.forEach(label -> select(label));
		selected.forEach(label -> deselect(label));
	}

	public void selectByTag() {
		commandService.run(SelectByTagCommand.class, true,
				"model", model, "control", controller);
	}
}
