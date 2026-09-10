package com.antigravity;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * AUTONOMOUS AI RESEARCH ASSISTANT - COMPREHENSIVE TEST SUITE
 * ============================================================================
 * Rigorous Unit, Integration, Mathematical, and Security Testing.
 * Covers:
 *  - NLP Tokenization, Stop-Word Elimination, Porter Stemming
 *  - Inverted Index Data Structures, Postings, Vocabulary
 *  - Mathematical Verifications of TF-IDF and Cosine Vector Similarity
 *  - Ranking Engine Top-K Min-Heap Correctness & Explainability
 *  - TextRank Extractive Graph-Based Summarization (Zero Hallucination)
 *  - Deterministic Autonomous Research Planning & Sub-Query Generation
 *  - Provenance Traceability & Grounded Citation Verification
 *  - Local File Repository Fallback & Roundtrip Persistence
 *  - Security Protections: Path Traversal Prevention & Secret Masking
 *  - Ground-Truth Evaluation Engine (P@K, R@K, F1, MRR, nDCG)
 * ============================================================================
 */
public class AntigravityTestSuite {

    private AntigravityBackend.InvertedIndex testIndex;
    private AntigravityBackend.RankingEngine rankingEngine;
    private AntigravityBackend.Repository fileRepo;
    private Path tempStorageDir;

    @BeforeEach
    void setUp() throws IOException {
        testIndex = new AntigravityBackend.InvertedIndex();
        rankingEngine = new AntigravityBackend.RankingEngine(testIndex);
        tempStorageDir = Files.createTempDirectory("antigravity_test_storage");
        fileRepo = new AntigravityBackend.FileRepository(tempStorageDir);
    }

    @AfterEach
    void tearDown() {
        if (fileRepo != null) fileRepo.close();
    }

    // ========================================================================
    // 1. NLP UNIT TESTS
    // ========================================================================

    @Test
    @DisplayName("Tokenizer correctly normalizes case, eliminates punctuation, and handles hyphens")
    void testTokenizer() {
        String input = "Machine-learning, AI & cyber-security: fast, scalable 100%!";
        List<String> tokens = AntigravityBackend.Tokenizer.tokenize(input);

        assertNotNull(tokens);
        assertTrue(tokens.contains("machine-learning") || tokens.contains("machine"));
        assertTrue(tokens.contains("ai"));
        assertTrue(tokens.contains("cyber-security") || tokens.contains("cyber"));
        assertTrue(tokens.contains("fast"));
        assertTrue(tokens.contains("scalable"));
        assertFalse(tokens.contains("&"));
        assertFalse(tokens.contains("%"));
    }

    @Test
    @DisplayName("StopWordFilter accurately discards common English syntactic stop words")
    void testStopWordFilter() {
        List<String> tokens = Arrays.asList("this", "is", "a", "deep", "learning", "network", "for", "intrusion", "detection");
        List<String> filtered = AntigravityBackend.StopWordFilter.filter(tokens);

        assertFalse(filtered.contains("this"));
        assertFalse(filtered.contains("is"));
        assertFalse(filtered.contains("a"));
        assertFalse(filtered.contains("for"));
        assertTrue(filtered.contains("deep"));
        assertTrue(filtered.contains("learning"));
        assertTrue(filtered.contains("network"));
        assertTrue(filtered.contains("intrusion"));
        assertTrue(filtered.contains("detection"));
    }

    @Test
    @DisplayName("Porter Stemmer conforms to classical morphological reductions")
    void testPorterStemmer() {
        assertEquals("connect", AntigravityBackend.Stemmer.stem("connecting"));
        assertEquals("connect", AntigravityBackend.Stemmer.stem("connection"));
        assertEquals("connect", AntigravityBackend.Stemmer.stem("connected"));
        assertEquals("retriev", AntigravityBackend.Stemmer.stem("retrieval"));
        assertEquals("classif", AntigravityBackend.Stemmer.stem("classification"));
        assertEquals("secur", AntigravityBackend.Stemmer.stem("security"));
    }

    // ========================================================================
    // 2. MATHEMATICAL IR & INDEXING TESTS
    // ========================================================================

    @Test
    @DisplayName("TF-IDF mathematically adheres to smoothed formulation and sublinear scaling")
    void testTfIdfFormulas() {
        int N = 10;
        int df = 2;
        double idf = AntigravityBackend.TFIDFEngine.calculateSmoothedIdf(N, df);
        // IDF(t) = ln((10 + 1) / (2 + 1)) + 1 = ln(11/3) + 1 ≈ 1.299 + 1 = 2.299
        assertEquals(Math.log(11.0 / 3.0) + 1.0, idf, 0.0001);

        double tf1 = AntigravityBackend.TFIDFEngine.calculateSublinearTf(1);
        assertEquals(1.0, tf1, 0.0001);

        double tf5 = AntigravityBackend.TFIDFEngine.calculateSublinearTf(5);
        // Sublinear TF: 1 + ln(5) ≈ 1 + 1.6094 = 2.6094
        assertEquals(1.0 + Math.log(5), tf5, 0.0001);
        assertTrue(tf5 < 5.0, "Sublinear TF must compress high term frequency");
    }

    @Test
    @DisplayName("InvertedIndex correctly indexes terms, computes posting lists, and calculates norms")
    void testInvertedIndexConstruction() {
        AntigravityBackend.Document doc1 = new AntigravityBackend.Document(
                "TEST-01", "Cyber Defense", "Source A", "Cyber defense uses machine learning models.", new HashMap<>(), null, Instant.now()
        );
        AntigravityBackend.Document doc2 = new AntigravityBackend.Document(
                "TEST-02", "Information Retrieval", "Source B", "Inverted index enables fast vector retrieval.", new HashMap<>(), null, Instant.now()
        );

        testIndex.addDocument(doc1);
        testIndex.addDocument(doc2);
        testIndex.computeVectorNorms();

        assertEquals(2, testIndex.getDocumentCount());
        assertTrue(testIndex.getVocabularySize() > 0);

        String cyberStem = AntigravityBackend.Stemmer.stem("cyber");
        assertEquals(1, testIndex.getDocumentFrequency(cyberStem));
        // "cyber" appears in title ("Cyber Defense") and content ("Cyber defense ...") -> tf = 2
        assertEquals(2, testIndex.getTermFrequency(cyberStem, "TEST-01"));
        assertEquals(0, testIndex.getTermFrequency(cyberStem, "TEST-02"));

        assertTrue(testIndex.getDocVectorNorm("TEST-01") > 0.0);
        assertTrue(testIndex.getDocVectorNorm("TEST-02") > 0.0);
    }

    // ========================================================================
    // 3. RETRIEVAL & RANKING TESTS
    // ========================================================================

    @Test
    @DisplayName("RankingEngine returns most relevant document at Rank 1 with explainable breakdown")
    void testRankingEngine() {
        AntigravityBackend.Document docA = new AntigravityBackend.Document(
                "DOC-A", "Malware Classification", "Source A",
                "Automated malware classification analyzes dynamic execution and static headers to categorize malicious software.",
                new HashMap<>(), null, Instant.now()
        );
        AntigravityBackend.Document docB = new AntigravityBackend.Document(
                "DOC-B", "Astronomy Stars", "Source B",
                "Stars and galaxies are observed through deep space optical and radio telescopes.",
                new HashMap<>(), null, Instant.now()
        );

        testIndex.addDocument(docA);
        testIndex.addDocument(docB);
        testIndex.computeVectorNorms();

        List<AntigravityBackend.SearchResult> results = rankingEngine.search("malware classification software", 5, 0.001);

        assertFalse(results.isEmpty());
        assertEquals("DOC-A", results.get(0).docId(), "DOC-A must rank #1 for malware classification query");
        assertEquals(1, results.get(0).rank());
        assertTrue(results.get(0).score() > 0.0);
        assertNotNull(results.get(0).explanation());
        assertTrue(results.get(0).explanation().contains("Relevance Score"));
    }

    // ========================================================================
    // 4. TEXTRANK SUMMARIZATION TESTS
    // ========================================================================

    @Test
    @DisplayName("TextRankSummarizer extracts verbatim sentences from source text without hallucination")
    void testTextRankSummarizer() {
        String article = "Machine learning enhances proactive cyber defense against zero-day exploits. " +
                "Traditional signature detection mechanisms often fail on mutated polymorphic variants. " +
                "Deep neural networks analyze network telemetry to discover subtle anomalous patterns. " +
                "Consequently, modern security teams prevent catastrophic breaches through automated intelligence.";

        String summary = AntigravityBackend.TextRankSummarizer.summarize(article, 2);

        assertNotNull(summary);
        assertFalse(summary.isEmpty());

        List<String> originalSentences = AntigravityBackend.SentenceSegmenter.segment(article);
        List<String> summarySentences = AntigravityBackend.SentenceSegmenter.segment(summary);

        assertEquals(2, summarySentences.size(), "Summary must contain exactly requested number of sentences");
        for (String s : summarySentences) {
            assertTrue(originalSentences.contains(s), "Extracted sentence must strictly originate from source text");
        }
    }

    // ========================================================================
    // 5. AUTONOMOUS RESEARCH ORCHESTRATION TESTS
    // ========================================================================

    @Test
    @DisplayName("ResearchPlanner deterministically generates targeted conceptual sub-queries")
    void testResearchPlanner() {
        String question = "What are the major applications of machine learning in cybersecurity?";
        List<String> subQueries = AntigravityBackend.ResearchPlanner.generateSubQueries(question);

        assertNotNull(subQueries);
        assertFalse(subQueries.isEmpty());
        assertTrue(subQueries.size() <= 4);
        assertTrue(subQueries.contains(question));

        List<String> plan = AntigravityBackend.ResearchPlanner.createPlan(question, subQueries);
        assertTrue(plan.size() >= 3);
    }

    @Test
    @DisplayName("ProvenanceManager assigns genuine citation identifiers grounded in corpus")
    void testProvenanceAndCitations() {
        List<AntigravityBackend.EvidenceSnippet> raw = Arrays.asList(
                new AntigravityBackend.EvidenceSnippet("", "Claim 1", "DOC-001", "ML Security", "Passage text 1", 0.85, "Journal A"),
                new AntigravityBackend.EvidenceSnippet("", "Claim 2", "DOC-002", "Intrusion Detection", "Passage text 2", 0.75, "Journal B")
        );

        Map<String, String> citations = new LinkedHashMap<>();
        List<AntigravityBackend.EvidenceSnippet> cited = AntigravityBackend.ProvenanceManager.assignCitations(raw, citations);

        assertEquals(2, cited.size());
        assertEquals("[C1]", cited.get(0).citationId());
        assertEquals("[C2]", cited.get(1).citationId());
        assertTrue(citations.containsKey("[C1]"));
        assertTrue(citations.get("[C1]").contains("DOC-001"));
    }

    // ========================================================================
    // 6. PERSISTENCE & LOCAL FALLBACK TESTS
    // ========================================================================

    @Test
    @DisplayName("FileRepository reliably saves, finds, and counts documents on disk")
    void testFileRepositoryPersistence() {
        AntigravityBackend.Document doc = new AntigravityBackend.Document(
                "STORE-01", "Persistent Title", "Source X", "Testing storage persistence in local file fallback mode.",
                new HashMap<>(), null, Instant.now()
        );

        fileRepo.saveDocument(doc);
        assertEquals(1, fileRepo.getDocumentCount());

        Optional<AntigravityBackend.Document> loaded = fileRepo.findDocumentById("STORE-01");
        assertTrue(loaded.isPresent());
        assertEquals("STORE-01", loaded.get().docId());
        assertEquals("Persistent Title", loaded.get().title());
        assertEquals("Source X", loaded.get().source());
        assertTrue(fileRepo.isHealthy());
    }

    // ========================================================================
    // 7. SECURITY & INTEGRITY TESTS
    // ========================================================================

    @Test
    @DisplayName("SecurityManager blocks directory traversal and sanitizes sensitive credentials")
    void testSecurityControls() {
        // Path Traversal Blocking
        Path base = Paths.get("data", "sample");
        assertThrows(SecurityException.class, () -> {
            AntigravityBackend.SecurityManager.validateAndResolvePath("../../etc/passwd", base);
        });

        // Secret Masking
        String sensitiveUri = "mongodb+srv://adminUser:superSecretPass123@cluster0.abc.mongodb.net/?retryWrites=true";
        String sanitized = AntigravityBackend.SecurityManager.sanitize(sensitiveUri);
        assertFalse(sanitized.contains("adminUser"));
        assertFalse(sanitized.contains("superSecretPass123"));
        assertTrue(sanitized.contains("***:***@"));

        // Query Validation
        assertThrows(IllegalArgumentException.class, () -> AntigravityBackend.SecurityManager.validateQuery(""));
        assertThrows(IllegalArgumentException.class, () -> AntigravityBackend.SecurityManager.validateQuery(null));
    }

    // ========================================================================
    // 8. EVALUATION BENCHMARK & GROUND TRUTH TESTS
    // ========================================================================

    @Test
    @DisplayName("EvaluationEngine runs successfully against ground truth and produces valid metrics")
    void testEvaluationEngine() throws Exception {
        Path sampleDir = Paths.get("data", "sample");
        Path gtPath = Paths.get("data", "evaluation", "ground_truth.json");

        if (Files.exists(sampleDir) && Files.exists(gtPath)) {
            AntigravityBackend.CorpusIngestionEngine ingester =
                    new AntigravityBackend.CorpusIngestionEngine(testIndex, fileRepo);
            ingester.ingestDirectory(sampleDir);

            AntigravityBackend.EvaluationEngine evalEngine =
                    new AntigravityBackend.EvaluationEngine(testIndex, rankingEngine);

            AntigravityBackend.EvaluationMetrics metrics = evalEngine.evaluate(gtPath, 5);

            assertNotNull(metrics);
            assertTrue(metrics.precisionAtK() >= 0.0 && metrics.precisionAtK() <= 1.0);
            assertTrue(metrics.recallAtK() >= 0.0 && metrics.recallAtK() <= 1.0);
            assertTrue(metrics.mrr() >= 0.0 && metrics.mrr() <= 1.0);
            assertTrue(metrics.ndcgAtK() >= 0.0 && metrics.ndcgAtK() <= 1.0);
            assertTrue(metrics.avgLatencyMs() >= 0.0);
        }
    }

    // ========================================================================
    // 9. BOUNDARY & EDGE CASE TESTS
    // ========================================================================

    @Test
    @DisplayName("Empty corpus search returns empty result list gracefully without exceptions")
    void testEmptyCorpusSearch() {
        AntigravityBackend.InvertedIndex emptyIndex = new AntigravityBackend.InvertedIndex();
        AntigravityBackend.RankingEngine emptyEngine = new AntigravityBackend.RankingEngine(emptyIndex);

        List<AntigravityBackend.SearchResult> results = emptyEngine.search("artificial intelligence", 5, 0.001);
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Search against an empty index must return an empty list");
    }

    @Test
    @DisplayName("Single document corpus indexes and retrieves correctly")
    void testSingleDocumentCorpus() {
        AntigravityBackend.Document singleDoc = new AntigravityBackend.Document(
                "SOLO-01", "Quantum Cryptography", "Physics Journal",
                "Quantum key distribution guarantees information-theoretic secrecy based on physical principles.",
                new HashMap<>(), null, Instant.now()
        );
        testIndex.addDocument(singleDoc);
        testIndex.computeVectorNorms();

        assertEquals(1, testIndex.getDocumentCount());
        List<AntigravityBackend.SearchResult> results = rankingEngine.search("quantum key distribution secrecy", 5, 0.001);

        assertEquals(1, results.size());
        assertEquals("SOLO-01", results.get(0).docId());
        assertEquals(1, results.get(0).rank());
        assertTrue(results.get(0).score() > 0.0);
    }

    @Test
    @DisplayName("K boundary conditions (K=1, K > matching docs, K=0) behave safely")
    void testBoundaryKValues() {
        AntigravityBackend.Document docA = new AntigravityBackend.Document(
                "BOUND-A", "Deep Learning Models", "Source A",
                "Deep learning models require gradient descent optimization.",
                new HashMap<>(), null, Instant.now()
        );
        AntigravityBackend.Document docB = new AntigravityBackend.Document(
                "BOUND-B", "Machine Learning Concepts", "Source B",
                "Machine learning algorithms build mathematical models from sample training data.",
                new HashMap<>(), null, Instant.now()
        );
        testIndex.addDocument(docA);
        testIndex.addDocument(docB);
        testIndex.computeVectorNorms();

        // Test K = 1
        List<AntigravityBackend.SearchResult> top1 = rankingEngine.search("learning models", 1, 0.001);
        assertEquals(1, top1.size());

        // Test K = 100 when only 2 documents match (should return at most 2, never crash)
        int validatedK = AntigravityBackend.SecurityManager.validateK(100, 5);
        List<AntigravityBackend.SearchResult> top100 = rankingEngine.search("learning models", validatedK, 0.001);
        assertTrue(top100.size() <= 2);

        // Test K = 0 (validateK clamps to default topK)
        int clampedK = AntigravityBackend.SecurityManager.validateK(0, 5);
        assertEquals(5, clampedK);
    }

    @Test
    @DisplayName("Query with only stop words returns empty result without crashing")
    void testStopWordsOnlyQuery() {
        AntigravityBackend.Document doc = new AntigravityBackend.Document(
                "DOC-01", "General Text", "Source", "This is an article about systems engineering.",
                new HashMap<>(), null, Instant.now()
        );
        testIndex.addDocument(doc);
        testIndex.computeVectorNorms();

        List<AntigravityBackend.SearchResult> results = rankingEngine.search("this is what was", 5, 0.001);
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Query containing only stop words must yield zero results");
    }

    // ========================================================================
    // 10. COMPLEXITY TELEMETRY & VERIFICATION TESTS
    // ========================================================================

    @Test
    @DisplayName("ComplexityProfile captures empirical telemetry accurately")
    void testComplexityProfileCapture() {
        AntigravityBackend.Document doc1 = new AntigravityBackend.Document(
                "COMP-01", "Intrusion Detection", "Source A",
                "Network intrusion detection systems monitor anomalous traffic patterns.",
                new HashMap<>(), null, Instant.now()
        );
        AntigravityBackend.Document doc2 = new AntigravityBackend.Document(
                "COMP-02", "Neural Networks", "Source B",
                "Neural networks train weight matrices using backpropagation algorithms.",
                new HashMap<>(), null, Instant.now()
        );
        testIndex.addDocument(doc1);
        testIndex.addDocument(doc2);
        testIndex.computeVectorNorms();

        rankingEngine.search("intrusion detection network", 5, 0.001);
        AntigravityBackend.ComplexityProfile profile = rankingEngine.getLastComplexityProfile();

        assertNotNull(profile);
        assertEquals(2, profile.corpusDocuments());
        assertTrue(profile.vocabularySize() > 0);
        assertTrue(profile.totalTokens() > 0);
        assertTrue(profile.avgDocumentLength() > 0);
        assertEquals("intrusion detection network", profile.lastQuery());
        assertTrue(profile.queryTermsCount() >= 2);
        assertTrue(profile.candidateDocumentsCount() >= 1);
        assertTrue(profile.postingsTraversedCount() >= 1);
        assertTrue(profile.executionTimeNanos() >= 0);
    }

    // ========================================================================
    // 11. KEYWORD EXTRACTION & HEALTH CHECKS
    // ========================================================================

    @Test
    @DisplayName("KeywordExtractor produces ranked salient domain keywords")
    void testKeywordExtractor() {
        AntigravityBackend.Document doc = new AntigravityBackend.Document(
                "KW-01", "Adversarial Machine Learning", "Security Lab",
                "Adversarial perturbations compromise neural network classifiers. Adversarial attacks manipulate gradient steps.",
                new HashMap<>(), null, Instant.now()
        );
        testIndex.addDocument(doc);
        testIndex.computeVectorNorms();

        List<Map.Entry<String, Double>> keywords = AntigravityBackend.KeywordExtractor.extractKeywords(doc, testIndex, 5);
        assertNotNull(keywords);
        assertFalse(keywords.isEmpty());
        assertTrue(keywords.size() <= 5);
        for (Map.Entry<String, Double> kw : keywords) {
            assertTrue(kw.getValue() > 0.0);
        }
    }

    @Test
    @DisplayName("HealthChecker correctly validates system components")
    void testHealthCheckDiagnostics() {
        AntigravityBackend.HealthReport report = AntigravityBackend.HealthChecker.checkHealth(testIndex, fileRepo);
        assertNotNull(report);
        assertTrue(report.javaRuntimeStatus().startsWith("PASS"));
        assertNotNull(report.mongoStatus());
        assertNotNull(report.overallStatus());
        assertNotNull(report.details());
    }

    // ========================================================================
    // 12. INPUT BOUNDS & DEFENSIVE SECURITY
    // ========================================================================

    @Test
    @DisplayName("Excessively long query string throws IllegalArgumentException")
    void testQueryLengthBoundary() {
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            longQuery.append("a");
        }
        assertThrows(IllegalArgumentException.class, () -> {
            AntigravityBackend.SecurityManager.validateQuery(longQuery.toString());
        });
    }

    @Test
    @DisplayName("Null byte injection in file path throws SecurityException")
    void testNullByteInjectionInPath() {
        assertThrows(SecurityException.class, () -> {
            AntigravityBackend.SecurityManager.validateAndResolvePath("corpus\0/secret.txt", Paths.get("data"));
        });
    }
}
