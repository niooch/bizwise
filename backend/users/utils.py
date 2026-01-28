from datetime import timedelta
from typing import Optional
from django.utils import timezone

from .models import UserStreak
from .badges import award_for_streak


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

    current_streak = 1 if created else 0

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
            current_streak = 1
        else:
            # activity already logged today; compute current length
            current_streak = (today - streak.begin_date).days + 1
    else:
        current_streak = 1

    streak.save()
    # Award streak-related badges
    award_for_streak(user, current_streak)
    return streak


def calculate_current_streak(streak: Optional[UserStreak]) -> int:
    """
    Return the length of the current streak (0 if broken or missing).
    A streak continues only if the last activity was today or yesterday.
    """
    if not streak:
        return 0

    today = timezone.localdate()
    if streak.last_activity_date < today - timedelta(days=1):
        return 0

    length = (streak.last_activity_date - streak.begin_date).days + 1
    return max(length, 0)
