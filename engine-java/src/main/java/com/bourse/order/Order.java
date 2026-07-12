package com.bourse.order;
import java.time.Instant;
import java.util.Objects; //for timestamp support and null validation
import java.util.Locale;

public class Order {

    private final String id;
    private final String symbol; //This is a unique symbol identifier
    private final Side side;
    private final OrderType type;
    private final long price;
    private final long quantity;
    private long remainingQuantity;
    private final Instant timestamp;
    private OrderStatus status;



//We define a constructor named Order

public Order(
    String id,
    String symbol,
    Side side,
    OrderType type,
    long price,
    long quantity

){
    //Id and symbol are essential
        if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("Order ID is required");
    }

    if (symbol == null || symbol.isBlank()) {
        throw new IllegalArgumentException("Symbol is required");
    }
    if (quantity <= 0) {
        throw new IllegalArgumentException("Quantity must be positive");
    }

    Objects.requireNonNull(side, "Side is required");
    Objects.requireNonNull(type, "Order type is required");

    //Limit orders req a price, because Limit order means `Buy at this price or less`, `Sell at this price or higher.`
    if (type == OrderType.LIMIT && price <= 0) {
        throw new IllegalArgumentException(
                "Limit order price must be positive"
        );
    }
    if (type == OrderType.MARKET && price != 0) {
        throw new IllegalArgumentException(
                "Market order price must be zero"
        );
    }

        this.id = id;
        this.symbol = symbol.trim().toUpperCase(Locale.ROOT);
        this.side = side;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.remainingQuantity = quantity;
        this.timestamp = Instant.now();
        this.status = OrderStatus.NEW;
    }

    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public Side getSide() {
        return side;
    }

    public OrderType getType() {
        return type;
    }

    public long getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getRemainingQuantity() {
        return remainingQuantity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void fill(long fillQuantity) {
        if (fillQuantity <= 0) {
            throw new IllegalArgumentException(
                    "Fill quantity must be greater than zero"
            );
        }

        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cancelled order cannot be filled"
            );
        }

        if (status == OrderStatus.FILLED) {
            throw new IllegalStateException(
                    "Filled order cannot be filled again"
            );
        }

        if (fillQuantity > remainingQuantity) {
            throw new IllegalArgumentException(
                    "Fill quantity cannot exceed remaining quantity"
            );
        }

        remainingQuantity -= fillQuantity;

        if (remainingQuantity == 0) {
            status = OrderStatus.FILLED;
        } else {
            status = OrderStatus.PARTIALLY_FILLED;
        }
    }

    public void cancel() {
        if (status == OrderStatus.FILLED) {
            throw new IllegalStateException(
                    "Filled order cannot be cancelled"
            );
        }

        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Order is already cancelled"
            );
        }

        status = OrderStatus.CANCELLED;
    }

    public boolean isFilled() {
        return status == OrderStatus.FILLED;
    }
}

