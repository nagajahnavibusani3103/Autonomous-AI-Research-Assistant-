# UML Component Diagram

## 1. System Component Architecture
The component diagram illustrates the high-level structural decomposition of the **Autonomous AI Research Assistant**, showing module dependencies, communication boundaries, and persistence decoupling.

```mermaid
graph TD
    subgraph CLI_Bundle ["Frontend Subsystem (AntigravityFrontend.java)"]
        [CLI Command Parser]
        [Interactive REPL Shell]
        [Visual Terminal Formatter]
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
    
    [Engine Facade API] --> [Security & Path Validator]
    [Engine Facade API] --> [Configuration Manager]
    [Engine Facade API] --> [Audit Logger]
    [Engine Facade API] --> [Synthesis_Component]
    [Engine Facade API] --> [IR_Component]
    [Engine Facade API] --> [Eval_Component]
    [Engine Facade API] --> [Persistence_Component]

    [IR_Component] --> [NLP_Component]
    [Synthesis_Component] --> [IR_Component]
    [Synthesis_Component] --> [NLP_Component]

    [Persistence_Component] --> [Repository Interface]
    [Repository Interface] --> [MongoDB Driver Adapter]
    [Repository Interface] --> [Local File Fallback Adapter]

    [MongoDB Driver Adapter] -.->|TLS / TCP 27017| MongoDB
    [Local File Fallback Adapter] -.->|NIO File I/O| LocalFiles
```

---

## 2. Component Coupling & Cohesion Analysis
- **High Cohesion**: Each component encapsulates a singular mathematical or algorithmic responsibility (e.g., `TextRankSummarizer` focuses strictly on sentence graph convergence; `SimilarityEngine` handles sparse vector products).
- **Low Coupling**: The `EngineFacade` acts as a mediator, allowing components to interact without holding direct circular references.
- **Fail-Safe Persistence**: The `Repository` interface cleanly insulates search and orchestration modules from physical storage mechanisms, allowing zero-downtime fallback between MongoDB and local JSON files.
