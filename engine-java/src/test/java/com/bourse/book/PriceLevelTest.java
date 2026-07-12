package com.bourse.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bourse.order.Order;
import com.bourse.order.OrderStatus;
import com.bourse.order.OrderType;
import com.bourse.order.Side;

import java.util.List;

import org.junit.jupiter.api.Test;

class PriceLevelTest {

    private static final long PRICE = 100;

    // --- helpers ---

    private static Order limit(String id, long price, long qty) {
        return new Order(id, "AAPL", Side.BUY, OrderType.LIMIT, price, qty);
    }

    private static Order limit(String id, long qty) {
        return limit(id, PRICE, qty);
    }

    // --- constructor ---

    @Test
    void newLevelIsEmptyWithGivenPrice() {
        PriceLevel level = new PriceLevel(PRICE);

        assertEquals(PRICE, level.getPrice());
        assertEquals(0, level.getTotalQuantity());
        assertEquals(0, level.getOrderCount());
        assertTrue(level.isEmpty());
    }

    @Test
    void nonPositivePriceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PriceLevel(0));
        assertThrows(IllegalArgumentException.class, () -> new PriceLevel(-1));
    }

    // --- addOrder ---

    @Test
    void addingOrderUpdatesCountAndTotalQuantity() {
        PriceLevel level = new PriceLevel(PRICE);

        level.addOrder(limit("1", 10));

        assertEquals(1, level.getOrderCount());
        assertEquals(10, level.getTotalQuantity());
        assertFalse(level.isEmpty());
    }

    @Test
    void totalQuantityAccumulatesAcrossOrders() {
        PriceLevel level = new PriceLevel(PRICE);

        level.addOrder(limit("1", 10));
        level.addOrder(limit("2", 5));

        assertEquals(2, level.getOrderCount());
        assertEquals(15, level.getTotalQuantity());
    }

    @Test
    void nullOrderIsRejected() {
        PriceLevel level = new PriceLevel(PRICE);

        assertThrows(NullPointerException.class, () -> level.addOrder(null));
    }

    @Test
    void orderWithMismatchedPriceIsRejected() {
        PriceLevel level = new PriceLevel(PRICE);

        assertThrows(IllegalArgumentException.class,
                () -> level.addOrder(limit("1", PRICE + 1, 10)));
    }

    @Test
    void marketOrderIsRejected() {
        PriceLevel level = new PriceLevel(PRICE);
        Order market = new Order("1", "AAPL", Side.BUY, OrderType.MARKET, 0, 10);

        assertThrows(IllegalArgumentException.class, () -> level.addOrder(market));
    }

    @Test
    void filledOrderCannotBeAdded() {
        PriceLevel level = new PriceLevel(PRICE);
        Order order = limit("1", 10);
        order.fill(10);

        assertThrows(IllegalStateException.class, () -> level.addOrder(order));
    }

    @Test
    void cancelledOrderCannotBeAdded() {
        PriceLevel level = new PriceLevel(PRICE);
        Order order = limit("1", 10);
        order.cancel();

        assertThrows(IllegalStateException.class, () -> level.addOrder(order));
    }

    // --- FIFO / peek ---

    @Test
    void peekReturnsOldestOrderFirst() {
        PriceLevel level = new PriceLevel(PRICE);
        Order first = limit("1", 10);
        Order second = limit("2", 5);

        level.addOrder(first);
        level.addOrder(second);

        assertSame(first, level.peekFirstOrder());
    }

    @Test
    void peekOnEmptyLevelReturnsNull() {
        PriceLevel level = new PriceLevel(PRICE);

        assertEquals(null, level.peekFirstOrder());
    }

    // --- fillFirstOrder (package-private) ---

    @Test
    void partialFillReducesTotalQuantityButKeepsOrder() {
        PriceLevel level = new PriceLevel(PRICE);
        Order order = limit("1", 10);
        level.addOrder(order);

        level.fillFirstOrder(4);

        assertEquals(6, level.getTotalQuantity());
        assertEquals(6, order.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, order.getStatus());
        assertEquals(1, level.getOrderCount());
        assertSame(order, level.peekFirstOrder());
    }

    @Test
    void fullyFillingHeadDrivesTotalQuantityToZero() {
        PriceLevel level = new PriceLevel(PRICE);
        Order order = limit("1", 10);
        level.addOrder(order);

        level.fillFirstOrder(10);

        assertEquals(0, level.getTotalQuantity());
        assertTrue(order.isFilled());
    }

    @Test
    void fillOnEmptyLevelIsRejected() {
        PriceLevel level = new PriceLevel(PRICE);

        assertThrows(IllegalStateException.class, () -> level.fillFirstOrder(1));
    }

    @Test
    void nonPositiveFillQuantityIsRejected() {
        PriceLevel level = new PriceLevel(PRICE);
        level.addOrder(limit("1", 10));

        assertThrows(IllegalArgumentException.class, () -> level.fillFirstOrder(0));
        assertThrows(IllegalArgumentException.class, () -> level.fillFirstOrder(-3));
    }

    @Test
    void fillExceedingHeadRemainingIsRejected() {
        PriceLevel level = new PriceLevel(PRICE);
        level.addOrder(limit("1", 10));

        assertThrows(IllegalArgumentException.class, () -> level.fillFirstOrder(11));
    }

    // --- removeOrder ---

    @Test
    void removingOrderUpdatesTotalQuantityAndCount() {
        PriceLevel level = new PriceLevel(PRICE);
        Order first = limit("1", 10);
        Order second = limit("2", 5);
        level.addOrder(first);
        level.addOrder(second);

        assertTrue(level.removeOrder(first));

        assertEquals(1, level.getOrderCount());
        assertEquals(5, level.getTotalQuantity());
        assertSame(second, level.peekFirstOrder());
    }

    @Test
    void removingAbsentOrderReturnsFalseAndKeepsQuantity() {
        PriceLevel level = new PriceLevel(PRICE);
        level.addOrder(limit("1", 10));

        assertFalse(level.removeOrder(limit("absent", 5)));
        assertEquals(10, level.getTotalQuantity());
    }

    @Test
    void removeUsesRemainingQuantityAfterPartialFill() {
        PriceLevel level = new PriceLevel(PRICE);
        Order order = limit("1", 10);
        level.addOrder(order);
        level.fillFirstOrder(4); // remaining 6, totalQuantity 6

        assertTrue(level.removeOrder(order));
        assertEquals(0, level.getTotalQuantity());
        assertTrue(level.isEmpty());
    }

    @Test
    void nullOrderRemovalIsRejected() {
        PriceLevel level = new PriceLevel(PRICE);

        assertThrows(NullPointerException.class, () -> level.removeOrder(null));
    }

    // --- getOrders (defensive copy) ---

    @Test
    void getOrdersReturnsSnapshotInFifoOrder() {
        PriceLevel level = new PriceLevel(PRICE);
        Order first = limit("1", 10);
        Order second = limit("2", 5);
        level.addOrder(first);
        level.addOrder(second);

        List<Order> orders = level.getOrders();

        assertEquals(List.of(first, second), orders);
    }

    @Test
    void getOrdersIsImmutable() {
        PriceLevel level = new PriceLevel(PRICE);
        level.addOrder(limit("1", 10));

        List<Order> orders = level.getOrders();

        assertThrows(UnsupportedOperationException.class,
                () -> orders.add(limit("2", 5)));
    }
}
