# 💳 Fintech High-Load Distributed Payment & Anti-Fraud Platform

Production-ready distributed banking and payment orchestration platform built on **Spring Boot 3, Java 21, Apache Kafka, PostgreSQL, and Flyway**, demonstrating enterprise architectural patterns (**Saga Choreography, Transactional Outbox, Idempotency, and Multi-Stage Containerization**).

---

## 🏛️ System Architecture & Event-Driven Flow

```text
       [ REST Client / Frontend ]
                   │
                   │ POST /api/v1/payments
                   ▼
       ┌───────────────────────┐
       │    payment-service    │ ───► [ PostgreSQL: payments (PENDING) ]
       │      (Port: 8083)     │ ───► [ PostgreSQL: outbox_events (PENDING) ]
       └───────────┬───────────┘
                   │
                   │  Outbox Poller (At-Least-Once Delivery)
                   ▼
     ┌───────────────────────────┐
     │ Topic: payment-created    │
     └─────────────┬─────────────┘
                   │
                   ▼
       ┌───────────────────────┐
       │  anti-fraud-service   │ ───► Evaluates Velocity & Blacklist Rules
       │      (Port: 8082)     │ ───► Persists Audit Log in antifraud_db
       └───────────┬───────────┘
                   │
                   │  Publish Verdict
                   ▼
     ┌───────────────────────────┐
     │  Topic: fraud-verdict     │
     └─────────────┬─────────────┘
                   │
                   ▼
       ┌───────────────────────┐
       │    payment-service    │
       └───────────┬───────────┘
                   │
                   │  OpenFeign (Synchronous ACID Money Transfer)
                   ▼
       ┌───────────────────────┐
       │    account-service    │ ───► Debit Sender & Credit Receiver
       │      (Port: 8081)     │      (Optimistic Locking on Balance)
       └───────────────────────┘

```

## 🚀 Key Architectural Highlights
- **Transactional Outbox Pattern:** Eliminates Dual-Write discrepancies between PostgreSQL and Kafka by storing outbox events in the same ACID database transaction.
- **Saga Orchestration & Async Anti-Fraud:** Event-driven transaction scoring with automated state machine (<span style="background-color: #333333;">PENDING</span> ➔ <span style="background-color: #333333;">FRAUD_APPROVED</span> / <span style="background-color: #333333;">FRAUD_REJECTED</span> ➔ <span style="background-color: #333333;">COMPLETED</span> / <span style="background-color: #333333;">FAILED</span>).
- **Idempotency & Race Condition Protection:** Account balance updates guarded with database constraints and optimistic state management.
- **Resilience & Fault Tolerance:** Handled edge-cases (insufficient funds, circuit breaking, downstream service unavailability).

## 🧪 End-to-End Verification & Business Scenarios

All business scenarios can be executed instantly via the included <span style="background-color: #333333;">test-requests.http</span> suite.

### Scenario 1: Happy Path Payment Flow (<span style="background-color: #333333;">COMPLETED</span>)

1.  **Create Accounts:** Created Sender (<span style="background-color: #333333;">Alice</span>) and Receiver (<span style="background-color: #333333;">Bob</span>) with USD currency.
2.  **Deposit:** Added <span style="background-color: #333333;">$5,000.00</span> to Alice's balance.
3.  **Transfer:** Executed <span style="background-color: #333333;">$1,500.00</span> payment from Alice to Bob.
4.  **Lifecycle:**
    - <span style="background-color: #333333;">payment-service</span> saved payment with <span style="background-color: #333333;">PENDING</span> status.
    - Outbox scheduler published event to <span style="background-color: #333333;">payment-created-events</span>.
    - <span style="background-color: #333333;">anti-fraud-service</span> evaluated risk and published <span style="background-color: #333333;">approved=true</span>.
    - <span style="background-color: #333333;">payment-service</span> performed money movement via <span style="background-color: #333333;">account-service</span>.
    - Final status transitioned to <span style="background-color: #333333;">COMPLETED</span>.
    - Resulting balances: Alice = <span style="background-color: #333333;">$3,500.00</span>, Bob = <span style="background-color: #333333;">$1,500.00</span>.
  
### Scenario 2: Anti-Fraud Single Limit Violation (<span style="background-color: #333333;">FRAUD_REJECTED</span>)

1. **Transfer:** Attempted to transfer <span style="background-color: #333333;">$150,000.00</span> (exceeding maximum single limit threshold of <span style="background-color: #333333;">$100,000.00</span>).
2. **Lifecycle:**
    - <span style="background-color: #333333;">anti-fraud-service</span> intercepted event and calculated rule violation.
    - Published verdict with <span style="background-color: #333333;">approved=false</span> and reason: "Transaction amount exceeds single limit of 100000.00".
    - Final status transitioned to <span style="background-color: #333333;">FRAUD_REJECTED</span>.
    - No balances were modified.

### Scenario 3: Insufficient Balance Edge-Case (<span style="background-color: #333333;">FAILED</span>)

1. **Transfer:** Attempted to transfer <span style="background-color: #333333;">$500.00</span> from account with <span style="background-color: #333333;">$0.00</span> balance.
2. **Lifecycle:**
    - Anti-Fraud check passed (<span style="background-color: #333333;">approved=true</span>).
    - <span style="background-color: #333333;">payment-service</span> attempted debit call to <span style="background-color: #333333;">account-service</span>.
    - <span style="background-color: #333333;">account-service</span> returned <span style="background-color: #333333;">400 Bad Request</span> (<span style="background-color: #333333;">Insufficient Funds</span>).
    - Final status transitioned to <span style="background-color: #333333;">FAILED</span> with audit message recorded in <span style="background-color: #333333;">failReason</span>.

## 🛠️ Tech Stack & Infrastructure

- **Backend:** Java 21, Spring Boot 3.3.x (Web, Data JPA, Cloud OpenFeign, Actuator)
- **Messaging:** Apache Kafka 7.x, Spring Kafka, Kafka UI
- **Databases:** PostgreSQL 16 (Dedicated DB per microservice: <span style="background-color: #333333;">account_db</span>, <span style="background-color: #333333;">antifraud_db</span>, <span style="background-color: #333333;">payment_db</span>)
- **Database Migrations:** Flyway
- **Tooling & Build:** Maven Multi-Module, Docker & Docker Compose