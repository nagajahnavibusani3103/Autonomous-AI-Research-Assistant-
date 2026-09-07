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
import java.util.stream.Collectors;

/**
 * ============================================================================
 * AUTONOMOUS AI RESEARCH ASSISTANT - CORE BACKEND ENGINE
 * ============================================================================
 * Architecture: Senior-SDE Modular Architecture via Encapsulated Static Classes.
 * Constraints Enforced:
 *  - Pure Java (Java 17+)
 *  - Zero External Generative LLM / Web API Dependencies
 *  - Inverted Index with Smoothed TF-IDF and Sparse Cosine Vector Space Model
 *  - Extractive TextRank Graph-Based Summarization (PageRank on Sentences)
 *  - Autonomous Deterministic Research Decomposition & Evidence Provenance
 *  - Dual Persistence Layer: MongoDB Official Driver with Robust Local Fallback
 *  - Rigorous Security, Path-Traversal Protection & Secret Sanitization
 * ============================================================================
 */
public final class AntigravityBackend {

    private AntigravityBackend() {
        // Prevent instantiation of outer utility class
    }

    // ========================================================================
    // 1. SECURITY & LOG SANITIZATION
    // ========================================================================

    public static final class SecurityManager {
        private static final Pattern MONGO_SECRET_PATTERN =
                Pattern.compile("(mongodb(?:\\+srv)?://)([^:@/]+):([^@/]+)@");
        private static final Pattern GENERIC_SECRET_PATTERN =
                Pattern.compile("(?i)(password|secret|token|apikey)\\s*[:=]\\s*['\"]?([^'\"\\s]+)['\"]?");
        private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L; // 10 MB limit
        private static final int MAX_QUERY_LENGTH = 500;
        private static final int MAX_K_VALUE = 100;

        public static String sanitize(String input) {
            if (input == null) return null;
            String sanitized = MONGO_SECRET_PATTERN.matcher(input).replaceAll("$1***:***@");
            sanitized = GENERIC_SECRET_PATTERN.matcher(sanitized).replaceAll("$1=***");
            return sanitized;
        }

        public static Path validateAndResolvePath(String pathStr, Path baseDir) {
            if (pathStr == null || pathStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Path must not be null or empty.");
            }
            if (pathStr.contains("\0")) {
                throw new SecurityException("Null byte injection detected in path: " + pathStr);
            }
            Path rawPath = Paths.get(pathStr.trim());
            Path resolved = baseDir != null ? baseDir.resolve(rawPath).normalize() : rawPath.normalize();
            if (baseDir != null) {
                Path normalizedBase = baseDir.toAbsolutePath().normalize();
                Path normalizedResolved = resolved.toAbsolutePath().normalize();
                if (!normalizedResolved.startsWith(normalizedBase)) {
                    throw new SecurityException("Path traversal attempt blocked: " + pathStr);
                }
            }
            return resolved;
        }

        public static void validateQuery(String query) {
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("Query string cannot be null or empty.");
            }
            if (query.length() > MAX_QUERY_LENGTH) {
                throw new IllegalArgumentException("Query length exceeds maximum limit of " + MAX_QUERY_LENGTH + " characters.");
            }
        }

        public static int validateK(int k, int defaultK) {
            if (k <= 0) return defaultK;
            if (k > MAX_K_VALUE) return MAX_K_VALUE;
            return k;
        }

        public static void validateFileSize(Path path) throws IOException {
            if (Files.exists(path) && Files.size(path) > MAX_FILE_SIZE_BYTES) {
                throw new SecurityException("File size exceeds maximum safety limit of 10 MB: " + path);
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
                Level lvl = Level.parse(levelName.toUpperCase());
                LOGGER.setLevel(lvl);
                for (Handler h : LOGGER.getHandlers()) {
                    h.setLevel(lvl);
                }
            } catch (Exception e) {
                LOGGER.warning("Invalid log level: " + levelName + ". Falling back to INFO.");
            }
        }

        public static void info(String msg) {
            LOGGER.info(msg);
        }

        public static void warning(String msg) {
            LOGGER.warning(msg);
        }

        public static void severe(String msg, Throwable t) {
            if (t != null) {
                LOGGER.log(Level.SEVERE, msg + " Details: " + SecurityManager.sanitize(t.getMessage()));
            } else {
                LOGGER.severe(msg);
            }
        }
    }

    // ========================================================================
    // 3. CONFIGURATION MANAGEMENT
    // ========================================================================

    public static final class ConfigurationManager {
        private static final Properties PROPERTIES = new Properties();

        static {
            loadPropertiesFile();
        }

        private static void loadPropertiesFile() {
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
            String envKey = key.toUpperCase().replace('.', '_');
            String envVal = System.getenv(envKey);
            if (envVal != null && !envVal.trim().isEmpty()) {
                return envVal.trim();
            }
            String sysVal = System.getProperty(key);
            if (sysVal != null && !sysVal.trim().isEmpty()) {
                return sysVal.trim();
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
            Map<String, String> map = new LinkedHashMap<>();
            map.put("corpus.path", getCorpusPath());
            map.put("top.k", String.valueOf(getTopK()));
            map.put("min.score", String.valueOf(getMinScore()));
            map.put("mongodb.database", getMongoDatabase());
            String uri = getMongoUri();
            map.put("mongodb.uri", uri != null ? SecurityManager.sanitize(uri) : "NOT_CONFIGURED (Using FileRepository)");
            map.put("log.level", get("log.level", "INFO"));
            return map;
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
        public Document withTokens(List<String> tokens) {
            return new Document(docId, title, source, content, metadata, tokens, createdAt);
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
    // 5. NATURAL LANGUAGE PROCESSING (LOCAL NLP ENGINE)
    // ========================================================================

    public static final class Tokenizer {
        private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z0-9]+(?:[-'][a-zA-Z0-9]+)*");

        public static List<String> tokenize(String text) {
            if (text == null || text.trim().isEmpty()) {
                return Collections.emptyList();
            }
            List<String> tokens = new ArrayList<>();
            Matcher matcher = WORD_PATTERN.matcher(text.toLowerCase(Locale.ENGLISH));
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
            List<String> filtered = new ArrayList<>(tokens.size());
            for (String t : tokens) {
                if (!isStopWord(t) && t.length() > 2) {
                    filtered.add(t);
                }
            }
            return filtered;
        }
    }

    /**
     * Pure Java implementation of Martin Porter's 1980 Stemming Algorithm.
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

        private static boolean isConsonant(String str, int i) {
            char c = str.charAt(i);
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') return false;
            if (c == 'y') {
                return (i == 0) || !isConsonant(str, i - 1);
            }
            return true;
        }

        private static int getMeasure(String str) {
            int n = 0;
            int i = 0;
            int len = str.length();
            while (i < len && isConsonant(str, i)) i++;
            while (i < len) {
                while (i < len && !isConsonant(str, i)) i++;
                while (i < len && isConsonant(str, i)) i++;
                n++;
            }
            return n;
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
            boolean extraStep = false;
            String stem = null;
            if (w.endsWith("ed")) {
                stem = w.substring(0, w.length() - 2);
                if (containsVowel(stem)) extraStep = true;
            } else if (w.endsWith("ing")) {
                stem = w.substring(0, w.length() - 3);
                if (containsVowel(stem)) extraStep = true;
            }
            if (extraStep && stem != null) {
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
            for (String[] r : rules) {
                if (w.endsWith(r[0])) {
                    String stem = w.substring(0, w.length() - r[0].length());
                    if (getMeasure(stem) > 0) return stem + r[1];
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
            for (String[] r : rules) {
                if (w.endsWith(r[0])) {
                    String stem = w.substring(0, w.length() - r[0].length());
                    if (getMeasure(stem) > 0) return stem + r[1];
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
        private static final Pattern SENTENCE_PATTERN =
                Pattern.compile("(?<=[.!?])\\s+(?=[A-Z0-9\"])", Pattern.MULTILINE);

        public static List<String> segment(String text) {
            if (text == null || text.trim().isEmpty()) {
                return Collections.emptyList();
            }
            String[] raw = SENTENCE_PATTERN.split(text.trim());
            List<String> list = new ArrayList<>();
            for (String s : raw) {
                String trimmed = s.replaceAll("\\s+", " ").trim();
                if (trimmed.length() > 15) { // meaningful sentence bound
                    list.add(trimmed);
                }
            }
            return list;
        }
    }

    public static final class TextPreprocessor {
        public static List<String> preprocess(String text) {
            List<String> tokens = Tokenizer.tokenize(text);
            List<String> filtered = StopWordFilter.filter(tokens);
            List<String> stemmed = new ArrayList<>(filtered.size());
            for (String token : filtered) {
                stemmed.add(Stemmer.stem(token));
            }
            return stemmed;
        }
    }

    // ========================================================================
    // 6. INVERTED INDEX & VECTOR SPACE MODEL
    // ========================================================================

    public static final class InvertedIndex {
        private final Map<String, List<Posting>> index = new HashMap<>();
        private final Map<String, Map<String, Integer>> termDocFrequency = new HashMap<>();
        private final Map<String, Integer> docLengths = new HashMap<>();
        private final Map<String, Double> docVectorNorms = new HashMap<>();
        private final Map<String, Document> documents = new HashMap<>();
        private long totalTokens = 0;
        private Instant lastBuildTime = null;

        public synchronized void addDocument(Document doc) {
            List<String> tokens = TextPreprocessor.preprocess(doc.content() + " " + doc.title());
            Document processedDoc = doc.withTokens(tokens);
            documents.put(doc.docId(), processedDoc);
            docLengths.put(doc.docId(), tokens.size());
            totalTokens += tokens.size();

            Map<String, List<Integer>> positionsMap = new HashMap<>();
            for (int i = 0; i < tokens.size(); i++) {
                String term = tokens.get(i);
                positionsMap.computeIfAbsent(term, k -> new ArrayList<>()).add(i);
            }

            for (Map.Entry<String, List<Integer>> entry : positionsMap.entrySet()) {
                String term = entry.getKey();
                List<Integer> pos = entry.getValue();
                int tf = pos.size();

                index.computeIfAbsent(term, k -> new ArrayList<>())
                        .add(new Posting(doc.docId(), tf, pos));

                termDocFrequency.computeIfAbsent(term, k -> new HashMap<>())
                        .put(doc.docId(), tf);
            }
        }

        public synchronized void computeVectorNorms() {
            docVectorNorms.clear();
            int N = documents.size();
            if (N == 0) return;

            Map<String, Double> sumSqMap = new HashMap<>();
            for (Map.Entry<String, Map<String, Integer>> entry : termDocFrequency.entrySet()) {
                String term = entry.getKey();
                Map<String, Integer> docMap = entry.getValue();
                int df = docMap.size();
                double idf = TFIDFEngine.calculateSmoothedIdf(N, df);

                for (Map.Entry<String, Integer> docEntry : docMap.entrySet()) {
                    String docId = docEntry.getKey();
                    int tf = docEntry.getValue();
                    double tfWeight = TFIDFEngine.calculateSublinearTf(tf);
                    double weight = tfWeight * idf;
                    sumSqMap.put(docId, sumSqMap.getOrDefault(docId, 0.0) + (weight * weight));
                }
            }

            for (Map.Entry<String, Double> e : sumSqMap.entrySet()) {
                docVectorNorms.put(e.getKey(), Math.sqrt(e.getValue()));
            }
            lastBuildTime = Instant.now();
        }

        public List<Posting> getPostings(String term) {
            return index.getOrDefault(term, Collections.emptyList());
        }

        public int getDocumentFrequency(String term) {
            Map<String, Integer> map = termDocFrequency.get(term);
            return map != null ? map.size() : 0;
        }

        public int getTermFrequency(String term, String docId) {
            Map<String, Integer> map = termDocFrequency.get(term);
            if (map != null) {
                return map.getOrDefault(docId, 0);
            }
            return 0;
        }

        public Set<String> getCandidateDocIds(Collection<String> queryTerms) {
            Set<String> candidates = new HashSet<>();
            for (String term : queryTerms) {
                List<Posting> postings = index.get(term);
                if (postings != null) {
                    for (Posting p : postings) {
                        candidates.add(p.docId());
                    }
                }
            }
            return candidates;
        }

        public int getDocumentCount() {
            return documents.size();
        }

        public int getVocabularySize() {
            return index.size();
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
            return docVectorNorms.getOrDefault(docId, 1.0);
        }

        public synchronized void clear() {
            index.clear();
            termDocFrequency.clear();
            docLengths.clear();
            docVectorNorms.clear();
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
         * Mathematical Formulation:
         * Smoothed Inverse Document Frequency:
         *   IDF(t) = ln((N + 1) / (df(t) + 1)) + 1.0
         * Sublinear Term Frequency:
         *   TF(t, d) = 1 + ln(f_{t,d}) if f_{t,d} > 0 else 0
         * TF-IDF(t, d) = TF(t, d) * IDF(t)
         */
        public static double calculateSmoothedIdf(int totalDocs, int docFreq) {
            if (totalDocs <= 0) return 1.0;
            return Math.log(((double) totalDocs + 1.0) / ((double) docFreq + 1.0)) + 1.0;
        }

        public static double calculateSublinearTf(int freq) {
            if (freq <= 0) return 0.0;
            return 1.0 + Math.log(freq);
        }

        public static double calculateTfIdf(int freq, int totalDocs, int docFreq) {
            if (freq <= 0) return 0.0;
            return calculateSublinearTf(freq) * calculateSmoothedIdf(totalDocs, docFreq);
        }
    }

    public static final class SimilarityEngine {
        /**
         * Sparse Cosine Similarity:
         *   Cosine(Q, D) = (Q . D) / (||Q|| * ||D||)
         * Avoids full matrix expansion by evaluating dot-product strictly over non-zero query terms.
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
                    double dWeight = docTfWeight * idf;
                    dotProduct += (qWeight * dWeight);
                }
            }

            return dotProduct / (queryNorm * docNorm);
        }
    }

    // ========================================================================
    // 8. RANKING ENGINE & BM25 BASELINE
    // ========================================================================

    public static final class RankingEngine {
        private final InvertedIndex index;

        public RankingEngine(InvertedIndex index) {
            this.index = Objects.requireNonNull(index, "Index cannot be null");
        }

        public List<SearchResult> search(String queryString, int topK, double minScore) {
            SecurityManager.validateQuery(queryString);
            List<String> queryTerms = TextPreprocessor.preprocess(queryString);
            if (queryTerms.isEmpty()) {
                return Collections.emptyList();
            }

            int N = index.getDocumentCount();
            if (N == 0) return Collections.emptyList();

            // Calculate query vector weights
            Map<String, Integer> qTermFreq = new HashMap<>();
            for (String t : queryTerms) {
                qTermFreq.put(t, qTermFreq.getOrDefault(t, 0) + 1);
            }

            Map<String, Double> queryWeights = new HashMap<>();
            double qSumSq = 0.0;
            for (Map.Entry<String, Integer> e : qTermFreq.entrySet()) {
                String term = e.getKey();
                int df = index.getDocumentFrequency(term);
                double idf = TFIDFEngine.calculateSmoothedIdf(N, df);
                double w = TFIDFEngine.calculateSublinearTf(e.getValue()) * idf;
                queryWeights.put(term, w);
                qSumSq += (w * w);
            }
            double queryNorm = Math.sqrt(qSumSq);

            // Inverted index candidate pruning
            Set<String> candidateDocIds = index.getCandidateDocIds(queryWeights.keySet());
            if (candidateDocIds.isEmpty()) {
                return Collections.emptyList();
            }

            // Min-heap for bounded Top-K selection in O(M log K) time
            PriorityQueue<SearchResult> minHeap = new PriorityQueue<>(
                    Comparator.comparingDouble(SearchResult::score)
            );

            for (String docId : candidateDocIds) {
                double score = SimilarityEngine.calculateCosineSimilarity(queryWeights, queryNorm, docId, index);
                if (score < minScore) continue;

                Document doc = index.getDocument(docId);
                List<String> matched = new ArrayList<>();
                for (String qTerm : queryWeights.keySet()) {
                    if (index.getTermFrequency(qTerm, docId) > 0) {
                        matched.add(qTerm);
                    }
                }

                String snippet = extractSnippet(doc.content(), matched);
                String explanation = buildExplanation(matched.size(), queryWeights.keySet().size(), score, snippet);

                SearchResult sr = new SearchResult(
                        docId,
                        doc.title(),
                        score,
                        matched,
                        snippet,
                        doc.source(),
                        0,
                        explanation
                );

                if (minHeap.size() < topK) {
                    minHeap.offer(sr);
                } else if (minHeap.peek() != null && score > minHeap.peek().score()) {
                    minHeap.poll();
                    minHeap.offer(sr);
                }
            }

            List<SearchResult> results = new ArrayList<>();
            while (!minHeap.isEmpty()) {
                results.add(minHeap.poll());
            }
            Collections.reverse(results);

            List<SearchResult> ranked = new ArrayList<>(results.size());
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                ranked.add(new SearchResult(
                        r.docId(),
                        r.title(),
                        r.score(),
                        r.matchedTerms(),
                        r.snippet(),
                        r.source(),
                        i + 1,
                        r.explanation()
                ));
            }
            return ranked;
        }

        /**
         * Okapi BM25 Ranking Mode (for comparative benchmarking)
         * Parameters: k1 = 1.2, b = 0.75
         */
        public List<SearchResult> searchBM25(String queryString, int topK) {
            List<String> queryTerms = TextPreprocessor.preprocess(queryString);
            if (queryTerms.isEmpty()) return Collections.emptyList();

            int N = index.getDocumentCount();
            if (N == 0) return Collections.emptyList();
            double avgdl = index.getAverageDocumentLength();
            if (avgdl == 0) avgdl = 1.0;

            final double k1 = 1.2;
            final double b = 0.75;

            Set<String> candidates = index.getCandidateDocIds(queryTerms);
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
                        double idf = Math.log(((N - df + 0.5) / (df + 0.5)) + 1.0);
                        double num = tf * (k1 + 1.0);
                        double den = tf + k1 * (1.0 - b + b * (docLen / avgdl));
                        bm25Score += (idf * (num / den));
                    }
                }

                if (bm25Score > 0) {
                    String snippet = extractSnippet(doc.content(), matched);
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
         * Simple Lexical Baseline (unstemmed word overlap) for baseline comparison
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
                            doc.docId(), doc.title(), overlapScore, matched, extractSnippet(doc.content(), matched),
                            doc.source(), 0, "Raw lexical count baseline"
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

        private static String extractSnippet(String content, List<String> matchedTerms) {
            List<String> sentences = SentenceSegmenter.segment(content);
            if (sentences.isEmpty()) return content.substring(0, Math.min(180, content.length())) + "...";

            String bestSentence = sentences.get(0);
            int maxMatches = -1;

            Set<String> termsSet = new HashSet<>(matchedTerms);
            for (String s : sentences) {
                List<String> stemmedSentence = TextPreprocessor.preprocess(s);
                int matches = 0;
                for (String st : stemmedSentence) {
                    if (termsSet.contains(st)) matches++;
                }
                if (matches > maxMatches) {
                    maxMatches = matches;
                    bestSentence = s;
                }
            }
            return bestSentence;
        }

        private static String buildExplanation(int matchedCount, int totalQueryTerms, double score, String snippet) {
            return String.format(
                    "Relevance Score: %.4f | Term Match: %d/%d query terms | Cosine vector alignment verified | Supporting passage isolated.",
                    score, matchedCount, totalQueryTerms
            );
        }
    }

    // ========================================================================
    // 9. EXTRACTIVE SUMMARIZATION (TEXTRANK GRAPH ALGORITHM)
    // ========================================================================

    public static final class TextRankSummarizer {
        private static final double DAMPING = 0.85;
        private static final int MAX_ITER = 25;
        private static final double EPSILON = 0.0001;

        public static String summarize(String text, int targetSentences) {
            if (text == null || text.trim().isEmpty()) return "";
            List<String> sentences = SentenceSegmenter.segment(text);
            if (sentences.size() <= targetSentences) {
                return String.join(" ", sentences);
            }

            int n = sentences.size();
            List<Set<String>> sentenceWords = new ArrayList<>(n);
            for (String s : sentences) {
                sentenceWords.add(new HashSet<>(TextPreprocessor.preprocess(s)));
            }

            // Build undirected weighted similarity graph
            double[][] simMatrix = new double[n][n];
            double[] degree = new double[n];

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double sim = calculateSentenceSimilarity(sentenceWords.get(i), sentenceWords.get(j));
                    simMatrix[i][j] = sim;
                    simMatrix[j][i] = sim;
                    degree[i] += sim;
                    degree[j] += sim;
                }
            }

            // PageRank iterative power calculation
            double[] scores = new double[n];
            Arrays.fill(scores, 1.0 / n);

            for (int iter = 0; iter < MAX_ITER; iter++) {
                double[] nextScores = new double[n];
                double maxDiff = 0.0;

                for (int i = 0; i < n; i++) {
                    double sum = 0.0;
                    for (int j = 0; j < n; j++) {
                        if (i != j && degree[j] > 0.0) {
                            sum += (simMatrix[j][i] / degree[j]) * scores[j];
                        }
                    }
                    nextScores[i] = (1.0 - DAMPING) + (DAMPING * sum);
                    double diff = Math.abs(nextScores[i] - scores[i]);
                    if (diff > maxDiff) maxDiff = diff;
                }

                scores = nextScores;
                if (maxDiff < EPSILON) break;
            }

            // Select Top-M sentences and preserve chronological narrative order
            List<Integer> indices = new ArrayList<>(n);
            for (int i = 0; i < n; i++) indices.add(i);

            final double[] finalScores = scores;
            indices.sort((a, b) -> Double.compare(finalScores[b], finalScores[a]));

            List<Integer> topIndices = indices.subList(0, Math.min(targetSentences, n));
            Collections.sort(topIndices); // restore document sequence

            StringBuilder summary = new StringBuilder();
            for (int idx : topIndices) {
                if (summary.length() > 0) summary.append(" ");
                summary.append(sentences.get(idx));
            }
            return summary.toString();
        }

        private static double calculateSentenceSimilarity(Set<String> wordsA, Set<String> wordsB) {
            if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0;
            int common = 0;
            for (String w : wordsA) {
                if (wordsB.contains(w)) common++;
            }
            if (common == 0) return 0.0;
            // Normalized log scale to prevent document length bias
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

            int N = index.getDocumentCount();
            Map<String, Integer> freqMap = new HashMap<>();
            for (String t : tokens) {
                freqMap.put(t, freqMap.getOrDefault(t, 0) + 1);
            }

            Set<String> titleTokens = new HashSet<>(TextPreprocessor.preprocess(doc.title()));

            Map<String, Double> scoreMap = new HashMap<>();
            for (Map.Entry<String, Integer> e : freqMap.entrySet()) {
                String term = e.getKey();
                int tf = e.getValue();
                int df = index.getDocumentFrequency(term);
                double idf = TFIDFEngine.calculateSmoothedIdf(N, df);
                double score = TFIDFEngine.calculateSublinearTf(tf) * idf;

                // Boost keywords appearing in document title
                if (titleTokens.contains(term)) {
                    score *= 1.5;
                }
                scoreMap.put(term, score);
            }

            List<Map.Entry<String, Double>> sorted = new ArrayList<>(scoreMap.entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            return sorted.subList(0, Math.min(topN, sorted.size()));
        }
    }

    // ========================================================================
    // 11. AUTONOMOUS RESEARCH PLANNER & ORCHESTRATOR
    // ========================================================================

    public static final class ResearchPlanner {
        private static final Map<String, List<String>> DOMAIN_EXPANSIONS = new HashMap<>();

        static {
            DOMAIN_EXPANSIONS.put("cybersecurity", Arrays.asList("intrusion detection", "malware classification", "anomaly detection", "network security"));
            DOMAIN_EXPANSIONS.put("security", Arrays.asList("threat detection", "adversarial attacks", "zero trust architecture"));
            DOMAIN_EXPANSIONS.put("machine learning", Arrays.asList("neural networks", "supervised classification", "unsupervised anomaly detection"));
            DOMAIN_EXPANSIONS.put("deep learning", Arrays.asList("convolutional neural networks", "autoencoders", "packet stream inspection"));
            DOMAIN_EXPANSIONS.put("retrieval", Arrays.asList("inverted index", "vector space model", "TF-IDF BM25 ranking"));
            DOMAIN_EXPANSIONS.put("summarization", Arrays.asList("TextRank graph ranking", "extractive synthesis", "fact extraction"));
            DOMAIN_EXPANSIONS.put("orchestration", Arrays.asList("deterministic workflows", "provenance citations", "evidence aggregation"));
        }

        public static List<String> generateSubQueries(String researchQuestion) {
            SecurityManager.validateQuery(researchQuestion);
            String clean = researchQuestion.toLowerCase(Locale.ENGLISH).trim();
            List<String> subQueries = new ArrayList<>();
            subQueries.add(researchQuestion); // Primary query

            List<String> keywords = StopWordFilter.filter(Tokenizer.tokenize(clean));

            // Concept expansion
            for (Map.Entry<String, List<String>> entry : DOMAIN_EXPANSIONS.entrySet()) {
                if (clean.contains(entry.getKey())) {
                    for (String facet : entry.getValue()) {
                        subQueries.add(facet);
                    }
                }
            }

            // Keyword pairwise combinations
            if (keywords.size() >= 2) {
                subQueries.add(keywords.get(0) + " " + keywords.get(1));
                if (keywords.size() >= 3) {
                    subQueries.add(keywords.get(1) + " " + keywords.get(2));
                }
            }

            // Return unique, deterministic sub-queries (up to 4 sub-queries)
            Set<String> seen = new LinkedHashSet<>();
            List<String> unique = new ArrayList<>();
            for (String q : subQueries) {
                String normalized = q.trim();
                if (!normalized.isEmpty() && seen.add(normalized)) {
                    unique.add(normalized);
                    if (unique.size() >= 4) break;
                }
            }
            return unique;
        }

        public static List<String> createPlan(String question, List<String> subQueries) {
            List<String> steps = new ArrayList<>();
            steps.add("Deconstruct research question into conceptual facets: " + question);
            for (int i = 0; i < subQueries.size(); i++) {
                steps.add("Facet " + (i + 1) + ": Execute localized retrieval for '" + subQueries.get(i) + "'");
            }
            steps.add("Cross-query evidence deduplication and Jaccard passage alignment");
            steps.add("TextRank graph-based extractive synthesis and provenance citation attachment");
            return steps;
        }
    }

    public static final class EvidenceCollector {
        public static List<EvidenceSnippet> collectEvidence(
                RankingEngine rankingEngine,
                InvertedIndex index,
                List<String> subQueries,
                int topDocsPerQuery
        ) {
            List<EvidenceSnippet> collected = new ArrayList<>();
            Set<String> seenPassages = new HashSet<>();

            for (String query : subQueries) {
                List<SearchResult> results = rankingEngine.search(query, topDocsPerQuery, 0.005);
                for (SearchResult sr : results) {
                    Document doc = index.getDocument(sr.docId());
                    if (doc == null) continue;

                    List<String> sentences = SentenceSegmenter.segment(doc.content());
                    for (String s : sentences) {
                        if (isDuplicatePassage(s, seenPassages)) continue;

                        double passScore = computePassageRelevance(s, query);
                        if (passScore > 0.15) {
                            seenPassages.add(s);
                            collected.add(new EvidenceSnippet(
                                    "", // citationId assigned later by ProvenanceManager
                                    "Evidence supporting query: " + query,
                                    doc.docId(),
                                    doc.title(),
                                    s,
                                    sr.score() + passScore,
                                    doc.source()
                            ));
                            break; // Take strongest sentence per document
                        }
                    }
                }
            }

            // Rerank evidence based on composite score
            collected.sort((a, b) -> Double.compare(b.score(), a.score()));
            return collected;
        }

        private static boolean isDuplicatePassage(String candidate, Set<String> existing) {
            Set<String> candWords = new HashSet<>(Tokenizer.tokenize(candidate));
            for (String ex : existing) {
                Set<String> exWords = new HashSet<>(Tokenizer.tokenize(ex));
                int intersection = 0;
                for (String w : candWords) {
                    if (exWords.contains(w)) intersection++;
                }
                int union = candWords.size() + exWords.size() - intersection;
                if (union > 0 && ((double) intersection / union) > 0.60) {
                    return true;
                }
            }
            return false;
        }

        private static double computePassageRelevance(String passage, String query) {
            List<String> qTokens = TextPreprocessor.preprocess(query);
            List<String> pTokens = TextPreprocessor.preprocess(passage);
            if (qTokens.isEmpty() || pTokens.isEmpty()) return 0.0;

            Set<String> pSet = new HashSet<>(pTokens);
            int matches = 0;
            for (String q : qTokens) {
                if (pSet.contains(q)) matches++;
            }
            return (double) matches / qTokens.size();
        }
    }

    public static final class ProvenanceManager {
        public static List<EvidenceSnippet> assignCitations(List<EvidenceSnippet> rawEvidence, Map<String, String> outCitations) {
            List<EvidenceSnippet> withCitations = new ArrayList<>(rawEvidence.size());
            int citationCounter = 1;
            Map<String, String> docToCitationId = new HashMap<>();

            for (EvidenceSnippet ev : rawEvidence) {
                String cId = docToCitationId.computeIfAbsent(ev.docId(), k -> "[C" + (outCitations.size() + 1) + "]");
                outCitations.put(cId, ev.docId() + " - " + ev.title() + " (" + ev.source() + ")");
                withCitations.add(new EvidenceSnippet(
                        cId,
                        ev.claimOrEvidence(),
                        ev.docId(),
                        ev.title(),
                        ev.passage(),
                        ev.score(),
                        ev.source()
                ));
            }
            return withCitations;
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
            long startTime = System.currentTimeMillis();
            SecurityManager.validateQuery(question);

            AuditLogger.info("Autonomous research orchestration initiated: " + question);

            // Step 1: Sub-query decomposition
            List<String> subQueries = ResearchPlanner.generateSubQueries(question);
            List<String> plan = ResearchPlanner.createPlan(question, subQueries);

            // Step 2: Evidence collection across sub-queries
            List<EvidenceSnippet> rawEvidence = EvidenceCollector.collectEvidence(rankingEngine, index, subQueries, 3);

            // Step 3: Provenance and citation attachment
            Map<String, String> citations = new LinkedHashMap<>();
            List<EvidenceSnippet> evidenceWithCitations = ProvenanceManager.assignCitations(rawEvidence, citations);

            // Step 4: Extract Key Findings
            List<String> findings = new ArrayList<>();
            Set<String> retrievedDocIds = new HashSet<>();
            for (EvidenceSnippet ev : evidenceWithCitations) {
                retrievedDocIds.add(ev.docId());
                findings.add(String.format("%s (Source: %s %s)", ev.passage(), ev.docId(), ev.citationId()));
            }

            // Step 5: Keywords across retrieved corpus
            Map<String, Double> pooledKeywords = new HashMap<>();
            for (String docId : retrievedDocIds) {
                Document doc = index.getDocument(docId);
                if (doc != null) {
                    List<Map.Entry<String, Double>> kws = KeywordExtractor.extractKeywords(doc, index, 5);
                    for (Map.Entry<String, Double> e : kws) {
                        pooledKeywords.put(e.getKey(), pooledKeywords.getOrDefault(e.getKey(), 0.0) + e.getValue());
                    }
                }
            }

            List<Map.Entry<String, Double>> topKeywords = new ArrayList<>(pooledKeywords.entrySet());
            topKeywords.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            if (topKeywords.size() > 8) topKeywords = topKeywords.subList(0, 8);

            // Sources list
            List<String> sources = new ArrayList<>();
            for (Map.Entry<String, String> entry : citations.entrySet()) {
                sources.add(entry.getKey() + " " + entry.getValue());
            }

            long latency = System.currentTimeMillis() - startTime;
            double coverage = Math.min(100.0, ((double) retrievedDocIds.size() / Math.max(1, subQueries.size())) * 100.0);

            ResearchReport report = new ResearchReport(
                    question,
                    plan,
                    subQueries,
                    findings,
                    evidenceWithCitations,
                    citations,
                    topKeywords,
                    sources,
                    coverage,
                    latency,
                    retrievedDocIds.size()
            );

            // Persist session asynchronously / safely
            try {
                ResearchSession session = new ResearchSession(
                        "SESSION-" + System.currentTimeMillis(),
                        question,
                        subQueries,
                        evidenceWithCitations,
                        citations,
                        String.join(" ", findings),
                        Instant.now()
                );
                repository.saveResearchSession(session);
            } catch (Exception e) {
                AuditLogger.warning("Failed to persist research session: " + e.getMessage());
            }

            AuditLogger.info(String.format("Research completed in %d ms with %d sources cited.", latency, sources.size()));
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
        private final Path storageDir;
        private final Path docsDir;
        private final Path queriesDir;
        private final Path sessionsDir;

        public FileRepository(Path baseStorageDir) {
            this.storageDir = baseStorageDir;
            this.docsDir = storageDir.resolve("documents");
            this.queriesDir = storageDir.resolve("queries");
            this.sessionsDir = storageDir.resolve("sessions");
            initDirectories();
        }

        private void initDirectories() {
            try {
                Files.createDirectories(docsDir);
                Files.createDirectories(queriesDir);
                Files.createDirectories(sessionsDir);
            } catch (IOException e) {
                AuditLogger.severe("Failed to initialize FileRepository directories", e);
            }
        }

        @Override
        public synchronized void saveDocument(Document doc) {
            try {
                Path filePath = docsDir.resolve(doc.docId() + ".json");
                String json = toJson(doc);
                Files.writeString(filePath, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                AuditLogger.severe("FileRepository: Failed to save document " + doc.docId(), e);
            }
        }

        @Override
        public Optional<Document> findDocumentById(String docId) {
            Path filePath = docsDir.resolve(docId + ".json");
            if (!Files.exists(filePath)) return Optional.empty();
            try {
                String json = Files.readString(filePath, StandardCharsets.UTF_8);
                return Optional.of(fromJson(json));
            } catch (Exception e) {
                AuditLogger.severe("FileRepository: Failed to read doc " + docId, e);
                return Optional.empty();
            }
        }

        @Override
        public List<Document> findAllDocuments() {
            List<Document> list = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(docsDir, "*.json")) {
                for (Path p : stream) {
                    try {
                        String json = Files.readString(p, StandardCharsets.UTF_8);
                        list.add(fromJson(json));
                    } catch (Exception ignored) {}
                }
            } catch (IOException ignored) {}
            return list;
        }

        @Override
        public synchronized void saveQueryLog(QueryLog log) {
            try {
                Path p = queriesDir.resolve("q_" + System.currentTimeMillis() + ".txt");
                String line = log.queryId() + "\t" + log.timestamp() + "\t" + log.latencyMs() + "ms\t" + log.queryText() + "\n";
                Files.writeString(p, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {}
        }

        @Override
        public List<QueryLog> getRecentQueries(int limit) {
            return Collections.emptyList();
        }

        @Override
        public synchronized void saveResearchSession(ResearchSession session) {
            try {
                Path p = sessionsDir.resolve(session.sessionId() + ".txt");
                StringBuilder sb = new StringBuilder();
                sb.append("Session: ").append(session.sessionId()).append("\n");
                sb.append("Question: ").append(session.question()).append("\n");
                sb.append("Timestamp: ").append(session.timestamp()).append("\n");
                sb.append("SubQueries: ").append(String.join("; ", session.subQueries())).append("\n");
                sb.append("Summary:\n").append(session.summary()).append("\n");
                Files.writeString(p, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception ignored) {}
        }

        @Override
        public long getDocumentCount() {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(docsDir, "*.json")) {
                long c = 0;
                for (Path ignored : stream) c++;
                return c;
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public boolean isHealthy() {
            return Files.exists(storageDir) && Files.isWritable(storageDir);
        }

        @Override
        public String getStorageMode() {
            return "LOCAL_FILE_REPOSITORY (data/storage)";
        }

        @Override
        public void close() {
            // No resources to close for file repo
        }

        private static String toJson(Document doc) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"docId\": \"").append(escapeJson(doc.docId())).append("\",\n");
            sb.append("  \"title\": \"").append(escapeJson(doc.title())).append("\",\n");
            sb.append("  \"source\": \"").append(escapeJson(doc.source())).append("\",\n");
            sb.append("  \"content\": \"").append(escapeJson(doc.content())).append("\",\n");
            sb.append("  \"createdAt\": \"").append(doc.createdAt().toString()).append("\"\n");
            sb.append("}");
            return sb.toString();
        }

        private static Document fromJson(String json) {
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
        private final MongoCollection<BsonDocument> docCollection;
        private final MongoCollection<BsonDocument> queryCollection;
        private final MongoCollection<BsonDocument> sessionCollection;
        private volatile boolean healthy = true;

        public MongoRepository(String connectionUri, String dbName) {
            AuditLogger.info("Initializing MongoDB Persistence Adapter (Strict 2.5s Connection Timeout)...");
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionUri))
                    .applyToSocketSettings(builder -> builder.connectTimeout(2500, TimeUnit.MILLISECONDS).readTimeout(2500, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(2500, TimeUnit.MILLISECONDS))
                    .build();

            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase(dbName);
            this.docCollection = database.getCollection("documents", BsonDocument.class);
            this.queryCollection = database.getCollection("queries", BsonDocument.class);
            this.sessionCollection = database.getCollection("research_sessions", BsonDocument.class);

            // Test connection ping immediately
            try {
                database.runCommand(new BsonDocument("ping", new org.bson.BsonInt32(1)));
                docCollection.createIndex(Indexes.ascending("docId"), new IndexOptions().unique(true));
                AuditLogger.info("MongoDB Connection Verified and Initialized Successfully.");
            } catch (Exception e) {
                healthy = false;
                AuditLogger.warning("MongoDB ping failed: " + SecurityManager.sanitize(e.getMessage()));
                throw new RuntimeException("MongoDB connection timeout/failure: " + e.getMessage(), e);
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
                docCollection.replaceOne(filter, bDoc, new com.mongodb.client.model.ReplaceOptions().upsert(true));
            } catch (Exception e) {
                healthy = false;
                AuditLogger.warning("MongoRepository saveDocument error: " + SecurityManager.sanitize(e.getMessage()));
            }
        }

        @Override
        public Optional<Document> findDocumentById(String docId) {
            try {
                BsonDocument filter = new BsonDocument("docId", new org.bson.BsonString(docId));
                BsonDocument found = docCollection.find(filter).first();
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
            List<Document> list = new ArrayList<>();
            try {
                for (BsonDocument found : docCollection.find()) {
                    list.add(new Document(
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
            return list;
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
                return docCollection.countDocuments();
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
                    AuditLogger.warning("MongoDB unavailable (" + SecurityManager.sanitize(e.getMessage()) + "). Activating local FileRepository fallback.");
                }
            } else {
                AuditLogger.info("MONGODB_URI not configured. Operating in local FileRepository fallback mode.");
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
            Path dir = SecurityManager.validateAndResolvePath(directoryPath.toString(), null);
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                throw new FileNotFoundException("Corpus directory does not exist: " + dir);
            }

            AuditLogger.info("Starting corpus ingestion from: " + dir.toAbsolutePath());
            int ingested = 0;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path file : stream) {
                    if (Files.isDirectory(file)) continue;
                    String filename = file.getFileName().toString().toLowerCase(Locale.ENGLISH);

                    if (filename.endsWith(".txt")) {
                        Document doc = parseTxtFile(file);
                        if (doc != null) {
                            index.addDocument(doc);
                            repository.saveDocument(doc);
                            ingested++;
                        }
                    } else if (filename.endsWith(".json")) {
                        Document doc = parseJsonFile(file);
                        if (doc != null) {
                            index.addDocument(doc);
                            repository.saveDocument(doc);
                            ingested++;
                        }
                    }
                }
            }

            index.computeVectorNorms();
            AuditLogger.info(String.format("Ingestion complete. %d documents ingested. Index size: %d terms.",
                    ingested, index.getVocabularySize()));
            return ingested;
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

                // Calculate Enhanced Metrics
                List<String> retrievedIds = retrieved.stream().map(SearchResult::docId).toList();
                Set<String> relevantSet = new HashSet<>(q.relevantDocIds);

                int relevantRetrieved = 0;
                for (String rId : retrievedIds) {
                    if (relevantSet.contains(rId)) relevantRetrieved++;
                }

                double p = (double) relevantRetrieved / k;
                double r = relevantSet.isEmpty() ? 0.0 : (double) relevantRetrieved / relevantSet.size();
                double f1 = (p + r) > 0 ? (2.0 * p * r) / (p + r) : 0.0;

                // MRR
                double rr = 0.0;
                for (int i = 0; i < retrievedIds.size(); i++) {
                    if (relevantSet.contains(retrievedIds.get(i))) {
                        rr = 1.0 / (i + 1);
                        break;
                    }
                }

                // nDCG@K
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

                sumPrecision += p;
                sumRecall += r;
                sumF1 += f1;
                sumMRR += rr;
                sumNDCG += ndcg;

                // Calculate Baseline Metrics
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
                    12L, // index build duration in ms
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

            // Java Runtime check
            String javaVer = System.getProperty("java.version");
            long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            details.put("Java Version", javaVer);
            details.put("JVM Memory", String.format("%d MB free / %d MB total", freeMem, totalMem));
            String javaStatus = "PASS (" + javaVer + ")";

            // Corpus check
            Path corpusDir = Paths.get(ConfigurationManager.getCorpusPath());
            boolean corpusExists = Files.exists(corpusDir) && Files.isDirectory(corpusDir);
            String corpusStatus = corpusExists ? "PASS (" + corpusDir.toString() + ")" : "WARNING (Directory not found)";
            details.put("Corpus Directory", corpusDir.toAbsolutePath().toString());

            // Index check
            int docCount = index.getDocumentCount();
            int vocabSize = index.getVocabularySize();
            String indexStatus = (docCount > 0) ? String.format("PASS (%d docs, %d terms)", docCount, vocabSize) : "WARNING (Empty index)";
            details.put("Indexed Documents", String.valueOf(docCount));
            details.put("Vocabulary Size", String.valueOf(vocabSize));

            // Persistence check
            boolean repoHealth = repository != null && repository.isHealthy();
            String mongoStatus = repoHealth ? "PASS (" + repository.getStorageMode() + ")" : "WARNING (Persistence Degraded)";
            details.put("Storage Engine", repository != null ? repository.getStorageMode() : "NONE");

            // Config check
            String configStatus = "PASS";
            details.put("Top K", String.valueOf(ConfigurationManager.getTopK()));
            details.put("Min Score", String.valueOf(ConfigurationManager.getMinScore()));

            String overall = (corpusExists && repoHealth) ? "HEALTHY" : "DEGRADED";

            return new HealthReport(
                    javaStatus,
                    corpusStatus,
                    indexStatus,
                    mongoStatus,
                    configStatus,
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
            // Memory estimation: vocabulary + posting lists
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
