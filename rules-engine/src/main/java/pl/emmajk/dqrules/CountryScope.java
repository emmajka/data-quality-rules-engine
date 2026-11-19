package pl.emmajk.dqrules;

import java.util.Objects;

/** Where a rule applies: to every country, or to one country code compared exactly. */
public sealed interface CountryScope {

    /** The scope of a rule that applies to records of every country. */
    CountryScope WORLD = new World();

    /**
     * Creates a scope limited to one country code.
     *
     * @throws NullPointerException if {@code country} is null
     * @throws IllegalArgumentException if {@code country} is blank
     */
    static CountryScope of(String country) {
        return new Country(country);
    }

    /**
     * Returns true if a rule with this scope applies to a record with the given country value.
     * The value is passed as is, so a missing or non-string country matches only {@link #WORLD}.
     */
    boolean appliesTo(Object recordCountry);

    /** The scope that matches every country. */
    record World() implements CountryScope {

        @Override
        public boolean appliesTo(Object recordCountry) {
            return true;
        }
    }

    /**
     * The scope that matches one country code.
     *
     * @param code the country code, never blank
     */
    record Country(String code) implements CountryScope {

        /**
         * Validates the code.
         *
         * @throws NullPointerException if {@code code} is null
         * @throws IllegalArgumentException if {@code code} is blank
         */
        public Country {
            Objects.requireNonNull(code, "country");
            if (code.isBlank()) {
                throw new IllegalArgumentException("country must not be blank");
            }
        }

        @Override
        public boolean appliesTo(Object recordCountry) {
            return code.equals(recordCountry);
        }
    }
}
