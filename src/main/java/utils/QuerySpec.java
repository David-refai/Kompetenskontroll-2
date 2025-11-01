package utils;

/**
 * <h2>Typed Query Strategy Pattern</h2>
 * Typed query built from UI selectors: field + operator + query + sort.
 * Used by the service to derive predicates/comparators.
 */
public final class QuerySpec {
    /**
     * Selected field (default NAME).
     */
    public Field field = Field.NAME;
    /**
     * Selected text op (default CONTAINS).
     */
    public TextOp textOp = TextOp.CONTAINS;
    /**
     * Selected numeric op (default EQ).
     */
    public NumOp numOp = NumOp.EQ;
    /**
     * Raw query string (trimmed).
     */
    public String query = "";
    /**
     * Sort mode (default NAME_ASC).
     */
    public Sort sort = Sort.NAME_ASC;

    /**
     * Field to filter on.
     */
    public enum Field {NAME, INDUSTRY, AGE, YEARS}

    /**
     * Numeric operator for AGE/YEARS.
     */
    public enum NumOp {EQ, GTE, LTE}

    /**
     * Text operator for NAME/INDUSTRY.
     */
    public enum TextOp {CONTAINS, STARTS_WITH, EQUALS}

    /**
     * Sort options.
     */
    public enum Sort {NAME_ASC, NAME_DESC, DATEOFREGISTER}
}

