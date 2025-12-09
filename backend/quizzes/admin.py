from django.contrib import admin

from .models import Quiz, Question, AnswerOption, AnswerPattern


class AnswerOptionInline(admin.TabularInline):
    model = AnswerOption
    extra = 2
    fields = ("content", "is_correct")


class AnswerPatternInline(admin.StackedInline):
    model = AnswerPattern
    extra = 0
    max_num = 1
    fields = ("pattern",)


@admin.register(Question)
class QuestionAdmin(admin.ModelAdmin):
    list_display = ("id", "content", "question_type", "quiz")
    list_filter = ("question_type", "quiz")
    search_fields = ("content",)
    inlines = [AnswerOptionInline, AnswerPatternInline]


class QuestionInline(admin.TabularInline):
    model = Question
    extra = 1
    fields = ("content", "question_type")


@admin.register(Quiz)
class QuizAdmin(admin.ModelAdmin):
    list_display = ("id", "name", "exp_weight", "questions_count")
    search_fields = ("name",)
    inlines = [QuestionInline]

    def questions_count(self, obj):
        return obj.questions.count()
    questions_count.short_description = "Questions"


admin.site.register(AnswerOption)
admin.site.register(AnswerPattern)
