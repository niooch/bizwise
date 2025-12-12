# pages/views.py
from django.shortcuts import render, get_object_or_404
from courses.models import Course, Lesson


def home_view(request):
    courses_list = Course.objects.prefetch_related('course_lessons').all()
    return render(request, "pages/home.html", {"courses": courses_list})


def course_detail_view(request, pk):
    # Pobieramy kurs lub zwracamy błąd 404
    course = get_object_or_404(Course, pk=pk)

    # Pobieramy lekcje posortowane po polu 'order' z tabeli pośredniej CourseLesson
    # select_related optymalizuje zapytania do bazy
    course_content = course.course_lessons.select_related('lesson').order_by('order')

    context = {
        "course": course,
        "course_content": course_content,
    }
    return render(request, "pages/course_detail.html", context)


def lesson_detail_view(request, pk):
    lesson = get_object_or_404(Lesson, pk=pk)

    # Pobieramy slajdy dla tej lekcji, posortowane po 'order'
    slides = lesson.slides.order_by('order')

    # Sprawdzamy czy do lekcji jest przypięty Quiz (przez model LessonQuiz)
    # first() zwróci pierwszy obiekt lub None
    linked_quiz = lesson.lesson_quizzes.first()

    context = {
        "lesson": lesson,
        "slides": slides,
        "quiz_link": linked_quiz.quiz if linked_quiz else None
    }
    return render(request, "pages/lesson_detail.html", context)