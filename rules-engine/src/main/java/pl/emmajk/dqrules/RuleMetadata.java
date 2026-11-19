package pl.emmajk.dqrules;

import java.util.Objects;
import java.util.Set;

/**
 * Describes a rule without computing anything: its lifecycle state, severity,
 * country scope and categories.
 *
 * @param status the lifecycle state
 * @param severity how serious a finding of the rule is
 * @param scope the countries the rule applies to, {@link CountryScope#WORLD} for all
 * @param categories free-form tags, possibly empty
 */
public record RuleMetadata(RuleStatus status, Severity severity, CountryScope scope, Set<String> categories) {

    /**
     * Validates the components and copies the categories.
     *
     * @throws NullPointerException if any component is null, or the categories hold a null
     */
    public RuleMetadata {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(scope, "scope");
        categories = Set.copyOf(Objects.requireNonNull(categories, "categories"));
    }

    /** Creates metadata for a rule that applies to every country and has no categories. */
    public static RuleMetadata of(RuleStatus status, Severity severity) {
        return new RuleMetadata(status, severity, CountryScope.WORLD, Set.of());
    }
}
