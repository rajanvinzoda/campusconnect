import express from 'express';
import http from 'http';
import { Server as SocketIOServer } from 'socket.io';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { PrismaClient } from '@prisma/client';
import { config } from './config/index.js';

import { createAuthRouter } from './modules/auth/auth.router.js';
import { createAdminRouter } from './modules/admin/admin.router.js';
import { createPostsRouter } from './modules/posts/posts.router.js';
import { createChatRouter } from './modules/chat/chat.router.js';
import { createForumsRouter } from './modules/forums/forums.router.js';
import { createEventsRouter } from './modules/events/events.router.js';
import { createResourcesRouter } from './modules/resources/resources.router.js';
import { createCareerRouter } from './modules/career/career.router.js';
import { createNotificationsRouter } from './modules/notifications/notifications.router.js';
import { createAiRouter } from './modules/ai/ai.router.js';
import { createSystemRouter } from './modules/system/system.router.js';
import { setupSocketGateway } from './realtime/socket.gateway.js';

const app = express();
const server = http.createServer(app);
const io = new SocketIOServer(server, {
  cors: {
    origin: config.corsOrigin,
    methods: ['GET', 'POST', 'PATCH', 'DELETE'],
  },
});

const prisma = new PrismaClient();

// Security and utility middleware
app.use(helmet());
app.use(cors({ origin: config.corsOrigin }));
app.use(express.json({ limit: '15mb' }));
app.use(express.urlencoded({ extended: true, limit: '15mb' }));
app.use(morgan('combined'));

// Setup Real-time WebSockets
setupSocketGateway(io, prisma);

// Global real-time dispatch helpers
const broadcastGlobalEvent = (event: string, data: any) => {
  io.emit(event, data);
};

const broadcastChannelEvent = (channelId: string, event: string, data: any) => {
  io.to(`channel_${channelId}`).emit(event, data);
};

// Mount API Routers
app.use('/api/v1/auth', createAuthRouter(prisma));
app.use('/api/v1/admin', createAdminRouter(prisma));
app.use('/api/v1/posts', createPostsRouter(prisma, broadcastGlobalEvent));
app.use('/api/v1/chat', createChatRouter(prisma, broadcastChannelEvent));
app.use('/api/v1/forums', createForumsRouter(prisma));
app.use('/api/v1/events', createEventsRouter(prisma));
app.use('/api/v1/resources', createResourcesRouter(prisma));
app.use('/api/v1/career', createCareerRouter(prisma));
app.use('/api/v1/notifications', createNotificationsRouter(prisma));
app.use('/api/v1/ai', createAiRouter(prisma));
app.use('/api/v1/system', createSystemRouter(prisma));

// Global Error Handler
app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('[ServerError]', err);
  res.status(err.status || 500).json({
    error: 'Internal server error occurred',
    message: config.nodeEnv === 'development' ? err.message : undefined,
  });
});

server.listen(config.port, () => {
  console.log(`=======================================================`);
  console.log(`🚀 CampusConnect Production Backend Running on port ${config.port}`);
  console.log(`🔒 Environment: ${config.nodeEnv}`);
  console.log(`🛡️  RBAC & Super Admin Security Enforcement Active`);
  console.log(`📡 WebSocket Gateway Operational`);
  console.log(`=======================================================`);
});

process.on('SIGTERM', async () => {
  console.log('SIGTERM received. Closing HTTP server and database pool...');
  server.close(async () => {
    await prisma.$disconnect();
    process.exit(0);
  });
});
