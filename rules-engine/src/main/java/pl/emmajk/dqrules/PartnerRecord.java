package pl.emmajk.dqrules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One business partner: named fields that include a string {@code id}, created with {@link #of(Map)}.
 *
 * <p>A class, not a Java record, so the representation stays hidden and can change.
 * Only the top-level fields are copied. Nested values are shared with the source map,
 * so the host must not change them afterwards.
 */
public final class PartnerRecord {

    private static final String ID_FIELD = "id";

    private final String id;
    private final Map<String, Object> fields;

    private PartnerRecord(String id, Map<String, Object> fields) {
        this.id = id;
        this.fields = fields;
    }

    /**
     * Creates a record from the given fields, which must contain a non-blank string {@code id}.
     *
     * @throws NullPointerException if {@code fields} is null
     * @throws IllegalArgumentException if the {@code id} field is missing, null, blank or not a string
     */
    public static PartnerRecord of(Map<String, ?> fields) {
        Objects.requireNonNull(fields, "fields");
        if (!(fields.get(ID_FIELD) instanceof String idText) || idText.isBlank()) {
            throw new IllegalArgumentException("record must have a non-blank string '" + ID_FIELD + "' field");
        }

        return new PartnerRecord(
                idText,
                Collections.unmodifiableMap(new LinkedHashMap<String, Object>(fields)));
    }

    /** Returns the record id, the value of the {@code id} field. */
    public String id() {
        return id;
    }

    /**
     * Returns the field value, or null if the field is absent or null.
     * The two cases are not distinguished.
     *
     * @throws NullPointerException if {@code field} is null
     */
    public Object get(String field) {
        Objects.requireNonNull(field, "field");

        return fields.get(field);
    }

    /** Two records are equal when their fields are equal, which includes the id. */
    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof PartnerRecord that && fields.equals(that.fields));
    }

    /** Consistent with {@link #equals}. It changes if the host mutates a nested value. */
    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    /** Returns only the id, so logs do not leak other field values such as IBANs. */
    @Override
    public String toString() {
        return "PartnerRecord[id=" + id + "]";
    }
}
