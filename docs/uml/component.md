# UML Component Diagram

## 1. System Component Architecture
The component diagram illustrates the high-level structural decomposition of the **Autonomous AI Research Assistant**, showing module dependencies, communication boundaries, and persistence decoupling.

```mermaid
graph TD
    subgraph CLI_Bundle ["Frontend Subsystem (AntigravityFrontend.java)"]
        [CLI Command Parser]
        [Interactive REPL Shell]
        [Visual Terminal Formatter]
        [Embedded HTTP Server & Web UI]
    end

    subgraph Core_Engine ["Backend Subsystem (AntigravityBackend.java)"]
        [Engine Facade API]
        [Security & Path Validator]
        [Configuration Manager]
        [Audit Logger]
        
        subgraph NLP_Component ["NLP & Lexical Processing"]
            [Tokenizer & Normalizer]
            [Stop-Word Filter]
            [Porter Stemmer Engine]
            [Sentence Segmenter]
        end

        subgraph IR_Component ["Information Retrieval Core"]
            [Inverted Index Engine]
            [TF-IDF Weighting Module]
            [Vector Space Similarity]
            [Top-K Min-Heap Ranker]
            [Complexity Telemetry Profiler]
        end

        subgraph Synthesis_Component ["Extractive Synthesis & Orchestration"]
            [Research Planner]
            [Evidence Collector & Deduplicator]
            [Provenance & Citation Registry]
            [TextRank Graph Summarizer]
            [Keyword Saliency Extractor]
        end

        subgraph Persistence_Component ["Data Access Abstraction"]
            [Repository Interface]
            [MongoDB Driver Adapter]
            [Local File Fallback Adapter]
        end

        subgraph Eval_Component ["Evaluation & Diagnostic Core"]
            [Benchmark Evaluation Engine]
            [Health Diagnostics Engine]
            [Telemetry Statistics Engine]
        end
    end

    subgraph External_Storage ["Storage Targets"]
        MongoDB[(Remote / Local MongoDB)]
        LocalFiles[("Local File System (data/storage/)")]
    end

    %% Linkages
    [CLI Command Parser] --> [Engine Facade API]
    [Interactive REPL Shell] --> [Engine Facade API]
    [Embedded HTTP Server & Web UI] --> [Engine Facade API]

    [Engine Facade API] --> [Security & Path Validator]
    [Engine Facade API] --> [Configuration Manager]
    [Engine Facade API] --> [Audit Logger]

    [Engine Facade API] --> [Inverted Index Engine]
    [Engine Facade API] --> [Top-K Min-Heap Ranker]
    [Engine Facade API] --> [Research Planner]
    [Engine Facade API] --> [TextRank Graph Summarizer]
    [Engine Facade API] --> [Keyword Saliency Extractor]
    [Engine Facade API] --> [Benchmark Evaluation Engine]
    [Engine Facade API] --> [Health Diagnostics Engine]
    [Engine Facade API] --> [Repository Interface]

    [Top-K Min-Heap Ranker] --> [Vector Space Similarity]
    [Top-K Min-Heap Ranker] --> [Complexity Telemetry Profiler]
    [Vector Space Similarity] --> [TF-IDF Weighting Module]
    [Inverted Index Engine] --> [Tokenizer & Normalizer]
    [Inverted Index Engine] --> [Porter Stemmer Engine]

    [Research Planner] --> [Top-K Min-Heap Ranker]
    [Research Planner] --> [Evidence Collector & Deduplicator]
    [Evidence Collector & Deduplicator] --> [Provenance & Citation Registry]

    [TextRank Graph Summarizer] --> [Sentence Segmenter]
    [TextRank Graph Summarizer] --> [Tokenizer & Normalizer]

    [Repository Interface] --> [MongoDB Driver Adapter]
    [Repository Interface] --> [Local File Fallback Adapter]

    [MongoDB Driver Adapter] -.->|TLS / TCP| MongoDB
    [Local File Fallback Adapter] -.->|NIO2 File I/O| LocalFiles
```

---

## 2. Component Decoupling & Senior-SDE Boundaries
1. **Frontend / Backend Decoupling**:
   `AntigravityFrontend` depends exclusively on `AntigravityBackend.EngineFacade`. The frontend has zero knowledge of raw posting lists, matrix norms, or database connection strings.
2. **Localhost HTTP Dashboard Integration**:
   The embedded web dashboard and REST API (`/api/search`, `/api/research`, `/api/complexity`, `/api/stats`, `/api/health`, `/api/evaluate`) run seamlessly from the same frontend entrypoint without external HTTP frameworks or external HTML assets.
3. **Database Plug-and-Play**:
   The `Repository` interface enables runtime selection between MongoDB Atlas and local disk without altering a single line of business logic.
