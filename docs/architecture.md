# System Architecture: Autonomous AI Research Assistant

## 1. Architectural Philosophy
The **Autonomous AI Research Assistant** is engineered following modern Senior-SDE enterprise architectural patterns, emphasizing:
- **Separation of Concerns (SoC)**: Clear boundaries between Presentation (CLI), Orchestration, Retrieval, NLP, Persistence, and Evaluation.
- **Dependency Inversion Principle (DIP)**: Core business logic depends strictly on repository abstractions (`Repository` interface), never directly on infrastructure or database drivers.
- **Zero-Trust Local Execution**: Elimination of external runtime dependencies (such as external LLM APIs or internet search endpoints) in favor of deterministic, auditable, mathematically verified local algorithms.
- **Single-Source Modularity**: Adherence to the project constraint requiring exactly one frontend and one backend application source file, achieved cleanly through static inner classes, records, and functional interfaces.

---

## 2. Layered Architecture Diagram

```mermaid
graph TD
    subgraph Presentation_Layer ["Presentation Layer (AntigravityFrontend.java)"]
        UI_CLI["Polished Command-Line Interface"]
        UI_REPL["Interactive REPL Shell (antigravity> )"]
        UI_DISPATCH["Command Parser & Dispatcher"]
        UI_FORMAT["ASCII Formatter & ANSI Visualizer"]
    end

    subgraph Facade_Layer ["Facade & Security Gateway (AntigravityBackend.java)"]
        FACADE["EngineFacade (Singleton Entrypoint)"]
        SEC["SecurityManager (Path & Input Sanitization)"]
        CONFIG["ConfigurationManager (Env & Properties)"]
        AUDIT["AuditLogger (java.util.logging + Secret Masking)"]
    end

    subgraph Orchestration_Layer ["Autonomous Orchestration Layer"]
        PLANNER["ResearchPlanner (Deterministic Decomposition)"]
        ORCH["ResearchOrchestrator (Multi-Query Synthesis)"]
        EVID["EvidenceCollector (Sentence Passage Filtering)"]
        PROV["ProvenanceManager (Citation Registry [C1], [C2])"]
    end

    subgraph NLP_IR_Layer ["NLP & Information Retrieval Engine"]
        NLP_TOK["Tokenizer & SentenceSegmenter"]
        NLP_STOP["StopWordFilter (150+ Lexicon)"]
        NLP_STEM["Porter Stemmer (Morphological Reductions)"]
        IDX["InvertedIndex (Postings, DF, Norms)"]
        TFIDF["TFIDFEngine (Smoothed IDF & Sublinear TF)"]
        SIM["SimilarityEngine (Sparse Cosine Vector Space)"]
        RANK["RankingEngine (Bounded Top-K Min-Heap)"]
        TEXTRANK["TextRankSummarizer (Graph PageRank)"]
        KW["KeywordExtractor (TF-IDF & Title Boost)"]
    end

    subgraph Persistence_Layer ["Persistence & Storage Abstraction Layer"]
        REPO_INT["Repository (Interface Abstraction)"]
        MONGO_REPO["MongoRepository (Official Driver + 2.5s Timeout)"]
        FILE_REPO["FileRepository (Local JSON / Zero Dependency)"]
    end

    subgraph Evaluation_Layer ["Benchmarking & Diagnostics"]
        EVAL["EvaluationEngine (P@K, R@K, F1, MRR, nDCG@K)"]
        HEALTH["HealthChecker (JVM, Corpus, Index, DB Diagnostics)"]
        STATS["StatsEngine (Vocabulary, Memory, Document Telemetry)"]
    end

    %% Wiring
    UI_CLI --> UI_DISPATCH
    UI_REPL --> UI_DISPATCH
    UI_DISPATCH --> FACADE
    FACADE --> SEC
    FACADE --> CONFIG
    FACADE --> AUDIT
    FACADE --> ORCH
    FACADE --> RANK
    FACADE --> TEXTRANK
    FACADE --> KW
    FACADE --> EVAL
    FACADE --> HEALTH
    FACADE --> STATS

    ORCH --> PLANNER
    ORCH --> RANK
    ORCH --> EVID
    ORCH --> PROV

    RANK --> IDX
    RANK --> TFIDF
    RANK --> SIM

    TEXTRANK --> NLP_TOK
    TEXTRANK --> NLP_STEM

    FACADE --> REPO_INT
    REPO_INT --> MONGO_REPO
    REPO_INT --> FILE_REPO
```

---

## 3. Detailed Component Breakdown

### 3.1. Presentation Layer (`AntigravityFrontend.java`)
- **Direct CLI Execution**: Parses single-command invocations such as `java -jar app.jar search "term" 5`.
- **Interactive Shell Mode**: Launches a continuous REPL environment when invoked without arguments, maintaining an active session with prompt `antigravity> `.
- **Visual Presentation**: Renders ASCII tables, boxed report cards, execution latency metrics, and color-coded status badges.

### 3.2. Facade & Security Layer
- **`EngineFacade`**: Singleton central coordinator providing a clean, thread-safe API for all frontend commands.
- **`SecurityManager`**: Enforces strict input validation:
  - Canonicalizes paths to prevent directory traversal (`../../etc/passwd`).
  - Limits query string lengths (max 500 characters).
  - Bounds Top-K parameters ($1 \le K \le 100$).
  - Sanitizes log outputs via regular expressions, stripping passwords and connection strings (`mongodb+srv://***:***@...`).
- **`ConfigurationManager`**: Priority-based parameter loading: Environment Variables > System Properties > `config.properties` > Defaults.
- **`AuditLogger`**: Configures `java.util.logging` with custom formatting, avoiding stdout pollution while recording structured timestamps and sanitized event payloads.

### 3.3. NLP & IR Core
- **`Tokenizer` & `SentenceSegmenter`**: Normalizes case, removes non-alphanumeric punctuation while preserving hyphenated compounds, and splits text into cohesive sentence units.
- **`StopWordFilter`**: High-performance $O(1)$ set lookup against 150+ English syntactic stop words.
- **`Stemmer`**: Pure Java implementation of Martin Porter's 1980 algorithm across all five transformation phases.
- **`InvertedIndex`**: Maps unique vocabulary stems to document postings with term frequencies and positional vectors. Precomputes Euclidean $L_2$ document norms:
  $$\|\vec{d}\|_2 = \sqrt{\sum_{t \in d} \left(TF(t, d) \cdot IDF(t)\right)^2}$$
- **`TFIDFEngine`**: Mathematically rigorous term weighting:
  $$TF(t, d) = 1 + \ln(f_{t, d}) \quad (\text{for } f_{t, d} > 0)$$
  $$IDF(t) = \ln\left(\frac{N + 1}{df(t) + 1}\right) + 1.0$$
- **`SimilarityEngine`**: Computes sparse vector cosine similarity strictly over non-zero query terms, avoiding dense matrix expansions:
  $$\text{Cosine}(Q, D) = \frac{\sum_{t \in Q \cap D} w_{t, Q} \cdot w_{t, D}}{\|\vec{Q}\|_2 \cdot \|\vec{D}\|_2}$$
- **`RankingEngine`**: Employs a bounded min-heap (`PriorityQueue`) of capacity $K$, reducing sorting complexity from $O(M \log M)$ to $O(M \log K)$, where $M$ is the candidate document count. Also provides Okapi BM25 and raw lexical baselines for benchmark comparisons.
- **`TextRankSummarizer`**: Graph-based extractive summarizer. Builds a sentence similarity matrix using length-normalized lexical overlap and runs iterative PageRank random walks until convergence.
- **`KeywordExtractor`**: Ranks terms by combining TF-IDF saliency with title-appearance boosts ($1.5\times$).

### 3.4. Autonomous Research Orchestration
- **`ResearchPlanner`**: Deconstructs overarching research questions into 3–5 targeted conceptual facets using semantic domain mappings and token combinatorics.
- **`EvidenceCollector`**: Executes multi-query retrieval, extracts the highest-scoring sentence from each document, eliminates near-duplicate passages via Jaccard token overlap ($> 0.60$), and reranks evidence.
- **`ProvenanceManager`**: Generates grounded citations (`[C1]`, `[C2]`, etc.) directly linked to verified document IDs, titles, and publication sources.
- **`ResearchSynthesizer`**: Compiles findings, citations, keywords, coverage percentages, and latency statistics into an executive research brief.

### 3.5. Persistence Layer & Local Fallback
- **`Repository` Interface**: Abstraction exposing CRUD methods for documents, query telemetry, and research sessions.
- **`MongoRepository`**: Uses the official `mongodb-driver-sync` driver with a strict 2.5-second connection/socket timeout. If MongoDB is unreachable, it logs a sanitized warning and transitions.
- **`FileRepository`**: Fully self-contained local JSON repository operating in `data/storage/`, ensuring 100% operational reliability even in air-gapped or database-free environments.
- **`RepositoryManager`**: Detects environment availability and automatically binds to `MongoRepository` or `FileRepository`.

### 3.6. Evaluation & Benchmarking
- **`EvaluationEngine`**: Computes macro-averaged Precision@K, Recall@K, F1@K, Mean Reciprocal Rank (MRR), and Normalized Discounted Cumulative Gain (nDCG@K) against labeled ground truth.
- **`HealthChecker` & `StatsEngine`**: Measures JVM memory, corpus status, vocabulary size, index memory footprint, and storage state.
