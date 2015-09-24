package com.riabchenko.idea;

import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import com.intellij.debugger.DebuggerContext;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.tree.DebuggerTreeNode;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.ValueDescriptor;
import com.intellij.debugger.ui.tree.render.*;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiExpression;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Type;
import com.sun.jdi.Value;

public class JsonRenderer extends NodeRendererImpl {
    public static final @NonNls String UNIQUE_ID = "JsonRenderer".intern();

    private boolean USE_CLASS_FILTERS = false;
    private ClassFilter[] myClassFilters = ClassFilter.EMPTY_ARRAY;

    public JsonRenderer() {
        setEnabled(true);
    }

    @Override
    public String getUniqueId() {
        return UNIQUE_ID;
    }

    @Override
    public @NonNls String getName() {
        return "toString";
    }

    @Override
    public void setName(String name) {
        // prohibit change
    }

    @Override
    public JsonRenderer clone() {
        final JsonRenderer cloned = (JsonRenderer) super.clone();
        final ClassFilter[] classFilters = (myClassFilters.length > 0) ? new ClassFilter[myClassFilters.length] : ClassFilter.EMPTY_ARRAY;
        for (int idx = 0; idx < classFilters.length; idx++) {
            classFilters[idx] = myClassFilters[idx].clone();
        }
        cloned.myClassFilters = classFilters;
        return cloned;
    }

    @Override
    public String calcLabel(final ValueDescriptor valueDescriptor, EvaluationContext evaluationContext, final DescriptorLabelListener labelListener)
            throws EvaluateException {
        final Value value = valueDescriptor.getValue();
        BatchEvaluator.getBatchEvaluator(evaluationContext.getDebugProcess()).invoke(new ToStringCommand(evaluationContext, value) {
            private boolean evaluated = false;

            @Override
            public void action() {
                if(!evaluated) {
                    try {
                        JSONWriter writer = new JSONWriter();
                        String message = writer.valueToJson(getEvaluationContext(), getValue());
                        this.evaluationResult(message);
                    } catch (EvaluateException ex) {
                        this.evaluationError(ex.getMessage());
                    }

                }
            }

            @Override
            public void evaluationResult(String message) {
                valueDescriptor.setValueLabel(
                        message == null ? "" : "\"" + DebuggerUtils.convertToPresentationString(DebuggerUtilsEx.truncateString(message)) + "\""
                );
                labelListener.labelChanged();
            }

            @Override
            public void evaluationError(String message) {
                final String msg = value != null ? message + " Can't evaluate to Json" : message;
                valueDescriptor.setValueLabelFailed(new EvaluateException(msg, null));
                labelListener.labelChanged();
            }

            public void setEvaluated() {
                this.evaluated = true;
            }
        });
        return XDebuggerUIConstants.COLLECTING_DATA_MESSAGE;
    }

    public boolean isUseClassFilters() {
        return USE_CLASS_FILTERS;
    }

    public void setUseClassFilters(boolean value) {
        USE_CLASS_FILTERS = value;
    }

    @Override
    public boolean isApplicable(Type type) {
        return type instanceof ReferenceType && (!USE_CLASS_FILTERS || isFiltered(type));
    }

    @Override
    public void buildChildren(Value value, ChildrenBuilder builder, EvaluationContext evaluationContext) {
        final DebugProcessImpl debugProcess = (DebugProcessImpl)evaluationContext.getDebugProcess();
        debugProcess.getDefaultRenderer(value).buildChildren(value, builder, evaluationContext);
    }

    @Override
    public PsiExpression getChildValueExpression(DebuggerTreeNode node, DebuggerContext context) throws EvaluateException {
        final Value parentValue = ((ValueDescriptor)node.getParent().getDescriptor()).getValue();
        final DebugProcessImpl debugProcess = (DebugProcessImpl)context.getDebugProcess();
        return debugProcess.getDefaultRenderer(parentValue).getChildValueExpression(node, context);
    }

    @Override
    public boolean isExpandable(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
        final DebugProcessImpl debugProcess = (DebugProcessImpl)evaluationContext.getDebugProcess();
        return debugProcess.getDefaultRenderer(value).isExpandable(value, evaluationContext, parentDescriptor);
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        final String value = JDOMExternalizerUtil.readField(element, "USE_CLASS_FILTERS");
        USE_CLASS_FILTERS = "true".equalsIgnoreCase(value);
        myClassFilters = DebuggerUtilsEx.readFilters(element.getChildren("filter"));
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        JDOMExternalizerUtil.writeField(element, "USE_CLASS_FILTERS", USE_CLASS_FILTERS ? "true" : "false");
        DebuggerUtilsEx.writeFilters(element, "filter", myClassFilters);
    }

    public ClassFilter[] getClassFilters() {
        return myClassFilters;
    }

    public void setClassFilters(ClassFilter[] classFilters) {
        myClassFilters = classFilters != null ? classFilters : ClassFilter.EMPTY_ARRAY;
    }

    private boolean isFiltered(Type t) {
        if (t instanceof ReferenceType) {
            for (ClassFilter classFilter : myClassFilters) {
                if (classFilter.isEnabled() && DebuggerUtilsEx.getSuperType(t, classFilter.getPattern()) != null) {
                    return true;
                }
            }
        }
        return DebuggerUtilsEx.isFiltered(t.name(), myClassFilters);
    }
}
