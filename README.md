# NSBM University Management System

Academic information hub that centralizes student onboarding, community engagement, logistics, and semester resources for NSBM Green University. The platform is built with Spring Boot 3, secures access with Spring Security, renders UI with Thymeleaf, and persists data through Spring Data JPA.

## Tech Stack
- Java 17, Spring Boot 3.2.1 (Web, Data JPA, Security, Validation, Thymeleaf)
- Spring Security 6 with role-based dashboards and throttled approvals
- Hibernate + MySQL for primary persistence (profile-based H2 support for local runs)
- Apache POI for Excel backups and restores of user data
- Thymeleaf templates for server-side rendered admin and student experiences
- Maven build lifecycle with `spring-boot-maven-plugin`

## Feature Map (What You Can Do)
The controllers in `src/main/java/lk/nsbm/university/controller` map directly to functional areas. Highlights include:

### Identity & Access
- Student, guardian, and shuttle registrations with validation flows ([AuthController](src/main/java/lk/nsbm/university/controller/AuthController.java)).
- Moderator approval queues with batch/semester assignment ([ModeratorController](src/main/java/lk/nsbm/university/controller/ModeratorController.java)).
- Profile management with avatar uploads and auto-generated fallbacks ([ProfileController](src/main/java/lk/nsbm/university/controller/ProfileController.java)).

### Administrative Workspace
- Central user directory with filtering by status/role, inline approvals, credential resets, and soft-guardrails for Super Admin actions.
- Excel backup/restore of accounts plus batch CRUD tooling ([AdminController](src/main/java/lk/nsbm/university/controller/AdminController.java)).
- Club catalog provisioning, cover-media management, and president assignment ([ClubAdminController](src/main/java/lk/nsbm/university/controller/ClubAdminController.java)).

### Communications & Engagement
- Announcement lifecycle (list, detail, manage) with creator attribution ([AnnouncementController](src/main/java/lk/nsbm/university/controller/AnnouncementController.java)).
- Editorial content hub with per-role category permissions, hero imagery uploads, and emoji-style reactions ([ContentController](src/main/java/lk/nsbm/university/controller/ContentController.java)).
- Club directory for students: discover, join/leave, and follow timelines aggregated from each club's post feed ([ClubDirectoryController](src/main/java/lk/nsbm/university/controller/ClubDirectoryController.java)).
- Club president console for updating pitch decks and publishing rich posts ([ClubPresidentController](src/main/java/lk/nsbm/university/controller/ClubPresidentController.java)).

### Academic & Student Services
- Role-aware dashboards that surface announcements, featured content, approval counters, and semester resource stats ([DashboardController](src/main/java/lk/nsbm/university/controller/DashboardController.java)).
- Semester hubs with gated access, per-semester timetables, download-controlled resources, and batch insights ([SemesterPageController](src/main/java/lk/nsbm/university/controller/SemesterPageController.java)).
- Timetable CRUD with batch and semester linkage plus redirects from student views ([TimetableController](src/main/java/lk/nsbm/university/controller/TimetableController.java)).
- Student logistics landing pages for shuttles and boarding guardians, fusing curated content with approved personnel profiles ([StudentLogisticsController](src/main/java/lk/nsbm/university/controller/StudentLogisticsController.java)).

### Academic Structure Management
- Admin-facing screens for managing faculties/schools and the degrees they offer (e.g., BSc in Computing under Faculty of Computing).
- Student assignments now capture faculty + degree links; Admins can bulk-update these attributes alongside batch/semester mappings.
- Semester hubs filter announcements, resources, and service widgets so students only see items tagged for their faculty/degree, preventing cross-stream noise.

### Enhanced User Governance
- Search-as-you-type lookup inside the user governance view so admins can instantly jump to a student using a partial or full Student ID (e.g., `ICT/2023/001`).
- The search chip coexists with status/role filters, enabling compound queries such as "pending club presidents whose IDs start with CLB".

### Notification System
- Unified notification feed stored per user with read/unread state (announcements, approvals, semester resource drops, shuttle updates).
- Admins and moderators can trigger targeted notifications (per role, faculty, or degree) while the system auto-publishes high-signal events like account approvals.
- Dashboard surfaces the latest unread notifications and links through to a dedicated `/notifications` view for history and preference toggles.

### Data Lifecycle & Safeguards
- Automatic seeding of baseline semesters, privileged users, and showcase content ([DataInitializer](src/main/java/lk/nsbm/university/config/DataInitializer.java)).
- Uploaded assets stored under `uploads/` (media, backups, club content) with server-side validation.

## Default Credentials (Seeded via DataInitializer)
Use the following accounts immediately after provisioning:

| Role | Student ID | Password | Notes |
| ---- | ---------- | -------- | ----- |
| Super Admin | ADMIN01 | Admin@123 | Tops hierarchy, can promote/demote admins and run backups. |
| Moderator | MOD01 | Mod@123 | Handles general student approvals. |
| Content Manager | CONTENT01 | Content@123 | Owns content categories and seeded service listings. |
| Editor | EDITOR01 | Editor@123 | Editorial desk identity with contact info prefilled. |

> Password rotation is recommended after first login. Additional roles (club presidents, guardians, shuttle drivers, etc.) are created through the admin workspace or registration flows.

## Running the Application Locally
1. **Prerequisites**: Java 17+, Maven 3.9+, and a running MySQL instance (or rely on the bundled H2 profile for lightweight experimentation).
2. **Configure datasource**: Copy `src/main/resources/application.properties` to a local override (for example, `application-local.properties`) and adjust the JDBC URL, username, and password. Switch between MySQL and H2 by activating the relevant Spring profile.
3. **Build & test**:
   ```bash
   mvn clean verify
   ```
4. **Run**:
   ```bash
   mvn spring-boot:run
   ```
   The server boots on `http://localhost:8080/`. Visiting `/login` redirects you to the dashboard once authenticated.
5. **Media directories**: Ensure `uploads/media` and `uploads/backups` are writable so avatar uploads, club covers, and Excel exports succeed.

## Operational Notes
- **Backups**: Admins can export users to timestamped Excel in `uploads/backups`. Restores enforce path whitelisting to avoid directory traversal.
- **Reactions & analytics**: Content reactions are aggregated per user and surfaced in listing/detail views.
- **Club feeds**: Club posts are paginated; students see personalized feeds derived from the clubs they joined.
- **Semester access control**: Non-admin users are limited to their assigned semester hub, ensuring learning materials stay scoped.

## Suggested Enhancements
1. **Faculty & Degree Catalog**
   - Model faculties, schools, and degree programs, link them to batches/semesters, and expose dedicated discovery pages.
   - Extend registration forms to capture intended faculty, enabling dashboards filtered by academic stream.
2. **Faculty Dashboards & Insights**
   - Provide deans with approval stats, club participation, and service usage filtered to their faculty.
3. **Curriculum Planner**
   - Introduce GPA trackers, course enrollment intents, and integration with the timetable module for conflict detection.
4. **Notification Center**
   - Deliver push/email/WebSocket alerts for approvals, new resources, and shuttle changes.

These upgrades layer naturally on top of existing controllers and entities, keeping the current Spring MVC architecture intact while broadening academic coverage.
