🚀 FlowForge

Forge reactive workflows with precision.

Build type-safe, reactive workflows in Java — without boilerplate, without runtime surprises.

⚡ The Problem

Most workflow engines force you into:

❌ Map<String, Object> everywhere

❌ Runtime casting (ClassCastException)

❌ Hidden coupling between steps

❌ Reflection-heavy execution

❌ Debugging nightmares

Even “modern” solutions still leak complexity.

✅ The FlowForge Approach

FlowForge flips the model:

Workflows are just type-safe function composition.

dsl.flow(CustomerTasks::getUser)
   .then(CustomerTasks::getOrders)
   .then(CustomerTasks::calculateDiscount);

That’s it.

🔥 Why This Is Different
1. 🛡️ Compile-Time Type Safety
Mono<User> getUser(Void in)
Mono<OrderSummary> getOrders(User in)

If types don’t match → your code doesn’t compile.

No runtime surprises. Ever.

2. 🔗 Automatic Data Propagation

Output of one task becomes input of the next.

No mapping. No glue code. No context passing.

.then(CustomerTasks::getOrders) // receives User automatically
3. ⚡ Reactive by Design

Built on Project Reactor:

Non-blocking

Backpressure-aware

High concurrency

4. 💥 Fail-Fast at Startup

Duplicate tasks → ❌ startup fails

Ambiguous mappings → ❌ startup fails

Invalid DAG → ❌ startup fails

If your app starts, your workflow is valid.

5. 🧠 Zero Runtime Reflection

Resolution happens once at startup

Execution uses precompiled plans

👉 Predictable, fast, debuggable.

🧩 Define Tasks (Annotation-First)
@TaskHandler
class CustomerTasks {

  @FlowTask(id = "getUser")
  Mono<User> getUser(Void in) {
      return Mono.just(...);
  }

  @FlowTask(id = "getOrders")
  Mono<OrderSummary> getOrders(User user) {
      return Mono.just(...);
  }

  @FlowTask(id = "discount")
  Mono<Discount> calculateDiscount(OrderSummary summary) {
      return Mono.just(...);
  }
}

No interfaces. No boilerplate. Just methods.

🏗️ Define a Workflow
@Bean
@FlowWorkflow(id = "customer-flow")
WorkflowExecutionPlan customerFlow(FlowDsl dsl) {
    return dsl.flow(CustomerTasks::getUser)
              .then(CustomerTasks::getOrders)
              .then(CustomerTasks::calculateDiscount)
              .build();
}
▶️ Execute
client.executeResult("customer-flow", null);

Or get full execution context:

client.execute("customer-flow", null);
🧠 How It Works (Simplified)

@TaskHandler classes are scanned at startup

Each @FlowTask is registered using full JVM signature

DSL builds a typed DAG (execution plan)

Runtime executes without reflection

🧬 Advanced Composition
Parallel
dsl.flow(A::task)
   .parallel(B::task, C::task)
Fork / Join
dsl.flow(A::task)
   .fork(B::task, C::task)
   .join(D::task)
🧰 Optional Context (Only When Needed)
Mono<Discount> discount(OrderSummary in, ReactiveExecutionContext ctx)

Use context only when necessary.
Keep tasks pure by default.

📦 Installation
Gradle
implementation("io.flowforge:flowforge-spring-boot-starter:1.1.0")
Maven
<dependency>
  <groupId>io.flowforge</groupId>
  <artifactId>flowforge-spring-boot-starter</artifactId>
  <version>1.1.0</version>
</dependency>
🧭 Philosophy

FlowForge is built on a few strict principles:

Types over strings

Compile-time over runtime

Explicit over magic

Simplicity over flexibility

🆚 When to Use FlowForge

Use it when:

You orchestrate multiple async steps

You care about correctness

You want predictable behavior

You are already using Reactor / WebFlux

🚫 When NOT to Use It

Long-running workflows (days/weeks)

BPMN / business-user-driven flows

Human-in-the-loop processes

📚 Documentation

Getting Started

Core Concepts

DSL Reference

Execution Model

Advanced Usage

💡 Final Thought

Most workflow engines try to hide complexity.

FlowForge removes it.

⭐ Contribute

PRs, ideas, and feedback are welcome.

📄 License

Apache 2.0