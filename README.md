# Bourse Engine

Bourse Engine is a Java-based trading and order-matching engine.

Its core responsibility is to accept buy and sell orders, maintain an order book, and produce trades using strict **price-time priority**—the same fundamental rule used by electronic exchanges.

The matching engine is being developed as a standalone library with:

* No application framework
* No database dependency
* No user-interface dependency
* No network or file I/O inside the core engine

This keeps the matching logic deterministic, testable, and suitable for benchmarking.

## Current Status

The following components are complete:

* Order-side representation using `BUY` and `SELL`
* Limit and market order types
* Order lifecycle tracking
* Partial and complete order fills
* Order cancellation
* Trade representation
* Validation for orders and trades
* Unit tests for `Order` and `Trade`

The order book and matching logic will be implemented next.

## Project Structure

```text
bourse-engine/
├── README.md
├── PLAN.md
├── CLAUDE.md
├── .gitignore
│
├── engine-java/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   └── java/com/bourse/
│       │       ├── order/
│       │       │   ├── Side.java
│       │       │   ├── OrderType.java
│       │       │   ├── OrderStatus.java
│       │       │   └── Order.java
│       │       ├── trade/
│       │       │   └── Trade.java
│       │       ├── book/
│       │       │   ├── PriceLevel.java
│       │       │   └── LimitOrderBook.java
│       │       └── engine/
│       │           └── MatchingEngine.java
│       │
│       └── test/
│           └── java/com/bourse/
│               ├── order/
│               │   └── OrderTest.java
│               ├── trade/
│               │   └── TradeTest.java
│               ├── book/
│               │   └── LimitOrderBookTest.java
│               └── engine/
│                   └── MatchingEngineTest.java
│
└── web/
```

The `web` module is reserved for a future React and TypeScript interface.

## Core Concepts

### Order

An `Order` represents a trader’s request to buy or sell an instrument.

It stores:

* A unique order ID
* The instrument symbol
* The buy or sell side
* The order type
* The price
* The original quantity
* The remaining quantity
* The creation timestamp
* The current status

Example:

```text
Order ID: ORD-1
Symbol: AAPL
Side: BUY
Type: LIMIT
Price: 15000
Quantity: 100
Remaining quantity: 100
Status: NEW
```

Most order fields are immutable after creation. Only the remaining quantity and status change as the order is filled or cancelled.

### Trade

A `Trade` represents an executed match between one buy order and one sell order.

It stores:

* A unique trade ID
* The instrument symbol
* The matched buy-order ID
* The matched sell-order ID
* The execution price
* The executed quantity
* The execution timestamp

An order describes what a trader requested. A trade records what was actually executed.

```text
Buy Order + Sell Order → Trade
```

The relationship is maintained through `buyOrderId` and `sellOrderId`, which reference the IDs of the two matched orders.

## Order Enums

### `Side`

```java
public enum Side {
    BUY,
    SELL
}
```

`Side` determines whether the trader wants to buy or sell.

Using an enum prevents invalid values and spelling mistakes that could occur with strings.

### `OrderType`

```java
public enum OrderType {
    LIMIT,
    MARKET
}
```

A limit order specifies an acceptable price.

* A limit buy executes at its limit price or lower.
* A limit sell executes at its limit price or higher.

A market order does not specify an execution price and attempts to trade against the best available prices.

In the current model:

* Limit-order prices must be positive.
* Market-order prices must be zero.

### `OrderStatus`

```java
public enum OrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED
}
```

An order moves through a defined lifecycle:

```text
NEW
 ├── PARTIALLY_FILLED
 │    ├── FILLED
 │    └── CANCELLED
 ├── FILLED
 └── CANCELLED
```

A filled or cancelled order cannot be filled again.

## Order Behaviour

### Filling an Order

The `fill(long fillQuantity)` method reduces the remaining quantity after a trade.

```java
order.fill(40);
```

For an order with an original quantity of `100`:

```text
Remaining: 100 → 60
Status: NEW → PARTIALLY_FILLED
```

When the remaining quantity reaches zero:

```text
Remaining: 0
Status: FILLED
```

The method rejects:

* Zero or negative fill quantities
* Fills larger than the remaining quantity
* Fills on cancelled orders
* Additional fills on completed orders

### Cancelling an Order

The `cancel()` method changes an active order’s status to `CANCELLED`.

Filled orders cannot be cancelled, and an already cancelled order cannot be cancelled again.

### Checking Completion

The `isFilled()` method returns `true` only when the order has the `FILLED` status.

## Why Getter Methods Are Used

The fields inside `Order` and `Trade` are private.

Getter methods allow other engine components to read those values without allowing them to modify the objects directly.

For example:

```java
if (order.getSide() == Side.BUY) {
    // Process the order as a bid
}
```

Order getters will be used by:

* `PriceLevel`
* `LimitOrderBook`
* `MatchingEngine`
* Unit tests
* A future API or user interface

Trade getters will be used for:

* Returning matching results
* Logging executions
* Testing trade values
* Displaying trade history

## Timestamp Handling

Order and trade timestamps are generated internally:

```java
this.timestamp = Instant.now();
```

This means callers do not need to provide timestamps manually.

Generating the timestamp inside the model ensures that every new object records its creation time automatically.

A future version may accept timestamps through a controlled clock abstraction to support deterministic testing, historical replay, and event simulation.

## Price Representation

Prices are stored using `long`, not `double`.

For example:

```text
15025 = $150.25
```

when one unit represents one cent.

Integer price representation avoids floating-point precision problems and allows prices to be compared exactly.

A production engine would normally define a tick size for each instrument.

## Planned Matching Model

Each instrument will have its own `LimitOrderBook`.

The book will contain:

* Bids ordered from highest price to lowest price
* Asks ordered from lowest price to highest price
* FIFO order queues at each price level
* A lookup map for locating orders by ID

Orders will follow price-time priority:

1. The best available price is matched first.
2. At the same price, the oldest order is matched first.
3. A trade executes at the price of the resting order.
4. Unmatched limit-order quantity remains in the book.
5. Unmatched market-order quantity is discarded.

## Building and Testing

Requirements:

* Java 17 or newer
* Maven 3.8 or newer

From the `engine-java` directory:

```bash
mvn test
```

To compile without running tests:

```bash
mvn compile
```

## Development Roadmap

The next implementation stages are:

1. Build `PriceLevel` using a FIFO `Deque<Order>`.
2. Build `LimitOrderBook` using ordered bid and ask maps.
3. Implement price-time-priority matching.
4. Implement order lookup and cancellation.
5. Build `MatchingEngine` to manage one book per symbol.
6. Add matching, cancellation, and price-priority tests.
7. Add performance benchmarks.
8. Add a web interface and API layer.

## Design Goals

Bourse Engine prioritizes:

* Correctness
* Deterministic behaviour
* Clear domain modelling
* Explicit validation
* Price-time priority
* Exact integer arithmetic
* High unit-test coverage
* Separation between domain logic and infrastructure

## Disclaimer

This project is for educational and portfolio purposes.

It is not intended for real-money trading or production exchange operation.
