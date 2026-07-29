package com.bourse.api.v1;

import java.util.Map;

import com.bourse.engine.MatchingEngine;

import io.javalin.http.Handler;

public class CancelOrder {

    public static Handler handler(MatchingEngine matchingEngine) {

        return ctx -> {

            String symbol = ctx.pathParam("symbol");
            String orderId = ctx.pathParam("orderId");

            boolean isCancelled =
                    matchingEngine.cancelOrder(symbol, orderId);

            if (isCancelled) {
                ctx.status(200);
                ctx.json(Map.of(
                        "message", "Order cancelled"
                ));
            } else {
                ctx.status(404);
                ctx.json(Map.of(
                        "message", "Order not found"
                ));
            }
        };
    }
}