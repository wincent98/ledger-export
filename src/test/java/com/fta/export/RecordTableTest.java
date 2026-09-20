package com.fta.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordTableTest {

    private RecordTable table;

    @BeforeEach
    void setUp() {
        table = new RecordTable();
        table.insert(new Record(3L, 3_000_000L, "c"));
        table.insert(new Record(1L, 1_000_000L, "a"));
        table.insert(new Record(2L, 2_000_000L, "b"));
    }

    @Test
    void keepsRowsOrderedByTimestamp() {
        List<Record> all = table.all();
        assertEquals(1L, all.get(0).id());
        assertEquals(2L, all.get(1).id());
        assertEquals(3L, all.get(2).id());
        assertEquals(3, table.size());
    }

    @Test
    void allReturnsAnImmutableView() {
        assertThrows(UnsupportedOperationException.class,
                () -> table.all().add(new Record(4L, 4_000_000L, "d")));
    }

    @Test
    void pageFromStartReturnsTheFirstRows() {
        List<Record> page = table.page(Cursor.START, 2);
        assertEquals(2, page.size());
        assertEquals(1L, page.get(0).id());
        assertEquals(2L, page.get(1).id());
    }

    @Test
    void pageResumesAfterAPositionBetweenTwoRows() {
        // 1_500_000 sits between row 1 and row 2, so the boundary is unambiguous.
        List<Record> page = table.page(new Cursor(1_500_000L, 1L), 10);
        assertEquals(2, page.size());
        assertEquals(2L, page.get(0).id());
        assertEquals(3L, page.get(1).id());
    }

    @Test
    void pageRejectsANonPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () -> table.page(Cursor.START, 0));
        assertTrue(table.page(Cursor.START, 1).size() == 1);
    }
}
