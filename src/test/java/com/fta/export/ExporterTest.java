package com.fta.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExporterTest {

    private static RecordTable table(int rows) {
        RecordTable t = new RecordTable();
        for (int i = 1; i <= rows; i++) {
            t.insert(new Record(i, i * 1_000_000L, "p" + i));
        }
        return t;
    }

    @Test
    void exposesItsBatchSize() {
        assertEquals(4, new Exporter(table(1), 4).batchSize());
    }

    @Test
    void firstBatchStartsFromTheBeginning() {
        ExportBatch b = new Exporter(table(5), 2).next("");
        assertEquals(2, b.size());
        assertEquals(1L, b.records().get(0).id());
        assertTrue(b.hasMore());
    }

    @Test
    void aShortBatchEndsTheScan() {
        ExportBatch b = new Exporter(table(2), 5).next("");
        assertEquals(2, b.size());
        assertFalse(b.hasMore());
        assertEquals("", b.nextToken());
    }
}
