# UML Sequence Diagrams

## 1. Ranked Search Execution Sequence

```mermaid
sequenceDiagram
    autonumber
    actor User as Researcher
    participant CLI as AntigravityFrontend
    participant Facade as EngineFacade
    participant Sec as SecurityManager
    participant NLP as TextPreprocessor
    participant Rank as RankingEngine
    participant Idx as InvertedIndex
    participant Sim as SimilarityEngine
    participant Repo as Repository

    User->>CLI: search "machine learning cybersecurity" 5
    CLI->>Facade: search(query, 5)
    Facade->>Sec: validateQuery(query)
    Sec-->>Facade: validation passed
    Facade->>Rank: search(query, 5, minScore)
    Rank->>NLP: preprocess(query)
    NLP-->>Rank: [machin, learn, cybersecur]
    Rank->>Idx: getCandidateDocIds(queryTerms)
    Idx-->>Rank: Set of candidate doc IDs {DOC-001, DOC-002, ...}
    
    loop For each candidate docId
        Rank->>Sim: calculateCosineSimilarity(qWeights, qNorm, docId, index)
        Sim->>Idx: getTermFrequency(term, docId)
        Idx-->>Sim: tf counts
        Sim-->>Rank: cosine score
        Rank->>Rank: offer to Min-Heap PriorityQueue
    end

    Rank-->>Facade: List<SearchResult> (Sorted Top-5)
    Facade->>Repo: saveQueryLog(log)
    Facade-->>CLI: List<SearchResult>
    CLI-->>User: Render Ranked Results Table + Evidence Snippets + Latency
```

---

## 2. Autonomous Research Brief Sequence

```mermaid
sequenceDiagram
    autonumber
    actor User as Researcher
    participant CLI as AntigravityFrontend
    participant Facade as EngineFacade
    participant Orch as ResearchOrchestrator
    participant Plan as ResearchPlanner
    participant Rank as RankingEngine
    participant Evid as EvidenceCollector
    participant Prov as ProvenanceManager
    participant KW as KeywordExtractor
    participant Repo as Repository

    User->>CLI: research "What are the applications of ML in cybersecurity?"
    CLI->>Facade: research(question)
    Facade->>Orch: conductResearch(question)
    Orch->>Plan: generateSubQueries(question)
    Plan-->>Orch: [Q1 (original), Q2 (threat detection), Q3 (adversarial attacks), Q4 (zero trust)]
    
    loop For each sub-query
        Orch->>Rank: search(subQuery, 5, minScore)
        Rank-->>Orch: List<SearchResult>
        Orch->>Evid: extractPassages(searchResults, subQuery)
        Evid-->>Orch: List<EvidenceSnippet>
    end

    Orch->>Evid: deduplicatePassages(allPassages, jaccardThreshold = 0.60)
    Evid-->>Orch: Deduplicated Passages
    Orch->>Prov: assignCitations(deduplicatedPassages)
    Prov-->>Orch: [C1, C2, C3...] Provenance Registry
    Orch->>KW: extractCorpusKeywords(topDocs, 8)
    KW-->>Orch: Salient Keywords List
    Orch-->>Facade: ResearchReport
    Facade->>Repo: saveResearchSession(session)
    Facade-->>CLI: ResearchReport
    CLI-->>User: Formatted Multi-Query Research Brief with Verifiable Citations
```

---

## 3. Algorithmic Complexity Telemetry Sequence

```mermaid
sequenceDiagram
    autonumber
    actor User as Researcher / Reviewer
    participant CLI as AntigravityFrontend
    participant Facade as EngineFacade
    participant Rank as RankingEngine
    participant Idx as InvertedIndex

    User->>CLI: complexity "machine learning intrusion detection"
    CLI->>Facade: getComplexityProfile(query)
    Facade->>Rank: search(query, 5, minScore)
    Note over Rank: Telemetry counters recorded:<br/>Q, M, P, K, executionTimeNanos
    Rank-->>Facade: List<SearchResult>
    Facade->>Rank: getLastComplexityProfile()
    Rank->>Idx: getDocumentCount(), getVocabularySize(), getTotalTokens(), getAvgDocLength()
    Idx-->>Rank: Corpus metadata
    Rank-->>Facade: ComplexityProfile Record
    Facade-->>CLI: ComplexityProfile Record
    CLI-->>User: Side-by-side Asymptotic Matrix + Empirical Telemetry + Bound Verification
```
