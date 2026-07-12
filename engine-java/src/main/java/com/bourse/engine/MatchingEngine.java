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
}