package com.riabchenko.idea;

import com.intellij.debugger.engine.SuspendContext;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.engine.managerThread.SuspendContextCommand;
import com.sun.jdi.Value;

public abstract class ToJsonCommand implements SuspendContextCommand {
    private final EvaluationContext evaluationContext;
    private final Value value;
    private boolean evaluated = false;

    protected ToJsonCommand(EvaluationContext evaluationContext, Value variable) {
        this.evaluationContext = evaluationContext;
        this.value = variable;
    }

    public void action() {
        if(!this.evaluated) {
            try {
                JSONWriter writer = new JSONWriter();
                String message = writer.valueToJson(this.evaluationContext, this.value);
                this.evaluationResult(message);
            } catch (EvaluateException ex) {
                this.evaluationError(ex.getMessage());
            }

        }
    }

    public void commandCancelled() {
    }

    public void setEvaluated() {
        this.evaluated = true;
    }

    public SuspendContext getSuspendContext() {
        return this.evaluationContext.getSuspendContext();
    }

    public abstract void evaluationResult(String message);

    public abstract void evaluationError(String message);

    public Value getValue() {
        return this.value;
    }

    public EvaluationContext getEvaluationContext() {
        return this.evaluationContext;
    }
}
