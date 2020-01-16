package sc.fiji.labeleditor.core.controller;

import net.imglib2.IterableInterval;
import net.imglib2.roi.labeling.LabelingType;
import org.scijava.Context;
import org.scijava.Initializable;
import org.scijava.plugin.Parameter;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.core.view.LabelEditorView;

import java.util.Set;

public class DefaultInteractiveLabeling<L> implements InteractiveLabeling<L>, Initializable {

	@Parameter
	Context context;

	protected final LabelEditorInterface<L> interfaceInstance;
	private final LabelEditorModel<L> model;
	private final LabelEditorView<L> view;

	public DefaultInteractiveLabeling(LabelEditorModel<L> model, LabelEditorView<L> view, LabelEditorInterface<L> interfaceInstance) {
		this.model = model;
		this.view = view;
		this.interfaceInstance = interfaceInstance;
	}

	@Override
	public LabelEditorModel<L> model() {
		return model;
	}

	@Override
	public LabelEditorView<L> view() {
		return view;
	}

	@Override
	public void initialize() {
		view.listeners().remove(interfaceInstance::onViewChange);
		model.tagging().listeners().remove(interfaceInstance::onTagChange);
		if(context != null) context.inject(interfaceInstance);
		view.listeners().add(interfaceInstance::onViewChange);
		model.tagging().listeners().add(interfaceInstance::onTagChange);
		interfaceInstance.display(view);
		interfaceInstance.installBehaviours(this);
	}

	@Override
	public LabelEditorInterface<L> interfaceInstance() {
		return interfaceInstance;
	}

	@Override
	public IterableInterval<LabelingType<L>> getLabelingInScope() {
		return model().labeling();
	}

	@Override
	public Set<L> getLabelSetInScope() {
		return model().labeling().getMapping().getLabels();
	}

	@Override
	public String toString() {
		return model().getName();
	}
}