package io.flowforge.workflow.result;

import java.util.List;
import java.util.Objects;

import io.flowforge.workflow.ReactiveExecutionContext;
import io.flowforge.workflow.graph.TaskNode;
import io.flowforge.workflow.plan.WorkflowExecutionPlan;

public final class WorkflowResultSelector {

  private WorkflowResultSelector() {
  }

  /**
   * Determines the final result of a workflow execution.
   * <p>
   * Rule:
   * <ul>
   * <li>If the plan has exactly one terminal task (no outgoing dependencies),
   * return its output.</li>
   * <li>Otherwise (multiple terminals or none), return the full execution
   * context.</li>
   * </ul>
   */
  public static Object select(WorkflowExecutionPlan plan, ReactiveExecutionContext ctx) {
    Objects.requireNonNull(plan, "plan");
    Objects.requireNonNull(ctx, "ctx");

    List<TaskNode> terminals = plan.nodes().stream()
        .filter(n -> n.dependents() == null || n.dependents().isEmpty())
        .toList();

    if (terminals.size() != 1) {
      return ctx;
    }

    TaskNode terminal = terminals.get(0);
    return ctx.get(terminal.descriptor().task().outputKey()).orElse(null);
  }
}
