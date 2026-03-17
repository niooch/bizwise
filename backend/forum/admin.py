from django.contrib import admin

from .models import (
    Comment,
    CommentReaction,
    Post,
    PostReaction,
    PostTag,
    Reaction,
    Tag,
)


class CommentInline(admin.TabularInline):
    model = Comment
    extra = 0
    fields = ("author", "content", "parent_comment", "creation_date")
    readonly_fields = ("creation_date",)
    autocomplete_fields = ("author", "parent_comment")
    show_change_link = True
    ordering = ("-creation_date",)


@admin.register(Post)
class PostAdmin(admin.ModelAdmin):
    list_display = ("id", "title", "author", "creation_date")
    search_fields = ("title", "content", "author__username", "author__email")
    list_filter = ("creation_date", "tags")
    readonly_fields = ("creation_date",)
    autocomplete_fields = ("author", "tags")
    inlines = [CommentInline]
    ordering = ("-creation_date",)


@admin.register(Comment)
class CommentAdmin(admin.ModelAdmin):
    list_display = ("id", "post", "author", "parent_comment", "creation_date")
    search_fields = ("content", "author__username", "author__email", "post__title")
    list_filter = ("creation_date", "post")
    readonly_fields = ("creation_date",)
    autocomplete_fields = ("post", "author", "parent_comment")
    ordering = ("-creation_date",)


@admin.register(Tag)
class TagAdmin(admin.ModelAdmin):
    list_display = ("id", "name")
    search_fields = ("name",)


@admin.register(Reaction)
class ReactionAdmin(admin.ModelAdmin):
    list_display = ("id", "reaction_type")
    search_fields = ("reaction_type",)


@admin.register(PostReaction)
class PostReactionAdmin(admin.ModelAdmin):
    list_display = ("id", "post", "reaction", "user", "created_at")
    search_fields = ("post__title", "user__username", "user__email", "reaction__reaction_type")
    list_filter = ("reaction", "created_at")
    readonly_fields = ("created_at",)
    autocomplete_fields = ("post", "reaction", "user")
    ordering = ("-created_at",)


@admin.register(CommentReaction)
class CommentReactionAdmin(admin.ModelAdmin):
    list_display = ("id", "comment", "reaction", "user", "created_at")
    search_fields = ("comment__content", "user__username", "user__email", "reaction__reaction_type")
    list_filter = ("reaction", "created_at")
    readonly_fields = ("created_at",)
    autocomplete_fields = ("comment", "reaction", "user")
    ordering = ("-created_at",)


@admin.register(PostTag)
class PostTagAdmin(admin.ModelAdmin):
    list_display = ("id", "post", "tag")
    search_fields = ("post__title", "tag__name")
    autocomplete_fields = ("post", "tag")
