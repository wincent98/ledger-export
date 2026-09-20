package com.fta.export;

/** One ledger row. Ordered by (updatedAtNanos, id); that pair is unique. */
public final class Record {

    private final long id;
    private final long updatedAtNanos;
    private final String payload;

    public Record(long id, long updatedAtNanos, String payload) {
        if (id <= 0L) {
            throw new IllegalArgumentException("id must be positive");
        }
        this.id = id;
        this.updatedAtNanos = updatedAtNanos;
        this.payload = payload;
    }

    public long id() {
        return id;
    }

    public long updatedAtNanos() {
        return updatedAtNanos;
    }

    public String payload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Record{" + id + "@" + updatedAtNanos + "}";
    }
}
