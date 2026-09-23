import { Router, Request, Response } from 'express';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { PrismaClient, UserRole, AccountStatus } from '@prisma/client';
import { config } from '../../config/index.js';
import { authenticateToken } from '../../middleware/auth.middleware.js';

export const createAuthRouter = (prisma: PrismaClient) => {
  const router = Router();

  // Public Registration - Strictly forces role to STUDENT
  router.post('/register', async (req: Request, res: Response): Promise<void> => {
    try {
      const { email, password, name, department, branch, bio, skills, interests } = req.body;

      if (!email || !password || !name) {
        res.status(400).json({ error: 'Email, password, and name are required' });
        return;
      }

      if (password.length < 8) {
        res.status(400).json({ error: 'Password must be at least 8 characters long' });
        return;
      }

      const existingUser = await prisma.user.findUnique({ where: { email: email.toLowerCase() } });
      if (existingUser) {
        res.status(409).json({ error: 'An account with this email already exists' });
        return;
      }

      const passwordHash = await bcrypt.hash(password, 12);

      // SECURITY RULE: Ignore any client-sent role, always force STUDENT
      const user = await prisma.user.create({
        data: {
          email: email.toLowerCase(),
          passwordHash,
          name,
          role: UserRole.STUDENT, // Strictly enforced
          status: AccountStatus.ACTIVE,
          department: department || 'General',
          branch: branch || 'General',
          bio: bio || '',
          skills: Array.isArray(skills) ? skills : [],
          interests: Array.isArray(interests) ? interests : [],
          isVerified: true,
        },
      });

      const accessToken = jwt.sign({ userId: user.id }, config.jwt.accessSecret, {
        expiresIn: '15m',
      });
      const refreshToken = jwt.sign({ userId: user.id }, config.jwt.refreshSecret, {
        expiresIn: '7d',
      });

      await prisma.userSession.create({
        data: {
          userId: user.id,
          refreshToken,
          userAgent: req.headers['user-agent'] || '',
          ipAddress: req.ip || '',
          expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
        },
      });

      res.status(201).json({
        user: {
          id: user.id,
          email: user.email,
          name: user.name,
          role: user.role,
          department: user.department,
          branch: user.branch,
          bio: user.bio,
          skills: user.skills,
          interests: user.interests,
        },
        accessToken,
        refreshToken,
      });
    } catch (err: any) {
      res.status(500).json({ error: 'Registration failed', details: err.message });
    }
  });

  // Login
  router.post('/login', async (req: Request, res: Response): Promise<void> => {
    try {
      const { email, password } = req.body;

      if (!email || !password) {
        res.status(400).json({ error: 'Email and password are required' });
        return;
      }

      const user = await prisma.user.findUnique({
        where: { email: email.toLowerCase() },
      });

      if (!user) {
        res.status(401).json({ error: 'Invalid credentials' });
        return;
      }

      if (user.status === AccountStatus.SUSPENDED) {
        res.status(403).json({ error: 'Your account is currently suspended' });
        return;
      }

      if (user.status === AccountStatus.BANNED) {
        res.status(403).json({ error: 'Your account has been banned' });
        return;
      }

      const isMatch = await bcrypt.compare(password, user.passwordHash);
      if (!isMatch) {
        res.status(401).json({ error: 'Invalid credentials' });
        return;
      }

      const accessToken = jwt.sign({ userId: user.id }, config.jwt.accessSecret, {
        expiresIn: '15m',
      });
      const refreshToken = jwt.sign({ userId: user.id }, config.jwt.refreshSecret, {
        expiresIn: '7d',
      });

      await prisma.userSession.create({
        data: {
          userId: user.id,
          refreshToken,
          userAgent: req.headers['user-agent'] || '',
          ipAddress: req.ip || '',
          expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
        },
      });

      // Audit log successful login
      await prisma.auditLog.create({
        data: {
          actorId: user.id,
          action: 'USER_LOGIN_SUCCESS',
          targetType: 'SESSION',
          ipAddress: req.ip || '',
        },
      });

      res.json({
        user: {
          id: user.id,
          email: user.email,
          name: user.name,
          role: user.role,
          department: user.department,
          branch: user.branch,
          bio: user.bio,
          skills: user.skills,
          interests: user.interests,
        },
        accessToken,
        refreshToken,
      });
    } catch (err: any) {
      res.status(500).json({ error: 'Login failed', details: err.message });
    }
  });

  // Token Refresh with Rotation
  router.post('/refresh', async (req: Request, res: Response): Promise<void> => {
    try {
      const { refreshToken } = req.body;
      if (!refreshToken) {
        res.status(400).json({ error: 'Refresh token required' });
        return;
      }

      const session = await prisma.userSession.findUnique({
        where: { refreshToken },
        include: { user: true },
      });

      if (!session || session.isRevoked || session.expiresAt < new Date()) {
        res.status(401).json({ error: 'Invalid or expired session' });
        return;
      }

      // Rotate refresh token
      const newAccessToken = jwt.sign({ userId: session.userId }, config.jwt.accessSecret, {
        expiresIn: '15m',
      });
      const newRefreshToken = jwt.sign({ userId: session.userId }, config.jwt.refreshSecret, {
        expiresIn: '7d',
      });

      // Revoke old and create new session
      await prisma.userSession.update({
        where: { id: session.id },
        data: { isRevoked: true },
      });

      await prisma.userSession.create({
        data: {
          userId: session.userId,
          refreshToken: newRefreshToken,
          userAgent: req.headers['user-agent'] || '',
          ipAddress: req.ip || '',
          expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
        },
      });

      res.json({
        accessToken: newAccessToken,
        refreshToken: newRefreshToken,
      });
    } catch (err: any) {
      res.status(401).json({ error: 'Refresh failed', details: err.message });
    }
  });

  // Logout
  router.post('/logout', authenticateToken(prisma), async (req: Request, res: Response): Promise<void> => {
    try {
      const { refreshToken } = req.body;
      if (refreshToken) {
        await prisma.userSession.updateMany({
          where: { refreshToken, userId: req.user!.id },
          data: { isRevoked: true },
        });
      }
      res.json({ message: 'Logged out successfully' });
    } catch (err: any) {
      res.status(500).json({ error: 'Logout failed', details: err.message });
    }
  });

  // Current User Session
  router.get('/me', authenticateToken(prisma), async (req: Request, res: Response): Promise<void> => {
    const user = await prisma.user.findUnique({
      where: { id: req.user!.id },
      select: {
        id: true,
        email: true,
        name: true,
        role: true,
        status: true,
        department: true,
        branch: true,
        bio: true,
        skills: true,
        interests: true,
        isVerified: true,
        createdAt: true,
      },
    });
    res.json({ user });
  });

  // Super Admin Bootstrap Endpoint - Protected by server secret
  router.post('/bootstrap-superadmin', async (req: Request, res: Response): Promise<void> => {
    try {
      const { bootstrapSecret, email, password, name } = req.body;

      if (!config.bootstrapSecret || !bootstrapSecret || bootstrapSecret !== config.bootstrapSecret) {
        res.status(403).json({ error: 'Unauthorized: Bootstrap endpoint is disabled or secret is invalid' });
        return;
      }

      if (!email || !password || !name) {
        res.status(400).json({ error: 'Email, password, and name are required' });
        return;
      }

      if (password.length < 12) {
        res.status(400).json({ error: 'Super Admin password must be at least 12 characters long' });
        return;
      }

      const passwordHash = await bcrypt.hash(password, 12);

      const superAdmin = await prisma.user.upsert({
        where: { email: email.toLowerCase() },
        update: {
          role: UserRole.SUPER_ADMIN,
          status: AccountStatus.ACTIVE,
          passwordHash,
        },
        create: {
          email: email.toLowerCase(),
          passwordHash,
          name,
          role: UserRole.SUPER_ADMIN,
          status: AccountStatus.ACTIVE,
          department: 'Executive Administration',
          branch: 'Campus Leadership',
          bio: 'Primary Super Administrator and System Owner.',
          isVerified: true,
        },
      });

      await prisma.auditLog.create({
        data: {
          actorId: superAdmin.id,
          action: 'SUPER_ADMIN_BOOTSTRAP_CREATED',
          targetType: 'SYSTEM',
          targetId: superAdmin.id,
          ipAddress: req.ip || '',
          metadata: { email: superAdmin.email },
        },
      });

      res.status(200).json({
        message: 'Super Admin successfully initialized',
        user: {
          id: superAdmin.id,
          email: superAdmin.email,
          name: superAdmin.name,
          role: superAdmin.role,
        },
      });
    } catch (err: any) {
      res.status(500).json({ error: 'Bootstrap failed', details: err.message });
    }
  });

  return router;
};
