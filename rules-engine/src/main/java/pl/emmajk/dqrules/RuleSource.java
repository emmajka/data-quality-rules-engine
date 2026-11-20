package pl.emmajk.dqrules;

import java.util.List;

/** Where a host's rules come from, such as rules built in code or loaded by the host's own loader. A catalog builder loads from it. */
@FunctionalInterface
public interface RuleSource {

    /**
     * Returns the rules to register, in registration order, never null. The builder reads the list once.
     * A failure must be an unchecked exception, which reaches the caller.
     */
    List<Rule> rules();
}
