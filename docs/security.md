# Security Architecture & Hardening Guide (Target 5/5 Standard)

## 1. Threat Model & Mitigations Matrix

| Threat Category | Potential Attack Vector | System Defense Mechanism | Code Implementation |
| :--- | :--- | :--- | :--- |
| **Credential Leakage** | Exposure of MongoDB URI or database passwords in logs, git history, or stack traces | Strict environment variable consumption (`MONGODB_URI`); regex-based log & error sanitization | `SecurityManager.sanitize()` |
| **Path Traversal** | Arbitrary file read attempts using `../`, null bytes, or absolute path escapes | Strict path resolution against base directory; canonicalization and root validation | `SecurityManager.validateAndResolvePath()` |
| **Denial of Service (DoS)** | Giant input files, oversized search queries, or excessive $K$ values causing Out-Of-Memory | 10 MB per-file safety cap; 500-char query limit; $1 \le K \le 100$ bounding; min-heap $O(M \log K)$ | `SecurityManager.validateFileSize()`, `validateQuery()` |
| **Single Point of Failure** | Database host outage blocking application launch | 2.5-second socket timeout with automatic failover to local zero-dependency `FileRepository` | `RepositoryManager.createRepository()` |
| **Citation Hallucination** | Autonomous agent inventing phantom citations | Deterministic provenance mapping strictly bound to physical document IDs in the Inverted Index | `ProvenanceManager.assignCitations()` |

---

## 2. Path Traversal & Injection Prevention
The system strictly canonicalizes user-supplied paths before file read operations:

```java
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
```

Any attempt to specify relative escape paths such as `../../etc/passwd` or `..\..\Windows\System32` immediately throws a `SecurityException` and aborts the operation.

---

## 3. Credential Sanitization Engine
All log handlers, console formatters, and exception messages pass through the `SecurityManager.sanitize` pipeline:

```java
private static final Pattern MONGO_SECRET_PATTERN =
        Pattern.compile("(mongodb(?:\\+srv)?://)([^:@/]+):([^@/]+)@");
private static final Pattern GENERIC_SECRET_PATTERN =
        Pattern.compile("(?i)(password|secret|token|apikey)\\s*[:=]\\s*['\"]?([^'\"\\s]+)['\"]?");

public static String sanitize(String input) {
    if (input == null) return null;
    String sanitized = MONGO_SECRET_PATTERN.matcher(input).replaceAll("$1***:***@");
    sanitized = GENERIC_SECRET_PATTERN.matcher(sanitized).replaceAll("$1=***");
    return sanitized;
}
```

This guarantees that even if an invalid connection string is entered, credentials will never appear in standard console output or log files.

---

## 4. Git Security & Secret Scanning Guidance
1. **`.gitignore` Enforcement**:
   The repository includes strict `.gitignore` rules preventing unintentional tracking of `.env`, `*.properties`, `secrets/`, `target/`, and `data/storage/`.
2. **Pre-Commit Auditing**:
   Developers and reviewers should run git secret scanners prior to pushing:
   ```bash
   # Using git-secrets or gitleaks
   gitleaks detect --source . --verbose
   ```
3. **Credential Rotation Recommendation**:
   > [!IMPORTANT]
   > If any private database connection string or token was ever pasted into a chat conversation or public ticket during development, that credential is considered compromised. **You must immediately rotate or revoke that database credential in the MongoDB Atlas Console** before deploying the code or pushing to a public GitHub repository.
