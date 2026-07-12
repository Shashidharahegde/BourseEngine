/*

The purpose of PriceLevel is to return the oldest sell order.

After MatchinEngine selects lowest sell brice i.e, best ask,
It retrives the PriceLevel (this) for that price

PriceLevel returns oldest sell order using FIFO queue
*/

package com.bourse.book;

import com.bourse.order.Order;
import com.bourse.order.OrderStatus;
import com.bourse.order.OrderType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class PriceLevel {

    private final long price;
    private final Deque<Order> orders;
    private long totalQuantity;

    // We define a constructor PriceLevel that runs when PriceLevel Object is created
    public PriceLevel(long price) {
        if (price <= 0) {
            throw new IllegalArgumentException(
                    "Price must be positive"
            );
        }

        this.price = price;
        this.orders = new ArrayDeque<>();
        this.totalQuantity = 0;
    }

    public void addOrder(Order order) {
        Objects.requireNonNull(
                order,
                "Order must not be null"
        );

        if (order.getType() != OrderType.LIMIT) {
            throw new IllegalArgumentException(
                    "Only limit orders can be added to a price level"
            );
        }

        if (order.getPrice() != price) {
            throw new IllegalArgumentException(
                    "Order price does not match price level"
            );
        }

        if (order.getStatus() == OrderStatus.FILLED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Completed orders cannot be added to a price level"
            );
        }

        orders.addLast(order);
        totalQuantity += order.getRemainingQuantity();
    }

    public Order peekFirstOrder() {
        return orders.peekFirst();
    }


    void fillFirstOrder(long fillQuantity) {
        Order head = orders.peekFirst();

        if (head == null) {
            throw new IllegalStateException(
                    "No orders at this price level"
            );
        }

        if (fillQuantity <= 0) {
            throw new IllegalArgumentException(
                    "Fill quantity must be positive"
            );
        }

        if (fillQuantity > head.getRemainingQuantity()) {
            throw new IllegalArgumentException(
                    "Fill quantity exceeds remaining quantity of head order"
            );
        }

        head.fill(fillQuantity);
        totalQuantity -= fillQuantity;

        if (totalQuantity < 0) {
            throw new IllegalStateException(
                    "PriceLevel totalQuantity went negative"
            );
        }
    }


    public boolean removeOrder(Order order) {
        Objects.requireNonNull(
                order,
                "Order must not be null"
        );

        boolean removed = orders.remove(order);

        if (removed) {
            totalQuantity -= order.getRemainingQuantity();
        }

        if (totalQuantity < 0) {
            throw new IllegalStateException(
                    "PriceLevel totalQuantity went negative"
            );
        }

        return removed;
    }

    public long getPrice() {
        return price;
    }

    public long getTotalQuantity() {
        return totalQuantity;
    }

    public int getOrderCount() {
        return orders.size();
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public List<Order> getOrders() {
        return List.copyOf(orders);
    }
}