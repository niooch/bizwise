from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError as DjangoValidationError

from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

from courses.models import UserProgress, Course
from quizzes.models import QuizResult
from .models import Avatar, UserProfile, UserStreak, Badge, UserBadge
from .utils import calculate_current_streak


def _build_image_url(obj, request):
    """
    Resolve an absolute URL for uploaded images or fallback to stored URL fields.
    """
    if getattr(obj, "image", None):
        url = obj.image.url
        if request:
            return request.build_absolute_uri(url)
        return url
    if hasattr(obj, "image_url"):
        return obj.image_url or None
    return None

User = get_user_model()


class RegisterSerializer(serializers.Serializer):
    nickname = serializers.CharField(max_length=150)
    password = serializers.CharField(write_only=True)

    def validate_nickname(self, value):
        if User.objects.filter(username=value).exists():
            raise serializers.ValidationError("Nickname already taken.")
        return value

    def validate_password(self, value):
        try:
            validate_password(value)
        except DjangoValidationError as e:
            raise serializers.ValidationError(list(e.messages))
        return value

    def create(self, validated_data):
        nickname = validated_data["nickname"]
        password = validated_data["password"]

        user = User.objects.create_user(
            username=nickname,
            password=password,
        )
        # create related profile objects
        UserProfile.objects.create(user=user)
        # streak will be created on first login / activity

        return user


class MyTokenObtainPairSerializer(TokenObtainPairSerializer):
    """
    Custom wrapper kept for schema consistency.
    """

    username_field = User.USERNAME_FIELD  # still 'username' internally


class AvatarUpdateSerializer(serializers.Serializer):
    avatar_id = serializers.PrimaryKeyRelatedField(
        queryset=Avatar.objects.all(), source="avatar"
    )


class AvatarSerializer(serializers.ModelSerializer):
    image_url = serializers.SerializerMethodField()

    class Meta:
        model = Avatar
        fields = ["id", "name", "image_url"]

    def get_image_url(self, obj):
        request = self.context.get("request") if hasattr(self, "context") else None
        return _build_image_url(obj, request)


class BadgeSerializer(serializers.ModelSerializer):
    image_url = serializers.SerializerMethodField()

    class Meta:
        model = Badge
        fields = ["id", "name", "description", "image_url"]

    def get_image_url(self, obj):
        request = self.context.get("request") if hasattr(self, "context") else None
        return _build_image_url(obj, request)


class UserBadgeSerializer(serializers.ModelSerializer):
    id = serializers.IntegerField(source="badge.id", read_only=True)
    name = serializers.CharField(source="badge.name", read_only=True)
    description = serializers.CharField(source="badge.description", read_only=True)
    image_url = serializers.SerializerMethodField()

    class Meta:
        model = UserBadge
        fields = ["id", "name", "description", "image_url", "awarded_at"]

    def get_image_url(self, obj):
        request = self.context.get("request") if hasattr(self, "context") else None
        return _build_image_url(obj.badge, request)


class UserMeSerializer(serializers.ModelSerializer):
    avatar = serializers.SerializerMethodField()
    exp = serializers.SerializerMethodField()
    streak = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ["id", "username", "avatar", "exp", "streak"]
        extra_kwargs = {
            "username": {"read_only": True},  # this is the nickname in the PDF
        }

    def get_avatar(self, obj):
        profile = getattr(obj, "profile", None)
        if profile and profile.avatar:
            request = self.context.get("request") if hasattr(self, "context") else None
            return {
                "id": profile.avatar.id,
                "name": profile.avatar.name,
                "image_url": _build_image_url(profile.avatar, request),
            }
        return None

    def get_exp(self, obj):
        """
        Simple EXP calculation:
        sum((best_score / 100) * quiz.exp_weight) over all quiz results.
        PDF mentions exp based on course/quiz weights. :contentReference[oaicite:3]{index=3}
        """
        results = QuizResult.objects.select_related("quiz").filter(user=obj)
        total = 0
        for r in results:
            # best_score is a percent (0-100); normalize to match exp_gained calculation
            total += int(r.best_score / 100.0 * r.quiz.exp_weight)
        return total

    def get_streak(self, obj):
        streak = getattr(obj, "streak", None)
        if not streak:
            return {
                "current_streak": 0,
                "best_streak": 0,
                #quick fix for frontend
                "begin_date": "1970-01-01",
                "last_activity_date": "1970-01-01",
            }
        return {
            "current_streak": calculate_current_streak(streak),
            "best_streak": streak.best_streak,
            "begin_date": streak.begin_date,
            "last_activity_date": streak.last_activity_date,
        }


class UserProgressSerializer(serializers.Serializer):
    """
    For /api/users/me/progress – list of completed courses (and optionally lessons). :contentReference[oaicite:4]{index=4}
    """
    completed_courses = serializers.ListField(child=serializers.IntegerField())
    completed_lessons = serializers.ListField(child=serializers.IntegerField())
