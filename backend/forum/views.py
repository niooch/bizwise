from django.db.models import Count, Q
from django.shortcuts import get_object_or_404

from rest_framework import generics, permissions, status, filters, serializers
from rest_framework.response import Response
from rest_framework.views import APIView
from drf_spectacular.utils import (
    extend_schema,
    extend_schema_view,
    OpenApiParameter,
    inline_serializer,
)
from drf_spectacular.types import OpenApiTypes

from .models import (
    Post,
    Tag,
    Comment,
    Reaction,
    PostReaction,
    CommentReaction,
)
from .serializers import (
    TagSerializer,
    PostListSerializer,
    PostCreateUpdateSerializer,
    PostDetailSerializer,
    CommentTreeSerializer,
)
from .permissions import IsAuthorOrReadOnly


@extend_schema_view(
    get=extend_schema(
        tags=["forum"],
        parameters=[
            OpenApiParameter(
                name="tag",
                type=OpenApiTypes.INT,
                location=OpenApiParameter.QUERY,
                description="Filter posts by tag id",
            ),
            OpenApiParameter(
                name="search",
                type=str,
                location=OpenApiParameter.QUERY,
                description="Search in title/content",
            ),
        ],
        responses=PostListSerializer,
    ),
    post=extend_schema(
        tags=["forum"],
        request=PostCreateUpdateSerializer,
        responses=PostCreateUpdateSerializer,
    ),
)
class PostListCreateView(generics.ListCreateAPIView):
    """
    GET /api/forum/posts
      - pagination (DRF default)
      - filter by ?tag=<id>
    POST /api/forum/posts
      - body: { title, content, tag_ids: [ids] }
    """
    permission_classes = [permissions.IsAuthenticated]
    filter_backends = [filters.SearchFilter]
    search_fields = ["title", "content"]

    def get_queryset(self):
        qs = (
            Post.objects.all()
            .annotate(
                comments_count=Count("comments", distinct=True),
                reactions_count=Count("post_reactions", distinct=True),
            )
            .order_by("-creation_date")
        )
        tag_id = self.request.query_params.get("tag")
        if tag_id:
            qs = qs.filter(post_tags__tag_id=tag_id)
        return qs

    def get_serializer_class(self):
        if self.request.method == "GET":
            return PostListSerializer
        return PostCreateUpdateSerializer

    def perform_create(self, serializer):
        serializer.save()  # user is taken from context in serializer.create()

    def get_serializer_context(self):
        ctx = super().get_serializer_context()
        ctx["request"] = self.request
        return ctx


@extend_schema_view(
    get=extend_schema(tags=["forum"], responses=PostDetailSerializer),
    put=extend_schema(tags=["forum"], request=PostCreateUpdateSerializer, responses=PostCreateUpdateSerializer),
    delete=extend_schema(tags=["forum"], responses=None),
)
class PostDetailView(generics.RetrieveUpdateDestroyAPIView):
    """
    GET /api/forum/posts/{id}
      - returns post with comment tree
    PUT /api/forum/posts/{id}
      - edit, only author
    DELETE /api/forum/posts/{id}
      - delete, only author
    """
    queryset = Post.objects.all().prefetch_related("comments__replies")
    permission_classes = [permissions.IsAuthenticated, IsAuthorOrReadOnly]

    def get_serializer_class(self):
        if self.request.method == "GET":
            return PostDetailSerializer
        return PostCreateUpdateSerializer

    def get_serializer_context(self):
        ctx = super().get_serializer_context()
        ctx["request"] = self.request
        return ctx


@extend_schema(
    tags=["forum"],
    request=inline_serializer(
        name="CommentCreateRequest",
        fields={
            "content": serializers.CharField(),
            "parent_comment_id": serializers.IntegerField(required=False),
        },
    ),
    responses=CommentTreeSerializer,
)
class CommentCreateView(APIView):
    """
    POST /api/forum/posts/{id}/comments
    Body:
      { "content": "...", "parent_comment_id": <optional> }
    """
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, pk: int):
        post = get_object_or_404(Post, pk=pk)
        content = request.data.get("content")
        parent_id = request.data.get("parent_comment_id")

        if not content:
            return Response(
                {"detail": "content is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        parent_comment = None
        if parent_id is not None:
            parent_comment = get_object_or_404(Comment, pk=parent_id, post=post)

        comment = Comment.objects.create(
            post=post,
            parent_comment=parent_comment,
            author=request.user,
            content=content,
        )

        serializer = CommentTreeSerializer(comment)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


@extend_schema(
    tags=["forum"],
    responses=None,
)
class CommentDeleteView(APIView):
    """
    DELETE /api/forum/comments/{id}
    Only author can delete their comment.
    """
    permission_classes = [permissions.IsAuthenticated]

    def delete(self, request, pk: int):
        comment = get_object_or_404(Comment, pk=pk)
        if comment.author != request.user:
            return Response(
                {"detail": "You are not the author of this comment."},
                status=status.HTTP_403_FORBIDDEN,
            )
        comment.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


@extend_schema(
    tags=["forum"],
    request=inline_serializer(
        name="PostReactRequest",
        fields={"reaction_type": serializers.CharField()},
    ),
    responses={
        200: inline_serializer(
            name="ReactResponse",
            fields={"status": serializers.CharField()},
        )
    },
)
class PostReactView(APIView):
    """
    POST /api/forum/posts/{id}/react
    Body: { "reaction_type": "LIKE" | "UPVOTE" | ... }
    Toggle behavior (add/remove reaction). :contentReference[oaicite:15]{index=15}
    """
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, pk: int):
        post = get_object_or_404(Post, pk=pk)
        reaction_type = request.data.get("reaction_type")
        if not reaction_type:
            return Response(
                {"detail": "reaction_type is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        reaction, _ = Reaction.objects.get_or_create(reaction_type=reaction_type)
        existing = PostReaction.objects.filter(
            post=post, reaction=reaction, user=request.user
        )

        if existing.exists():
            existing.delete()
            toggled = "removed"
        else:
            PostReaction.objects.create(
                post=post, reaction=reaction, user=request.user
            )
            toggled = "added"

        return Response({"status": toggled}, status=status.HTTP_200_OK)


@extend_schema(
    tags=["forum"],
    request=inline_serializer(
        name="CommentReactRequest",
        fields={"reaction_type": serializers.CharField()},
    ),
    responses={
        200: inline_serializer(
            name="CommentReactResponse",
            fields={"status": serializers.CharField()},
        )
    },
)
class CommentReactView(APIView):
    """
    POST /api/forum/comments/{id}/react
    Body: { "reaction_type": "LIKE" | "UPVOTE" | ... }
    """
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request, pk: int):
        comment = get_object_or_404(Comment, pk=pk)
        reaction_type = request.data.get("reaction_type")
        if not reaction_type:
            return Response(
                {"detail": "reaction_type is required"},
                status=status.HTTP_400_BAD_REQUEST,
            )

        reaction, _ = Reaction.objects.get_or_create(reaction_type=reaction_type)
        existing = CommentReaction.objects.filter(
            comment=comment, reaction=reaction, user=request.user
        )

        if existing.exists():
            existing.delete()
            toggled = "removed"
        else:
            CommentReaction.objects.create(
                comment=comment, reaction=reaction, user=request.user
            )
            toggled = "added"

        return Response({"status": toggled}, status=status.HTTP_200_OK)


@extend_schema(
    tags=["forum"],
    responses=TagSerializer,
)
class TagListView(generics.ListAPIView):
    """
    GET /api/forum/tags
    Returns list of all tags.
    """
    queryset = Tag.objects.all().order_by("name")
    serializer_class = TagSerializer
    permission_classes = [permissions.IsAuthenticated]
