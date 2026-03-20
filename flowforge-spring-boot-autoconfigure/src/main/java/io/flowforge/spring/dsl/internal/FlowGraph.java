package io.flowforge.spring.dsl.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import io.flowforge.spring.dsl.DefaultFlowBranch;
import io.flowforge.spring.dsl.FlowBranch;
import io.flowforge.spring.dsl.FlowBuilder;
import io.flowforge.task.TaskDefinition;
import io.flowforge.task.TaskId;
import io.flowforge.validation.TypeMetadata;
import io.flowforge.workflow.policy.ExecutionPolicy;

public class FlowGraph {

    private final Map<TaskId, TaskDefinition<?, ?>> definitions = new LinkedHashMap<>();
    private final Set<Edge> edges = new LinkedHashSet<>();
    private final Map<TaskId, ExecutionPolicy> policies = new LinkedHashMap<>();

    private final TaskId start;
    protected Set<TaskId> tails;

    private FlowGraph(TaskDefinition<?, ?> startTask) {
        Objects.requireNonNull(startTask, "startTask");
        registerDefinition(startTask);
        this.start = startTask.id();
        this.tails = new LinkedHashSet<>();
        this.tails.add(start);
    }

    public static FlowGraph start(TaskDefinition<?, ?> startTask) {
        return new FlowGraph(startTask);
    }

    public TaskId start() {
        return start;
    }

    public Set<TaskId> nodes() {
        return Collections.unmodifiableSet(definitions.keySet());
    }

    public Set<Edge> edges() {
        return Collections.unmodifiableSet(edges);
    }

    public Set<TaskId> tails() {
        return Collections.unmodifiableSet(tails);
    }

    public Map<TaskId, TypeMetadata> typeMetadata() {
        Map<TaskId, TypeMetadata> metadata = new LinkedHashMap<>();
        for (TaskDefinition<?, ?> definition : definitions.values()) {
            metadata.put(definition.id(), new TypeMetadata(definition.inputType(), definition.outputType()));
        }
        return Collections.unmodifiableMap(metadata);
    }

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

    public Optional<ExecutionPolicy> policy(TaskId taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return Optional.ofNullable(policies.get(taskId));
    }

    public void applyPolicy(TaskId taskId, ExecutionPolicy policy) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(policy, "policy");
        if (!definitions.containsKey(taskId)) {
            throw new IllegalArgumentException("Unknown task id for policy assignment: " + taskId.getValue());
        }
        policies.merge(taskId, policy, ExecutionPolicy::andThen);
    }

    public void join(TaskDefinition<?, ?> task) {
        then(task);
    }

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

    protected final void registerDefinition(TaskDefinition<?, ?> task) {
        TaskDefinition<?, ?> existing = definitions.putIfAbsent(task.id(), task);
        if (existing != null && !existing.equals(task)) {
            throw new IllegalArgumentException(
                    "Conflicting task definitions for id '" + task.idValue() + "': "
                            + existing + " vs " + task
            );
        }
    }

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
