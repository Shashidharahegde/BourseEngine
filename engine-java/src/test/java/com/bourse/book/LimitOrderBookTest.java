package com.bourse.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bourse.order.Order;
import com.bourse.order.OrderStatus;
import com.bourse.order.OrderType;
import com.bourse.order.Side;

import org.junit.jupiter.api.Test;

class LimitOrderBookTest {

    // --- helpers ---

    private static Order limit(String id, Side side, long price, long qty) {
        return new Order(id, "AAPL", side, OrderType.LIMIT, price, qty);
    }

    private static LimitOrderBook book() {
        return new LimitOrderBook("AAPL");
    }

    // --- constructor ---

    @Test
    void newBookIsEmpty() {
        LimitOrderBook book = book();

        assertTrue(book.isEmpty());
        assertEquals(0, book.getOrderCount());
        assertEquals(0, book.getBidLevelCount());
        assertEquals(0, book.getAskLevelCount());
        assertNull(book.getBestBidOrder());
        assertNull(book.getBestAskOrder());
        assertNull(book.getBestBidPrice());
        assertNull(book.getBestAskPrice());
    }

    @Test
    void symbolIsTrimmedAndUppercased() {
        assertEquals("AAPL", new LimitOrderBook("  aapl ").getSymbol());
    }

    @Test
    void nullOrBlankSymbolIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new LimitOrderBook(null));
        assertThrows(IllegalArgumentException.class, () -> new LimitOrderBook("  "));
    }

    // --- addOrder ---

    @Test
    void addingBidRegistersOrderAndBestPrice() {
        LimitOrderBook book = book();
        Order bid = limit("1", Side.BUY, 100, 10);

        book.addOrder(bid);

        assertFalse(book.isEmpty());
        assertEquals(1, book.getOrderCount());
        assertEquals(1, book.getBidLevelCount());
        assertTrue(book.containsOrder("1"));
        assertSame(bid, book.getOrder("1"));
        assertSame(bid, book.getBestBidOrder());
        assertEquals(100L, book.getBestBidPrice());
    }

    @Test
    void nullOrderIsRejected() {
        assertThrows(NullPointerException.class, () -> book().addOrder(null));
    }

    @Test
    void orderWithForeignSymbolIsRejected() {
        LimitOrderBook book = book();
        Order foreign = limit("1", Side.BUY, 100, 10);
        Order msft = new Order("2", "MSFT", Side.BUY, OrderType.LIMIT, 100, 10);

        book.addOrder(foreign); // sanity: matching symbol is fine
        assertThrows(IllegalArgumentException.class, () -> book.addOrder(msft));
    }

    @Test
    void marketOrderCannotRest() {
        LimitOrderBook book = book();
        Order market = new Order("1", "AAPL", Side.BUY, OrderType.MARKET, 0, 10);

        assertThrows(IllegalArgumentException.class, () -> book.addOrder(market));
    }

    @Test
    void completedOrderCannotBeAdded() {
        LimitOrderBook book = book();
        Order filled = limit("1", Side.BUY, 100, 10);
        filled.fill(10);

        assertThrows(IllegalStateException.class, () -> book.addOrder(filled));
    }

    @Test
    void duplicateOrderIdIsRejected() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.BUY, 100, 10));

        assertThrows(IllegalArgumentException.class,
                () -> book.addOrder(limit("1", Side.BUY, 101, 5)));
    }

    // --- price-time priority ---

    @Test
    void bestBidIsHighestPrice() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.BUY, 100, 10));
        book.addOrder(limit("2", Side.BUY, 105, 10));
        book.addOrder(limit("3", Side.BUY, 95, 10));

        assertEquals(105L, book.getBestBidPrice());
        assertEquals("2", book.getBestBidOrder().getId());
        assertEquals(3, book.getBidLevelCount());
    }

    @Test
    void bestAskIsLowestPrice() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.SELL, 100, 10));
        book.addOrder(limit("2", Side.SELL, 105, 10));
        book.addOrder(limit("3", Side.SELL, 95, 10));

        assertEquals(95L, book.getBestAskPrice());
        assertEquals("3", book.getBestAskOrder().getId());
        assertEquals(3, book.getAskLevelCount());
    }

    @Test
    void ordersAtSamePriceKeepFifoOrder() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.BUY, 100, 10));
        book.addOrder(limit("2", Side.BUY, 100, 10));

        assertEquals(1, book.getBidLevelCount());
        assertEquals("1", book.getBestBidOrder().getId());
    }

    // --- fillBest*Order ---

    @Test
    void partialFillKeepsOrderResting() {
        LimitOrderBook book = book();
        Order bid = limit("1", Side.BUY, 100, 10);
        book.addOrder(bid);

        Order filled = book.fillBestBidOrder(4);

        assertSame(bid, filled);
        assertEquals(6, bid.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, bid.getStatus());
        assertTrue(book.containsOrder("1"));
        assertEquals(1, book.getOrderCount());
        assertSame(bid, book.getBestBidOrder());
    }

    @Test
    void fullFillRemovesOrderAndLevel() {
        LimitOrderBook book = book();
        Order bid = limit("1", Side.BUY, 100, 10);
        book.addOrder(bid);

        Order filled = book.fillBestBidOrder(10);

        assertSame(bid, filled);
        assertTrue(bid.isFilled());
        assertFalse(book.containsOrder("1"));
        assertEquals(0, book.getOrderCount());
        assertEquals(0, book.getBidLevelCount());
        assertNull(book.getBestBidPrice());
    }

    @Test
    void fillAdvancesToNextOrderAtSameLevel() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.BUY, 100, 10));
        book.addOrder(limit("2", Side.BUY, 100, 7));

        book.fillBestBidOrder(10); // exhausts order 1

        assertFalse(book.containsOrder("1"));
        assertEquals("2", book.getBestBidOrder().getId());
        assertEquals(1, book.getBidLevelCount());
    }

    @Test
    void fillAdvancesToNextLevelWhenTopEmpties() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.SELL, 95, 10));
        book.addOrder(limit("2", Side.SELL, 100, 10));

        book.fillBestAskOrder(10); // exhausts the 95 level

        assertEquals(100L, book.getBestAskPrice());
        assertEquals("2", book.getBestAskOrder().getId());
        assertEquals(1, book.getAskLevelCount());
    }

    @Test
    void fillingEmptySideIsRejected() {
        LimitOrderBook book = book();

        assertThrows(IllegalStateException.class, () -> book.fillBestBidOrder(1));
        assertThrows(IllegalStateException.class, () -> book.fillBestAskOrder(1));
    }

    @Test
    void fillExceedingHeadRemainingIsRejected() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.BUY, 100, 10));

        assertThrows(IllegalArgumentException.class, () -> book.fillBestBidOrder(11));
    }

    // --- cancelOrder ---

    @Test
    void cancelRemovesOrderAndLevel() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.BUY, 100, 10));

        assertTrue(book.cancelOrder("1"));
        assertFalse(book.containsOrder("1"));
        assertEquals(0, book.getOrderCount());
        assertEquals(0, book.getBidLevelCount());
    }

    @Test
    void cancelKeepsOtherOrdersAtSameLevel() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.BUY, 100, 10));
        book.addOrder(limit("2", Side.BUY, 100, 5));

        assertTrue(book.cancelOrder("1"));
        assertEquals("2", book.getBestBidOrder().getId());
        assertEquals(1, book.getBidLevelCount());
    }

    @Test
    void cancelPartiallyFilledOrderIsAllowed() {
        LimitOrderBook book = book();
        book.addOrder(limit("1", Side.BUY, 100, 10));
        book.fillBestBidOrder(4);

        assertTrue(book.cancelOrder("1"));
        assertFalse(book.containsOrder("1"));
    }

    @Test
    void cancellingUnknownOrderReturnsFalse() {
        assertFalse(book().cancelOrder("nope"));
    }

    // --- removeCompletedOrder ---

    @Test
    void removeCompletedRemovesCancelledOrder() {
        LimitOrderBook book = book();
        Order order = limit("1", Side.BUY, 100, 10);
        book.addOrder(order);
        order.cancel();

        book.removeCompletedOrder(order);

        assertFalse(book.containsOrder("1"));
        assertEquals(0, book.getBidLevelCount());
    }

    @Test
    void removeCompletedRejectsActiveOrder() {
        LimitOrderBook book = book();
        Order order = limit("1", Side.BUY, 100, 10);
        book.addOrder(order);

        assertThrows(IllegalStateException.class, () -> book.removeCompletedOrder(order));
    }

    @Test
    void removeCompletedRejectsForeignOrder() {
        LimitOrderBook book = book();
        Order stored = limit("1", Side.BUY, 100, 10);
        book.addOrder(stored);

        Order impostor = limit("1", Side.BUY, 100, 10);
        impostor.cancel();

        assertThrows(IllegalArgumentException.class,
                () -> book.removeCompletedOrder(impostor));
    }

    @Test
    void removeCompletedRejectsNull() {
        assertThrows(NullPointerException.class, () -> book().removeCompletedOrder(null));
    }

    // --- lookups / validation ---

    @Test
    void getOrderReturnsNullForUnknownId() {
        assertNull(book().getOrder("missing"));
    }

    @Test
    void lookupsRejectBlankId() {
        LimitOrderBook book = book();

        assertThrows(IllegalArgumentException.class, () -> book.getOrder(" "));
        assertThrows(IllegalArgumentException.class, () -> book.containsOrder(null));
        assertThrows(IllegalArgumentException.class, () -> book.cancelOrder(""));
    }

    // --- both sides coexist ---

    @Test
    void bidsAndAsksAreTrackedIndependently() {
        LimitOrderBook book = book();
        book.addOrder(limit("b1", Side.BUY, 99, 10));
        book.addOrder(limit("a1", Side.SELL, 101, 10));

        assertEquals(99L, book.getBestBidPrice());
        assertEquals(101L, book.getBestAskPrice());
        assertEquals(2, book.getOrderCount());
        assertEquals(1, book.getBidLevelCount());
        assertEquals(1, book.getAskLevelCount());
    }
}
