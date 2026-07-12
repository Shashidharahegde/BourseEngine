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

//

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


    private String generateTradeId() {
        String tradeId =
                "TRD-" + nextTradeSequence;

        nextTradeSequence++;

        return tradeId;
    }

}