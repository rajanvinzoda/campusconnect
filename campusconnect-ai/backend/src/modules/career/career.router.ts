import { Router, Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { authenticateToken } from '../../middleware/auth.middleware.js';

export const createCareerRouter = (prisma: PrismaClient) => {
  const router = Router();
  router.use(authenticateToken(prisma));

  router.get('/', async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user!.id;
      const jobs = await prisma.careerOpportunity.findMany({
        orderBy: { createdAt: 'desc' },
        include: {
          applications: { where: { userId }, select: { id: true } },
        },
      });

      const formatted = jobs.map((j) => ({
        id: j.id,
        companyName: j.companyName,
        roleTitle: j.roleTitle,
        type: j.type,
        location: j.location,
        stipend: j.stipend,
        deadline: j.deadline,
        requiredSkills: j.requiredSkills,
        description: j.description,
        isApplied: j.applications.length > 0,
      }));

      res.json({ careerItems: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch career opportunities', details: err.message });
    }
  });

  router.post('/:id/apply', async (req: Request, res: Response): Promise<void> => {
    try {
      const jobId = req.params.id;
      const userId = req.user!.id;

      await prisma.careerApplication.upsert({
        where: { jobId_userId: { jobId, userId } },
        update: {},
        create: { jobId, userId },
      });

      res.json({ message: 'Application submitted successfully', isApplied: true });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to apply', details: err.message });
    }
  });

  return router;
};
