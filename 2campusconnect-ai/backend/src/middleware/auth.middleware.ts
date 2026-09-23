import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config/index.js';
import { PrismaClient, UserRole, AccountStatus } from '@prisma/client';

export interface AuthenticatedUser {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  status: AccountStatus;
}

declare global {
  namespace Express {
    interface Request {
      user?: AuthenticatedUser;
    }
  }
}

export const authenticateToken = (prisma: PrismaClient) => {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.split(' ')[1] : null;

    if (!token) {
      res.status(401).json({ error: 'Access token required' });
      return;
    }

    try {
      const decoded = jwt.verify(token, config.jwt.accessSecret) as { userId: string };
      const user = await prisma.user.findUnique({
        where: { id: decoded.userId },
        select: { id: true, email: true, name: true, role: true, status: true },
      });

      if (!user) {
        res.status(401).json({ error: 'User not found or deleted' });
        return;
      }

      if (user.status === AccountStatus.SUSPENDED) {
        res.status(403).json({ error: 'Account suspended. Contact campus administration.' });
        return;
      }

      if (user.status === AccountStatus.BANNED) {
        res.status(403).json({ error: 'Account banned due to security policy violations.' });
        return;
      }

      req.user = user;
      next();
    } catch (err) {
      res.status(401).json({ error: 'Invalid or expired access token' });
    }
  };
};

export const requireRole = (allowedRoles: UserRole[], prisma: PrismaClient) => {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    if (!req.user) {
      res.status(401).json({ error: 'Unauthenticated' });
      return;
    }

    if (!allowedRoles.includes(req.user.role)) {
      // Security audit log for unauthorized privileged access attempt
      try {
        await prisma.auditLog.create({
          data: {
            actorId: req.user.id,
            action: 'UNAUTHORIZED_PRIVILEGE_ATTEMPT',
            targetType: 'API_ENDPOINT',
            targetId: req.originalUrl,
            ipAddress: req.ip || '',
            metadata: {
              attemptedRoleRequired: allowedRoles,
              userRole: req.user.role,
              method: req.method,
            },
          },
        });
      } catch (_) {}

      res.status(403).json({
        error: 'Forbidden: Insufficient privileges. This incident has been logged.',
      });
      return;
    }

    next();
  };
};
