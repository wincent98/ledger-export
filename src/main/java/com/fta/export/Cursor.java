package com.fta.export;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Resume point of a keyset scan. Serialised into an opaque token that downstream
 * consumers hold on to between batches.
 *
 * Token layout: base64("v2:<updatedAtNanos>:<id>"). Legacy v1 tokens carry the
 * timestamp in whole milliseconds and are still accepted on decode.
 */
public final class Cursor {

    private static final String PREFIX = "v2";
    private static final String LEGACY_PREFIX = "v1";
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
        // Full nanosecond precision: rounding to milliseconds would skip rows that
        // share the enclosing millisecond but sit after the cursor position.
        String raw = PREFIX + ":" + cursor.updatedAtNanos + ":" + cursor.id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String token) {
        if (token == null || token.isEmpty()) {
            return START;
        }
        String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        String[] parts = raw.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("unrecognised cursor token: " + raw);
        }
        if (PREFIX.equals(parts[0])) {
            return new Cursor(Long.parseLong(parts[1]), Long.parseLong(parts[2]));
        }
        if (LEGACY_PREFIX.equals(parts[0])) {
            // v1 stored whole milliseconds; resume at the start of that millisecond.
            long millis = Long.parseLong(parts[1]);
            return new Cursor(millis * 1_000_000L, Long.parseLong(parts[2]));
        }
        throw new IllegalArgumentException("unrecognised cursor token: " + raw);
    }

    public static Cursor of(Record record) {
        return new Cursor(record.updatedAtNanos(), record.id());
    }

    @Override
    public String toString() {
        return isStart() ? "START" : "(" + updatedAtNanos + "," + id + ")";
    }
}
