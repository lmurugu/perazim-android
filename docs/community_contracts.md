# Perazim Mission Church — Community Upgrade Contract Specification

> **Version**: 1.0.0-PROD  
> **Target Scope**: 10–20 Member Closed Pilot (Embu Headquarters, Nairobi Fellowship, Meru Outreach)  
> **Branch**: `phase-B-community`  
> **Database Version**: Room SQLite Version 6 (Migration from Version 5)  
> **Server Engine**: Node.js / Express + PostgreSQL 15 Container (`perazim-test-db`)  
> **Timezone**: Africa/Nairobi (UTC+3)

---

## 1. ID Formats & Conventions

All entities utilize typed prefixes with cryptographically random or timestamped unique suffixes:
- `post_`: Community Post (`post_1700000000_abc123`)
- `comm_`: Post Comment (`comm_1700000000_xyz789`)
- `group_`: Community Group (`group_cell_embu`, `group_worship_team`)
- `conv_`: 1-to-1 Conversation (`conv_u1_u2` where IDs are lexicographically sorted)
- `msg_`: Direct Message (`msg_1700000000_m456`)
- `att_`: Media Attachment filename (`att_1700000000_p987.jpg`)
- `notif_`: In-App Notification (`notif_1700000000_n123`)
- `chal_`: Daily Challenge (`chal_2026-11-05`)

---

## 2. PostgreSQL Server Schema

### 2.1 Community Posts (`community_posts`)
```sql
CREATE TABLE IF NOT EXISTS community_posts (
    id VARCHAR(64) PRIMARY KEY,
    author_id VARCHAR(64) NOT NULL REFERENCES users(id),
    author_name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'GENERAL', -- 'GENERAL', 'PRAYER', 'PRAISE', 'EVENT'
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    media_url TEXT DEFAULT '',
    group_id VARCHAR(64) DEFAULT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_answered BOOLEAN NOT NULL DEFAULT FALSE,
    event_date BIGINT DEFAULT NULL,
    event_location VARCHAR(255) DEFAULT '',
    amen_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON community_posts (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_type ON community_posts (type);
CREATE INDEX IF NOT EXISTS idx_posts_group ON community_posts (group_id);
```

### 2.2 Post Reactions (`post_amens`)
Exactly 1 reaction type (`Amen`). 1 active Amen per member per post:
```sql
CREATE TABLE IF NOT EXISTS post_amens (
    post_id VARCHAR(64) NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (post_id, user_id)
);
```

### 2.3 Post Comments (`post_comments`)
Flat, non-nested comments:
```sql
CREATE TABLE IF NOT EXISTS post_comments (
    id VARCHAR(64) PRIMARY KEY,
    post_id VARCHAR(64) NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    author_id VARCHAR(64) NOT NULL REFERENCES users(id),
    author_name VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON post_comments (post_id, created_at ASC);
```

### 2.4 Owner-Managed Groups (`community_groups` & `group_memberships`)
```sql
CREATE TABLE IF NOT EXISTS community_groups (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    created_by VARCHAR(64) NOT NULL REFERENCES users(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS group_memberships (
    group_id VARCHAR(64) NOT NULL REFERENCES community_groups(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER', -- 'MEMBER', 'LEADER', 'ADMIN'
    joined_at BIGINT NOT NULL,
    PRIMARY KEY (group_id, user_id)
);
```

### 2.5 Direct Messages (`direct_messages`)
Strict 1-to-1 private messaging with status lifecycle (`PENDING` -> `SENT` -> `DELIVERED` -> `READ`):
```sql
CREATE TABLE IF NOT EXISTS direct_messages (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    sender_id VARCHAR(64) NOT NULL REFERENCES users(id),
    receiver_id VARCHAR(64) NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    media_url TEXT DEFAULT '',
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED',
    created_at BIGINT NOT NULL,
    delivered_at BIGINT DEFAULT NULL
);
CREATE INDEX IF NOT EXISTS idx_dm_conv ON direct_messages (conversation_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_dm_receiver ON direct_messages (receiver_id, delivery_status);
```

### 2.6 Event RSVPs (`event_rsvps`)
```sql
CREATE TABLE IF NOT EXISTS event_rsvps (
    post_id VARCHAR(64) NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'ATTENDING', -- 'ATTENDING', 'DECLINED'
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (post_id, user_id)
);
```

### 2.7 In-App Notifications (`in_app_notifications`)
```sql
CREATE TABLE IF NOT EXISTS in_app_notifications (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id VARCHAR(64) NOT NULL,
    actor_name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL, -- 'MESSAGE', 'COMMENT', 'GROUP', 'EVENT'
    target_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_notif_user ON in_app_notifications (user_id, created_at DESC);
```

### 2.8 Daily Bible Challenge (`daily_challenges` & `user_challenge_completions`)
```sql
CREATE TABLE IF NOT EXISTS daily_challenges (
    date VARCHAR(32) PRIMARY KEY, -- 'YYYY-MM-DD'
    riddle_id VARCHAR(64) NOT NULL,
    question TEXT NOT NULL,
    answer VARCHAR(255) NOT NULL,
    hint TEXT DEFAULT '',
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS user_challenge_completions (
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date VARCHAR(32) NOT NULL,
    completed_at BIGINT NOT NULL,
    PRIMARY KEY (user_id, date)
);
```

---

## 3. REST API Contract Endpoints

### 3.1 Feed & Posts (`/api/community/posts`)
- `GET /api/community/posts`: Query parameters `type`, `groupId`, `cursor`, `limit` (default 20).
- `POST /api/community/posts`: Create post (Authenticated). Body `{ id, type, title, content, mediaUrl, groupId, eventDate, eventLocation }`.
- `PUT /api/community/posts/:id`: Edit post (Author or Admin). Body `{ title, content, isAnswered }`.
- `DELETE /api/community/posts/:id`: Delete post (Author or Admin).
- `POST /api/community/posts/:id/amen`: Toggle/add Amen (Idempotent 1-Amen per member).
- `GET /api/community/posts/:id/comments`: Fetch comments for post.
- `POST /api/community/posts/:id/comments`: Add comment. Body `{ id, content }`.

### 3.2 Groups (`/api/community/groups`)
- `GET /api/community/groups`: List user's accessible groups.
- `GET /api/community/groups/:id/members`: List group members (Members only).
- `POST /api/community/groups`: Create group (Admin only).
- `POST /api/community/groups/:id/members`: Add member (Admin only).
- `DELETE /api/community/groups/:id/members/:userId`: Remove member (Admin only).

### 3.3 Private Messaging (`/api/community/messages`)
- `GET /api/community/conversations`: List conversations with last message, delivery status, and unread count.
- `GET /api/community/messages/:conversationId`: Message history with cursor pagination.
- `POST /api/community/messages`: Send direct message. Body `{ id, conversationId, receiverId, content, mediaUrl }`. Returns `{ status: 'DELIVERED', deliveredAt }`.
- `POST /api/community/messages/ack`: Acknowledge delivery/read. Body `{ messageIds: [...] }`.

### 3.4 Media Attachments (`/api/community/media`)
- `POST /api/community/media/upload`: Single compressed photo. Multipart form or Base64 JSON payload.
  - Server validation: ≤1MB, ≤1600px dimension, JPEG/PNG/WebP magic bytes, strips EXIF.
  - Returns `{ mediaUrl: '/uploads/att_...' }`.

### 3.5 Events & RSVPs (`/api/community/events`)
- `POST /api/community/posts/:id/rsvp`: Body `{ status: 'ATTENDING' | 'DECLINED' }`.
- `GET /api/community/posts/:id/rsvps`: List RSVPs and counts.

### 3.6 Daily Challenge (`/api/community/challenge`)
- `GET /api/community/challenge/today`: Retrieves today's challenge in Africa/Nairobi date.
- `POST /api/community/challenge/submit`: Body `{ answer }`. Validates answer, records completion if correct. Exactly once per day; no reward farming.

### 3.7 In-App Notifications (`/api/community/notifications`)
- `GET /api/community/notifications`: List unread and recent notifications.
- `POST /api/community/notifications/read`: Mark all or specific notifications as read.

### 3.8 Profile Update (`/api/auth/profile`)
- `PUT /api/auth/profile`: Update display name, bio, and avatarUrl.

---

## 4. Room Database Architecture (v5 -> v6)

### 4.1 Migration `MIGRATION_5_6`
Executed in `PerazimDatabase.java`:
- Create tables: `community_posts`, `post_comments`, `community_groups`, `group_memberships`, `event_rsvps`, `daily_challenges`, `challenge_completions`.
- Update `users`: Add column `bio TEXT DEFAULT ''`.
- Update `messages`: Add column `mediaUrl TEXT DEFAULT ''`.
