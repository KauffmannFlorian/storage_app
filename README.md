# Teletronics Storage API

A RESTful file storage service built with **Spring Boot 3 (Java 21)** and **MongoDB GridFS**.
Designed to handle large uploads, secure access, and metadata management.

---

## Overview

Features:

* File upload, download, rename, delete, retrieve
* Tag-based filtering and visibility control (PUBLIC/PRIVATE)
* Secure, unguessable download tokens
* Duplicate prevention (same filename or content)
* Health check and Swagger documentation

---

## Tech Stack

* **Language:** Java 21
* **Framework:** Spring Boot 3
* **Database:** MongoDB + GridFS
* **File type:** Apache Tika
* **Build:** Maven
* **Docs:** Swagger (springdoc-openapi)
* **Tests:** JUnit 5
* **Container:** Docker, CI via GitHub Actions

---

## Run

```bash
docker run -d -p 27017:27017 -e MONGO_INITDB_DATABASE=storage_app --name mongo mongo
mvn clean spring-boot:run
mvn test
```

## Run with Docker

```bash
docker build -t storage_app:latest .
docker run --memory=1g --storage-opt size=200m --rm storage_app java -version
```

Swagger UI → [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## API Summary

| Method | Endpoint        | Description                           |
| ------ |-----------------|---------------------------------------|
| POST   | `/files/upload` | Upload a file                         |
| GET    | `/files`        | List files (filter by visibility/tag) |
| GET    | `/public`       | List public files                     |   
| GET    | `/files/download/{token}` | Download a file                       |
| PATCH  | `/files/{id}/rename`      | Rename a file                         |
| DELETE | `/files/{id}`             | Delete a file                         |
| GET    | `/health`                 | Health check                          |

All endpoints require `X-User-Id` header.

---

## Tests Implemented

* Parallel upload (same name/content)
* 2GB simulated upload
* Unauthorized delete attempt
* List all public files
* Health check

Run:

```bash
mvn test
```

---

## Non-Functional Requirements

| Requirement       | Status |
| ----------------- | ------ |
| ≤1GB memory limit | ✅      |
| ≤200MB disk image | ✅      |
| No UI (API only)  | ✅      |
| MongoDB usage     | ✅      |
| Dockerized        | ✅      |
| CI build          | ✅      |

