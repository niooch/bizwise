from django.conf import settings
from django.db import models
from django.contrib.auth import get_user_model
from django.utils import timezone

User = get_user_model()


class Avatar(models.Model):
    """
    Dostepne awatary
    """
    name = models.CharField(max_length=50, unique=True)
    image_url = models.URLField(blank=True)
    image = models.ImageField(upload_to="avatars/", null=True, blank=True)

    def __str__(self) -> str:
        return self.name


class UserProfile(models.Model):
    """
    dodatkowe pola nie z automatu brane z auth
    """
    user = models.OneToOneField(
        User,
        on_delete=models.CASCADE,
        related_name="profile",
    )
    avatar = models.ForeignKey(
        Avatar,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="profiles",
    )

    def __str__(self) -> str:
        return f"Profile of {self.user.username}"


class UserStreak(models.Model):
    """
    """
    user = models.OneToOneField(
        User,
        on_delete=models.CASCADE,
        primary_key=True,
        related_name="streak",
    )
    best_streak = models.PositiveIntegerField(default=0)
    begin_date = models.DateField()
    last_activity_date = models.DateField()

    def __str__(self) -> str:
        return f"{self.user.username} – best streak {self.best_streak}"


class Badge(models.Model):
    """
    Odznaki zdobywane przez użytkowników.
    """
    name = models.CharField(max_length=100, unique=True)
    description = models.TextField(blank=True)
    image_url = models.URLField(blank=True)
    image = models.ImageField(upload_to="badges/", null=True, blank=True)

    def __str__(self) -> str:
        return self.name


class UserBadge(models.Model):
    """
    Przypisanie odznaki do użytkownika z datą przyznania.
    """
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="user_badges",
    )
    badge = models.ForeignKey(
        Badge,
        on_delete=models.CASCADE,
        related_name="user_badges",
    )
    awarded_at = models.DateTimeField(default=timezone.now)

    class Meta:
        unique_together = ("user", "badge")

    def __str__(self) -> str:
        return f"{self.user.username} – {self.badge.name}"
