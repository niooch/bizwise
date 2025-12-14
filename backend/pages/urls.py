# pages/urls.py
from django.urls import path
from .views import home_view, course_detail_view, lesson_detail_view

urlpatterns = [
    path("", home_view, name="home"),
    # URL: /course/1/ (gdzie 1 to ID kursu)
    path("course/<int:pk>/", course_detail_view, name="course_detail"),
    # URL: /lesson/5/ (gdzie 5 to ID lekcji)
    path("lesson/<int:pk>/", lesson_detail_view, name="lesson_detail"),
]