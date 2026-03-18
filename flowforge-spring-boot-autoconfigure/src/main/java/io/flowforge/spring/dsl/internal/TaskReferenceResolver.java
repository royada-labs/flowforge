package io.flowforge.spring.dsl.internal;

import io.flowforge.spring.dsl.TaskCallRef;
import io.flowforge.spring.dsl.TaskMethodRef;
import io.flowforge.task.TaskDefinition;

public interface TaskReferenceResolver {
    <I, O> TaskDefinition<I, O> resolve(TaskMethodRef<?, I, O> ref);
    <B, I, O> TaskDefinition<I, O> resolve(TaskCallRef<B, I, O> ref);
}
