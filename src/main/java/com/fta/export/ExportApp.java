package com.fta.export;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs one full keyset export over a fixed table and checks the result against the
 * table contents. Every row must come out exactly once.
 */
public final class ExportApp {

    private static final long BASE_NANOS = 1_700_000_000_000L * 1_000_000L;
    private static final int BATCH_SIZE = 5;
    private static final int MAX_BATCHES = 200;

    public static void main(String[] args) {
        int batchSize = args.length > 0 ? Integer.parseInt(args[0]) : BATCH_SIZE;
        RecordTable table = seed();
        Exporter exporter = new Exporter(table, batchSize);

        System.out.println("ledger-export 1.6.0");
        System.out.println("rows=" + table.size() + " batchSize=" + batchSize);
        System.out.println("scanning ...");

        List<Long> exported = new ArrayList<>();
        String token = "";
        int batches = 0;
        boolean aborted = false;

        while (true) {
            ExportBatch batch = exporter.next(token);
            batches++;
            for (Record record : batch.records()) {
                exported.add(record.id());
            }
            if (!batch.hasMore()) {
                break;
            }
            token = batch.nextToken();
            if (batches >= MAX_BATCHES) {
                aborted = true;
                break;
            }
        }

        report(table, exported, batches, aborted);
    }

    private static RecordTable seed() {
        // Write bursts are uneven: some milliseconds carry one row, some carry four.
        int[] perMilli = {3, 1, 4, 2, 3, 2, 4, 1, 3, 2, 4, 3};
        RecordTable table = new RecordTable();
        long id = 1L;
        for (int bucket = 0; bucket < perMilli.length; bucket++) {
            long bucketNanos = BASE_NANOS + bucket * 1_000_000L;
            for (int k = 0; k < perMilli[bucket]; k++) {
                table.insert(new Record(id++, bucketNanos + k * 200_000L, "row-" + id));
            }
        }
        // A few rows land in the very same nanosecond as an earlier row. The pair is still
        // unique because the id breaks the tie.
        table.insert(new Record(id++, BASE_NANOS + 2 * 1_000_000L + 200_000L, "tie-2"));
        table.insert(new Record(id++, BASE_NANOS + 6 * 1_000_000L, "tie-6"));
        table.insert(new Record(id++, BASE_NANOS + 10 * 1_000_000L + 400_000L, "tie-10"));
        return table;
    }

    private static void report(RecordTable table, List<Long> exported, int batches, boolean aborted) {
        Map<Long, Integer> seen = new LinkedHashMap<>();
        for (Long id : exported) {
            seen.merge(id, 1, Integer::sum);
        }

        Set<Long> missed = new LinkedHashSet<>();
        for (Record record : table.all()) {
            if (!seen.containsKey(record.id())) {
                missed.add(record.id());
            }
        }

        int duplicated = 0;
        List<Long> duplicatedIds = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : seen.entrySet()) {
            if (entry.getValue() > 1) {
                duplicated += entry.getValue() - 1;
                duplicatedIds.add(entry.getKey());
            }
        }

        System.out.println();
        System.out.println("=== export report ===");
        if (!missed.isEmpty()) {
            System.out.println("  MISSED     ids " + missed);
        }
        if (!duplicatedIds.isEmpty()) {
            System.out.println("  DUPLICATED ids " + duplicatedIds);
        }
        if (aborted) {
            System.out.println("  ABORTED    cursor stopped advancing after " + batches + " batches");
        }

        System.out.println();
        System.out.println("table rows      : " + table.size());
        System.out.println("exported rows   : " + exported.size());
        System.out.println("missed rows     : " + missed.size());
        System.out.println("duplicated rows : " + duplicated);
        System.out.println("batches         : " + batches);

        if (!missed.isEmpty() || duplicated > 0 || aborted) {
            System.out.println();
            System.out.println("FAILED: export does not match the table");
            System.exit(1);
        }
        System.out.println();
        System.out.println("OK: every row exported exactly once");
    }

    private ExportApp() {
    }
}
