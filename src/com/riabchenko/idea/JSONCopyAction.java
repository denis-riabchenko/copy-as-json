package com.riabchenko.idea;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.openapi.project.Project;
import com.sun.jdi.Value;

public class JSONCopyAction extends BaseCopyAction {
    protected String processText(Project project, Value value, DebuggerTreeNodeImpl debuggerTreeNode, DebuggerContextImpl debuggerContext) throws EvaluateException {
        EvaluationContext evaluationContext = debuggerContext.createEvaluationContext();
        JSONWriter writer = new JSONWriter();
        return writer.valueToJson(evaluationContext, value);
    }
}
