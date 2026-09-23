import { Server as SocketIOServer, Socket } from 'socket.io';
import jwt from 'jsonwebtoken';
import { PrismaClient, UserRole } from '@prisma/client';
import { config } from '../config/index.js';

interface AuthenticatedSocket extends Socket {
  user?: {
    id: string;
    email: string;
    name: string;
    role: UserRole;
  };
}

export const setupSocketGateway = (io: SocketIOServer, prisma: PrismaClient) => {
  // Authenticate socket connections using JWT
  io.use(async (socket: AuthenticatedSocket, next) => {
    try {
      const token =
        socket.handshake.auth?.token ||
        (socket.handshake.headers.authorization?.startsWith('Bearer ')
          ? socket.handshake.headers.authorization.split(' ')[1]
          : null);

      if (!token) {
        return next(new Error('Authentication error: Missing token'));
      }

      const decoded = jwt.verify(token, config.jwt.accessSecret) as { userId: string };
      const user = await prisma.user.findUnique({
        where: { id: decoded.userId },
        select: { id: true, email: true, name: true, role: true, status: true },
      });

      if (!user || user.status !== 'ACTIVE') {
        return next(new Error('Authentication error: User inactive or unauthorized'));
      }

      socket.user = user;
      next();
    } catch (err: any) {
      next(new Error(`Authentication failed: ${err.message}`));
    }
  });

  io.on('connection', (socket: AuthenticatedSocket) => {
    const user = socket.user!;
    console.log(`[Socket] User connected: ${user.name} (${user.id}) [${user.role}]`);

    // Join private user room for targeted notifications
    socket.join(`user_${user.id}`);

    // Broadcast user online status
    socket.broadcast.emit('presence:update', {
      userId: user.id,
      name: user.name,
      isOnline: true,
    });

    // Join chat channel with verification
    socket.on('channel:join', async ({ channelId }) => {
      if (!channelId || typeof channelId !== 'string') return;
      try {
        const channel = await prisma.chatChannel.findUnique({ where: { id: channelId } });
        if (channel) {
          socket.join(`channel_${channelId}`);
          console.log(`[Socket] ${user.name} joined channel_${channelId}`);
        }
      } catch (err) {
        console.error('[Socket] channel:join error:', err);
      }
    });

    // Leave chat channel
    socket.on('channel:leave', ({ channelId }) => {
      socket.leave(`channel_${channelId}`);
    });

    // Typing indicators
    socket.on('typing:start', ({ channelId }) => {
      socket.to(`channel_${channelId}`).emit('typing:status', {
        channelId,
        userId: user.id,
        userName: user.name,
        isTyping: true,
      });
    });

    socket.on('typing:stop', ({ channelId }) => {
      socket.to(`channel_${channelId}`).emit('typing:status', {
        channelId,
        userId: user.id,
        userName: user.name,
        isTyping: false,
      });
    });

    socket.on('disconnect', () => {
      console.log(`[Socket] User disconnected: ${user.name} (${user.id})`);
      socket.broadcast.emit('presence:update', {
        userId: user.id,
        name: user.name,
        isOnline: false,
      });
    });
  });
};
