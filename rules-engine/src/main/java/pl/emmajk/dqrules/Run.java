package pl.emmajk.dqrules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A prepared evaluation of a catalog's rules over a collection of records. Nothing runs until
 * {@link #execute()} is called, and every call runs again.
 *
 * <p>The collection is captured by reference, not copied. The run itself is immutable, so it is
 * thread-safe as long as nobody modifies the collection.
 */
public final class Run {

    private static final String COUNTRY_FIELD = "country";

    private final RuleCatalog catalog;
    private final Collection<PartnerRecord> records;

    Run(RuleCatalog catalog, Collection<PartnerRecord> records) {
        this.catalog = catalog;
        this.records = records;
    }

    /**
     * Evaluates the applicable rules on every record, record by record and in catalog order,
     * and returns the results. It reads the records as they are at the time of the call.
     * A rule that throws an {@code Exception} on a record becomes an error in the report and
     * the run goes on. An {@code Error}, such as {@code OutOfMemoryError}, is not caught.
     *
     * @throws NullPointerException if the collection holds a null record
     */
    public RunReport execute() {
        List<Rule> released = catalog.rules().stream()
                .filter(rule -> rule.metadata().status() == RuleStatus.RELEASED)
                .toList();
        List<Result> results = new ArrayList<>();
        List<RuleError> errors = new ArrayList<>();
        for (PartnerRecord partner : records) {
            Objects.requireNonNull(partner, "record");
            Object country = partner.get(COUNTRY_FIELD);
            for (Rule rule : released) {
                if (rule.metadata().scope().appliesTo(country)) {
                    try {
                        results.add(evaluate(rule, partner));
                    } catch (Exception failure) {
                        errors.add(RuleError.from(rule.id(), partner.id(), failure));
                    }
                }
            }
        }

        return new RunReport(results, errors);
    }

    private static Result evaluate(Rule rule, PartnerRecord partner) {
        String value = rule.logic().compute(partner);
        Decision decision = rule.mapping().decide(value);
        List<String> fields = rule.inputs();
        Provenance.Input[] inputs = new Provenance.Input[fields.size()];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = new Provenance.Input(fields.get(i), partner.get(fields.get(i)));
        }

        Provenance provenance = new Provenance(rule.id(), partner.id(), value, List.of(inputs));
        return new Result(provenance, decision, rule.metadata().severity());
    }
}
