# 🧠 Candidate Bank – Recruitment Management System

![Java](https://img.shields.io/badge/Java-21-blue)
![Maven](https://img.shields.io/badge/Maven-Build-orange)
![License](https://img.shields.io/badge/License-MIT-green)
![Tests](https://img.shields.io/badge/Tests-Passed-brightgreen)
![Platform](https://img.shields.io/badge/Platform-Desktop-lightgrey)

---

## 📋 Overview
**Candidate Bank** is a lightweight **desktop recruitment management system** built with Java 21 and Swing.  
It helps recruiters easily manage candidates — add, update, filter, and search by experience, age, or industry.  
The project demonstrates a clean **layered architecture** with a focus on modular design, testability, and persistence.

---

## 🧱 Architecture

| Layer | Description |
|-------|--------------|
| **Entities** | Core domain objects (`Candidate`, etc.) |
| **Repository (DTO)** | Handles persistence using file or SQLite (`CandidateRepository`) |
| **Service** | Business logic, validation, filtering, sorting |
| **Utils** | QuerySpec, Filter, and helper classes for typed queries |

---

## 🚀 Features

✅ Add / Update / Delete candidates  
✅ Filter by industry, experience, or age  
✅ Sort by name or registration date  
✅ In-memory + File-based repository (TSV)  
✅ Validation and error handling  
✅ Unit tests with JUnit 5 + Mockito  
✅ Ready for SQLite or JDBC extension

---

## ⚙️ Tech Stack

| Technology | Purpose |
|-------------|----------|
| **Java 21 (Temurin)** | Main language |
| **Maven** | Build automation |
| **JUnit 5** | Testing framework |
| **Mockito** | Mock-based testing |
| **SLF4J + Logback** | Logging |

---

## 🧩 Project Structure

---

## 🧰 Build & Run

### 🏗️ Compile and package
```bash
mvn clean package
java -jar target/Candidate-Bank-1.0-SNAPSHOT.jar
mvn test

Candidate c = new Candidate("David", 39, "IT", 12);
CandidateService service = new CandidateService(new CandidateRepository("data/candidates.tsv"));
service.add(c);
service.query(new QuerySpec(Field.INDUSTRY, TextOp.CONTAINS, "IT"));

