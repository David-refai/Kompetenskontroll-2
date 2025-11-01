package utils;

import org.example.entities.Candidate;
import org.example.repo.CandidateStore;

import java.util.List;

/**
 * Utility class providing sample data (seeding) and console printing helpers.
 * --------------------------------------------------------------
 * Responsibilities:
 *  - Seed the repository with initial {@link Candidate} data if empty.
 *  - Print formatted candidate information to the console.
 *  - Provide small console utilities (print, println, printMenu).
 * -
 * This class is final and cannot be instantiated.
 */
public final class Utility {

    /** Private constructor to prevent instantiation. */
    private Utility() {}

    // =====================================================================
    //  CONFIGURATION (Colors)
    // =====================================================================

    /** Enable or disable ANSI colors in console output. */
    private static final boolean USE_COLOR = true; // set too false to disable colors

    private static String c(String code) { return USE_COLOR ? code : ""; }

    private static final String R  = c("\u001B[0m");   // Reset
    private static final String B  = c("\u001B[1m");   // Bold
    private static final String CY = c("\u001B[36m");  // Cyan
    private static final String GR = c("\u001B[90m");  // Gray
    private static final String BL = c("\u001B[34m");  // Blue

    /** Returns "-" for null or blank strings to avoid null text in console. */
    private static String nz(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    // =====================================================================
    //  DATA LOADING (SEEDING)
    // =====================================================================

    /**
     * Loads sample candidate data only if the repository is empty.
     * <p>
     * This ensures the method can be safely called multiple times
     * without duplicating data.
     *
     * @param store the repository (or store) to populate
     */
    public static void loadIfEmpty(CandidateStore store) {
        if (store.findAll().isEmpty()) {
            load(store);
        }
    }

    /**
     * Always loads and appends the sample candidate data,
     * even if data already exists in the repository.
     *
     * @param store the repository to add sample candidates into
     */
    public static void load(CandidateStore store) {
        for (Candidate c : samples()) {
            store.add(c);
        }
    }

    /**
     * Provides a predefined list of sample {@link Candidate} objects
     * used for seeding demo or testing data.
     *
     * @return immutable list of sample candidates
     */
    public static List<Candidate> samples() {
        return List.of(
                cand("Alice Johnson",   28, "Software",    5),
                cand("Bob Martin",      35, "Finance",     10),
                cand("Carla Gomez",     31, "Marketing",   7),
                cand("David Alrefai",   26, "Software",    3),
                cand("Elena Petrova",   29, "Healthcare",  6),
                cand("Fahad Al-Salem",  33, "Sales",       8),
                cand("Grace Kim",       24, "Design",      2),
                cand("Hassan Ali",      41, "Education",   15),
                cand("Isabella Rossi",  30, "Software",    6),
                cand("Jamal Hassan",    27, "Retail",      4),
                cand("Karin Svensson",  38, "Operations",  12),
                cand("Lars Nilsson",    22, "Support",     1)
        );
    }

    /**
     * Factory helper for creating candidate objects with given parameters.
     * IDs are always set to zero to allow the repository to auto-assign them.
     *
     * @param name candidate name
     * @param age candidate age
     * @param industry candidate industry
     * @param years candidate’s years of experience
     * @return a new {@link Candidate} instance
     */
    private static Candidate cand(String name, int age, String industry, int years) {
        Candidate c = new Candidate();
        c.setId(0L); // let repository assign ID
        c.setName(name);
        c.setAge(age);
        c.setIndustry(industry);
        c.setYearsOfExperience(years);

        return c;
    }

    // =====================================================================
    //  CONSOLE PRINTING (FORMATTER)
    // =====================================================================

    /**
     * Prints a single candidate as a formatted card in the console.
     * Uses box-drawing characters and color highlighting.
     *
     * @param candidate the candidate to display
     */
    public static void printListOfCa(Candidate candidate) {
        if (candidate == null) {
            System.out.println(GR + "No candidate to display." + R);
            return;
        }

        String name = nz(candidate.getName());
        String industry = nz(candidate.getIndustry());
        int age = candidate.getAge();
        int yrs = candidate.getYearsOfExperience();
        long id = candidate.getId();

        int width = 50;
        String hr = "═".repeat(width - 2);

        // Header
        System.out.println(BL + "╔" + hr + "╗" + R);
        System.out.println(BL + "║" + R + "  " + B + CY + "Name: " + R + name +
                " ".repeat(Math.max(0, width - 10 - name.length())) + BL + "║" + R);
        System.out.println(BL + "╠" + "═".repeat(width - 2) + "╣" + R);

        // Body
        System.out.printf(BL + "║" + R + "  %s%-20s%s: %s%n", CY, "ID", R, id);
        System.out.printf(BL + "║" + R + "  %s%-20s%s: %s%n", CY, "Industry", R, industry);
        System.out.printf(BL + "║" + R + "  %s%-20s%s: %d%n", CY, "Age", R, age);
        System.out.printf(BL + "║" + R + "  %s%-20s%s: %d%n",
                CY, "Years of Experience", R, yrs);

        // Footer
        System.out.println(BL + "╚" + "═".repeat(width - 2) + "╝" + R);
    }

    // =====================================================================
    //  BASIC CONSOLE UTILITIES
    // =====================================================================

    /**
     * Prints a line of text to the console with a newline.
     *
     * @param s text to print
     */
    public static void println(String s) {
        System.out.println(s);
    }

    /**
     * Prints text to the console without a newline.
     *
     * @param s text to print
     */
    public static void print(String s) {
        System.out.print(s);
    }

    /**
     * Prints a numbered list of menu options to the console.
     * Each option is numbered starting from 1.
     *
     * @param items variable number of menu items to print
     */
    public static void printMenu(String... items) {
        for (int i = 0; i < items.length; i++) {
            println((i + 1) + ") " + items[i]);
        }
    }
}
