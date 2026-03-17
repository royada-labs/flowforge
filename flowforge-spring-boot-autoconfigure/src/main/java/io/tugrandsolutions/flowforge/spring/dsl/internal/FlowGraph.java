package io.tugrandsolutions.flowforge.spring.dsl.internal;

import io.tugrandsolutions.flowforge.spring.dsl.DefaultFlowBranch;
import io.tugrandsolutions.flowforge.spring.dsl.FlowBranch;
import io.tugrandsolutions.flowforge.validation.TypeMetadata;

import java.util.*;
import java.util.function.Consumer;

public class FlowGraph {

    private final Set<String> nodes = new LinkedHashSet<>();
    private final Set<Edge> edges = new LinkedHashSet<>();
    private final Map<String, TypeMetadata> typeMetadata = new LinkedHashMap<>();

    private final String start;
    private Set<String> tails;

    private FlowGraph(String start) {
        this.start = Objects.requireNonNull(start, "start");
        this.nodes.add(start);
        this.tails = new LinkedHashSet<>();
        this.tails.add(start);
    }

    public static FlowGraph start(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId");
        }
        return new FlowGraph(taskId);
    }

    public String start() {
        return start;
    }

    public Set<String> nodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public Set<Edge> edges() {
        return Collections.unmodifiableSet(edges);
    }

    public Set<String> tails() {
        return Collections.unmodifiableSet(tails);
    }

    /**
     * Registers type metadata for the given task id.
     * Called by the typed DSL when {@code TaskDefinition} is used.
     *
     * @param taskId     the task id
     * @param inputType  the declared input type
     * @param outputType the declared output type
     */
    public void registerTypeMetadata(String taskId, Class<?> inputType, Class<?> outputType) {
        typeMetadata.put(taskId, new TypeMetadata(inputType, outputType));
    }

    /**
     * Returns collected type metadata (unmodifiable).
     *
     * @return map from task id to type metadata
     */
    public Map<String, TypeMetadata> typeMetadata() {
        return Collections.unmodifiableMap(typeMetadata);
    }

    public void then(String taskId) {
        requireTaskId(taskId);

        nodes.add(taskId);

        // connect all current tails -> taskId
        for (String t : tails) {
            edges.add(new Edge(t, taskId));
        }

        // new tails = {taskId}
        tails = new LinkedHashSet<>();
        tails.add(taskId);
    }

    public void join(String taskId) {
        // join is semantically identical to then, but kept for clarity
        then(taskId);
    }

    public void fork(List<Consumer<FlowBranch>> branches) {
        Objects.requireNonNull(branches, "branches");
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("fork requires at least 1 branch");
        }

        // Each branch starts from current tails (shared base)
        // We create a branch graph that shares nodes/edges sets by reference? No.
        // We create independent branch graphs that append into THIS graph, but start with current tails.
        // Implementation: for each branch, we clone a "view" that writes into THIS graph but has its own tails.
        Set<String> unionTails = new LinkedHashSet<>();

        for (Consumer<FlowBranch> consumer : branches) {
            Objects.requireNonNull(consumer, "branch consumer");

            FlowGraph branch = new BranchOverlayGraph(this, this.tails);
            consumer.accept(new DefaultFlowBranch(branch));

            unionTails.addAll(branch.tails());
        }

        // after fork, tails become union of branch tails
        this.tails = unionTails;
    }

    private static void requireTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId");
        }
    }

    /**
     * Overlay graph that appends nodes/edges into a shared parent,
     * while keeping an independent tails set for the branch.
     */
    private static final class BranchOverlayGraph extends FlowGraph {

        private final FlowGraph parent;

        BranchOverlayGraph(FlowGraph parent, Set<String> initialTails) {
            super(parent.start); // not used directly; we override accessors
            this.parent = parent;
            this.setTails(new LinkedHashSet<>(initialTails));
        }

        @Override
        public Set<String> nodes() {
            return parent.nodes();
        }

        @Override
        public Set<Edge> edges() {
            return parent.edges();
        }

        @Override
        public void registerTypeMetadata(String taskId, Class<?> inputType, Class<?> outputType) {
            parent.registerTypeMetadata(taskId, inputType, outputType);
        }

        @Override
        public Map<String, TypeMetadata> typeMetadata() {
            return parent.typeMetadata();
        }

        @Override
        public void then(String taskId) {
            requireTaskId(taskId);

            parent.nodes.add(taskId);

            for (String t : super.tails) {
                parent.edges.add(new Edge(t, taskId));
            }

            Set<String> newTails = new LinkedHashSet<>();
            newTails.add(taskId);
            setTails(newTails);
        }

        @Override
        public void join(String taskId) {
            then(taskId);
        }

        @Override
        public void fork(List<Consumer<FlowBranch>> branches) {
            Objects.requireNonNull(branches, "branches");
            if (branches.isEmpty()) {
                throw new IllegalArgumentException("fork requires at least 1 branch");
            }

            Set<String> unionTails = new LinkedHashSet<>();
            for (Consumer<FlowBranch> c : branches) {
                FlowGraph nestedBranch = new BranchOverlayGraph(parent, super.tails);
                c.accept(new DefaultFlowBranch(nestedBranch));
                unionTails.addAll(nestedBranch.tails());
            }
            setTails(unionTails);
        }

        private void setTails(Set<String> newTails) {
            super.tails = newTails;
        }
    }
}