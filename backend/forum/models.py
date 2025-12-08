from django.conf import settings
from django.db import models

User = settings.AUTH_USER_MODEL


class ReactionType(models.TextChoices):
    UPVOTE = "UPVOTE", "Upvote"
    LIKE = "LIKE", "Like"
    LAUGH = "LAUGH", "Laugh"
    SAD = "SAD", "Sad"
    ANGRY = "ANGRY", "Angry"
    # extend if you want


class Post(models.Model):
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="posts",
    )
    title = models.CharField(max_length=255)
    content = models.TextField()
    creation_date = models.DateTimeField(auto_now_add=True)
    tags = models.ManyToManyField(
        "Tag",
        through="PostTag",
        related_name="posts",
        blank=True,
    )

    def __str__(self) -> str:
        return self.title


class Tag(models.Model):
    name = models.CharField(max_length=50, unique=True)

    def __str__(self) -> str:
        return self.name


class PostTag(models.Model):
    post = models.ForeignKey(
        Post,
        on_delete=models.CASCADE,
        related_name="post_tags",
    )
    tag = models.ForeignKey(
        Tag,
        on_delete=models.CASCADE,
        related_name="post_tags",
    )

    class Meta:
        unique_together = ("post", "tag")

    def __str__(self) -> str:
        return f"{self.post} – {self.tag}"


class Comment(models.Model):
    post = models.ForeignKey(
        Post,
        on_delete=models.CASCADE,
        related_name="comments",
    )
    parent_comment = models.ForeignKey(
        "self",
        null=True,
        blank=True,
        on_delete=models.CASCADE,
        related_name="replies",
    )
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="comments",
    )
    content = models.TextField()
    creation_date = models.DateTimeField(auto_now_add=True)

    def __str__(self) -> str:
        return f"Comment by {self.author} on {self.post}"


class Reaction(models.Model):
    reaction_type = models.CharField(
        max_length=20,
        choices=ReactionType.choices,
    )

    def __str__(self) -> str:
        return self.reaction_type


class PostReaction(models.Model):
    post = models.ForeignKey(
        Post,
        on_delete=models.CASCADE,
        related_name="post_reactions",
    )
    reaction = models.ForeignKey(
        Reaction,
        on_delete=models.CASCADE,
        related_name="post_reactions",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="post_reactions",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("post", "reaction", "user")

    def __str__(self) -> str:
        return f"{self.user} {self.reaction} on {self.post}"


class CommentReaction(models.Model):
    comment = models.ForeignKey(
        Comment,
        on_delete=models.CASCADE,
        related_name="comment_reactions",
    )
    reaction = models.ForeignKey(
        Reaction,
        on_delete=models.CASCADE,
        related_name="comment_reactions",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="comment_reactions",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ("comment", "reaction", "user")

    def __str__(self) -> str:
        return f"{self.user} {self.reaction} on comment {self.comment_id}"
