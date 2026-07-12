package com.bourse.book;

import com.bourse.order.Order;
import com.bourse.order.OrderStatus;
import com.bourse.order.OrderType;
import com.bourse.order.Side;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public final class LimitOrderBook {

    private final String symbol;

    /*
     * Buy-price levels ordered from highest price to lowest price.
     */
    private final NavigableMap<Long, PriceLevel> bids;

    /*
     * Sell-price levels ordered from lowest price to highest price.
     */
    private final NavigableMap<Long, PriceLevel> asks;


    private final Map<String, Order> ordersById;

    public LimitOrderBook(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Symbol must not be null or blank"
            );
        }

        this.symbol = symbol
                .trim()
                .toUpperCase(Locale.ROOT);

        this.bids = new TreeMap<>(
                Collections.reverseOrder()
        );

        this.asks = new TreeMap<>();
        this.ordersById = new HashMap<>();
    }

    public void addOrder(Order order) {
        Objects.requireNonNull(
                order,
                "Order must not be null"
        );

        validateOrderForBook(order);

        NavigableMap<Long, PriceLevel> levels =
                getLevelsForSide(order.getSide());

        PriceLevel priceLevel = levels.computeIfAbsent(
                order.getPrice(),
                PriceLevel::new
        );

        priceLevel.addOrder(order);
        ordersById.put(order.getId(), order);
    }

    public Order getOrder(String orderId) {
        validateOrderId(orderId);
        return ordersById.get(orderId);
    }

    public boolean containsOrder(String orderId) {
        validateOrderId(orderId);
        return ordersById.containsKey(orderId);
    }

    public Order getBestBidOrder() {
        PriceLevel level = getBestBidLevel();

        return level == null
                ? null
                : level.peekFirstOrder();
    }

    public Order getBestAskOrder() {
        PriceLevel level = getBestAskLevel();

        return level == null
                ? null
                : level.peekFirstOrder();
    }

    public PriceLevel getBestBidLevel() {
        return getBestLevel(bids);
    }

    public PriceLevel getBestAskLevel() {
        return getBestLevel(asks);
    }

    public Long getBestBidPrice() {
        PriceLevel level = getBestBidLevel();

        return level == null
                ? null
                : level.getPrice();
    }

    public Long getBestAskPrice() {
        PriceLevel level = getBestAskLevel();

        return level == null
                ? null
                : level.getPrice();
    }

    public boolean cancelOrder(String orderId) {
        validateOrderId(orderId);

        Order order = ordersById.get(orderId);

        if (order == null) {
            return false;
        }

        order.cancel();
        removeOrderFromBook(order);

        return true;
    }

    public void removeCompletedOrder(Order order) {
        Objects.requireNonNull(
                order,
                "Order must not be null"
        );

        if (order.getStatus() != OrderStatus.FILLED
                && order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Only filled or cancelled orders can be removed"
            );
        }

        Order storedOrder = ordersById.get(order.getId());

        if (storedOrder != order) {
            throw new IllegalArgumentException(
                    "Order is not present in this order book"
            );
        }

        removeOrderFromBook(order);
    }

    public int getOrderCount() {
        return ordersById.size();
    }

    public int getBidLevelCount() {
        removeEmptyLevels(bids);
        return bids.size();
    }

    public int getAskLevelCount() {
        removeEmptyLevels(asks);
        return asks.size();
    }

    public boolean isEmpty() {
        return ordersById.isEmpty();
    }

    public String getSymbol() {
        return symbol;
    }

    private PriceLevel getBestLevel(
            NavigableMap<Long, PriceLevel> levels
    ) {
        while (!levels.isEmpty()) {
            Map.Entry<Long, PriceLevel> bestEntry =
                    levels.firstEntry();

            PriceLevel priceLevel = bestEntry.getValue();

            if (!priceLevel.isEmpty()) {
                return priceLevel;
            }

            levels.pollFirstEntry();
        }

        return null;
    }

    private void removeOrderFromBook(Order order) {
        NavigableMap<Long, PriceLevel> levels =
                getLevelsForSide(order.getSide());

        PriceLevel priceLevel =
                levels.get(order.getPrice());

        if (priceLevel != null) {
            priceLevel.removeOrder(order);

            if (priceLevel.isEmpty()) {
                levels.remove(order.getPrice());
            }
        }

        ordersById.remove(order.getId());
    }

    private void removeEmptyLevels(
            NavigableMap<Long, PriceLevel> levels
    ) {
        levels.entrySet().removeIf(
                entry -> entry.getValue().isEmpty()
        );
    }

    private NavigableMap<Long, PriceLevel> getLevelsForSide(
            Side side
    ) {
        Objects.requireNonNull(
                side,
                "Side must not be null"
        );

        return side == Side.BUY
                ? bids
                : asks;
    }

    private void validateOrderForBook(Order order) {
        if (!symbol.equals(order.getSymbol())) {
            throw new IllegalArgumentException(
                    "Order symbol does not match order book symbol"
            );
        }

        if (order.getType() != OrderType.LIMIT) {
            throw new IllegalArgumentException(
                    "Only limit orders can rest in the order book"
            );
        }

        if (order.getStatus() == OrderStatus.FILLED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Completed orders cannot be added to the order book"
            );
        }

        if (ordersById.containsKey(order.getId())) {
            throw new IllegalArgumentException(
                    "An order with this ID already exists"
            );
        }
    }

    private void validateOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Order ID must not be null or blank"
            );
        }
    }
}