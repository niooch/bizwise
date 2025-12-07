from datetime import timedelta
from django.utils import timezone

from .models import UserStreak


def update_user_streak(user):
    """
    Implements logic from spec for UserStreak. :contentReference[oaicite:10]{index=10}
    - If last_activity_date is yesterday -> continue streak
    - Else -> reset streak starting today
    - Always update best_streak if current streak is longer
    """
    today = timezone.localdate()
    streak, created = UserStreak.objects.get_or_create(
        user=user,
        defaults={
            "best_streak": 1,
            "begin_date": today,
            "last_activity_date": today,
        },
    )

    if not created:
        if streak.last_activity_date == today - timedelta(days=1):
            # continue streak
            current_streak = (today - streak.begin_date).days + 1
            streak.last_activity_date = today
            if current_streak > streak.best_streak:
                streak.best_streak = current_streak
        elif streak.last_activity_date != today:
            # reset streak
            streak.begin_date = today
            streak.last_activity_date = today
            if streak.best_streak < 1:
                streak.best_streak = 1

    streak.save()

