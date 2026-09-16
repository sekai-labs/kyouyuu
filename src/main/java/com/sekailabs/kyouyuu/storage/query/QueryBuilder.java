package com.sekailabs.kyouyuu.storage.query;

import com.sekailabs.kyouyuu.storage.DatabaseType;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryBuilder {

    public static SelectQuery.SelectQueryBuilder select() {
        return SelectQuery.builder();
    }

    public static InsertQuery.InsertQueryBuilder insert() {
        return InsertQuery.builder();
    }

    public static UpdateQuery.UpdateQueryBuilder update() {
        return UpdateQuery.builder();
    }

    public static DeleteQuery.DeleteQueryBuilder delete() {
        return DeleteQuery.builder();
    }

    @Getter
    @Builder
    public static class SelectQuery {
        private final String table;
        @Singular
        private final List<String> columns;
        private final String where;
        private final String orderBy;
        private final Integer limit;

        public String build() {
            StringBuilder sb = new StringBuilder("SELECT ");
            if (columns == null || columns.isEmpty()) {
                sb.append("*");
            } else {
                sb.append(String.join(", ", columns));
            }
            sb.append(" FROM ").append(table);
            if (where != null && !where.isBlank()) {
                sb.append(" WHERE ").append(where);
            }
            if (orderBy != null && !orderBy.isBlank()) {
                sb.append(" ORDER BY ").append(orderBy);
            }
            if (limit != null && limit > 0) {
                sb.append(" LIMIT ").append(limit);
            }
            return sb.append(";").toString();
        }
    }

    @Getter
    @Builder
    public static class InsertQuery {
        private final String table;
        @Singular
        private final List<String> columns;
        @Singular
        private final List<String> values;
        @Singular
        private final List<String> conflictKeys;
        @Singular
        private final List<String> updateColumns;

        public String build(DatabaseType type) {
            StringBuilder sb = new StringBuilder();
            List<String> vals = (values == null || values.isEmpty()) ?
                    Collections.nCopies(columns.size(), "?") : values;

            if (conflictKeys != null && !conflictKeys.isEmpty() && updateColumns != null && !updateColumns.isEmpty()) {
                if (type == DatabaseType.POSTGRESQL) {
                    sb.append("INSERT INTO ").append(table).append(" (");
                    sb.append(String.join(", ", columns));
                    sb.append(") VALUES (");
                    sb.append(String.join(", ", vals));
                    sb.append(") ON CONFLICT (");
                    sb.append(String.join(", ", conflictKeys));
                    sb.append(") DO UPDATE SET ");
                    List<String> updates = new ArrayList<>();
                    for (String col : updateColumns) {
                        updates.add(col + " = EXCLUDED." + col);
                    }
                    sb.append(String.join(", ", updates));
                } else if (type == DatabaseType.MYSQL) {
                    sb.append("INSERT INTO ").append(table).append(" (");
                    sb.append(String.join(", ", columns));
                    sb.append(") VALUES (");
                    sb.append(String.join(", ", vals));
                    sb.append(") ON DUPLICATE KEY UPDATE ");
                    List<String> updates = new ArrayList<>();
                    for (String col : updateColumns) {
                        updates.add(col + " = VALUES(" + col + ")");
                    }
                    sb.append(String.join(", ", updates));
                } else {
                    sb.append("INSERT OR REPLACE INTO ").append(table).append(" (");
                    sb.append(String.join(", ", columns));
                    sb.append(") VALUES (");
                    sb.append(String.join(", ", vals));
                    sb.append(")");
                }
            } else {
                sb.append("INSERT INTO ").append(table).append(" (");
                sb.append(String.join(", ", columns));
                sb.append(") VALUES (");
                sb.append(String.join(", ", vals));
                sb.append(")");
            }
            return sb.append(";").toString();
        }
    }

    @Getter
    @Builder
    public static class UpdateQuery {
        private final String table;
        @Singular("set")
        private final List<String> sets;
        private final String where;

        public String build() {
            StringBuilder sb = new StringBuilder("UPDATE ").append(table).append(" SET ");
            sb.append(String.join(", ", sets));
            if (where != null && !where.isBlank()) {
                sb.append(" WHERE ").append(where);
            }
            return sb.append(";").toString();
        }
    }

    @Getter
    @Builder
    public static class DeleteQuery {
        private final String table;
        private final String where;

        public String build() {
            StringBuilder sb = new StringBuilder("DELETE FROM ").append(table);
            if (where != null && !where.isBlank()) {
                sb.append(" WHERE ").append(where);
            }
            return sb.append(";").toString();
        }
    }
}
