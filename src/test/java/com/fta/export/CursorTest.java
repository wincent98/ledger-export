package com.fta.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorTest {

    /** A timestamp that sits exactly on a millisecond boundary. */
    private static final long ALIGNED = 1_700_000_000_000L * 1_000_000L;

    @Test
    void startIsRecognisable() {
        assertTrue(Cursor.START.isStart());
        assertFalse(new Cursor(ALIGNED, 1L).isStart());
    }

    @Test
    void startEncodesToAnEmptyToken() {
        assertEquals("", Cursor.encode(Cursor.START));
        assertEquals("", Cursor.encode(null));
    }

    @Test
    void emptyTokenDecodesBackToStart() {
        assertTrue(Cursor.decode("").isStart());
        assertTrue(Cursor.decode(null).isStart());
    }

    @Test
    void roundTripsAnAlignedPosition() {
        Cursor decoded = Cursor.decode(Cursor.encode(new Cursor(ALIGNED, 42L)));
        assertEquals(ALIGNED, decoded.updatedAtNanos());
        assertEquals(42L, decoded.id());
    }

    @Test
    void roundTripsAnUnalignedPositionWithoutLosingSubMillisecondRows() {
        // Regression: encode used to round up to the enclosing millisecond, so rows
        // later in the same millisecond were skipped on resume.
        long unaligned = ALIGNED + 123_456L;
        Cursor decoded = Cursor.decode(Cursor.encode(new Cursor(unaligned, 7L)));
        assertEquals(unaligned, decoded.updatedAtNanos());
        assertEquals(7L, decoded.id());
    }

    @Test
    void buildsACursorFromARecord() {
        Cursor c = Cursor.of(new Record(9L, ALIGNED, "p"));
        assertEquals(ALIGNED, c.updatedAtNanos());
        assertEquals(9L, c.id());
    }

    @Test
    void anAlreadyIssuedV1TokenStillResolvesToTheSamePosition() {
        // This exact token is held by downstream consumers and must keep working.
        String issued = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("v1:1700000000000:42".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Cursor decoded = Cursor.decode(issued);
        assertEquals(1_700_000_000_000L * 1_000_000L, decoded.updatedAtNanos());
        assertEquals(42L, decoded.id());
    }

    @Test
    void rejectsAGarbageToken() {
        String junk = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("nope".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> Cursor.decode(junk));
    }
}
