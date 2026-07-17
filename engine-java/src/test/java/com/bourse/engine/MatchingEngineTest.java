package com.bourse.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bourse.book.LimitOrderBook;
import com.bourse.order.Order;
import com.bourse.order.OrderStatus;
import com.bourse.order.OrderType;
import com.bourse.order.Side;
import com.bourse.trade.Trade;

import java.util.List;

import org.junit.jupiter.api.Test;

class MatchingEngineTest {

    // --- helpers ---

    private static Order limit(String id, Side side, long price, long qty) {
        return new Order(id, "AAPL", side, OrderType.LIMIT, price, qty);
    }

    private static Order market(String id, Side side, long qty) {
        return new Order(id, "AAPL", side, OrderType.MARKET, 0, qty);
    }

    // --- submission validation ---

    @Test
    void nullOrderIsRejected() {
        assertThrows(NullPointerException.class, () -> new MatchingEngine().submitOrder(null));
    }

    @Test
    void nonNewOrderIsRejected() {
        MatchingEngine engine = new MatchingEngine();
        Order partiallyFilled = limit("1", Side.BUY, 100, 10);
        partiallyFilled.fill(4);

        assertThrows(IllegalStateException.class, () -> engine.submitOrder(partiallyFilled));
    }

    @Test
    void duplicateActiveIdIsRejected() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("1", Side.BUY, 100, 10)); // rests

        assertThrows(IllegalArgumentException.class,
                () -> engine.submitOrder(limit("1", Side.BUY, 99, 5)));
    }

    // --- resting with no cross ---

    @Test
    void unmatchedLimitBuyRestsAsBid() {
        MatchingEngine engine = new MatchingEngine();

        List<Trade> trades = engine.submitOrder(limit("1", Side.BUY, 100, 10));

        assertTrue(trades.isEmpty());
        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertNotNull(book);
        assertEquals(100L, book.getBestBidPrice());
        assertTrue(book.containsOrder("1"));
        assertEquals(OrderStatus.NEW, book.getOrder("1").getStatus());
    }

    @Test
    void unmatchedLimitSellRestsAsAsk() {
        MatchingEngine engine = new MatchingEngine();

        engine.submitOrder(limit("1", Side.SELL, 100, 10));

        assertEquals(100L, engine.getOrderBook("AAPL").getBestAskPrice());
    }

    @Test
    void buyBelowBestAskDoesNotCross() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("ask", Side.SELL, 105, 10));

        List<Trade> trades = engine.submitOrder(limit("bid", Side.BUY, 100, 10));

        assertTrue(trades.isEmpty());
        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertEquals(100L, book.getBestBidPrice());
        assertEquals(105L, book.getBestAskPrice());
        assertEquals(2, book.getOrderCount());
    }

    // --- full match ---

    @Test
    void incomingBuyFullyMatchesRestingAsk() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("sell", Side.SELL, 100, 10));

        List<Trade> trades = engine.submitOrder(limit("buy", Side.BUY, 100, 10));

        assertEquals(1, trades.size());
        Trade t = trades.get(0);
        assertEquals("AAPL", t.getSymbol());
        assertEquals("buy", t.getBuyOrderId());
        assertEquals("sell", t.getSellOrderId());
        assertEquals(100L, t.getPrice());
        assertEquals(10L, t.getQuantity());

        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertTrue(book.isEmpty());
        assertFalse(book.containsOrder("sell"));
    }

    @Test
    void incomingSellFullyMatchesRestingBid() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("buy", Side.BUY, 100, 10));

        List<Trade> trades = engine.submitOrder(limit("sell", Side.SELL, 100, 10));

        assertEquals(1, trades.size());
        Trade t = trades.get(0);
        assertEquals("buy", t.getBuyOrderId());
        assertEquals("sell", t.getSellOrderId());
        assertEquals(100L, t.getPrice());
        assertEquals(10L, t.getQuantity());
        assertTrue(engine.getOrderBook("AAPL").isEmpty());
    }

    @Test
    void matchedTradeIsRecordedInHistory() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("sell", Side.SELL, 100, 10));
        engine.submitOrder(limit("buy", Side.BUY, 100, 10));

        assertEquals(1, engine.getTradeHistory().size());
        assertEquals("buy", engine.getTradeHistory().get(0).getBuyOrderId());
    }

    // --- trade executes at resting price (price improvement) ---

    @Test
    void tradeExecutesAtRestingAskPrice() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("sell", Side.SELL, 100, 10));

        List<Trade> trades = engine.submitOrder(limit("buy", Side.BUY, 110, 10));

        assertEquals(100L, trades.get(0).getPrice()); // buyer improves from 110 to 100
    }

    @Test
    void tradeExecutesAtRestingBidPrice() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("buy", Side.BUY, 110, 10));

        List<Trade> trades = engine.submitOrder(limit("sell", Side.SELL, 100, 10));

        assertEquals(110L, trades.get(0).getPrice()); // seller improves from 100 to 110
    }

    // --- partial fills ---

    @Test
    void largerIncomingBuyRestsWithRemainder() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("sell", Side.SELL, 100, 4));

        List<Trade> trades = engine.submitOrder(limit("buy", Side.BUY, 100, 10));

        assertEquals(1, trades.size());
        assertEquals(4L, trades.get(0).getQuantity());

        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertFalse(book.containsOrder("sell")); // resting ask fully consumed
        assertTrue(book.containsOrder("buy"));    // remainder rests as bid
        Order resting = book.getOrder("buy");
        assertEquals(6L, resting.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, resting.getStatus());
        assertEquals(100L, book.getBestBidPrice());
    }

    @Test
    void smallerIncomingBuyLeavesRestingAskPartiallyFilled() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("sell", Side.SELL, 100, 10));

        List<Trade> trades = engine.submitOrder(limit("buy", Side.BUY, 100, 4));

        assertEquals(4L, trades.get(0).getQuantity());
        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertFalse(book.containsOrder("buy")); // incoming fully filled, does not rest
        Order restingAsk = book.getOrder("sell");
        assertEquals(6L, restingAsk.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, restingAsk.getStatus());
    }

    // --- multi-level sweep & priority ---

    @Test
    void buySweepsMultipleAskLevelsInPriceOrder() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("a1", Side.SELL, 100, 5));
        engine.submitOrder(limit("a2", Side.SELL, 101, 5));

        List<Trade> trades = engine.submitOrder(limit("buy", Side.BUY, 101, 8));

        assertEquals(2, trades.size());
        // best (lowest) ask matched first
        assertEquals(100L, trades.get(0).getPrice());
        assertEquals(5L, trades.get(0).getQuantity());
        assertEquals(101L, trades.get(1).getPrice());
        assertEquals(3L, trades.get(1).getQuantity());

        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertFalse(book.containsOrder("a1"));
        assertEquals(2L, book.getOrder("a2").getRemainingQuantity()); // 5 - 3 left
        assertFalse(book.containsOrder("buy")); // incoming fully filled
    }

    @Test
    void samePriceLevelMatchesOldestFirst() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("first", Side.SELL, 100, 5));
        engine.submitOrder(limit("second", Side.SELL, 100, 5));

        List<Trade> trades = engine.submitOrder(limit("buy", Side.BUY, 100, 5));

        assertEquals(1, trades.size());
        assertEquals("first", trades.get(0).getSellOrderId()); // FIFO: oldest first
        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertFalse(book.containsOrder("first"));
        assertTrue(book.containsOrder("second"));
    }

    // --- trade id sequencing ---

    @Test
    void tradeIdsAreSequential() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("a1", Side.SELL, 100, 5));
        engine.submitOrder(limit("a2", Side.SELL, 101, 5));

        List<Trade> trades = engine.submitOrder(limit("buy", Side.BUY, 101, 10));

        assertEquals("TRD-1", trades.get(0).getId());
        assertEquals("TRD-2", trades.get(1).getId());
    }

    // --- market orders ---

    @Test
    void marketBuyFullyMatchesAndDoesNotRest() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("sell", Side.SELL, 100, 10));

        List<Trade> trades = engine.submitOrder(market("mkt", Side.BUY, 10));

        assertEquals(1, trades.size());
        assertEquals(100L, trades.get(0).getPrice());
        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertTrue(book.isEmpty());
        assertFalse(book.containsOrder("mkt"));
    }

    @Test
    void marketBuyDiscardsUnfilledRemainder() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("sell", Side.SELL, 100, 4));

        Order mkt = market("mkt", Side.BUY, 10);
        List<Trade> trades = engine.submitOrder(mkt);

        assertEquals(1, trades.size());
        assertEquals(4L, trades.get(0).getQuantity());
        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertFalse(book.containsOrder("mkt")); // leftover discarded, not rested
        assertEquals(OrderStatus.CANCELLED, mkt.getStatus());
        assertTrue(book.isEmpty());
    }

    @Test
    void marketOrderWithNoLiquidityProducesNoTrades() {
        MatchingEngine engine = new MatchingEngine();

        Order mkt = market("mkt", Side.BUY, 10);
        List<Trade> trades = engine.submitOrder(mkt);

        assertTrue(trades.isEmpty());
        assertEquals(OrderStatus.CANCELLED, mkt.getStatus());
        assertTrue(engine.getOrderBook("AAPL").isEmpty());
    }

    @Test
    void marketSellMatchesRestingBids() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("b1", Side.BUY, 101, 5));
        engine.submitOrder(limit("b2", Side.BUY, 100, 5));

        List<Trade> trades = engine.submitOrder(market("mkt", Side.SELL, 8));

        assertEquals(2, trades.size());
        assertEquals(101L, trades.get(0).getPrice()); // highest bid first
        assertEquals(5L, trades.get(0).getQuantity());
        assertEquals(100L, trades.get(1).getPrice());
        assertEquals(3L, trades.get(1).getQuantity());
        // b2 had qty 5, 3 consumed -> 2 remains
        assertEquals(2L, engine.getOrderBook("AAPL").getOrder("b2").getRemainingQuantity());
    }

    // --- per-symbol isolation ---

    @Test
    void ordersAreRoutedToPerSymbolBooks() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(new Order("a", "AAPL", Side.BUY, OrderType.LIMIT, 100, 10));
        engine.submitOrder(new Order("m", "MSFT", Side.BUY, OrderType.LIMIT, 200, 10));

        assertEquals(2, engine.getOrderBookCount());
        assertTrue(engine.getOrderBook("AAPL").containsOrder("a"));
        assertTrue(engine.getOrderBook("MSFT").containsOrder("m"));
        assertFalse(engine.getOrderBook("AAPL").containsOrder("m"));
    }

    @Test
    void symbolIsNormalizedOnSubmitAndLookup() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(new Order("a", "  aapl ", Side.BUY, OrderType.LIMIT, 100, 10));

        assertSame(engine.getOrderBook("AAPL"), engine.getOrderBook("aapl"));
        assertTrue(engine.getOrderBook("aapl").containsOrder("a"));
    }

    // --- cancellation ---

    @Test
    void cancelRestingOrderThroughEngine() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("1", Side.BUY, 100, 10));

        assertTrue(engine.cancelOrder("AAPL", "1"));
        assertFalse(engine.getOrderBook("AAPL").containsOrder("1"));
    }

    @Test
    void cancelUnknownSymbolReturnsFalse() {
        assertFalse(new MatchingEngine().cancelOrder("NOPE", "1"));
    }

    @Test
    void cancelUnknownOrderReturnsFalse() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("1", Side.BUY, 100, 10));

        assertFalse(engine.cancelOrder("AAPL", "missing"));
    }

    @Test
    void cancelToleratesSymbolCasing() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("1", Side.BUY, 100, 10));

        assertTrue(engine.cancelOrder("aapl", "1"));
    }

    // --- accessors ---

    @Test
    void getOrderBookRejectsBlankSymbol() {
        MatchingEngine engine = new MatchingEngine();

        assertThrows(IllegalArgumentException.class, () -> engine.getOrderBook(" "));
        assertThrows(IllegalArgumentException.class, () -> engine.cancelOrder(null, "1"));
    }

    @Test
    void getOrderBookReturnsNullForUnseenSymbol() {
        assertNull(new MatchingEngine().getOrderBook("AAPL"));
    }

    @Test
    void tradeHistoryIsImmutableSnapshot() {
        MatchingEngine engine = new MatchingEngine();
        engine.submitOrder(limit("sell", Side.SELL, 100, 10));
        engine.submitOrder(limit("buy", Side.BUY, 100, 10));

        List<Trade> history = engine.getTradeHistory();
        Trade extra = new Trade("TRD-X", "AAPL", "b", "s", 100, 1);

        assertThrows(UnsupportedOperationException.class, () -> history.add(extra));
    }

    // --- end-to-end scenario ---

    @Test
    void multiStepScenarioLeavesConsistentBookAndHistory() {
        MatchingEngine engine = new MatchingEngine();

        engine.submitOrder(limit("s1", Side.SELL, 101, 10));
        engine.submitOrder(limit("s2", Side.SELL, 102, 10));
        engine.submitOrder(limit("b1", Side.BUY, 99, 10));

        // crossing buy: takes all of s1 @101, 5 of s2 @102, nothing rests
        List<Trade> trades = engine.submitOrder(limit("b2", Side.BUY, 102, 15));

        assertEquals(2, trades.size());
        assertEquals(10L, trades.get(0).getQuantity());
        assertEquals(5L, trades.get(1).getQuantity());

        LimitOrderBook book = engine.getOrderBook("AAPL");
        assertFalse(book.containsOrder("s1"));
        assertFalse(book.containsOrder("b2"));
        assertEquals(5L, book.getOrder("s2").getRemainingQuantity());
        assertEquals(99L, book.getBestBidPrice());  // b1 still rests
        assertEquals(102L, book.getBestAskPrice());
        assertEquals(2, engine.getTradeHistory().size());
    }
}
