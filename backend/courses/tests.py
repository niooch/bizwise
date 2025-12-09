from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase
from django.contrib.auth import get_user_model

from .models import Course, Lesson, CourseLesson, LessonProgress, UserProgress, Slide
from quizzes.models import Quiz

User = get_user_model()


class CourseEndpointsTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(username="learner", password="pass12345")
        self.client.force_authenticate(self.user)

    def _make_course_with_lessons(self, lessons_count=2):
        course = Course.objects.create(name="Business Basics")
        lessons = []
        for i in range(lessons_count):
            lesson = Lesson.objects.create(name=f"Lesson {i+1}")
            CourseLesson.objects.create(course=course, lesson=lesson, order=i)
            lessons.append(lesson)
        return course, lessons

    def test_course_detail_marks_only_first_lesson_unlocked_initially(self):
        course, lessons = self._make_course_with_lessons()

        url = reverse("courses-detail", args=[course.id])
        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertFalse(resp.data["lessons"][0]["locked"])
        self.assertTrue(resp.data["lessons"][1]["locked"])

    def test_lesson_complete_creates_progress_and_course_completion(self):
        course, lessons = self._make_course_with_lessons(lessons_count=2)

        first_url = reverse("lessons-complete", args=[lessons[0].id])
        second_url = reverse("lessons-complete", args=[lessons[1].id])

        resp1 = self.client.post(first_url)
        self.assertEqual(resp1.status_code, status.HTTP_200_OK)
        self.assertTrue(
            LessonProgress.objects.filter(user=self.user, lesson=lessons[0]).exists()
        )
        self.assertFalse(
            UserProgress.objects.filter(user=self.user, course=course).exists()
        )

        resp2 = self.client.post(second_url)
        self.assertEqual(resp2.status_code, status.HTTP_200_OK)
        self.assertTrue(
            UserProgress.objects.filter(user=self.user, course=course).exists()
        )

    def test_lesson_detail_returns_slides_and_quiz_id(self):
        course, lessons = self._make_course_with_lessons(lessons_count=1)
        lesson = lessons[0]
        Slide.objects.create(lesson=lesson, order=0, text_content="Hello", image_url="")
        quiz = Quiz.objects.create(name="Quiz 1", exp_weight=10)
        from .models import LessonQuiz

        LessonQuiz.objects.create(lesson=lesson, quiz=quiz)

        url = reverse("lessons-detail", args=[lesson.id])
        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data["quiz_id"], quiz.id)
        self.assertEqual(len(resp.data["slides"]), 1)
