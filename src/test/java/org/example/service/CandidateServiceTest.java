package org.example.service;

import org.example.entities.Candidate;
import org.example.error.ErrorHandling;
import org.example.repo.CandidateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.QuerySpec;

import java.util.List;
import java.util.function.Predicate;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CandidateService}.
 * Uses Mockito to mock the persistence layer (CandidateStore).
 */
class CandidateServiceTest {

    private CandidateStore store;
    private CandidateService service;

    @BeforeEach
    void setUp() {
        store = mock(CandidateStore.class);
        service = new CandidateService(store);
    }

    // ---------------------------
    // Add / Validation tests
    // ---------------------------

    @Test
    void testAddValidCandidateCallsRepository() {
        Candidate c = new Candidate("David", 35, "IT", 10);
        service.add(c);

        verify(store, times(1)).add(c);
    }

    @Test
    void testAddInvalidAgeThrowsError() {
        Candidate c = new Candidate("Anna", 15, "Finance", 2);
        var ex = assertThrows(ErrorHandling.class, () -> service.add(c));
        assertTrue(ex.getMessage().contains("Minimum age"));
    }

    @Test
    void testAddInvalidYearsThrowsError() {
        Candidate c = new Candidate("Mark", 30, "IT", 50);
        var ex = assertThrows(ErrorHandling.class, () -> service.add(c));
        assertTrue(ex.getMessage().contains("Invalid years"));
    }

    @Test
    void testAddIndustryIsNumberThrowsError() {
        Candidate c = new Candidate("Lisa", 25, "1234", 3);
        var ex = assertThrows(ErrorHandling.class, () -> service.add(c));
        assertTrue(ex.getMessage().contains("Industry must be text"));
    }

    // ---------------------------
    // Update tests
    // ---------------------------

    @Test
    void testUpdateExistingCandidateReturnsUpdated() {
        Candidate c = new Candidate("John", 40, "Tech", 15);
        c.setId(1);
        when(store.update(c)).thenReturn(c);

        Candidate result = service.update(c);
        assertEquals("John", result.getName());
        verify(store).update(c);
    }

    @Test
    void testUpdateMissingCandidateThrowsNotFound() {
        Candidate c = new Candidate("John", 40, "Tech", 15);
        c.setId(999);
        when(store.update(c)).thenReturn(null);

        var ex = assertThrows(ErrorHandling.class, () -> service.update(c));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void testUpdateWithoutIdThrowsError() {
        Candidate c = new Candidate("John", 40, "Tech", 15);
        var ex = assertThrows(ErrorHandling.class, () -> service.update(c));
        assertTrue(ex.getMessage().contains("id required"));
    }

    // ---------------------------
    // Delete tests
    // ---------------------------

    @Test
    void testDeleteValidId() {
        service.delete(5);
        verify(store).delete(5);
    }

    @Test
    void testDeleteInvalidIdThrows() {
        var ex = assertThrows(ErrorHandling.class, () -> service.delete(0));
        assertTrue(ex.getMessage().contains("id must be > 0"));
    }

    // ---------------------------
    // Query tests
    // ---------------------------

    @Test
    void testQueryWithPredicateAndSort() {
        Candidate a = new Candidate("Alice", 30, "IT", 5);
        Candidate b = new Candidate("Bob", 25, "HR", 2);
        when(store.findAll()).thenReturn(List.of(a, b));

        Predicate<Candidate> filter = c -> c.getAge() >= 26;
        Comparator<Candidate> sort = Comparator.comparing(Candidate::getName);

        List<Candidate> result = service.query(filter, sort);

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    void testQueryHandlesNullSpec() {
        when(store.findAll()).thenReturn(List.of(
                new Candidate("Eva", 33, "Tech", 8)
        ));

        List<Candidate> result = service.query((QuerySpec) null);
        assertEquals(1, result.size());
    }

    @Test
    void testQueryBuildPredicateNumericErrorReturnsEmpty() {
        QuerySpec spec = new QuerySpec();
        spec.field = QuerySpec.Field.AGE;
        spec.query = "invalid";
        spec.numOp = QuerySpec.NumOp.GTE;

        Predicate<Candidate> p = service.buildPredicate(spec);
        assertFalse(p.test(new Candidate("Tom", 22, "IT", 1)));
    }
}
