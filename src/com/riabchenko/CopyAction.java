package com.riabchenko;

import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;

import java.awt.datatransfer.StringSelection;

public class CopyAction extends BaseCopyAction {
    protected void processText(Project project, String text, DebuggerTreeNodeImpl debuggertreenodeimpl, DebuggerContextImpl debuggercontextimpl) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }
}
