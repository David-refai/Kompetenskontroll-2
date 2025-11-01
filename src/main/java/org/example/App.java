package org.example;

import org.example.error.ErrorHandling;
import org.example.entities.Candidate;
import org.example.service.CandidateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.QuerySpec;
import utils.QuerySpec.Field;
import utils.QuerySpec.NumOp;
import utils.QuerySpec.Sort;
import utils.QuerySpec.TextOp;

import java.util.List;
import java.util.Scanner;

import static utils.QuerySpec.Sort.*;
import static utils.Utility.*;

/**
 * Console-based (non-Spring) Candidate Management Application.
 * ------------------------------------------------------------
 * This class provides a simple command-line interface (CLI) to manage candidates.
 * -
 * Features:
 *  - Create, Read, Update, Delete candidates (CRUD)
 *  - Search by text or number fields (name, industry, age, years)
 *  - Sorting results alphabetically or numerically
 * -
 * Internally delegates all logic to {@link CandidateService}.
 */
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    private final CandidateService service;
    private final Scanner in = new Scanner(System.in);

    // Optional ANSI color codes for colored terminal output
    private static final boolean COLOR = true;
    private static String c(String code) { return COLOR ? code : ""; }
    private static final String R   = c("\u001B[0m");
    private static final String B   = c("\u001B[1m");
    private static final String G   = c("\u001B[32m");
    private static final String Y   = c("\u001B[33m");
    private static final String RED = c("\u001B[31m");

    /**
     * Constructs the console application and injects a CandidateService instance.
     *
     * @param service service layer responsible for business logic and repository access
     */
    public App(CandidateService service) {
        this.service = service;
    }

    /**
     * Starts the main console loop.
     * Displays the menu and dispatches actions until the user exits.
     */
    public void run() {
        while (true) {
            println(B + "\n=== Candidate Manager ===" + R);
            printMenu("List all", "Add", "Update", "Delete", "Search");
            println("0) Exit");
            print(Y + "Choose: " + R);

            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> listAll();
                    case "2" -> add();
                    case "3" -> update();
                    case "4" -> deleteById();
                    case "5" -> searchMenu();
                    case "0" -> {
                        println(G + "Goodbye!" + R);
                        return;
                    }
                    default -> println(RED + "Invalid choice." + R);
                }
            } catch (ErrorHandling eh) {
                println(RED + "[App Error] " + eh.getCode() + ": " + eh.getMessage() + R);
            } catch (Exception e) {
                println(RED + "[System Error] " + e.getMessage() + R);
            }
        }
    }

    // =====================================================================
    //  CRUD METHODS
    // =====================================================================

    /**
     * Displays all existing candidates in the repository.
     * Prints "No candidates found" if the list is empty.
     */
    private void listAll() {
        List<Candidate> all = service.findAll();
        if (all.isEmpty()) {
            log.warn("{} No candidate found.", B);
            return;
        }
        for (Candidate candidate : all) {
            printListOfCa(candidate);
        }

    }

    /**
     * Prompts the user to enter candidate data (name, age, industry, years).
     * Validates and saves a new candidate using the service layer.
     */
    private void add() {
        println(B + "\n-- Add Candidate --" + R);

        String name = ask("Name");
        int age = askInt("Age");
        String industry = ask("Industry");
        int years = askInt("Years of experience");


        Candidate c = new Candidate(name, age, industry, years);
        service.add(c);
        log.info("{}Added Candidate {} {}", B, c.getName(), c.getAge());
    }

    /**
     * Updates an existing candidate’s data by ID.
     * Allows partial updates — leaving a field blank keeps its current value.
     */
    private void update() {
        println(B + "\n-- Update Candidate --" + R);
        long id = askLong();

        // Find candidate by ID
        List<Candidate> list = service.query(x -> x.getId() == id, null);
        if (list.isEmpty()) {
            throw ErrorHandling.notFound("No Candidate found with id " + id);
        }

        Candidate c = list.getFirst();
        println(Y + "Press Enter to keep current value." + R);

        String name = askOpt("Name", c.getName());
        String ageS = askOpt("Age", Integer.toString(c.getAge()));
        String ind  = askOpt("Industry", c.getIndustry());
        String yrsS = askOpt("Years", Integer.toString(c.getYearsOfExperience()));

        // Apply new values if provided
        if (!name.isBlank()) c.setName(name.trim());
        if (!ageS.isBlank()) c.setAge(parseIntSafe(ageS, c.getAge()));
        if (!ind.isBlank())  c.setIndustry(ind.trim());
        if (!yrsS.isBlank()) c.setYearsOfExperience(parseIntSafe(yrsS, c.getYearsOfExperience()));

        Candidate updated = service.update(c);
        println(G + "Updated successfully (ID=" + updated.getId() + ") 🎉" + R);
    }

    /**
     * Deletes a candidate by its ID.
     * Asks for confirmation and then calls the service layer to remove it.
     */
    private void deleteById() {
        println(B + "\n-- Delete Candidate --" + R);
        long id = askLong();
        service.delete(id);
        println(G + "Deleted ID=" + id + R);
    }

    // =====================================================================
    //  SEARCH METHODS
    // =====================================================================

    /**
     * Interactive search allowing field selection, filter type, and sorting.
     * Combines all QuerySpec options: text, numeric, and sort.
     */
    private void searchMenu() {
        println(B + "\n-- Search Candidates --" + R);

        // 1️⃣ Choose which field to search (name, industry, age, years)
        Field field = chooseField();

        // 2️⃣ Enter search query
        String query = ask("(text/number, depends on field: 👉🏻");

        // 3️⃣ Choose operator type
        TextOp textOp = TextOp.CONTAINS;
        NumOp numOp = NumOp.EQ;
        if (field == Field.NAME || field == Field.INDUSTRY) {
            textOp = chooseTextOp();
        } else if (field == Field.AGE || field == Field.YEARS) {
            numOp = chooseNumOp();
        }

        // 4️⃣ Choose sorting order
        Sort sort = chooseSort();

        // 5️⃣ Execute query
        QuerySpec spec = new QuerySpec();
        spec.field = field;
        spec.query = query;
        spec.textOp = textOp;
        spec.numOp = numOp;
        spec.sort = sort;

        List<Candidate> results = service.query(spec);
        if (results.isEmpty()) {
            log.warn("{} No candidate found. 😵‍💫", B);
        } else {
            for (Candidate candidate : results) {
                printListOfCa(candidate);
            }
//        service.findByName(query);
        }
    }

    // =====================================================================
    //  INPUT HELPERS
    // =====================================================================

    /**
     * Prompts the user with a label and reads a single line of text input.
     */
    private String ask(String label) {
        print(B + label + ": " + R);
        return in.nextLine().trim();
    }

    /**
     * Prompts with a default value shown in brackets.
     * Pressing Enter keeps the existing value.
     */
    private String askOpt(String label, String current) {
        print(B + label + " [" + current + "]: " + R);
        return in.nextLine();
    }

    /**
     * Reads and validates an integer from console input.
     * Keeps asking until a valid number is entered.
     */
    private int askInt(String label) {
        while (true) {
            String s = ask(label);
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                log.warn("{}Enter a valid integer.{}", RED, R);
            }
        }
    }

    /**
     * Reads and validates a long (ID) from console input.
     * Keeps asking until a valid long number is entered.
     */
    private long askLong() {
        while (true) {
            String s = ask("ID");
            try {
                return Long.parseLong(s);
            } catch (Exception e) {
                println(RED + "Enter a valid long number." + R);
            }
        }
    }

    /**
     * Safely parses a string into an integer.
     * Returns a default value if parsing fails.
     */
    private static int parseIntSafe(String v, int def) {
        try {
            return Integer.parseInt(v); }
        catch (Exception e) {
            return def;
        }
    }



    // =====================================================================
    //  ENUM CHOICE MENUS
    // =====================================================================

    /**
     * Lets the user select which field to filter by: name, industry, age, or years.
     */
    private Field chooseField() {
        println("\nSelect field: 👇");
        printMenu("NAME", "INDUSTRY", "AGE", "YEARS");
        return switch (ask("Choice")) {
            case "2" -> Field.INDUSTRY;
            case "3" -> Field.AGE;
            case "4" -> Field.YEARS;
            default -> Field.NAME;
        };
    }

    /**
     * Lets the user choose a text-based operator for name or industry filtering.
     */
    private TextOp chooseTextOp() {
        println("\nText operator:");
        printMenu("CONTAINS", "STARTS_WITH", "EQUALS");
        return switch (ask("Choice")) {
            case "2" -> TextOp.STARTS_WITH;
            case "3" -> TextOp.EQUALS;
            default -> TextOp.CONTAINS;
        };
    }

    /**
     * Lets the user choose a numeric comparison operator (EQ, GTE, LTE).
     */
    private NumOp chooseNumOp() {
        println("\nNumber operator:");
        printMenu("EQ", "GTE", "LTE");
        return switch (ask("Choice")) {
            case "2" -> NumOp.GTE;
            case "3" -> NumOp.LTE;
            default -> NumOp.EQ;
        };
    }

    /**
     * Lets the user choose a sorting mode for query results.
     */
    private Sort chooseSort() {

        println("\nSorting:");
        printMenu("TIME_OF_REGISTERING", "NAME_ASC", "NAME_DESC");
        return switch (ask("Choice")) {
            case "2" -> NAME_ASC;
            case "3" -> NAME_DESC;
            default -> DATEOFREGISTER;
        };
    }
}
