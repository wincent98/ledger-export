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
        int first = 0;
        if (!cursor.isStart()) {
            first = firstIndexAfter(cursor);
        }
        List<Record> out = new ArrayList<>();
        for (int i = first; i < rows.size() && out.size() < limit; i++) {
            out.add(rows.get(i));
        }
        return out;
    }

    /** Index of the first row that comes strictly after {@code cursor} in scan order. */
    private int firstIndexAfter(Cursor cursor) {
        if (cursor.legacyMillisMarker()) {
            // Decoded v1 token: resume immediately after the row the token was issued on.
            // The id disambiguates the position the rounded millisecond boundary lost.
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).id() == cursor.id()) {
                    return i + 1;
                }
            }
            // Marker row no longer present: fall back to the strict keyset boundary at the
            // decoded millisecond, which never replays already delivered rows.
        }
        for (int i = 0; i < rows.size(); i++) {
            Record row = rows.get(i);
            if (row.updatedAtNanos() > cursor.updatedAtNanos()
                    || (row.updatedAtNanos() == cursor.updatedAtNanos()
                        && row.id() > cursor.id())) {
                return i;
            }
        }
        return rows.size();
    }
}
