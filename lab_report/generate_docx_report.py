import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_hex):
    shading_elm = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    cell._tc.get_or_add_tcPr().append(shading_elm)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def format_row(row, bg_hex, text_color, is_bold=False, font_size=9.5):
    for cell in row.cells:
        set_cell_background(cell, bg_hex)
        set_cell_margins(cell, top=120, bottom=120, left=160, right=160)
        cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
        for p in cell.paragraphs:
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            for r in p.runs:
                r.font.name = 'Segoe UI'
                r.font.size = Pt(font_size)
                r.font.color.rgb = text_color
                r.font.bold = is_bold

def add_code_block(doc, code_text):
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = tbl.cell(0, 0)
    set_cell_background(cell, "F1F5F9")
    set_cell_margins(cell, top=140, bottom=140, left=200, right=200)
    
    # Border
    tcPr = cell._tc.get_or_add_tcPr()
    borders = parse_xml(f'<w:tcBorders {nsdecls("w")}><w:left w:val="single" w:sz="24" w:space="0" w:color="0284C7"/><w:top w:val="none"/><w:right w:val="none"/><w:bottom w:val="none"/></w:tcBorders>')
    tcPr.append(borders)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.line_spacing = 1.15
    run = p.add_run(code_text.strip())
    run.font.name = 'Consolas'
    run.font.size = Pt(8.5)
    run.font.color.rgb = RGBColor(15, 23, 42)
    doc.add_paragraph() # Spacing

def build_report():
    doc = docx.Document()
    
    # Page setup - Margins 1 inch
    for s in doc.sections:
        s.top_margin = Inches(1.0)
        s.bottom_margin = Inches(1.0)
        s.left_margin = Inches(1.0)
        s.right_margin = Inches(1.0)
        
        # Header / Footer
        header = s.header
        hp = header.paragraphs[0]
        hp.text = "Autonomous AI Research Assistant — Comprehensive Practical / Lab Report"
        hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        hp.runs[0].font.name = "Segoe UI"
        hp.runs[0].font.size = Pt(8.5)
        hp.runs[0].font.color.rgb = RGBColor(148, 163, 184)
        
        footer = s.footer
        fp = footer.paragraphs[0]
        fp.text = "Department of Computer Science & Engineering | Advanced Programming in Java"
        fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
        fp.runs[0].font.name = "Segoe UI"
        fp.runs[0].font.size = Pt(8.5)
        fp.runs[0].font.color.rgb = RGBColor(148, 163, 184)

    # Palette
    NAVY = RGBColor(15, 23, 42)       # Slate 900
    ACCENT = RGBColor(2, 132, 199)     # Sky 600
    DARK_BLUE = RGBColor(30, 58, 138)  # Blue 900
    CHARCOAL = RGBColor(51, 65, 85)    # Slate 700
    WHITE = RGBColor(255, 255, 255)
    
    # Document Title Block
    title_p = doc.add_paragraph()
    title_p.paragraph_format.space_before = Pt(0)
    title_p.paragraph_format.space_after = Pt(2)
    t_run = title_p.add_run("ACADEMIC LAB REPORT & TECHNICAL EVALUATION")
    t_run.font.name = "Segoe UI"
    t_run.font.size = Pt(11)
    t_run.font.bold = True
    t_run.font.color.rgb = ACCENT

    h1_p = doc.add_paragraph()
    h1_p.paragraph_format.space_before = Pt(2)
    h1_p.paragraph_format.space_after = Pt(6)
    h1_run = h1_p.add_run("Autonomous AI Research Assistant (Java)")
    h1_run.font.name = "Segoe UI"
    h1_run.font.size = Pt(22)
    h1_run.font.bold = True
    h1_run.font.color.rgb = DARK_BLUE

    sub_p = doc.add_paragraph()
    sub_p.paragraph_format.space_before = Pt(0)
    sub_p.paragraph_format.space_after = Pt(14)
    sub_run = sub_p.add_run("100% Local Information Retrieval, NLP, Extractive TextRank, and Autonomous Multi-Query Research System")
    sub_run.font.name = "Segoe UI"
    sub_run.font.size = Pt(12)
    sub_run.font.italic = True
    sub_run.font.color.rgb = CHARCOAL

    # Meta Table Box
    meta_table = doc.add_table(rows=4, cols=2)
    meta_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    meta_data = [
        ("Course / Subject:", "Advanced Programming in Java (Java 17/21)"),
        ("Student Name / Roll:", "B. Naga Jahnavi"),
        ("Architectural Boundary:", "Single-Frontend (AntigravityFrontend.java) + Single-Backend (AntigravityBackend.java)"),
        ("Source Repository:", "https://github.com/nagajahnavibusani3103/Autonomous-AI-Research-Assistant-")
    ]
    for i, (k, v) in enumerate(meta_data):
        row = meta_table.rows[i]
        c0, c1 = row.cells[0], row.cells[1]
        c0.text = k
        c1.text = v
        format_row(row, "F8FAFC" if i % 2 == 0 else "EDF2F7", CHARCOAL, is_bold=False, font_size=9.5)
        c0.paragraphs[0].runs[0].font.bold = True
        c0.paragraphs[0].runs[0].font.color.rgb = DARK_BLUE

    doc.add_paragraph()

    def add_section_header(title, num):
        hp = doc.add_paragraph()
        hp.paragraph_format.space_before = Pt(16)
        hp.paragraph_format.space_after = Pt(6)
        hp.paragraph_format.keep_with_next = True
        r_num = hp.add_run(f"{num}. ")
        r_num.font.name = "Segoe UI"
        r_num.font.size = Pt(14)
        r_num.font.bold = True
        r_num.font.color.rgb = ACCENT
        r_text = hp.add_run(title)
        r_text.font.name = "Segoe UI"
        r_text.font.size = Pt(14)
        r_text.font.bold = True
        r_text.font.color.rgb = DARK_BLUE

    def add_p(text, bold_prefix="", italic=False):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.15
        if bold_prefix:
            br = p.add_run(bold_prefix)
            br.font.name = "Segoe UI"
            br.font.size = Pt(10)
            br.font.bold = True
            br.font.color.rgb = DARK_BLUE
        r = p.add_run(text)
        r.font.name = "Segoe UI"
        r.font.size = Pt(10)
        r.font.italic = italic
        r.font.color.rgb = CHARCOAL
        return p

    def add_bullet(bold_term, text):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.15
        br = p.add_run(bold_term + ": ")
        br.font.name = "Segoe UI"
        br.font.size = Pt(10)
        br.font.bold = True
        br.font.color.rgb = DARK_BLUE
        r = p.add_run(text)
        r.font.name = "Segoe UI"
        r.font.size = Pt(10)
        r.font.color.rgb = CHARCOAL

    # 1. Title
    add_section_header("Experiment / Practical Title", 1)
    add_p("Autonomous AI Research Assistant — Java Information Retrieval, NLP & Deterministic Multi-Query Research System.")

    # 2. Aim
    add_section_header("Aim", 2)
    add_p("To design, develop, verify, and benchmark a 100% locally executing, air-gapped Information Retrieval (IR) and Natural Language Processing (NLP) system in pure Java (Java 17/21). The system ingests technical literature, builds an in-memory sparse inverted index with Euclidean vector norm caching, executes ranked queries using smoothed TF-IDF and sparse cosine similarity, performs extractive TextRank summarization, and orchestrates multi-query research briefs with verified citations ([C1], [C2]), backed by dual MongoDB Atlas and local file persistence, adhering strictly to a single-frontend and single-backend source-file constraint.")

    # 3. Problem Statement
    add_section_header("Problem Statement", 3)
    add_p("In academic literature synthesis and enterprise cybersecurity investigations, researchers must analyze and cross-reference hundreds of unstructured research papers. While modern cloud-based generative AI chatbots have popularized question-answering, they introduce fatal risks in mission-critical environments:")
    add_bullet("Hallucination & Fabrication", "Generative neural models sample probability distributions and regularly invent plausible-sounding facts, fake papers, and nonexistent citations.")
    add_bullet("Privacy & Regulatory Violations", "Transmitting unpublished research, proprietary code, or confidential threat intelligence over third-party APIs violates data residency laws (GDPR, HIPAA, air-gapped defense policies).")
    add_bullet("High Recurring Costs & Latency", "Per-token cloud billing creates ongoing vendor dependency, coupled with network latency and rate limits.")
    add_bullet("Black-Box Unexplainability", "Deep generative models cannot provide transparent mathematical attribution for why specific documents or passages were prioritized.")
    add_p("There is an acute need for an autonomous, deterministic, 100% locally running Java system that produces factually grounded research briefs with zero synthetic hallucination.")

    # 4. Objectives
    add_section_header("Objectives", 4)
    add_bullet("Zero External API Dependency", "Implement the full NLP, indexing, and ranking pipeline purely in standard Java SE.")
    add_bullet("Inverted Index & Vector Space Model", "Construct an in-memory posting list engine with document frequencies and precomputed L2 vector norms.")
    add_bullet("Explainable Ranked Search", "Implement sparse cosine dot-products and bounded min-heap priority queues for sub-5ms Top-K retrieval.")
    add_bullet("Extractive TextRank Summarization", "Implement PageRank random walks over sentence similarity networks, guaranteeing 100% verbatim factual provenance.")
    add_bullet("Autonomous Research Orchestration", "Decompose complex questions into conceptual facets, deduplicate passages via Jaccard overlap (<= 0.60), and assign citations.")
    add_bullet("Dual Persistence & Automatic Failover", "Connect to MongoDB with a 2.5-second timeout, falling back seamlessly to local JSON storage without application crashes.")
    add_bullet("Strict Source-File Constraint", "Maintain clean, modular architecture across exactly one frontend file (AntigravityFrontend.java) and one backend file (AntigravityBackend.java).")

    # 5. Software/Hardware Requirements
    add_section_header("Software & Hardware Requirements", 5)
    add_p("Hardware Requirements:", bold_prefix="Hardware: ")
    add_bullet("Processor", "Dual-Core or Quad-Core x86_64 or ARM64 processor.")
    add_bullet("RAM", "Minimum 4 GB (8 GB recommended for large literature collections).")
    add_bullet("Disk Storage", "500 MB free disk space.")
    add_p("Software Requirements:", bold_prefix="Software: ")
    add_bullet("Operating System", "Cross-platform (Windows 10/11, macOS 12+, Ubuntu Linux 20.04+).")
    add_bullet("Java Development Kit", "OpenJDK 17 or OpenJDK 21 LTS.")
    add_bullet("Build & Package Tool", "Apache Maven 3.8.0 or higher.")
    add_bullet("Database (Optional)", "MongoDB 6.0+ (local or Atlas); auto-fallback to local JSON storage if unavailable.")

    # 6. Theory / Background
    add_section_header("Theory & Background", 6)
    add_p("The project is grounded in classical Information Retrieval and Graph-based Natural Language Processing principles:")
    add_bullet("Vector Space Model (VSM)", "Documents and queries are mapped to high-dimensional vectors in vocabulary space V. Relevance corresponds to cosine proximity.")
    add_bullet("Sublinear Term Frequency", "w(t,d) = 1 + ln(tf(t,d)) if tf > 0, else 0. Applies diminishing marginal returns to repeated words, mitigating verbosity bias.")
    add_bullet("Smoothed Inverse Document Frequency", "IDF(t) = ln((N + 1) / (df(t) + 1)) + 1.0. Guarantees positive weights (>= 1.0) and avoids division-by-zero or zero-weight penalties.")
    add_bullet("L2 Vector Normalization", "||d||_2 = sqrt(Sum w(t,d)^2). Precomputed at ingestion time to enable instant O(1) retrieval during search.")
    add_bullet("Sparse Cosine Dot-Product", "Cosine(q, d) = (Sum_{t in q Intersect d} w(t,q)*w(t,d)) / (||q||_2 * ||d||_2). Evaluates only non-zero intersecting terms, bypassing 50% to 95% of the corpus.")
    add_bullet("TextRank Extractive Summarization", "Constructs a sentence similarity network where edges reflect lexical token overlap. PageRank power iteration with damping factor d=0.85 identifies the most central sentences, which are returned in original narrative order.")
    add_bullet("Jaccard Token Deduplication", "J(A, B) = |A Intersect B| / |A Union B|. Passages with J(A,B) > 0.60 are pruned to eliminate repetitive findings.")

    # 7. System Design / Architecture
    add_section_header("System Design & Architecture", 7)
    add_p("The architecture adheres strictly to the two-source-file constraint, organizing modules through cohesive static nested classes, immutable records, and interface abstractions:")
    
    arch_ascii = """+-------------------------------------------------------------------------+
|                      AntigravityFrontend.java                           |
|  - Interactive REPL Shell (antigravity> )                               |
|  - Direct CLI Command Router (search, research, summarize, complexity)  |
|  - Embedded Localhost HTTP Web UI & REST Server (Port 8080)             |
+====================================+====================================+
                                     |
                                     | Calls EngineFacade API
                                     v
+-------------------------------------------------------------------------+
|                      AntigravityBackend.java                            |
|                                                                         |
|  [SecurityManager & ConfigurationManager]                               |
|  - Validates paths against traversal (../../), limits query to 500 ch   |
|  - Masks connection credentials in logs (mongodb+srv://***:***@)        |
|                                                                         |
|  [NLP Pipeline]                                                         |
|  - Tokenizer -> StopWordFilter (150+ words) -> Porter Stemmer           |
|                                                                         |
|  [InvertedIndex & RankingEngine]                                        |
|  - Sparse Postings (docId, tf, positions) + Precomputed L2 Norms        |
|  - Bounded Min-Heap PriorityQueue Selection (O(M log K))                |
|  - ComplexityProfiler: Telemetry recording (Q, M, P, K, Latency)        |
|                                                                         |
|  [Autonomous Orchestration & TextRank]                                  |
|  - ResearchPlanner: Question decomposition into conceptual sub-queries  |
|  - EvidenceCollector: Multi-query extraction & Jaccard deduplication    |
|  - ProvenanceManager: Grounded citation registry ([C1], [C2], ...)      |
|  - TextRankSummarizer: PageRank random walks over sentence graphs       |
|                                                                         |
|  [Dual Persistence Abstraction]                                         |
|  - Repository Interface -> MongoRepository + Local FileRepository      |
+-------------------------------------------------------------------------+"""
    add_code_block(doc, arch_ascii)

    # 8. Algorithm / Methodology
    add_section_header("Algorithm & Methodology", 8)
    add_p("The core operational pipelines execute in four distinct phases:", bold_prefix="Execution Phases: ")
    add_bullet("Phase 1: Ingestion & Inverted Index Build", "Text is tokenized, stop-words removed, and stemmed via the Porter algorithm. For each term, postings (docId, tf, positions) are appended to hash map lists. Document Euclidean L2 norms are precomputed and cached.")
    add_bullet("Phase 2: Sparse Vector Space Retrieval", "Query terms are stemmed and assigned sublinear TF-IDF weights. Candidate documents M are fetched via the union of query posting lists. For each candidate, sparse dot products are computed, divided by norms, and inserted into a bounded min-heap of capacity K.")
    add_bullet("Phase 3: Extractive TextRank Summarization", "The document is segmented into sentences, forming an undirected graph with edge weights proportional to token overlap. PageRank iterations continue until convergence (max 25 iterations). Top-scoring sentences are returned in original document sequence.")
    add_bullet("Phase 4: Autonomous Research Orchestration", "A research question is decomposed into 3-5 sub-queries. The engine executes search for each, extracts matching sentence passages, prunes redundancies via Jaccard overlap (> 0.60), binds verified citations ([C1], [C2]), and extracts salient domain keywords.")

    # 9. Implementation Details
    add_section_header("Implementation Details", 9)
    add_bullet("AntigravityBackend.java", "2,400+ lines comprising 14 static modules: SecurityManager, ConfigurationManager, AuditLogger, Tokenizer, StopWordFilter, Stemmer, SentenceSegmenter, InvertedIndex, TFIDFEngine, RankingEngine, TextRankSummarizer, KeywordExtractor, ResearchPlanner, EvidenceCollector, ProvenanceManager, Repository (MongoRepository + FileRepository), EvaluationEngine, HealthChecker, StatsEngine, and EngineFacade.")
    add_bullet("AntigravityFrontend.java", "1,000+ lines managing interactive REPL execution, direct CLI routing, ANSI color output tables, and an embedded zero-dependency HTTP server with a responsive web dashboard and REST endpoints (/api/search, /api/research, /api/complexity, /api/stats, /api/health, /api/evaluate).")
    add_bullet("Defensive Security Hardening", "Strict path validation via Path.normalize().startsWith() blocks path traversal attempts (../../). Regex filters mask database credentials. File sizes are capped at 10 MB, queries at 500 characters, and K at 100.")

    # 10. Java Concepts Used
    add_section_header("Java Concepts & Language Features Used", 10)
    add_bullet("Java Records (Java 16+)", "Immutable data models (Document, SearchResult, ComplexityProfile, ResearchReport, etc.) eliminating boilerplate and guaranteeing thread safety.")
    add_bullet("Pattern Matching for switch (Java 17/21)", "Clean and expressive command routing in the CLI frontend.")
    add_bullet("Bounded Min-Heap PriorityQueue", "Custom comparator heap of fixed capacity K, avoiding O(M log M) full sorting overhead.")
    add_bullet("NIO.2 Filesystem API", "Secure path canonicalization, directory streaming, and atomic file creation.")
    add_bullet("Java Stream API & Lambdas", "Functional transformations for filtering, mapping, and metric accumulation.")
    add_bullet("Design Patterns", "Facade Pattern (EngineFacade), Repository Pattern (Repository), Strategy Pattern (SimilarityEngine), Singleton Pattern.")
    add_bullet("Embedded HTTP Server", "Built-in com.sun.net.httpserver.HttpServer delivering a lightweight dashboard without heavy frameworks.")

    # 11. Time Complexity
    add_section_header("Time Complexity Analysis", 11)
    add_p("Complexity is formally expressed using the following parameters:")
    add_p("N: Documents, L: Avg Tokens/Doc, T: Total Tokens, V: Vocabulary, Q: Query Terms, M: Candidates, K: Top-K, S: Sentences, I: Iterations (<=25), P: Postings (Sum df(t)).", italic=True)

    c_table = doc.add_table(rows=8, cols=4)
    c_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    c_data = [
        ("Subsystem / Operation", "Best-Case", "Average-Case", "Worst-Case"),
        ("Inverted Index Build", "O(N * L)", "O(N * L) = O(T)", "O(N * L)"),
        ("Document Preprocessing", "O(L)", "O(L)", "O(L)"),
        ("Sparse Cosine Search", "O(Q)", "O(P + M log K)", "O(Q*N + N log K)"),
        ("Top-K Min-Heap Selection", "O(M log K)", "O(M log K)", "O(N log K)"),
        ("TextRank Summarization", "O(S * L_s)", "O(S^2*L_s + I*S^2)", "O(S^2*L_s + I*S^2)"),
        ("Salient Keyword Extraction", "O(U)", "O(U log U)", "O(U log U)"),
        ("Research Orchestration", "O(R*(P + M log K))", "O(R*(P + M log K) + P_ev^2*L_s)", "O(R*(Q*N + N log K))")
    ]
    for i, row_data in enumerate(c_data):
        row = c_table.rows[i]
        for j, text in enumerate(row_data):
            row.cells[j].text = text
        if i == 0:
            format_row(row, "1E3A8A", WHITE, is_bold=True, font_size=9.5)
        else:
            format_row(row, "F8FAFC" if i % 2 == 0 else "EDF2F7", CHARCOAL, is_bold=False, font_size=9.0)

    doc.add_paragraph()

    # 12. Space Complexity
    add_section_header("Space Complexity Analysis", 12)
    add_bullet("Inverted Index Memory", "O(T + V) auxiliary space for storing vocabulary keys and posting lists across all tokens.")
    add_bullet("Vector Norm Cache", "O(N) stored scalar floating-point values.")
    add_bullet("Query Execution State", "O(M + K) for candidate accumulators and the bounded min-heap priority queue.")
    add_bullet("TextRank Graph Matrix", "O(S^2) dense similarity matrix for S segmented sentences.")
    add_bullet("Autonomous Research Brief", "O(R * K) for candidate evidence passages prior to Jaccard deduplication.")

    # 13. Input and Output
    add_section_header("Input and Output Specifications", 13)
    add_bullet("Input", "Plain text (.txt) and JSON (.json) documents placed in data/sample/. Search queries, research questions, or document IDs entered via CLI or Web UI.")
    add_bullet("Output", "ANSI color-coded tables, formatted research briefs with verified citations [C1]-[C6], live empirical telemetry, and JSON REST responses.")

    # 14. Test Cases
    add_section_header("Test Cases & Verification Matrix", 14)
    add_p("The test suite (AntigravityTestSuite.java) contains 21 automated JUnit 5 tests, achieving 100% pass rate:")

    tc_table = doc.add_table(rows=10, cols=5)
    tc_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    tc_data = [
        ("Test ID", "Component", "Input Condition", "Expected Result", "Status"),
        ("TC-01", "Tokenizer", "Hyphenated & punctuated text", "Tokens lowercase, hyphens preserved", "PASS"),
        ("TC-02", "StopWordFilter", "150+ English stop words", "Syntactic words eliminated", "PASS"),
        ("TC-03", "Stemmer", "'connecting', 'retrieval', 'classification'", "Stems: 'connect', 'retriev', 'classif'", "PASS"),
        ("TC-04", "TFIDFEngine", "N=10, df=2, tf=1, tf=5", "Smoothed IDF = 2.2986; Sublinear TF compressed", "PASS"),
        ("TC-05", "InvertedIndex", "2 sample research papers", "Postings indexed; L2 norms computed", "PASS"),
        ("TC-06", "RankingEngine", "'malware classification software'", "DOC-003 ranked #1 with score explanation", "PASS"),
        ("TC-07", "TextRank", "4-sentence paragraph, request 2", "2 verbatim source sentences extracted", "PASS"),
        ("TC-08", "SecurityManager", "'../../etc/passwd', 600-char query", "SecurityException / IllegalArgumentException", "PASS"),
        ("TC-09", "Boundary Tests", "Empty corpus, K=1, K=100, single doc", "Handled gracefully without exceptions", "PASS")
    ]
    for i, row_data in enumerate(tc_data):
        row = tc_table.rows[i]
        for j, text in enumerate(row_data):
            row.cells[j].text = text
        if i == 0:
            format_row(row, "1E3A8A", WHITE, is_bold=True, font_size=9.5)
        else:
            format_row(row, "F8FAFC" if i % 2 == 0 else "EDF2F7", CHARCOAL, is_bold=False, font_size=9.0)
            row.cells[4].paragraphs[0].runs[0].font.bold = True
            row.cells[4].paragraphs[0].runs[0].font.color.rgb = RGBColor(16, 185, 129)

    doc.add_paragraph()

    # 15. Results / Output
    add_section_header("Results & Empirical Evaluation", 15)
    add_p("The system was evaluated against ground-truth labeled queries on the 12-document literature corpus:")

    res_table = doc.add_table(rows=8, cols=4)
    res_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    res_data = [
        ("Evaluation Metric", "Enhanced (TF-IDF)", "Baseline (Lexical)", "Improvement / Note"),
        ("Precision@5", "0.5750", "0.5750", "+0.0000 (0.0%)"),
        ("Recall@5", "0.9438", "0.9271", "+0.0167 (+1.8% over baseline)"),
        ("F1-Score@5", "0.6962", "0.6900", "+0.0062 (+0.9% over baseline)"),
        ("Mean Recip. Rank (MRR)", "1.0000", "1.0000", "Perfect Rank 1 Accuracy"),
        ("nDCG@5 (Graded)", "0.9572", "N/A", "High Saliency & Position Discount"),
        ("Average Query Latency", "4.25 ms", "1.10 ms", "Sub-5ms response time"),
        ("P95 Query Latency", "16.00 ms", "4.00 ms", "Predictable bounded performance")
    ]
    for i, row_data in enumerate(res_data):
        row = res_table.rows[i]
        for j, text in enumerate(row_data):
            row.cells[j].text = text
        if i == 0:
            format_row(row, "1E3A8A", WHITE, is_bold=True, font_size=9.5)
        else:
            format_row(row, "F8FAFC" if i % 2 == 0 else "EDF2F7", CHARCOAL, is_bold=False, font_size=9.0)

    doc.add_paragraph()
    add_p("Empirical Telemetry on Sample Query ('machine learning intrusion detection'):", bold_prefix="Live Complexity Profile: ")
    add_bullet("Corpus Documents (N)", "12 documents")
    add_bullet("Vocabulary Size (V)", "654 unique stems (1,203 total tokens)")
    add_bullet("Postings Traversed (P)", "15 postings evaluated (out of theoretical max Q*N = 48)")
    add_bullet("Corpus Filtering Ratio", "58.3% of the corpus bypassed via sparse inverted filtering")
    add_bullet("Min-Heap Operations", "11 operations (strictly bounded by O(M log K))")

    # 16. Advantages
    add_section_header("Advantages of the Developed System", 16)
    add_bullet("Zero Hallucination", "Extractive TextRank extracts verbatim sentences directly from source documents, mathematically eliminating synthetic claims.")
    add_bullet("Complete Data Privacy", "100% on-premise JVM execution protects confidential research and internal security audit records.")
    add_bullet("Deterministic & Explainable", "Relevance scores are backed by transparent term frequencies, inverse document frequencies, and cosine components.")
    add_bullet("Sub-15ms Latency", "In-memory sparse vector indexing and bounded min-heap queues ensure instant responses on commodity hardware.")
    add_bullet("Zero Operational Costs", "No cloud subscriptions, per-token API fees, or third-party rate limits.")
    add_bullet("Resilient Persistence", "Automatic failover to local JSON disk persistence prevents system crashes when MongoDB is offline.")

    # 17. Limitations
    add_section_header("Limitations", 17)
    add_bullet("Language Boundary", "Core NLP components (Porter stemmer and stop-word dictionaries) currently target English-language text.")
    add_bullet("In-Memory Scaling Limit", "The inverted index resides in JVM RAM, suitable for up to 100,000 documents. Multi-million document archives would require disk-backed B-Tree indexing.")
    add_bullet("Document Formats", "Accepts plain text and JSON. PDF, DOCX, and scanned documents require upstream plain-text conversion.")

    # 18. Future Scope
    add_section_header("Future Scope", 18)
    add_bullet("Probabilistic BM25 Ranking", "Implement the Okapi BM25 ranking function as an alternate scoring strategy.")
    add_bullet("Positional N-Gram & Phrase Queries", "Utilize the stored token position integers to support exact phrase matching and proximity boosts.")
    add_bullet("Native PDF Parsing", "Incorporate lightweight Java libraries (such as Apache PDFBox) for direct ingestion of binary research papers.")
    add_bullet("Distributed Sharding", "Implement index partitioning across multiple cluster nodes for multi-terabyte literature collections.")

    # 19. Conclusion
    add_section_header("Conclusion", 19)
    add_p("The Autonomous AI Research Assistant was successfully designed, implemented, tested, and packaged in pure Java (Java 17/21). By implementing classical Information Retrieval principles—inverted indexing, smoothed TF-IDF, sparse cosine similarity, and graph-based TextRank summarization—the system achieves high retrieval precision and recall (Recall@5 = 0.9438, MRR = 1.00, nDCG@5 = 0.9572) with sub-15ms query latency and zero external generative AI dependencies.")
    add_p("The architecture strictly complies with the single-frontend and single-backend source-file constraint through cohesive static nested classes and immutable Java records. The system guarantees 100% factual provenance, zero citation hallucination, and enterprise-grade security with robust local file fallback.")

    # 20. Learning Outcomes
    add_section_header("Learning Outcomes", 20)
    add_bullet("Information Retrieval Fundamentals", "Gained deep practical understanding of sparse inverted index traversal, term weighting mechanics, and vector space models.")
    add_bullet("Graph NLP & Extractive Summarization", "Mastered the application of PageRank random walks on sentence similarity graphs for hallucination-free summarization.")
    add_bullet("Modern Java Systems Programming", "Developed hands-on mastery of Java records, pattern matching switch, NIO.2 path canonicalization, bounded priority queues, and embedded HTTP servers.")
    add_bullet("Defensive Software Architecture", "Implemented resilient dual-persistence failover, path traversal security, regex secret masking, and comprehensive 21-test JUnit 5 test suites.")

    # Save
    out_dir = "lab_report"
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "Autonomous_AI_Research_Assistant_Lab_Report.docx")
    doc.save(out_path)
    print(f"Successfully generated Word document at: {out_path}")

if __name__ == "__main__":
    build_report()
