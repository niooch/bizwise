from rest_framework import serializers

from .models import Quiz, Question, AnswerOption


class AnswerOptionPublicSerializer(serializers.ModelSerializer):
    class Meta:
        model = AnswerOption
        fields = ["id", "content"]  # no is_correct here


class QuestionPublicSerializer(serializers.ModelSerializer):
    answer_options = serializers.SerializerMethodField()

    class Meta:
        model = Question
        fields = ["id", "question_type", "content", "answer_options"]

    def get_answer_options(self, obj):
        # Only for closed questions, open questions have none
        if obj.question_type == "CLOSED":
            options = obj.answer_options.all()
            return AnswerOptionPublicSerializer(options, many=True).data
        return []


class QuizDetailSerializer(serializers.ModelSerializer):
    questions = QuestionPublicSerializer(many=True)

    class Meta:
        model = Quiz
        fields = ["id", "name", "questions"]


class QuizAnswerSerializer(serializers.Serializer):
    question_id = serializers.IntegerField()
    selected_option_id = serializers.IntegerField(required=False)
    numeric_answer = serializers.FloatField(required=False)

    def validate(self, attrs):
        if "selected_option_id" not in attrs and "numeric_answer" not in attrs:
            raise serializers.ValidationError(
                "Either selected_option_id or numeric_answer must be provided."
            )
        return attrs


class QuizSubmitSerializer(serializers.Serializer):
    answers = QuizAnswerSerializer(many=True)

