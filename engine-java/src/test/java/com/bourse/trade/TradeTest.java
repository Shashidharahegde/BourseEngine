package com.bourse.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TradeTest {

    // --- Constructor: happy paths ---

    @Test
    void tradeIsCreatedWithExpectedState() {
        Trade trade = new Trade("t1", "AAPL", "buy1", "sell1", 150, 10);

        assertEquals("t1", trade.getId());
        assertEquals("AAPL", trade.getSymbol());
        assertEquals("buy1", trade.getBuyOrderId());
        assertEquals("sell1", trade.getSellOrderId());
        assertEquals(150, trade.getPrice());
        assertEquals(10, trade.getQuantity());
        assertNotNull(trade.getTimestamp());
    }

    @Test
    void symbolIsTrimmedAndUppercased() {
        Trade trade = new Trade("t1", "  aapl  ", "buy1", "sell1", 150, 10);

        assertEquals("AAPL", trade.getSymbol());
    }

    // --- Constructor: validation failures ---

    @Test
    void nullIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade(null, "AAPL", "buy1", "sell1", 150, 10));
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("  ", "AAPL", "buy1", "sell1", 150, 10));
    }

    @Test
    void nullSymbolIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", null, "buy1", "sell1", 150, 10));
    }

    @Test
    void blankSymbolIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "   ", "buy1", "sell1", 150, 10));
    }

    @Test
    void nullBuyOrderIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "AAPL", null, "sell1", 150, 10));
    }

    @Test
    void blankBuyOrderIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "AAPL", "  ", "sell1", 150, 10));
    }

    @Test
    void nullSellOrderIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "AAPL", "buy1", null, 150, 10));
    }

    @Test
    void blankSellOrderIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "AAPL", "buy1", "  ", 150, 10));
    }

    @Test
    void nonPositivePriceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "AAPL", "buy1", "sell1", 0, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "AAPL", "buy1", "sell1", -5, 10));
    }

    @Test
    void nonPositiveQuantityIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "AAPL", "buy1", "sell1", 150, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "AAPL", "buy1", "sell1", 150, -3));
    }

    @Test
    void identicalBuyAndSellOrderIdsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Trade("t1", "AAPL", "same", "same", 150, 10));
    }

    // --- toString() ---

    @Test
    void toStringContainsKeyFields() {
        Trade trade = new Trade("t1", "AAPL", "buy1", "sell1", 150, 10);

        String text = trade.toString();

        assertTrue(text.contains("t1"));
        assertTrue(text.contains("AAPL"));
        assertTrue(text.contains("buy1"));
        assertTrue(text.contains("sell1"));
        assertTrue(text.contains("150"));
        assertTrue(text.contains("10"));
    }
}
