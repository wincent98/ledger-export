package com.fta.export;

import java.util.List;

/** Drives the keyset scan one batch at a time. */
public class Exporter {

    private final RecordTable table;
    private final int batchSize;

    public Exporter(RecordTable table, int batchSize) {
        this.table = table;
        this.batchSize = batchSize;
    }

    public int batchSize() {
        return batchSize;
    }

    /** Reads the batch that follows {@code token} and returns the token for the next call. */
    public ExportBatch next(String token) {
        Cursor cursor = Cursor.decode(token);
        List<Record> page = table.page(cursor, batchSize);
        if (page.isEmpty()) {
            return new ExportBatch(page, "");
        }
        Record last = page.get(page.size() - 1);
        String nextToken = page.size() < batchSize ? "" : Cursor.encode(Cursor.of(last));
        return new ExportBatch(page, nextToken);
    }
}
