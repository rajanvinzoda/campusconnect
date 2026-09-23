import { Router, Request, Response } from 'express';
import { PrismaClient, PostType } from '@prisma/client';
import { authenticateToken } from '../../middleware/auth.middleware.js';

export const createPostsRouter = (prisma: PrismaClient, ioBroadcast?: (event: string, data: any) => void) => {
  const router = Router();
  router.use(authenticateToken(prisma));

  // List all posts with current user's interaction state
  router.get('/', async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user!.id;
      const posts = await prisma.post.findMany({
        orderBy: { createdAt: 'desc' },
        include: {
          author: {
            select: { id: true, name: true, role: true, department: true, avatarUrl: true },
          },
          likes: { where: { userId }, select: { id: true } },
          bookmarks: { where: { userId }, select: { id: true } },
          _count: { select: { likes: true, comments: true } },
        },
      });

      const formatted = posts.map((p) => ({
        id: p.id,
        authorId: p.author.id,
        authorName: p.author.name,
        authorRole: p.author.role,
        authorAvatar: p.author.avatarUrl,
        department: p.author.department,
        timestamp: p.createdAt.getTime(),
        type: p.type,
        content: p.content,
        mediaUrls: p.mediaUrls,
        category: p.category,
        hashtags: p.hashtags,
        likesCount: p._count.likes,
        commentsCount: p._count.comments,
        isLiked: p.likes.length > 0,
        isBookmarked: p.bookmarks.length > 0,
      }));

      res.json({ posts: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch posts', details: err.message });
    }
  });

  // Create post
  router.post('/', async (req: Request, res: Response): Promise<void> => {
    try {
      const { content, type, category, hashtags, mediaUrls } = req.body;
      if (!content || !content.trim()) {
        res.status(400).json({ error: 'Post content cannot be empty' });
        return;
      }

      if (type === PostType.ANNOUNCEMENT) {
        const allowedRoles: string[] = ['SUPER_ADMIN', 'UNIVERSITY_ADMIN', 'DEPARTMENT_ADMIN', 'FACULTY', 'CLUB_LEADER'];
        if (!allowedRoles.includes(req.user!.role)) {
          res.status(403).json({ error: 'Unauthorized: Only verified leaders and faculty can publish announcements' });
          return;
        }
      }

      const post = await prisma.post.create({
        data: {
          authorId: req.user!.id,
          content: content.trim(),
          type: (type as PostType) || PostType.TEXT,
          category: category || 'General',
          hashtags: Array.isArray(hashtags) ? hashtags : [],
          mediaUrls: Array.isArray(mediaUrls) ? mediaUrls : [],
        },
        include: {
          author: { select: { id: true, name: true, role: true, department: true, avatarUrl: true } },
        },
      });

      const formatted = {
        id: post.id,
        authorId: post.author.id,
        authorName: post.author.name,
        authorRole: post.author.role,
        authorAvatar: post.author.avatarUrl,
        department: post.author.department,
        timestamp: post.createdAt.getTime(),
        type: post.type,
        content: post.content,
        mediaUrls: post.mediaUrls,
        category: post.category,
        hashtags: post.hashtags,
        likesCount: 0,
        commentsCount: 0,
        isLiked: false,
        isBookmarked: false,
      };

      if (ioBroadcast) {
        ioBroadcast('post:new', formatted);
      }

      res.status(201).json({ post: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to create post', details: err.message });
    }
  });

  // Toggle Like
  router.post('/:id/like', async (req: Request, res: Response): Promise<void> => {
    try {
      const postId = req.params.id;
      const userId = req.user!.id;

      const existing = await prisma.postLike.findUnique({
        where: { postId_userId: { postId, userId } },
      });

      if (existing) {
        await prisma.postLike.delete({ where: { id: existing.id } });
        res.json({ isLiked: false });
      } else {
        await prisma.postLike.create({ data: { postId, userId } });
        res.json({ isLiked: true });
      }
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to toggle like', details: err.message });
    }
  });

  // Toggle Bookmark
  router.post('/:id/bookmark', async (req: Request, res: Response): Promise<void> => {
    try {
      const postId = req.params.id;
      const userId = req.user!.id;

      const existing = await prisma.postBookmark.findUnique({
        where: { postId_userId: { postId, userId } },
      });

      if (existing) {
        await prisma.postBookmark.delete({ where: { id: existing.id } });
        res.json({ isBookmarked: false });
      } else {
        await prisma.postBookmark.create({ data: { postId, userId } });
        res.json({ isBookmarked: true });
      }
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to toggle bookmark', details: err.message });
    }
  });

  // Delete Post (Author or Administration only)
  router.delete('/:id', async (req: Request, res: Response): Promise<void> => {
    try {
      const postId = req.params.id;
      const userId = req.user!.id;

      const post = await prisma.post.findUnique({ where: { id: postId } });
      if (!post) {
        res.status(404).json({ error: 'Post not found' });
        return;
      }

      const isAuthor = post.authorId === userId;
      const isAdmin = ['SUPER_ADMIN', 'UNIVERSITY_ADMIN', 'DEPARTMENT_ADMIN'].includes(req.user!.role);

      if (!isAuthor && !isAdmin) {
        res.status(403).json({ error: 'Forbidden: You do not have permission to delete this post' });
        return;
      }

      await prisma.post.delete({ where: { id: postId } });
      res.json({ success: true, message: 'Post deleted successfully' });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to delete post', details: err.message });
    }
  });

  return router;
};
