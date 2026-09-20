package com.fta.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** In memory stand in for the ledger table, ordered by (updatedAtNanos, id). */
public class RecordTable {

    private static final Comparator<Record> ORDER =
            Comparator.comparingLong(Record::updatedAtNanos).thenComparingLong(Record::id);

    private final List<Record> rows = new ArrayList<>();

    public void insert(Record record) {
        rows.add(record);
        rows.sort(ORDER);
    }

    public int size() {
        return rows.size();
    }

    public List<Record> all() {
        return Collections.unmodifiableList(new ArrayList<>(rows));
    }

    /**
     * Returns at most {@code limit} rows positioned strictly after {@code cursor}
     * in (updatedAtNanos, id) order.
     */
    public List<Record> page(Cursor cursor, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        List<Record> out = new ArrayList<>();
        for (Record row : rows) {
            if (!cursor.isStart() && row.updatedAtNanos() < cursor.updatedAtNanos()) {
                continue;
            }
            out.add(row);
            if (out.size() == limit) {
                break;
            }
        }
        return out;
    }
}
