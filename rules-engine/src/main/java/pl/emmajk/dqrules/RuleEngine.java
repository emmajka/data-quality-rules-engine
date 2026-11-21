package pl.emmajk.dqrules;

import java.util.Collection;
import java.util.Objects;

/**
 * Evaluates a catalog's rules over records. It holds only the immutable catalog, so one instance
 * can serve a whole application.
 */
public final class RuleEngine {

    private final RuleCatalog catalog;

    private RuleEngine(RuleCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Creates an engine for the catalog.
     *
     * @throws NullPointerException if {@code catalog} is null
     */
    public static RuleEngine of(RuleCatalog catalog) {
        return new RuleEngine(Objects.requireNonNull(catalog, "catalog"));
    }

    /**
     * Prepares a run over the records and executes nothing yet. The collection is captured by reference,
     * so it must not be modified while the run is executing, and changes made before {@link Run#execute()}
     * are seen. Share the run between threads only with a collection that is not modified.
     *
     * @throws NullPointerException if {@code records} is null
     */
    public Run on(Collection<PartnerRecord> records) {
        return new Run(catalog, Objects.requireNonNull(records, "records"));
    }
}
