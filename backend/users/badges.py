from typing import Optional

from courses.models import UserProgress
from forum.models import Comment
from .models import Badge, UserBadge


# Badge names used in seeding and logic
BADGE_FIRST_COURSE = "Pierwszy Kurs"
BADGE_QUIZ_MASTER = "Quizowy As"
BADGE_STREAK_7 = "Siedem Dni Ciągiem"
BADGE_STREAK_30 = "Miesiąc Regularności"
BADGE_SPRINTER = "Sprinter"
BADGE_HELPING_HAND = "Pomocna Dłoń"
BADGE_COLLECTOR = "Kolekcjoner"
BADGE_LOYAL_USER = "Wierny Użytkownik"


def _get_badge_by_name(name: str) -> Optional[Badge]:
    try:
        return Badge.objects.get(name=name)
    except Badge.DoesNotExist:
        return None


def award_badge(user, badge_name: str) -> bool:
    """
    Create UserBadge if it does not exist. Returns True when newly awarded.
    """
    badge = _get_badge_by_name(badge_name)
    if not badge:
        return False
    _, created = UserBadge.objects.get_or_create(user=user, badge=badge)
    return created


def _award_collector_if_needed(user):
    """
    Award collector badge after earning at least 5 badges.
    """
    badge = _get_badge_by_name(BADGE_COLLECTOR)
    if not badge:
        return
    already_has = UserBadge.objects.filter(user=user, badge=badge).exists()
    if already_has:
        return
    total = UserBadge.objects.filter(user=user).count()
    if total >= 5:
        UserBadge.objects.create(user=user, badge=badge)


def award_for_course_completion(user):
    """
    Award 'Pierwszy Kurs' when the user completes their first course.
    """
    courses_completed = UserProgress.objects.filter(user=user).count()
    if courses_completed == 1:
        award_badge(user, BADGE_FIRST_COURSE)
        _award_collector_if_needed(user)


def award_for_quiz_score(user, score: float):
    """
    Award 'Quizowy As' for a perfect score.
    """
    if score >= 100.0:
        if award_badge(user, BADGE_QUIZ_MASTER):
            _award_collector_if_needed(user)


def award_for_streak(user, current_streak: int):
    """
    Award streak-based badges.
    """
    changed = False
    if current_streak >= 30:
        changed = award_badge(user, BADGE_STREAK_30) or changed
    if current_streak >= 7:
        changed = award_badge(user, BADGE_STREAK_7) or changed
    if current_streak >= 14:
        changed = award_badge(user, BADGE_LOYAL_USER) or changed
    if changed:
        _award_collector_if_needed(user)


def award_for_comment(user):
    """
    Award 'Pomocna Dłoń' on the first posted comment.
    """
    comments_count = Comment.objects.filter(author=user).count()
    if comments_count == 1:
        if award_badge(user, BADGE_HELPING_HAND):
            _award_collector_if_needed(user)


def award_sprinter(user):
    """
    Award 'Sprinter' (call this when client/server determines fast completion).
    """
    if award_badge(user, BADGE_SPRINTER):
        _award_collector_if_needed(user)
