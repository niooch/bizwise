from rest_framework import serializers

from .models import Post, Tag, Comment, Reaction, PostReaction, CommentReaction


class TagSerializer(serializers.ModelSerializer):
    class Meta:
        model = Tag
        fields = ["id", "name"]


class PostListSerializer(serializers.ModelSerializer):
    author_nickname = serializers.CharField(source="author.username", read_only=True)
    tags = TagSerializer(many=True, source="post_tags__tag", read_only=True)
    comments_count = serializers.IntegerField(read_only=True)
    reactions_count = serializers.IntegerField(read_only=True)

    class Meta:
        model = Post
        fields = [
            "id",
            "title",
            "content",
            "creation_date",
            "author_nickname",
            "comments_count",
            "reactions_count",
        ]


class PostCreateUpdateSerializer(serializers.ModelSerializer):
    tag_ids = serializers.ListField(
        child=serializers.IntegerField(), write_only=True, required=False
    )

    class Meta:
        model = Post
        fields = ["title", "content", "tag_ids"]

    def create(self, validated_data):
        tag_ids = validated_data.pop("tag_ids", [])
        user = self.context["request"].user
        post = Post.objects.create(author=user, **validated_data)
        if tag_ids:
            tags = Tag.objects.filter(id__in=tag_ids)
            from .models import PostTag
            for t in tags:
                PostTag.objects.create(post=post, tag=t)
        return post

    def update(self, instance, validated_data):
        tag_ids = validated_data.pop("tag_ids", None)
        instance.title = validated_data.get("title", instance.title)
        instance.content = validated_data.get("content", instance.content)
        instance.save()

        if tag_ids is not None:
            from .models import PostTag
            PostTag.objects.filter(post=instance).delete()
            tags = Tag.objects.filter(id__in=tag_ids)
            for t in tags:
                PostTag.objects.create(post=instance, tag=t)

        return instance


class CommentTreeSerializer(serializers.ModelSerializer):
    author_nickname = serializers.CharField(source="author.username", read_only=True)
    replies = serializers.SerializerMethodField()

    class Meta:
        model = Comment
        fields = ["id", "author_nickname", "content", "creation_date", "replies"]

    def get_replies(self, obj):
        children = obj.replies.all().order_by("creation_date")
        return CommentTreeSerializer(children, many=True, context=self.context).data


class PostDetailSerializer(serializers.ModelSerializer):
    author_nickname = serializers.CharField(source="author.username", read_only=True)
    tags = TagSerializer(many=True, source="post_tags__tag", read_only=True)
    comments = CommentTreeSerializer(many=True, read_only=True)

    class Meta:
        model = Post
        fields = [
            "id",
            "title",
            "content",
            "creation_date",
            "author_nickname",
            "tags",
            "comments",
        ]

