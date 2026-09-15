# Autonomous AI Research Assistant — Comprehensive Project Report

**Course**: Advanced Programming in Java  
**Project Title**: Autonomous AI Research Assistant  
**Language**: Pure Java (Java 17+ / Java 21)  
**Architecture**: Single-Frontend + Single-Backend 
**Interface**: Command-Line Interface (CLI) + Embedded Localhost HTTP Web UI  
**Database**: MongoDB Atlas with Automated Local File Persistence Fallback  
**External Generative AI / LLM APIs**: NONE (100% Local Execution)  

---

## Table of Contents
1. Cover Page & Project Metadata
2. Executive Summary & Introduction
3. Problem Statement & Industry Context
4. Project Objectives & Success Criteria
5. Functional Requirements (FR-1 through FR-9)
6. Non-Functional Requirements (NFR-1 through NFR-5)
7. System Architecture & Design Patterns
8. End-to-End Autonomous Research Workflow
9. UML Diagrams (Use Case, Class, Component, Sequence)
10. Design Rationale & Technical Trade-offs
11. Java 17+ Implementation & OOP Concepts Demonstrated
12. Natural Language Processing (NLP) Algorithms
13. Information Retrieval (IR) & Vector Space Algorithms
14. Autonomous Research Planning & Evidence Orchestration
15. Database & Persistence Design (MongoDB + Local Fallback)
16. Security Engineering & Threat Mitigation (5/5 Standard)
17. Verification & Test Strategy
18. Evaluation Framework & Mathematical Formulations
19. Performance Profiling & Latency Benchmarks
20. Algorithmic Time Complexity Analysis
21. Algorithmic Space Complexity Analysis
22. Actual System Output & Verification Logs
23. Engineering Challenges Encountered & Resolutions
24. Key Academic & Technical Learnings
25. Assumptions & Architectural Limitations
26. Future Enhancements & Scalability Roadmap

---

## 1. Cover Page & Project Metadata
* **Student Project**: Autonomous AI Research Assistant 
* **Author / Developer**: B. Naga Jahnavi
* **Core Technologies**: Java SE 17/21, MongoDB Java Sync Driver 4.11.1, JUnit Jupiter 5.10.2, Apache Maven 3.9.6
* **Domain**: Information Retrieval, Natural Language Processing, Vector Space Models, Autonomous AI Systems

---

## 2. Executive Summary & Introduction
Modern academic literature analysis and enterprise cybersecurity investigations require rapid synthesis across hundreds of unstructured documents. While modern cloud-based Large Language Model (LLM) chatbots have gained widespread popularity, they present fundamental liabilities: synthetic hallucinations, non-existent citations, recurring per-token cloud costs, and high privacy risks when proprietary data is transmitted to third parties.

The **Autonomous AI Research Assistant** is an autonomous, deterministic, and 100% locally executing Information Retrieval (IR) and Natural Language Processing (NLP) system written entirely in high-performance Java. Operating without external AI APIs (no OpenAI, Anthropic, or Gemini), it implements an inverted index, smoothed TF-IDF vector space modeling, sparse cosine similarity ranking, bounded min-heap priority queues, graph-based TextRank extractive summarization, and a deterministic question decomposition orchestrator. The system extracts verbatim supporting evidence from local literature and anchors 100% of its findings to verified, traceable document citations (`[C1]`, `[C2]`).

---

## 3. Problem Statement & Industry Context
Enterprise teams operating in air-gapped environments, defense intelligence, and academic research face strict regulatory constraints (e.g., GDPR, HIPAA, Defense Data Residency) that prohibit transmitting raw documents to cloud endpoints. Furthermore, when generative language models are prompted for citations, they frequently fabricate bibliographic references.

There is a direct need for an on-premise, deterministic IR assistant that:
1. Ingests raw technical literature in multi-format text (.txt, .json).
2. Builds an in-memory sparse inverted index with positional tracking and $L_2$ Euclidean vector norms.
3. Automatically decomposes research inquiries into conceptual facets.
4. Ranks candidate passages using explainable vector space models.
5. Employs PageRank-based extractive summarization to guarantee zero synthetic hallucination.
6. Persists research sessions to MongoDB with immediate local disk failover if the remote database is offline.

---

## 4. Project Objectives & Success Criteria
1. **Autonomous Research Orchestration**: Automatically generate 3–5 targeted sub-queries from a high-level question and synthesize multi-source evidence.
2. **Zero-Hallucination Extractive Evidence**: Extract verbatim supporting passages directly from source text without stochastic word generation.
3. **Traceable Provenance**: Provide a verified Citation Registry linking every extracted claim to document IDs and publication metadata.
4. **Senior-SDE Software Engineering**: Maintain high cohesion and low coupling within the strict constraint of **exactly one frontend Java file** and **exactly one backend Java file**.
5. **Defensible Mathematics**: Implement established formulations including smoothed IDF ($\ln((N+1)/(df+1)) + 1$), sublinear TF ($1 + \ln(tf)$), and sparse cosine dot products.
6. **Robust Dual Persistence**: Integrate official MongoDB synchronous driver with seamless local file fallback and zero secret leakage.

---

## 5. Functional Requirements (FR)
* **FR-1: Corpus Ingestion & Validation**: Ingest plain text (.txt) and JSON (.json) documents with size bounds ($\le 10$ MB) and path validation.
* **FR-2: Local NLP Pipeline**: Execute tokenization, case normalization, punctuation filtering, 150+ stop-word removal, and Porter morphological stemming.
* **FR-3: Inverted Index Generation**: Maintain term postings (`docId`, `termFrequency`, positions), vocabulary sets, and precomputed $L_2$ vector norms.
* **FR-4: Ranked Vector Space Retrieval**: Execute Top-$K$ retrieval using sparse cosine similarity with explainable relevance breakdown.
* **FR-5: Autonomous Research Orchestration**: Decompose complex queries into sub-queries, gather multi-document evidence, deduplicate passages (Jaccard $\le 0.60$), and construct citation briefs.
* **FR-6: TextRank Extractive Summarization**: Construct sentence similarity graphs and compute PageRank random walks to extract top salient sentences.
* **FR-7: Salient Keyword Extraction**: Identify distinguishing keywords using TF-IDF weights and title prominence boosts.
* **FR-8: Dual Persistence & Failover**: Persist documents, query telemetry, and research sessions to MongoDB Atlas; automatically fall back to local disk storage when offline.
* **FR-9: Scientific Evaluation Engine**: Calculate Precision@K, Recall@K, F1@K, MRR, and nDCG@K against labeled ground-truth benchmarks.

---

## 6. Non-Functional Requirements (NFR)
* **NFR-1: Performance & Latency**: Sub-50ms query latency; bounded min-heap $O(M \log K)$ selection; sublinear sparse index scanning.
* **NFR-2: Security & Privacy (5/5 Standard)**: Strict canonical path validation blocking directory traversal (`../`) and null-byte injection; regex-based credential masking for logs.
* **NFR-3: High Reliability & Fault Tolerance**: Zero application crashes if external database connection fails or times out; instant fallback to local JSON repository.
* **NFR-4: Modularity & Code Cleanliness**: Adherence to SOLID principles through encapsulated static nested classes and immutable Java records.
* **NFR-5: Resource Efficiency**: Sparse vector evaluation preventing full-corpus matrix scanning ($O(P)$ vs $O(N \cdot V)$).

---

## 7. System Architecture & Design Patterns
The system is divided into two primary source compilation units:
1. `Frontend.java`: Contains the CLI REPL, command routing, ANSI visual formatting, and embedded localhost HTTP web server with JSON REST APIs (`/api/search`, `/api/research`, `/api/complexity`, `/api/evaluate`, `/api/health`, `/api/stats`).
2. `Backend.java`: Contains all core business logic, NLP preprocessors, Inverted Index, TF-IDF engine, TextRank summarizer, research planner, security manager, and persistence adapters.

### Key Design Patterns:
* **Facade Pattern (`EngineFacade`)**: Exposes a unified API concealing subsystem complexity.
* **Repository Pattern (`Repository`, `MongoRepository`, `FileRepository`)**: Abstracts storage mechanisms, enabling seamless runtime switching between MongoDB and disk.
* **Strategy Pattern (`SimilarityEngine`)**: Isolates mathematical scoring logic from indexing and ranking.
* **Immutable Data Records**: Uses Java records (`Document`, `Posting`, `SearchResult`, `EvidenceSnippet`, `ResearchReport`, `ComplexityProfile`) for thread safety and memory efficiency.

---

## 8. End-to-End Autonomous Research Workflow
```
[User Query] 
     │
     ▼
[SecurityManager.validateQuery]
     │
     ▼
[ResearchPlanner.generateSubQueries] ──► Generates 3-5 Conceptual Sub-Queries
     │
     ▼
[RankingEngine.search] (Iterated over Sub-Queries)
     │   ├── Inverted Index Candidate Selection (O(P))
     │   ├── Sparse Cosine Similarity Calculation
     │   └── Bounded Min-Heap Selection (O(M log K))
     ▼
[EvidenceCollector.extractPassages]
     │
     ▼
[Passage Deduplication] ──► Jaccard Token Overlap Filtering (threshold <= 0.60)
     │
     ▼
[ProvenanceManager.assignCitations] ──► Maps [C1], [C2] to Source Documents
     │
     ▼
[KeywordExtractor.extractCorpusKeywords] ──► TF-IDF + Title Prominence
     │
     ▼
[ResearchReport Assembly] ──► Saved to Repository & Rendered to CLI/Web
```

---

## 9. UML Diagrams
The complete set of UML diagrams is maintained in the `docs/uml/` directory:
* **Use Case Diagram** (`docs/uml/use-case.md`): Actors, use cases (UC1–UC12), and persistence failover boundary.
* **Class Diagram** (`docs/uml/class.md`): Static structure showing `Frontend`, `EngineFacade`, records, and repositories.
* **Component Diagram** (`docs/uml/component.md`): Modular decomposition and subsystem linkages.
* **Sequence Diagrams** (`docs/uml/sequence.md`): End-to-end execution sequences for Search, Autonomous Research, and Complexity Profiling.

---

## 10. Design Rationale & Technical Trade-offs
1. **Two-Source-File Constraint vs Multi-Package Architecture**:
   To comply with the university project submission standard, all frontend components reside in `Frontend.java` and backend components in `Backend.java`. Clean modularity is achieved through encapsulated static classes rather than sprawling package trees.
2. **Sparse Inverted Index vs Dense Matrix Multiplications**:
   Dense vector models allocate $N \times V$ matrices, wasting gigabytes of memory on zero entries. The sparse inverted index only examines documents containing at least one query term, bypassing 50% to 95% of the corpus.
3. **TextRank Extractive Summarization vs Generative Summarization**:
   Generative models cannot guarantee that extracted facts exist in the input. TextRank extracts verbatim sentences, ensuring complete factual fidelity and auditability.

---

## 11. Java 17+ Implementation & OOP Concepts Demonstrated
* **Java Records**: Concise, immutable data carriers with automatic `equals()`, `hashCode()`, and `toString()`.
* **Pattern Matching for `switch`**: Clean command dispatching in `executeCommand()`.
* **NIO.2 Filesystem API**: Robust path resolution, directory streams, and atomic file writes.
* **Java Stream API**: Declarative, functional filtering, mapping, and metric reductions.
* **Encapsulation & Defensive Copying**: Unmodifiable collections returned across module boundaries.

---

## 12. Natural Language Processing (NLP) Algorithms
* **Tokenization**: Alphanumeric regex preserving internal hyphens (`[a-zA-Z0-9]+(?:[-'][a-zA-Z0-9]+)*`).
* **Stop-Word Filtering**: $O(1)$ lookup across 150+ common English syntactic stop words.
* **Porter Morphological Stemmer**: 5-phase algorithmic reduction of word suffixes to morphological roots.
* **Sentence Segmentation**: Boundary detection handling abbreviations (`e.g.`, `i.e.`) and sentence terminators (`.`, `!`, `?`).

---

## 13. Information Retrieval (IR) & Vector Space Algorithms
* **Sublinear Term Frequency**:
  $$w_{t,d} = 1 + \ln(\text{tf}(t, d)) \quad \text{for } \text{tf} > 0$$
* **Smoothed Inverse Document Frequency**:
  $$\text{IDF}(t) = \ln\left(\frac{N + 1}{\text{df}(t) + 1}\right) + 1.0$$
* **Vector $L_2$ Euclidean Norm**:
  $$\|d\|_2 = \sqrt{\sum_{t \in d} w_{t,d}^2}$$
* **Sparse Cosine Similarity**:
  $$\text{Cosine}(q, d) = \frac{\sum_{t \in q \cap d} w_{t,q} \cdot w_{t,d}}{\|q\|_2 \cdot \|d\|_2}$$

---

## 14. Autonomous Research Planning & Evidence Orchestration
* **Question Decomposition**: The `ResearchPlanner` isolates noun phrases and conceptual facets to synthesize sub-queries.
* **Passage Extraction**: Extracts sentences containing query keywords with adjacent sentence context.
* **Jaccard Deduplication**: Compares candidate passages; passages with Jaccard coefficient $J(A, B) > 0.60$ are deduplicated to ensure diverse perspectives.
* **Citation Registry**: Replaces raw text claims with verifiable tokens (`[C1]`, `[C2]`), providing full title and publication metadata.

---

## 15. Database & Persistence Design (MongoDB + Local Fallback)
* **MongoDB Integration**: Uses official `mongodb-driver-sync` (v4.11.1) with TLS and connection pooling.
* **Automated Local Fallback**: If `MONGODB_URI` is unset or unreachable within a 2.5-second timeout, the `RepositoryManager` seamlessly initializes `FileRepository`, persisting JSON records to `data/storage/`.
* **Zero Credential Exposure**: Passwords and connection secrets are scrubbed from all logs and diagnostic endpoints.

---

## 16. Security Engineering & Threat Mitigation (5/5 Standard)
1. **Path Traversal Blocking**: Validates all file paths against base directories using `Path.normalize().startsWith()`, throwing `SecurityException` on traversal attempts.
2. **Null-Byte Injection Detection**: Scans path strings for `\0` characters.
3. **Regex Secret Masking**: Matches MongoDB URIs and authorization tokens, replacing sensitive credentials with `***:***@`.
4. **Input Boundaries**: Enforces 500-character maximum query length and 10MB maximum file size limits to prevent Denial of Service.

---

## 17. Verification & Test Strategy 
* NLP tokenization, stop-words, and Porter stemming.
* Mathematical verification of smoothed IDF, sublinear TF, and vector norms.
* Inverted index construction and posting list integrity.
* Ranking engine Rank 1 accuracy and explainability.
* TextRank extractive summarization sentence preservation.
* Autonomous research planning and citation grounding.
* Persistence roundtrips on local disk repository.
* Security controls: directory traversal rejection, null-byte injection blocking, secret masking.
* Boundary tests: empty corpus, single-document corpus, boundary $K$ values, stop-words-only queries.
* Algorithmic complexity telemetry capture.

---

## 18. Evaluation Framework & Mathematical Formulations
Retrieval quality is benchmarked against `data/evaluation/ground_truth.json` containing labeled queries and graded relevance judgments:
* **Precision@K**:
  $$\text{P@K} = \frac{|\text{Retrieved}_K \cap \text{Relevant}|}{K}$$
* **Recall@K**:
  $$\text{R@K} = \frac{|\text{Retrieved}_K \cap \text{Relevant}|}{|\text{Relevant}|}$$
* **F1-Score@K**:
  $$\text{F1@K} = 2 \cdot \frac{\text{P@K} \cdot \text{R@K}}{\text{P@K} + \text{R@K}}$$
* **Mean Reciprocal Rank (MRR)**:
  $$\text{MRR} = \frac{1}{|Q_{eval}|} \sum_{i=1}^{|Q_{eval}|} \frac{1}{\text{rank}_i}$$
* **Normalized Discounted Cumulative Gain (nDCG@K)**:
  $$\text{DCG@K} = \sum_{i=1}^K \frac{2^{\text{rel}_i} - 1}{\log_2(i + 1)}, \quad \text{nDCG@K} = \frac{\text{DCG@K}}{\text{IDCG@K}}$$

---

## 19. Performance Profiling & Latency Benchmarks
Empirical measurements obtained from the live evaluation suite on the 12-document research collection:

| Evaluation Metric | Enhanced Model (TF-IDF) | Baseline Model (Lexical) | Improvement |
| :--- | :--- | :--- | :--- |
| **Precision@5** | **0.5750** | 0.5750 | +0.0000 (0.0%) |
| **Recall@5** | **0.9438** | 0.9271 | **+0.0167 (+1.8%)** |
| **F1-Score@5** | **0.6962** | 0.6900 | **+0.0062 (+0.9%)** |
| **Mean Recip. Rank (MRR)**| **1.0000** | 1.0000 | **Perfect Rank 1** |
| **nDCG@5 (Graded)** | **0.9572** | N/A | **+High Saliency** |
| **Average Query Latency** | **4.25 ms** | 1.10 ms | Sub-5ms retrieval |
| **P95 Query Latency** | **16.00 ms** | 4.00 ms | Highly predictable |
| **Corpus Test Size** | **12 documents** | 12 documents | Ground truth collection |

---

## 20. Algorithmic Time Complexity Analysis
Using variables $N, L, T, V, Q, M, K, S, I, P, R$:
* **Inverted Index Build**: $O(N \cdot L) = O(T)$.
* **Preprocessing**: $O(L)$ per document.
* **Sparse Cosine Search**: $O(P + M \log K)$ where $P = \sum_{t \in Q} df(t) \ll Q \cdot N$.
* **Top-K Min-Heap Selection**: $O(M \log K)$ vs unoptimized sorting $O(M \log M)$.
* **TextRank Summarization**: $O(S^2 \cdot L_s + I \cdot S^2)$ with $I \le 25$.
* **Salient Keyword Extraction**: $O(U \log U)$ where $U$ is unique document vocabulary.
* **Research Orchestration**: $O(R \cdot (P + M \log K) + P_{ev}^2 \cdot L_s)$.

---

## 21. Algorithmic Space Complexity Analysis
* **Inverted Index**: $O(T + V)$ auxiliary space for posting lists and unique vocabulary entries.
* **Document Vector Norms**: $O(N)$ stored scalar floats.
* **Query Execution**: $O(M + K)$ for candidate scoring accumulators and priority queue storage.
* **TextRank Sentence Graph**: $O(S^2)$ for the pairwise sentence similarity matrix.
* **Autonomous Brief**: $O(R \cdot K)$ for candidate evidence snippets.

---

## 22. Actual System Output & Verification Logs

### Sample CLI Ranked Search
```
Executing Ranked Vector Space Search for: "machine learning cybersecurity" (Top-5)...

RANK | DOC ID     | SCORE   | TITLE                                  | MATCHED TERMS
----------------------------------------------------------------------------------------------------
1    | DOC-001    | 0.3639  | Machine Learning Applications in Mod... | cybersecur, learn, machin
     Evidence: "Machine learning has emerged as a cornerstone in modern cybersecurity defense..."
     Why Ranked: Relevance Score: 0.3639 | Matched terms: [cybersecur, learn, machin]
----------------------------------------------------------------------------------------------------
Search completed in 6 ms. 5 candidate results ranked.
```

### Sample Autonomous Research Brief
```
Question: What are the applications of machine learning in cybersecurity?

1. DETERMINISTIC RESEARCH PLAN & SUB-QUERIES:
   1. Analyze question and identify domain facets: What are the applications of machine learning in cybersecurity?
   2. Sub-query 1: Retrieve literature for 'What are the applications of machine learning in cybersecurity?'
   3. Sub-query 2: Retrieve literature for 'threat detection'
   4. Sub-query 3: Retrieve literature for 'adversarial attacks'
   5. Sub-query 4: Retrieve literature for 'zero trust architecture'
   6. Deduplicate candidate passages via Jaccard token overlap threshold (<= 0.60)
   7. Rank evidence by composite score and attach verifiable provenance citations

2. SYNTHESIZED KEY FINDINGS (EXTRACTIVE EVIDENCE):
   [Finding 1] Machine learning has emerged as a cornerstone in modern cybersecurity defense architectures. (Source: DOC-001 [C1])
   [Finding 2] Attackers employ evasion attacks during test time, perturbing malicious inputs with imperceptible modifications that trigger misclassification into benign classes. (Source: DOC-008 [C2])
   [Finding 3] Zero-trust network architecture operates under the core principle of continuous verification and explicit trust minimization. (Source: DOC-011 [C3])

3. SOURCE PROVENANCE & CITATION REGISTRY:
   * [C1] DOC-001 - Machine Learning Applications in Modern Cybersecurity and Threat Intelligence (Journal of Cybersecurity and Applied AI, Vol. 14, 2024)
   * [C2] DOC-008 - Adversarial Attacks on Machine Learning Models in Network Security (IEEE Security & Privacy, 2024)
   * [C3] DOC-011 - Zero-Trust Architecture and Behavioral Anomaly Detection Using AI (Journal of Network and Computer Applications, 2024)

4. SALIENT TOPIC KEYWORDS:
   machin (13.16)  learn (12.52)  deep (10.28)  zero-trust (10.28)  adversari (10.28)

5. EXECUTION METRICS:
   * Evidence Coverage   : 100.0%
   * Query Latency       : 48 ms
   * Documents Evaluated : 6
```

---

## 23. Engineering Challenges Encountered & Resolutions
1. **Challenge**: Avoiding division-by-zero and negative weights in small or single-document corpora.  
   **Resolution**: Implemented smoothed IDF $\ln((N+1)/(df+1)) + 1.0$, guaranteeing $IDF \ge 1.0$ under all corpus distributions.
2. **Challenge**: Preventing cloud database connection hangs during offline evaluation.  
   **Resolution**: Implemented a 2.5-second connection timeout on the MongoDB client with automated fallback to `FileRepository`.
3. **Challenge**: Strict two-source-file submission constraint while maintaining senior-SDE clean code.  
   **Resolution**: Utilized cohesive static nested classes, interfaces, and Java records, preventing architectural bloat without sacrificing clean boundaries.

---

## 24. Key Academic & Technical Learnings
* **Mathematical Vector Space Modeling**: Gained deep appreciation of sparse inverted index mechanics vs dense matrix operations.
* **Deterministic NLP vs Stochastic Generative AI**: Extractive TextRank and citation registries provide 100% verifiable provenance essential for compliance-critical domains.
* **Robust Java Systems Programming**: Mastered modern Java 17/21 language features including records, pattern matching, NIO.2 file channels, and bounded priority queues.

---

## 25. Assumptions & Architectural Limitations
* **Language Scope**: Preprocessing components (Porter stemmer and stop-word list) are tailored to English-language technical literature.
* **In-Memory Scale**: The inverted index is maintained in JVM heap memory, optimized for corpora up to 100,000 documents. Larger collections would require disk-backed B-Trees (e.g., Lucene directory structures).

---

## 26. Future Enhancements & Scalability Roadmap
1. **BM25 Probabilistic Ranking**: Add Okapi BM25 as an alternative scoring strategy to vector space cosine similarity.
2. **Positional N-Gram Indexing**: Support phrase searching and proximity boosting using the stored token position integers.
3. **Faceted Metadata Filtering**: Enable search queries filtered by publication year, author, or threat category.

---
