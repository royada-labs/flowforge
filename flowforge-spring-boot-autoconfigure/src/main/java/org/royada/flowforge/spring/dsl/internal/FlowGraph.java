package org.royada.flowforge.spring.dsl.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.royada.flowforge.spring.dsl.DefaultFlowBranch;
import org.royada.flowforge.spring.dsl.FlowBranch;
import org.royada.flowforge.spring.dsl.FlowBuilder;
import org.royada.flowforge.task.TaskDefinition;
import org.royada.flowforge.task.TaskId;
import org.royada.flowforge.validation.TypeMetadata;
import org.royada.flowforge.workflow.policy.ExecutionPolicy;

/**
 * Mutable graph model used by the Flow DSL before materialization into an execution plan.
 */
public class FlowGraph {

    private final Map<TaskId, TaskDefinition<?, ?>> definitions = new LinkedHashMap<>();
    private final Set<Edge> edges = new LinkedHashSet<>();
    private final Map<TaskId, ExecutionPolicy> policies = new LinkedHashMap<>();

    private final TaskId start;
    /**
     * Current tail nodes where the next sequential step will be attached.
     */
    protected Set<TaskId> tails;

    private FlowGraph(TaskDefinition<?, ?> startTask) {
        Objects.requireNonNull(startTask, "startTask");
        registerDefinition(startTask);
        this.start = startTask.id();
        this.tails = new LinkedHashSet<>();
        this.tails.add(start);
    }

    /**
     * Creates a new graph starting from the provided task.
     *
     * @param startTask start task definition
     * @return new graph instance
     */
    public static FlowGraph start(TaskDefinition<?, ?> startTask) {
        return new FlowGraph(startTask);
    }

    /**
     * Returns the start node id.
     *
     * @return start task id
     */
    public TaskId start() {
        return start;
    }

    /**
     * Returns all registered node ids.
     *
     * @return immutable node id set
     */
    public Set<TaskId> nodes() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    /**
     * Returns graph edges.
     *
     * @return immutable edge set
     */
    public Set<Edge> edges() {
        return Collections.unmodifiableSet(edges);
    }

    /**
     * Returns current graph tails.
     *
     * @return immutable tail set
     */
    public Set<TaskId> tails() {
        return Collections.unmodifiableSet(tails);
    }

    /**
     * Returns per-task type metadata gathered from task definitions.
     *
     * @return immutable type metadata map
     */
    public Map<TaskId, TypeMetadata> typeMetadata() {
        Map<TaskId, TypeMetadata> metadata = new LinkedHashMap<>();
        for (TaskDefinition<?, ?> definition : definitions.values()) {
            metadata.put(definition.id(), new TypeMetadata(definition.inputType(), definition.outputType()));
        }
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Appends a sequential task after all current tails.
     *
     * @param task task definition to append
     */
    public void then(TaskDefinition<?, ?> task) {
        Objects.requireNonNull(task, "task");
        registerDefinition(task);

        TaskId taskId = task.id();
        for (TaskId tail : tails) {
            edges.add(new Edge(tail, taskId));
        }

        tails = new LinkedHashSet<>();
        tails.add(taskId);
    }

    /**
     * Returns any policy currently attached to the given task id.
     *
     * @param taskId task id
     * @return optional policy
     */
    public Optional<ExecutionPolicy> policy(TaskId taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return Optional.ofNullable(policies.get(taskId));
    }

    /**
     * Attaches or composes an execution policy for a task present in this graph.
     *
     * @param taskId task id
     * @param policy policy to apply
     */
    public void applyPolicy(TaskId taskId, ExecutionPolicy policy) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(policy, "policy");
        if (!definitions.containsKey(taskId)) {
            throw new IllegalArgumentException("Unknown task id for policy assignment: " + taskId.getValue());
        }
        policies.merge(taskId, policy, ExecutionPolicy::andThen);
    }

    /**
     * Alias for {@link #then(TaskDefinition)} when joining branches.
     *
     * @param task join task definition
     */
    public void join(TaskDefinition<?, ?> task) {
        then(task);
    }

    /**
     * Forks execution into parallel branch builders and merges tails afterward.
     *
     * @param branches branch consumers
     * @param builder flow builder used inside branch wrappers
     * @param referenceResolver resolver for method references inside branches
     */
    public void fork(List<Consumer<FlowBranch>> branches, FlowBuilder builder, TaskReferenceResolver referenceResolver) {
        Objects.requireNonNull(branches, "branches");
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(referenceResolver, "referenceResolver");
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("fork requires at least 1 branch");
        }

        Set<TaskId> unionTails = new LinkedHashSet<>();

        for (Consumer<FlowBranch> consumer : branches) {
            Objects.requireNonNull(consumer, "branch consumer");

            FlowGraph branch = new BranchOverlayGraph(this, this.tails);
            consumer.accept(new DefaultFlowBranch(branch, builder, referenceResolver));

            unionTails.addAll(branch.tails());
        }

        this.tails = unionTails;
    }

    /**
     * Registers a definition or verifies equality for existing ids.
     *
     * @param task task definition
     */
    protected final void registerDefinition(TaskDefinition<?, ?> task) {
        TaskDefinition<?, ?> existing = definitions.putIfAbsent(task.id(), task);
        if (existing != null && !existing.equals(task)) {
            throw new IllegalArgumentException(
                    "Conflicting task definitions for id '" + task.idValue() + "': "
                            + existing + " vs " + task
            );
        }
    }

    /**
     * Resolves an already-registered definition by id.
     *
     * @param id task id
     * @return task definition
     */
    protected final TaskDefinition<?, ?> definition(TaskId id) {
        TaskDefinition<?, ?> definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalStateException("Missing task definition for id: " + id.getValue());
        }
        return definition;
    }

    /**
     * Overlay graph that appends nodes/edges into a shared parent,
     * while keeping an independent tails set for the branch.
     */
    private static final class BranchOverlayGraph extends FlowGraph {

        private final FlowGraph parent;

        BranchOverlayGraph(FlowGraph parent, Set<TaskId> initialTails) {
            super(parent.definition(parent.start));
            this.parent = parent;
            this.setTails(new LinkedHashSet<>(initialTails));
        }

        @Override
        public Set<TaskId> nodes() {
            return parent.nodes();
        }

        @Override
        public Set<Edge> edges() {
            return parent.edges();
        }

        @Override
        public Map<TaskId, TypeMetadata> typeMetadata() {
            return parent.typeMetadata();
        }

        @Override
        public Optional<ExecutionPolicy> policy(TaskId taskId) {
            return parent.policy(taskId);
        }

        @Override
        public void applyPolicy(TaskId taskId, ExecutionPolicy policy) {
            parent.applyPolicy(taskId, policy);
        }

        @Override
        public void then(TaskDefinition<?, ?> task) {
            Objects.requireNonNull(task, "task");
            parent.registerDefinition(task);

            TaskId taskId = task.id();
            for (TaskId tail : super.tails) {
                parent.edges.add(new Edge(tail, taskId));
            }

            Set<TaskId> newTails = new LinkedHashSet<>();
            newTails.add(taskId);
            setTails(newTails);
        }

        @Override
        public void join(TaskDefinition<?, ?> task) {
            then(task);
        }

        @Override
        public void fork(List<Consumer<FlowBranch>> branches, FlowBuilder builder, TaskReferenceResolver referenceResolver) {
            Objects.requireNonNull(branches, "branches");
            Objects.requireNonNull(builder, "builder");
            Objects.requireNonNull(referenceResolver, "referenceResolver");
            if (branches.isEmpty()) {
                throw new IllegalArgumentException("fork requires at least 1 branch");
            }

            Set<TaskId> unionTails = new LinkedHashSet<>();
            for (Consumer<FlowBranch> consumer : branches) {
                FlowGraph nestedBranch = new BranchOverlayGraph(parent, super.tails);
                consumer.accept(new DefaultFlowBranch(nestedBranch, builder, referenceResolver));
                unionTails.addAll(nestedBranch.tails());
            }
            setTails(unionTails);
        }

        private void setTails(Set<TaskId> newTails) {
            super.tails = newTails;
        }
    }
}
