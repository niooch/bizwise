from datetime import timedelta

from django.contrib.auth import get_user_model
from django.test import override_settings
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase
from users.badges import (
    award_for_quiz_score,
    award_for_course_completion,
    award_for_comment,
    award_for_streak,
    BADGE_QUIZ_MASTER,
    BADGE_FIRST_COURSE,
    BADGE_HELPING_HAND,
    BADGE_STREAK_7,
    BADGE_LOYAL_USER,
)

User = get_user_model()


@override_settings(
    PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"],
)
class AuthLoginTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="alice", password="secret12345"
        )
        self.url = reverse("auth-login")

    def test_login_with_username_and_password(self):
        payload = {"username": "alice", "password": "secret12345"}
        resp = self.client.post(self.url, payload, format="json")

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertIn("access", resp.data)
        self.assertIn("refresh", resp.data)

    def test_login_missing_username_returns_400(self):
        payload = {"password": "secret12345"}
        resp = self.client.post(self.url, payload, format="json")

        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("username", resp.data)


@override_settings(
    PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"],
)
class UserEndpointsTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="bob", password="secret12345"
        )
        self.client.force_authenticate(self.user)

    def test_register_returns_tokens_and_creates_user(self):
        url = reverse("auth-register")
        payload = {"nickname": "newuser", "password": "StrongPass123!"}

        resp = self.client.post(url, payload, format="json")

        self.assertEqual(resp.status_code, status.HTTP_201_CREATED)
        self.assertIn("access", resp.data)
        self.assertIn("refresh", resp.data)
        self.assertTrue(User.objects.filter(username="newuser").exists())

    def test_me_returns_basic_profile_fields(self):
        url = reverse("users-me")
        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data["username"], self.user.username)
        self.assertIn("avatar", resp.data)
        self.assertIn("exp", resp.data)
        self.assertIn("streak", resp.data)

    def test_list_avatars(self):
        from users.models import Avatar

        Avatar.objects.create(name="Hero", image_url="http://img")
        url = reverse("auth-avatars")

        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertGreaterEqual(len(resp.data), 1)

    def test_me_progress_lists_completed_courses(self):
        from courses.models import Course, UserProgress
        course = Course.objects.create(name="Economics 101")
        UserProgress.objects.create(
            user=self.user, course=course, completion_date="2024-01-01"
        )

        url = reverse("users-me-progress")
        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertIn(course.id, resp.data["completed_courses"])

    def test_me_avatar_updates_user_profile(self):
        from users.models import Avatar, UserProfile

        avatar = Avatar.objects.create(name="Hero", image_url="http://img")
        UserProfile.objects.create(user=self.user)

        url = reverse("users-me-avatar")
        resp = self.client.patch(url, {"avatar_id": avatar.id}, format="json")

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.user.refresh_from_db()
        self.assertEqual(self.user.profile.avatar, avatar)

    def test_me_best_streak_returns_defaults_without_record(self):
        url = reverse("users-me-streak-best")

        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data["best_streak"], 0)

    def test_me_current_streak_calculates_length(self):
        from users.models import UserStreak

        today = timezone.localdate()
        UserStreak.objects.create(
            user=self.user,
            best_streak=5,
            begin_date=today - timedelta(days=2),
            last_activity_date=today,
        )

        url = reverse("users-me-streak-current")
        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data["current_streak"], 3)

    def test_me_badges_returns_user_badges(self):
        from users.models import Badge, UserBadge

        badge = Badge.objects.create(
            name="Fast Learner",
            description="Complete your first course quickly",
            image="badges/fast.png",
        )
        UserBadge.objects.create(user=self.user, badge=badge)

        url = reverse("users-me-badges")
        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertGreaterEqual(len(resp.data), 1)
        self.assertEqual(resp.data[0]["name"], badge.name)


@override_settings(
    PASSWORD_HASHERS=["django.contrib.auth.hashers.MD5PasswordHasher"],
)
class BadgeAwardTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="chris", password="secret12345"
        )

    def _ensure_badge(self, name):
        from users.models import Badge

        Badge.objects.get_or_create(name=name)

    def test_quiz_master_award(self):
        from users.models import UserBadge

        self._ensure_badge(BADGE_QUIZ_MASTER)
        award_for_quiz_score(self.user, 100.0)

        self.assertTrue(
            UserBadge.objects.filter(user=self.user, badge__name=BADGE_QUIZ_MASTER).exists()
        )

    def test_first_course_award(self):
        from courses.models import Course, UserProgress
        from users.models import UserBadge

        self._ensure_badge(BADGE_FIRST_COURSE)
        course = Course.objects.create(name="Course X")
        UserProgress.objects.create(user=self.user, course=course, completion_date=timezone.now())

        award_for_course_completion(self.user)

        self.assertTrue(
            UserBadge.objects.filter(user=self.user, badge__name=BADGE_FIRST_COURSE).exists()
        )

    def test_comment_award(self):
        from forum.models import Post, Comment
        from users.models import UserBadge

        self._ensure_badge(BADGE_HELPING_HAND)
        post = Post.objects.create(author=self.user, title="t", content="c")
        Comment.objects.create(post=post, author=self.user, content="hi")

        award_for_comment(self.user)

        self.assertTrue(
            UserBadge.objects.filter(user=self.user, badge__name=BADGE_HELPING_HAND).exists()
        )

    def test_streak_awards(self):
        from users.models import UserBadge

        self._ensure_badge(BADGE_STREAK_7)
        self._ensure_badge(BADGE_LOYAL_USER)

        award_for_streak(self.user, current_streak=14)

        self.assertTrue(
            UserBadge.objects.filter(user=self.user, badge__name=BADGE_STREAK_7).exists()
        )
        self.assertTrue(
            UserBadge.objects.filter(user=self.user, badge__name=BADGE_LOYAL_USER).exists()
        )
