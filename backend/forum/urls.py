from django.urls import path

from .views import (
    PostListCreateView,
    PostDetailView,
    CommentCreateView,
    CommentDeleteView,
    CommentDetailView,
    PostReactView,
    CommentReactView,
    PostReactionsSummaryView,
    CommentReactionsSummaryView,
    TagListView,
)

urlpatterns = [
    path("posts/", PostListCreateView.as_view(), name="forum-posts"),
    path("posts/<int:pk>/", PostDetailView.as_view(), name="forum-post-detail"),
    path("posts/<int:pk>/reactions/", PostReactionsSummaryView.as_view(), name="forum-post-reactions"),
    path("posts/<int:pk>/comments/", CommentCreateView.as_view(), name="forum-post-comments"),
    path("comments/<int:pk>/", CommentDetailView.as_view(), name="forum-comment-detail"),
    path("comments/<int:pk>/reactions/", CommentReactionsSummaryView.as_view(), name="forum-comment-reactions"),
    path("posts/<int:pk>/react/", PostReactView.as_view(), name="forum-post-react"),
    path("comments/<int:pk>/react/", CommentReactView.as_view(), name="forum-comment-react"),
    path("tags/", TagListView.as_view(), name="forum-tags"),
]
