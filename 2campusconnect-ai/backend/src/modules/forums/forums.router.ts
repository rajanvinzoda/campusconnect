import { Router, Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { authenticateToken } from '../../middleware/auth.middleware.js';

export const createForumsRouter = (prisma: PrismaClient) => {
  const router = Router();
  router.use(authenticateToken(prisma));

  router.get('/topics', async (req: Request, res: Response): Promise<void> => {
    try {
      const topics = await prisma.forumTopic.findMany({
        orderBy: { createdAt: 'desc' },
        include: {
          author: { select: { id: true, name: true, role: true } },
          _count: { select: { replies: true } },
        },
      });

      const formatted = topics.map((t) => ({
        id: t.id,
        title: t.title,
        authorName: t.author.name,
        authorRole: t.author.role,
        category: t.category,
        content: t.content,
        timestamp: t.createdAt.getTime(),
        upvotes: t.upvotes,
        downvotes: t.downvotes,
        userVote: 0,
        repliesCount: t._count.replies,
        tags: t.tags,
        isSolved: t.isSolved,
      }));

      res.json({ topics: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch topics', details: err.message });
    }
  });

  router.post('/topics', async (req: Request, res: Response): Promise<void> => {
    try {
      const { title, category, content, tags } = req.body;
      if (!title || !content) {
        res.status(400).json({ error: 'Title and content are required' });
        return;
      }

      const topic = await prisma.forumTopic.create({
        data: {
          authorId: req.user!.id,
          title: title.trim(),
          category: category || 'General Academic',
          content: content.trim(),
          tags: Array.isArray(tags) ? tags : [],
        },
        include: { author: { select: { name: true, role: true } } },
      });

      res.status(201).json({
        topic: {
          id: topic.id,
          title: topic.title,
          authorName: topic.author.name,
          authorRole: topic.author.role,
          category: topic.category,
          content: topic.content,
          timestamp: topic.createdAt.getTime(),
          upvotes: 0,
          downvotes: 0,
          userVote: 0,
          repliesCount: 0,
          tags: topic.tags,
          isSolved: false,
        },
      });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to create topic', details: err.message });
    }
  });

  router.post('/topics/:id/vote', async (req: Request, res: Response): Promise<void> => {
    try {
      const { id } = req.params;
      const { delta } = req.body; // 1 for up, -1 for down

      const topic = await prisma.forumTopic.findUnique({ where: { id } });
      if (!topic) {
        res.status(404).json({ error: 'Topic not found' });
        return;
      }

      const updated = await prisma.forumTopic.update({
        where: { id },
        data: {
          upvotes: delta > 0 ? { increment: 1 } : topic.upvotes,
          downvotes: delta < 0 ? { increment: 1 } : topic.downvotes,
        },
      });

      res.json({ upvotes: updated.upvotes, downvotes: updated.downvotes });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to vote', details: err.message });
    }
  });

  return router;
};
