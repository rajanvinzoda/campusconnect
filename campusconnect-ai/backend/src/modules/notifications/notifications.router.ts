import { Router, Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { authenticateToken } from '../../middleware/auth.middleware.js';

export const createNotificationsRouter = (prisma: PrismaClient) => {
  const router = Router();
  router.use(authenticateToken(prisma));

  router.get('/', async (req: Request, res: Response): Promise<void> => {
    try {
      const notifications = await prisma.notification.findMany({
        where: { userId: req.user!.id },
        orderBy: { createdAt: 'desc' },
        take: 30,
      });

      const formatted = notifications.map((n) => ({
        id: n.id,
        title: n.title,
        message: n.message,
        type: n.type,
        timestamp: n.createdAt.getTime(),
        isRead: n.isRead,
      }));

      res.json({ notifications: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch notifications', details: err.message });
    }
  });

  router.patch('/:id/read', async (req: Request, res: Response): Promise<void> => {
    try {
      const { id } = req.params;
      await prisma.notification.updateMany({
        where: { id, userId: req.user!.id },
        data: { isRead: true },
      });
      res.json({ success: true });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to mark notification read', details: err.message });
    }
  });

  return router;
};
