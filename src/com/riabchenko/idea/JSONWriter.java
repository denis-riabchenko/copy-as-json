package com.riabchenko.idea;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.gson.stream.JsonWriter;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.sun.jdi.*;

public class JSONWriter {
    public String valueToJson(EvaluationContext evaluationContext, Value value) throws
            EvaluateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.setIndent("    ");

            writeValue(evaluationContext, null, value, writer);

            writer.close();

            return outputStream.toString("UTF-8");
        } catch (IOException ex) {
            throw EvaluateExceptionUtil.createEvaluateException("Can't write JSON.", ex);
        }
    }

    private void writeValue(EvaluationContext evaluationContext, DebugProcess debugProcess, Value value, JsonWriter writer) throws EvaluateException {
        try {
            if (value == null) {
                writer.nullValue();
            } else if (value instanceof StringReference) {
                writer.value(((StringReference) value).value());
            } else if (DebuggerUtilsEx.isInteger(value)) {
                writer.value(((PrimitiveValue) value).longValue());
            } else if (DebuggerUtilsEx.isNumeric(value)) {
                writer.value(((PrimitiveValue) value).doubleValue());
            } else if (value instanceof BooleanValue) {
                writer.value(((PrimitiveValue) value).booleanValue());
            } else if (value instanceof CharValue) {
                writer.value(((PrimitiveValue) value).charValue());
            } else if (value instanceof ArrayReference) {
                writer.beginArray();

                for (Value element : ((ArrayReference)value).getValues()) {
                    writeValue(evaluationContext, debugProcess, element, writer);
                }

                writer.endArray();
            } else if (value instanceof ObjectReference) {
                writer.beginObject();

                ObjectReference objRef = (ObjectReference) value;
                List<Method> methods = findMethods(objRef.referenceType());
                if (methods != null) {
                    if (debugProcess == null) {
                        debugProcess = evaluationContext.getDebugProcess();
                    }
                    for (Method method : methods) {
                        try {
                            Value val = debugProcess.invokeInstanceMethod(evaluationContext, objRef, method, Collections
                                    .emptyList(), 0);
                            String name = methodToPropertyName(method);
                            writeValue(evaluationContext, debugProcess, val, writer.name(name));
                        } catch (Exception ignored) {
                            ignored.printStackTrace();
                        }
                    }
                }

                writer.endObject();
            } else {
                writeValue(evaluationContext, debugProcess, value, writer);
            }
        } catch (IOException ex) {
            throw EvaluateExceptionUtil.createEvaluateException("Can't evaluate.", ex);
        }
    }

    private List<Method> findMethods(ReferenceType refType) {
        List<Method> result = new ArrayList<Method>();

        List<Method> methods = refType.visibleMethods();
        if (methods != null) {
            for (Method method : methods) {
                if (!StringUtils.equals(method.declaringType().name(), Object.class.getCanonicalName())
                        && !method.isConstructor() && StringUtils.startsWith(method.name(), "get")
                        && (method.argumentTypeNames() == null || method.argumentTypeNames().isEmpty())) {
                    result.add(method);
                }
            }
        }

        return result;
    }

    private String methodToPropertyName(Method method) {
        String name = method.name();
        if ((name.startsWith("set") || name.startsWith("get")) && name.length() > 3) {
            String ret = name.substring(3, 4).toLowerCase();
            if (name.length() > 4)
                ret += name.substring(4);
            name = ret;
        }
        return name;
    }
}
