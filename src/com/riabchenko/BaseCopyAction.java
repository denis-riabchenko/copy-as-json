package com.riabchenko;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.actions.DebuggerAction;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl;
import com.intellij.debugger.ui.tree.ValueDescriptor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.util.ProgressWindowWithNotification;
import com.intellij.openapi.project.Project;
import com.sun.jdi.Value;
import org.jetbrains.annotations.Nullable;

public abstract class BaseCopyAction extends DebuggerAction {

    public void actionPerformed(AnActionEvent e) {
        final DataContext actionContext = e.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(actionContext);
        final DebuggerTreeNodeImpl node = getSelectedNode(actionContext);
//        final String text = getValueText(node);
//        if (text != null) {
//            DebuggerInvocationUtil.swingInvokeLater(project, new Runnable() {
//                public void run() {
//                    processText(project, text, node, null);
//                }
//            });
//            return;
//        }
        final Value value = getValue(node);
        if (value == null) {
            return;
        }
        final DebuggerManagerEx debuggerManager = DebuggerManagerEx.getInstanceEx(project);
        if(debuggerManager == null) {
            return;
        }
        final DebuggerContextImpl debuggerContext = debuggerManager.getContext();
        if (debuggerContext == null || debuggerContext.getDebuggerSession() == null) {
            return;
        }

        final ProgressWindowWithNotification progressWindow = new ProgressWindowWithNotification(true, project);
        SuspendContextCommandImpl getTextCommand = new SuspendContextCommandImpl(debuggerContext.getSuspendContext()) {
            public Priority getPriority() {
                return Priority.HIGH;
            }

            public void contextAction() throws Exception {
                //noinspection HardCodedStringLiteral
                progressWindow.setText(DebuggerBundle.message("progress.evaluating", "toString()"));

                final String valueAsString = DebuggerUtilsEx.getValueOrErrorAsString(debuggerContext.createEvaluationContext(), value);

                if (progressWindow.isCanceled()) {
                    return;
                }

                DebuggerInvocationUtil.swingInvokeLater(project, new Runnable() {
                    public void run() {
                        String text = valueAsString;
                        if (text == null) {
                            text = "";
                        }
                        processText(project, text, node, debuggerContext);
                    }
                });
            }
        };
        progressWindow.setTitle(DebuggerBundle.message("title.evaluating"));
        debuggerContext.getDebugProcess().getManagerThread().startProgress(getTextCommand, progressWindow);
    }

    protected abstract void processText(final Project project, String text, DebuggerTreeNodeImpl node, DebuggerContextImpl debuggerContext);

    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Value value = getValue(getSelectedNode(e.getDataContext()));
        presentation.setEnabled(value != null);
        presentation.setVisible(value != null);
    }


    @Nullable
    private static Value getValue(final DebuggerTreeNodeImpl node) {
        if (node == null) {
            return null;
        }
        NodeDescriptorImpl descriptor = node.getDescriptor();
        if (!(descriptor instanceof ValueDescriptor)) {
            return null;
        }
        return ((ValueDescriptor)descriptor).getValue();
    }
}