package com.fta.export;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

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

    @Test
    void exportsEveryRowExactlyOnceAcrossSubMillisecondBoundariesAndTies() {
        // Regression: batches cut through partially elapsed milliseconds and through
        // groups of rows sharing one timestamp; every row must come out exactly once.
        RecordTable t = new RecordTable();
        long base = 1_700_000_000_000L * 1_000_000L;
        long id = 1L;
        for (int milli = 0; milli < 4; milli++) {
            for (int k = 0; k < 3; k++) {
                t.insert(new Record(id++, base + milli * 1_000_000L + k * 300_000L, "p"));
            }
        }
        t.insert(new Record(id++, base + 1_000_000L, "tie"));
        t.insert(new Record(id++, base + 2_600_000L, "tie2"));

        Exporter exporter = new Exporter(t, 4);
        Map<Long, Integer> seen = new HashMap<>();
        String token = "";
        int guard = 0;
        while (true) {
            ExportBatch batch = exporter.next(token);
            for (Record record : batch.records()) {
                seen.merge(record.id(), 1, Integer::sum);
            }
            if (!batch.hasMore()) {
                break;
            }
            token = batch.nextToken();
            assertTrue(++guard < 100, "cursor stopped advancing");
        }

        assertEquals(t.size(), seen.size());
        for (Map.Entry<Long, Integer> entry : seen.entrySet()) {
            assertEquals(1, entry.getValue(), "row " + entry.getKey() + " exported twice");
        }
    }
}
