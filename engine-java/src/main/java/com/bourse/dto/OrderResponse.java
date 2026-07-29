package com.bourse.dto;
import com.bourse.order.Order;
import com.bourse.order.Side;
import com.bourse.order.OrderType;
import com.bourse.order.OrderStatus;

public record OrderResponse (
        String order,
        String symbol,
        Side side,
        OrderType type,
        long price,
        long quantity,
        long remainingQuantity,
        OrderStatus status

){
     public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getSymbol(),
                order.getSide(),
                order.getType(),
                order.getPrice(),
                order.getQuantity(),
                order.getRemainingQuantity(),
                order.getStatus()
        );
    }
}
