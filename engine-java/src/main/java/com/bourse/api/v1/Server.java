package com.bourse.api.v1;

import com.bourse.engine.MatchingEngine;
import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.*;

public class Server {

    public static void main(String[] args) {

        MatchingEngine matchingEngine = new MatchingEngine();

        Javalin.create(config -> {
            config.routes.apiBuilder(() -> {

                post(
                    "/api/v1/placeOrder",
                    PlaceOrder.handler(matchingEngine)
                );

                delete(
                    "/api/v1/orders/{symbol}/{orderId}",
                    CancelOrder.handler(matchingEngine)
                );

            });
        }).start(7070);

        System.out.println("Server running on http://localhost:7070");
    }
}