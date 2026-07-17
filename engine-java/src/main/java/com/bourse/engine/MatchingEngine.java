package com.bourse.engine;

import com.bourse.book.LimitOrderBook;
import com.bourse.order.Order;
import com.bourse.order.OrderStatus;
import com.bourse.order.OrderType;
import com.bourse.order.Side;
import com.bourse.trade.Trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class MatchingEngine {

    private final Map<String, LimitOrderBook> orderBooks; //Each symbol has its own independent order book.

    private final List<Trade> tradeHistory;

    private long nextTradeSequence; // Genrating Trade ids
    public MatchingEngine() {
        this.orderBooks = new HashMap<>();
        this.tradeHistory = new ArrayList<>();
        this.nextTradeSequence = 1;
    }


    public synchronized List<Trade> submitOrder(Order order) {
        Objects.requireNonNull(
                order,
                "Order must not be null"
        );

        validateOrderForSubmission(order);

        LimitOrderBook orderBook = orderBooks.computeIfAbsent(
                order.getSymbol(),
                LimitOrderBook::new
        );

        if (orderBook.containsOrder(order.getId())) {
            throw new IllegalArgumentException(
                    "An active order with this ID already exists"
            );
        }

        List<Trade> generatedTrades = new ArrayList<>();

        if (order.getSide() == Side.BUY) {
            matchBuyOrder(
                    order,
                    orderBook,
                    generatedTrades
            );
        } else {
            matchSellOrder(
                    order,
                    orderBook,
                    generatedTrades
            );
        }

        if (!order.isFilled()) {
            if (order.getType() == OrderType.LIMIT) {
                orderBook.addOrder(order);
            } else {
                order.cancel();
            }
        }

        tradeHistory.addAll(generatedTrades);

        return List.copyOf(generatedTrades);
    }


//methods: matchSellOrder & matchBuyOrder,  

    private void matchBuyOrder(
            Order buyOrder,
            LimitOrderBook orderBook,
            List<Trade> generatedTrades
    ) {
        while (!buyOrder.isFilled()) {
            Order sellOrder = orderBook.getBestAskOrder();

            if (sellOrder == null) {
                break;
            }

            if (buyOrder.getType() == OrderType.LIMIT
                    && sellOrder.getPrice() > buyOrder.getPrice()) {
                break;
            }

            long executedQuantity = Math.min(
                    buyOrder.getRemainingQuantity(),
                    sellOrder.getRemainingQuantity()
            );

            Order executedSellOrder =
                    orderBook.fillBestAskOrder(
                            executedQuantity
                    );

            buyOrder.fill(executedQuantity);

            Trade trade = new Trade(
                    generateTradeId(),
                    buyOrder.getSymbol(),
                    buyOrder.getId(),
                    executedSellOrder.getId(),
                    executedSellOrder.getPrice(),
                    executedQuantity
            );

            generatedTrades.add(trade);
        }
    }
    

    private void matchSellOrder(
            Order sellOrder,
            LimitOrderBook orderBook,
            List<Trade> generatedTrades
    ) {
        while (!sellOrder.isFilled()) {
            Order buyOrder = orderBook.getBestBidOrder();

            if (buyOrder == null) {
                break;
            }

            if (sellOrder.getType() == OrderType.LIMIT
                    && buyOrder.getPrice() < sellOrder.getPrice()) {
                break;
            }

            long executedQuantity = Math.min(
                    sellOrder.getRemainingQuantity(),
                    buyOrder.getRemainingQuantity()
            );

            Order executedBuyOrder =
                    orderBook.fillBestBidOrder(
                            executedQuantity
                    );

            sellOrder.fill(executedQuantity);

            Trade trade = new Trade(
                    generateTradeId(),
                    sellOrder.getSymbol(),
                    executedBuyOrder.getId(),
                    sellOrder.getId(),
                    executedBuyOrder.getPrice(),
                    executedQuantity
            );

            generatedTrades.add(trade);
        }
    }

    //Cancelling Order Logic:
    public synchronized boolean cancelOrder(
            String symbol,
            String orderId
    ) {
        String normalizedSymbol =
                normalizeSymbol(symbol);

        LimitOrderBook orderBook =
                orderBooks.get(normalizedSymbol);

        if (orderBook == null) {
            return false;
        }

        return orderBook.cancelOrder(orderId);
    }

    // Retrieves the order book for a symbol:

    public synchronized LimitOrderBook getOrderBook(
            String symbol
    ) {
        String normalizedSymbol =
                normalizeSymbol(symbol);

        return orderBooks.get(normalizedSymbol);
    }

    private String generateTradeId() {
        String tradeId =
                "TRD-" + nextTradeSequence;

        nextTradeSequence++;

        return tradeId;
    }

    public synchronized List<Trade> getTradeHistory() {
        return List.copyOf(tradeHistory);
    }

    public synchronized int getOrderBookCount() {
        return orderBooks.size();
    }

    private void validateOrderForSubmission(Order order) {
        if (order.getStatus() != OrderStatus.NEW) {
            throw new IllegalStateException(
                    "Only new orders can be submitted"
            );
        }
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Symbol must not be null or blank"
            );
        }

        return symbol
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}