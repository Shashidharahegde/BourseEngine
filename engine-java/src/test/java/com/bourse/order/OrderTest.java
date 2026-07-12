package com.bourse.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderTest {

    // --- Constructor: happy paths ---

    @Test
    void limitOrderIsCreatedWithExpectedState() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 150, 10);

        assertEquals("1", order.getId());
        assertEquals("AAPL", order.getSymbol());
        assertEquals(Side.BUY, order.getSide());
        assertEquals(OrderType.LIMIT, order.getType());
        assertEquals(150, order.getPrice());
        assertEquals(10, order.getQuantity());
        assertEquals(10, order.getRemainingQuantity());
        assertEquals(OrderStatus.NEW, order.getStatus());
        assertNotNull(order.getTimestamp());
        assertFalse(order.isFilled());
    }

    @Test
    void marketOrderRequiresZeroPrice() {
        Order order = new Order("2", "MSFT", Side.SELL, OrderType.MARKET, 0, 5);

        assertEquals(0, order.getPrice());
        assertEquals(OrderType.MARKET, order.getType());
        assertEquals(OrderStatus.NEW, order.getStatus());
    }

    @Test
    void symbolIsTrimmedAndUppercased() {
        Order order = new Order("3", "  aapl  ", Side.BUY, OrderType.LIMIT, 100, 1);

        assertEquals("AAPL", order.getSymbol());
    }

    // --- Constructor: validation failures ---

    @Test
    void nullIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order(null, "AAPL", Side.BUY, OrderType.LIMIT, 100, 1));
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("  ", "AAPL", Side.BUY, OrderType.LIMIT, 100, 1));
    }

    @Test
    void nullSymbolIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("1", null, Side.BUY, OrderType.LIMIT, 100, 1));
    }

    @Test
    void blankSymbolIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("1", "   ", Side.BUY, OrderType.LIMIT, 100, 1));
    }

    @Test
    void nonPositiveQuantityIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, -5));
    }

    @Test
    void nullSideIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new Order("1", "AAPL", null, OrderType.LIMIT, 100, 1));
    }

    @Test
    void nullTypeIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new Order("1", "AAPL", Side.BUY, null, 100, 1));
    }

    @Test
    void limitOrderWithNonPositivePriceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, -1, 1));
    }

    @Test
    void marketOrderWithNonZeroPriceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order("1", "AAPL", Side.BUY, OrderType.MARKET, 100, 1));
    }

    // --- fill() ---

    @Test
    void partialFillUpdatesRemainingAndStatus() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);

        order.fill(4);

        assertEquals(6, order.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, order.getStatus());
        assertFalse(order.isFilled());
    }

    @Test
    void fullFillMarksOrderFilled() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);

        order.fill(10);

        assertEquals(0, order.getRemainingQuantity());
        assertEquals(OrderStatus.FILLED, order.getStatus());
        assertTrue(order.isFilled());
    }

    @Test
    void successiveFillsExhaustOrder() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);

        order.fill(3);
        order.fill(7);

        assertEquals(0, order.getRemainingQuantity());
        assertEquals(OrderStatus.FILLED, order.getStatus());
    }

    @Test
    void fillQuantityMustBePositive() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);

        assertThrows(IllegalArgumentException.class, () -> order.fill(0));
        assertThrows(IllegalArgumentException.class, () -> order.fill(-2));
    }

    @Test
    void fillCannotExceedRemainingQuantity() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);

        assertThrows(IllegalArgumentException.class, () -> order.fill(11));
    }

    @Test
    void filledOrderCannotBeFilledAgain() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);
        order.fill(10);

        assertThrows(IllegalStateException.class, () -> order.fill(1));
    }

    @Test
    void cancelledOrderCannotBeFilled() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);
        order.cancel();

        assertThrows(IllegalStateException.class, () -> order.fill(1));
    }

    // --- cancel() ---

    @Test
    void cancelMovesOrderToCancelled() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void partiallyFilledOrderCanBeCancelled() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);
        order.fill(4);

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void filledOrderCannotBeCancelled() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);
        order.fill(10);

        assertThrows(IllegalStateException.class, order::cancel);
    }

    @Test
    void alreadyCancelledOrderCannotBeCancelledAgain() {
        Order order = new Order("1", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10);
        order.cancel();

        assertThrows(IllegalStateException.class, order::cancel);
    }
}
