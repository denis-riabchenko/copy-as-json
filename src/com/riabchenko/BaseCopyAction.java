package com.riabchenko;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.DebuggerInvocationUtil;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.actions.DebuggerAction;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl;
import com.intellij.debugger.ui.tree.ValueDescriptor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.util.ProgressWindowWithNotification;
import com.intellij.openapi.project.Project;
import com.sun.jdi.Value;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.StringSelection;

public abstract class BaseCopyAction extends DebuggerAction {
    public void actionPerformed(AnActionEvent e) {
        final DataContext actionContext = e.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(actionContext);
        final DebuggerTreeNodeImpl node = getSelectedNode(actionContext);
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

                final String valueAsString = processText(project, value, node, debuggerContext);

                if (progressWindow.isCanceled()) {
                    return;
                }

                DebuggerInvocationUtil.swingInvokeLater(project, new Runnable() {
                    public void run() {
                        CopyPasteManager.getInstance().setContents(new StringSelection(valueAsString));
                    }
                });
            }
        };
        progressWindow.setTitle(DebuggerBundle.message("title.evaluating"));
        debuggerContext.getDebugProcess().getManagerThread().startProgress(getTextCommand, progressWindow);
    }

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

    protected abstract String processText(Project project, Value value, DebuggerTreeNodeImpl debuggerTreeNode, DebuggerContextImpl debuggerContext);
}