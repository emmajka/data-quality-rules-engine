package pl.emmajk.dqrules;

/** The outcome of one rule for one record, the closed set the task defines. */
public enum Decision {
    VALID,
    INVALID,
    REVIEW,
    NOT_APPLICABLE
}
