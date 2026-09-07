# Database Design & Persistence Architecture

## 1. Overview
The **Autonomous AI Research Assistant** implements a resilient dual-tier persistence layer. It provides enterprise-grade persistence via an official **MongoDB Synchronous Java Driver** implementation, coupled with an automatic, zero-dependency **Local File Repository** fallback to ensure that the system never suffers from a single point of failure.

---

## 2. MongoDB Schema Specification

### 2.1. `documents` Collection
Stores ingested and pre-processed research literature.
```json
{
  "_id": ObjectId("66ddf8a12b3c4d5e6f7a8b9c"),
  "docId": "DOC-001",
  "title": "Machine Learning Applications in Modern Cybersecurity and Threat Intelligence",
  "source": "Journal of Cybersecurity and Applied AI, Vol. 14, 2024",
  "content": "Machine learning has emerged as a cornerstone in modern cybersecurity defense architectures...",
  "createdAt": NumberLong(1725732400000)
}
```
**Indexes**:
- `{ docId: 1 }` (Unique: `true`) — Ensures $O(1)$ lookup and prevents document duplicates.
- `{ createdAt: -1 }` — Enables time-series sorting and chronological auditing.

### 2.2. `queries` Collection
Maintains audit logs of search queries, retrieved document IDs, and execution latency.
```json
{
  "_id": ObjectId("66ddf8b22b3c4d5e6f7a8b9d"),
  "queryId": "Q-1725732410500",
  "queryText": "machine learning cybersecurity",
  "latencyMs": NumberLong(27),
  "timestamp": NumberLong(1725732410500)
}
```
**Indexes**:
- `{ queryId: 1 }` (Unique: `true`)
- `{ timestamp: -1 }`

### 2.3. `research_sessions` Collection
Stores complete traces of autonomous research sessions, including decomposed sub-queries, evidence passages, citation mappings, and synthesized briefs.
```json
{
  "_id": ObjectId("66ddf8c32b3c4d5e6f7a8b9e"),
  "sessionId": "SESSION-1725732420000",
  "question": "What are the applications of machine learning in cybersecurity?",
  "summary": "Machine learning has emerged as a cornerstone in modern cybersecurity...",
  "timestamp": NumberLong(1725732420000)
}
```
**Indexes**:
- `{ sessionId: 1 }` (Unique: `true`)
- `{ timestamp: -1 }`

---

## 3. Local File Repository Fallback (`FileRepository`)
When MongoDB is unavailable, network connections time out, or credentials are not supplied, the system activates `FileRepository`.

### Directory Structure:
```text
data/
└── storage/
    ├── documents/
    │   ├── DOC-001.json
    │   ├── DOC-002.json
    │   └── ...
    ├── queries/
    │   └── q_1725732410500.txt
    └── sessions/
        └── SESSION-1725732420000.txt
```

### Key Properties:
- **Zero-Dependency**: Uses pure `java.nio.file` standard libraries.
- **Atomic Operations**: Safe concurrent read/write via synchronization.
- **Fail-Safe Operation**: Completely decoupled from external network availability, ensuring the application always functions cleanly during evaluations, offline demonstrations, and air-gapped deployments.

---

## 4. Security & Connection Protection
- **No Hardcoded URIs**: Connection URIs are read strictly from `MONGODB_URI` environment variable.
- **Strict 2.5s Timeout**: Configured with `serverSelectionTimeout(2500, TimeUnit.MILLISECONDS)` so that if a database host is unreachable, the system instantly logs a sanitized warning and fails over to local storage without hanging.
- **Credential Masking**: All connection strings in logs, exceptions, and diagnostics are masked via regex:
  `mongodb+srv://<user>:<pass>@...` $\to$ `mongodb+srv://***:***@...`
