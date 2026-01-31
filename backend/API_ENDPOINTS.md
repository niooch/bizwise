# Bizwise API endpoints

Base URL: `/api/` (JWT auth required unless noted). JWTs are issued via SimpleJWT and passed as `Authorization: Bearer <access>`.

## Auth & user (`/api/auth/…`)
- `POST /auth/register/` — create account. Body: `{ "nickname": "...", "password": "..." }`. Returns created user `{id, nickname}` plus `access`/`refresh` tokens.
- `POST /auth/login/` — obtain JWT tokens. Body: `{ "username": "...", "password": "..." }` (use the nickname chosen at registration as `username`). Returns `{ "access": "...", "refresh": "..." }`.
- `POST /auth/logout/` — blacklist a refresh token. Body: `{ "refresh": "..." }`.
- `GET /auth/avatars/` — list available avatars (PNG). Returns `[{ id, name, image_url }]`.
- `GET /auth/me/` — current user profile. Returns `{ id, username, avatar, exp, streak }` where `avatar` is either `null` or `{ id, name, image_url }`; `streak` is `{ current_streak, best_streak, begin_date, last_activity_date }` (placeholder dates when streak not started yet). `exp` is the sum of `(best_score / 100) * quiz.exp_weight` across the user's quiz results.
- `GET /auth/me/streak/best/` — best streak summary for the current user. Returns `{ best_streak, begin_date, last_activity_date }` (placeholder dates when missing).
- `GET /auth/me/streak/current/` — current streak length with dates. Returns `{ current_streak, begin_date, last_activity_date }` (placeholder dates when missing).
- `GET /auth/me/badges/` — list badges earned by the current user. Returns `[{ id, name, description, image_url, awarded_at }]`.
- `GET /auth/me/progress/` — completion summary. Returns `{ completed_courses: [course_id], completed_lessons: [] }` (lessons placeholder for future expansion).
- `PATCH /auth/me/avatar/` — choose avatar. Body: `{ "avatar_id": <id> }`. Returns `{ "status": "ok" }`.

## Courses & lessons (`/api/courses/…`)
- `GET /courses/` — list courses. Query: `?search=<text>` (by name), `?ordering=name|-name`. Returns `[{ id, name }]`.
- `GET /courses/<course_id>/` — course detail with lesson list (no slide content). Returns `{ id, name, lessons: [{ id, name, order, locked, completed }] }`. `locked` toggles based on previous lesson completion.
- `GET /courses/lessons/<lesson_id>/` — lesson detail. Returns `{ id, name, slides: [{ id, order, text_content, image_url }], quiz_id }` (`quiz_id` may be `null`).
- `POST /courses/lessons/<lesson_id>/complete/` — mark lesson completed for current user and, when all lessons in a course are finished, record course completion (awards badge “Pierwszy Kurs” on first completed course). Optional body flag `{"completed_fast": true}` can be sent to award “Sprinter”. Returns `{ "status": "ok" }`.

## Quizzes (`/api/quizzes/…`)
- `GET /quizzes/` — list all quizzes. Returns `[{ id, name, exp_weight }]`.
- `GET /quizzes/<quiz_id>/` — quiz detail without answers. Returns `{ id, name, questions: [{ id, question_type, content, answer_options }] }`; `answer_options` contain `{ id, content }` for closed questions, empty list for open/numeric.
- `GET /quizzes/<quiz_id>/answers/` — quiz answer key. Returns `{ id, name, questions: [{ id, question_type, content, correct_answer_options, correct_numeric_pattern }] }`; `correct_answer_options` contains only correct options `{ id, content }` for closed questions, and `correct_numeric_pattern` is a string for open/numeric questions (otherwise `null`).
- `POST /quizzes/<quiz_id>/submit/` — submit answers and score. Body: `{ "answers": [{ "question_id": 1, "selected_option_id": 10 }, { "question_id": 2, "numeric_answer": 1995 }] }` (each answer must include either `selected_option_id` or `numeric_answer`). Response: `{ score, exp_gained, questions_total, correct_answers }`. Also updates stored best score and user streak.

## Forum (`/api/forum/…`)
- `GET /forum/posts/` — list posts ordered by creation. Query: `?tag=<tag_id>` filter; `?search=<text>` across title/content. Returns `[{ id, title, content, creation_date, author_nickname, tags, comments_count, reactions_count }]`.
- `POST /forum/posts/` — create post. Body: `{ "title": "...", "content": "...", "tag_ids": [<id>, ...]? }`. Returns created post fields from the request schema.
- `GET /forum/posts/<post_id>/` — post detail with comment tree. Returns `{ id, title, content, creation_date, author_nickname, tags, comments }` where each comment is `{ id, author_nickname, content, creation_date, replies: [...] }`.
- `PUT /forum/posts/<post_id>/` — update post (author only). Body matches create payload. Returns updated fields.
- `DELETE /forum/posts/<post_id>/` — delete post (author only). No body/response content.
- `POST /forum/posts/<post_id>/comments/` — add comment to post. Body: `{ "content": "...", "parent_comment_id": <id>? }`. Returns created comment tree node.
- `DELETE /forum/comments/<comment_id>/` — delete own comment. No body; returns 204.
- `POST /forum/posts/<post_id>/react/` — toggle reaction for a post. Body: `{ "reaction_type": "LIKE" | "UPVOTE" | "LAUGH" | "SAD" | "ANGRY" }`. Returns `{ "status": "added" | "removed" }`.
- `POST /forum/comments/<comment_id>/react/` — toggle reaction for a comment. Same body/response as post reaction.
- `GET /forum/tags/` — list all tags. Returns `[{ id, name }]`.

## API schema & docs
- `GET /api/schema/` — raw OpenAPI schema (Spectacular).
- `GET /api/docs/` — Swagger UI for the schema (public).
- `GET /api/redoc/` — ReDoc UI for the schema (public).
