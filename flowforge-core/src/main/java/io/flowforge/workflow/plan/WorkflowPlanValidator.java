package io.flowforge.workflow.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.flowforge.task.TaskDescriptor;
import io.flowforge.task.TaskId;

/**
 * Validates workflow plans before execution to catch structural issues early.
 * Performs checks for:
 * - Unique task IDs
 * - Existing dependencies
 * - Cycles in the dependency graph
 * - Consistent root configuration
 */
public final class WorkflowPlanValidator {

  private WorkflowPlanValidator() {
    // Utility class
  }

  /**
   * Validates a list of task descriptors.
   * 
   * @param descriptors the task descriptors to validate
   * @throws InvalidPlanException if validation fails
   */
  public static void validate(List<TaskDescriptor> descriptors) {
    if (descriptors == null || descriptors.isEmpty()) {
      throw new InvalidPlanException("Workflow plan cannot be empty");
    }

    validateUniqueIds(descriptors);
    validateDependenciesExist(descriptors);
    detectCycles(descriptors);
  }

  private static void validateUniqueIds(List<TaskDescriptor> descriptors) {
    Set<TaskId> seen = new HashSet<>();
    List<TaskId> duplicates = new ArrayList<>();

    for (TaskDescriptor descriptor : descriptors) {
      TaskId id = descriptor.id();
      if (!seen.add(id)) {
        duplicates.add(id);
      }
    }

    if (!duplicates.isEmpty()) {
      throw new InvalidPlanException(
          "Duplicate task IDs found: " + duplicates);
    }
  }

  private static void validateDependenciesExist(List<TaskDescriptor> descriptors) {
    Set<TaskId> allIds = new HashSet<>();
    for (TaskDescriptor descriptor : descriptors) {
      allIds.add(descriptor.id());
    }

    List<String> missingDeps = new ArrayList<>();
    for (TaskDescriptor descriptor : descriptors) {
      for (TaskId depId : descriptor.dependencies()) {
        if (!allIds.contains(depId)) {
          missingDeps.add(descriptor.id() + " depends on missing task: " + depId);
        }
      }
    }

    if (!missingDeps.isEmpty()) {
      throw new InvalidPlanException(
          "Missing dependencies: " + String.join(", ", missingDeps));
    }
  }

  private static void detectCycles(List<TaskDescriptor> descriptors) {
    // Build adjacency map
    Map<TaskId, Set<TaskId>> graph = new HashMap<>();
    for (TaskDescriptor descriptor : descriptors) {
      graph.put(descriptor.id(), new HashSet<>(descriptor.dependencies()));
    }

    // DFS with three colors: WHITE (unvisited), GRAY (visiting), BLACK (visited)
    Map<TaskId, Color> colors = new HashMap<>();
    for (TaskDescriptor descriptor : descriptors) {
      colors.put(descriptor.id(), Color.WHITE);
    }

    for (TaskDescriptor descriptor : descriptors) {
      if (colors.get(descriptor.id()) == Color.WHITE) {
        if (hasCycleDFS(descriptor.id(), graph, colors, new ArrayList<>())) {
          throw new InvalidPlanException(
              "Cycle detected in workflow graph starting at: " + descriptor.id());
        }
      }
    }
  }

  private static boolean hasCycleDFS(
      TaskId current,
      Map<TaskId, Set<TaskId>> graph,
      Map<TaskId, Color> colors,
      List<TaskId> path) {
    colors.put(current, Color.GRAY);
    path.add(current);

    Set<TaskId> dependencies = graph.get(current);
    if (dependencies != null) {
      for (TaskId dep : dependencies) {
        Color depColor = colors.get(dep);
        if (depColor == Color.GRAY) {
          // Back edge found - cycle detected
          return true;
        }
        if (depColor == Color.WHITE) {
          if (hasCycleDFS(dep, graph, colors, path)) {
            return true;
          }
        }
      }
    }

    colors.put(current, Color.BLACK);
    path.remove(path.size() - 1);
    return false;
  }

  private enum Color {
    WHITE, GRAY, BLACK
  }
}
