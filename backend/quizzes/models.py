from django.conf import settings
from django.db import models

User = settings.AUTH_USER_MODEL


class QuestionType(models.TextChoices):
    OPEN = "OPEN", "Open"
    CLOSED = "CLOSED", "Closed"


class Quiz(models.Model):
    name = models.CharField(max_length=255)
    exp_weight = models.PositiveIntegerField(default=0)

    def __str__(self) -> str:
        return self.name


class Question(models.Model):
    quiz = models.ForeignKey(
        Quiz,
        on_delete=models.CASCADE,
        related_name="questions",
    )
    question_type = models.CharField(
        max_length=10,
        choices=QuestionType.choices,
    )
    content = models.TextField()

    def __str__(self) -> str:
        return f"[{self.quiz}] {self.content[:50]}"


class AnswerPattern(models.Model):
    question = models.OneToOneField(
        Question,
        on_delete=models.CASCADE,
        related_name="answer_pattern",
    )
    pattern = models.CharField(
        max_length=255,
        help_text="e.g. '42', '10-20', '>= 1995', etc.",
    )

    def __str__(self) -> str:
        return f"Pattern for Q{self.question_id}: {self.pattern}"


class AnswerOption(models.Model):
    question = models.ForeignKey(
        Question,
        on_delete=models.CASCADE,
        related_name="answer_options",
    )
    content = models.CharField(max_length=255)
    is_correct = models.BooleanField(default=False)

    def __str__(self) -> str:
        return f"{self.content} ({'correct' if self.is_correct else 'wrong'})"


class QuizResult(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="quiz_results",
    )
    quiz = models.ForeignKey(
        Quiz,
        on_delete=models.CASCADE,
        related_name="results",
    )
    best_score = models.FloatField()
    last_completion_date = models.DateTimeField()

    class Meta:
        unique_together = ("user", "quiz")

    def __str__(self) -> str:
        return f"{self.user} – {self.quiz}: {self.best_score}"
