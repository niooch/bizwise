from django.contrib import admin

from .models import Avatar, Badge, UserBadge, UserProfile, UserStreak


@admin.register(Avatar)
class AvatarAdmin(admin.ModelAdmin):
    list_display = ("name", "image", "image_url")
    search_fields = ("name",)


@admin.register(Badge)
class BadgeAdmin(admin.ModelAdmin):
    list_display = ("name", "image", "image_url")
    search_fields = ("name",)


@admin.register(UserBadge)
class UserBadgeAdmin(admin.ModelAdmin):
    list_display = ("user", "badge", "awarded_at")
    search_fields = ("user__username", "badge__name")
    list_filter = ("badge",)


@admin.register(UserProfile)
class UserProfileAdmin(admin.ModelAdmin):
    list_display = ("user", "avatar")


@admin.register(UserStreak)
class UserStreakAdmin(admin.ModelAdmin):
    list_display = ("user", "best_streak", "begin_date", "last_activity_date")
