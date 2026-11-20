package pl.emmajk.dqrules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The rules a run is checked against, in registration order. It holds rules of every status,
 * because choosing which ones run is a separate step. It is immutable, so it can be shared
 * across runs and threads. Build one with {@link #builder()}.
 */
public final class RuleCatalog {

    private final Map<String, Rule> byId;
    private final List<Rule> rules;

    private RuleCatalog(Map<String, Rule> byId) {
        this.byId = Collections.unmodifiableMap(byId);
        this.rules = List.copyOf(byId.values());
    }

    /** Returns an empty builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Returns a builder that starts with the rules of this catalog, to make a changed copy. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.registered.putAll(byId);

        return builder;
    }

    /** Returns the rules in registration order. */
    public List<Rule> rules() {
        return rules;
    }

    /**
     * Returns the rule with the given id, compared exactly.
     *
     * @throws NullPointerException if {@code id} is null
     */
    public Optional<Rule> find(String id) {
        return Optional.ofNullable(byId.get(Objects.requireNonNull(id, "id")));
    }

    /**
     * Returns true if a rule with the given id is registered, compared exactly.
     *
     * @throws NullPointerException if {@code id} is null
     */
    public boolean contains(String id) {
        return byId.containsKey(Objects.requireNonNull(id, "id"));
    }

    /**
     * Collects rules into a catalog. Registering never replaces a rule silently:
     * an id that is already registered is an error. Not thread-safe, use it from one thread
     * and share the catalog it builds.
     */
    public static final class Builder {

        private final Map<String, Rule> registered = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Registers the rule.
         *
         * @throws NullPointerException if {@code rule} is null
         * @throws IllegalArgumentException if a rule with the same id is already registered
         */
        public Builder register(Rule rule) {
            Objects.requireNonNull(rule, "rule");
            if (registered.putIfAbsent(rule.id(), rule) != null) {
                throw new IllegalArgumentException("rule id '" + rule.id() + "' is already registered");
            }

            return this;
        }

        /**
         * Registers all the rules or none, in iteration order. The rules must be finite and are read once.
         * Every id that is already registered, or repeated in the rules, is reported in one exception.
         *
         * @throws NullPointerException if {@code rules} or one of its rules is null
         * @throws IllegalArgumentException if any id is already registered or repeated
         */
        public Builder registerAll(Iterable<Rule> rules) {
            Objects.requireNonNull(rules, "rules");
            Map<String, Rule> batch = new LinkedHashMap<>();
            Set<String> duplicates = new LinkedHashSet<>();
            for (Rule rule : rules) {
                Objects.requireNonNull(rule, "rule");
                if (registered.containsKey(rule.id()) || batch.putIfAbsent(rule.id(), rule) != null) {
                    duplicates.add(rule.id());
                }
            }
            if (!duplicates.isEmpty()) {
                throw new IllegalArgumentException("rule ids are already registered or repeated: "
                        + duplicates.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", ")));
            }
            registered.putAll(batch);

            return this;
        }

        /**
         * Registers all the rules of the source, with the same all-or-nothing rule as {@link #registerAll}.
         * A failure of the source propagates and registers nothing.
         *
         * @throws NullPointerException if {@code source} is null or returns null
         */
        public Builder registerFrom(RuleSource source) {
            List<Rule> sourceRules = Objects.requireNonNull(source, "source").rules();

            return registerAll(Objects.requireNonNull(sourceRules, "source returned null rules"));
        }

        /** Returns an immutable catalog of the rules registered so far. The builder stays usable. */
        public RuleCatalog build() {
            return new RuleCatalog(new LinkedHashMap<>(registered));
        }
    }
}
