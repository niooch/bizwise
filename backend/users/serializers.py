from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError as DjangoValidationError

from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer

from courses.models import UserProgress, Course
from quizzes.models import QuizResult
from .models import Avatar, UserProfile, UserStreak

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
    class Meta:
        model = Avatar
        fields = ["id", "name", "image_url"]


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
            return {
                "id": profile.avatar.id,
                "name": profile.avatar.name,
                "image_url": profile.avatar.image_url,
            }
        return None

    def get_exp(self, obj):
        """
        Simple EXP calculation:
        sum(best_score * quiz.exp_weight) over all quiz results.
        PDF mentions exp based on course/quiz weights. :contentReference[oaicite:3]{index=3}
        """
        results = QuizResult.objects.select_related("quiz").filter(user=obj)
        total = 0
        for r in results:
            total += int(r.best_score * r.quiz.exp_weight)
        return total

    def get_streak(self, obj):
        streak = getattr(obj, "streak", None)
        if not streak:
            return {
                "best_streak": 0,
                #quick fix for frontend
                "begin_date": "1970-01-01",
                "last_activity_date": "1970-01-01",
            }
        return {
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
