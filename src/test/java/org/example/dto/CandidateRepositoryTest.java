package org.example.dto;

import org.example.entities.Candidate;
import org.example.error.ErrorHandling;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests for {@link CandidateRepository}.
 * Works on a temporary directory to verify real file persistence.
 */
class CandidateRepositoryTest {

    private Path tempDir;
    private Path dataFile;
    private CandidateRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("cand_repo_test_");
        dataFile = tempDir.resolve("candidates.tsv");
        repo = new CandidateRepository(dataFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a)) // delete children first
                    .forEach(p -> p.toFile().delete());
        }
    }

    // ------------------------------------------------------------
    // ADD tests
    // ------------------------------------------------------------

    @Test
    void testAddValidCandidatePersistsToFile() throws IOException {
        Candidate c = new Candidate("David", 35, "IT", 10);
        repo.add(c);

        List<String> lines = Files.readAllLines(dataFile);
        assertEquals(2, lines.size()); // header + one row
        assertTrue(lines.get(1).contains("David"));
    }

    @Test
    void testAddAutoAssignsId() {
        Candidate c = new Candidate("Anna", 28, "Finance", 5);
        repo.add(c);
        assertTrue(c.getId() > 0);
    }

    @Test
    void testAddInvalidAgeThrows() {
        Candidate c = new Candidate("Bob", 15, "Tech", 2);
        assertThrows(ErrorHandling.class, () -> repo.add(c));
    }

    // ------------------------------------------------------------
    // UPDATE tests
    // ------------------------------------------------------------

    @Test
    void testUpdateExistingCandidateRewritesFile() throws IOException {
        Candidate c = new Candidate("Eva", 30, "HR", 6);
        repo.add(c);
        long id = c.getId();

        c.setIndustry("Recruitment");
        Candidate updated = repo.update(c);

        assertEquals("Recruitment", updated.getIndustry());

        List<String> lines = Files.readAllLines(dataFile);
        assertTrue(lines.get(1).contains("Recruitment"));
    }

    @Test
    void testUpdateMissingCandidateThrowsNotFound() {
        Candidate c = new Candidate("Tom", 40, "IT", 15);
        c.setId(999);
        assertThrows(ErrorHandling.class, () -> repo.update(c));
    }

    @Test
    void testUpdateInvalidIdThrowsBadRequest() {
        Candidate c = new Candidate("Tom", 40, "IT", 15);
        assertThrows(ErrorHandling.class, () -> repo.update(c));
    }

    // ------------------------------------------------------------
    // DELETE tests
    // ------------------------------------------------------------

    @Test
    void testDeleteValidCandidateRemovesFromFile() throws IOException {
        Candidate c = new Candidate("Sara", 32, "IT", 8);
        repo.add(c);
        long id = c.getId();
        repo.delete(id);

        List<String> lines = Files.readAllLines(dataFile);
        assertEquals(1, lines.size()); // only header remains
    }

    @Test
    void testDeleteInvalidIdThrows() {
        assertThrows(ErrorHandling.class, () -> repo.delete(0));
    }

    @Test
    void testDeleteNonExistingThrowsNotFound() {
        assertThrows(ErrorHandling.class, () -> repo.delete(999));
    }

    // ------------------------------------------------------------
    // LOAD & PERSISTENCE tests
    // ------------------------------------------------------------

    @Test
    void testRepositoryLoadsExistingDataOnStartup() throws IOException {
        // write fake data file manually
        Files.writeString(dataFile,
                "id\tname\tage\tindustry\tyears\tdate_of_register\n" +
                        "1\tJohn\t30\tIT\t5\t2025-10-31 12:00:00\n",
                StandardOpenOption.TRUNCATE_EXISTING);

        CandidateRepository r2 = new CandidateRepository(dataFile.toString());
        List<Candidate> all = r2.findAll();

        assertEquals(1, all.size());
        assertEquals("John", all.get(0).getName());
    }

    // ------------------------------------------------------------
    // VALIDATION edge cases
    // ------------------------------------------------------------

    @Test
    void testIndustryAsNumberThrowsBadRequest() {
        Candidate c = new Candidate("Mark", 25, "1234", 2);
        assertThrows(ErrorHandling.class, () -> repo.add(c));
    }

    @Test
    void testYearsGreaterThanAgeThrowsBadRequest() {
        Candidate c = new Candidate("Ali", 20, "IT", 30);
        assertThrows(ErrorHandling.class, () -> repo.add(c));
    }
}
