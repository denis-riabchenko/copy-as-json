package com.riabchenko;

import com.google.gson.Gson;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.ui.impl.watch.DebuggerTreeNodeImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.sun.jdi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CopyAction extends BaseCopyAction {
    private static final Key TO_STRING_METHOD_KEY = new Key("CachedToStringMethod");

    protected String processText(Project project, Value value, DebuggerTreeNodeImpl debuggerTreeNode, DebuggerContextImpl debuggerContext) {
        Gson gson = new Gson();
        return gson.toJson(getValueOrError(debuggerContext.createEvaluationContext(), value));
    }


    private Object getValueOrError(EvaluationContextImpl evaluationContext, Value value) {
        Object result = null;
        try {
            if (value instanceof ObjectReference) {
                if (value instanceof ArrayReference) {
                    List<Object> list = new ArrayList<Object>();
                    for (Value element : ((ArrayReference)value).getValues()) {
                        list.add(getValueOrError(evaluationContext, element));
                    }
                    return list;
                }

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
                result = debugProcess.invokeInstanceMethod(evaluationContext, objRef, toStringMethod, Collections.emptyList(), 0);
            }
        } catch (Exception ex) {
            result = ex.getMessage();
        }

        if (result == null) {
            return "null";
        } else {
            return (result instanceof StringReference) ? ((StringReference) result).value() : result.toString();
        }
    }

    public static Method findMethod(ReferenceType refType, String methodName, String methodSignature) {
        Method method;
        if (refType instanceof ArrayType) {
            method = findMethod(refType.virtualMachine().classesByName("java.lang.Object").get(0), methodName, methodSignature);
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
