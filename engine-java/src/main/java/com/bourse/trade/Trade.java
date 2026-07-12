package com.bourse.trade;

import java.time.Instant;
import java.util.Objects;

public class Trade {

    private final String id;
    private final String symbol;
    private final String buyOrderId; // buyOrderID and sellOrderId refer to the normal id field of each matched Order (from Order.java)
    private final String sellOrderId;
    private final long price;
    private final long quantity;
    private final Instant timestamp;

    public Trade(
            String id,
            String symbol,
            String buyOrderId,
            String sellOrderId,
            long price,
            long quantity
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Trade ID is required");
        }

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }

        if (buyOrderId == null || buyOrderId.isBlank()) {
            throw new IllegalArgumentException("Buy order ID is required");
        }

        if (sellOrderId == null || sellOrderId.isBlank()) {
            throw new IllegalArgumentException("Sell order ID is required");
        }

        if (price <= 0) {
            throw new IllegalArgumentException("Trade price must be positive");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Trade quantity must be positive"
            );
        }

        if (Objects.equals(buyOrderId, sellOrderId)) {
            throw new IllegalArgumentException(
                    "Buy and sell order IDs must be different"
            );
        }

        this.id = id;
        this.symbol = symbol.trim().toUpperCase();
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getBuyOrderId() {
        return buyOrderId;
    }

    public String getSellOrderId() {
        return sellOrderId;
    }

    public long getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "id='" + id + '\'' +
                ", symbol='" + symbol + '\'' +
                ", buyOrderId='" + buyOrderId + '\'' +
                ", sellOrderId='" + sellOrderId + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", timestamp=" + timestamp +
                '}';
    }
}