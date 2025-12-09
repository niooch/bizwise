from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase
from django.contrib.auth import get_user_model

from .models import Post, Comment, Reaction, PostReaction, CommentReaction

User = get_user_model()


class ForumEndpointsTests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(username="poster", password="pass12345")
        self.client.force_authenticate(self.user)
        self.post = Post.objects.create(author=self.user, title="Hello", content="World")

    def test_reaction_summary_for_post(self):
        reaction = Reaction.objects.create(reaction_type="LIKE")
        PostReaction.objects.create(post=self.post, reaction=reaction, user=self.user)

        url = reverse("forum-post-reactions", args=[self.post.id])
        resp = self.client.get(url)

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data["reactions"].get("LIKE"), 1)

    def test_comment_patch_requires_author_and_updates(self):
        comment = Comment.objects.create(
            post=self.post, author=self.user, content="Old"
        )
        url = reverse("forum-comment-detail", args=[comment.id])

        resp = self.client.patch(url, {"content": "New content"}, format="json")

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        comment.refresh_from_db()
        self.assertEqual(comment.content, "New content")
