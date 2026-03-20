package org.royada.flowforge.workflow.result;

import java.util.List;
import java.util.Objects;

import org.royada.flowforge.workflow.ReactiveExecutionContext;
import org.royada.flowforge.workflow.graph.TaskNode;
import org.royada.flowforge.workflow.plan.WorkflowExecutionPlan;

/**
 * Helper class for selecting the final output of a workflow execution.
 */
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
   * 
   * @param plan the workflow execution plan
   * @param ctx the execution context
   * @return the selected result object
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
