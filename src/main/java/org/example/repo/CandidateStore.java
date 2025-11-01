package org.example.repo;

import org.example.entities.Candidate;

import java.sql.SQLException;
import java.util.List;

/**
 * Persistence port for {@link Candidate} entities.
 * Defines the operations the Service depends on (DIP).
 */
public interface CandidateStore {

    /**
     * Fetch an immutable snapshot of all candidates.
     *
     * @return list copy of all candidates in storage (never {@code null})
     */
    List<Candidate> findAll();

    /**
     * Insert a new candidate.
     *
     * @param c candidate to insert (must be non-null)
     */
     void add(Candidate c);

    /**
     * Update an existing candidate.
     *
     * @param c candidate with an existing id (>0)
     * @return obj
     */
     Candidate update(Candidate c);

    /**
     * Delete a candidate by id.
     *
     * @param id candidate id (>0)
     */
    void delete(long id);
}
