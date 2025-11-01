package utils;

import org.example.entities.Candidate;

import java.util.Comparator;
import java.util.function.Predicate;

/**
 * <h2>Typed Query Strategy Pattern</h2>
 * Reusable Stream predicates and comparators for {@link Candidate}.
 * <p>
 * Purpose:
 * <ul>
 *   <li>Encapsulate common filters/sorters (OCP-friendly)</li>
 *   <li>Keep UI/Service code clean and expressive</li>
 * </ul>
 */
public final class Filters {
    private Filters() {
    }

    /**
     * Matches candidates whose industry contains the given query (case-insensitive).
     *
     * @param q free-text query; null/blank → matches all
     * @return predicate for {@code Candidate}
     */
    public static Predicate<Candidate> byIndustryContains(String q) {
        String qq = q == null ? "" : q.trim().toLowerCase();
        if (qq.isEmpty())
            return c -> true; // no filter if query is empty
        return c -> c.getIndustry() != null && c.getIndustry().toLowerCase().contains(qq);
    }

    /**
     * Matches candidates with {@code yearsOfExperience >= min}.
     *
     * @param min minimum years of experience
     * @return predicate for {@code Candidate}
     */
    public static Predicate<Candidate> byMinYears(int min) {
        return c -> c.getYearsOfExperience() >= min;
    }

    /**
     * Sort by first name ascending (null-safe, case-insensitive).
     *
     * @return comparator for {@code Candidate}
     */
    public static Comparator<Candidate> byFirstNameAsc() {
        return Comparator.comparing(c -> c.getName() == null ? "" : c.getName().toLowerCase());
    }

    /**
     * Sort by first name descending (null-safe, case-insensitive).
     *
     * @return comparator for {@code Candidate}
     */
    public static Comparator<Candidate> byFirstNameDesc() {
        return byFirstNameAsc().reversed();
    }


    /**
     * Default sorting, when user not chios any typ of sort, instead of null value.
     *
     * @return comparator for {@code Candidate}
     */
    public static Comparator<Candidate> byDateOfRegistering() {
        return Comparator.comparing(
                Candidate::getDateTimeOfRegister,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
    }
}
