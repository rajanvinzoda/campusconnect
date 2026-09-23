import { Router, Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { authenticateToken } from '../../middleware/auth.middleware.js';

export const createEventsRouter = (prisma: PrismaClient) => {
  const router = Router();
  router.use(authenticateToken(prisma));

  router.get('/', async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user!.id;
      const events = await prisma.campusEvent.findMany({
        orderBy: { createdAt: 'desc' },
        include: {
          registrations: { where: { userId }, select: { id: true } },
          _count: { select: { registrations: true } },
        },
      });

      const formatted = events.map((e) => ({
        id: e.id,
        title: e.title,
        organizer: e.organizer,
        category: e.category,
        date: e.date,
        location: e.location,
        description: e.description,
        attendeesCount: e._count.registrations,
        isRegistered: e.registrations.length > 0,
        qrCodeSeed: `QR_EVT_${e.id}_${userId}`,
      }));

      res.json({ events: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch events', details: err.message });
    }
  });

  router.post('/:id/register', async (req: Request, res: Response): Promise<void> => {
    try {
      const eventId = req.params.id;
      const userId = req.user!.id;

      const existing = await prisma.eventRegistration.findUnique({
        where: { eventId_userId: { eventId, userId } },
      });

      if (existing) {
        await prisma.eventRegistration.delete({ where: { id: existing.id } });
        res.json({ isRegistered: false });
      } else {
        await prisma.eventRegistration.create({ data: { eventId, userId } });
        res.json({ isRegistered: true });
      }
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to toggle event registration', details: err.message });
    }
  });

  return router;
};
