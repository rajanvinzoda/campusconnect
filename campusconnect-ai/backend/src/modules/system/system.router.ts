import { Router, Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';

export const createSystemRouter = (prisma: PrismaClient) => {
  const router = Router();

  router.get('/health', async (req: Request, res: Response): Promise<void> => {
    try {
      // Test DB connection
      await prisma.$queryRaw`SELECT 1`;

      res.json({
        status: 'UP',
        timestamp: Date.now(),
        uptimeSeconds: Math.floor(process.uptime()),
        database: 'CONNECTED_POSTGRES',
        environment: process.env.NODE_ENV || 'production',
        gateway: 'CAMPUSCONNECT_API_GATEWAY_V1',
        activeNodes: ['Gateway-US-East', 'Real-Time WebSocket Gateway', 'Central DB Node'],
      });
    } catch (err: any) {
      res.status(503).json({
        status: 'DEGRADED',
        error: 'Database connection check failed',
        details: err.message,
      });
    }
  });

  return router;
};
