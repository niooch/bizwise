from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase
from django.contrib.auth import get_user_model

from .models import Quiz, Question, AnswerOption, QuizResult, QuestionType

User = get_user_model()


class QuizEndpointsTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(username="quizzer", password="pass12345")
        self.client.force_authenticate(self.user)

    def _make_quiz_with_closed_question(self):
        quiz = Quiz.objects.create(name="Finance Quiz", exp_weight=50)
        question = Question.objects.create(
            quiz=quiz, question_type=QuestionType.CLOSED, content="2+2?"
        )
        wrong = AnswerOption.objects.create(question=question, content="3", is_correct=False)
        correct = AnswerOption.objects.create(question=question, content="4", is_correct=True)
        return quiz, question, correct, wrong

    def test_quiz_detail_hides_correct_flag(self):
        quiz, question, correct, _ = self._make_quiz_with_closed_question()
        url = reverse("quiz-detail", args=[quiz.id])

        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        opts = resp.data["questions"][0]["answer_options"]
        self.assertEqual(len(opts), 2)
        self.assertNotIn("is_correct", opts[0])

    def test_quiz_submit_scores_and_persists_result(self):
        quiz, question, correct, wrong = self._make_quiz_with_closed_question()
        url = reverse("quiz-submit", args=[quiz.id])

        payload = {"answers": [{"question_id": question.id, "selected_option_id": correct.id}]}
        resp = self.client.post(url, payload, format="json")

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data["score"], 100.0)
        self.assertEqual(resp.data["correct_answers"], 1)
        self.assertEqual(resp.data["exp_gained"], quiz.exp_weight)

        result = QuizResult.objects.get(user=self.user, quiz=quiz)
        self.assertEqual(result.best_score, 100.0)

    def test_quiz_leaderboard_and_results(self):
        quiz, question, correct, wrong = self._make_quiz_with_closed_question()
        submit_url = reverse("quiz-submit", args=[quiz.id])
        self.client.post(
            submit_url,
            {"answers": [{"question_id": question.id, "selected_option_id": correct.id}]},
            format="json",
        )

        leaderboard_url = reverse("quiz-leaderboard", args=[quiz.id])
        resp = self.client.get(leaderboard_url)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        leaderboard = resp.data["leaderboard"]
        self.assertEqual(len(leaderboard), 1)
        self.assertEqual(leaderboard[0]["best_score"], 100.0)
        self.assertEqual(leaderboard[0]["user_id"], self.user.id)
        self.assertEqual(leaderboard[0]["username"], self.user.username)

        # results endpoint
        results_url = reverse("quiz-results", args=[quiz.id])
        resp_res = self.client.get(results_url)
        self.assertEqual(resp_res.status_code, status.HTTP_200_OK)
        self.assertEqual(resp_res.data["best_score"], 100.0)
