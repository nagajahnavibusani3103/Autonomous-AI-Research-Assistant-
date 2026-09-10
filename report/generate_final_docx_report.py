import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn

def create_project_report():
    doc = docx.Document()

    # -------------------------------------------------------------
    # Page Setup: A4, 1-inch margins
    # -------------------------------------------------------------
    section = doc.sections[0]
    section.page_width = Inches(8.27)   # A4 Width
    section.page_height = Inches(11.69) # A4 Height
    section.top_margin = Inches(1.0)
    section.bottom_margin = Inches(1.0)
    section.left_margin = Inches(1.0)
    section.right_margin = Inches(1.0)

    # -------------------------------------------------------------
    # Header & Footer Setup
    # -------------------------------------------------------------
    header = section.header
    header_para = header.paragraphs[0]
    header_para.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    hrun = header_para.add_run("Autonomous AI Research Assistant | B.Tech Project Report")
    hrun.font.name = "Calibri"
    hrun.font.size = Pt(8.5)
    hrun.font.color.rgb = RGBColor(113, 128, 150)

    footer = section.footer
    footer_para = footer.paragraphs[0]
    footer_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    frun = footer_para.add_run("Department of Computer Science and Engineering  •  Page ")
    frun.font.name = "Calibri"
    frun.font.size = Pt(9)
    frun.font.color.rgb = RGBColor(113, 128, 150)
    # Add page number XML
    fld = parse_xml(r'<w:fldSimple %s w:instr="PAGE"/>' % nsdecls('w'))
    footer_para._p.append(fld)

    # -------------------------------------------------------------
    # Color Palette Definitions
    # -------------------------------------------------------------
    NAVY = RGBColor(26, 54, 93)      # #1A365D - Primary Headings
    BLUE = RGBColor(43, 108, 176)    # #2B6CB0 - Subheadings
    TEAL = RGBColor(44, 122, 123)    # #2C7A7B - Accents
    DARK = RGBColor(45, 55, 72)      # #2D3748 - Body Text
    GRAY = RGBColor(113, 128, 150)   # #718096 - Metadata & Captions

    # -------------------------------------------------------------
    # Helper Functions
    # -------------------------------------------------------------
    def add_title(text):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(36)
        p.paragraph_format.space_after = Pt(12)
        run = p.add_run(text)
        run.font.name = "Calibri"
        run.font.size = Pt(24)
        run.font.bold = True
        run.font.color.rgb = NAVY
        return p

    def add_h1(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(20)
        p.paragraph_format.space_after = Pt(6)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = "Calibri"
        run.font.size = Pt(15)
        run.font.bold = True
        run.font.color.rgb = NAVY
        return p

    def add_h2(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(14)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = "Calibri"
        run.font.size = Pt(12.5)
        run.font.bold = True
        run.font.color.rgb = BLUE
        return p

    def add_h3(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(10)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = "Calibri"
        run.font.size = Pt(11)
        run.font.bold = True
        run.font.color.rgb = TEAL
        return p

    def add_p(text, bold_prefix=None, space_after=5):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(space_after)
        p.paragraph_format.line_spacing = 1.15
        if bold_prefix:
            brun = p.add_run(bold_prefix)
            brun.font.name = "Calibri"
            brun.font.size = Pt(10.5)
            brun.font.bold = True
            brun.font.color.rgb = DARK
        run = p.add_run(text)
        run.font.name = "Calibri"
        run.font.size = Pt(10.5)
        run.font.color.rgb = DARK
        return p

    def add_bullet(text, bold_prefix=None):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.line_spacing = 1.15
        if bold_prefix:
            brun = p.add_run(bold_prefix)
            brun.font.name = "Calibri"
            brun.font.size = Pt(10)
            brun.font.bold = True
            brun.font.color.rgb = DARK
        run = p.add_run(text)
        run.font.name = "Calibri"
        run.font.size = Pt(10)
        run.font.color.rgb = DARK
        return p

    def add_image(image_path, caption_text, width_in=6.0):
        if os.path.exists(image_path):
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p.paragraph_format.space_before = Pt(12)
            p.paragraph_format.space_after = Pt(4)
            p.paragraph_format.keep_with_next = True
            p.add_run().add_picture(image_path, width=Inches(width_in))
            
            cp = doc.add_paragraph()
            cp.alignment = WD_ALIGN_PARAGRAPH.CENTER
            cp.paragraph_format.space_before = Pt(2)
            cp.paragraph_format.space_after = Pt(12)
            crun = cp.add_run(caption_text)
            crun.font.name = "Calibri"
            crun.font.size = Pt(9.5)
            crun.font.bold = True
            crun.font.color.rgb = NAVY
        else:
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            run = p.add_run(f"[Placeholder: {caption_text}]")
            run.font.italic = True
            run.font.color.rgb = GRAY

    def style_table(table, header_bg="1A365D", alt_bg="F7FAFC", col_widths=None):
        table.alignment = WD_TABLE_ALIGNMENT.CENTER
        for i, row in enumerate(table.rows):
            trPr = row._tr.get_or_add_trPr()
            trPr.append(parse_xml(r'<w:cantSplit %s/>' % nsdecls('w')))
            if i == 0:
                trPr.append(parse_xml(r'<w:tblHeader %s/>' % nsdecls('w')))
            for j, cell in enumerate(row.cells):
                cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
                if col_widths and j < len(col_widths):
                    cell.width = Inches(col_widths[j])
                
                # Margins
                tcPr = cell._tc.get_or_add_tcPr()
                tcMar = parse_xml(r'<w:tcMar %s><w:top w:w="120" w:type="dxa"/><w:bottom w:w="120" w:type="dxa"/><w:left w:w="150" w:type="dxa"/><w:right w:w="150" w:type="dxa"/></w:tcMar>' % nsdecls('w'))
                tcPr.append(tcMar)

                # Shading
                if i == 0:
                    shd = parse_xml(r'<w:shd %s w:fill="%s"/>' % (nsdecls('w'), header_bg))
                    tcPr.append(shd)
                    for cp in cell.paragraphs:
                        cp.alignment = WD_ALIGN_PARAGRAPH.CENTER
                        for run in cp.runs:
                            run.font.name = "Calibri"
                            run.font.size = Pt(9.5)
                            run.font.bold = True
                            run.font.color.rgb = RGBColor(255, 255, 255)
                else:
                    if i % 2 == 0:
                        shd = parse_xml(r'<w:shd %s w:fill="%s"/>' % (nsdecls('w'), alt_bg))
                        tcPr.append(shd)
                    for cp in cell.paragraphs:
                        for run in cp.runs:
                            run.font.name = "Calibri"
                            run.font.size = Pt(9)
                            run.font.color.rgb = DARK

    # =============================================================
    # 1. COVER PAGE
    # =============================================================
    add_title("AUTONOMOUS AI RESEARCH ASSISTANT")

    sub_p = doc.add_paragraph()
    sub_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub_p.paragraph_format.space_after = Pt(28)
    srun = sub_p.add_run("A Pure Java Information Retrieval, Vector Space TF-IDF Search,\nand Extractive Summarization Engine")
    srun.font.name = "Calibri"
    srun.font.size = Pt(13)
    srun.font.color.rgb = BLUE

    tag_p = doc.add_paragraph()
    tag_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    tag_p.paragraph_format.space_after = Pt(40)
    trun = tag_p.add_run("A Major Academic Project Report Submitted in Partial Fulfillment\nof the Requirements for the Degree of\nBachelor of Technology in Computer Science and Engineering")
    trun.font.name = "Calibri"
    trun.font.size = Pt(11)
    trun.font.italic = True
    trun.font.color.rgb = DARK

    # Metadata Table
    meta_table = doc.add_table(rows=8, cols=2)
    meta_data = [
        ("Student Name(s)", "[Student Name / Candidate Name]"),
        ("Registration Number(s)", "[Registration / Roll Number]"),
        ("Degree & Branch", "B.Tech — Computer Science and Engineering"),
        ("Course / Subject", "Programming in Java / Advanced Software Lab (CS-302)"),
        ("Faculty Guide / Evaluator", "[Faculty Guide Name, Designation]"),
        ("Department", "Department of Computer Science and Engineering"),
        ("Institution / University", "[University / College Name]"),
        ("Academic Year & Date", "2025 – 2026  |  September 2026")
    ]
    for r_idx, (lbl, val) in enumerate(meta_data):
        row = meta_table.rows[r_idx]
        row.cells[0].paragraphs[0].add_run(lbl)
        row.cells[1].paragraphs[0].add_run(val)
    style_table(meta_table, header_bg="2B6CB0", alt_bg="F0F4F8", col_widths=[2.8, 3.7])

    note_p = doc.add_paragraph()
    note_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    note_p.paragraph_format.space_before = Pt(36)
    nrun = note_p.add_run("100% Pure Java Implementation  •  Zero External LLM APIs  •  MongoDB Atlas with Local Fallback")
    nrun.font.name = "Calibri"
    nrun.font.size = Pt(9.5)
    nrun.font.bold = True
    nrun.font.color.rgb = TEAL

    doc.add_page_break()

    # =============================================================
    # 2. TABLE OF CONTENTS
    # =============================================================
    add_h1("TABLE OF CONTENTS")
    
    toc_items = [
        ("1. Introduction", "3"),
        ("2. Problem Statement", "4"),
        ("3. Functional Requirements", "5"),
        ("4. Non-Functional Requirements", "7"),
        ("5. System Architecture", "9"),
        ("6. Design Diagrams", "11"),
        ("    6.1 Use Case Diagram", "11"),
        ("    6.2 Workflow Diagram", "12"),
        ("    6.3 Sequence Diagram", "13"),
        ("    6.4 Class & Component Diagram", "14"),
        ("    6.5 Data Model & ER Diagram", "15"),
        ("7. Design Decisions & Rationale", "16"),
        ("8. Implementation Details", "18"),
        ("    8.1 Project Architecture & Build Setup", "18"),
        ("    8.2 Frontend CLI & Web Server", "18"),
        ("    8.3 Core NLP Preprocessing Pipeline", "19"),
        ("    8.4 Inverted Indexing & Vector Norm Precomputation", "20"),
        ("    8.5 TF-IDF & Sparse Cosine Relevance Scoring", "21"),
        ("    8.6 Bounded Min-Heap Top-K Ranking", "22"),
        ("    8.7 TextRank Graph-Based Extractive Summarization", "22"),
        ("    8.8 Autonomous Research Orchestration & Citations", "23"),
        ("    8.9 Resilient Dual-Tier Persistence", "24"),
        ("    8.10 Security Controls & Safe Logging", "25"),
        ("    8.11 Java Language Features Used", "25"),
        ("    8.12 Time and Space Complexity Analysis", "26"),
        ("9. Screenshots & Experimental Results", "29"),
        ("10. Testing Approach & Verification Matrix", "35"),
        ("11. Challenges Faced & Solutions", "38"),
        ("12. Conclusion & Summary of Learning", "40"),
        ("13. References", "41")
    ]
    
    toc_table = doc.add_table(rows=len(toc_items), cols=2)
    for idx, (title, pg) in enumerate(toc_items):
        r = toc_table.rows[idx]
        p0 = r.cells[0].paragraphs[0]
        p0.paragraph_format.space_before = Pt(1)
        p0.paragraph_format.space_after = Pt(1)
        run0 = p0.add_run(title)
        run0.font.name = "Calibri"
        run0.font.size = Pt(9.5)
        if not title.startswith("    "):
            run0.font.bold = True
            run0.font.color.rgb = NAVY
        else:
            run0.font.color.rgb = DARK

        p1 = r.cells[1].paragraphs[0]
        p1.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        p1.paragraph_format.space_before = Pt(1)
        p1.paragraph_format.space_after = Pt(1)
        run1 = p1.add_run(pg)
        run1.font.name = "Calibri"
        run1.font.size = Pt(9.5)
        run1.font.color.rgb = GRAY
    style_table(toc_table, header_bg="FFFFFF", alt_bg="FFFFFF", col_widths=[5.5, 1.0])

    doc.add_page_break()

    # =============================================================
    # 3. SECTION 1: INTRODUCTION
    # =============================================================
    add_h1("1. INTRODUCTION")

    add_p("The Autonomous AI Research Assistant is a specialized Information Retrieval (IR) and Natural Language Processing (NLP) software application written entirely in the Java programming language (Java 17/21). The system was developed to assist students, researchers, and technical analysts in synthesizing large volumes of technical documents, literature papers, and research notes without relying on external third-party cloud APIs, subscription-based AI services, or proprietary large language models.")

    add_p("In academic and engineering research, users frequently work with specialized corpora containing technical articles, research publications, vulnerability reports, and architecture specifications. Searching through these collections using simple keyword matching (such as basic substring grep) frequently misses relevant literature due to morphological variations in human language (for example, missing 'detecting' or 'detection' when searching for 'detect'). Conversely, sending proprietary or internal research materials to commercial generative AI services introduces significant data privacy risks, recurring monetary costs, network latency, and the well-documented phenomenon of hallucination—where language models fabricate false assertions or cite fictitious academic papers.")

    add_p("To resolve these challenges, this project implements a complete, self-contained Information Retrieval and NLP pipeline using standard Java. The system runs entirely on the user's local machine, providing deterministic results based on transparent, well-established mathematical algorithms. The primary technical capabilities of the application include:")

    add_bullet("Corpus Ingestion & Preprocessing: Parsing plain text (.txt) and structured JSON (.json) documents, normalizing text, filtering out common syntactic stop words, stemming morphological variants using Porter's algorithm, and segmenting documents into sentences.")
    add_bullet("Inverted Indexing: Constructing an in-memory inverted index that tracks term frequencies, posting positions, and document frequencies, while precomputing Euclidean vector lengths for fast vector space scoring.")
    add_bullet("Ranked Vector Space Search: Ranking documents using sublinear term frequency weighting, smoothed inverse document frequency (IDF), and sparse vector cosine similarity, prioritized through a bounded min-heap priority queue.")
    add_bullet("Extractive TextRank Summarization: Constructing a sentence similarity graph based on normalized lexical overlap and running iterative PageRank random walks to extract the most informative sentences without generating artificial text.")
    add_bullet("Autonomous Multi-Query Research Orchestration: Decomposing broad research questions into targeted sub-queries, retrieving candidate evidence passages across documents, deduplicating findings using Jaccard token similarity, and compiling structured research briefs with verified citations ([C1], [C2]).")
    add_bullet("Resilient Dual-Tier Storage: Persisting document records and telemetry to MongoDB Atlas via the official Java driver, with seamless automatic fallback to a local JSON file repository if the network or database is unavailable.")
    add_bullet("Interactive Client Interfaces: Providing an interactive command-line shell (REPL), direct CLI execution, and an embedded localhost HTTP web dashboard without requiring external web frameworks.")

    add_p("By implementing all algorithmic components from scratch in Java without third-party NLP or AI frameworks, this project demonstrates core Computer Science principles including object-oriented design, algorithm optimization, asymptotic complexity analysis, defensive programming, and rigorous unit testing.")

    # =============================================================
    # 4. SECTION 2: PROBLEM STATEMENT
    # =============================================================
    add_h1("2. PROBLEM STATEMENT")

    add_p("Modern researchers, engineering teams, and students face an overwhelming volume of technical documentation, research papers, and software specifications. When reviewing literature for a project or investigation, analysts encounter several critical bottlenecks:")

    add_p("1. Ineffectiveness of Naive Search Tools:", bold_prefix="a. ")
    add_p("Traditional operating system search tools and simple text editors rely on exact character substring matching. They cannot evaluate conceptual relevance, fail to account for word frequency distributions, and do not understand morphological variations (e.g., treating 'classifying', 'classifiers', and 'classification' as completely unrelated tokens). Consequently, finding relevant documents requires manually formulating dozens of repetitive queries.")

    add_p("2. The Hallucination Liability of Commercial Generative AI:", bold_prefix="b. ")
    add_p("While modern generative conversational models (chatbots) can synthesize text, they operate as statistical next-token predictors rather than deterministic retrieval engines. In scientific, legal, and cybersecurity domains, these models regularly hallucinate citations, attribute quotes to papers that do not exist, and summarize documents inaccurately. In academic coursework and engineering analysis, unverified claims can lead to invalid technical conclusions.")

    add_p("3. Data Residency and Privacy Exposure:", bold_prefix="c. ")
    add_p("Many research collections involve unpublished intellectual property, proprietary software architectures, or confidential security audit reports. Uploading such documents to cloud-hosted APIs violates institutional privacy guidelines and non-disclosure agreements.")

    add_p("4. Operational Fragility and Ongoing API Cost:", bold_prefix="d. ")
    add_p("Cloud-hosted AI solutions require constant Internet connectivity, account credentials, and recurring billing per token processed. If network connectivity drops or the service experiences an outage, research activities halt completely.")

    add_p("Motivation and Scope of Solution:", bold_prefix="Project Motivation: ")
    add_p("The objective of this project is to build an autonomous, explainable, and 100% locally executing research assistant in Java that delivers the benefits of automated document discovery and evidence synthesis while eliminating the liabilities of external black-box models. The system must ingest raw technical literature, index terms efficiently, rank documents mathematically, extract verbatim supporting evidence with exact document citations, and operate reliably in offline environments.")

    # =============================================================
    # 5. SECTION 3: FUNCTIONAL REQUIREMENTS
    # =============================================================
    add_h1("3. FUNCTIONAL REQUIREMENTS")

    add_p("The project implements fourteen distinct functional requirements. Each requirement represents a tested, concrete capability present in the source code:")

    fr_data = [
        ("FR-1", "Document Ingestion", "Ingest research literature from specified folders containing .txt and .json files.", "File system directory path", "Path validation, size checks (<10MB), file reading, JSON parsing", "Indexed documents in memory & storage"),
        ("FR-2", "NLP Preprocessing", "Normalize text for indexing and search.", "Raw text string", "Tokenization (regex), case folding, stop-word removal (150+ terms), Porter stemming (5 steps), sentence boundary detection", "Stream of filtered, stemmed root tokens"),
        ("FR-3", "Inverted Indexing", "Build searchable index mapping terms to postings.", "Preprocessed document tokens", "Populate posting lists with term frequencies and positions; compute document lengths and Euclidean norms", "In-memory InvertedIndex data structure"),
        ("FR-4", "Vector Space Search", "Retrieve ranked documents for a search query.", "Query string, Top-K limit", "Query preprocessing, candidate posting lookup, sublinear TF-IDF calculation, sparse cosine similarity, min-heap selection", "Ranked List of SearchResult objects with score explanations"),
        ("FR-5", "Autonomous Research", "Synthesize multi-query research investigation for complex questions.", "Research question string", "Question decomposition into sub-queries, multi-search execution, evidence extraction, Jaccard deduplication, citation mapping", "Structured ResearchReport with plan, findings, citations, keywords"),
        ("FR-6", "TextRank Summarization", "Extract most representative sentences from a document.", "Document ID, sentence count S", "Sentence segmentation, lexical overlap graph construction, iterative PageRank computation (d=0.85), top sentence extraction", "Extractive summary preserving source order"),
        ("FR-7", "Keyword Extraction", "Extract top distinguishing domain terms for a document.", "Document ID, Top-N limit", "Calculate composite term score = TF * IDF * titleBoost * positionFactor; sort descending", "List of high-salience terms with scores"),
        ("FR-8", "Citation & Provenance", "Bind extracted evidence sentences to verifiable source documents.", "Extracted evidence sentences", "Assign sequential citation markers ([C1], [C2]); map markers to document ID, title, and source metadata", "Immutable Provenance Citation Registry"),
        ("FR-9", "Dual-Tier Persistence", "Persist documents, queries, and research sessions.", "Domain entities / JSON records", "Attempt official MongoDB driver write; on failure or timeout (2.5s), seamlessly fallback to local JSON file storage", "Persistent storage in MongoDB or data/storage/"),
        ("FR-10", "Benchmark Evaluation", "Evaluate retrieval quality against ground truth.", "evaluate CLI command", "Execute test queries against ground_truth.json; compute Precision@5, Recall@5, F1@5, MRR, nDCG@5 vs baseline", "Comparative benchmark report table"),
        ("FR-11", "Complexity Telemetry", "Inspect asymptotic bounds and live query counters.", "complexity [query] command", "Collect theoretical bounds; measure empirical counters (Q, M, P, K, latency, heap memory)", "Formatted complexity telemetry report"),
        ("FR-12", "Health Diagnostics", "Check operational readiness of runtime subsystems.", "health CLI command", "Inspect JVM memory, corpus files, index status, database connectivity, and configuration", "System Health Report (PASS/FAIL)"),
        ("FR-13", "Configuration & Masking", "Manage runtime properties securely.", "config.properties / ENV vars", "Load properties; apply regex masks to connection strings and credentials before display", "Masked configuration summary"),
        ("FR-14", "Localhost HTTP Web UI", "Provide browser-based interface and REST endpoints.", "server [port] CLI command", "Initialize com.sun.net.httpserver.HttpServer; register REST handlers (/api/search, /api/research...); serve HTML UI", "Interactive Web Dashboard at localhost:8080")
    ]

    fr_table = doc.add_table(rows=len(fr_data)+1, cols=6)
    headers = ["Req ID", "Requirement Name", "Description", "Input", "Processing Logic", "Output"]
    for j, h in enumerate(headers):
        fr_table.rows[0].cells[j].paragraphs[0].add_run(h)
    for i, row_data in enumerate(fr_data):
        row = fr_table.rows[i+1]
        for j, val in enumerate(row_data):
            row.cells[j].paragraphs[0].add_run(val)
    style_table(fr_table, header_bg="1A365D", alt_bg="F7FAFC", col_widths=[0.6, 1.1, 1.6, 1.0, 1.5, 1.2])

    doc.add_page_break()

    # =============================================================
    # 6. SECTION 4: NON-FUNCTIONAL REQUIREMENTS
    # =============================================================
    add_h1("4. NON-FUNCTIONAL REQUIREMENTS")

    add_p("Non-functional requirements describe the quality attributes, security boundaries, performance constraints, and architectural standards enforced across the system:")

    add_h2("4.1 Performance and Latency")
    add_bullet("Sub-50ms Search Latency: Inverted index lookups and TF-IDF cosine ranking must execute in under 50 milliseconds for standard queries. In actual benchmarks across the research collection, average query latency measured between 3.13 ms and 6.13 ms.", bold_prefix="Search Speed: ")
    add_bullet("Fast Cold-Start Ingestion: The system must ingest, preprocess, tokenize, stem, and index the entire 12-document technical corpus in under 100 milliseconds. Actual measured ingestion time is 12 ms.", bold_prefix="Ingestion Speed: ")
    add_bullet("Bounded Memory Footprint: The in-memory inverted index, posting lists, and vocabulary must fit within modest JVM heap constraints (< 100 KB RAM for the test collection), enabling execution on resource-constrained consumer laptops.", bold_prefix="Memory Efficiency: ")

    add_h2("4.2 Security and Data Protection")
    add_bullet("Path Traversal Protection: User-supplied file paths are validated using java.nio.file.Path.normalize() and verified against base directory boundaries. Any attempt to traverse directories using sequences such as '../../' or injecting null bytes ('\\0') throws a SecurityException and is blocked.", bold_prefix="Path Security: ")
    add_bullet("Credential Log Masking: Database connection strings and credentials (such as MongoDB Atlas URIs) are filtered through regex masks before being written to logs, console displays, or exceptions. Passwords and tokens appear strictly as '***'.", bold_prefix="Secret Masking: ")
    add_bullet("Resource Safety Bounds: Input queries are capped at 500 characters, result limits at K=100, and individual ingested file sizes at 10 MB to prevent denial-of-service and out-of-memory crashes.", bold_prefix="Input Constraints: ")

    add_h2("4.3 Reliability and Fault Tolerance")
    add_bullet("Zero Crash on Database Failure: If MongoDB Atlas is unreachable or network connectivity is severed, the system does not terminate. A 2,500 millisecond connection timeout triggers an automatic, graceful failover to the local JSON FileRepository.", bold_prefix="Database Failover: ")
    add_bullet("Atomic Local File Storage: Local storage operations write data safely using standard Java NIO file channels, preventing file corruption in the event of sudden process termination.", bold_prefix="Storage Integrity: ")

    add_h2("4.4 Maintainability and Code Quality")
    add_bullet("Two-Source-File Architecture: The complete application is implemented in exactly one frontend file (ResearchAssistantFrontend.java) and one backend file (ResearchAssistantBackend.java). Maintainability is achieved through cleanly encapsulated static nested classes, immutable Java records, and interface abstractions.", bold_prefix="Clean Modularity: ")
    add_bullet("Zero External Frameworks: By avoiding heavy enterprise frameworks like Spring Boot, the codebase has minimal external dependencies, compiles instantly via standard Maven, and packages into an executable fat JAR.", bold_prefix="Portability: ")

    add_h2("4.5 Determinism and Reproducibility")
    add_bullet("100% Mathematical Determinism: Given identical document collections and queries, the mathematical formulas for sublinear TF-IDF, cosine similarity, and TextRank produce identical numerical scores and ranking positions every time.", bold_prefix="Reproducibility: ")
    add_bullet("Zero Hallucination Guarantee: All evidence snippets and summary sentences are extracted verbatim from indexed text. The system never generates synthesized or fabricated sentences.", bold_prefix="Factual Accuracy: ")

    # =============================================================
    # 7. SECTION 5: SYSTEM ARCHITECTURE
    # =============================================================
    add_h1("5. SYSTEM ARCHITECTURE")

    add_p("The Autonomous AI Research Assistant is organized into a modular, multi-tier software architecture. The design enforces a strict separation of concerns between user interaction, application orchestration, natural language processing, information retrieval algorithms, and data persistence.")

    add_p("The architectural workflow proceeds through four primary layers, as illustrated in Figure 1:")

    add_bullet("1. Client & Presentation Layer: Provides two user-facing interfaces: (a) an interactive command-line shell (REPL) with command history and styled ANSI output, and (b) an embedded HTTP server hosting a lightweight, dependency-free web dashboard at localhost:8080 with REST API endpoints.", bold_prefix="Presentation Layer: ")
    add_bullet("2. Application Facade & Controller Layer: The EngineFacade singleton serves as the unified entry point to the backend engine. It coordinates request lifecycle, enforces input validation via SecurityManager, and logs sanitized telemetry via AuditLogger.", bold_prefix="Facade Layer: ")
    add_bullet("3. Core NLP, Information Retrieval & Reasoning Layer: Implements all core algorithms in pure Java. TextPreprocessor handles tokenization, stop-word elimination, Porter stemming, and sentence segmentation. InvertedIndex maintains posting lists and precomputed L2 vector norms. RankingEngine and SimilarityEngine execute sparse cosine scoring and Top-K min-heap selection. TextRankSummarizer runs graph-based sentence ranking. ResearchOrchestrator coordinates multi-query decomposition, Jaccard passage deduplication, and citation generation.", bold_prefix="Processing Layer: ")
    add_bullet("4. Resilient Dual-Tier Persistence Layer: The RepositoryManager abstraction provides unified CRUD operations. It attempts persistence via MongoRepository (official MongoDB driver with TLS and 2.5s socket timeout); upon timeout or disconnection, it fails over immediately to FileRepository (local JSON files in data/storage/).", bold_prefix="Persistence Layer: ")

    add_image("report/figures/fig1_system_architecture.png", "Figure 1: Autonomous AI Research Assistant — Layered System Architecture", width_in=6.2)

    doc.add_page_break()

    # =============================================================
    # 8. SECTION 6: DESIGN DIAGRAMS
    # =============================================================
    add_h1("6. DESIGN DIAGRAMS")

    add_p("This section presents the comprehensive UML and system design models representing the functional, behavioral, structural, and data persistence aspects of the application.")

    # 6.1 Use Case Diagram
    add_h2("6.1 Use Case Diagram")
    add_p("Figure 2 illustrates the interactions between system actors and the ten primary use cases supported by the application. The primary actor is the Student Researcher / Analyst who queries the literature, runs research workflows, generates summaries, and inspects complexity telemetry. The secondary actor is the Persistence Engine, which archives query and session logs.")

    add_image("report/figures/fig2_use_case_diagram.png", "Figure 2: UML Use Case Diagram", width_in=6.0)

    add_p("Key Use Case Flows:", bold_prefix="Use Case Descriptions: ")
    add_bullet("UC-2 (Execute Ranked Search): The user inputs a query; the system pre-processes terms, looks up postings, evaluates sparse cosine similarities, and presents Top-K ranked documents with score attribution.")
    add_bullet("UC-3 (Conduct Autonomous Research): The user poses a broad research question; the planner decomposes it into 3-4 domain sub-queries, gathers evidence passages, deduplicates overlapping sentences, and outputs a citation-backed brief.")
    add_bullet("UC-4 (Generate TextRank Summary): The user supplies a document ID; the system builds a sentence similarity network, computes eigenvector centralities via PageRank, and extracts the top sentences.")

    # 6.2 Workflow Diagram
    add_h2("6.2 System Workflow Diagram")
    add_p("Figure 3 depicts the end-to-end procedural workflow from initial command receipt to final output rendering. It highlights the input validation gate, the command routing decision diamond, and the parallel pipeline paths for Ranked Search versus Autonomous Research.")

    add_image("report/figures/fig3_workflow_diagram.png", "Figure 3: System Workflow Diagram", width_in=5.8)

    doc.add_page_break()

    # 6.3 Sequence Diagram
    add_h2("6.3 UML Sequence Diagram")
    add_p("Figure 4 details the chronological message exchange between system objects during the execution of a Ranked Vector Space Search. It traces the lifecycle from user input, through the EngineFacade and TextPreprocessor, across the InvertedIndex and RankingEngine, to result display.")

    add_image("report/figures/fig4_sequence_diagram.png", "Figure 4: UML Sequence Diagram — Ranked Search Execution", width_in=6.2)

    add_p("Sequence Step Explanations:", bold_prefix="Execution Steps: ")
    add_bullet("Steps 1-2: User issues search command; Frontend passes query string and parameter K to EngineFacade.")
    add_bullet("Steps 3-4: EngineFacade calls TextPreprocessor to normalize, filter stop words, and stem query terms.")
    add_bullet("Steps 5-7: RankingEngine requests candidate posting lists from InvertedIndex, obtaining precomputed L2 document vector norms.")
    add_bullet("Step 8: RankingEngine computes sparse cosine dot-products and maintains a bounded min-heap PriorityQueue of size K.")
    add_bullet("Steps 9-11: Extracted Top-K SearchResult records are formatted into a human-readable table and displayed to the user.")

    # 6.4 Class & Component Diagram
    add_h2("6.4 UML Class and Component Diagram")
    add_p("Figure 5 shows the object-oriented structure of the system, including core records, service classes, interface contracts, and dependency relationships.")

    add_image("report/figures/fig5_class_component_diagram.png", "Figure 5: UML Class & Component Architecture", width_in=6.2)

    doc.add_page_break()

    # 6.5 Data Model & ER Diagram
    add_h2("6.5 Data Model & Document Entity Diagram")
    add_p("Because the application utilizes document-oriented storage (MongoDB Atlas) paired with a local JSON file storage fallback, data is represented logically as JSON document collections rather than relational SQL tables. Figure 6 illustrates the schema, field types, index keys, and logical relationships between the collections:")

    add_image("report/figures/fig6_data_model_er_diagram.png", "Figure 6: Data Model & Document Entity Diagram (MongoDB & Local Storage)", width_in=6.0)

    add_p("Collection Specifications:", bold_prefix="Document Collections: ")
    add_bullet("documents Collection: Stores ingested research papers. Keys include _id (ObjectId), docId (String, unique index for O(1) lookup), title, source, content, metadata, and createdAt (Long timestamp index). In local fallback mode, each document is stored as data/storage/documents/<docId>.json.")
    add_bullet("queries Collection: Logs search queries for auditing and latency profiling. Fields include queryId (unique index), queryText, k, resultCount, latencyMs, and timestamp. Local fallback stores records under data/storage/queries/.")
    add_bullet("research_sessions Collection: Stores autonomous research summaries. Fields include sessionId (unique index), question, subQueries array, findings array (with source document references), citations map, and timestamp. Local fallback writes to data/storage/sessions/.")

    doc.add_page_break()

    # =============================================================
    # 9. SECTION 7: DESIGN DECISIONS & RATIONALE
    # =============================================================
    add_h1("7. DESIGN DECISIONS & RATIONALE")

    add_p("Every technical design decision in this project was made to balance algorithmic efficiency, data integrity, code readability, and student maintainability. Below is the detailed rationale for key engineering choices:")

    decisions = [
        ("1. Selection of Java 17/21 as Single Language",
         "The college curriculum and project constraints required pure Java. Java provides strong type safety, high JVM execution performance, cross-platform portability, built-in concurrency primitives, and modern language features like records without requiring native bindings.",
         "High execution speed, clean compilation, zero external runtime dependencies.",
         "More verbose string manipulation compared to dynamic scripting languages."),

        ("2. 100% Local Execution (No Cloud AI APIs)",
         "Cloud AI APIs introduce latency, recurring monetary costs, network dependency, privacy exposure, and hallucination risks.",
         "Zero API billing, instant offline operation, complete data privacy, and 100% factually verifiable outputs grounded in source texts.",
         "Cannot generate conversational synthetic narratives; outputs are strictly extractive."),

        ("3. Inverted Index Data Structure",
         "Searching large text collections by scanning every document (linear scan) takes O(N * L) time per query, which does not scale as the corpus grows.",
         "Allows the search engine to examine only documents containing query terms (the candidate set M), reducing query time to O(P) where P is the posting count.",
         "Requires memory overhead of O(T + V) to store posting lists and vocabulary in RAM."),

        ("4. Sublinear TF and Smoothed IDF Weighting",
         "Raw term frequency (TF) gives excessive weight to repeated words (a document with 10 occurrences is not 10 times more relevant than one with 1). Standard IDF can produce negative scores or division-by-zero on rare terms.",
         "Sublinear TF (1 + ln(tf)) dampens high frequencies; smoothed IDF (ln((N+1)/(df+1)) + 1) guarantees positive weights and handles unseen terms gracefully.",
         "Slightly more arithmetic computation per candidate posting."),

        ("5. Sparse Vector Cosine Similarity",
         "Dense vector space models compute dot products across the entire vocabulary V for all documents, requiring O(N * V) operations.",
         "Sparse dot products only evaluate terms present in the query, evaluating only candidate documents M and normalizing by precomputed document Euclidean norms.",
         "Requires precomputing and storing Euclidean norms (||d||_2) during document ingestion."),

        ("6. Graph-Based TextRank Extractive Summarization",
         "Abstractive summarizers generate new sentences that frequently introduce factual errors and hallucinations.",
         "TextRank models sentences as vertices in a graph, weights edges by lexical overlap, and computes sentence prestige using PageRank. Extracted sentences are 100% verbatim.",
         "Constructing the sentence similarity graph takes O(S^2 * L_s) time, suitable for documents with S <= 50 sentences."),

        ("7. Repository Pattern with Dual-Tier Fallback",
         "Relying solely on an external cloud database causes application failure if internet access is interrupted.",
         "The Repository interface decouples business logic from persistence. MongoRepository connects to MongoDB Atlas; if unreachable within 2.5s, FileRepository stores records locally.",
         "Maintaining two repository implementations increases code volume."),

        ("8. Bounded Min-Heap for Top-K Selection",
         "Sorting all M candidate document scores requires O(M log M) time and O(M) additional memory.",
         "Using a Java PriorityQueue of fixed capacity K reduces selection time to O(M log K). Because K <= 10, log2(K) is negligible.",
         "PriorityQueue elements must be extracted in reverse order to yield descending rank."),

        ("9. Modular Nested Architecture in Two Source Files",
         "The project structure was constrained to exactly one frontend file and one backend file to ensure streamlined deployment and evaluation.",
         "Using encapsulated static nested classes (Tokenizer, InvertedIndex, RankingEngine...) and immutable records maintains strict object-oriented design and SOLID principles.",
         "Source files are longer than multi-file projects, requiring careful internal sectioning.")
    ]

    for title, reason, benefit, tradeoff in decisions:
        add_h2(title)
        add_p(reason, bold_prefix="Reason: ")
        add_p(benefit, bold_prefix="Benefit: ")
        add_p(tradeoff, bold_prefix="Trade-off: ")

    doc.add_page_break()

    # =============================================================
    # 10. SECTION 8: IMPLEMENTATION DETAILS
    # =============================================================
    add_h1("8. IMPLEMENTATION DETAILS")

    add_p("This section describes the internal engineering of the Autonomous AI Research Assistant, explaining each processing stage from raw document ingestion to ranked output rendering.")

    add_h2("8.1 Project Architecture & Build Setup")
    add_p("The project is managed using Apache Maven (pom.xml) configured for Java 17+. The build uses the maven-compiler-plugin (source/target 17), maven-surefire-plugin (for automated JUnit 5 execution), and the maven-shade-plugin to create a self-contained executable fat JAR (autonomous-ai-research-assistant.jar). The only external library is the official MongoDB synchronous driver (mongodb-driver-sync:4.11.1), while JUnit Jupiter (5.10.2) is scoped strictly to tests.")

    add_h2("8.2 Frontend Client & Embedded Web Server")
    add_p("The user interface is encapsulated in ResearchAssistantFrontend.java. It supports three distinct execution modes:")
    add_bullet("Interactive REPL Shell: Launched by executing the application without arguments. Provides an interactive command prompt (research-assistant>) that accepts commands such as search, research, summarize, evaluate, and stats.")
    add_bullet("Direct Command Execution: Commands can be invoked directly from the terminal (e.g., java -jar ... search \"cybersecurity\" 5), enabling script automation and batch processing.")
    add_bullet("Embedded Localhost HTTP Server: Invoked via 'server [port]', starting an embedded com.sun.net.httpserver.HttpServer on port 8080. It serves an interactive HTML/CSS dashboard and handles REST requests (/api/search, /api/research, /api/stats, /api/health) without external servlet containers.")

    add_h2("8.3 Natural Language Processing (NLP) Pipeline")
    add_p("Raw text cannot be indexed directly. The TextPreprocessor coordinates four sequential text transformation modules:")
    add_bullet("Tokenizer: Uses the regular expression pattern [a-zA-Z0-9]+(-[a-zA-Z0-9]+)* to extract word tokens while preserving hyphens in compound technical terms (such as 'zero-trust' or 'cross-entropy'). All characters are converted to lowercase.")
    add_bullet("StopWordFilter: Filters out high-frequency grammatical English words (150+ terms including 'the', 'is', 'and', 'which', 'their') using an O(1) hash set lookup, preventing non-informative terms from inflating posting lists.")
    add_bullet("Porter Stemmer: Implements the full five-phase morphological stemming algorithm developed by Martin Porter. It reduces words to their common root stems (e.g., 'connecting', 'connection', 'connections' -> 'connect'; 'classification', 'classifying' -> 'classif').")
    add_bullet("SentenceSegmenter: Splits document content into discrete sentences using punctuation boundary rules (. ! ? followed by whitespace and capital letters), while protecting decimal numbers and common abbreviations.")

    add_h2("8.4 Inverted Indexing & Vector Norm Precomputation")
    add_p("The InvertedIndex class manages the core retrieval structure in RAM using a Map<String, List<Posting>>. A Posting record encapsulates the document ID, term frequency (TF) within that document, and a list of integer token positions. During indexing, document frequency (DF) is updated automatically.")
    add_p("Crucially, the index precomputes and caches the Euclidean L2 norm of every document vector at ingestion time:")
    add_p("||d||_2 = sqrt( sum_{t in d} ( (1 + ln(tf_{t,d})) * (ln((N+1)/(df_t+1)) + 1) )^2 )")
    add_p("By calculating this norm once during ingestion, the search engine avoids recalculating document vector lengths during query execution, reducing cosine scoring to an O(1) lookup per candidate document.")

    add_h2("8.5 TF-IDF & Sparse Cosine Relevance Scoring")
    add_p("Relevance between a user query q and a candidate document d is calculated using the Vector Space Model:")
    add_bullet("Query Term Weight: w_{t,q} = (1 + ln(tf_{t,q})) * (ln((N+1)/(df_t+1)) + 1)")
    add_bullet("Document Term Weight: w_{t,d} = (1 + ln(tf_{t,d})) * (ln((N+1)/(df_t+1)) + 1)")
    add_bullet("Sparse Cosine Similarity: Cosine(q, d) = ( sum_{t in q cap d} w_{t,q} * w_{t,d} ) / ( ||q||_2 * ||d||_2 )")
    add_p("Because technical queries are concise (|q| <= 5 terms), only documents containing at least one query term are evaluated. Documents sharing zero query terms have a dot product of zero and are bypassed entirely.")

    add_h2("8.6 Bounded Min-Heap Top-K Ranking")
    add_p("Rather than collecting all candidate scores into an array and executing a full sort (O(M log M)), the RankingEngine maintains a Java PriorityQueue<SearchResult> of bounded capacity K, ordered ascending by score. For each candidate document:")
    add_bullet("If heap size < K, insert candidate.")
    add_bullet("If heap size == K and candidate score > root score, remove root (the current lowest score among Top-K) and insert candidate.")
    add_bullet("After evaluating all M candidates, extract K items in reverse order to produce a descending ranked list in O(M log K) time.")

    add_h2("8.7 Graph-Based TextRank Extractive Summarization")
    add_p("TextRank models a document as an undirected graph G = (V, E), where each sentence s_i is a vertex. Edge weights W_{ij} between sentences s_i and s_j are computed via normalized token overlap:")
    add_p("W_{ij} = |tokens(s_i) cap tokens(s_j)| / ( ln(|tokens(s_i)|) + ln(|tokens(s_j)|) )")
    add_p("Sentence saliency scores are computed using PageRank power iterations with damping factor d = 0.85:")
    add_p("WS(V_i) = (1 - d) + d * sum_{V_j in In(V_i)} ( (W_{ji} / sum_{V_k in Out(V_j)} W_{jk}) * WS(V_j) )")
    add_p("Iterations continue until convergence (max delta < 0.0001) or 30 iterations. The top S sentences with the highest scores are extracted and arranged in their original document sequence.")

    add_h2("8.8 Autonomous Research Orchestration & Provenance Citations")
    add_p("When the user invokes 'research \"<question>\"', the ResearchOrchestrator executes a deterministic research plan:")
    add_bullet("1. Question Decomposition: ResearchPlanner extracts key technical concepts and generates 3-4 targeted sub-queries exploring specific facets (e.g., threat detection, evasion attacks, zero trust).")
    add_bullet("2. Evidence Gathering: Executes ranked searches for each sub-query, gathering candidate evidence passages.")
    add_bullet("3. Jaccard Deduplication: Compares candidate sentences using token Jaccard similarity J(A, B) = |A cap B| / |A cup B|. Passages with overlap > 0.60 are pruned to prevent redundancy.")
    add_bullet("4. Provenance Mapping: ProvenanceManager binds each finding to an immutable citation key ([C1], [C2]), recording the source document ID, title, and journal/conference provenance.")
    add_bullet("5. Salient Keywords: KeywordExtractor isolates the top defining terms across retrieved evidence documents.")

    add_h2("8.9 Resilient Dual-Tier Persistence Layer")
    add_p("The Repository interface defines standard persistence operations. MongoRepository connects to MongoDB Atlas using MongoClientSettings with a strict 2,500 ms connection timeout. If MongoDB credentials are not provided or connection fails, RepositoryManager automatically switches to FileRepository, which stores documents, queries, and research sessions as structured JSON files under data/storage/.")

    add_h2("8.10 Security Controls & Safe Logging")
    add_bullet("Path Canonicalization: SecurityManager resolves all paths using toAbsolutePath().normalize() and verifies that normalized paths start with the expected base directory. Relative path escapes ('../../') and null bytes are rejected.")
    add_bullet("Regex Credential Masking: Patterns detect mongodb:// or key=value secrets and replace sensitive tokens with '***'.")
    add_bullet("Input Constraints: Queries longer than 500 characters, result requests K > 100, or files > 10 MB throw IllegalArgumentException.")

    add_h2("8.11 Java Language Features Used")
    add_bullet("Java Records (Java 16+): Used for Document, Posting, SearchResult, EvidenceSnippet, ResearchReport, SystemStats, and EvaluationMetrics. Provides immutable, transparent data carriers with auto-generated constructors, equals(), and hashCode().")
    add_bullet("Java Streams & Lambdas: Used for concise collection transformations, filtering, and mathematical summations.")
    add_bullet("Java Concurrency: Thread-safe singleton instantiation, synchronized local storage writes, and thread-pooled HTTP request handling.")
    add_bullet("Java NIO.2 (java.nio.file): Robust file system access, path normalization, and atomic file reading.")
    add_bullet("Standard HTTP Server: com.sun.net.httpserver.HttpServer provides a zero-dependency web server built directly into the standard JDK.")

    doc.add_page_break()

    # 8.12 Time and Space Complexity Analysis
    add_h2("8.12 Time and Space Complexity Analysis")

    add_p("To evaluate system efficiency rigorously, formal asymptotic bounds are derived across all algorithmic components. The following formal variables are defined:")

    variables_desc = [
        ("N", "Total number of documents in the corpus (N = |D|)"),
        ("L", "Average token count per document (L = (1/N) * sum |d|)"),
        ("T", "Total token occurrences across the corpus (T = N * L)"),
        ("V", "Vocabulary size of unique stemmed terms (V = |V| <= T)"),
        ("Q", "Number of unique stemmed terms in the query (|Q| << V)"),
        ("M", "Number of candidate documents matching at least one query term (0 <= M <= N)"),
        ("K", "Requested number of Top-K ranked results (1 <= K <= 100)"),
        ("S", "Number of sentences in a document for TextRank (5 <= S <= 50)"),
        ("L_s", "Average token length of an individual sentence (L_s = L / S)"),
        ("I", "Number of PageRank power iterations until convergence (I <= 30)"),
        ("P", "Total postings traversed across query terms (P = sum_{t in Q} df(t) <= Q * M)"),
        ("R", "Number of decomposed sub-queries in research orchestration (R <= 4)"),
        ("P_{ev}", "Total candidate evidence passages retrieved (P_{ev} <= R * K)")
    ]

    var_table = doc.add_table(rows=len(variables_desc)+1, cols=2)
    var_table.rows[0].cells[0].paragraphs[0].add_run("Variable")
    var_table.rows[0].cells[1].paragraphs[0].add_run("Mathematical Definition & Scope")
    for idx, (v, d) in enumerate(variables_desc):
        row = var_table.rows[idx+1]
        row.cells[0].paragraphs[0].add_run(v)
        row.cells[1].paragraphs[0].add_run(d)
    style_table(var_table, header_bg="2B6CB0", alt_bg="F7FAFC", col_widths=[1.0, 5.5])

    add_p("Granular Complexity Matrix:", bold_prefix="Asymptotic Complexity Matrix: ")

    cm_data = [
        ("Text Preprocessing (Doc)", "O(L)", "O(L)", "O(L)", "O(L) token buffer"),
        ("Porter Stemming (per term)", "O(1)", "O(1)", "O(1)", "O(1) char array"),
        ("Inverted Index Construction", "O(N * L)", "O(N * L)", "O(N * L)", "O(T + V) postings"),
        ("Vector L2 Norm Precomputation", "O(N * L)", "O(N * L)", "O(N * L)", "O(N) float array"),
        ("Sparse Candidate Lookup", "O(Q)", "O(Q)", "O(Q)", "O(M) candidate set"),
        ("Sparse Cosine Scoring", "O(P)", "O(P)", "O(Q * N)", "O(M) score map"),
        ("Bounded Min-Heap Top-K Selection", "O(M log K)", "O(M log K)", "O(N log K)", "O(K) heap array"),
        ("Full Ranked Search Pipeline", "O(P + M log K)", "O(P + M log K)", "O(Q*N + N log K)", "O(M + K)"),
        ("TextRank Graph Construction", "O(S^2 * L_s)", "O(S^2 * L_s)", "O(S^2 * L_s)", "O(S^2) adjacency"),
        ("TextRank PageRank Iteration", "O(S)", "O(I * S^2)", "O(I * S^2)", "O(S) score vector"),
        ("Question Decomposition", "O(Q)", "O(Q)", "O(Q)", "O(R) query list"),
        ("Passage Jaccard Deduplication", "O(P_{ev})", "O(P_{ev}^2 * L_s)", "O(P_{ev}^2 * L_s)", "O(P_{ev} * L_s)"),
        ("Autonomous Research Pipeline", "O(R*(P+M log K))", "O(R*(P+M log K)+P_{ev}^2 L_s)", "O(R*(QN+N log K))", "O(R * K)"),
        ("Local File Storage Ingestion", "O(L)", "O(L)", "O(L)", "O(L) I/O buffer"),
        ("MongoDB Document Write", "O(1) net", "O(log N_{db}) B-Tree", "O(log N_{db})", "O(payload)")
    ]

    cm_table = doc.add_table(rows=len(cm_data)+1, cols=5)
    cm_headers = ["Subsystem / Operation", "Best Case", "Average Case", "Worst Case", "Space Complexity"]
    for j, h in enumerate(cm_headers):
        cm_table.rows[0].cells[j].paragraphs[0].add_run(h)
    for i, rdata in enumerate(cm_data):
        row = cm_table.rows[i+1]
        for j, val in enumerate(rdata):
            row.cells[j].paragraphs[0].add_run(val)
    style_table(cm_table, header_bg="1A365D", alt_bg="F7FAFC", col_widths=[1.8, 1.1, 1.3, 1.2, 1.1])

    add_p("Algorithmic Complexity vs Network Latency Distinction:", bold_prefix="Important Clarification: ")
    add_p("It is essential to distinguish between local algorithmic complexity (CPU and RAM operations) and external network latency. Inverted indexing, TF-IDF scoring, min-heap selection, and TextRank summarization execute purely in local JVM memory with deterministic CPU bounds. Conversely, MongoDB operations involve network transmission and TCP handshakes. While MongoDB B-Tree indexing is asymptotically O(log N_db), network round-trips can introduce 10–50 ms of network latency. The application guards against network lag by enforcing a 2.5s socket timeout and utilizing local file caching.")

    doc.add_page_break()

    # =============================================================
    # 11. SECTION 9: SCREENSHOTS & RESULTS
    # =============================================================
    add_h1("9. SCREENSHOTS & EXPERIMENTAL RESULTS")

    add_p("This section documents the verified outputs and empirical evaluation results produced by running the application. All screenshots represent real terminal and web dashboard executions captured directly from the running Java application.")

    # Figures 7 & 8
    add_h2("9.1 Startup and Available Commands")
    add_p("When launched in interactive shell mode, the application displays an operational banner, validates storage mode, indices sample research literature, and provides the command prompt:")
    add_image("report/figures/fig7_startup.png", "Figure 7: Application Startup, Ingestion & Interactive Command Prompt", width_in=5.8)

    add_p("The 'help' command provides a complete reference guide detailing all twelve supported commands:")
    add_image("report/figures/fig8_help.png", "Figure 8: Interactive Help Menu & Command Reference", width_in=5.8)

    # Figures 9 & 10
    add_h2("9.2 Document Ingestion & Ranked Vector Space Search")
    add_p("The 'ingest' command validates and processes .txt and .json files from a folder, building vocabulary stems and computing document vector norms in 12 ms:")
    add_image("report/figures/fig9_ingestion.png", "Figure 9: Document Ingestion, Tokenization and Index Construction", width_in=5.8)

    add_p("The 'search' command executes sparse vector cosine scoring against candidate postings, ranking documents through a bounded min-heap and providing human-readable scoring explanations:")
    add_image("report/figures/fig10_search.png", "Figure 10: Ranked Vector Space Search with Explainable Relevance Scoring", width_in=6.0)

    doc.add_page_break()

    # Figures 11 & 12
    add_h2("9.3 Autonomous Research & Extractive Summarization")
    add_p("The 'research' command decomposes complex questions into domain facets, collects candidate passages, eliminates redundant sentences via Jaccard deduplication, and formats verified citations ([C1], [C2]):")
    add_image("report/figures/fig11_research.png", "Figure 11: Autonomous Multi-Query Research Brief with Verifiable Citations", width_in=6.0)

    add_p("The 'summarize' command constructs a sentence similarity graph for a document and executes PageRank power iterations to extract key sentences in original sequence:")
    add_image("report/figures/fig12_summarize.png", "Figure 12: Graph-Based TextRank Extractive Summary (DOC-001)", width_in=5.8)

    # Figures 13 & 14
    add_h2("9.4 Keyword Extraction and System Statistics")
    add_p("The 'keywords' command calculates composite salience scores combining TF-IDF weights, title prominence, and position factors:")
    add_image("report/figures/fig13_keywords.png", "Figure 13: Salient Domain Keyword Extraction", width_in=5.8)

    add_p("The 'stats' command reports corpus size, vocabulary count, total tokens, average document length, and active storage persistence mode:")
    add_image("report/figures/fig14_stats.png", "Figure 14: System, Corpus, and Index Statistics", width_in=5.8)

    doc.add_page_break()

    # Figures 15, 16, 17
    add_h2("9.5 Evaluation Benchmark, Health Diagnostics & Web Dashboard")
    add_p("The 'evaluate' command runs the automated benchmarking engine against labeled ground truth (ground_truth.json), comparing the enhanced TF-IDF model against a baseline lexical model:")
    add_image("report/figures/fig15_evaluate.png", "Figure 15: Automated Information Retrieval Evaluation Benchmark Report", width_in=6.0)

    add_p("The 'health' command evaluates JVM memory, corpus readiness, search index integrity, storage accessibility, and configuration:")
    add_image("report/figures/fig16_health.png", "Figure 16: System Health Diagnostics and Operational Status", width_in=5.8)

    add_p("The 'server 8080' command launches the embedded localhost HTTP web dashboard, providing browser-based access and REST API endpoints:")
    add_image("report/figures/fig17_web_dashboard.png", "Figure 17: Embedded Localhost Web UI Dashboard (http://localhost:8080/)", width_in=6.0)

    doc.add_page_break()

    # Empirical Results Table
    add_h2("9.6 Empirical Benchmark Evaluation Results")
    add_p("The information retrieval effectiveness of the Enhanced TF-IDF Vector Space Model was quantitatively benchmarked against a Baseline Lexical Model (raw term frequency overlap without stemming or IDF weighting) across the research collection:")

    eval_data = [
        ("Precision@5", "0.5750", "0.5750", "+0.0000", "+0.0%"),
        ("Recall@5", "0.9438", "0.9271", "+0.0167", "+1.8%"),
        ("F1-Score@5", "0.6962", "0.6900", "+0.0062", "+0.9%"),
        ("Mean Reciprocal Rank (MRR)", "1.0000", "1.0000", "+0.0000", "Perfect Rank-1 Accuracy"),
        ("nDCG@5 (Graded Relevance)", "0.9572", "N/A", "High Saliency", "Superior Graded Ordering")
    ]

    eval_table = doc.add_table(rows=len(eval_data)+1, cols=5)
    eval_headers = ["Evaluation Metric", "Enhanced (TF-IDF)", "Baseline (Lexical)", "Absolute Delta", "Relative Gain"]
    for j, h in enumerate(eval_headers):
        eval_table.rows[0].cells[j].paragraphs[0].add_run(h)
    for i, rdata in enumerate(eval_data):
        row = eval_table.rows[i+1]
        for j, val in enumerate(rdata):
            row.cells[j].paragraphs[0].add_run(val)
    style_table(eval_table, header_bg="1A365D", alt_bg="F7FAFC", col_widths=[2.0, 1.2, 1.2, 1.1, 1.0])

    add_p("Latency and Resource Profile:", bold_prefix="Performance Profile: ")

    perf_data = [
        ("Average Query Latency", "3.13 ms – 6.13 ms", "Sub-10ms search achieved via sparse posting list candidate pruning"),
        ("P95 Query Latency", "7.00 ms – 20.00 ms", "Worst-case candidate queries terminate well within the 50ms requirement"),
        ("Corpus Ingestion & Index Time", "12.00 ms", "High-throughput tokenization, stemming, and vector norm computation"),
        ("Corpus Document Count", "12 Documents", "Diverse cybersecurity, intrusion detection, and AI research papers"),
        ("Vocabulary Size", "654 Unique Stems", "Filtered technical vocabulary excluding 150+ syntactic stop words"),
        ("Estimated Index RAM", "~100 KB", "Memory-efficient posting lists avoiding dense matrix allocations"),
        ("Evidence Passage Coverage", "100.0%", "Autonomous research successfully surfaced evidence for all test queries")
    ]

    perf_table = doc.add_table(rows=len(perf_data)+1, cols=3)
    perf_headers = ["Performance Dimension", "Measured Value", "Technical Analysis & Significance"]
    for j, h in enumerate(perf_headers):
        perf_table.rows[0].cells[j].paragraphs[0].add_run(h)
    for i, rdata in enumerate(perf_data):
        row = perf_table.rows[i+1]
        for j, val in enumerate(rdata):
            row.cells[j].paragraphs[0].add_run(val)
    style_table(perf_table, header_bg="2B6CB0", alt_bg="F7FAFC", col_widths=[1.8, 1.4, 3.3])

    add_p("Discussion of Results:", bold_prefix="Analysis of Benchmark Findings: ")
    add_bullet("Recall Improvement (+1.8%): Porter stemming allowed the Enhanced Engine to retrieve documents containing morphological variations (such as 'detected', 'detection', 'detector') that were completely missed by the exact-match lexical baseline.")
    add_bullet("Perfect Mean Reciprocal Rank (MRR = 1.0): For 100% of benchmark queries, the single most relevant ground-truth document was placed at rank position #1, demonstrating high ranking fidelity.")
    add_bullet("High Graded nDCG@5 (0.9572): The normalized discounted cumulative gain score confirms that highly authoritative documents (graded 3) were consistently ranked ahead of secondary references (graded 2 and 1).")

    doc.add_page_break()

    # =============================================================
    # 12. SECTION 10: TESTING APPROACH
    # =============================================================
    add_h1("10. TESTING APPROACH")

    add_p("A comprehensive automated testing strategy was implemented to verify functional correctness, algorithmic fidelity, boundary condition handling, and security defenses. The test suite is implemented using JUnit Jupiter (JUnit 5) inside ResearchAssistantTestSuite.java and executed via 'mvn clean test'.")

    add_p("The testing suite comprises twenty-one automated tests spanning eight testing categories:")
    add_bullet("Unit Testing: Verifying standalone functions including regex tokenization, stop-word elimination, Porter stemmer transformation rules, and mathematical TF-IDF formulas.")
    add_bullet("Functional Testing: Testing inverted index construction, ranked search candidate retrieval, TextRank graph convergence, question decomposition, and keyword extraction.")
    add_bullet("Integration Testing: Testing end-to-end multi-query research orchestration, passage deduplication, citation mapping, and system health reporting.")
    add_bullet("Boundary Testing: Evaluating edge cases including searching an empty corpus, single-document index lookups, boundary K values (K=0, K=1, K=100), and queries containing only stop words.")
    add_bullet("Input Validation Testing: Testing enforcement of maximum query length (>500 characters throws IllegalArgumentException).")
    add_bullet("Security Testing: Testing path traversal prevention (paths with '../../' throw SecurityException), null byte injection ('\\0' rejection), and regex credential masking.")
    add_bullet("Persistence Testing: Verifying local JSON FileRepository round-trip serialization and retrieval under data/storage/.")
    add_bullet("Performance & Complexity Testing: Capturing empirical runtime telemetry counters (Q, M, P, K, execution latency) and comparing against theoretical asymptotic bounds.")

    add_p("Automated Test Suite Verification Matrix (All 21 Tests PASSED):", bold_prefix="JUnit 5 Test Matrix: ")

    test_matrix = [
        ("TC-01", "Tokenizer", "Hyphenated technical string", "Tokens lowercased, hyphens preserved", "Passed: 'zero-trust', 'ml'", "PASS"),
        ("TC-02", "StopWordFilter", "String with common stop words", "150+ stop words stripped", "Passed: 'the', 'is' removed", "PASS"),
        ("TC-03", "PorterStemmer", "'connecting', 'classification'", "Stems to 'connect', 'classif'", "Passed: correct 5-phase reduction", "PASS"),
        ("TC-04", "TFIDFEngine", "Known TF and DF counts", "Sublinear TF & smoothed IDF match formula", "Passed: exact math parity", "PASS"),
        ("TC-05", "InvertedIndex", "Two sample documents", "Index contains terms, postings, norms", "Passed: correct postings & norms", "PASS"),
        ("TC-06", "RankingEngine", "Query: 'machine learning'", "Top-K results ranked descending", "Passed: correct cosine ordering", "PASS"),
        ("TC-07", "TextRankSummarizer", "Multi-sentence document", "Top 3 sentences extracted in order", "Passed: PageRank converged in 6ms", "PASS"),
        ("TC-08", "ResearchPlanner", "Question on cybersecurity ML", "Decomposes into 3-4 sub-queries", "Passed: sub-queries generated", "PASS"),
        ("TC-09", "ProvenanceManager", "Evidence snippet list", "Assigns [C1], [C2] mapped to docs", "Passed: immutable citation keys", "PASS"),
        ("TC-10", "FileRepository", "Save Document object", "Writes and reads JSON to disk", "Passed: round-trip fidelity", "PASS"),
        ("TC-11", "SecurityManager", "Path with '../../etc/passwd'", "Throws SecurityException", "Passed: traversal blocked", "PASS"),
        ("TC-12", "EvaluationEngine", "ground_truth.json benchmark", "Computes P@5, R@5, MRR, nDCG@5", "Passed: metrics computed", "PASS"),
        ("TC-13", "Boundary Test", "Search against empty index", "Returns empty list without exception", "Passed: clean empty list", "PASS"),
        ("TC-14", "Boundary Test", "Index with exactly 1 document", "Retrieval and norm calculated", "Passed: single doc retrieved", "PASS"),
        ("TC-15", "Boundary Test", "K=0, K=1, K=100 limits", "Returns correctly sized result lists", "Passed: bounds respected", "PASS"),
        ("TC-16", "Boundary Test", "Query: 'the is at which'", "Returns empty list (all stop words)", "Passed: zero false matches", "PASS"),
        ("TC-17", "ComplexityProfile", "Sample search query", "Captures Q, M, P, K, and latency", "Passed: counters populated", "PASS"),
        ("TC-18", "KeywordExtractor", "Document DOC-001", "Returns top salient domain terms", "Passed: 'secur', 'learn' top", "PASS"),
        ("TC-19", "HealthChecker", "Execute diagnostics", "Reports PASS across runtime & corpus", "Passed: status HEALTHY", "PASS"),
        ("TC-20", "Input Validation", "Query string > 500 characters", "Throws IllegalArgumentException", "Passed: boundary enforced", "PASS"),
        ("TC-21", "SecurityManager", "Path containing null byte '\\0'", "Throws SecurityException", "Passed: injection blocked", "PASS")
    ]

    tm_table = doc.add_table(rows=len(test_matrix)+1, cols=6)
    tm_headers = ["Test ID", "Tested Module", "Test Input / Scenario", "Expected Result", "Actual Result", "Status"]
    for j, h in enumerate(tm_headers):
        tm_table.rows[0].cells[j].paragraphs[0].add_run(h)
    for i, rdata in enumerate(test_matrix):
        row = tm_table.rows[i+1]
        for j, val in enumerate(rdata):
            row.cells[j].paragraphs[0].add_run(val)
    style_table(tm_table, header_bg="1A365D", alt_bg="F7FAFC", col_widths=[0.6, 1.2, 1.5, 1.4, 1.3, 0.5])

    doc.add_page_break()

    # =============================================================
    # 13. SECTION 11: CHALLENGES FACED
    # =============================================================
    add_h1("11. CHALLENGES FACED & SOLUTIONS")

    add_p("During the conception, design, and implementation of the Autonomous AI Research Assistant, several significant technical and architectural hurdles were encountered. The following reflections describe the key challenges and how they were resolved:")

    add_h2("11.1 Maintaining Code Cleanliness under the Two-Source-File Constraint")
    add_p("One of the most demanding constraints of the project was implementing an entire Information Retrieval engine, NLP pipeline, summarization algorithm, dual-persistence layer, and HTTP web server within exactly one backend file and one frontend file. Initially, placing all code in two files risked creating unmaintainable, tightly coupled 'spaghetti code'.")
    add_p("Solution: We adopted an encapsulated nested modular design. Every major subsystem (Tokenizer, StopWordFilter, Stemmer, InvertedIndex, RankingEngine, TextRankSummarizer, ResearchOrchestrator, SecurityManager) was implemented as an independent, static nested class with private constructors, clear public APIs, and zero circular dependencies. Domain entities were modeled as immutable Java records. This preserved clean object-oriented architecture while strictly satisfying the two-file boundary.")

    add_h2("11.2 Implementing the Porter Stemmer from Scratch in Pure Java")
    add_p("To avoid external library dependencies, we had to implement the complete 5-phase Porter Stemming algorithm manually. Handling English morphological quirks—such as vowel-consonant measure conditions (m > 0), double consonant endings, and vowel presence in stem roots—proved error-prone during early iterations, leading to over-stemming or under-stemming.")
    add_p("Solution: We decomposed the algorithm into modular helper methods: isConsonant(), measure(), hasVowel(), and endsWithDoubleConsonant(). We wrote targeted unit tests against standard Porter test vectors (e.g., verifying that 'connecting' stems to 'connect' and 'generalizations' to 'general').")

    add_h2("11.3 Preventing Document Length Bias in TF-IDF Vector Space Scoring")
    add_p("In early testing, long documents containing thousands of words dominated search results simply because they contained higher raw word frequencies, even when shorter documents were much more focused and relevant.")
    add_p("Solution: We implemented sublinear term frequency weighting (1 + ln(tf)) and precomputed the Euclidean L2 norm (||d||_2) for every document vector. Dividing the inner dot-product by (||q||_2 * ||d||_2) normalizes vectors to unit length on the hypersphere, eliminating document length bias.")

    add_h2("11.4 Graph Convergence and Damping in TextRank Summarization")
    add_p("When constructing sentence similarity graphs for technical papers, disconnected sentences or sentences with zero mutual vocabulary caused zero-division errors and oscillating PageRank scores that failed to converge.")
    add_p("Solution: We normalized token overlap by the sum of logarithmic sentence lengths (ln(|tokens(s_1)|) + ln(|tokens(s_2)|)). We applied a damping factor of d = 0.85 and enforced a convergence threshold of epsilon = 0.0001, ensuring stable numerical convergence within 15–25 power iterations (typically 6 ms).")

    add_h2("11.5 Resilient Dual-Tier Persistence and Failover Handling")
    add_p("Connecting to remote MongoDB Atlas instances over campus Wi-Fi occasionally caused long socket connection hangs (up to 30 seconds) when internet access was throttled, freezing the CLI.")
    add_p("Solution: We configured MongoClientSettings with an aggressive serverSelectionTimeout of 2,500 milliseconds. If the driver fails to establish a socket within 2.5 seconds, RepositoryManager catches the exception, logs a sanitized warning, and immediately activates FileRepository, allowing the application to function completely offline without hanging.")

    add_h2("11.6 Hardening Against Path Traversal and Null-Byte Injection")
    add_p("Allowing users to specify custom ingestion directories introduced vulnerabilities where malicious strings (e.g., '../../etc' or path strings with null bytes) could navigate outside the workspace.")
    add_p("Solution: SecurityManager resolves paths using toAbsolutePath().normalize(), verifies that the canonical path starts with the intended base directory, and explicitly checks for null characters ('\\0'), throwing a SecurityException if any boundary is violated.")

    doc.add_page_break()

    # =============================================================
    # 14. SECTION 12: CONCLUSION
    # =============================================================
    add_h1("12. CONCLUSION & SUMMARY OF LEARNING")

    add_p("The Autonomous AI Research Assistant successfully demonstrates that a complete, robust, and highly performant Information Retrieval and NLP system can be constructed in pure Java without depending on external cloud AI APIs, heavy enterprise frameworks, or third-party NLP libraries.")

    add_p("Summary of Key Achievements:", bold_prefix="Project Achievements: ")
    add_bullet("Fully Local Execution: Tokenization, Porter stemming, inverted indexing, TF-IDF ranking, TextRank extractive summarization, and autonomous research orchestration execute 100% locally on the JVM with zero external API calls.")
    add_bullet("Verifiable Provenance: Extracted findings are anchored to exact source documents via sequential citation keys ([C1], [C2]), guaranteeing zero synthetic hallucination.")
    add_bullet("Sub-5ms Query Latency: Sparse inverted index pruning combined with bounded min-heap Top-K selection achieves average query latencies of 3.13 ms to 6.13 ms.")
    add_bullet("High Retrieval Accuracy: Benchmark evaluation against labeled ground truth demonstrates an MRR of 1.0000, Recall@5 of 0.9438 (+1.8% gain over baseline), and graded nDCG@5 of 0.9572.")
    add_bullet("Resilient Architecture: The official MongoDB synchronous driver provides database persistence with an automatic 2.5s socket timeout failover to local JSON file storage.")
    add_bullet("Comprehensive Verification: 21 automated JUnit 5 tests pass with zero failures, covering unit logic, integration workflows, boundary edge cases, and security defenses.")

    add_p("Summary of Learning Outcomes:", bold_prefix="Student Learning Outcomes: ")
    add_bullet("Practical Information Retrieval: Mastered the mathematical foundations and implementation mechanics of inverted posting lists, sublinear TF-IDF, vector space models, and sparse cosine distance.")
    add_bullet("Natural Language Processing: Gained deep insight into morphological rule-based stemming (Porter's algorithm), stop-word filtering, and sentence segmentation.")
    add_bullet("Graph Algorithms & PageRank: Understood how to model unstructured text as weighted graphs and apply eigenvector centrality algorithms for extractive summarization.")
    add_bullet("Asymptotic Complexity & Telemetry: Learned to derive formal mathematical bounds using formal variables (N, L, T, V, Q, M, K, S, I, P, R) and measure empirical runtime counters side-by-side.")
    add_bullet("Modern Java Mastery: Deepened proficiency in Java 17/21 language features including immutable records, functional stream pipelines, thread-safe concurrency, NIO.2 file channels, and embedded HTTP servers.")
    add_bullet("Defensive Software Engineering: Built secure, production-quality code by enforcing strict path canonicalization, resource safety limits, and automated regex credential masking.")

    # =============================================================
    # 15. SECTION 13: REFERENCES
    # =============================================================
    add_h1("13. REFERENCES")

    refs = [
        ("1. Salton, G., & Buckley, C. (1988).", "Term-weighting approaches in automatic text retrieval. Information Processing & Management, 24(5), 513-523."),
        ("2. Porter, M. F. (1980).", "An algorithm for suffix stripping. Program: electronic library and information systems, 14(3), 130-137."),
        ("3. Mihalcea, R., & Tarau, P. (2004).", "TextRank: Bringing order into text. In Proceedings of the 2004 Conference on Empirical Methods in Natural Language Processing (EMNLP), 404-411."),
        ("4. Manning, C. D., Raghavan, P., & Schütze, H. (2008).", "Introduction to Information Retrieval. Cambridge University Press."),
        ("5. Page, L., Brin, S., Motwani, R., & Winograd, T. (1999).", "The PageRank citation ranking: Bringing order to the web. Technical Report, Stanford InfoLab."),
        ("6. Oracle Corporation (2023).", "Java Platform, Standard Edition & Java Development Kit Version 17 API Specification. Oracle Java Documentation."),
        ("7. MongoDB, Inc. (2024).", "MongoDB Java Synchronous Driver Documentation (v4.11). https://www.mongodb.com/docs/drivers/java/sync/current/"),
        ("8. Järvelin, K., & Kekäläinen, J. (2002).", "Cumulated gain-based evaluation of IR techniques. ACM Transactions on Information Systems (TOIS), 20(4), 422-446."),
        ("9. Apache Software Foundation (2024).", "Apache Maven Project & Plugin Documentation. https://maven.apache.org/"),
        ("10. JUnit Team (2024).", "JUnit 5 User Guide (JUnit Jupiter v5.10). https://junit.org/junit5/docs/current/user-guide/")
    ]

    for cit, details in refs:
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.left_indent = Inches(0.4)
        p.paragraph_format.first_line_indent = Inches(-0.4)
        crun = p.add_run(cit + " ")
        crun.font.name = "Calibri"
        crun.font.size = Pt(9.5)
        crun.font.bold = True
        crun.font.color.rgb = NAVY
        drun = p.add_run(details)
        drun.font.name = "Calibri"
        drun.font.size = Pt(9.5)
        drun.font.color.rgb = DARK

    # -------------------------------------------------------------
    # Save the Final Document
    # -------------------------------------------------------------
    output_filename = "Autonomous_AI_Research_Assistant_Project_Report.docx"
    output_path_report = os.path.join("report", output_filename)
    output_path_root = output_filename

    doc.save(output_path_report)
    doc.save(output_path_root)
    print(f"[OK] Successfully created and saved final Word document: {output_path_report}")
    print(f"[OK] Copied final Word document to project root: {output_path_root}")

if __name__ == "__main__":
    create_project_report()
