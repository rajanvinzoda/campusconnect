import { Router, Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { authenticateToken } from '../../middleware/auth.middleware.js';

export const createChatRouter = (prisma: PrismaClient, ioBroadcast?: (channelId: string, event: string, data: any) => void) => {
  const router = Router();
  router.use(authenticateToken(prisma));

  // Get user channels
  router.get('/channels', async (req: Request, res: Response): Promise<void> => {
    try {
      const channels = await prisma.chatChannel.findMany({
        include: {
          messages: {
            take: 1,
            orderBy: { createdAt: 'desc' },
          },
        },
      });

      const formatted = channels.map((c) => ({
        id: c.id,
        name: c.name,
        isGroup: c.isGroup,
        category: c.category,
        lastMessage: c.messages[0]?.text || 'No messages yet',
        lastMessageTime: c.messages[0]?.createdAt.getTime() || c.createdAt.getTime(),
        unreadCount: 0,
        isOnline: true,
      }));

      res.json({ channels: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch channels', details: err.message });
    }
  });

  // Get channel messages
  router.get('/channels/:id/messages', async (req: Request, res: Response): Promise<void> => {
    try {
      const channelId = req.params.id;
      const channel = await prisma.chatChannel.findUnique({ where: { id: channelId } });
      if (!channel) {
        res.status(404).json({ error: 'Chat channel not found' });
        return;
      }

      const messages = await prisma.chatMessage.findMany({
        where: { channelId },
        orderBy: { createdAt: 'asc' },
        include: { sender: { select: { id: true, name: true } } },
      });

      const formatted = messages.map((m) => ({
        id: m.id,
        channelId: m.channelId,
        senderId: m.senderId,
        senderName: m.sender.name,
        text: m.text,
        timestamp: m.createdAt.getTime(),
        isVoiceNote: m.isVoiceNote,
        voiceDurationSec: m.voiceDurationSec,
        attachmentUrl: m.attachmentUrl,
      }));

      res.json({ messages: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to fetch messages', details: err.message });
    }
  });

  // Send message
  router.post('/channels/:id/messages', async (req: Request, res: Response): Promise<void> => {
    try {
      const channelId = req.params.id;
      const channel = await prisma.chatChannel.findUnique({ where: { id: channelId } });
      if (!channel) {
        res.status(404).json({ error: 'Chat channel not found' });
        return;
      }

      const { text, isVoiceNote, voiceDurationSec, attachmentUrl } = req.body;

      if (!text && !attachmentUrl) {
        res.status(400).json({ error: 'Message content or attachment required' });
        return;
      }

      const msg = await prisma.chatMessage.create({
        data: {
          channelId,
          senderId: req.user!.id,
          text: text || '',
          isVoiceNote: !!isVoiceNote,
          voiceDurationSec: voiceDurationSec || 0,
          attachmentUrl: attachmentUrl || null,
        },
        include: { sender: { select: { id: true, name: true } } },
      });

      const formatted = {
        id: msg.id,
        channelId: msg.channelId,
        senderId: msg.senderId,
        senderName: msg.sender.name,
        text: msg.text,
        timestamp: msg.createdAt.getTime(),
        isVoiceNote: msg.isVoiceNote,
        voiceDurationSec: msg.voiceDurationSec,
        attachmentUrl: msg.attachmentUrl,
      };

      if (ioBroadcast) {
        ioBroadcast(channelId, 'chat:message', formatted);
      }

      res.status(201).json({ message: formatted });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to send message', details: err.message });
    }
  });

  return router;
};
