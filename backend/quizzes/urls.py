from django.urls import path

from .views import (
    QuizDetailView,
    QuizSubmitView,
    QuizResultView,
    QuizLeaderboardView,
)

urlpatterns = [
    path("<int:pk>/", QuizDetailView.as_view(), name="quiz-detail"),
    path("<int:pk>/submit/", QuizSubmitView.as_view(), name="quiz-submit"),
    path("<int:pk>/results/", QuizResultView.as_view(), name="quiz-results"),
    path("<int:pk>/leaderboard/", QuizLeaderboardView.as_view(), name="quiz-leaderboard"),
]
