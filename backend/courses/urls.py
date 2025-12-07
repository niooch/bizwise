from django.urls import path

from .views import (
    CourseListView,
    CourseDetailView,
    LessonDetailView,
    LessonCompleteView,
)

urlpatterns = [
    path("", CourseListView.as_view(), name="courses-list"),
    path("<int:pk>/", CourseDetailView.as_view(), name="courses-detail"),
    path("lessons/<int:pk>/", LessonDetailView.as_view(), name="lessons-detail"),
    path(
        "lessons/<int:pk>/complete/",
        LessonCompleteView.as_view(),
        name="lessons-complete",
    ),
]

