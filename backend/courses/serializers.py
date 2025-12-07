from rest_framework import serializers

from .models import Course, Lesson, Slide, CourseLesson, LessonQuiz


class CourseListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Course
        fields = ["id", "name"]


class LessonInCourseSerializer(serializers.Serializer):
    id = serializers.IntegerField()
    name = serializers.CharField()
    order = serializers.IntegerField()
    locked = serializers.BooleanField()
    completed = serializers.BooleanField()


class CourseDetailSerializer(serializers.Serializer):
    id = serializers.IntegerField()
    name = serializers.CharField()
    lessons = LessonInCourseSerializer(many=True)


class SlideSerializer(serializers.ModelSerializer):
    class Meta:
        model = Slide
        fields = ["id", "order", "text_content", "image_url"]


class LessonDetailSerializer(serializers.Serializer):
    id = serializers.IntegerField()
    name = serializers.CharField()
    slides = SlideSerializer(many=True)
    quiz_id = serializers.IntegerField(allow_null=True)

