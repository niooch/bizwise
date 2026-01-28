from django.urls import path

from .views import (
    RegisterView,
    LoginView,
    LogoutView,
    RefreshView,
    MeView,
    MeProgressView,
    MeBestStreakView,
    MeCurrentStreakView,
    MeBadgesView,
    MeAvatarView,
    AvatarListView,
)

urlpatterns = [
    # Auth
    path("register/", RegisterView.as_view(), name="auth-register"),
    path("login/", LoginView.as_view(), name="auth-login"),
    path("logout/", LogoutView.as_view(), name="auth-logout"),
    path("refresh/", RefreshView.as_view(), name="auth-refresh"),
    path("avatars/", AvatarListView.as_view(), name="auth-avatars"),

    # User + gamification
    path("me/", MeView.as_view(), name="users-me"),
    path("me/progress/", MeProgressView.as_view(), name="users-me-progress"),
    path("me/streak/best/", MeBestStreakView.as_view(), name="users-me-streak-best"),
    path("me/streak/current/", MeCurrentStreakView.as_view(), name="users-me-streak-current"),
    path("me/badges/", MeBadgesView.as_view(), name="users-me-badges"),
    path("me/avatar/", MeAvatarView.as_view(), name="users-me-avatar"),
]
