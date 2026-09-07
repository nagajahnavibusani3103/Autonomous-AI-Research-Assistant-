# Problem Statement & System Definition: Autonomous AI Research Assistant

## 1. Problem Definition
Scientific and technical research requires literature synthesis, cross-document fact extraction, relevance assessment, and provenance tracking across extensive unstructured text corpora. While modern Large Language Model (LLM) chatbots have popularized automated question answering, they introduce fundamental liabilities in mission-critical environments:
1. **Hallucination & Fabrication**: Generative language models regularly invent plausible-sounding facts and non-existent citations.
2. **High Latency & Cloud Dependency**: Proprietary AI APIs introduce recurring per-token costs, vendor lock-in, and unpredictable latency.
3. **Data Sovereignty & Privacy Exposure**: Sending internal security audits, intellectual property, or proprietary research to third-party endpoints violates compliance and privacy regulations.
4. **Lack of Deterministic Explainability**: Deep neural generators operate as black boxes, preventing researchers from understanding why a particular assertion or document was prioritized.

There is a critical need for an **autonomous, deterministic, 100% locally executing Information Retrieval (IR) and Natural Language Processing (NLP) engine** written in high-performance Java that can ingest unstructured literature, decompose complex research questions, rank evidence via mathematically rigorous vector space models, extract verbatim supporting passages, and generate traceable, citation-backed research briefs without external AI API dependencies.

---

## 2. Scope of the System
The **Autonomous AI Research Assistant** is engineered in pure Java (Java 17+) as a standalone CLI application and engine. 

### In-Scope:
- **Corpus Ingestion**: Multi-format parsing (.txt and .json), file security validation, size enforcement, and structural metadata extraction.
- **Local NLP Pipeline**: Word tokenization, case normalization, punctuation filtering, 150+ stop-word elimination, Porter morphological stemming, and sentence segmentation.
- **Inverted Index & Vector Space Model**: Posting lists with positional data, term frequencies ($TF$), document frequencies ($DF$), document lengths, sublinear term frequency weighting, and smoothed inverse document frequency ($IDF$).
- **Explainable Ranked Search**: Top-K retrieval via bounded min-heap priority queues, sparse vector cosine similarity, and human-readable score attribution.
- **Extractive Summarization**: Graph-based TextRank adaptation using iterative PageRank random walks over sentence similarity networks (guaranteeing zero synthetic hallucination).
- **Salient Keyword Extraction**: Multi-factor scoring incorporating TF-IDF weights, title boosts, and position heuristics.
- **Autonomous Research Orchestration**: Deterministic question decomposition into conceptual facets, multi-query evidence gathering, passage deduplication, reranking, and citation registry mapping (`[C1]`, `[C2]`).
- **Resilient Dual Persistence**: Official MongoDB driver integration with seamless, automated fallback to a local JSON/file-based repository if MongoDB is offline or unconfigured.
- **Security & Integrity Controls**: Path traversal prevention, input boundary enforcement, and regex credential sanitization.
- **Scientific Evaluation Engine**: Automated benchmark suite computing Precision@K, Recall@K, F1@K, Mean Reciprocal Rank (MRR), and Normalized Discounted Cumulative Gain (nDCG@K) against ground-truth datasets.

### Out-of-Scope:
- Cloud-hosted external generative LLM APIs (OpenAI, Anthropic Claude, Google Gemini).
- Real-time unvetted web crawling (mitigates link rot, external rate limits, and network volatility).
- Graphical web browser frontends (eliminating JavaScript/HTML attack surfaces in favor of a polished Java CLI).

---

## 3. Target Users
1. **Cybersecurity & Threat Intelligence Analysts**: Searching and correlating threat reports, CVE disclosures, attack vectors, and intrusion detection methodologies.
2. **Academic & Industrial Researchers**: Synthesizing peer-reviewed literature, isolating key findings, and validating provenance without synthetic model drift.
3. **Air-Gapped / Privacy-Restricted Enterprise Teams**: Organizations requiring automated document retrieval and research assistance under strict data residency constraints.
4. **Software Engineering & IR Evaluators**: Engineers benchmarking classical vector space models, BM25 formulations, and graph-based ranking algorithms.

---

## 4. Primary Objectives
1. **Autonomous Orchestration**: Decompose high-level technical queries into targeted sub-queries and synthesize multi-source evidence into a cohesive brief.
2. **Verifiable Provenance**: Anchor 100% of extracted claims to verbatim source document IDs, eliminating citation fabrication.
3. **Zero-API Dependency**: Execute all NLP, indexing, and summarization tasks on local JVM hardware without external subscriptions.
4. **Defensible Mathematics**: Implement established IR formulations (smoothed IDF, sublinear TF, sparse cosine dot-products, and PageRank convergence).
5. **Senior-SDE Software Quality**: Comply with the single-frontend and single-backend source-file constraint via modular nested architecture, defensive programming, and rigorous unit testing.

---

## 5. Functional Requirements
- **FR-1: Document Ingestion**: Ingest and validate plain text (.txt) and structured JSON (.json) documents, populating document metadata and assigned identifiers.
- **FR-2: Inverted Index Generation**: Build an in-memory inverted index containing term postings, local frequencies, and document lengths.
- **FR-3: Ranked Retrieval**: Execute vector space model queries using smoothed TF-IDF and sparse cosine similarity, returning Top-K results with explanations.
- **FR-4: Autonomous Research**: Plan and execute multi-faceted research for complex questions, extracting supporting passages and generating citations.
- **FR-5: Extractive TextRank Summarization**: Segment documents into sentences, construct a similarity graph, compute PageRank saliency, and extract Top-M sentences.
- **FR-6: Keyword Salience Extraction**: Identify top-N distinguishing keywords using TF-IDF weights and title prominence.
- **FR-7: Persistence & Local Fallback**: Persist documents, query telemetry, and research sessions to MongoDB when available; seamlessly fallback to local disk storage when unavailable.
- **FR-8: Benchmark Evaluation**: Evaluate retrieval effectiveness using Precision@5, Recall@5, F1@5, MRR, and nDCG@5 against a labeled test collection.
- **FR-9: System Diagnostics & Health Check**: Report operational status across the JVM runtime, local corpus, inverted index, persistence layer, and configuration.

---

## 6. Non-Functional Requirements
- **NFR-1: Performance & Latency**: Sub-50 millisecond query latency for Top-K candidate search over indexed research documents.
- **NFR-2: Security & Privacy (5/5 Quality Target)**: Absolute protection against credential leakage via automated log sanitization; strict path canonicalization preventing directory traversal attacks.
- **NFR-3: High Reliability & Fault Tolerance**: Zero application crash if external database connection fails or times out; instant fallback to local JSON repository.
- **NFR-4: Maintainability & Modularity**: Adherence to SOLID principles through clean separation of concerns inside encapsulated static classes, records, and interfaces.
- **NFR-5: Resource Efficiency**: Sparse vector evaluation preventing full-corpus matrix scanning; bounded Top-K priority queue preventing memory exhaustion.

---

## 7. Major System Modules
1. **Corpus Ingestion & Validation Module**: Parses and normalizes incoming literature.
2. **Core Local NLP Pipeline Module**: Tokenization, stop-word elimination, Porter stemming, and sentence segmentation.
3. **Inverted Index & Vector Space Model Module**: Inverted index maintenance and vector norm computation.
4. **Intelligent Ranking & Explainability Engine**: Top-K min-heap search and feature attribution breakdown.
5. **Extractive TextRank Summarization Module**: Graph-based PageRank sentence extraction.
6. **Keyword Salience Extraction Module**: Multi-factor lexical scoring.
7. **Autonomous Research Orchestration Module**: Rule-based question decomposition, multi-query retrieval, and evidence deduplication.
8. **Provenance & Citation Registry Module**: Verifiable source mapping.
9. **Dual Persistence Abstraction Module**: MongoDB official driver adapter with local file fallback.
10. **Evaluation & Benchmarking Engine**: Reproducible information retrieval metric calculations.
11. **Security, Auditing & Configuration Module**: Path canonicalization, input boundary checks, and secret sanitization.
12. **Polished Java CLI Frontend Module**: Interactive REPL and direct command-line execution interfaces.

---

## 8. Expected Outcomes
- An executable, self-contained JAR file (`autonomous-ai-research-assistant.jar`) capable of cold-start document ingestion, ranked search, autonomous research, TextRank summarization, and benchmark evaluation.
- Verifiable benchmark report demonstrating clear precision, recall, and MRR improvements of the Enhanced TF-IDF Vector Space Model over baseline lexical matching.
- Zero secrets committed or logged across git history and console output.

---

## 9. Assumptions & Limitations
- **Corpus Domain**: The system is tuned for technical, computer science, and cybersecurity literature written in English.
- **Corpus Boundary**: The autonomous research orchestrator synthesizes evidence exclusively from documents present in the local corpus; unindexed external facts will not be hallucinated.
- **Language**: Core NLP components (Porter Stemmer, Stop-Word Filter, Sentence Segmenter) target English text.
