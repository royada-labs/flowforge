package org.royada.flowforge.validation;


import java.util.List;
import java.util.Objects;

/**
 * The result of validating a workflow definition.
 *
 * <p>Contains a list of {@link FlowValidationError} instances. Use
 * {@link #isValid()} to check whether the workflow can be executed
 * (i.e., no ERROR-level issues).
 */
public final class FlowValidationResult {

    private static final FlowValidationResult VALID = new FlowValidationResult(List.of());

    private final List<FlowValidationError> errors;

    private FlowValidationResult(List<FlowValidationError> errors) {
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
    }

    /**
     * Creates a result from the given list of errors.
     *
     * @param errors the validation errors; must not be null
     * @return a new result
     */
    public static FlowValidationResult of(List<FlowValidationError> errors) {
        Objects.requireNonNull(errors, "errors");
        if (errors.isEmpty()) {
            return VALID;
        }
        return new FlowValidationResult(errors);
    }

    /** Returns a valid (empty) result. */
    public static FlowValidationResult valid() {
        return VALID;
    }

    /**
     * Returns {@code true} if there are no ERROR-level issues.
     * Warnings do not block execution.
     *
     * @return {@code true} if the workflow can be executed
     */
    public boolean isValid() {
        return errors.stream()
                .noneMatch(e -> e.severity() == FlowValidationError.Severity.ERROR);
    }

    /**
     * Returns all validation findings (errors and warnings).
     *
     * @return an unmodifiable list of all errors
     */
    public List<FlowValidationError> errors() {
        return errors;
    }

    /**
     * Returns only ERROR-level issues.
     *
     * @return an unmodifiable list of errors
     */
    public List<FlowValidationError> errorsOnly() {
        return errors.stream()
                .filter(e -> e.severity() == FlowValidationError.Severity.ERROR)
                .toList();
    }

    /**
     * Returns only WARNING-level issues.
     *
     * @return an unmodifiable list of warnings
     */
    public List<FlowValidationError> warningsOnly() {
        return errors.stream()
                .filter(e -> e.severity() == FlowValidationError.Severity.WARNING)
                .toList();
    }

    /**
     * Returns a human-readable, compiler-style formatted summary.
     *
     * @return the formatted validation report
     */
    public String formatted() {
        if (errors.isEmpty()) {
            return "Validation passed: no errors.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("FlowForge workflow validation found ")
                .append(errorsOnly().size()).append(" error(s), ")
                .append(warningsOnly().size()).append(" warning(s):\n");

        for (int i = 0; i < errors.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(errors.get(i).formatted()).append('\n');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return formatted();
    }
}
