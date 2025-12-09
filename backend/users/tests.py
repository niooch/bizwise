from django.contrib.auth import get_user_model
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

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
