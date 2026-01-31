from django.urls import path

from .views import (
    QuizListView,
    QuizDetailView,
    QuizAnswerKeyView,
    QuizSubmitView,
    QuizResultView,
    QuizLeaderboardView,
)

urlpatterns = [
    path("", QuizListView.as_view(), name="quiz-list"),
    path("<int:pk>/", QuizDetailView.as_view(), name="quiz-detail"),
    path("<int:pk>/answers/", QuizAnswerKeyView.as_view(), name="quiz-answer-key"),
    path("<int:pk>/submit/", QuizSubmitView.as_view(), name="quiz-submit"),
    path("<int:pk>/results/", QuizResultView.as_view(), name="quiz-results"),
    path("<int:pk>/leaderboard/", QuizLeaderboardView.as_view(), name="quiz-leaderboard"),
]
