package com.fta.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordTest {

    @Test
    void exposesItsFields() {
        Record r = new Record(7L, 1_234_567L, "p");
        assertEquals(7L, r.id());
        assertEquals(1_234_567L, r.updatedAtNanos());
        assertEquals("p", r.payload());
    }

    @Test
    void rejectsANonPositiveId() {
        assertThrows(IllegalArgumentException.class, () -> new Record(0L, 1L, "p"));
        assertThrows(IllegalArgumentException.class, () -> new Record(-1L, 1L, "p"));
    }

    @Test
    void acceptsAZeroTimestamp() {
        assertEquals(0L, new Record(1L, 0L, "p").updatedAtNanos());
    }

    @Test
    void acceptsANullPayload() {
        assertEquals(null, new Record(1L, 1L, null).payload());
    }

    @Test
    void toStringShowsIdAndTimestamp() {
        assertTrue(new Record(7L, 99L, "p").toString().contains("7@99"));
    }
}
