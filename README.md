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
- **Saga Orchestration & Async Anti-Fraud:** Event-driven transaction scoring with automated state machine (`PENDING` ➔ `FRAUD_APPROVED` / `RAUD_REJECTED` ➔ `COMPLETED` / `FAILED`).
- **Idempotency & Race Condition Protection:** Account balance updates guarded with database constraints and optimistic state management.
- **Resilience & Fault Tolerance:** Handled edge-cases (insufficient funds, circuit breaking, downstream service unavailability).

## 🧪 End-to-End Verification & Business Scenarios

All business scenarios can be executed instantly via the included `test-requests.http` suite.

### Scenario 1: Happy Path Payment Flow (`COMPLETED`)

1.  **Create Accounts:** Created Sender (`Alice`) and Receiver (`Bob`) with USD currency.
2.  **Deposit:** Added `$5,000.00` to Alice's balance.
3.  **Transfer:** Executed `$1,500.00` payment from Alice to Bob.
4.  **Lifecycle:**
    - `payment-service` saved payment with `PENDING` status.
    - Outbox scheduler published event to `payment-created-events`.
    - `anti-fraud-service` evaluated risk and published `approved=true`.
    - `payment-service` performed money movement via `account-service`.
    - Final status transitioned to `COMPLETED`.
    - Resulting balances: Alice = `$3,500.00`, Bob = `$1,500.00`.
  
### Scenario 2: Anti-Fraud Single Limit Violation (`FRAUD_REJECTED`)

1. **Transfer:** Attempted to transfer `$150,000.00` (exceeding maximum single limit threshold of `$100,000.00`).
2. **Lifecycle:**
    - `anti-fraud-service` intercepted event and calculated rule violation.
    - Published verdict with `approved=false` and reason: "Transaction amount exceeds single limit of 100000.00".
    - Final status transitioned to `FRAUD_REJECTED`.
    - No balances were modified.

### Scenario 3: Insufficient Balance Edge-Case (`FAILED`)

1. **Transfer:** Attempted to transfer `$500.00` from account with `$0.00` balance.
2. **Lifecycle:**
    - Anti-Fraud check passed (`approved=true`).
    - `payment-service` attempted debit call to `account-service`.
    - `account-service` returned `400 Bad Request` (`Insufficient Funds`).
    - Final status transitioned to `FAILED` with audit message recorded in `failReason`.

## 🛠️ Tech Stack & Infrastructure

- **Backend:** Java 21, Spring Boot 3.3.x (Web, Data JPA, Cloud OpenFeign, Actuator)
- **Messaging:** Apache Kafka 7.x, Spring Kafka, Kafka UI
- **Databases:** PostgreSQL 16 (Dedicated DB per microservice: `account_db`, `antifraud_db`, `payment_db`)
- **Database Migrations:** Flyway
- **Tooling & Build:** Maven Multi-Module, Docker & Docker Compose

## 🐳 Deployment & Container Orchestration

### 1. One-Click Launch via Docker Compose

To build and spin up the entire distributed ecosystem (PostgreSQL, Kafka, Kafka UI, and all 3 microservices) in isolated networks:

```bash
docker compose -f deploy/docker-compose/docker-compose.yml up --build -d
```

### Services Map:
- Account Service: `http://localhost:8081`
- Anti-Fraud Service: `http://localhost:8082`
- Payment Service: `http://localhost:8083`
- Kafka UI Web Dashboard: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

### 2. Kubernetes (K8s) Cluster Deployment

To deploy to a Kubernetes cluster (Minikube / Kind / EKS / GKE):

```bash
# 1. Apply Namespace, Configs and Secrets
kubectl apply -f deploy/k8s/00-namespace.yaml
kubectl apply -f deploy/k8s/01-config-and-secrets.yaml

# 2. Deploy Microservices with Health Probes & Resource Limits
kubectl apply -f deploy/k8s/02-account-service.yaml
kubectl apply -f deploy/k8s/03-anti-fraud-service.yaml
kubectl apply -f deploy/k8s/04-payment-service.yaml

# 3. Verify Pods Status
kubectl get pods -n fintech-platform
```
