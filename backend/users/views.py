from django.utils import timezone
from rest_framework import generics, permissions, status
from rest_framework.response import Response
from rest_framework.views import APIView

from rest_framework_simplejwt.views import TokenObtainPairView, TokenBlacklistView
from rest_framework_simplejwt.tokens import RefreshToken

from django.contrib.auth import get_user_model

from courses.models import UserProgress
from .models import UserProfile, Avatar, UserStreak
from .serializers import (
    RegisterSerializer,
    MyTokenObtainPairSerializer,
    AvatarUpdateSerializer,
    UserMeSerializer,
    UserProgressSerializer,
)

User = get_user_model()


# -----------------------
# Auth endpoints
# -----------------------

class RegisterView(generics.CreateAPIView):
    """
    POST /api/auth/register
    Body: { "nickname": "...", "password": "..." }
    """
    serializer_class = RegisterSerializer
    permission_classes = [permissions.AllowAny]

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()

        # Optionally auto-login by returning JWT here:
        refresh = RefreshToken.for_user(user)
        data = {
            "user": {
                "id": user.id,
                "nickname": user.username,
            },
            "access": str(refresh.access_token),
            "refresh": str(refresh),
        }
        return Response(data, status=status.HTTP_201_CREATED)


class LoginView(TokenObtainPairView):
    """
    POST /api/auth/login
    Body: { "nickname": "...", "password": "..." }
    Returns: { access, refresh }
    """
    permission_classes = [permissions.AllowAny]
    serializer_class = MyTokenObtainPairSerializer


class LogoutView(TokenBlacklistView):
    """
    POST /api/auth/logout
    Body: { "refresh": "..." }
    Unvalidates the refresh token using SimpleJWT blacklist. :contentReference[oaicite:5]{index=5}
    """
    # uses default serializer from TokenBlacklistView
    permission_classes = [permissions.IsAuthenticated]


# -----------------------
# User endpoints
# -----------------------

class MeView(generics.RetrieveAPIView):
    """
    GET /api/users/me
    Returns: avatar, nickname, exp, streak. :contentReference[oaicite:6]{index=6}
    """
    serializer_class = UserMeSerializer

    def get_object(self):
        return self.request.user


class MeProgressView(APIView):
    """
    GET /api/users/me/progress
    Returns: list of completed course IDs (and lessons list placeholder). :contentReference[oaicite:7]{index=7}
    """

    def get(self, request):
        user = request.user

        course_progress_qs = UserProgress.objects.filter(user=user).values_list(
            "course_id", flat=True
        )

        data = {
            "completed_courses": list(course_progress_qs),
            "completed_lessons": [],  # can be implemented later if you add LessonProgress
        }

        serializer = UserProgressSerializer(data=data)
        serializer.is_valid(raise_exception=True)
        return Response(serializer.data)


class MeAvatarView(APIView):
    """
    PATCH /api/users/me/avatar
    Body: { "avatar_id": <id> } :contentReference[oaicite:8]{index=8}
    """

    def patch(self, request):
        user = request.user
        profile, _ = UserProfile.objects.get_or_create(user=user)

        serializer = AvatarUpdateSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        profile.avatar = serializer.validated_data["avatar"]
        profile.save()

        return Response({"status": "ok"})

