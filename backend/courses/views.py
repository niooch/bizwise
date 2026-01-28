from django.shortcuts import get_object_or_404
from django.utils import timezone

from rest_framework import generics, permissions, status, filters, serializers
from rest_framework.response import Response
from rest_framework.views import APIView
from drf_spectacular.utils import (
    extend_schema,
    OpenApiParameter,
    inline_serializer,
)
from users.badges import award_for_course_completion, award_sprinter

from .models import (
    Course,
    Lesson,
    Slide,
    CourseLesson,
    LessonQuiz,
    LessonProgress,
    UserProgress,
)


from .serializers import (
    CourseListSerializer,
    CourseDetailSerializer,
    LessonDetailSerializer,
)


@extend_schema(
    tags=["courses"],
    parameters=[
        OpenApiParameter(
            name="search",
            description="Search courses by name",
            required=False,
            type=str,
            location=OpenApiParameter.QUERY,
        ),
        OpenApiParameter(
            name="ordering",
            description="Order by name (e.g. name or -name)",
            required=False,
            type=str,
            location=OpenApiParameter.QUERY,
        ),
    ],
    responses=CourseListSerializer,
)
class CourseListView(generics.ListAPIView):
    """
    GET /api/courses
    Supports simple search & ordering via query params:
    - ?search=<text>  (by name)
    - ?ordering=name or -name
    """
    queryset = Course.objects.all()
    serializer_class = CourseListSerializer
    permission_classes = [permissions.IsAuthenticated]
    filter_backends = [filters.SearchFilter, filters.OrderingFilter]
    search_fields = ["name"]
    ordering_fields = ["name"]
    ordering = ["name"]


@extend_schema(
    tags=["courses"],
    responses=CourseDetailSerializer,
)
class CourseDetailView(APIView):
    """
    GET /api/courses/{id}
    Returns: course with list of lessons (title, order, lock/completed status),
    WITHOUT slide content. :contentReference[oaicite:5]{index=5}
    """

    permission_classes = [permissions.IsAuthenticated]

    def get(self, request, pk: int):
        course = get_object_or_404(Course, pk=pk)

        course_lessons = (
            CourseLesson.objects
            .select_related("lesson")
            .filter(course=course)
            .order_by("order")
        )

        from .models import LessonProgress
        completed_ids = set(
            LessonProgress.objects.filter(
                user=request.user, lesson__in=[cl.lesson for cl in course_lessons]
            ).values_list("lesson_id", flat=True)
        )

        lessons_payload = []
        prev_completed = True  # first lesson always unlocked

        for idx, cl in enumerate(course_lessons):
            lesson = cl.lesson
            is_completed = lesson.id in completed_ids

            if idx == 0:
                locked = False
            else:
                locked = not prev_completed

            lessons_payload.append(
                {
                    "id": lesson.id,
                    "name": lesson.name,
                    "order": cl.order,
                    "locked": locked,
                    "completed": is_completed,
                }
            )

            # for next iteration
            prev_completed = is_completed

        serializer = CourseDetailSerializer(
            {"id": course.id, "name": course.name, "lessons": lessons_payload}
        )
        return Response(serializer.data, status=status.HTTP_200_OK)


@extend_schema(
    tags=["courses"],
    responses=LessonDetailSerializer,
)
class LessonDetailView(APIView):
    """
    GET /api/lessons/{id}
    Returns: full lesson with slides and assigned quiz ID. :contentReference[oaicite:6]{index=6}
    """
    permission_classes = [permissions.IsAuthenticated]

    def get(self, request, pk: int):
        lesson = get_object_or_404(Lesson, pk=pk)
        slides = Slide.objects.filter(lesson=lesson).order_by("order")

        lesson_quiz = (
            LessonQuiz.objects.filter(lesson=lesson)
            .values_list("quiz_id", flat=True)
            .first()
        )

        data = {
            "id": lesson.id,
            "name": lesson.name,
            "slides": slides,
            "quiz_id": lesson_quiz,
        }
        serializer = LessonDetailSerializer(data)
        return Response(serializer.data, status=status.HTTP_200_OK)


@extend_schema(
    tags=["courses"],
    request=None,
    responses={
        200: inline_serializer(
            name="LessonCompleteResponse",
            fields={"status": serializers.CharField()},
        )
    },
)
class LessonCompleteView(APIView):
    """
    POST /api/lessons/{id}/complete
    - Marks lesson as completed for current user
    - If all lessons in a course are completed, creates UserProgress for that course
      (first time only) :contentReference[oaicite:7]{index=7}
    """

    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, pk: int):
        user = request.user
        lesson = get_object_or_404(Lesson, pk=pk)
        completed_fast = str(request.data.get("completed_fast", "")).lower() in (
            "true",
            "1",
            "yes",
            "on",
        )

        # 1. mark lesson as completed
        LessonProgress.objects.get_or_create(
            user=user,
            lesson=lesson,
            defaults={"completion_date": timezone.now()},
        )

        # 2. for each course containing this lesson, check if all its lessons completed
        course_links = CourseLesson.objects.filter(lesson=lesson).select_related("course")
        now = timezone.now()

        for cl in course_links:
            course = cl.course
            course_lessons = CourseLesson.objects.filter(course=course).values_list(
                "lesson_id", flat=True
            )
            completed_for_course = LessonProgress.objects.filter(
                user=user, lesson_id__in=course_lessons
            ).values_list("lesson_id", flat=True)

            if set(course_lessons).issubset(set(completed_for_course)):
                # all lessons in this course completed
                progress, created = UserProgress.objects.get_or_create(
                    user=user,
                    course=course,
                    defaults={"completion_date": now},
                )
                if created:
                    award_for_course_completion(user)

        if completed_fast:
            award_sprinter(user)

        return Response({"status": "ok"}, status=status.HTTP_200_OK)
