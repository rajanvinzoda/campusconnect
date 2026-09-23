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

  // Post Opportunity (Authorized Roles Only)
  router.post('/', async (req: Request, res: Response): Promise<void> => {
    try {
      const allowedRoles = ['SUPER_ADMIN', 'UNIVERSITY_ADMIN', 'FACULTY', 'ALUMNI'];
      if (!allowedRoles.includes(req.user!.role)) {
        res.status(403).json({ error: 'Forbidden: Only faculty, university placement administrators, or verified alumni can publish opportunities' });
        return;
      }

      const { companyName, roleTitle, type, location, stipend, deadline, requiredSkills, description } = req.body;
      if (!companyName || !roleTitle || !location) {
        res.status(400).json({ error: 'Company name, role title, and location are required' });
        return;
      }

      const job = await prisma.careerOpportunity.create({
        data: {
          companyName: companyName.trim(),
          roleTitle: roleTitle.trim(),
          type: type || 'Full-Time',
          location: location.trim(),
          stipend: stipend || 'Competitive',
          deadline: deadline || 'Rolling',
          requiredSkills: Array.isArray(requiredSkills) ? requiredSkills : [],
          description: description || '',
        },
      });

      res.status(201).json({
        careerItem: {
          id: job.id,
          companyName: job.companyName,
          roleTitle: job.roleTitle,
          type: job.type,
          location: job.location,
          stipend: job.stipend,
          deadline: job.deadline,
          requiredSkills: job.requiredSkills,
          description: job.description,
          isApplied: false,
        },
      });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to post career opportunity', details: err.message });
    }
  });

  return router;
};
