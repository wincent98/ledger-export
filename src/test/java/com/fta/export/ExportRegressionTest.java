package com.fta.export;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regression coverage for the keyset boundary bugs: a batch ending inside a
 * millisecond and several rows sharing the exact same nanosecond.
 */
class ExportRegressionTest {

    private static RecordTable denseTable() {
        RecordTable table = new RecordTable();
        long base = 1_700_000_000_000L * 1_000_000L;
        long id = 1L;
        // Four rows packed into the same millisecond, 200 microseconds apart.
        for (int k = 0; k < 4; k++) {
            table.insert(new Record(id++, base + k * 200_000L, "r" + id));
        }
        // Two rows at the exact same nanosecond, distinguished only by id.
        table.insert(new Record(id++, base + 1_000_000L, "tie-a"));
        table.insert(new Record(id++, base + 1_000_000L, "tie-b"));
        table.insert(new Record(id++, base + 1_200_000L, "tail"));
        return table;
    }

    @Test
    void everyRowIsExportedExactlyOnceAcrossBatches() {
        RecordTable table = denseTable();
        Exporter exporter = new Exporter(table, 2);

        Map<Long, Integer> seen = new LinkedHashMap<>();
        String token = "";
        for (int batch = 0; batch < 100; batch++) {
            ExportBatch next = exporter.next(token);
            for (Record record : next.records()) {
                seen.merge(record.id(), 1, Integer::sum);
            }
            token = next.nextToken();
            if (token.isEmpty()) {
                break;
            }
        }

        assertEquals(table.size(), seen.size(), "a row was missed");
        for (Map.Entry<Long, Integer> entry : seen.entrySet()) {
            assertEquals(1, entry.getValue(), "row exported more than once: " + entry.getKey());
        }
    }

    @Test
    void aLegacyV1TokenResumesAtTheRightRow() {
        RecordTable table = denseTable();
        // Old token issued on id 4, whose timestamp (base + 600_000ns) was rounded up
        // to the next millisecond by the v1 encoder.
        long baseMillis = 1_700_000_000_000L;
        String issued = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("v1:" + baseMillis + ":4").getBytes(StandardCharsets.UTF_8));

        List<Long> ids = new ArrayList<>();
        String token = issued;
        do {
            ExportBatch next = new Exporter(table, 2).next(token);
            for (Record record : next.records()) {
                ids.add(record.id());
            }
            token = next.nextToken();
        } while (!token.isEmpty());

        // Rows 1-4 were already delivered before the token; resumption must yield 5,6,7 once.
        assertEquals(List.of(5L, 6L, 7L), ids);
        assertFalse(ids.contains(4L), "last row before the token must not be replayed");
    }
}
