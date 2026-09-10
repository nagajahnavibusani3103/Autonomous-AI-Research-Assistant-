# UML Class Diagram & Object-Oriented Architecture

## 1. Design Overview
The system achieves Senior-SDE modularity within the **two-source-file constraint** using encapsulated static nested classes, immutable Java records, and interface abstractions.

```mermaid
classDiagram
    class AntigravityFrontend {
        +main(String[] args)
        -runInteractiveShell(EngineFacade engine)
        -executeCommand(String[] args, EngineFacade engine)
        -handleHelp()
        -handleIngest(String[] args, EngineFacade engine)
        -handleSearch(String[] args, EngineFacade engine)
        -handleResearch(String[] args, EngineFacade engine)
        -handleSummarize(String[] args, EngineFacade engine)
        -handleKeywords(String[] args, EngineFacade engine)
        -handleComplexity(String[] args, EngineFacade engine)
        -handleStats(EngineFacade engine)
        -handleEvaluate(EngineFacade engine)
        -handleHealth(EngineFacade engine)
        -handleConfig()
        -handleServer(String[] args, EngineFacade engine)
    }

    class EngineFacade {
        -InvertedIndex index
        -Repository repository
        -RankingEngine rankingEngine
        -CorpusIngestionEngine ingestionEngine
        -ResearchOrchestrator researchOrchestrator
        -EvaluationEngine evaluationEngine
        +getInstance() EngineFacade
        +ingest(String path) int
        +search(String query, int k) List~SearchResult~
        +research(String question) ResearchReport
        +summarize(String docId, int sentences) String
        +keywords(String docId, int topN) List~Entry~
        +getComplexityProfile(String query) ComplexityProfile
        +stats() SystemStats
        +health() HealthReport
        +evaluate() EvaluationMetrics
        +shutdown() void
    }

    class Document {
        <<record>>
        +String docId
        +String title
        +String source
        +String content
        +Map metadata
        +List tokens
        +Instant createdAt
    }

    class Posting {
        <<record>>
        +String docId
        +int termFrequency
        +List~Integer~ positions
    }

    class SearchResult {
        <<record>>
        +String docId
        +String title
        +double score
        +List~String~ matchedTerms
        +String snippet
        +String source
        +int rank
        +String explanation
    }

    class ResearchReport {
        <<record>>
        +String question
        +List~String~ researchPlan
        +List~String~ subQueries
        +List~String~ keyFindings
        +List~EvidenceSnippet~ evidenceList
        +Map~String,String~ citations
        +List~Entry~ topKeywords
        +List~String~ sources
        +double evidenceCoverage
        +long queryLatencyMs
        +int documentsRetrieved
    }

    class ComplexityProfile {
        <<record>>
        +int corpusDocuments
        +int vocabularySize
        +long totalTokens
        +double avgDocumentLength
        +String lastQuery
        +int queryTermsCount
        +int candidateDocumentsCount
        +int postingsTraversedCount
        +int topKRequested
        +long executionTimeNanos
        +double executionTimeMs
    }

    class Repository {
        <<interface>>
        +saveDocument(Document doc) void
        +findDocumentById(String id) Optional~Document~
        +saveQueryLog(QueryLog log) void
        +saveResearchSession(ResearchSession session) void
        +getDocumentCount() long
        +isHealthy() boolean
        +getStorageMode() String
        +close() void
    }

    class MongoRepository {
        -MongoClient mongoClient
        -MongoDatabase database
        -MongoCollection docCollection
        +saveDocument(Document doc) void
        +findDocumentById(String id) Optional~Document~
    }

    class FileRepository {
        -Path storageDirectory
        +saveDocument(Document doc) void
        +findDocumentById(String id) Optional~Document~
    }

    class InvertedIndex {
        -Map~String, List~Posting~~ indexMap
        -Map~String, Document~ documentStore
        -Map~String, Double~ docVectorNorms
        +addDocument(Document doc) void
        +getPostings(String term) List~Posting~
        +computeVectorNorms() void
    }

    class RankingEngine {
        -InvertedIndex index
        -ComplexityProfile lastProfile
        +search(String query, int k, double minScore) List~SearchResult~
        +getLastComplexityProfile() ComplexityProfile
    }

    AntigravityFrontend --> EngineFacade : invokes
    EngineFacade --> InvertedIndex : manages
    EngineFacade --> Repository : persists via
    EngineFacade --> RankingEngine : delegates search
    MongoRepository ..|> Repository : implements
    FileRepository ..|> Repository : implements
    RankingEngine --> InvertedIndex : reads postings
    RankingEngine --> SearchResult : produces
    RankingEngine --> ComplexityProfile : profiles
    EngineFacade --> ResearchReport : generates
```

---

## 2. Core Architectural Patterns Applied

1. **Facade Pattern (`EngineFacade`)**:
   Provides a clean, unified interface concealing subsystem complexity across ingestion, indexing, ranking, TextRank summarization, and MongoDB/file persistence.

2. **Repository Pattern (`Repository`, `MongoRepository`, `FileRepository`)**:
   Decouples data access from domain logic. Supports seamless, automatic failover from MongoDB Atlas to local JSON file storage.

3. **Strategy Pattern (`SimilarityEngine`)**:
   Encapsulates vector space scoring strategies, allowing isolated calculation of sublinear term frequency, smoothed inverse document frequency, and cosine dot products.

4. **Immutable Records**:
   All transfer objects (`Document`, `Posting`, `SearchResult`, `EvidenceSnippet`, `ResearchReport`, `ComplexityProfile`, `SystemStats`, `EvaluationMetrics`) are declared as Java records ensuring thread safety, zero mutation side effects, and concise serialization.
