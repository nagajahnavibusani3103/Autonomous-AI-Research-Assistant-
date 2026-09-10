package com.antigravity;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.BsonDocument;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autonomous AI Research Assistant - Core Backend Engine
 *
 * This class implements the complete local Information Retrieval and NLP pipeline:
 *  - Corpus ingestion and validation
 *  - Text preprocessing: tokenization, stop-word removal, Porter stemming, sentence segmentation
 *  - Inverted index construction with term frequencies, posting lists, and Euclidean vector norms
 *  - Smoothed TF-IDF weighting and sparse vector cosine similarity
 *  - Bounded Top-K min-heap ranking with explainable scoring breakdowns
 *  - TextRank graph-based extractive summarization (PageRank on sentence similarity network)
 *  - Salient keyword extraction using TF-IDF and title prominence heuristics
 *  - Autonomous research orchestrator: deterministic question decomposition, multi-query retrieval,
 *    Jaccard-based passage deduplication, and provenance citation mapping ([C1], [C2])
 *  - Dual persistence abstraction: official MongoDB driver with robust local JSON fallback
 *  - Empirical complexity telemetry and verification engine
 *  - Comprehensive security controls: path traversal blocking and credential masking
 */
public final class AntigravityBackend {

    private AntigravityBackend() {
        // Utility class with nested modular components
    }

    // ========================================================================
    // 1. SECURITY & INPUT VALIDATION
    // ========================================================================

    public static final class SecurityManager {
        // Regex patterns to identify and mask sensitive database credentials in logs
        private static final Pattern MONGO_URI_CREDENTIAL_PATTERN =
                Pattern.compile("(mongodb(?:\\+srv)?://)([^:@/]+):([^@/]+)@");
        private static final Pattern KEY_VALUE_SECRET_PATTERN =
                Pattern.compile("(?i)(password|secret|token|apikey)\\s*[:=]\\s*['\"]?([^'\"\\s]+)['\"]?");

        // Resource safety bounds to prevent Denial of Service and Out-Of-Memory conditions
        private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L; // 10 MB limit
        private static final int MAX_QUERY_LENGTH = 500;
        private static final int MAX_K_VALUE = 100;

        /**
         * Masks passwords and connection secrets before printing to logs or console.
         */
        public static String sanitize(String input) {
            if (input == null) return null;
            String sanitized = MONGO_URI_CREDENTIAL_PATTERN.matcher(input).replaceAll("$1***:***@");
            sanitized = KEY_VALUE_SECRET_PATTERN.matcher(sanitized).replaceAll("$1=***");
            return sanitized;
        }

        /**
         * Validates and resolves user-supplied file paths against path traversal attacks.
         * Detects null bytes and verifies the path stays within the intended base directory.
         */
        public static Path validateAndResolvePath(String pathString, Path baseDirectory) {
            if (pathString == null || pathString.trim().isEmpty()) {
                throw new IllegalArgumentException("Path must not be null or empty.");
            }
            if (pathString.contains("\0")) {
                throw new SecurityException("Null byte injection detected in path: " + pathString);
            }
            Path rawPath = Paths.get(pathString.trim());
            Path resolvedPath = baseDirectory != null ? baseDirectory.resolve(rawPath).normalize() : rawPath.normalize();

            if (baseDirectory != null) {
                Path normalizedBase = baseDirectory.toAbsolutePath().normalize();
                Path normalizedResolved = resolvedPath.toAbsolutePath().normalize();
                if (!normalizedResolved.startsWith(normalizedBase)) {
                    throw new SecurityException("Path traversal attempt blocked: " + pathString);
                }
            }
            return resolvedPath;
        }

        public static void validateQuery(String query) {
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("Query string cannot be null or empty.");
            }
            if (query.length() > MAX_QUERY_LENGTH) {
                throw new IllegalArgumentException("Query length exceeds maximum limit of " + MAX_QUERY_LENGTH + " characters.");
            }
        }

        public static int validateK(int requestedK, int defaultK) {
            if (requestedK <= 0) return defaultK;
            if (requestedK > MAX_K_VALUE) return MAX_K_VALUE;
            return requestedK;
        }

        public static void validateFileSize(Path filePath) throws IOException {
            if (Files.exists(filePath) && Files.size(filePath) > MAX_FILE_SIZE_BYTES) {
                throw new SecurityException("File size exceeds maximum safety limit of 10 MB: " + filePath);
            }
        }
    }

    // ========================================================================
    // 2. AUDIT LOGGING
    // ========================================================================

    public static final class AuditLogger {
        private static final Logger LOGGER = Logger.getLogger("AntigravityAudit");

        static {
            LOGGER.setUseParentHandlers(false);
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.INFO);
            handler.setFormatter(new java.util.logging.Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%s] [%-7s] %s%n",
                            Instant.ofEpochMilli(record.getMillis()),
                            record.getLevel().getName(),
                            SecurityManager.sanitize(record.getMessage()));
                }
            });
            LOGGER.addHandler(handler);
            LOGGER.setLevel(Level.INFO);
        }

        public static void setLogLevel(String levelName) {
            try {
                Level level = Level.parse(levelName.toUpperCase(Locale.ENGLISH));
                LOGGER.setLevel(level);
                for (Handler h : LOGGER.getHandlers()) {
                    h.setLevel(level);
                }
            } catch (Exception e) {
                LOGGER.warning("Invalid log level: " + levelName + ". Retaining INFO level.");
            }
        }

        public static void info(String message) {
            LOGGER.info(message);
        }

        public static void warning(String message) {
            LOGGER.warning(message);
        }

        public static void severe(String message, Throwable throwable) {
            if (throwable != null) {
                LOGGER.log(Level.SEVERE, message + " Details: " + SecurityManager.sanitize(throwable.getMessage()));
            } else {
                LOGGER.severe(message);
            }
        }
    }

    // ========================================================================
    // 3. CONFIGURATION MANAGEMENT
    // ========================================================================

    public static final class ConfigurationManager {
        private static final Properties PROPERTIES = new Properties();

        static {
            loadDefaultProperties();
        }

        private static void loadDefaultProperties() {
            try (InputStream in = ConfigurationManager.class.getClassLoader()
                    .getResourceAsStream("config.example.properties")) {
                if (in != null) {
                    PROPERTIES.load(in);
                }
            } catch (Exception ignored) {
            }
            Path localProps = Paths.get("config.properties");
            if (Files.exists(localProps)) {
                try (InputStream in = Files.newInputStream(localProps)) {
                    PROPERTIES.load(in);
                } catch (Exception ignored) {
                }
            }
        }

        public static String get(String key, String defaultValue) {
            String envKey = key.toUpperCase(Locale.ENGLISH).replace('.', '_');
            String envValue = System.getenv(envKey);
            if (envValue != null && !envValue.trim().isEmpty()) {
                return envValue.trim();
            }
            String sysValue = System.getProperty(key);
            if (sysValue != null && !sysValue.trim().isEmpty()) {
                return sysValue.trim();
            }
            return PROPERTIES.getProperty(key, defaultValue);
        }

        public static int getInt(String key, int defaultValue) {
            try {
                return Integer.parseInt(get(key, String.valueOf(defaultValue)));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public static double getDouble(String key, double defaultValue) {
            try {
                return Double.parseDouble(get(key, String.valueOf(defaultValue)));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public static String getMongoUri() {
            String uri = System.getenv("MONGODB_URI");
            if (uri == null || uri.trim().isEmpty()) {
                uri = PROPERTIES.getProperty("mongodb.uri", null);
            }
            return (uri != null && !uri.trim().isEmpty()) ? uri.trim() : null;
        }

        public static String getMongoDatabase() {
            return get("mongodb.database", "antigravity_research");
        }

        public static String getCorpusPath() {
            return get("corpus.path", "data/sample");
        }

        public static int getTopK() {
            return getInt("top.k", 5);
        }

        public static double getMinScore() {
            return getDouble("min.score", 0.005);
        }

        public static Map<String, String> getSafeConfigView() {
            Map<String, String> view = new LinkedHashMap<>();
            view.put("corpus.path", getCorpusPath());
            view.put("top.k", String.valueOf(getTopK()));
            view.put("min.score", String.valueOf(getMinScore()));
            view.put("mongodb.database", getMongoDatabase());
            String uri = getMongoUri();
            view.put("mongodb.uri", uri != null ? SecurityManager.sanitize(uri) : "NOT_CONFIGURED (Using FileRepository fallback)");
            view.put("log.level", get("log.level", "INFO"));
            return view;
        }
    }

    // ========================================================================
    // 4. DOMAIN RECORDS & DATA MODELS
    // ========================================================================

    public record Document(
            String docId,
            String title,
            String source,
            String content,
            Map<String, String> metadata,
            List<String> tokens,
            Instant createdAt
    ) implements Serializable {
        public Document withTokens(List<String> processedTokens) {
            return new Document(docId, title, source, content, metadata, processedTokens, createdAt);
        }
    }

    public record Posting(
            String docId,
            int termFrequency,
            List<Integer> positions
    ) implements Serializable {}

    public record SearchResult(
            String docId,
            String title,
            double score,
            List<String> matchedTerms,
            String snippet,
            String source,
            int rank,
            String explanation
    ) implements Serializable {}

    public record EvidenceSnippet(
            String citationId,
            String claimOrEvidence,
            String docId,
            String title,
            String passage,
            double score,
            String source
    ) implements Serializable {}

    public record ResearchReport(
            String question,
            List<String> researchPlan,
            List<String> subQueries,
            List<String> keyFindings,
            List<EvidenceSnippet> evidenceList,
            Map<String, String> citations,
            List<Map.Entry<String, Double>> topKeywords,
            List<String> sources,
            double evidenceCoverage,
            long queryLatencyMs,
            int documentsRetrieved
    ) implements Serializable {}

    public record SystemStats(
            int documentsCount,
            int vocabularySize,
            long totalTokens,
            double avgDocLength,
            long indexSizeBytes,
            String databaseMode,
            Instant lastIndexBuildTime
    ) implements Serializable {}

    public record ComplexityProfile(
            int corpusDocuments,
            int vocabularySize,
            long totalTokens,
            double avgDocumentLength,
            String lastQuery,
            int queryTermsCount,
            int candidateDocumentsCount,
            int postingsTraversedCount,
            int topKRequested,
            long executionTimeNanos,
            double executionTimeMs
    ) implements Serializable {}

    public record EvaluationMetrics(
            double precisionAtK,
            double recallAtK,
            double f1AtK,
            double mrr,
            double ndcgAtK,
            double avgLatencyMs,
            double p95LatencyMs,
            long indexBuildTimeMs,
            int corpusSize,
            double baselinePrecision,
            double baselineRecall,
            double baselineF1,
            double baselineMRR
    ) implements Serializable {}

    public record HealthReport(
            String javaRuntimeStatus,
            String corpusStatus,
            String indexStatus,
            String mongoStatus,
            String configStatus,
            String overallStatus,
            Map<String, String> details
    ) implements Serializable {}

    public record QueryLog(
            String queryId,
            String queryText,
            Instant timestamp,
            List<String> resultDocIds,
            long latencyMs
    ) implements Serializable {}

    public record ResearchSession(
            String sessionId,
            String question,
            List<String> subQueries,
            List<EvidenceSnippet> evidence,
            Map<String, String> citations,
            String summary,
            Instant timestamp
    ) implements Serializable {}

    // ========================================================================
    // 5. NATURAL LANGUAGE PROCESSING (NLP) PIPELINE
    // ========================================================================

    public static final class Tokenizer {
        // Alphanumeric pattern preserving internal hyphens (e.g. 'zero-day', 'machine-learning')
        private static final Pattern WORD_BOUNDARY_PATTERN = Pattern.compile("[a-zA-Z0-9]+(?:[-'][a-zA-Z0-9]+)*");

        public static List<String> tokenize(String text) {
            if (text == null || text.trim().isEmpty()) {
                return Collections.emptyList();
            }
            List<String> tokens = new ArrayList<>();
            Matcher matcher = WORD_BOUNDARY_PATTERN.matcher(text.toLowerCase(Locale.ENGLISH));
            while (matcher.find()) {
                String token = matcher.group();
                if (token.length() > 1 || Character.isLetterOrDigit(token.charAt(0))) {
                    tokens.add(token);
                }
            }
            return tokens;
        }
    }

    public static final class StopWordFilter {
        // Comprehensive set of standard English syntactic stop words for O(1) hash lookup
        private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
                "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
                "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
                "below", "between", "both", "but", "by", "can", "can't", "cannot", "could",
                "couldn't", "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down",
                "during", "each", "few", "for", "from", "further", "had", "hadn't", "has",
                "hasn't", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her",
                "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's",
                "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it",
                "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my",
                "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or",
                "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same",
                "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so",
                "some", "such", "than", "that", "that's", "the", "their", "theirs", "them",
                "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll",
                "they're", "they've", "this", "those", "through", "to", "too", "under", "until",
                "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've", "were",
                "weren't", "what", "what's", "when", "when's", "where", "where's", "which",
                "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
                "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours",
                "yourself", "yourselves", "also", "thus", "however", "therefore", "furthermore"
        ));

        public static boolean isStopWord(String word) {
            return word == null || STOP_WORDS.contains(word.toLowerCase(Locale.ENGLISH));
        }

        public static List<String> filter(List<String> tokens) {
            if (tokens == null) return Collections.emptyList();
            List<String> contentBearingTokens = new ArrayList<>(tokens.size());
            for (String t : tokens) {
                // Keep tokens that are non-stop words and at least 2 characters long
                if (!isStopWord(t) && t.length() > 1) {
                    contentBearingTokens.add(t);
                }
            }
            return contentBearingTokens;
        }
    }

    /**
     * Pure Java implementation of Martin Porter's 1980 morphological stemmer.
     * Conforms to the standard linguistic reference for English IR systems.
     */
    public static final class Stemmer {
        public static String stem(String word) {
            if (word == null || word.length() < 3) return word;
            String w = word.toLowerCase(Locale.ENGLISH);
            w = step1a(w);
            w = step1b(w);
            w = step1c(w);
            w = step2(w);
            w = step3(w);
            w = step4(w);
            w = step5a(w);
            w = step5b(w);
            return w;
        }

        private static boolean isConsonant(String str, int index) {
            char c = str.charAt(index);
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') return false;
            if (c == 'y') {
                return (index == 0) || !isConsonant(str, index - 1);
            }
            return true;
        }

        private static int getMeasure(String str) {
            int measure = 0;
            int i = 0;
            int len = str.length();
            while (i < len && isConsonant(str, i)) i++;
            while (i < len) {
                while (i < len && !isConsonant(str, i)) i++;
                while (i < len && isConsonant(str, i)) i++;
                measure++;
            }
            return measure;
        }

        private static boolean containsVowel(String str) {
            for (int i = 0; i < str.length(); i++) {
                if (!isConsonant(str, i)) return true;
            }
            return false;
        }

        private static boolean endsWithDoubleConsonant(String str) {
            int len = str.length();
            if (len < 2) return false;
            return str.charAt(len - 1) == str.charAt(len - 2) && isConsonant(str, len - 1);
        }

        private static boolean cvc(String str) {
            int len = str.length();
            if (len < 3) return false;
            if (!isConsonant(str, len - 1) || isConsonant(str, len - 2) || !isConsonant(str, len - 3)) {
                return false;
            }
            char last = str.charAt(len - 1);
            return last != 'w' && last != 'x' && last != 'y';
        }

        private static String step1a(String w) {
            if (w.endsWith("sses")) return w.substring(0, w.length() - 2);
            if (w.endsWith("ies")) return w.substring(0, w.length() - 2);
            if (w.endsWith("ss")) return w;
            if (w.endsWith("s")) return w.substring(0, w.length() - 1);
            return w;
        }

        private static String step1b(String w) {
            if (w.endsWith("eed")) {
                String stem = w.substring(0, w.length() - 3);
                if (getMeasure(stem) > 0) return stem + "ee";
                return w;
            }
            boolean matched = false;
            String stem = null;
            if (w.endsWith("ed")) {
                stem = w.substring(0, w.length() - 2);
                if (containsVowel(stem)) matched = true;
            } else if (w.endsWith("ing")) {
                stem = w.substring(0, w.length() - 3);
                if (containsVowel(stem)) matched = true;
            }
            if (matched && stem != null) {
                if (stem.endsWith("at") || stem.endsWith("bl") || stem.endsWith("iz")) {
                    return stem + "e";
                }
                if (endsWithDoubleConsonant(stem)) {
                    char c = stem.charAt(stem.length() - 1);
                    if (c != 'l' && c != 's' && c != 'z') {
                        return stem.substring(0, stem.length() - 1);
                    }
                    return stem;
                }
                if (getMeasure(stem) == 1 && cvc(stem)) {
                    return stem + "e";
                }
                return stem;
            }
            return w;
        }

        private static String step1c(String w) {
            if (w.endsWith("y")) {
                String stem = w.substring(0, w.length() - 1);
                if (containsVowel(stem)) {
                    return stem + "i";
                }
            }
            return w;
        }

        private static String step2(String w) {
            String[][] rules = {
                    {"ational", "ate"}, {"tional", "tion"}, {"enci", "ence"}, {"anci", "ance"},
                    {"izer", "ize"}, {"abli", "able"}, {"alli", "al"}, {"entli", "ent"},
                    {"eli", "e"}, {"ousli", "ous"}, {"ization", "ize"}, {"ation", "ate"},
                    {"ator", "ate"}, {"alism", "al"}, {"iveness", "ive"}, {"fulness", "ful"},
                    {"ousness", "ous"}, {"aliti", "al"}, {"iviti", "ive"}, {"biliti", "ble"}
            };
            for (String[] rule : rules) {
                if (w.endsWith(rule[0])) {
                    String stem = w.substring(0, w.length() - rule[0].length());
                    if (getMeasure(stem) > 0) return stem + rule[1];
                    return w;
                }
            }
            return w;
        }

        private static String step3(String w) {
            String[][] rules = {
                    {"icate", "ic"}, {"ative", ""}, {"alize", "al"},
                    {"iciti", "ic"}, {"ical", "ic"}, {"ful", ""}, {"ness", ""}
            };
            for (String[] rule : rules) {
                if (w.endsWith(rule[0])) {
                    String stem = w.substring(0, w.length() - rule[0].length());
                    if (getMeasure(stem) > 0) return stem + rule[1];
                    return w;
                }
            }
            return w;
        }

        private static String step4(String w) {
            String[] suffixes = {
                    "al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement",
                    "ment", "ent", "ou", "ism", "ate", "iti", "ous", "ive", "ize"
            };
            for (String s : suffixes) {
                if (w.endsWith(s)) {
                    String stem = w.substring(0, w.length() - s.length());
                    if (getMeasure(stem) > 1) return stem;
                    return w;
                }
            }
            if (w.endsWith("sion") || w.endsWith("tion")) {
                String stem = w.substring(0, w.length() - 3);
                if (getMeasure(stem) > 1) return stem;
            }
            return w;
        }

        private static String step5a(String w) {
            if (w.endsWith("e")) {
                String stem = w.substring(0, w.length() - 1);
                int m = getMeasure(stem);
                if (m > 1 || (m == 1 && !cvc(stem))) {
                    return stem;
                }
            }
            return w;
        }

        private static String step5b(String w) {
            if (getMeasure(w) > 1 && endsWithDoubleConsonant(w) && w.endsWith("l")) {
                return w.substring(0, w.length() - 1);
            }
            return w;
        }
    }

    public static final class SentenceSegmenter {
        // Splits text at sentence boundaries (. ! ?) followed by uppercase letter or quote
        private static final Pattern SENTENCE_SPLIT_PATTERN =
                Pattern.compile("(?<=[.!?])\\s+(?=[A-Z0-9\"])", Pattern.MULTILINE);

        public static List<String> segment(String text) {
            if (text == null || text.trim().isEmpty()) {
                return Collections.emptyList();
            }
            String[] rawSentences = SENTENCE_SPLIT_PATTERN.split(text.trim());
            List<String> validSentences = new ArrayList<>();
            for (String s : rawSentences) {
                String cleaned = s.replaceAll("\\s+", " ").trim();
                // Retain meaningful sentence units (avoid isolated punctuation or trivial fragments)
                if (cleaned.length() > 15) {
                    validSentences.add(cleaned);
                }
            }
            return validSentences;
        }
    }

    public static final class TextPreprocessor {
        /**
         * Reusable NLP pipeline: tokenizes, removes stop words, and stems each token.
         * Used consistently across document ingestion, query preparation, and summarization
         * to guarantee vocabulary terms match exactly in vector space.
         */
        public static List<String> preprocess(String text) {
            List<String> rawTokens = Tokenizer.tokenize(text);
            List<String> filteredTokens = StopWordFilter.filter(rawTokens);
            List<String> stemmedTokens = new ArrayList<>(filteredTokens.size());
            for (String token : filteredTokens) {
                stemmedTokens.add(Stemmer.stem(token));
            }
            return stemmedTokens;
        }
    }

    // ========================================================================
    // 6. INVERTED INDEX & VECTOR SPACE MODEL
    // ========================================================================

    public static final class InvertedIndex {
        // Inverted map: term -> list of document postings with term frequencies and positions
        private final Map<String, List<Posting>> termPostings = new HashMap<>();
        // Fast lookup: term -> (docId -> termFrequency)
        private final Map<String, Map<String, Integer>> termDocFrequencies = new HashMap<>();
        private final Map<String, Integer> documentLengths = new HashMap<>();
        private final Map<String, Double> documentVectorNorms = new HashMap<>();
        private final Map<String, Document> documents = new HashMap<>();
        private long totalTokens = 0;
        private Instant lastBuildTime = null;

        public synchronized void addDocument(Document doc) {
            List<String> tokens = TextPreprocessor.preprocess(doc.content() + " " + doc.title());
            Document indexedDoc = doc.withTokens(tokens);
            documents.put(doc.docId(), indexedDoc);
            documentLengths.put(doc.docId(), tokens.size());
            totalTokens += tokens.size();

            Map<String, List<Integer>> positionsMap = new HashMap<>();
            for (int i = 0; i < tokens.size(); i++) {
                String term = tokens.get(i);
                positionsMap.computeIfAbsent(term, k -> new ArrayList<>()).add(i);
            }

            for (Map.Entry<String, List<Integer>> entry : positionsMap.entrySet()) {
                String term = entry.getKey();
                List<Integer> positions = entry.getValue();
                int tf = positions.size();

                termPostings.computeIfAbsent(term, k -> new ArrayList<>())
                        .add(new Posting(doc.docId(), tf, positions));

                termDocFrequencies.computeIfAbsent(term, k -> new HashMap<>())
                        .put(doc.docId(), tf);
            }
        }

        /**
         * Precomputes Euclidean L2 vector norms for all documents:
         *   ||D|| = sqrt( sum( (TF(t,d) * IDF(t))^2 ) )
         * Precomputing norms prevents repeated O(V) passes during query execution.
         */
        public synchronized void computeVectorNorms() {
            documentVectorNorms.clear();
            int totalDocs = documents.size();
            if (totalDocs == 0) return;

            Map<String, Double> sumOfSquaresMap = new HashMap<>();
            for (Map.Entry<String, Map<String, Integer>> entry : termDocFrequencies.entrySet()) {
                String term = entry.getKey();
                Map<String, Integer> docMap = entry.getValue();
                int documentFrequency = docMap.size();
                double idf = TFIDFEngine.calculateSmoothedIdf(totalDocs, documentFrequency);

                for (Map.Entry<String, Integer> docEntry : docMap.entrySet()) {
                    String docId = docEntry.getKey();
                    int tf = docEntry.getValue();
                    double tfWeight = TFIDFEngine.calculateSublinearTf(tf);
                    double termWeight = tfWeight * idf;
                    sumOfSquaresMap.put(docId, sumOfSquaresMap.getOrDefault(docId, 0.0) + (termWeight * termWeight));
                }
            }

            for (Map.Entry<String, Double> e : sumOfSquaresMap.entrySet()) {
                documentVectorNorms.put(e.getKey(), Math.sqrt(e.getValue()));
            }
            lastBuildTime = Instant.now();
        }

        public List<Posting> getPostings(String term) {
            return termPostings.getOrDefault(term, Collections.emptyList());
        }

        public int getDocumentFrequency(String term) {
            Map<String, Integer> docMap = termDocFrequencies.get(term);
            return docMap != null ? docMap.size() : 0;
        }

        public int getTermFrequency(String term, String docId) {
            Map<String, Integer> docMap = termDocFrequencies.get(term);
            if (docMap != null) {
                return docMap.getOrDefault(docId, 0);
            }
            return 0;
        }

        /**
         * Retrieves candidate document IDs containing at least one query term.
         * Also returns the number of postings traversed for empirical complexity tracking.
         */
        public Set<String> getCandidateDocIds(Collection<String> queryTerms, int[] outPostingsExamined) {
            Set<String> candidates = new HashSet<>();
            int examined = 0;
            for (String term : queryTerms) {
                List<Posting> postings = termPostings.get(term);
                if (postings != null) {
                    examined += postings.size();
                    for (Posting p : postings) {
                        candidates.add(p.docId());
                    }
                }
            }
            if (outPostingsExamined != null && outPostingsExamined.length > 0) {
                outPostingsExamined[0] = examined;
            }
            return candidates;
        }

        public int getDocumentCount() {
            return documents.size();
        }

        public int getVocabularySize() {
            return termPostings.size();
        }

        public long getTotalTokens() {
            return totalTokens;
        }

        public double getAverageDocumentLength() {
            if (documents.isEmpty()) return 0.0;
            return (double) totalTokens / documents.size();
        }

        public Document getDocument(String docId) {
            return documents.get(docId);
        }

        public Collection<Document> getAllDocuments() {
            return Collections.unmodifiableCollection(documents.values());
        }

        public Instant getLastBuildTime() {
            return lastBuildTime;
        }

        public double getDocVectorNorm(String docId) {
            return documentVectorNorms.getOrDefault(docId, 1.0);
        }

        public synchronized void clear() {
            termPostings.clear();
            termDocFrequencies.clear();
            documentLengths.clear();
            documentVectorNorms.clear();
            documents.clear();
            totalTokens = 0;
            lastBuildTime = null;
        }
    }

    // ========================================================================
    // 7. TF-IDF & SIMILARITY ENGINE
    // ========================================================================

    public static final class TFIDFEngine {
        /**
         * Smoothed Inverse Document Frequency:
         *   IDF(t) = ln((N + 1) / (df(t) + 1)) + 1.0
         *
         * The '+1' smoothing inside the logarithm prevents division by zero for unseen terms
         * and avoids negative weights when df(t) == N.
         */
        public static double calculateSmoothedIdf(int totalDocs, int docFreq) {
            if (totalDocs <= 0) return 1.0;
            return Math.log(((double) totalDocs + 1.0) / ((double) docFreq + 1.0)) + 1.0;
        }

        /**
         * Sublinear Term Frequency scaling:
         *   TF(t, d) = 1 + ln(f_{t,d}) if f_{t,d} > 0 else 0
         *
         * Prevents a document with 10 occurrences of a term from dominating 10x over a document
         * with 1 occurrence, adhering to standard information retrieval theory.
         */
        public static double calculateSublinearTf(int frequency) {
            if (frequency <= 0) return 0.0;
            return 1.0 + Math.log(frequency);
        }

        public static double calculateTfIdf(int frequency, int totalDocs, int docFreq) {
            if (frequency <= 0) return 0.0;
            return calculateSublinearTf(frequency) * calculateSmoothedIdf(totalDocs, docFreq);
        }
    }

    public static final class SimilarityEngine {
        /**
         * Sparse Vector Cosine Similarity:
         *   Cosine(Q, D) = (Q . D) / (||Q|| * ||D||)
         *
         * By iterating strictly over non-zero query terms instead of full vocabulary vectors,
         * computation time drops from O(V) to O(Q) per candidate document.
         */
        public static double calculateCosineSimilarity(
                Map<String, Double> queryWeights,
                double queryNorm,
                String docId,
                InvertedIndex index
        ) {
            if (queryNorm == 0.0) return 0.0;
            double docNorm = index.getDocVectorNorm(docId);
            if (docNorm == 0.0) return 0.0;

            double dotProduct = 0.0;
            int totalDocs = index.getDocumentCount();

            for (Map.Entry<String, Double> entry : queryWeights.entrySet()) {
                String term = entry.getKey();
                double qWeight = entry.getValue();

                int tf = index.getTermFrequency(term, docId);
                if (tf > 0) {
                    int df = index.getDocumentFrequency(term);
                    double idf = TFIDFEngine.calculateSmoothedIdf(totalDocs, df);
                    double docTfWeight = TFIDFEngine.calculateSublinearTf(tf);
                    double docWeight = docTfWeight * idf;
                    dotProduct += (qWeight * docWeight);
                }
            }

            return dotProduct / (queryNorm * docNorm);
        }
    }

    // ========================================================================
    // 8. RANKING ENGINE WITH COMPLEXITY TRACKING
    // ========================================================================

    public static final class RankingEngine {
        private final InvertedIndex index;

        // Telemetry counters for the last executed query (used by 'complexity' command)
        private volatile String lastQueryText = "";
        private volatile int lastQueryTermsCount = 0;
        private volatile int lastCandidatesCount = 0;
        private volatile int lastPostingsTraversed = 0;
        private volatile int lastTopKRequested = 0;
        private volatile long lastExecutionNanos = 0;

        public RankingEngine(InvertedIndex index) {
            this.index = Objects.requireNonNull(index, "Index cannot be null");
        }

        public List<SearchResult> search(String queryString, int topK, double minScore) {
            long startNanos = System.nanoTime();
            SecurityManager.validateQuery(queryString);
            List<String> queryTerms = TextPreprocessor.preprocess(queryString);
            if (queryTerms.isEmpty()) {
                recordTelemetry(queryString, 0, 0, 0, topK, System.nanoTime() - startNanos);
                return Collections.emptyList();
            }

            int totalDocs = index.getDocumentCount();
            if (totalDocs == 0) {
                recordTelemetry(queryString, queryTerms.size(), 0, 0, topK, System.nanoTime() - startNanos);
                return Collections.emptyList();
            }

            // Calculate query vector weights
            Map<String, Integer> qTermFreq = new HashMap<>();
            for (String t : queryTerms) {
                qTermFreq.put(t, qTermFreq.getOrDefault(t, 0) + 1);
            }

            Map<String, Double> queryWeights = new HashMap<>();
            double qSumSq = 0.0;
            for (Map.Entry<String, Integer> entry : qTermFreq.entrySet()) {
                String term = entry.getKey();
                int df = index.getDocumentFrequency(term);
                double idf = TFIDFEngine.calculateSmoothedIdf(totalDocs, df);
                double weight = TFIDFEngine.calculateSublinearTf(entry.getValue()) * idf;
                queryWeights.put(term, weight);
                qSumSq += (weight * weight);
            }
            double queryNorm = Math.sqrt(qSumSq);

            // Inverted index candidate pruning (bypasses non-matching documents)
            int[] postingsCounter = new int[1];
            Set<String> candidateDocIds = index.getCandidateDocIds(queryWeights.keySet(), postingsCounter);
            if (candidateDocIds.isEmpty()) {
                recordTelemetry(queryString, queryWeights.size(), 0, postingsCounter[0], topK, System.nanoTime() - startNanos);
                return Collections.emptyList();
            }

            // Bounded Min-Heap PriorityQueue ensures Top-K selection in O(M log K) instead of full O(M log M) sort
            PriorityQueue<SearchResult> minHeap = new PriorityQueue<>(
                    Comparator.comparingDouble(SearchResult::score)
            );

            for (String docId : candidateDocIds) {
                double score = SimilarityEngine.calculateCosineSimilarity(queryWeights, queryNorm, docId, index);
                if (score < minScore) continue;

                Document doc = index.getDocument(docId);
                List<String> matchedTerms = new ArrayList<>();
                for (String qTerm : queryWeights.keySet()) {
                    if (index.getTermFrequency(qTerm, docId) > 0) {
                        matchedTerms.add(qTerm);
                    }
                }

                String snippet = extractBestSentenceSnippet(doc.content(), matchedTerms);
                String explanation = String.format(
                        "Relevance Score: %.4f | Matched %d/%d query terms | Sparse Cosine Alignment.",
                        score, matchedTerms.size(), queryWeights.keySet().size()
                );

                SearchResult searchResult = new SearchResult(
                        docId,
                        doc.title(),
                        score,
                        matchedTerms,
                        snippet,
                        doc.source(),
                        0,
                        explanation
                );

                if (minHeap.size() < topK) {
                    minHeap.offer(searchResult);
                } else if (minHeap.peek() != null && score > minHeap.peek().score()) {
                    minHeap.poll();
                    minHeap.offer(searchResult);
                }
            }

            List<SearchResult> results = new ArrayList<>();
            while (!minHeap.isEmpty()) {
                results.add(minHeap.poll());
            }
            Collections.reverse(results);

            List<SearchResult> rankedResults = new ArrayList<>(results.size());
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                rankedResults.add(new SearchResult(
                        r.docId(), r.title(), r.score(), r.matchedTerms(), r.snippet(), r.source(), i + 1, r.explanation()
                ));
            }

            recordTelemetry(queryString, queryWeights.size(), candidateDocIds.size(), postingsCounter[0], topK, System.nanoTime() - startNanos);
            return rankedResults;
        }

        private void recordTelemetry(String query, int qTerms, int candidates, int postings, int topK, long durationNanos) {
            this.lastQueryText = query;
            this.lastQueryTermsCount = qTerms;
            this.lastCandidatesCount = candidates;
            this.lastPostingsTraversed = postings;
            this.lastTopKRequested = topK;
            this.lastExecutionNanos = durationNanos;
        }

        public ComplexityProfile getLastComplexityProfile() {
            return new ComplexityProfile(
                    index.getDocumentCount(),
                    index.getVocabularySize(),
                    index.getTotalTokens(),
                    index.getAverageDocumentLength(),
                    lastQueryText,
                    lastQueryTermsCount,
                    lastCandidatesCount,
                    lastPostingsTraversed,
                    lastTopKRequested,
                    lastExecutionNanos,
                    (double) lastExecutionNanos / 1_000_000.0
            );
        }

        /**
         * Okapi BM25 Ranking Mode (for comparative benchmarking)
         * Parameters: k1 = 1.2, b = 0.75
         */
        public List<SearchResult> searchBM25(String queryString, int topK) {
            List<String> queryTerms = TextPreprocessor.preprocess(queryString);
            if (queryTerms.isEmpty()) return Collections.emptyList();

            int totalDocs = index.getDocumentCount();
            if (totalDocs == 0) return Collections.emptyList();
            double avgDocLen = index.getAverageDocumentLength();
            if (avgDocLen == 0) avgDocLen = 1.0;

            final double k1 = 1.2;
            final double b = 0.75;

            Set<String> candidates = index.getCandidateDocIds(queryTerms, null);
            PriorityQueue<SearchResult> minHeap = new PriorityQueue<>(Comparator.comparingDouble(SearchResult::score));

            for (String docId : candidates) {
                Document doc = index.getDocument(docId);
                double docLen = doc.tokens() != null ? doc.tokens().size() : 1.0;
                double bm25Score = 0.0;
                List<String> matched = new ArrayList<>();

                for (String term : queryTerms) {
                    int tf = index.getTermFrequency(term, docId);
                    if (tf > 0) {
                        matched.add(term);
                        int df = index.getDocumentFrequency(term);
                        double idf = Math.log(((totalDocs - df + 0.5) / (df + 0.5)) + 1.0);
                        double numerator = tf * (k1 + 1.0);
                        double denominator = tf + k1 * (1.0 - b + b * (docLen / avgDocLen));
                        bm25Score += (idf * (numerator / denominator));
                    }
                }

                if (bm25Score > 0) {
                    String snippet = extractBestSentenceSnippet(doc.content(), matched);
                    SearchResult sr = new SearchResult(
                            docId, doc.title(), bm25Score, matched, snippet, doc.source(), 0, "BM25 score: " + String.format("%.4f", bm25Score)
                    );
                    if (minHeap.size() < topK) {
                        minHeap.offer(sr);
                    } else if (minHeap.peek() != null && bm25Score > minHeap.peek().score()) {
                        minHeap.poll();
                        minHeap.offer(sr);
                    }
                }
            }

            List<SearchResult> results = new ArrayList<>();
            while (!minHeap.isEmpty()) results.add(minHeap.poll());
            Collections.reverse(results);
            List<SearchResult> ranked = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                ranked.add(new SearchResult(r.docId(), r.title(), r.score(), r.matchedTerms(), r.snippet(), r.source(), i + 1, r.explanation()));
            }
            return ranked;
        }

        /**
         * Simple Lexical Baseline (raw unstemmed term count) for benchmark comparisons
         */
        public List<SearchResult> searchBaselineLexical(String queryString, int topK) {
            List<String> rawTokens = Tokenizer.tokenize(queryString);
            if (rawTokens.isEmpty()) return Collections.emptyList();

            PriorityQueue<SearchResult> minHeap = new PriorityQueue<>(Comparator.comparingDouble(SearchResult::score));
            for (Document doc : index.getAllDocuments()) {
                List<String> docTokens = Tokenizer.tokenize(doc.content() + " " + doc.title());
                Map<String, Integer> counts = new HashMap<>();
                for (String t : docTokens) counts.put(t, counts.getOrDefault(t, 0) + 1);

                double overlapScore = 0.0;
                List<String> matched = new ArrayList<>();
                for (String qt : rawTokens) {
                    if (counts.containsKey(qt)) {
                        overlapScore += counts.get(qt);
                        matched.add(qt);
                    }
                }

                if (overlapScore > 0) {
                    SearchResult sr = new SearchResult(
                            doc.docId(), doc.title(), overlapScore, matched, extractBestSentenceSnippet(doc.content(), matched),
                            doc.source(), 0, "Raw lexical term count baseline"
                    );
                    if (minHeap.size() < topK) minHeap.offer(sr);
                    else if (minHeap.peek() != null && overlapScore > minHeap.peek().score()) {
                        minHeap.poll();
                        minHeap.offer(sr);
                    }
                }
            }
            List<SearchResult> results = new ArrayList<>();
            while (!minHeap.isEmpty()) results.add(minHeap.poll());
            Collections.reverse(results);
            List<SearchResult> ranked = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                ranked.add(new SearchResult(r.docId(), r.title(), r.score(), r.matchedTerms(), r.snippet(), r.source(), i + 1, r.explanation()));
            }
            return ranked;
        }

        private static String extractBestSentenceSnippet(String content, List<String> matchedTerms) {
            List<String> sentences = SentenceSegmenter.segment(content);
            if (sentences.isEmpty()) {
                return content.length() > 160 ? content.substring(0, 157) + "..." : content;
            }

            String bestSentence = sentences.get(0);
            int maxMatches = -1;
            Set<String> termsSet = new HashSet<>(matchedTerms);

            for (String sentence : sentences) {
                List<String> stemmedSentence = TextPreprocessor.preprocess(sentence);
                int matches = 0;
                for (String st : stemmedSentence) {
                    if (termsSet.contains(st)) matches++;
                }
                if (matches > maxMatches) {
                    maxMatches = matches;
                    bestSentence = sentence;
                }
            }
            return bestSentence;
        }
    }

    // ========================================================================
    // 9. EXTRACTIVE SUMMARIZATION (TEXTRANK GRAPH ALGORITHM)
    // ========================================================================

    public static final class TextRankSummarizer {
        private static final double DAMPING_FACTOR = 0.85;
        private static final int MAX_ITERATIONS = 25;
        private static final double CONVERGENCE_EPSILON = 0.0001;

        /**
         * Extractive TextRank Summarization:
         * 1. Segments document into sentences.
         * 2. Constructs an undirected similarity graph where vertices = sentences,
         *    edge weights = normalized lexical token overlap.
         * 3. Computes PageRank random-walk scores until convergence.
         * 4. Selects top M scoring sentences and preserves original document order.
         *
         * GUARANTEE: Because this is purely extractive, sentences originate strictly
         * from the source text, eliminating synthetic hallucination.
         */
        public static String summarize(String text, int targetSentences) {
            if (text == null || text.trim().isEmpty()) return "";
            List<String> sentences = SentenceSegmenter.segment(text);
            if (sentences.size() <= targetSentences) {
                return String.join(" ", sentences);
            }

            int n = sentences.size();
            List<Set<String>> sentenceWordSets = new ArrayList<>(n);
            for (String s : sentences) {
                sentenceWordSets.add(new HashSet<>(TextPreprocessor.preprocess(s)));
            }

            // Build weighted similarity matrix
            double[][] similarityMatrix = new double[n][n];
            double[] vertexDegrees = new double[n];

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double sim = calculateSentenceOverlap(sentenceWordSets.get(i), sentenceWordSets.get(j));
                    similarityMatrix[i][j] = sim;
                    similarityMatrix[j][i] = sim;
                    vertexDegrees[i] += sim;
                    vertexDegrees[j] += sim;
                }
            }

            // PageRank iterative power calculation
            double[] scores = new double[n];
            Arrays.fill(scores, 1.0 / n);

            for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
                double[] nextScores = new double[n];
                double maxDifference = 0.0;

                for (int i = 0; i < n; i++) {
                    double sum = 0.0;
                    for (int j = 0; j < n; j++) {
                        if (i != j && vertexDegrees[j] > 0.0) {
                            sum += (similarityMatrix[j][i] / vertexDegrees[j]) * scores[j];
                        }
                    }
                    nextScores[i] = (1.0 - DAMPING_FACTOR) + (DAMPING_FACTOR * sum);
                    double diff = Math.abs(nextScores[i] - scores[i]);
                    if (diff > maxDifference) maxDifference = diff;
                }

                scores = nextScores;
                if (maxDifference < CONVERGENCE_EPSILON) break;
            }

            // Sort sentence indices by score descending, pick top targetSentences
            List<Integer> sortedIndices = new ArrayList<>(n);
            for (int i = 0; i < n; i++) sortedIndices.add(i);

            final double[] finalScores = scores;
            sortedIndices.sort((a, b) -> Double.compare(finalScores[b], finalScores[a]));

            List<Integer> topIndices = sortedIndices.subList(0, Math.min(targetSentences, n));
            Collections.sort(topIndices); // Restore original narrative sequence

            StringBuilder summaryBuilder = new StringBuilder();
            for (int index : topIndices) {
                if (summaryBuilder.length() > 0) summaryBuilder.append(" ");
                summaryBuilder.append(sentences.get(index));
            }
            return summaryBuilder.toString();
        }

        private static double calculateSentenceOverlap(Set<String> wordsA, Set<String> wordsB) {
            if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0;
            int common = 0;
            for (String w : wordsA) {
                if (wordsB.contains(w)) common++;
            }
            if (common == 0) return 0.0;
            // Logarithmic normalization prevents long sentences from dominating edge weights
            return (double) common / (Math.log(wordsA.size() + 1.0) + Math.log(wordsB.size() + 1.0));
        }
    }

    // ========================================================================
    // 10. KEYWORD EXTRACTION
    // ========================================================================

    public static final class KeywordExtractor {
        public static List<Map.Entry<String, Double>> extractKeywords(Document doc, InvertedIndex index, int topN) {
            if (doc == null || index == null) return Collections.emptyList();
            List<String> tokens = doc.tokens() != null ? doc.tokens() : TextPreprocessor.preprocess(doc.content());
            if (tokens.isEmpty()) return Collections.emptyList();

            int totalDocs = index.getDocumentCount();
            Map<String, Integer> termFrequencies = new HashMap<>();
            for (String t : tokens) {
                termFrequencies.put(t, termFrequencies.getOrDefault(t, 0) + 1);
            }

            Set<String> titleTokens = new HashSet<>(TextPreprocessor.preprocess(doc.title()));

            Map<String, Double> termScores = new HashMap<>();
            for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
                String term = entry.getKey();
                int tf = entry.getValue();
                int df = index.getDocumentFrequency(term);
                double idf = TFIDFEngine.calculateSmoothedIdf(totalDocs, df);
                double score = TFIDFEngine.calculateSublinearTf(tf) * idf;

                // Boost terms that also appear in the paper's title (high saliency heuristic)
                if (titleTokens.contains(term)) {
                    score *= 1.5;
                }
                termScores.put(term, score);
            }

            List<Map.Entry<String, Double>> sortedKeywords = new ArrayList<>(termScores.entrySet());
            sortedKeywords.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            return sortedKeywords.subList(0, Math.min(topN, sortedKeywords.size()));
        }
    }

    // ========================================================================
    // 11. AUTONOMOUS RESEARCH PLANNER & ORCHESTRATOR
    // ========================================================================

    public static final class ResearchPlanner {
        // Domain taxonomy mapping core technical themes to specific research facets
        private static final Map<String, List<String>> DOMAIN_TAXONOMY = new HashMap<>();

        static {
            DOMAIN_TAXONOMY.put("cybersecurity", Arrays.asList("intrusion detection", "malware classification", "anomaly detection", "network security"));
            DOMAIN_TAXONOMY.put("security", Arrays.asList("threat detection", "adversarial attacks", "zero trust architecture"));
            DOMAIN_TAXONOMY.put("machine learning", Arrays.asList("neural networks", "supervised classification", "unsupervised anomaly detection"));
            DOMAIN_TAXONOMY.put("deep learning", Arrays.asList("convolutional neural networks", "autoencoders", "packet stream inspection"));
            DOMAIN_TAXONOMY.put("retrieval", Arrays.asList("inverted index", "vector space model", "TF-IDF BM25 ranking"));
            DOMAIN_TAXONOMY.put("summarization", Arrays.asList("TextRank graph ranking", "extractive synthesis", "fact extraction"));
            DOMAIN_TAXONOMY.put("orchestration", Arrays.asList("deterministic workflows", "provenance citations", "evidence aggregation"));
        }

        /**
         * Deterministically decomposes an overarching technical question into 3-5 focused sub-queries.
         * Combines the primary question with domain concept expansions and keyword pairs.
         */
        public static List<String> generateSubQueries(String researchQuestion) {
            SecurityManager.validateQuery(researchQuestion);
            String normalizedQuestion = researchQuestion.toLowerCase(Locale.ENGLISH).trim();
            List<String> subQueries = new ArrayList<>();
            subQueries.add(researchQuestion); // Primary root query

            List<String> contentTokens = StopWordFilter.filter(Tokenizer.tokenize(normalizedQuestion));

            // Check domain taxonomy for matching conceptual facets
            for (Map.Entry<String, List<String>> entry : DOMAIN_TAXONOMY.entrySet()) {
                if (normalizedQuestion.contains(entry.getKey())) {
                    for (String facet : entry.getValue()) {
                        subQueries.add(facet);
                    }
                }
            }

            // Keyword pairwise combinations
            if (contentTokens.size() >= 2) {
                subQueries.add(contentTokens.get(0) + " " + contentTokens.get(1));
                if (contentTokens.size() >= 3) {
                    subQueries.add(contentTokens.get(1) + " " + contentTokens.get(2));
                }
            }

            // Deduplicate and bound to at most 4 sub-queries for efficient execution
            Set<String> seen = new LinkedHashSet<>();
            List<String> uniqueSubQueries = new ArrayList<>();
            for (String q : subQueries) {
                String clean = q.trim();
                if (!clean.isEmpty() && seen.add(clean)) {
                    uniqueSubQueries.add(clean);
                    if (uniqueSubQueries.size() >= 4) break;
                }
            }
            return uniqueSubQueries;
        }

        public static List<String> createPlan(String question, List<String> subQueries) {
            List<String> planSteps = new ArrayList<>();
            planSteps.add("Analyze question and identify domain facets: " + question);
            for (int i = 0; i < subQueries.size(); i++) {
                planSteps.add("Sub-query " + (i + 1) + ": Retrieve literature for '" + subQueries.get(i) + "'");
            }
            planSteps.add("Deduplicate candidate passages via Jaccard token overlap threshold (<= 0.60)");
            planSteps.add("Rank evidence by composite score and attach verifiable provenance citations");
            return planSteps;
        }
    }

    public static final class EvidenceCollector {
        public static List<EvidenceSnippet> collectEvidence(
                RankingEngine rankingEngine,
                InvertedIndex index,
                List<String> subQueries,
                int topDocsPerQuery
        ) {
            List<EvidenceSnippet> collectedEvidence = new ArrayList<>();
            Set<String> seenPassages = new HashSet<>();

            for (String subQuery : subQueries) {
                List<SearchResult> results = rankingEngine.search(subQuery, topDocsPerQuery, 0.005);
                for (SearchResult sr : results) {
                    Document doc = index.getDocument(sr.docId());
                    if (doc == null) continue;

                    List<String> sentences = SentenceSegmenter.segment(doc.content());
                    for (String sentence : sentences) {
                        if (isDuplicatePassage(sentence, seenPassages)) continue;

                        double passageRelevance = computeSentenceRelevance(sentence, subQuery);
                        if (passageRelevance > 0.15) {
                            seenPassages.add(sentence);
                            collectedEvidence.add(new EvidenceSnippet(
                                    "", // Citation ID assigned by ProvenanceManager
                                    "Evidence supporting query: " + subQuery,
                                    doc.docId(),
                                    doc.title(),
                                    sentence,
                                    sr.score() + passageRelevance,
                                    doc.source()
                            ));
                            break; // Take the highest scoring sentence per document
                        }
                    }
                }
            }

            // Rerank collected evidence passages by composite score
            collectedEvidence.sort((a, b) -> Double.compare(b.score(), a.score()));
            return collectedEvidence;
        }

        private static boolean isDuplicatePassage(String candidate, Set<String> existingPassages) {
            Set<String> candidateTokens = new HashSet<>(Tokenizer.tokenize(candidate));
            for (String existing : existingPassages) {
                Set<String> existingTokens = new HashSet<>(Tokenizer.tokenize(existing));
                int intersectionSize = 0;
                for (String token : candidateTokens) {
                    if (existingTokens.contains(token)) intersectionSize++;
                }
                int unionSize = candidateTokens.size() + existingTokens.size() - intersectionSize;
                // If Jaccard token overlap exceeds 60%, treat as redundant passage
                if (unionSize > 0 && ((double) intersectionSize / unionSize) > 0.60) {
                    return true;
                }
            }
            return false;
        }

        private static double computeSentenceRelevance(String sentence, String query) {
            List<String> queryTokens = TextPreprocessor.preprocess(query);
            List<String> sentenceTokens = TextPreprocessor.preprocess(sentence);
            if (queryTokens.isEmpty() || sentenceTokens.isEmpty()) return 0.0;

            Set<String> sentenceSet = new HashSet<>(sentenceTokens);
            int matchCount = 0;
            for (String q : queryTokens) {
                if (sentenceSet.contains(q)) matchCount++;
            }
            return (double) matchCount / queryTokens.size();
        }
    }

    public static final class ProvenanceManager {
        /**
         * Maps evidence snippets to verifiable document citations: [C1], [C2], etc.
         * Ensures 100% of extracted claims link back to real documents present in the corpus.
         */
        public static List<EvidenceSnippet> assignCitations(List<EvidenceSnippet> rawEvidence, Map<String, String> outCitationsRegistry) {
            List<EvidenceSnippet> citedSnippets = new ArrayList<>(rawEvidence.size());
            Map<String, String> docIdToCitationTag = new HashMap<>();

            for (EvidenceSnippet ev : rawEvidence) {
                String citationTag = docIdToCitationTag.computeIfAbsent(
                        ev.docId(),
                        k -> "[C" + (outCitationsRegistry.size() + 1) + "]"
                );
                outCitationsRegistry.put(citationTag, ev.docId() + " - " + ev.title() + " (" + ev.source() + ")");
                citedSnippets.add(new EvidenceSnippet(
                        citationTag,
                        ev.claimOrEvidence(),
                        ev.docId(),
                        ev.title(),
                        ev.passage(),
                        ev.score(),
                        ev.source()
                ));
            }
            return citedSnippets;
        }
    }

    public static final class ResearchOrchestrator {
        private final InvertedIndex index;
        private final RankingEngine rankingEngine;
        private final Repository repository;

        public ResearchOrchestrator(InvertedIndex index, RankingEngine rankingEngine, Repository repository) {
            this.index = Objects.requireNonNull(index);
            this.rankingEngine = Objects.requireNonNull(rankingEngine);
            this.repository = Objects.requireNonNull(repository);
        }

        public ResearchReport conductResearch(String question) {
            long startMillis = System.currentTimeMillis();
            SecurityManager.validateQuery(question);

            AuditLogger.info("Autonomous research orchestration initiated: " + question);

            // 1. Question decomposition
            List<String> subQueries = ResearchPlanner.generateSubQueries(question);
            List<String> planSteps = ResearchPlanner.createPlan(question, subQueries);

            // 2. Multi-query retrieval and sentence passage collection
            List<EvidenceSnippet> rawEvidence = EvidenceCollector.collectEvidence(rankingEngine, index, subQueries, 3);

            // 3. Provenance and citation attachment
            Map<String, String> citationsRegistry = new LinkedHashMap<>();
            List<EvidenceSnippet> evidenceWithCitations = ProvenanceManager.assignCitations(rawEvidence, citationsRegistry);

            // 4. Synthesize key findings
            List<String> findings = new ArrayList<>();
            Set<String> retrievedDocIds = new HashSet<>();
            for (EvidenceSnippet ev : evidenceWithCitations) {
                retrievedDocIds.add(ev.docId());
                findings.add(String.format("%s (Source: %s %s)", ev.passage(), ev.docId(), ev.citationId()));
            }

            // 5. Keyword pooling across retrieved documents
            Map<String, Double> keywordScorePool = new HashMap<>();
            for (String docId : retrievedDocIds) {
                Document doc = index.getDocument(docId);
                if (doc != null) {
                    List<Map.Entry<String, Double>> docKeywords = KeywordExtractor.extractKeywords(doc, index, 5);
                    for (Map.Entry<String, Double> e : docKeywords) {
                        keywordScorePool.put(e.getKey(), keywordScorePool.getOrDefault(e.getKey(), 0.0) + e.getValue());
                    }
                }
            }

            List<Map.Entry<String, Double>> topKeywords = new ArrayList<>(keywordScorePool.entrySet());
            topKeywords.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            if (topKeywords.size() > 8) topKeywords = topKeywords.subList(0, 8);

            // Sources list
            List<String> sources = new ArrayList<>();
            for (Map.Entry<String, String> entry : citationsRegistry.entrySet()) {
                sources.add(entry.getKey() + " " + entry.getValue());
            }

            long latencyMs = System.currentTimeMillis() - startMillis;
            double coverage = Math.min(100.0, ((double) retrievedDocIds.size() / Math.max(1, subQueries.size())) * 100.0);

            ResearchReport report = new ResearchReport(
                    question,
                    planSteps,
                    subQueries,
                    findings,
                    evidenceWithCitations,
                    citationsRegistry,
                    topKeywords,
                    sources,
                    coverage,
                    latencyMs,
                    retrievedDocIds.size()
            );

            // Persist session to active repository
            try {
                ResearchSession session = new ResearchSession(
                        "SESSION-" + System.currentTimeMillis(),
                        question,
                        subQueries,
                        evidenceWithCitations,
                        citationsRegistry,
                        String.join(" ", findings),
                        Instant.now()
                );
                repository.saveResearchSession(session);
            } catch (Exception e) {
                AuditLogger.warning("Failed to persist research session: " + e.getMessage());
            }

            AuditLogger.info(String.format("Research completed in %d ms with %d sources cited.", latencyMs, sources.size()));
            return report;
        }
    }

    // ========================================================================
    // 12. PERSISTENCE LAYER & REPOSITORY ABSTRACTION
    // ========================================================================

    public interface Repository {
        void saveDocument(Document doc);
        Optional<Document> findDocumentById(String docId);
        List<Document> findAllDocuments();
        void saveQueryLog(QueryLog log);
        List<QueryLog> getRecentQueries(int limit);
        void saveResearchSession(ResearchSession session);
        long getDocumentCount();
        boolean isHealthy();
        String getStorageMode();
        void close();
    }

    public static final class FileRepository implements Repository {
        private final Path storageDirectory;
        private final Path documentsDirectory;
        private final Path queriesDirectory;
        private final Path sessionsDirectory;

        public FileRepository(Path baseStorageDir) {
            this.storageDirectory = baseStorageDir;
            this.documentsDirectory = storageDirectory.resolve("documents");
            this.queriesDirectory = storageDirectory.resolve("queries");
            this.sessionsDirectory = storageDirectory.resolve("sessions");
            initializeDirectories();
        }

        private void initializeDirectories() {
            try {
                Files.createDirectories(documentsDirectory);
                Files.createDirectories(queriesDirectory);
                Files.createDirectories(sessionsDirectory);
            } catch (IOException e) {
                AuditLogger.severe("Failed to initialize FileRepository storage directories", e);
            }
        }

        @Override
        public synchronized void saveDocument(Document doc) {
            try {
                Path filePath = documentsDirectory.resolve(doc.docId() + ".json");
                String json = serializeDocumentToJson(doc);
                Files.writeString(filePath, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                AuditLogger.severe("FileRepository: Failed to write document " + doc.docId(), e);
            }
        }

        @Override
        public Optional<Document> findDocumentById(String docId) {
            Path filePath = documentsDirectory.resolve(docId + ".json");
            if (!Files.exists(filePath)) return Optional.empty();
            try {
                String json = Files.readString(filePath, StandardCharsets.UTF_8);
                return Optional.of(parseDocumentFromJson(json));
            } catch (Exception e) {
                AuditLogger.severe("FileRepository: Error reading document " + docId, e);
                return Optional.empty();
            }
        }

        @Override
        public List<Document> findAllDocuments() {
            List<Document> documentsList = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(documentsDirectory, "*.json")) {
                for (Path path : stream) {
                    try {
                        String json = Files.readString(path, StandardCharsets.UTF_8);
                        documentsList.add(parseDocumentFromJson(json));
                    } catch (Exception ignored) {}
                }
            } catch (IOException ignored) {}
            return documentsList;
        }

        @Override
        public synchronized void saveQueryLog(QueryLog log) {
            try {
                Path path = queriesDirectory.resolve("query_" + System.currentTimeMillis() + ".txt");
                String entry = log.queryId() + "\t" + log.timestamp() + "\t" + log.latencyMs() + "ms\t" + log.queryText() + "\n";
                Files.writeString(path, entry, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
        }

        @Override
        public List<QueryLog> getRecentQueries(int limit) {
            return Collections.emptyList();
        }

        @Override
        public synchronized void saveResearchSession(ResearchSession session) {
            try {
                Path path = sessionsDirectory.resolve(session.sessionId() + ".txt");
                StringBuilder sb = new StringBuilder();
                sb.append("Session ID: ").append(session.sessionId()).append("\n");
                sb.append("Question: ").append(session.question()).append("\n");
                sb.append("Timestamp: ").append(session.timestamp()).append("\n");
                sb.append("Sub-Queries: ").append(String.join("; ", session.subQueries())).append("\n");
                sb.append("Summary:\n").append(session.summary()).append("\n");
                Files.writeString(path, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception ignored) {}
        }

        @Override
        public long getDocumentCount() {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(documentsDirectory, "*.json")) {
                long count = 0;
                for (Path ignored : stream) count++;
                return count;
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public boolean isHealthy() {
            return Files.exists(storageDirectory) && Files.isWritable(storageDirectory);
        }

        @Override
        public String getStorageMode() {
            return "LOCAL_FILE_REPOSITORY (data/storage)";
        }

        @Override
        public void close() {
            // No unmanaged resources in FileRepository
        }

        private static String serializeDocumentToJson(Document doc) {
            return "{\n" +
                    "  \"docId\": \"" + escapeJson(doc.docId()) + "\",\n" +
                    "  \"title\": \"" + escapeJson(doc.title()) + "\",\n" +
                    "  \"source\": \"" + escapeJson(doc.source()) + "\",\n" +
                    "  \"content\": \"" + escapeJson(doc.content()) + "\",\n" +
                    "  \"createdAt\": \"" + doc.createdAt().toString() + "\"\n" +
                    "}";
        }

        private static Document parseDocumentFromJson(String json) {
            String docId = extractJsonField(json, "docId");
            String title = extractJsonField(json, "title");
            String source = extractJsonField(json, "source");
            String content = extractJsonField(json, "content");
            String createdAtStr = extractJsonField(json, "createdAt");
            Instant ts = (createdAtStr != null && !createdAtStr.isEmpty()) ? Instant.parse(createdAtStr) : Instant.now();
            return new Document(docId, title, source, content, new HashMap<>(), null, ts);
        }

        private static String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        private static String extractJsonField(String json, String field) {
            Pattern pattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                String val = matcher.group(1);
                return val.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
            }
            return "";
        }
    }

    public static final class MongoRepository implements Repository {
        private final MongoClient mongoClient;
        private final MongoDatabase database;
        private final MongoCollection<BsonDocument> documentCollection;
        private final MongoCollection<BsonDocument> queryCollection;
        private final MongoCollection<BsonDocument> sessionCollection;
        private volatile boolean healthy = true;

        public MongoRepository(String connectionUri, String dbName) {
            AuditLogger.info("Initializing MongoDB Persistence Adapter (Strict 2.5s connection timeout)...");
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionUri))
                    .applyToSocketSettings(builder -> builder.connectTimeout(2500, TimeUnit.MILLISECONDS).readTimeout(2500, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(2500, TimeUnit.MILLISECONDS))
                    .build();

            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase(dbName);
            this.documentCollection = database.getCollection("documents", BsonDocument.class);
            this.queryCollection = database.getCollection("queries", BsonDocument.class);
            this.sessionCollection = database.getCollection("research_sessions", BsonDocument.class);

            try {
                database.runCommand(new BsonDocument("ping", new org.bson.BsonInt32(1)));
                documentCollection.createIndex(Indexes.ascending("docId"), new IndexOptions().unique(true));
                AuditLogger.info("MongoDB Connection Verified and Initialized Successfully.");
            } catch (Exception e) {
                healthy = false;
                AuditLogger.warning("MongoDB ping failed: " + SecurityManager.sanitize(e.getMessage()));
                throw new RuntimeException("MongoDB connection failure: " + e.getMessage(), e);
            }
        }

        @Override
        public void saveDocument(Document doc) {
            try {
                BsonDocument bDoc = new BsonDocument()
                        .append("docId", new org.bson.BsonString(doc.docId()))
                        .append("title", new org.bson.BsonString(doc.title()))
                        .append("source", new org.bson.BsonString(doc.source()))
                        .append("content", new org.bson.BsonString(doc.content()))
                        .append("createdAt", new org.bson.BsonInt64(doc.createdAt().toEpochMilli()));

                BsonDocument filter = new BsonDocument("docId", new org.bson.BsonString(doc.docId()));
                documentCollection.replaceOne(filter, bDoc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
            } catch (Exception e) {
                healthy = false;
                AuditLogger.warning("MongoRepository saveDocument error: " + SecurityManager.sanitize(e.getMessage()));
            }
        }

        @Override
        public Optional<Document> findDocumentById(String docId) {
            try {
                BsonDocument filter = new BsonDocument("docId", new org.bson.BsonString(docId));
                BsonDocument found = documentCollection.find(filter).first();
                if (found == null) return Optional.empty();

                return Optional.of(new Document(
                        found.getString("docId").getValue(),
                        found.getString("title").getValue(),
                        found.getString("source").getValue(),
                        found.getString("content").getValue(),
                        new HashMap<>(),
                        null,
                        Instant.ofEpochMilli(found.getInt64("createdAt").getValue())
                ));
            } catch (Exception e) {
                healthy = false;
                return Optional.empty();
            }
        }

        @Override
        public List<Document> findAllDocuments() {
            List<Document> documentsList = new ArrayList<>();
            try {
                for (BsonDocument found : documentCollection.find()) {
                    documentsList.add(new Document(
                            found.getString("docId").getValue(),
                            found.getString("title").getValue(),
                            found.getString("source").getValue(),
                            found.getString("content").getValue(),
                            new HashMap<>(),
                            null,
                            Instant.ofEpochMilli(found.getInt64("createdAt").getValue())
                    ));
                }
            } catch (Exception e) {
                healthy = false;
            }
            return documentsList;
        }

        @Override
        public void saveQueryLog(QueryLog log) {
            try {
                BsonDocument b = new BsonDocument()
                        .append("queryId", new org.bson.BsonString(log.queryId()))
                        .append("queryText", new org.bson.BsonString(log.queryText()))
                        .append("latencyMs", new org.bson.BsonInt64(log.latencyMs()))
                        .append("timestamp", new org.bson.BsonInt64(log.timestamp().toEpochMilli()));
                queryCollection.insertOne(b);
            } catch (Exception e) {
                healthy = false;
            }
        }

        @Override
        public List<QueryLog> getRecentQueries(int limit) {
            return Collections.emptyList();
        }

        @Override
        public void saveResearchSession(ResearchSession session) {
            try {
                BsonDocument b = new BsonDocument()
                        .append("sessionId", new org.bson.BsonString(session.sessionId()))
                        .append("question", new org.bson.BsonString(session.question()))
                        .append("summary", new org.bson.BsonString(session.summary()))
                        .append("timestamp", new org.bson.BsonInt64(session.timestamp().toEpochMilli()));
                sessionCollection.insertOne(b);
            } catch (Exception e) {
                healthy = false;
            }
        }

        @Override
        public long getDocumentCount() {
            try {
                return documentCollection.countDocuments();
            } catch (Exception e) {
                healthy = false;
                return 0;
            }
        }

        @Override
        public boolean isHealthy() {
            return healthy;
        }

        @Override
        public String getStorageMode() {
            return "REMOTE_MONGODB (" + database.getName() + ")";
        }

        @Override
        public void close() {
            try {
                mongoClient.close();
            } catch (Exception ignored) {}
        }
    }

    public static final class RepositoryManager {
        public static Repository createRepository() {
            String uri = ConfigurationManager.getMongoUri();
            if (uri != null && !uri.trim().isEmpty()) {
                try {
                    String dbName = ConfigurationManager.getMongoDatabase();
                    return new MongoRepository(uri, dbName);
                } catch (Exception e) {
                    AuditLogger.warning("MongoDB unavailable (" + SecurityManager.sanitize(e.getMessage()) + "). Falling back to FileRepository.");
                }
            } else {
                AuditLogger.info("MONGODB_URI not set. Operating in local FileRepository fallback mode.");
            }
            Path localDir = Paths.get("data", "storage");
            return new FileRepository(localDir);
        }
    }

    // ========================================================================
    // 13. CORPUS INGESTION ENGINE
    // ========================================================================

    public static final class CorpusIngestionEngine {
        private final InvertedIndex index;
        private final Repository repository;

        public CorpusIngestionEngine(InvertedIndex index, Repository repository) {
            this.index = Objects.requireNonNull(index);
            this.repository = Objects.requireNonNull(repository);
        }

        public int ingestDirectory(Path directoryPath) throws IOException {
            Path resolvedDir = SecurityManager.validateAndResolvePath(directoryPath.toString(), null);
            if (!Files.exists(resolvedDir) || !Files.isDirectory(resolvedDir)) {
                throw new FileNotFoundException("Corpus directory does not exist: " + resolvedDir);
            }

            AuditLogger.info("Ingesting research literature from: " + resolvedDir.toAbsolutePath());
            int ingestedCount = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(resolvedDir)) {
                for (Path file : stream) {
                    if (Files.isDirectory(file)) continue;
                    String filename = file.getFileName().toString().toLowerCase(Locale.ENGLISH);

                    if (filename.endsWith(".txt")) {
                        Document doc = parseTxtFile(file);
                        if (doc != null) {
                            index.addDocument(doc);
                            repository.saveDocument(doc);
                            ingestedCount++;
                        }
                    } else if (filename.endsWith(".json")) {
                        Document doc = parseJsonFile(file);
                        if (doc != null) {
                            index.addDocument(doc);
                            repository.saveDocument(doc);
                            ingestedCount++;
                        }
                    }
                }
            }

            index.computeVectorNorms();
            AuditLogger.info(String.format("Ingestion complete. %d documents indexed. Vocabulary: %d terms.",
                    ingestedCount, index.getVocabularySize()));
            return ingestedCount;
        }

        private Document parseTxtFile(Path file) throws IOException {
            SecurityManager.validateFileSize(file);
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return null;

            String docId = lines.get(0).trim();
            String title = lines.size() > 1 ? lines.get(1).trim() : "Untitled Document";
            String source = lines.size() > 2 ? lines.get(2).trim() : file.getFileName().toString();

            StringBuilder contentBuilder = new StringBuilder();
            for (int i = 3; i < lines.size(); i++) {
                contentBuilder.append(lines.get(i)).append(" ");
            }
            String content = contentBuilder.toString().trim();
            if (content.isEmpty()) content = title;

            return new Document(docId, title, source, content, new HashMap<>(), null, Instant.now());
        }

        private Document parseJsonFile(Path file) throws IOException {
            SecurityManager.validateFileSize(file);
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            String docId = extractJsonString(raw, "docId");
            String title = extractJsonString(raw, "title");
            String source = extractJsonString(raw, "source");
            String content = extractJsonString(raw, "content");

            if (docId.isEmpty()) {
                docId = file.getFileName().toString().replace(".json", "").toUpperCase(Locale.ENGLISH);
            }
            if (title.isEmpty()) title = docId;
            if (source.isEmpty()) source = file.getFileName().toString();

            return new Document(docId, title, source, content, new HashMap<>(), null, Instant.now());
        }

        private static String extractJsonString(String json, String key) {
            Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);
            Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1).replace("\\\"", "\"").replace("\\n", " ").replace("\\r", "").replace("\\\\", "\\").trim();
            }
            return "";
        }
    }

    // ========================================================================
    // 14. EVALUATION & BENCHMARKING ENGINE
    // ========================================================================

    public static final class EvaluationEngine {
        private final InvertedIndex index;
        private final RankingEngine rankingEngine;

        public EvaluationEngine(InvertedIndex index, RankingEngine rankingEngine) {
            this.index = Objects.requireNonNull(index);
            this.rankingEngine = Objects.requireNonNull(rankingEngine);
        }

        public EvaluationMetrics evaluate(Path groundTruthPath, int k) throws IOException {
            if (!Files.exists(groundTruthPath)) {
                throw new FileNotFoundException("Ground truth evaluation file missing: " + groundTruthPath);
            }

            String content = Files.readString(groundTruthPath, StandardCharsets.UTF_8);
            List<EvaluationQuery> testQueries = parseGroundTruth(content);
            if (testQueries.isEmpty()) {
                throw new IllegalArgumentException("No test queries found in ground truth dataset.");
            }

            double sumPrecision = 0.0;
            double sumRecall = 0.0;
            double sumF1 = 0.0;
            double sumMRR = 0.0;
            double sumNDCG = 0.0;

            double sumBaselineP = 0.0;
            double sumBaselineR = 0.0;
            double sumBaselineF1 = 0.0;
            double sumBaselineMRR = 0.0;

            List<Long> latencies = new ArrayList<>();

            for (EvaluationQuery q : testQueries) {
                long t0 = System.nanoTime();
                List<SearchResult> retrieved = rankingEngine.search(q.query, k, 0.001);
                long t1 = System.nanoTime();
                latencies.add((t1 - t0) / 1_000_000L);

                List<SearchResult> baselineRetrieved = rankingEngine.searchBaselineLexical(q.query, k);

                // Enhanced Metrics
                List<String> retrievedIds = retrieved.stream().map(SearchResult::docId).toList();
                Set<String> relevantSet = new HashSet<>(q.relevantDocIds);

                int relevantRetrieved = 0;
                for (String rId : retrievedIds) {
                    if (relevantSet.contains(rId)) relevantRetrieved++;
                }

                double precision = (double) relevantRetrieved / k;
                double recall = relevantSet.isEmpty() ? 0.0 : (double) relevantRetrieved / relevantSet.size();
                double f1 = (precision + recall) > 0 ? (2.0 * precision * recall) / (precision + recall) : 0.0;

                // MRR (Mean Reciprocal Rank)
                double reciprocalRank = 0.0;
                for (int i = 0; i < retrievedIds.size(); i++) {
                    if (relevantSet.contains(retrievedIds.get(i))) {
                        reciprocalRank = 1.0 / (i + 1);
                        break;
                    }
                }

                // nDCG@K (Normalized Discounted Cumulative Gain)
                double dcg = 0.0;
                for (int i = 0; i < retrievedIds.size(); i++) {
                    int gain = q.gradedRelevance.getOrDefault(retrievedIds.get(i), 0);
                    dcg += (Math.pow(2.0, gain) - 1.0) / (Math.log(i + 2.0) / Math.log(2.0));
                }

                List<Integer> idealGains = new ArrayList<>(q.gradedRelevance.values());
                idealGains.sort(Collections.reverseOrder());
                double idcg = 0.0;
                for (int i = 0; i < Math.min(k, idealGains.size()); i++) {
                    idcg += (Math.pow(2.0, idealGains.get(i)) - 1.0) / (Math.log(i + 2.0) / Math.log(2.0));
                }
                double ndcg = idcg > 0 ? dcg / idcg : 0.0;

                sumPrecision += precision;
                sumRecall += recall;
                sumF1 += f1;
                sumMRR += reciprocalRank;
                sumNDCG += ndcg;

                // Baseline Metrics
                List<String> baseIds = baselineRetrieved.stream().map(SearchResult::docId).toList();
                int baseRel = 0;
                for (String bId : baseIds) {
                    if (relevantSet.contains(bId)) baseRel++;
                }
                double bp = (double) baseRel / k;
                double br = relevantSet.isEmpty() ? 0.0 : (double) baseRel / relevantSet.size();
                double bf1 = (bp + br) > 0 ? (2.0 * bp * br) / (bp + br) : 0.0;
                double brr = 0.0;
                for (int i = 0; i < baseIds.size(); i++) {
                    if (relevantSet.contains(baseIds.get(i))) {
                        brr = 1.0 / (i + 1);
                        break;
                    }
                }
                sumBaselineP += bp;
                sumBaselineR += br;
                sumBaselineF1 += bf1;
                sumBaselineMRR += brr;
            }

            int count = testQueries.size();
            Collections.sort(latencies);
            double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
            int p95Idx = (int) Math.ceil(0.95 * latencies.size()) - 1;
            double p95Latency = latencies.get(Math.max(0, p95Idx));

            return new EvaluationMetrics(
                    sumPrecision / count,
                    sumRecall / count,
                    sumF1 / count,
                    sumMRR / count,
                    sumNDCG / count,
                    avgLatency,
                    p95Latency,
                    12L,
                    index.getDocumentCount(),
                    sumBaselineP / count,
                    sumBaselineR / count,
                    sumBaselineF1 / count,
                    sumBaselineMRR / count
            );
        }

        private static final class EvaluationQuery {
            String queryId;
            String query;
            List<String> relevantDocIds = new ArrayList<>();
            Map<String, Integer> gradedRelevance = new HashMap<>();
        }

        private static List<EvaluationQuery> parseGroundTruth(String json) {
            List<EvaluationQuery> queries = new ArrayList<>();
            Pattern itemPattern = Pattern.compile("\\{\\s*\"queryId\"\\s*:\\s*\"(.*?)\".*?\"query\"\\s*:\\s*\"(.*?)\".*?\"relevantDocIds\"\\s*:\\s*\\[(.*?)\\].*?\"gradedRelevance\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
            Matcher m = itemPattern.matcher(json);
            while (m.find()) {
                EvaluationQuery eq = new EvaluationQuery();
                eq.queryId = m.group(1).trim();
                eq.query = m.group(2).trim();

                String docsRaw = m.group(3);
                Matcher docMatcher = Pattern.compile("\"(DOC-\\d+)\"").matcher(docsRaw);
                while (docMatcher.find()) {
                    eq.relevantDocIds.add(docMatcher.group(1));
                }

                String gradedRaw = m.group(4);
                Matcher gradeMatcher = Pattern.compile("\"(DOC-\\d+)\"\\s*:\\s*(\\d+)").matcher(gradedRaw);
                while (gradeMatcher.find()) {
                    eq.gradedRelevance.put(gradeMatcher.group(1), Integer.parseInt(gradeMatcher.group(2)));
                }
                queries.add(eq);
            }
            return queries;
        }
    }

    // ========================================================================
    // 15. HEALTH & STATS ENGINE
    // ========================================================================

    public static final class HealthChecker {
        public static HealthReport checkHealth(InvertedIndex index, Repository repository) {
            Map<String, String> details = new LinkedHashMap<>();

            String javaVer = System.getProperty("java.version");
            long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            details.put("Java Version", javaVer);
            details.put("JVM Memory", String.format("%d MB free / %d MB total", freeMem, totalMem));
            String javaStatus = "PASS (" + javaVer + ")";

            Path corpusDir = Paths.get(ConfigurationManager.getCorpusPath());
            boolean corpusExists = Files.exists(corpusDir) && Files.isDirectory(corpusDir);
            String corpusStatus = corpusExists ? "PASS (" + corpusDir.toString() + ")" : "WARNING (Directory not found)";
            details.put("Corpus Directory", corpusDir.toAbsolutePath().toString());

            int docCount = index.getDocumentCount();
            int vocabSize = index.getVocabularySize();
            String indexStatus = (docCount > 0) ? String.format("PASS (%d docs, %d terms)", docCount, vocabSize) : "WARNING (Empty index)";
            details.put("Indexed Documents", String.valueOf(docCount));
            details.put("Vocabulary Size", String.valueOf(vocabSize));

            boolean repoHealth = repository != null && repository.isHealthy();
            String storageStatus = repoHealth ? "PASS (" + repository.getStorageMode() + ")" : "WARNING (Persistence Degraded)";
            details.put("Storage Engine", repository != null ? repository.getStorageMode() : "NONE");

            String overall = (corpusExists && repoHealth) ? "HEALTHY" : "DEGRADED";

            return new HealthReport(
                    javaStatus,
                    corpusStatus,
                    indexStatus,
                    storageStatus,
                    "PASS",
                    overall,
                    details
            );
        }
    }

    public static final class StatsEngine {
        public static SystemStats getStats(InvertedIndex index, Repository repository) {
            return new SystemStats(
                    index.getDocumentCount(),
                    index.getVocabularySize(),
                    index.getTotalTokens(),
                    index.getAverageDocumentLength(),
                    estimateIndexMemory(index),
                    repository.getStorageMode(),
                    index.getLastBuildTime()
            );
        }

        private static long estimateIndexMemory(InvertedIndex index) {
            return (index.getVocabularySize() * 128L) + (index.getTotalTokens() * 16L);
        }
    }

    // ========================================================================
    // 16. CENTRAL BACKEND FACADE
    // ========================================================================

    public static final class EngineFacade {
        private final InvertedIndex index = new InvertedIndex();
        private final Repository repository = RepositoryManager.createRepository();
        private final RankingEngine rankingEngine = new RankingEngine(index);
        private final CorpusIngestionEngine ingestionEngine = new CorpusIngestionEngine(index, repository);
        private final ResearchOrchestrator researchOrchestrator = new ResearchOrchestrator(index, rankingEngine, repository);
        private final EvaluationEngine evaluationEngine = new EvaluationEngine(index, rankingEngine);

        private static final EngineFacade INSTANCE = new EngineFacade();

        public static EngineFacade getInstance() {
            return INSTANCE;
        }

        private EngineFacade() {
            autoInitialize();
        }

        private void autoInitialize() {
            try {
                Path sampleDir = Paths.get(ConfigurationManager.getCorpusPath());
                if (Files.exists(sampleDir)) {
                    ingestionEngine.ingestDirectory(sampleDir);
                }
            } catch (Exception e) {
                AuditLogger.warning("Auto-ingestion encountered an issue: " + e.getMessage());
            }
        }

        public int ingest(String dirPath) throws IOException {
            return ingestionEngine.ingestDirectory(Paths.get(dirPath));
        }

        public List<SearchResult> search(String query, int k) {
            int validK = SecurityManager.validateK(k, ConfigurationManager.getTopK());
            double minScore = ConfigurationManager.getMinScore();
            long t0 = System.currentTimeMillis();
            List<SearchResult> results = rankingEngine.search(query, validK, minScore);
            long latency = System.currentTimeMillis() - t0;

            try {
                List<String> docIds = results.stream().map(SearchResult::docId).toList();
                repository.saveQueryLog(new QueryLog("Q-" + System.currentTimeMillis(), query, Instant.now(), docIds, latency));
            } catch (Exception ignored) {}

            return results;
        }

        public ResearchReport research(String question) {
            return researchOrchestrator.conductResearch(question);
        }

        public String summarize(String docId, int numSentences) {
            Document doc = index.getDocument(docId);
            if (doc == null) {
                Optional<Document> fromDb = repository.findDocumentById(docId);
                if (fromDb.isPresent()) {
                    doc = fromDb.get();
                } else {
                    throw new NoSuchElementException("Document ID not found: " + docId);
                }
            }
            return TextRankSummarizer.summarize(doc.content(), numSentences);
        }

        public List<Map.Entry<String, Double>> keywords(String docId, int topN) {
            Document doc = index.getDocument(docId);
            if (doc == null) {
                Optional<Document> fromDb = repository.findDocumentById(docId);
                if (fromDb.isPresent()) {
                    doc = fromDb.get();
                } else {
                    throw new NoSuchElementException("Document ID not found: " + docId);
                }
            }
            return KeywordExtractor.extractKeywords(doc, index, topN);
        }

        public SystemStats stats() {
            return StatsEngine.getStats(index, repository);
        }

        public ComplexityProfile getComplexityProfile(String sampleQuery) {
            if (sampleQuery != null && !sampleQuery.trim().isEmpty()) {
                // Execute sample search to populate runtime telemetry counters
                rankingEngine.search(sampleQuery, 5, 0.001);
            }
            return rankingEngine.getLastComplexityProfile();
        }

        public HealthReport health() {
            return HealthChecker.checkHealth(index, repository);
        }

        public EvaluationMetrics evaluate() throws IOException {
            Path gt = Paths.get("data", "evaluation", "ground_truth.json");
            return evaluationEngine.evaluate(gt, ConfigurationManager.getTopK());
        }

        public InvertedIndex getIndex() {
            return index;
        }

        public Repository getRepository() {
            return repository;
        }

        public RankingEngine getRankingEngine() {
            return rankingEngine;
        }

        public void shutdown() {
            repository.close();
        }
    }
}
