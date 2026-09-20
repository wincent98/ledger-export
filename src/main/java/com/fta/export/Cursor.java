package com.fta.export;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Resume point of a keyset scan. Serialised into an opaque token that downstream
 * consumers hold on to between batches.
 *
 * Token layout: base64("v1:<updatedAtMillis>:<id>").
 */
public final class Cursor {

    private static final String PREFIX = "v1";
    public static final Cursor START = new Cursor(Long.MIN_VALUE, 0L);

    private final long updatedAtNanos;
    private final long id;

    public Cursor(long updatedAtNanos, long id) {
        this.updatedAtNanos = updatedAtNanos;
        this.id = id;
    }

    public long updatedAtNanos() {
        return updatedAtNanos;
    }

    public long id() {
        return id;
    }

    public boolean isStart() {
        return updatedAtNanos == Long.MIN_VALUE && id == 0L;
    }

    public static String encode(Cursor cursor) {
        if (cursor == null || cursor.isStart()) {
            return "";
        }
        // Round the timestamp up to the enclosing millisecond so a partially elapsed
        // millisecond is not replayed on resume.
        long millis = (cursor.updatedAtNanos + 999_999L) / 1_000_000L;
        String raw = PREFIX + ":" + millis + ":" + cursor.id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String token) {
        if (token == null || token.isEmpty()) {
            return START;
        }
        String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        String[] parts = raw.split(":");
        if (parts.length != 3 || !PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("unrecognised cursor token: " + raw);
        }
        long millis = Long.parseLong(parts[1]);
        return new Cursor(millis * 1_000_000L, Long.parseLong(parts[2]));
    }

    public static Cursor of(Record record) {
        return new Cursor(record.updatedAtNanos(), record.id());
    }

    @Override
    public String toString() {
        return isStart() ? "START" : "(" + updatedAtNanos + "," + id + ")";
    }
}
