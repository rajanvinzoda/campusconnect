import { Router, Request, Response } from 'express';
import { PrismaClient, UserRole, AccountStatus } from '@prisma/client';
import { authenticateToken, requireRole } from '../../middleware/auth.middleware.js';

export const createAdminRouter = (prisma: PrismaClient) => {
  const router = Router();

  // All endpoints require SUPER_ADMIN authority
  router.use(authenticateToken(prisma));
  router.use(requireRole([UserRole.SUPER_ADMIN], prisma));

  // Get all users
  router.get('/users', async (req: Request, res: Response): Promise<void> => {
    try {
      const users = await prisma.user.findMany({
        select: {
          id: true,
          email: true,
          name: true,
          role: true,
          status: true,
          department: true,
          branch: true,
          createdAt: true,
          _count: {
            select: { posts: true, sessions: true },
          },
        },
        orderBy: { createdAt: 'desc' },
      });
      res.json({ users });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch users', details: err.message });
    }
  });

  // Change user role
  router.patch('/users/:id/role', async (req: Request, res: Response): Promise<void> => {
    try {
      const { id } = req.params;
      const { role } = req.body;

      if (!Object.values(UserRole).includes(role)) {
        res.status(400).json({ error: 'Invalid role specified' });
        return;
      }

      const targetUser = await prisma.user.findUnique({ where: { id } });
      if (!targetUser) {
        res.status(404).json({ error: 'User not found' });
        return;
      }

      // Prevent self-demotion if the current user is the sole SUPER_ADMIN
      if (targetUser.id === req.user!.id && role !== UserRole.SUPER_ADMIN) {
        const superAdminCount = await prisma.user.count({
          where: { role: UserRole.SUPER_ADMIN, status: AccountStatus.ACTIVE },
        });
        if (superAdminCount <= 1) {
          res.status(400).json({ error: 'Cannot demote the sole active Super Administrator' });
          return;
        }
      }

      const updated = await prisma.user.update({
        where: { id },
        data: { role },
      });

      // Audit Log
      await prisma.auditLog.create({
        data: {
          actorId: req.user!.id,
          action: 'ROLE_MODIFIED',
          targetType: 'USER',
          targetId: id,
          ipAddress: req.ip || '',
          metadata: { oldRole: targetUser.role, newRole: role },
        },
      });

      res.json({ message: 'Role updated successfully', user: updated });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to update role', details: err.message });
    }
  });

  // Suspend or Ban User + Revoke all active sessions
  router.patch('/users/:id/status', async (req: Request, res: Response): Promise<void> => {
    try {
      const { id } = req.params;
      const { status } = req.body;

      if (!Object.values(AccountStatus).includes(status)) {
        res.status(400).json({ error: 'Invalid status specified' });
        return;
      }

      if (id === req.user!.id) {
        res.status(400).json({ error: 'Super Admin cannot suspend or ban themselves' });
        return;
      }

      const updated = await prisma.user.update({
        where: { id },
        data: { status },
      });

      // Force revoke all active sessions for suspended or banned user
      if (status !== AccountStatus.ACTIVE) {
        await prisma.userSession.updateMany({
          where: { userId: id },
          data: { isRevoked: true },
        });
      }

      // Audit Log
      await prisma.auditLog.create({
        data: {
          actorId: req.user!.id,
          action: `USER_STATUS_${status}`,
          targetType: 'USER',
          targetId: id,
          ipAddress: req.ip || '',
          metadata: { newStatus: status },
        },
      });

      res.json({ message: `User status changed to ${status}`, user: updated });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to update status', details: err.message });
    }
  });

  // Force Logout User (Revoke all active sessions)
  router.post('/users/:id/force-logout', async (req: Request, res: Response): Promise<void> => {
    try {
      const { id } = req.params;
      await prisma.userSession.updateMany({
        where: { userId: id },
        data: { isRevoked: true },
      });

      await prisma.auditLog.create({
        data: {
          actorId: req.user!.id,
          action: 'FORCE_LOGOUT_EXECUTED',
          targetType: 'USER',
          targetId: id,
          ipAddress: req.ip || '',
        },
      });

      res.json({ message: 'User sessions successfully revoked' });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to revoke sessions', details: err.message });
    }
  });

  // Moderate & Delete Post
  router.delete('/posts/:id', async (req: Request, res: Response): Promise<void> => {
    try {
      const { id } = req.params;
      const post = await prisma.post.findUnique({ where: { id } });
      if (!post) {
        res.status(404).json({ error: 'Post not found' });
        return;
      }

      await prisma.post.delete({ where: { id } });

      await prisma.auditLog.create({
        data: {
          actorId: req.user!.id,
          action: 'POST_REMOVED_BY_ADMIN',
          targetType: 'POST',
          targetId: id,
          ipAddress: req.ip || '',
          metadata: { authorId: post.authorId, snippet: post.content.substring(0, 100) },
        },
      });

      res.json({ message: 'Post removed by Super Administrator' });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to remove post', details: err.message });
    }
  });

  // Real System Metrics & Platform Health
  router.get('/metrics', async (req: Request, res: Response): Promise<void> => {
    try {
      const [totalUsers, activeSessions, totalPosts, totalMessages, totalResources, recentAudits] = await Promise.all([
        prisma.user.count(),
        prisma.userSession.count({ where: { isRevoked: false, expiresAt: { gt: new Date() } } }),
        prisma.post.count(),
        prisma.chatMessage.count(),
        prisma.academicResource.count(),
        prisma.auditLog.findMany({
          take: 15,
          orderBy: { createdAt: 'desc' },
          include: { actor: { select: { name: true, email: true, role: true } } },
        }),
      ]);

      const uptimeSec = Math.floor(process.uptime());

      res.json({
        health: {
          isOnline: true,
          uptimeSeconds: uptimeSec,
          serverVersion: '3.2.0-Production-Node',
          activeClusterNodes: ['Production-API-Worker-1', 'WebSocket-Gateway-01', 'Postgres-Primary'],
        },
        counts: {
          totalUsers,
          activeSessions,
          totalPosts,
          totalMessages,
          totalResources,
        },
        recentAudits,
      });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch metrics', details: err.message });
    }
  });

  // Audit Logs
  router.get('/audit-logs', async (req: Request, res: Response): Promise<void> => {
    try {
      const logs = await prisma.auditLog.findMany({
        take: 50,
        orderBy: { createdAt: 'desc' },
        include: { actor: { select: { id: true, name: true, email: true, role: true } } },
      });
      res.json({ logs });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch audit logs', details: err.message });
    }
  });

  return router;
};
