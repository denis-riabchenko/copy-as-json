package com.riabchenko;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.sun.jdi.*;

import java.util.Collections;
import java.util.List;

public class CopyAction extends BaseCopyAction {
    private static final Key TO_STRING_METHOD_KEY = new Key("CachedToStringMethod");

    protected String processText(Project project, Value value, DebuggerTreeNodeImpl debuggerTreeNode, DebuggerContextImpl debuggerContext) {
        // OK
        //return DebuggerUtilsEx.getValueOrErrorAsString(debuggerContext.createEvaluationContext(), value);

        // NOT OK
        //  LinkageError: loader constraint violation: loader (instance of com/intellij/ide/plugins/cl/PluginClassLoader) previously initiated loading for a different type with name "com/sun/jdi/Value": loader constraint violation: loader (instance of com/intellij/ide/plugins/cl/PluginClassLoader) previously initiated loading for a different type with name "com/sun/jdi/Value"
        return getValueOrErrorAsString(debuggerContext.createEvaluationContext(), value);
    }


    private String getValueOrErrorAsString(EvaluationContextImpl evaluationContext, Value value) {
        try {
            try {
                if (value == null)
                    return "null";
            } catch (ObjectCollectedException ignored) {
                throw EvaluateExceptionUtil.OBJECT_WAS_COLLECTED;
            }
            if (value instanceof ObjectReference) {
                ObjectReference objRef = (ObjectReference) value;
                DebugProcess debugProcess = evaluationContext.getDebugProcess();
                Method toStringMethod = (Method) debugProcess.getUserData(TO_STRING_METHOD_KEY);
                if (toStringMethod == null)
                    try {
                        ReferenceType refType = objRef.virtualMachine().classesByName("java.lang.Object").get(0);
                        toStringMethod = findMethod(refType, "toString", "()Ljava/lang/String;");
                        debugProcess.putUserData(TO_STRING_METHOD_KEY, toStringMethod);
                    } catch (Exception ignored) {
                        throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.cannot.evaluate.tostring", new Object[]{
                                objRef.referenceType().name()
                        }));
                    }
                if (toStringMethod == null)
                    throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.cannot.evaluate.tostring", new Object[]{
                            objRef.referenceType().name()
                    }));
                Value result = debugProcess.invokeInstanceMethod(evaluationContext, objRef, toStringMethod, Collections.emptyList(), 0);
                if (result == null)
                    return "null";
                return (result instanceof StringReference) ? ((StringReference) result).value() : result.toString();
            }
        } catch (EvaluateException evaluateexception) {
            return evaluateexception.getMessage();
        }
        return "null";
    }

    public static Method findMethod(ReferenceType refType, String methodName, String methodSignature) {
        Method method;
        if (refType instanceof ArrayType) {
            method = findMethod((ReferenceType) refType.virtualMachine().classesByName("java.lang.Object").get(0), methodName, methodSignature);
            if (method != null)
                return method;
        }
        method = null;
        if (methodSignature != null) {
            if (refType instanceof ClassType)
                method = ((ClassType) refType).concreteMethodByName(methodName, methodSignature);
            if (method == null) {
                List methods = refType.methodsByName(methodName, methodSignature);
                if (methods.size() > 0)
                    method = (Method) methods.get(0);
            }
        } else {
            List methods = null;
            if (refType instanceof ClassType)
                methods = refType.methodsByName(methodName);
            if (methods != null && methods.size() > 0)
                method = (Method) methods.get(0);
        }
        return method;
    }
}
