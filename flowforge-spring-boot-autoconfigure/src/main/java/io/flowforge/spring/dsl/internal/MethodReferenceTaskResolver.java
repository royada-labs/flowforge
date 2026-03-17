package io.flowforge.spring.dsl.internal;

import io.flowforge.spring.dsl.TaskCallRef;
import io.flowforge.spring.dsl.TaskMethodRef;
import io.flowforge.spring.registry.TaskDefinitionRegistry;
import io.flowforge.task.TaskDefinition;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Resolves typed task method references at workflow build-time.
 */
public final class MethodReferenceTaskResolver implements TaskReferenceResolver {

    private final TaskDefinitionRegistry definitions;

    public MethodReferenceTaskResolver(TaskDefinitionRegistry definitions) {
        this.definitions = Objects.requireNonNull(definitions, "definitions");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I, O> TaskDefinition<I, O> resolve(TaskMethodRef<?, I, O> ref) {
        Objects.requireNonNull(ref, "ref");
        SerializedLambda lambda = extract(ref);
        return (TaskDefinition<I, O>) resolve(lambda);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B, I, O> TaskDefinition<I, O> resolve(TaskCallRef<B, I, O> ref) {
        Objects.requireNonNull(ref, "ref");
        SerializedLambda lambda = extract(ref);
        return (TaskDefinition<I, O>) resolve(lambda);
    }

    private TaskDefinition<?, ?> resolve(SerializedLambda lambda) {
        String methodName = lambda.getImplMethodName();
        String implClass = lambda.getImplClass();
        String methodDescriptor = lambda.getImplMethodSignature();

        return definitions.findByMethodRef(implClass, methodName, methodDescriptor)
                .orElseThrow(() -> new IllegalStateException(
                        "No @FlowTask metadata found for method reference '" + implClass + "::" + methodName
                                + "' with signature '" + methodDescriptor + "'"
                ));
    }

    private static SerializedLambda extract(Object fn) {
        try {
            Method writeReplace = fn.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            Object replacement = writeReplace.invoke(fn);
            if (replacement instanceof SerializedLambda serializedLambda) {
                return serializedLambda;
            }
            throw new IllegalStateException("Unable to extract SerializedLambda from method reference");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to resolve method reference metadata", e);
        }
    }
}
