package com.fta.export;

import java.util.Collections;
import java.util.List;

public final class ExportBatch {

    private final List<Record> records;
    private final String nextToken;

    public ExportBatch(List<Record> records, String nextToken) {
        this.records = Collections.unmodifiableList(records);
        this.nextToken = nextToken;
    }

    public List<Record> records() {
        return records;
    }

    public String nextToken() {
        return nextToken;
    }

    public boolean hasMore() {
        return !nextToken.isEmpty();
    }

    public int size() {
        return records.size();
    }
}
