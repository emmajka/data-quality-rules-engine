package pl.emmajk.dqrules;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A rule that failed on a record. It describes the failure without keeping the exception, so a
 * report stays small and holds no foreign object graphs.
 *
 * @param ruleId the id of the failed rule, never blank
 * @param recordId the id of the record it failed on, never blank
 * @param errorType the class name of the root cause, never blank
 * @param message the message of the root cause, cut to a limit, empty if it has none
 * @param origin the top stack frames of the root cause, empty if it has none
 */
public record RuleError(String ruleId, String recordId, String errorType, String message, String origin) {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_FRAMES = 3;

    /**
     * Validates the components.
     *
     * @throws NullPointerException if any component is null
     * @throws IllegalArgumentException if the rule id, record id or error type is blank
     */
    public RuleError {
        requireNotBlank(ruleId, "ruleId");
        requireNotBlank(recordId, "recordId");
        requireNotBlank(errorType, "errorType");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(origin, "origin");
    }

    /** Describes a failure by its root cause, so the engine never has to keep the exception. */
    static RuleError from(String ruleId, String recordId, Exception failure) {
        Throwable root = rootCause(failure);

        return new RuleError(ruleId, recordId, root.getClass().getName(), message(root), origin(root));
    }

    /** Follows the causes to the last one, and stops at a cycle. */
    private static Throwable rootCause(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable root = failure;
        seen.add(root);
        while (root.getCause() != null && seen.add(root.getCause())) {
            root = root.getCause();
        }

        return root;
    }

    private static String message(Throwable failure) {
        String message = failure.getMessage();
        if (message == null) {
            return "";
        }
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        // do not cut a surrogate pair in half
        int end = Character.isHighSurrogate(message.charAt(MAX_MESSAGE_LENGTH - 1))
                ? MAX_MESSAGE_LENGTH - 1 : MAX_MESSAGE_LENGTH;

        return message.substring(0, end);
    }

    /** Takes only the top frames, which show where it failed without keeping the whole trace. */
    private static String origin(Throwable failure) {
        return Arrays.stream(failure.getStackTrace())
                .limit(MAX_FRAMES)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining(" <- "));
    }

    private static void requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    /** Returns the ids and the type only, because a message can contain field values. */
    @Override
    public String toString() {
        return "RuleError[ruleId=" + ruleId + ", recordId=" + recordId + ", errorType=" + errorType + "]";
    }
}
