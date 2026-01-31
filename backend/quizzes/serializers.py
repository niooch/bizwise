from rest_framework import serializers

from .models import Quiz, Question, AnswerOption


class QuizListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Quiz
        fields = ["id", "name", "exp_weight"]


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


class AnswerOptionCorrectSerializer(serializers.ModelSerializer):
    class Meta:
        model = AnswerOption
        fields = ["id", "content"]


class QuestionAnswerKeySerializer(serializers.ModelSerializer):
    correct_answer_options = serializers.SerializerMethodField()
    correct_numeric_pattern = serializers.SerializerMethodField()

    class Meta:
        model = Question
        fields = [
            "id",
            "question_type",
            "content",
            "correct_answer_options",
            "correct_numeric_pattern",
        ]

    def get_correct_answer_options(self, obj):
        if obj.question_type == "CLOSED":
            options = obj.answer_options.filter(is_correct=True)
            return AnswerOptionCorrectSerializer(options, many=True).data
        return []

    def get_correct_numeric_pattern(self, obj):
        if obj.question_type == "OPEN" and hasattr(obj, "answer_pattern") and obj.answer_pattern:
            return obj.answer_pattern.pattern
        return None


class QuizAnswerKeySerializer(serializers.ModelSerializer):
    questions = QuestionAnswerKeySerializer(many=True)

    class Meta:
        model = Quiz
        fields = ["id", "name", "questions"]
