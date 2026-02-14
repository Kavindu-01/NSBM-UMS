NSBM University Management System

A Spring Boot 3 platform that centralizes admissions, academic services, and community engagement for NSBM Green University. The stack combines Spring MVC + Thymeleaf for dynamic views, Spring Data JPA for persistence, Spring Security for RBAC, and a structured `uploads/` hierarchy for user-generated media. This README captures everything you need to understand, run, test, and deploy the platform end-to-end.

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture & Tech Stack](#architecture--tech-stack)
3. [Directory Layout](#directory-layout)
4. [Platform Capabilities](#platform-capabilities)
5. [Data Model Highlights](#data-model-highlights)
6. [Security Model](#security-model)
7. [Local Setup](#local-setup)
8. [Configuration Profiles](#configuration-profiles)
9. [Build & Run](#build--run)
10. [Testing & QA](#testing--qa)
11. [Seed Users & Roles](#seed-users--roles)
12. [Assets & Media Handling](#assets--media-handling)
13. [Screenshots](#screenshots)
14. [Deployment Checklist](#deployment-checklist)

## System Overview
NSBM UMS unifies several workflows under one authenticated experience:
- Role-specific dashboards for super admins, moderators, club leaders, guardians, shuttle drivers, and students
- Academic resource hub with semester-aware content, timetable management, and document distribution
- Student logistics center that surfaces shuttle routes, guardian boarding listings, and services metadata
- Engagement channel featuring announcements, social feed posts, club directories, and notification badges

## Architecture & Tech Stack
- **Language & Runtime**: Java 17, Spring Boot 3.2.x, Maven 3.9+
- **Framework Modules**: Spring Web, Spring Security 6, Spring Data JPA, Bean Validation, Thymeleaf, Apache POI (Excel)
- **Persistence Layer**: MySQL 8 for production; H2 profile for local demos and automated tests
- **View Layer**: Thymeleaf templates with Bootstrap-based styling and modular fragments under `templates/`
- **Security**: Form login, CSRF enforcement, custom `UserDetailsService`, granular role guards per controller
- **Packaging & Ops**: `spring-boot-maven-plugin` for executable JARs, profile-specific property files, optional Actuator hooks

## Directory Layout
```text
.
├── docs/                     # Process docs, runbooks (e.g., Postman testing guide)
├── src/main/java             # Application code (controllers, entities, services)
├── src/main/resources        # Thymeleaf templates, static assets, YAML/properties
├── src/test/java             # Unit/integration tests
├── uploads/                  # Runtime media: avatars, club posts, social imagery, backups
├── backups/                  # Optional long-lived exports or DB dumps
├── pom.xml                   # Maven descriptor
└── README.md                 # This guide
```

## Platform Capabilities
### Identity & Access
- Self-service registration flows for students, boarding guardians, and shuttle drivers (`AuthController`)
- Moderator approval queues with audit trails tied to batches and semesters (`ModeratorController`)
- Profile center with avatar uploads, bios, and recovery code workflows (`ProfileController`)

### Administration & Governance
- User governance board with search, role reassignment, and Excel import/export via Apache POI (`AdminController`)
- Faculty, degree, and batch management modules to align academic data (`FacultyController`, `SemesterPageController`)
- Club administration suite covering discovery, leadership, membership, and storytelling (`ClubAdminController`, `ClubDirectoryController`, `ClubPresidentController`)

### Student Services & Logistics
- Dashboard variants for each role (`DashboardController`)
- Timetable CRUD plus read-optimized student views (`TimetableController`)
- Shuttle & boarding hubs combining curated profiles, notices, and listing submission forms (`StudentLogisticsController`)

### Engagement & Communications
- Announcements with importance tagging and attribution (`AnnouncementController`)
- Editorial content hub with reactions (`ContentController`, `ContentReaction`)
- Social feed featuring posts, attachments, reactions, and threaded comments
- Centralized notifications table supporting read/unread states and contextual links

## Data Model Highlights
Entity classes live under [src/main/java/lk/nsbm/university/entity](src/main/java/lk/nsbm/university/entity). Key relationships for ER diagrams:
- `User` ↔ `Role`, `AccountStatus`, and FK links to `Faculty`, `DegreeProgram`, `Batch`, `Semester`
- Academic hierarchy: `Faculty` → `DegreeProgram`, `Semester` → `Batch`, `Batch` ↔ `Timetable`
- Clubs: `Club`, `ClubMembership` (join table), `ClubPost`
- Social graph: `SocialPost` with child tables `SocialPostMedia`, `SocialPostReaction`, `SocialPostComment`
- Logistics: `ShuttleNotice` authored by drivers, `BoardingListing` authored by guardians (with photo collection table)
- Content & notifications: `Content`, `ContentReaction`, `Announcement`, `Notification`

## Security Model
- Guard rules are defined in [src/main/java/lk/nsbm/university/security/SecurityConfig.java](src/main/java/lk/nsbm/university/security/SecurityConfig.java)
- Highlights:
  - `/login`, `/register/**`, `/css|js|images|media` are public
  - `/admin/**` requires `SUPER_ADMIN` or `ADMIN`
  - `/moderator/**` allows `SUPER_ADMIN`, `ADMIN`, `MODERATOR`
  - `/services/**`, `/clubs/**`, `/social/**`, `/dashboard` require authentication
  - CSRF tokens are mandatory for POST/PUT/DELETE (see [docs/postman-testing.md](docs/postman-testing.md) for Postman instructions)
- Passwords are hashed with `BCryptPasswordEncoder`

## Local Setup
1. **Install prerequisites**: Java 17, Maven 3.9+, Node.js (optional for Newman), MySQL 8 if not using H2
2. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd "Project Demo/New folder"
   ```
3. **Configure data sources**
   - Copy `src/main/resources/application.properties` and tailor credentials for your environment
   - For MySQL specify `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
   - For H2 simply enable the `h2` profile (properties already provided in `application-h2.properties`)
4. **Prepare directories**: ensure `uploads/` and `backups/` exist and are writable; they are ignored by Git but required at runtime

## Configuration Profiles
- **mysql** (default): Targets an external MySQL database; set creds through properties or environment variables
- **h2**: Launch with `mvn spring-boot:run -Dspring-boot.run.profiles=h2` to spin up an in-memory DB for demos/tests
- **custom**: Add `application-<profile>.properties` for QA or staging and activate via `--spring.profiles.active=<profile>`

Profile precedence follows Spring Boot conventions: command-line args > environment variables > profile file > `application.properties`.

## Build & Run
### Development mode (MySQL)
```bash
mvn clean verify
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
Visit <http://localhost:8080> and sign in with a seeded account.

### Development mode (H2)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Optional H2 console: <http://localhost:8080/h2-console> (credentials defined in `application-h2.properties`).

### Packaged JAR
```bash
mvn clean package -DskipTests
java -jar target/university-management-1.0.0.jar --spring.profiles.active=mysql
```

### Automated tests
```bash
mvn test
```

## Testing & QA
- **Manual API/UI**: Follow the beginner-friendly walkthrough in [docs/postman-testing.md](docs/postman-testing.md) to capture CSRF tokens, authenticate, and export a Postman collection. The same guide explains how to run the suite via Newman and how to pair it with the packaged JAR for offline testers.
- **UI smoke checks**: Role dashboards under `src/main/resources/templates/dashboard/` provide quick visual sanity tests for each persona.
- **Data initialization**: [DataInitializer](src/main/java/lk/nsbm/university/config/DataInitializer.java) seeds semesters, content, and privileged accounts so QA can start immediately.

## Seed Users & Roles
| Role | Student ID | Password | Permissions |
| ---- | ---------- | -------- | ----------- |
| SUPER_ADMIN | ADMIN01 | Admin@123 | Full platform control, user management, approvals |
| MODERATOR | MOD01 | Mod@123 | Approvals queue, logistics oversight |
| CONTENT_MANAGER | CONTENT01 | Content@123 | Editorial content + services catalog |
| EDITOR | EDITOR01 | Editor@123 | Showcase updates, announcements |

Create additional users through the registration flows or admin console as needed (e.g., shuttle drivers, guardians, club presidents).

## Assets & Media Handling
- `uploads/media` holds avatars, club covers, social images
- `uploads/club-posts`, `uploads/social`, and `uploads/semester` store feature-specific files referenced by templates
- `uploads/backups` stores Excel exports generated from the admin UI

## Screenshots

Admin Dashboard :- <img width="1920" height="2988" alt="screencapture-localhost-8080-dashboard-2026-02-14-18_26_19" src="https://github.com/user-attachments/assets/7bebcc5a-f53a-4100-829e-9a45d4faca08" />
Student Dashboard :- <img width="1920" height="868" alt="screencapture-localhost-8080-dashboard-2026-02-14-18_32_44" src="https://github.com/user-attachments/assets/e15e3b37-94ca-43b1-8b04-e7ecf7257341" />
Shuttle Driver Dashboard :- <img width="1920" height="868" alt="screencapture-localhost-8080-dashboard-2026-02-14-18_34_34" src="https://github.com/user-attachments/assets/e6ed753d-4590-46a5-bb84-4bd02b32494f" />
Boarding Guardian Dashboard :- <img width="1920" height="868" alt="screencapture-localhost-8080-dashboard-2026-02-14-18_37_20" src="https://github.com/user-attachments/assets/6b7333c0-98c6-4823-a685-f08e61cd71af" />
Boarding Guardian Registration :- <img width="1920" height="1201" alt="screencapture-localhost-8080-register-guardian-2026-02-14-18_35_48" src="https://github.com/user-attachments/assets/db8ec802-de59-42f3-82ca-8ad56b1893bd" />
Shuttle Driver Registration :- <img width="1920" height="1327" alt="screencapture-localhost-8080-register-shuttle-2026-02-14-18_33_47" src="https://github.com/user-attachments/assets/7bb49afa-66f5-4615-a019-6ee44b3816ab" />
Login UI :- <img width="1920" height="887" alt="screencapture-localhost-8080-login-2026-02-14-18_40_06" src="https://github.com/user-attachments/assets/e85b4f36-08e8-45d9-97b0-ff6ec4bdd044" />

## Deployment Checklist
- [ ] Externalize secrets via environment variables or a vault (`SPRING_DATASOURCE_USERNAME`, SMTP creds, etc.)
- [ ] Provision MySQL (or managed equivalent) and ensure schema migrations run before the first boot
- [ ] Configure persistent volumes for `uploads/` and `backups/`
- [ ] Harden security: disable H2 console, reduce log levels to `INFO`, enforce HTTPS via reverse proxy or `server.ssl.*`
- [ ] Enable monitoring (Logback JSON, ELK, Grafana Loki, or Spring Boot Actuator) for runtime visibility
- [ ] Schedule periodic cleanup of exports and orphaned media files

