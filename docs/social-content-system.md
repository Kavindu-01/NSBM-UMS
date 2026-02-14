# Social Content System Blueprint

## 1. North Star Objectives
- Give every approved student, club lead, and service owner a living activity stream that feels current and conversational instead of a static bulletin.
- Keep all posting, reactions, and moderation controls anchored in the existing Spring MVC stack (Thymeleaf + role-based security) to minimize cognitive load for admins.
- Ensure Semester Hub remains the single source of truth for academic artefacts while the social layer handles community chatter, media drops, and pulse checks.

## 2. Experience Pillars
1. **Campus Feed** – personalized infinite scroll that blends:
   - Posts from the student’s semester, faculty, and subscribed clubs.
   - Signal-boosted notices from admins/moderators.
   - Media carousels for heavy visual posts.
2. **Profiles & Timelines** – each user and club gets a timeline tab showing authored posts, pinned updates, and engagement stats.
3. **Micro-communities** – club and service workspaces host filtered feeds plus member-only polls.
4. **Moderation Console** – queue-based triage for reported or flagged posts with audit history.

## 3. Functional Requirements
- **Posting**: rich-text editor (Markdown subset), multi-image/video uploads (max 4), attachment tagging (semester, faculty, club, service).
- **Audience Targeting**: public (logged-in), faculty, semester, batch, club members, or custom role cohorts.
- **Engagement**: emoji reactions (existing logic in [src/main/java/lk/nsbm/university/service/ContentReactionService.java](src/main/java/lk/nsbm/university/service/ContentReactionService.java)), threaded comments, @mentions with notification fan-out.
- **Moderation**: profanity/PII heuristics, manual flagging, quarantine state, escalation ladder.
- **Discovery**: hashtag search, trending topics, saved filters per user.
- **Notifications**: new post in followed space, replies, mentions, moderation outcomes.

## 4. Data Model Draft
| Entity | Purpose | Key Fields |
| --- | --- | --- |
| `SocialPost` | Canonical post record | `id`, `author_id`, `body`, `rendered_body`, `visibility`, `audience_scope`, `status`, `pin_expires_at`, `created_at`, `updated_at` |
| `PostMediaAsset` | Uploaded media per post | `id`, `post_id`, `storage_path`, `media_type`, `width`, `height`, `duration_ms`, `thumbnail_path` |
| `PostAudienceEdge` | Precomputed mapping between post and eligible audiences | `id`, `post_id`, `audience_type` (SEMESTER/FACULTY/ROLE/CLUB), `audience_id` |
| `PostReaction` | Emoji-style signal | `id`, `post_id`, `user_id`, `reaction_type`, `created_at` |
| `PostComment` | Threaded replies | `id`, `post_id`, `parent_comment_id`, `author_id`, `body`, `status`, `created_at` |
| `PostModerationEvent` | Audit log | `id`, `post_id`, `action`, `reason`, `performed_by`, `created_at` |
| `FeedCursor` | Infinite-scroll state | `id`, `user_id`, `cursor_token`, `filters` |

### Schema Notes
- Use soft deletes (`status=ARCHIVED`) to preserve moderation history.
- Rendered body stores sanitized HTML generated from Markdown to avoid runtime parsing during feed hydration.
- Post visibility logic ($V_{post}$) = base visibility enum + audience edges. Effective scope per user evaluated as $V_{user}(post) = V_{post} \cap audience(user)$.

## 5. Services & Controllers
| Layer | Responsibility | Highlights |
| --- | --- | --- |
| `SocialComposerController` | GET composer modal, POST new post, PATCH edit. | Reuses `MediaStorageService` for uploads; enforces rate limits. |
| `SocialFeedController` | `/social/feed`, `/social/profile/{id}`, `/social/club/{id}` | Accepts filters `?scope=SEMESTER&facultyId=...`. Returns paginated DTO w/ reaction and comment counts. |
| `SocialInteractionController` | Reaction + comment endpoints | Shares DTO contracts with `ContentReactionService` for consistency. |
| `SocialModerationController` | Queue + bulk actions | Only ADMIN/MODERATOR/CONTENT_MANAGER roles. |
| `FeedAggregationService` | Resolves eligible posts per user | Pull model initially (SQL via audience edges) with caching for hot cohorts. |
| `ComposerService` | Validates audience scope, persists posts/assets | Wraps `SocialPostRepository`, `PostMediaRepository`, `PostAudienceRepository`. |
| `ModerationService` | Heuristics + manual workflows | Integrates with `NotificationService` to ping authors. |

## 6. UI & Interaction Design
- **Templates** (new):
  - `templates/social/feed.html`: hero banner + filter chips (All, Faculty, Semester, Clubs). Masonry cards with media gallery, reaction bar, and inline comment drawer.
  - `templates/social/profile.html`: cover section reusing avatar gradient logic from [src/main/resources/templates/profile/index.html](src/main/resources/templates/profile/index.html); tabs for Posts, About, Media.
  - `templates/social/moderation.html`: Kanban columns (New, Investigating, Resolved) for reported posts.
- **Components**: `fragments/social/composer.html`, `fragments/social/post-card.html`, `fragments/social/comment-thread.html`.
- **Motion/Visual**: adopt Space Grotesk + accent neon gradients already used in Semester Hub. Introduce staggered fade-up on feed load and micro-interactions for reaction hover states.
- **Responsive Strategy**: feed columns collapse from 3→2→1 with pinned composer staying at top on mobile.

## 7. Security & Moderation
- Route gating via `SecurityConfig`: `/social/manage/**` (admins/moderators), `/social/feed/**` (authenticated users), `/social/profile/**` (owner or admin for private drafts).
- CSRF-protected forms for composer, comment, moderation actions.
- Rate limiting middleware (bucket by userId) to prevent spam (e.g., 5 posts / 5 minutes, 30 comments / 5 minutes).
- Automated checks: banned keywords list, image scanning hook (placeholder for future Azure Content Moderator integration).

## 8. Notifications & Activity Stream
- Reuse NotificationService to push events:
  - New comment on your post → `SOCIAL_COMMENT`
  - Mention → `SOCIAL_MENTION`
  - Post flagged & approved/rejected → `SOCIAL_MODERATION`
- Dashboard widgets (user/guardian/shuttle) pull top 3 social posts relevant to role, replacing the old announcements list.

## 9. Delivery Plan
1. **Iteration 1 – Foundations**
   - Create entities/repositories, migrations, and composer service.
   - Stub feed controller returning static DTOs for UI scaffolding.
2. **Iteration 2 – Feed & Interactions**
   - Build feed queries, reaction/comment endpoints, notification hooks.
   - Implement front-end components with progressive enhancement.
3. **Iteration 3 – Moderation & Analytics**
   - Add reporting, moderation console, trend metrics, and admin dashboards.
4. **Iteration 4 – Enhancements**
   - Hashtag search, saved filters, WebSocket fan-out for real-time updates.

## 10. Migration & Backfill
- Seed pilot posts from existing `Content` entries (top 10) to prevent empty state.
- Map announcement history into pinned social posts per semester, ensuring continuity post-announcement retirement.
- Provide toggle in admin dashboard to switch from “Announcements” widget to “Social Pulse” once content volume crosses threshold.

## 11. Open Questions
- Should guardian and shuttle roles have posting rights or read-only feeds?
- Long-term media storage: continue on local filesystem or move to Azure Blob/S3 for scale?
- Need for GraphQL endpoint if mobile app consumes the same social feed?

---
This blueprint keeps the social surface tightly integrated with current services while paving a path toward richer, real-time community experiences without fragmenting tech stacks.
