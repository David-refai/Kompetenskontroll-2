package org.example;

import org.example.dto.CandidateRepository;
import org.example.service.CandidateService;

import static utils.Utility.loadIfEmpty;

/**
 * Entry point of the Candidate Management application.
 * -
 * This version uses:
 *  - CandidateRepository → file-based data storage (TSV format)
 *  - CandidateService → business logic layer
 *  - SeedData.loadIfEmpty → optional initial data loader
 * -
 * The App class (if present) usually provides a CLI or service layer to run interactions.
 */
public class Main {
    public static void main(String[] args) {

        // 1️⃣ Create a repository that handles file persistence
        var repo = new CandidateRepository("data/candidates.tsv");

        // 2️⃣ Create the service layer that uses the repository
        var service = new CandidateService(repo);

        // 3️⃣ Load some sample data if the file is currently empty
        loadIfEmpty(repo);

        // 4️⃣ Start the main application (likely a CLI or menu-driven system)
        // If you don’t use a UI, you can comment this out and directly call service methods
        new App(service).run();
    }
}
