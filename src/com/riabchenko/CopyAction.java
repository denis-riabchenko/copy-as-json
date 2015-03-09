package com.riabchenko;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ide.CopyPasteManager;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.StringSelection;

public class CopyAction extends AnAction {
    public static final DataKey<DefaultMutableTreeNode> SELECTED_NODE = DataKey.create("SELECTED_NODE");

    public CopyAction() {
    }

    public CopyAction(JComponent component) {
        final AnAction action = ActionManager.getInstance().getAction("$Copy");
        if (action != null) {
            copyFrom(action);
            registerCustomShortcutSet(getShortcutSet(), component);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(isEnabled(e));
    }

    public void actionPerformed(AnActionEvent e) {
        final DefaultMutableTreeNode node = e.getData(SELECTED_NODE);
        Object obj = node.getUserObject();
//        if (node instanceof GeneratedStructureModel.StructureNode) {
//            final GeneratedStructureModel.StructureNode structureNode = (GeneratedStructureModel.StructureNode)node;
//            final OutputEventQueue.NodeEvent event = structureNode.getUserObject();
//            setClipboardData(event.getValue());
//        }
    }

    private static void setClipboardData(String value) {
        CopyPasteManager.getInstance().setContents(new StringSelection(value));
    }

    protected static boolean isEnabled(AnActionEvent e) {
        return true;
//        final DefaultMutableTreeNode node = e.getData(SELECTED_NODE);
//        if (node instanceof GeneratedStructureModel.StructureNode) {
//            final GeneratedStructureModel.StructureNode structureNode = (GeneratedStructureModel.StructureNode)node;
//            final OutputEventQueue.NodeEvent event = structureNode.getUserObject();
//            return event != null && event.getValue() != null;
//        }
//        return false;
    }
}
