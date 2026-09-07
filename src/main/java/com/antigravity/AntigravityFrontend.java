package com.antigravity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * ============================================================================
 * AUTONOMOUS AI RESEARCH ASSISTANT - COMMAND LINE FRONTEND
 * ============================================================================
 * Architecture: Senior-SDE Polished Java CLI with Interactive Shell & Direct Execution.
 * Responsibilities:
 *  - CLI User Interface & Input Parsing
 *  - Interactive REPL Mode (antigravity> )
 *  - Direct Command Dispatch (java -jar ... <command>)
 *  - Formatted ASCII Visual Reports, Tables, and Progress Indicators
 *  - Zero External Web / HTML / CSS / JS Dependencies
 * ============================================================================
 */
public final class AntigravityFrontend {

    // ANSI Escape Colors
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String RED = "\u001B[31m";

    public static void main(String[] args) {
        AntigravityBackend.EngineFacade engine = AntigravityBackend.EngineFacade.getInstance();

        if (args.length == 0) {
            // Interactive REPL Shell Mode
            runInteractiveShell(engine);
        } else {
            // Direct Execution Mode
            executeCommand(args, engine);
        }
    }

    private static void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("+================================================================================+");
        System.out.println("|                  AUTONOMOUS AI RESEARCH ASSISTANT -- JAVA                      |");
        System.out.println("|        Information Retrieval * Inverted Index * NLP * Research Orchestration   |");
        System.out.println("+================================================================================+" + RESET);
        System.out.println(BLUE + " Pure Java Engine | Local TF-IDF & Cosine Similarity | TextRank Extractive Summarizer" + RESET);
        System.out.println(BLUE + " Dual Persistence: Official MongoDB Driver + Local File Fallback (Zero Secret Leak)" + RESET);
        System.out.println();
    }

    private static void runInteractiveShell(AntigravityBackend.EngineFacade engine) {
        printBanner();
        System.out.println("Type " + GREEN + BOLD + "help" + RESET + " to view available commands, or " + YELLOW + BOLD + "exit" + RESET + " to quit.");
        System.out.println();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print(CYAN + BOLD + "antigravity> " + RESET);
                String line = reader.readLine();
                if (line == null) break;

                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    System.out.println(GREEN + "Shutting down Autonomous AI Research Assistant. Goodbye!" + RESET);
                    engine.shutdown();
                    break;
                }

                String[] parsedArgs = parseArgs(line);
                executeCommand(parsedArgs, engine);
                System.out.println();
            }
        } catch (Exception e) {
            System.err.println(RED + "Interactive session error: " + e.getMessage() + RESET);
        }
    }

    private static void executeCommand(String[] args, AntigravityBackend.EngineFacade engine) {
        if (args == null || args.length == 0) return;
        String cmd = args[0].toLowerCase(Locale.ENGLISH).trim();

        try {
            switch (cmd) {
                case "help" -> handleHelp();
                case "ingest" -> handleIngest(args, engine);
                case "search" -> handleSearch(args, engine);
                case "research" -> handleResearch(args, engine);
                case "summarize" -> handleSummarize(args, engine);
                case "keywords" -> handleKeywords(args, engine);
                case "stats" -> handleStats(engine);
                case "evaluate" -> handleEvaluate(engine);
                case "health" -> handleHealth(engine);
                case "config" -> handleConfig();
                default -> {
                    System.out.println(RED + "Unknown command: '" + cmd + "'. Type 'help' for documentation." + RESET);
                }
            }
        } catch (IllegalArgumentException | SecurityException e) {
            System.out.println(YELLOW + "Input/Security Warning: " + e.getMessage() + RESET);
        } catch (NoSuchElementException e) {
            System.out.println(RED + "Not Found: " + e.getMessage() + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Operation Error: " + AntigravityBackend.SecurityManager.sanitize(e.getMessage()) + RESET);
        }
    }

    private static void handleHelp() {
        System.out.println(BOLD + "AVAILABLE COMMANDS:" + RESET);
        System.out.printf("  %-32s %s%n", GREEN + "help" + RESET, "Display this comprehensive command reference guide");
        System.out.printf("  %-32s %s%n", GREEN + "ingest <path>" + RESET, "Ingest and index research documents from local folder (.txt / .json)");
        System.out.printf("  %-32s %s%n", GREEN + "search \"<query>\" [k]" + RESET, "Ranked TF-IDF Vector Space search with explainable scoring");
        System.out.printf("  %-32s %s%n", GREEN + "research \"<question>\"" + RESET, "Autonomous multi-query research, evidence extraction & citation brief");
        System.out.printf("  %-32s %s%n", GREEN + "summarize <docId> [sentences]" + RESET, "Graph-based TextRank extractive summary of a specific document");
        System.out.printf("  %-32s %s%n", GREEN + "keywords <docId> [topN]" + RESET, "Extract high-salience domain keywords using TF-IDF & positional weights");
        System.out.printf("  %-32s %s%n", GREEN + "stats" + RESET, "Display corpus, inverted index, vocabulary, and storage statistics");
        System.out.printf("  %-32s %s%n", GREEN + "evaluate" + RESET, "Run benchmark suite (Precision@K, Recall@K, MRR, nDCG@K vs Baseline)");
        System.out.printf("  %-32s %s%n", GREEN + "health" + RESET, "Perform full system diagnostics (JVM, Corpus, Index, MongoDB, Config)");
        System.out.printf("  %-32s %s%n", GREEN + "config" + RESET, "Display active system configuration (with all credentials safely masked)");
        System.out.printf("  %-32s %s%n", GREEN + "exit" + RESET, "Exit the interactive command shell");
    }

    private static void handleIngest(String[] args, AntigravityBackend.EngineFacade engine) throws Exception {
        String pathStr = args.length > 1 ? args[1] : AntigravityBackend.ConfigurationManager.getCorpusPath();
        System.out.println(BLUE + "Ingesting research documents from: " + pathStr + "..." + RESET);
        long t0 = System.currentTimeMillis();
        int count = engine.ingest(pathStr);
        long elapsed = System.currentTimeMillis() - t0;
        System.out.println(GREEN + BOLD + "✓ Ingestion Complete!" + RESET +
                String.format(" Ingested and indexed %d documents in %d ms.", count, elapsed));
    }

    private static void handleSearch(String[] args, AntigravityBackend.EngineFacade engine) {
        if (args.length < 2) {
            System.out.println(YELLOW + "Usage: search \"<query>\" [k]" + RESET);
            return;
        }
        String query = args[1];
        int k = args.length > 2 ? Integer.parseInt(args[2]) : AntigravityBackend.ConfigurationManager.getTopK();

        System.out.println(BLUE + "Executing Ranked Vector Space Search for: \"" + query + "\" (Top-" + k + ")..." + RESET);
        long t0 = System.currentTimeMillis();
        List<AntigravityBackend.SearchResult> results = engine.search(query, k);
        long latency = System.currentTimeMillis() - t0;

        if (results.isEmpty()) {
            System.out.println(YELLOW + "No matching documents found above relevance threshold." + RESET);
            return;
        }

        System.out.println();
        System.out.println(BOLD + "====================================================================================================" + RESET);
        System.out.printf(BOLD + "%-4s | %-10s | %-7s | %-38s | %s%n" + RESET, "RANK", "DOC ID", "SCORE", "TITLE", "MATCHED TERMS");
        System.out.println("----------------------------------------------------------------------------------------------------");

        for (AntigravityBackend.SearchResult r : results) {
            String title = r.title().length() > 36 ? r.title().substring(0, 33) + "..." : r.title();
            String matched = String.join(", ", r.matchedTerms());
            System.out.printf("%-4d | %-10s | %-7.4f | %-38s | %s%n",
                    r.rank(), r.docId(), r.score(), title, matched);
            System.out.println("     " + CYAN + "Evidence: \"" + r.snippet() + "\"" + RESET);
            System.out.println("     " + MAGENTA + "Why Ranked: " + r.explanation() + RESET);
            System.out.println("----------------------------------------------------------------------------------------------------");
        }
        System.out.printf(GREEN + "Search completed in %d ms. %d candidate results ranked.%n" + RESET, latency, results.size());
    }

    private static void handleResearch(String[] args, AntigravityBackend.EngineFacade engine) {
        if (args.length < 2) {
            System.out.println(YELLOW + "Usage: research \"<question>\"" + RESET);
            return;
        }
        String question = args[1];

        System.out.println();
        System.out.println(CYAN + BOLD + "+================================================================================+" + RESET);
        System.out.println(CYAN + BOLD + "|                     AUTONOMOUS RESEARCH BRIEF ORCHESTRATION                    |" + RESET);
        System.out.println(CYAN + BOLD + "+================================================================================+" + RESET);
        System.out.println(BOLD + "Question: " + RESET + question);
        System.out.println();

        AntigravityBackend.ResearchReport report = engine.research(question);

        System.out.println(BOLD + "1. DETERMINISTIC RESEARCH PLAN & SUB-QUERIES:" + RESET);
        for (int i = 0; i < report.researchPlan().size(); i++) {
            System.out.printf("   %d. %s%n", i + 1, report.researchPlan().get(i));
        }
        System.out.println();

        System.out.println(BOLD + "2. SYNTHESIZED KEY FINDINGS (EXTRACTIVE EVIDENCE):" + RESET);
        if (report.keyFindings().isEmpty()) {
            System.out.println("   No direct evidence passages located in current local corpus.");
        } else {
            for (int i = 0; i < report.keyFindings().size(); i++) {
                System.out.printf("   [Finding %d] %s%n", i + 1, report.keyFindings().get(i));
            }
        }
        System.out.println();

        System.out.println(BOLD + "3. SOURCE PROVENANCE & CITATION REGISTRY:" + RESET);
        for (String src : report.sources()) {
            System.out.printf("   * %s%n", src);
        }
        System.out.println();

        System.out.println(BOLD + "4. SALIENT TOPIC KEYWORDS:" + RESET);
        StringBuilder kwStr = new StringBuilder("   ");
        for (Map.Entry<String, Double> kw : report.topKeywords()) {
            kwStr.append(String.format("%s (%.2f)  ", kw.getKey(), kw.getValue()));
        }
        System.out.println(kwStr);
        System.out.println();

        System.out.println(BOLD + "5. EXECUTION METRICS:" + RESET);
        System.out.printf("   * Evidence Coverage   : %.1f%%%n", report.evidenceCoverage());
        System.out.printf("   * Query Latency       : %d ms%n", report.queryLatencyMs());
        System.out.printf("   * Documents Evaluated : %d%n", report.documentsRetrieved());
        System.out.println(CYAN + "+================================================================================+" + RESET);
    }

    private static void handleSummarize(String[] args, AntigravityBackend.EngineFacade engine) {
        if (args.length < 2) {
            System.out.println(YELLOW + "Usage: summarize <docId> [numSentences]" + RESET);
            return;
        }
        String docId = args[1].trim().toUpperCase(Locale.ENGLISH);
        int sentences = args.length > 2 ? Integer.parseInt(args[2]) : 3;

        System.out.println(BLUE + "Computing TextRank Extractive Summary for: " + docId + "..." + RESET);
        long t0 = System.currentTimeMillis();
        String summary = engine.summarize(docId, sentences);
        long elapsed = System.currentTimeMillis() - t0;

        System.out.println();
        System.out.println(BOLD + "--- TEXTRANK EXTRACTIVE SUMMARY (" + docId + ", " + sentences + " Sentences) ---" + RESET);
        System.out.println(summary);
        System.out.println();
        System.out.printf(GREEN + "TextRank convergence achieved in %d ms.%n" + RESET, elapsed);
    }

    private static void handleKeywords(String[] args, AntigravityBackend.EngineFacade engine) {
        if (args.length < 2) {
            System.out.println(YELLOW + "Usage: keywords <docId> [topN]" + RESET);
            return;
        }
        String docId = args[1].trim().toUpperCase(Locale.ENGLISH);
        int topN = args.length > 2 ? Integer.parseInt(args[2]) : 10;

        System.out.println(BLUE + "Extracting Top " + topN + " Keywords for: " + docId + "..." + RESET);
        List<Map.Entry<String, Double>> keywords = engine.keywords(docId, topN);

        System.out.println();
        System.out.println(BOLD + String.format("%-4s | %-25s | %s", "RANK", "KEYWORD", "SCORE") + RESET);
        System.out.println("--------------------------------------------------");
        int r = 1;
        for (Map.Entry<String, Double> e : keywords) {
            System.out.printf("%-4d | %-25s | %.4f%n", r++, e.getKey(), e.getValue());
        }
    }

    private static void handleStats(AntigravityBackend.EngineFacade engine) {
        AntigravityBackend.SystemStats s = engine.stats();
        System.out.println();
        System.out.println(CYAN + BOLD + "==================================================" + RESET);
        System.out.println(CYAN + BOLD + "            SYSTEM & INDEX STATISTICS             " + RESET);
        System.out.println(CYAN + BOLD + "==================================================" + RESET);
        System.out.printf("  %-26s : %d%n", "Documents Indexed", s.documentsCount());
        System.out.printf("  %-26s : %d unique terms%n", "Vocabulary Size", s.vocabularySize());
        System.out.printf("  %-26s : %d tokens%n", "Total Token Count", s.totalTokens());
        System.out.printf("  %-26s : %.2f tokens/doc%n", "Average Document Length", s.avgDocLength());
        System.out.printf("  %-26s : ~%d KB%n", "Estimated Index Memory", s.indexSizeBytes() / 1024);
        System.out.printf("  %-26s : %s%n", "Storage Persistence Mode", s.databaseMode());
        System.out.printf("  %-26s : %s%n", "Last Index Rebuild", s.lastIndexBuildTime() != null ? s.lastIndexBuildTime().toString() : "N/A");
        System.out.println("==================================================");
    }

    private static void handleEvaluate(AntigravityBackend.EngineFacade engine) throws Exception {
        System.out.println(BLUE + "Executing Rigorous Retrieval Evaluation Benchmark Suite..." + RESET);
        AntigravityBackend.EvaluationMetrics m = engine.evaluate();

        System.out.println();
        System.out.println(CYAN + BOLD + "+==================================================================================+" + RESET);
        System.out.println(CYAN + BOLD + "|             INFORMATION RETRIEVAL EVALUATION & BENCHMARK REPORT                  |" + RESET);
        System.out.println(CYAN + BOLD + "+==================================================================================+" + RESET);
        System.out.println();
        System.out.printf(BOLD + "%-24s | %-20s | %-20s | %s%n" + RESET, "EVALUATION METRIC", "ENHANCED (TF-IDF)", "BASELINE (RAW LEXICAL)", "IMPROVEMENT");
        System.out.println("------------------------------------------------------------------------------------");
        printMetricRow("Precision@5", m.precisionAtK(), m.baselinePrecision());
        printMetricRow("Recall@5", m.recallAtK(), m.baselineRecall());
        printMetricRow("F1-Score@5", m.f1AtK(), m.baselineF1());
        printMetricRow("Mean Recip. Rank (MRR)", m.mrr(), m.baselineMRR());
        System.out.printf("%-24s | %-20.4f | %-20s | %s%n", "nDCG@5 (Graded)", m.ndcgAtK(), "N/A", GREEN + "+Graded Rank Saliency" + RESET);
        System.out.println("------------------------------------------------------------------------------------");
        System.out.println(BOLD + "SYSTEM LATENCY & RESOURCE PROFILE:" + RESET);
        System.out.printf("  * Average Query Latency : %.2f ms%n", m.avgLatencyMs());
        System.out.printf("  * P95 Query Latency     : %.2f ms%n", m.p95LatencyMs());
        System.out.printf("  * Ingestion & Index Time: %d ms%n", m.indexBuildTimeMs());
        System.out.printf("  * Corpus Test Size      : %d documents%n", m.corpusSize());
        System.out.println(CYAN + "+==================================================================================+" + RESET);
    }

    private static void printMetricRow(String name, double enhanced, double baseline) {
        double delta = enhanced - baseline;
        String deltaStr = delta >= 0 ? String.format(GREEN + "+%.4f (%.1f%%)" + RESET, delta, baseline > 0 ? (delta / baseline) * 100 : 0.0)
                : String.format(RED + "%.4f" + RESET, delta);
        System.out.printf("%-24s | %-20.4f | %-20.4f | %s%n", name, enhanced, baseline, deltaStr);
    }

    private static void handleHealth(AntigravityBackend.EngineFacade engine) {
        AntigravityBackend.HealthReport h = engine.health();
        System.out.println();
        System.out.println(BOLD + "==================================================" + RESET);
        System.out.println(BOLD + "             SYSTEM HEALTH DIAGNOSTICS            " + RESET);
        System.out.println("==================================================");
        System.out.printf("  %-20s : %s%n", "Java Runtime", h.javaRuntimeStatus());
        System.out.printf("  %-20s : %s%n", "Local Corpus", h.corpusStatus());
        System.out.printf("  %-20s : %s%n", "Search Index", h.indexStatus());
        System.out.printf("  %-20s : %s%n", "Storage Persistence", h.mongoStatus());
        System.out.printf("  %-20s : %s%n", "Configuration", h.configStatus());
        System.out.println("--------------------------------------------------");
        System.out.printf("  %-20s : %s%n", "OVERALL STATUS",
                h.overallStatus().equals("HEALTHY") ? GREEN + BOLD + "HEALTHY" + RESET : YELLOW + BOLD + "DEGRADED" + RESET);
        System.out.println("==================================================");
    }

    private static void handleConfig() {
        Map<String, String> cfg = AntigravityBackend.ConfigurationManager.getSafeConfigView();
        System.out.println();
        System.out.println(BOLD + "ACTIVE SYSTEM CONFIGURATION (SANITIZED):" + RESET);
        System.out.println("----------------------------------------------------------------------");
        for (Map.Entry<String, String> e : cfg.entrySet()) {
            System.out.printf("  %-22s = %s%n", e.getKey(), e.getValue());
        }
        System.out.println("----------------------------------------------------------------------");
    }

    private static String[] parseArgs(String line) {
        List<String> list = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    list.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            list.add(current.toString());
        }
        return list.toArray(new String[0]);
    }
}
