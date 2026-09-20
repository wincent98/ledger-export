package com.fta.export;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Resume point of a keyset scan. Serialised into an opaque token that downstream
 * consumers hold on to between batches.
 *
 * Token layouts:
 *   v2 (current): base64("v2:<updatedAtNanos>:<id>"), nanosecond precision, no rounding.
 *   v1 (legacy):  base64("v1:<updatedAtMillis>:<id>"), issued by 1.6.0 and earlier.
 *
 * A decoded v1 token keeps {@link #legacyMillisMarker()} set so the scan can resume
 * precisely at the row the token was issued for even though its timestamp was rounded.
 */
public final class Cursor {

    private static final String PREFIX_V2 = "v2";
    private static final String PREFIX_V1 = "v1";
    public static final Cursor START = new Cursor(Long.MIN_VALUE, 0L, false);

    private final long updatedAtNanos;
    private final long id;
    private final boolean legacyMillisMarker;

    public Cursor(long updatedAtNanos, long id) {
        this(updatedAtNanos, id, false);
    }

    private Cursor(long updatedAtNanos, long id, boolean legacyMillisMarker) {
        this.updatedAtNanos = updatedAtNanos;
        this.id = id;
        this.legacyMillisMarker = legacyMillisMarker;
    }

    public long updatedAtNanos() {
        return updatedAtNanos;
    }

    public long id() {
        return id;
    }

    /**
     * True when this cursor was decoded from a legacy v1 token: {@link #updatedAtNanos()}
     * is a millisecond boundary and {@link #id()} names the actual last-seen row instead
     * of an arbitrary position at that boundary.
     */
    boolean legacyMillisMarker() {
        return legacyMillisMarker;
    }

    public boolean isStart() {
        return updatedAtNanos == Long.MIN_VALUE && id == 0L;
    }

    public static String encode(Cursor cursor) {
        if (cursor == null || cursor.isStart()) {
            return "";
        }
        // Keep the full nanosecond timestamp: rounding it (the old v1 behaviour) jumped
        // over every row sharing the enclosing millisecond and silently dropped them.
        String raw = PREFIX_V2 + ":" + cursor.updatedAtNanos + ":" + cursor.id;
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
        if (PREFIX_V2.equals(parts[0])) {
            return new Cursor(Long.parseLong(parts[1]), Long.parseLong(parts[2]), false);
        }
        if (PREFIX_V1.equals(parts[0])) {
            // Legacy token: the timestamp was rounded up to a millisecond boundary, so the
            // pair (millis, id) does not name a real scan position. Flag it as a legacy
            // marker so RecordTable can resume right after the row carrying this id.
            long millis = Long.parseLong(parts[1]);
            return new Cursor(millis * 1_000_000L, Long.parseLong(parts[2]), true);
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
