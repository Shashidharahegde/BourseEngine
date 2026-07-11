# Bourse Engine

A limit order-book matching engine — the component at the centre of every exchange, responsible for pairing buy and sell orders under strict price-time priority.

Written in Java. No framework, no database, no I/O in the core: the matching engine is a pure library that can be reasoned about, tested, and benchmarked in isolation.

---

## Status

> **In active development.** The domain model and book structure are in place; the matching loop is being implemented. This section is kept honest — see the roadmap below for exactly what is and isn't built.

| Component | Status |
|---|---|
| Domain model (`Order`, `Trade`, enums) | ✅ Built |
| `PriceLevel` — FIFO queue + aggregate volume | ✅ Built |
| `OrderBook` — two-sided, price-ordered | ✅ Built |
| `MatchingEngine` — limit matching, partial fills | 🚧 In progress |
| Cancellation (O(1)) | 🚧 In progress |
| Market orders | ⬜ Planned |
| Property-based invariant tests | ⬜ Planned |
| REST API + WebSocket L2 feed | ⬜ Planned |
| Event-sourced recovery (replay + snapshots) | ⬜ Planned |
| React order-book UI | ⬜ Planned |
| AWS deployment (CDK, ECS/Fargate) | ⬜ Planned |
| Latency benchmarks (p50/p99) | ⬜ Planned |

---

## The problem

An exchange receives a continuous stream of orders for an instrument. Buyers post **bids**; sellers post **asks**. When the highest bid meets the lowest ask, the **spread** is crossed and a trade executes.

The engine's job is to maintain the book of all resting (unfilled) orders and, for every incoming order, decide instantly what it matches against — under two rules:

1. **Price priority** — the best price is matched first. An incoming buy takes the *lowest* ask; an incoming sell hits the *highest* bid.
2. **Time priority** — among orders at the *same* price, the one that arrived earliest is filled first (FIFO).

A trade executes at the **resting order's price**, not the incoming order's. An order that cannot be fully matched rests in the book with its remaining quantity.

This is deceptively hard to do well. Every operation is on the hot path, and the three operations have conflicting structural demands:

- **Add** — must find or create the correct price level, in order.
- **Cancel** — must remove an arbitrary order from anywhere in the book. In real markets *cancellations vastly outnumber executions*, so cancel performance matters more than match performance.
- **Match** — must repeatedly access the best price level and walk it in arrival order.

No single data structure serves all three well. The design below composes three.

---

## Design decisions

### The book: an ordered map of price levels, each holding a FIFO queue

```
OrderBook
├── bids : TreeMap<Long, PriceLevel>   (descending — firstEntry() = best bid)
├── asks : TreeMap<Long, PriceLevel>   (ascending  — firstEntry() = best ask)
└── byId : HashMap<String, Order>      (O(1) cancel)

PriceLevel
├── orders : Deque<Order>   (FIFO — enforces time priority for free)
└── totalVolume : long      (maintained on add/remove — O(1) depth queries)
```

| Operation | Complexity |
|---|---|
| Add order | O(log M) to find/create the level, O(1) to append within it |
| Cancel order | O(1) lookup via `byId`, O(1) unlink from the deque |
| Best bid / best ask | O(1) — `firstEntry()` |
| Match | O(1) to reach the best level, then linear in orders *consumed* |

*(M = number of distinct price levels, typically far smaller than N, the number of orders.)*

### Why a `TreeMap` (red-black tree) and not a heap

A heap is the more common textbook answer — a min-heap of asks, a max-heap of bids — and it gives O(1) access to the single best price. It was rejected for two reasons:

1. **Mid-structure removal.** A binary heap cannot locate an arbitrary element. Cancelling an order that empties a price level requires removing that level from the *middle* of the heap, which needs an auxiliary map tracking each element's heap index — and that index changes on every sift. It's fragile bookkeeping on the hottest path in the system.
2. **Ordered traversal.** A heap exposes only the *best* price cheaply. Publishing L2 market data (aggregated depth across the top N levels) requires walking levels in price order — natural in a tree, awkward in a heap.

A `TreeMap` gives O(log M) insert and removal *by key*, with no side-index to maintain, plus ordered iteration. The tradeoff — O(log M) vs O(1) best-price access — is not the bottleneck at this scale.

### Why prices are `long`, never `double`

Prices are stored as integers in **minor units** (paise/cents). Floating-point cannot represent decimal money exactly, so comparisons drift and arithmetic accumulates error. In a matching engine, `bid >= ask` being wrong by `1e-15` is a correctness bug that silently produces or suppresses trades. Integer arithmetic is exact.

### Why the matching core is single-threaded

The engine is a **single-writer, deterministic core**. Concurrency lives at the *edges*: many clients feed orders into one sequenced ingress stream, and a separate fan-out publishes the outbound trade feed. The matching loop itself processes that stream one command at a time.

This is deliberate, not a limitation:

- **Determinism.** The same input sequence always produces the same trades. No wall-clock reads, no randomness inside the core — timestamps arrive *on* the order, they are not generated during matching. This makes the engine replayable and exhaustively testable.
- **Latency.** Two threads mutating one book require locks on the hot path, and lock contention costs more than the parallelism gains.

This mirrors the architecture used by production exchanges — most publicly, LMAX, whose engine runs business logic in-memory on a single thread surrounded by an event-sourced ring buffer.

### Recovery via event sourcing *(planned)*

The book will not be persisted. Instead, the **append-only stream of commands** (new order, cancel) is the source of truth, and the book is a derived projection rebuilt by replaying that log — with periodic snapshots so replay does not start from zero.

This is what makes the determinism above valuable: given the same log, replay reconstructs a byte-identical book. It gives crash recovery, an immutable audit trail, and replay-based testing from a single mechanism.

---

## Layout

```
bourse-engine/
├── engine-java/
│   └── src/main/java/com/bourse/
│       ├── order/     Order, Side, OrderType, OrderStatus
│       ├── trade/     Trade
│       ├── book/      OrderBook, PriceLevel
│       └── engine/    MatchingEngine   ← the single-writer core
└── web/               React + TypeScript order-book UI (planned)
```

The engine has a two-method public surface:

```java
List<Trade> submit(Order order);
void        cancel(String orderId);
```

Everything else is internal. The core has no knowledge of what is calling it — REST, WebSocket, or a test harness.

---

## Running

```bash
cd engine-java
mvn test
```

---

## Roadmap

**Phase 1 — Core engine.** Limit matching, partial fills, price-time priority, cancellation, market orders. Property-based invariant tests: the book never crosses, quantity is always conserved, time priority is never violated. Golden-master replay tests for determinism.

**Phase 2 — Service.** Sequenced ingress with monotonic sequence numbers, idempotent order IDs, REST endpoints, WebSocket L2 depth feed with gap detection.

**Phase 3 — Durability.** Event-sourced command log, snapshots, replay-to-rebuild on startup.

**Phase 4 — Interface.** Live order book, trade tape, depth chart.

**Phase 5 — Infrastructure.** Docker, AWS via CDK (ECS/Fargate for the stateful core — deliberately *not* Lambda), CloudWatch latency dashboards, CI.

**Phase 6 — Performance.** Load generator, p50/p99 match latency, and an array-indexed price-ladder book benchmarked against the `TreeMap` implementation.

---

## References

- *How to Build a Fast Limit Order Book* — the canonical writeup on the tree-of-levels + linked-list + hashmap structure.
- Fowler, M. — *The LMAX Architecture* (2011).
- Thompson, Farley, Barker, Gee & Stewart — *Disruptor: High performance alternative to bounded queues for exchanging data between concurrent threads* (LMAX, 2011).