package com.fta.export;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportBatchTest {

    private static List<Record> rows() {
        return new ArrayList<>(Arrays.asList(new Record(1L, 10L, "a"), new Record(2L, 20L, "b")));
    }

    @Test
    void exposesRecordsAndToken() {
        ExportBatch b = new ExportBatch(rows(), "tok");
        assertEquals(2, b.size());
        assertEquals("tok", b.nextToken());
    }

    @Test
    void hasMoreFollowsTheToken() {
        assertTrue(new ExportBatch(rows(), "tok").hasMore());
        assertFalse(new ExportBatch(rows(), "").hasMore());
    }

    @Test
    void recordsAreImmutable() {
        ExportBatch b = new ExportBatch(rows(), "");
        assertThrows(UnsupportedOperationException.class, () -> b.records().add(new Record(3L, 30L, "c")));
    }

    @Test
    void anEmptyBatchHasSizeZero() {
        assertEquals(0, new ExportBatch(new ArrayList<>(), "").size());
    }
}
