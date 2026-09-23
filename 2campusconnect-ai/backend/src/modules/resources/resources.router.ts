import { Router, Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { authenticateToken } from '../../middleware/auth.middleware.js';

export const createResourcesRouter = (prisma: PrismaClient) => {
  const router = Router();
  router.use(authenticateToken(prisma));

  router.get('/', async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user!.id;
      const resources = await prisma.academicResource.findMany({
        orderBy: { createdAt: 'desc' },
        include: {
          downloads: { where: { userId }, select: { id: true } },
          _count: { select: { downloads: true } },
        },
      });

      const formatted = resources.map((r) => ({
        id: r.id,
        title: r.title,
        subject: r.subject,
        department: r.department,
        fileType: r.fileType,
        authorName: r.authorName,
        rating: r.rating,
        downloadsCount: r._count.downloads,
        isDownloaded: r.downloads.length > 0,
      }));

      res.json({ resources: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch resources', details: err.message });
    }
  });

  router.post('/:id/download', async (req: Request, res: Response): Promise<void> => {
    try {
      const resourceId = req.params.id;
      const userId = req.user!.id;

      const existing = await prisma.resourceDownload.findUnique({
        where: { resourceId_userId: { resourceId, userId } },
      });

      if (existing) {
        await prisma.resourceDownload.delete({ where: { id: existing.id } });
        res.json({ isDownloaded: false });
      } else {
        await prisma.resourceDownload.create({ data: { resourceId, userId } });
        res.json({ isDownloaded: true });
      }
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to toggle resource download', details: err.message });
    }
  });

  return router;
};
