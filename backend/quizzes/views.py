from django.shortcuts import get_object_or_404
from django.utils import timezone

from rest_framework import permissions, status
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Quiz, Question, AnswerOption, QuizResult
from .serializers import QuizDetailSerializer, QuizSubmitSerializer
from users.utils import update_user_streak


def check_numeric_answer(pattern: str, value: float) -> bool:
    """
    Very simple numeric pattern parser based on spec examples. :contentReference[oaicite:11]{index=11}
    - "42" -> exact match
    - "10-20" -> inclusive range
    You can extend this later (>=, <=, etc.).
    """
    pattern = pattern.strip()
    if "-" in pattern:
        try:
            lo, hi = pattern.split("-", 1)
            lo = float(lo.strip())
            hi = float(hi.strip())
            return lo <= value <= hi
        except ValueError:
            return False
    else:
        try:
            target = float(pattern)
            return value == target
        except ValueError:
            return False


class QuizDetailView(APIView):
    """
    GET /api/quizzes/{id}
    Returns questions + options WITHOUT is_correct/pattern fields. :contentReference[oaicite:12]{index=12}
    """
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request, pk: int):
        quiz = get_object_or_404(
            Quiz.objects.prefetch_related("questions__answer_options"),
            pk=pk,
        )
        serializer = QuizDetailSerializer(quiz)
        return Response(serializer.data, status=status.HTTP_200_OK)


class QuizSubmitView(APIView):
    """
    POST /api/quizzes/{id}/submit
    Body:
    {
      "answers": [
        { "question_id": 1, "selected_option_id": 10 },
        { "question_id": 2, "numeric_answer": 1995 }
      ]
    }
    Returns: { score, exp_gained } (score 0–100). 
    """
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, pk: int):
        quiz = get_object_or_404(
            Quiz.objects.prefetch_related("questions__answer_options", "questions__answer_pattern"),
            pk=pk,
        )

        serializer = QuizSubmitSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        answers = serializer.validated_data["answers"]

        # Build map of quiz questions
        questions = {q.id: q for q in quiz.questions.all()}
        total_questions = len(questions)
        correct = 0

        for ans in answers:
            qid = ans["question_id"]
            question = questions.get(qid)
            if not question:
                continue  # ignore answers for non-existing/non-quiz questions

            if question.question_type == "CLOSED":
                option_id = ans.get("selected_option_id")
                if not option_id:
                    continue
                try:
                    option = question.answer_options.get(id=option_id)
                except AnswerOption.DoesNotExist:
                    continue
                if option.is_correct:
                    correct += 1
            else:  # OPEN numeric
                numeric = ans.get("numeric_answer", None)
                if numeric is None:
                    continue
                if hasattr(question, "answer_pattern") and question.answer_pattern:
                    if check_numeric_answer(question.answer_pattern.pattern, numeric):
                        correct += 1

        score = 0.0
        if total_questions > 0:
            score = (correct / total_questions) * 100.0

        # Update QuizResult (best_score + last_completion_date)
        now = timezone.now()
        result, created = QuizResult.objects.get_or_create(
            user=request.user,
            quiz=quiz,
            defaults={"best_score": score, "last_completion_date": now},
        )
        if not created and score > result.best_score:
            result.best_score = score
            result.last_completion_date = now
            result.save()

        # Update streak
        update_user_streak(request.user)

        # Calculate exp gained for this attempt
        exp_gained = int(score / 100.0 * quiz.exp_weight)

        return Response(
            {
                "score": score,
                "exp_gained": exp_gained,
                "questions_total": total_questions,
                "correct_answers": correct,
            },
            status=status.HTTP_200_OK,
        )
