# AI Resume Forge - Java Backend

A minimal Spring Boot backend scaffold.

## Requirements

- Java 17+
- Maven 3.8+

## Run

```bash
cd java-backend
mvn spring-boot:run
```

Server starts on `http://localhost:8080`.

## Build

```bash
mvn clean package
```

## Structure

```
src/main/java/com/airesumeforge/
├── AiResumeForgeApplication.java   # Main class
├── controller/                      # REST controllers
├── service/                         # Business logic
├── repository/                      # Data access
├── model/                           # Entities
└── config/                          # Configuration
```
