import uuid
from pathlib import Path

from django.contrib import admin
from django import forms
from django.conf import settings
from django.core.files.storage import FileSystemStorage

from .models import Course, Lesson, Slide, CourseLesson, LessonQuiz


class SlideInlineForm(forms.ModelForm):
    """
    Adds file upload support for slide images; stores under STATIC_ROOT/slides and
    sets image_url accordingly.
    """
    image_upload = forms.FileField(required=False, help_text="Optional image file for this slide")

    class Meta:
        model = Slide
        fields = ["order", "text_content", "image_url", "image_upload"]
        help_texts = {
            "text_content": (
                "Obsługiwane znaczniki: &lt;b&gt;tekst&lt;/b&gt; (pogrubienie), "
                "&lt;i&gt;tekst&lt;/i&gt; (kursywa), &lt;br&gt; (nowa linia). "
                "Przykład: To jest &lt;b&gt;ważne&lt;/b&gt; i &lt;i&gt;pochylone&lt;/i&gt;."
            )
        }

    def save(self, commit=True):
        instance = super().save(commit=False)
        upload = self.cleaned_data.get("image_upload")
        if upload:
            slides_dir = Path(settings.STATIC_ROOT) / "slides"
            slides_dir.mkdir(parents=True, exist_ok=True)
            storage = FileSystemStorage(location=slides_dir, base_url=f"{settings.STATIC_URL}slides/")
            filename = f"{uuid.uuid4().hex}_{upload.name}"
            storage.save(filename, upload)
            instance.image_url = storage.url(filename)
        if commit:
            instance.save()
        return instance


class SlideInline(admin.TabularInline):
    model = Slide
    form = SlideInlineForm
    extra = 1
    fields = ("order", "text_content", "image_url", "image_upload")
    ordering = ("order",)


class LessonQuizInline(admin.StackedInline):
    model = LessonQuiz
    extra = 0
    max_num = 1
    fields = ("quiz",)


@admin.register(Lesson)
class LessonAdmin(admin.ModelAdmin):
    list_display = ("id", "name")
    search_fields = ("name",)
    inlines = [SlideInline, LessonQuizInline]


class CourseLessonInline(admin.TabularInline):
    model = CourseLesson
    extra = 1
    fields = ("lesson", "order")
    ordering = ("order",)


@admin.register(Course)
class CourseAdmin(admin.ModelAdmin):
    list_display = ("id", "name")
    search_fields = ("name",)
    inlines = [CourseLessonInline]


# Register models that can also be edited standalone if needed
@admin.register(Slide)
class SlideAdmin(admin.ModelAdmin):
    form = SlideInlineForm
    list_display = ("id", "lesson", "order", "image_url")
    list_filter = ("lesson",)
    search_fields = ("text_content",)

admin.site.register(CourseLesson)
admin.site.register(LessonQuiz)
