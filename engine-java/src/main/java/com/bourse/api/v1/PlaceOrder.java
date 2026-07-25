package com.bourse.api.v1;
import java.util.List;

import com.bourse.engine.MatchingEngine;
import com.bourse.order.OrderType;
import com.bourse.order.Side;
import com.bourse.trade.Trade;

import io.javalin.Javalin;

import com.bourse.dto.OrderRequest;
class PlaceOrder{

        public static void main(String[] args) {

            MatchingEngine matchingEngine = new MatchingEngine();
            
             Javalin.create(config -> {
                config.routes.post("/api/v1/placeOrder", ctx -> {
                        OrderRequest request = ctx.bodyAsClass(OrderRequest.class);

                        List<Trade> trades = matchingEngine.submitOrder(
                        request.symbol(),
                        Side.valueOf(request.side()),
                        OrderType.valueOf(request.type()),
                        request.price(),
                        request.quantity()
                    );

                    ctx.status(200);
                    ctx.json(trades);

                    }) ;

            }).start(7070);
        };
            
}


