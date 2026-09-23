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

  // Create Event (Authorized Roles Only)
  router.post('/', async (req: Request, res: Response): Promise<void> => {
    try {
      const allowedRoles = ['SUPER_ADMIN', 'UNIVERSITY_ADMIN', 'DEPARTMENT_ADMIN', 'FACULTY', 'CLUB_LEADER'];
      if (!allowedRoles.includes(req.user!.role)) {
        res.status(403).json({ error: 'Forbidden: Only authorized campus leaders and staff can create events' });
        return;
      }

      const { title, organizer, category, date, location, description } = req.body;
      if (!title || !date || !location) {
        res.status(400).json({ error: 'Title, date, and location are required' });
        return;
      }

      const event = await prisma.campusEvent.create({
        data: {
          title: title.trim(),
          organizer: organizer?.trim() || req.user!.name,
          category: category || 'General',
          date: date.trim(),
          location: location.trim(),
          description: description?.trim() || '',
        },
      });

      res.status(201).json({
        event: {
          id: event.id,
          title: event.title,
          organizer: event.organizer,
          category: event.category,
          date: event.date,
          location: event.location,
          description: event.description,
          attendeesCount: 0,
          isRegistered: false,
          qrCodeSeed: `QR_EVT_${event.id}_${req.user!.id}`,
        },
      });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to create event', details: err.message });
    }
  });

  // Delete Event (Authorized Roles Only)
  router.delete('/:id', async (req: Request, res: Response): Promise<void> => {
    try {
      const allowedRoles = ['SUPER_ADMIN', 'UNIVERSITY_ADMIN', 'DEPARTMENT_ADMIN', 'FACULTY', 'CLUB_LEADER'];
      if (!allowedRoles.includes(req.user!.role)) {
        res.status(403).json({ error: 'Forbidden: Insufficient privileges to delete events' });
        return;
      }

      const eventId = req.params.id;
      const event = await prisma.campusEvent.findUnique({ where: { id: eventId } });
      if (!event) {
        res.status(404).json({ error: 'Event not found' });
        return;
      }

      await prisma.campusEvent.delete({ where: { id: eventId } });
      res.json({ success: true, message: 'Event deleted successfully' });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to delete event', details: err.message });
    }
  });

  return router;
};
