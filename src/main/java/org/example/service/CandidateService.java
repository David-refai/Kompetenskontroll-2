package org.example.service;

import org.example.error.ErrorHandling;
import org.example.entities.Candidate;
import org.example.repo.CandidateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.QuerySpec;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static utils.Filters.*; // byFirstNameAsc/Desc, byIndustryContains, byMinYears

/**
 * Application service orchestrating business operations on {@link Candidate}.

 * Responsibilities:
 *  - Convert UI-level {@link QuerySpec} into a Predicate/Comparator
 *  - Run in-memory filtering/sorting over the repository snapshot
 *  - Enforce domain validation (above the UI)
 *  - Delegate persistence to {@link CandidateStore} (DIP)

 * Exceptions:
 *  - Throws {@link ErrorHandling} for user-facing/HTTP-friendly errors:
 *      BAD_REQUEST for validation issues,
 *      NOT_FOUND for missing entities (when relevant).
 */
public class CandidateService {
    private static final Logger log = LoggerFactory.getLogger(CandidateService.class);

    private final CandidateStore store;

    /**
     * @param store abstraction of persistence (DIP). In production: SQLite/File repo, etc.
     */
    public CandidateService(CandidateStore store) {
        this.store = store;
    }

    // ---------------------------
    // Read/query API
    // ---------------------------

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    /**
     * Query using explicit predicate/sort.
     *
     * @param filter predicate to select candidates (nullable -> treated as match-all)
     * @param sort   comparator to order candidates (nullable -> no ordering)
     * @return filtered/sorted list snapshot (never null)
     */
    public List<Candidate> query(Predicate<Candidate> filter, Comparator<Candidate> sort) {
        var stream = store.findAll().stream();
        if (filter != null)
            stream = stream.filter(filter);
        if (sort != null)
            stream = stream.sorted(sort);
        var out = stream.collect(Collectors.toList());
        log.debug("query(): result={}", out.size());
        return out;
    }

    /**
     * Builds a list based on a structured query specification.
     * Converts the spec into a {@link java.util.function.Predicate} and an optional {@link java.util.Comparator}.
     *
     * @param spec UI-driven query spec (null → empty spec)
     * @return filtered and optionally sorted snapshot; never {@code null}
     */
    public List<Candidate> query(QuerySpec spec) {
        if (spec == null) spec = new QuerySpec();
        Predicate<Candidate> p = buildPredicate(spec);
        Comparator<Candidate> c = buildComparator(spec);
        return query(p, c);
    }

    /**
     * Build a filtering predicate from the given {@link QuerySpec}.
     * NAME/INDUSTRY use text operators; AGE/YEARS use numeric operators.
     * If a numeric parse fails, returns a predicate that matches nothing.
     *
     * @param s the structured query specification
     * @return predicate to filter candidates according to {@code s}
     */
    public Predicate<Candidate> buildPredicate(QuerySpec s) {
        try {
            switch (s.field) {
                case NAME -> {
                    String q = safeLower(s.query);
                    return switch (s.textOp) {
                        case CONTAINS    -> x -> safeLower(x.getName()).contains(q);
                        case STARTS_WITH -> x -> safeLower(x.getName()).startsWith(q);
                        case EQUALS      -> x -> safeLower(x.getName()).equals(q);
                    };
                }
                case INDUSTRY -> {
                    return byIndustryContains(s.query); // case-insensitive inside helper
                }
                case AGE -> {
                    int n = Integer.parseInt(s.query);
                    return switch (s.numOp) {
                        case EQ  -> x -> x.getAge() == n;
                        case GTE -> x -> x.getAge() >= n;
                        case LTE -> x -> x.getAge() <= n;
                    };
                }
                case YEARS -> {
                    int n = Integer.parseInt(s.query);
                    return switch (s.numOp) {
                        case EQ  -> x -> x.getYearsOfExperience() == n;
                        case GTE -> byMinYears(n);
                        case LTE -> x -> x.getYearsOfExperience() <= n;
                    };
                }
                default -> {
                    return x -> true; // unknown -> no filtering
                }
            }
        } catch (NumberFormatException e) {
            // Bad number in numeric fields → no matches
            log.debug("buildPredicate(): bad number '{}', returning always-false", s.query);
            return x -> false;
        }
    }

    /**
     * Build a Comparator from {@link QuerySpec#sort}.
     * Returns null when no sorting is requested.
     */
    private Comparator<Candidate> buildComparator(QuerySpec s) {
        return switch (s.sort) {
            case NAME_ASC  -> byFirstNameAsc();
            case NAME_DESC -> byFirstNameDesc();
            case DATEOFREGISTER -> byDateOfRegistering();
        };
    }




    // ---------------------------
    // Write API
    // ---------------------------

    /**
     * Add a candidate after validation.
     * @param c candidate to add
     * @throws ErrorHandling if validation fails (BAD_REQUEST) or repository fails (INTERNAL_ERROR)
     */
    public void add(Candidate c) {
        validate(c, true);
        try {
            store.add(c);
        } catch (RuntimeException ex) {
            // Normalize unexpected repo errors to INTERNAL (leave original cause inside)
            throw new ErrorHandling("Failed to add candidate", ex, 500, "SERVICE_ADD", null);
        }
    }

    /**
     * Update a candidate after domain validation.
     *
     * @param c candidate with id > 0
     * @return the updated entity (copy) or throws NOT_FOUND if id doesn't exist
     */
    public Candidate update(Candidate c) {
        validate(c, false);
        try {
            Candidate updated = store.update(c);
            if (updated == null) {
                throw ErrorHandling.notFound("update(): id=" + c.getId() + " not found");
            }
            return updated;
        } catch (ErrorHandling eh) {
            // propagate structured errors as-is
            throw eh;
        } catch (RuntimeException ex) {
            throw new ErrorHandling("Failed to update candidate id=" + c.getId(),
                    ex, 500, "SERVICE_UPDATE", null);
        }
    }

    /**
     * Delete by id after validation.
     *
     * @param id candidate id > 0
     */
    public void delete(long id) {
        if (id <= 0) throw ErrorHandling.badRequest("id must be > 0");
        try {
            store.delete(id);
        } catch (ErrorHandling eh) {
            throw eh; // keep NOT_FOUND/INTERNAL mapping from repository
        } catch (RuntimeException ex) {
            throw new ErrorHandling("Failed to delete candidate id=" + id,
                    ex, 500, "SERVICE_DELETE", null);
        }
    }

    /**
     * Fetch all candidates (info-level logging).
     * @return immutable snapshot of all candidates
     */
    public List<Candidate> findAll() {
        System.out.println();
        return store.findAll();
    }

    // ---------------------------
    // Validation
    // ---------------------------

    /**
     * Centralized domain validation (kept above the UI).
     *
     * @param isCreate when true, id may be 0; when false, id must be > 0
     * @throws ErrorHandling BAD_REQUEST on any rule violation
     */
    private void validate(Candidate c, boolean isCreate) {
        String ind = getInd(c, isCreate);
        if (ind.matches("\\d+"))
            throw ErrorHandling.badRequest("Industry must be text, not a number");

        int age = c.getAge();
        int yrs = c.getYearsOfExperience();
        if (age < 16)
            throw ErrorHandling.badRequest("Minimum age is 16");
        if (yrs < 0 || yrs > age)
            throw ErrorHandling.badRequest("Invalid years of experience");
    }

    /**
     * Extract and basic-check identity & text fields:
     * - candidate must not be null
     * - if updated: id must be > 0
     * - name required (non-empty)
     * - industry length >= 2
     */
    private static String getInd(Candidate c, boolean isCreate) {
        if (c == null)
            throw ErrorHandling.badRequest("candidate is null");
        if (!isCreate && c.getId() <= 0)
            throw ErrorHandling.badRequest("id required for update");

        String name = c.getName() == null ? "" : c.getName().trim();
        String ind  = c.getIndustry() == null ? "" : c.getIndustry().trim();

        if (name.isEmpty())
            throw ErrorHandling.badRequest("Name is required");
        if (ind.length() < 2)
            throw ErrorHandling.badRequest("Industry must be at least 2 characters");
        return ind;
    }

}
