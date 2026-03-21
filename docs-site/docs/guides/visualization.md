# Architecture Visualization: JSON & Mermaid

FlowForge provides built-in tools to inspect and export your workflow design. This is essential for auditing, documentation, and building custom management dashboards.

---

## 🎨 The `FlowVisualizer`

The `FlowVisualizer` is a utility that transforms a `WorkflowExecutionPlan` into a `FlowVisualization` object. This object contains a structured representation of the Directed Acyclic Graph (DAG).

### Basic Usage

```java
@Autowired
private FlowDsl dsl;

public void exportMyWorkflow() {
    // 1. Construct your plan
    WorkflowExecutionPlan plan = dsl.flow(OrderTasks::fetchOrder)
            .then(OrderTasks::validateOrder)
            .build();

    // 2. Generate the visualization (with an empty validation result)
    FlowVisualization viz = FlowVisualizer.visualize(plan, FlowValidationResult.of(java.util.List.of()));

    // 3. Choose your format
    String json = viz.toJson();
    String mermaid = viz.toMermaid();
}
```

---

## 🏛️ Visualizing Registered Workflows

In a real Spring Boot application, most of your workflows are already defined as `@FlowWorkflow` beans. You don't need to recreate the plan to visualize it; you can retrieve it directly from the **`WorkflowPlanRegistry`**.

### Retrieving by ID

```java
@Autowired
private WorkflowPlanRegistry registry;

public String getWorkflowGraph(String workflowId) {
    // 🔍 Find the already registered execution plan in the registry
    return registry.find(workflowId)
            .map(plan -> FlowVisualizer.visualize(plan, FlowValidationResult.of(java.util.List.of())).toMermaid())
            .orElse("Workflow not found!");
}
```

---

## 📥 Export Formats

### 1. JSON Export
The JSON output is ideal for external tools or custom UIs. It includes all nodes, their input/output types, and the connections between them.

**Schema Example:**
```json
{
  "nodes": [
    { "taskId": "fetchOrder", "inputType": "OrderRequest", "outputType": "Order" },
    { "taskId": "validateOrder", "inputType": "Order", "outputType": "ValidationResult" }
  ],
  "edges": [
    { "from": "fetchOrder", "to": "validateOrder" }
  ]
}
```

### 2. Mermaid Export
Mermaid is a simple markdown-based language for generating charts. You can take the output of `viz.toMermaid()` and paste it into any Markdown viewer (like GitHub or Docusaurus) to render a live graph.

**Output Example:**
```mermaid
graph TD
    fetchOrder[fetchOrder\n(OrderRequest → Order)]
    validateOrder[validateOrder\n(Order → ValidationResult)]
    
    fetchOrder --> validateOrder
```

---

## 🔍 Visualizing Errors

If a workflow fails validation during startup, FlowForge captures the errors and highlights them in the visualization. 

In the Mermaid export, nodes with errors are automatically styled with **red borders**, making it easy to spot type mismatches or circular dependencies in complex designs.
