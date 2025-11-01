package org.example.dto;

import org.example.error.ErrorHandling;
import org.example.entities.Candidate;
import org.example.repo.CandidateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * File-based repository implementation for {@link Candidate}.
 * <p>
 * Stores candidate data in a simple TSV (Tab-Separated Values) file.
 * Each operation rewrites the entire file to maintain data consistency.
 * No database (JDBC) or UI components are used.
 */
public class CandidateRepository implements CandidateStore {
    private static final Logger log = LoggerFactory.getLogger(CandidateRepository.class);

    private static final String HEADER = "id\tname\tage\tindustry\tyears\tdate_of_register";
    private static final String TAB = "\t";
    private static final String NEWLINE = "\n";

    /* File where candidate data is stored */
    private final Path file;
    /*In-memory list representing the data*/
    private final List<Candidate> store;
    /*Auto-increment ID generator*/
    private long nextId;

    /**
     * Initializes the repository and loads existing data from file.
     * If the file doesn't exist, it's created automatically.
     *
     * @param filePath path to the TSV data file
     */
    public CandidateRepository(String filePath) {
        this.file = Paths.get(filePath);
        this.store = new ArrayList<>();
        try {
            initStorage();          // Ensure directory and file exist
            loadAllFromDisk();      // Load all existing records into memory
            this.nextId = computeNextId();  // Calculate the next ID
            log.info("Repository initialized. Candidates in store = {}, nextId = {}", store.size(), nextId);
        } catch (IOException e) {
            throw new ErrorHandling("Failed to initialize file repository: " + filePath, e, 500, "REPO_INIT", null);
        }
    }

    /**
     * Returns an immutable copy of all stored candidates.
     */
    @Override
    public List<Candidate> findAll() {
        return List.copyOf(store);
    }

    /**
     * Adds a new candidate to the repository.
     * - Auto-assigns an ID if not provided.
     * - Validates input data.
     * - Persists all data to file.
     */
    @Override
    public void add(Candidate c) {
        Objects.requireNonNull(c, "candidate");
        if (c.getId() <= 0) {
            c.setId(nextId++);
        }
        validate(c);
        store.add(copy(c));   // Store a defensive copy to avoid external modification
        persist();            // Save data to file
    }

    /**
     * Updates an existing candidate by ID.
     * - Requires a valid (>0) ID.
     * - Validates input data.
     * - Replaces the existing record in memory and persists it.
     *
     * @return the updated candidate
     * @throws ErrorHandling if ID not found or validation fails
     */
    @Override
    public Candidate update(Candidate c) {
        Objects.requireNonNull(c, "candidate");
        if (c.getId() <= 0)
            throw ErrorHandling.badRequest("update(): id must be > 0");

        validate(c);

        for (int i = 0; i < store.size(); i++) {
            if (Objects.equals(store.get(i).getId(), c.getId())) {
                Candidate copy = copy(c);
                store.set(i, copy);
                persist();
                return copy;
            }
        }
        throw ErrorHandling.notFound("update(): id=" + c.getId() + " not found");
    }

    /**
     * Deletes a candidate by ID.
     * @param id unique identifier of the candidate
     * @throws ErrorHandling if ID is invalid or candidate not found
     */
    @Override
    public void delete(long id) {
        if (id <= 0)
            throw ErrorHandling.badRequest("delete(): id must be > 0");

        boolean removed = store.removeIf(x -> Objects.equals(x.getId(), id));
        if (removed) {
            persist();
        } else {
            throw ErrorHandling.notFound("delete(): id=" + id + " not found");
        }
    }

    // ------------------------------------------------------------
    // File Persistence
    // ------------------------------------------------------------

    /**
     * Creates the storage file and its parent directory if they don't exist.
     * Writes a header row for readability.
     */
    private void initStorage() throws IOException {
        Path parent = file.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(file)) {
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                w.write(HEADER);
                w.write(NEWLINE);
            }
        }
    }

    /**
     * Loads all candidates from the TSV file into memory.
     * Skips malformed or invalid rows but logs warnings.
     */
    private void loadAllFromDisk() throws IOException {

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        store.clear();
        boolean first = true;
        for (String line : lines) {
            if (first) { first = false; continue; }  // Skip header line
            if (line.isBlank()) continue;

            String[] parts = line.split("\t", -1);
            if (parts.length != 6) {
                log.warn("Skipping malformed line: {}", line);
                continue;
            }
            try {
                Candidate c = new Candidate();
                c.setId(Long.parseLong(parts[0]));
                c.setName(unescape(parts[1]));
                c.setAge(Integer.parseInt(parts[2]));
                c.setIndustry(unescape(parts[3]));
                c.setYearsOfExperience(Integer.parseInt(parts[4]));
                c.setDateTimeOfRegister(String.valueOf(parts[5]));
                store.add(c);
            } catch (RuntimeException ex) {
                log.warn("Skipping invalid row: {}", line, ex);
            }
        }
    }

    /**
     * Calculates the next sequential ID based on the current highest ID.
     */
    private long computeNextId() {
        return store.stream()
                .map(Candidate::getId)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L) + 1L;
    }

    /**
     * Persists all in-memory candidates to disk using an atomic replacement:
     * - Writes to a temporary file first
     * - Then replaces the main file to prevent corruption
     */
    private void persist() {
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {

            w.write(HEADER);
            w.write(NEWLINE);
            for (Candidate c : store) {
                w.write(String.valueOf(c.getId())); w.write(TAB);
                w.write(escape(c.getName()));       w.write(TAB);
                w.write(String.valueOf(c.getAge())); w.write(TAB);
                w.write(escape(c.getIndustry()));   w.write(TAB);
                w.write(String.valueOf(c.getYearsOfExperience())); w.write(TAB);
                w.write(String.valueOf(c.getDateTimeOfRegister()));
                w.write(NEWLINE);
            }
        } catch (IOException e) {
            throw new ErrorHandling("Failed to write repository file (tmp): " + tmp,
                    e, 500, "REPO_WRITE", null);
        }

        // Atomically replace the target file (prevents partial data loss)
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ErrorHandling("Failed to replace repository file: " + file,
                    e, 500, "REPO_REPLACE", null);
        }
    }

    // ------------------------------------------------------------
    // Validation & Utility Methods
    // ------------------------------------------------------------

    /**
     * Validates logical domain rules for a candidate.
     * Ensures valid name, industry, age, and experience years.
     */
    private static void validate(Candidate c) {
        String name = safe(c.getName());
        String industry = safe(c.getIndustry());
        int age = c.getAge();
        int years = c.getYearsOfExperience();

        if (name.isEmpty())
            throw ErrorHandling.badRequest("Name is required");
        if (industry.length() < 2)
            throw ErrorHandling.badRequest("Industry must be at least 2 characters");
        if (industry.matches("\\d+"))
            throw ErrorHandling.badRequest("Industry must be text, not a number");
        if (age < 16)
            throw ErrorHandling.badRequest("Minimum age is 16");
        if (years < 0 || years > age)
            throw ErrorHandling.badRequest("Invalid years of experience");
    }

    /** Returns an empty string if null, otherwise trims spaces. */
    private static String safe(String s) { return s == null ? "" : s.trim(); }

    /** Returns a deep copy of a Candidate to prevent external modification. */
    private static Candidate copy(Candidate c) {
        Candidate x = new Candidate();
        x.setId(c.getId());
        x.setName(c.getName());
        x.setAge(c.getAge());
        x.setIndustry(c.getIndustry());
        x.setYearsOfExperience(c.getYearsOfExperience());
        return x;
    }

    /** Escapes special characters (tabs, newlines, slashes) for safe TSV writing. */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /** Reverses escaping to restore original text from TSV format. */
    private static String unescape(String s) {
        if (s == null) return "";
        String r = s.replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\t", "\t");
        return r.replace("\\\\", "\\");
    }
}
