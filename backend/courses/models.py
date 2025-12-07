from django.conf import settings
from django.db import models

User = settings.AUTH_USER_MODEL


class Course(models.Model):
    name = models.CharField(max_length=255)

    def __str__(self) -> str:
        return self.name


class Lesson(models.Model):
    name = models.CharField(max_length=255)

    def __str__(self) -> str:
        return self.name


class Slide(models.Model):
    lesson = models.ForeignKey(
        Lesson,
        on_delete=models.CASCADE,
        related_name="slides",
    )
    text_content = models.TextField()
    order = models.PositiveIntegerField()
    image_url = models.URLField(blank=True, null=True)

    class Meta:
        ordering = ["lesson", "order"]
        unique_together = ("lesson", "order")

    def __str__(self) -> str:
        return f"{self.lesson.name} – slide #{self.order}"


class UserProgress(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="course_progress",
    )
    course = models.ForeignKey(
        Course,
        on_delete=models.CASCADE,
        related_name="user_progress",
    )
    completion_date = models.DateTimeField()

    class Meta:
        unique_together = ("user", "course")

    def __str__(self) -> str:
        return f"{self.user} completed {self.course} at {self.completion_date}"


class CourseLesson(models.Model):
    course = models.ForeignKey(
        Course,
        on_delete=models.CASCADE,
        related_name="course_lessons",
    )
    lesson = models.ForeignKey(
        Lesson,
        on_delete=models.CASCADE,
        related_name="course_lessons",
    )
    order = models.PositiveIntegerField(default=0)

    class Meta:
        unique_together = ("course", "lesson")
        ordering = ["course", "order"]

    def __str__(self) -> str:
        return f"{self.course} – {self.lesson} (#{self.order})"


class LessonQuiz(models.Model):
    lesson = models.ForeignKey(
        Lesson,
        on_delete=models.CASCADE,
        related_name="lesson_quizzes",
    )
    quiz = models.ForeignKey(
        "quizzes.Quiz",
        on_delete=models.CASCADE,
        related_name="lesson_links",
    )

    class Meta:
        unique_together = ("lesson", "quiz")

    def __str__(self) -> str:
        return f"{self.lesson} – {self.quiz}"
