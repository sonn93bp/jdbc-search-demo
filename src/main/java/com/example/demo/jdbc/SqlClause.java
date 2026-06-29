package com.example.demo.jdbc;

public sealed interface SqlClause permits SqlClause.Safe, SqlClause.Param {

    String sql();

    /**
     * A developer-authored SQL fragment (SELECT list, ORDER BY col, etc.)
     * NEVER pass user input here.
     */
    record Safe(String sql) implements SqlClause {
        public Safe {
            // Reject obviously dangerous patterns at construction time
            validate(sql);
        }

        private static void validate(String sql) {
            if (sql == null || sql.isBlank()) {
                throw new IllegalArgumentException("SQL clause must not be blank");
            }
            // Block stacked statements and comment injections
            String normalized = sql.stripLeading().toUpperCase();
            if (normalized.contains(";") || normalized.contains("--") || normalized.contains("/*")) {
                throw new IllegalArgumentException(
                        "Potentially unsafe SQL fragment rejected: " + sql);
            }
        }
    }

    /**
     * A user-supplied value — always rendered as a named parameter (:name),
     * never interpolated into the SQL string.
     */
    record Param(String paramName, Object value) implements SqlClause {
        @Override
        public String sql() { return ":" + paramName; }
    }

    // Factory methods
    static Safe  safe(String sql)                    { return new Safe(sql); }
    static Param param(String paramName, Object value) { return new Param(paramName, value); }
}
