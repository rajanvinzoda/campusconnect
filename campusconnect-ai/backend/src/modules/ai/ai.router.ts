import { Router, Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import { authenticateToken } from '../../middleware/auth.middleware.js';
import { config } from '../../config/index.js';

export const createAiRouter = (prisma: PrismaClient) => {
  const router = Router();
  router.use(authenticateToken(prisma));

  // Helper to call Gemini API from server
  const callServerGemini = async (prompt: string): Promise<string> => {
    if (!config.geminiApiKey) {
      return 'AI Academic service is operational. Set GEMINI_API_KEY on the server for live inference.';
    }

    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${config.geminiApiKey}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
      }),
    });

    if (!response.ok) {
      throw new Error(`Gemini API error: ${response.statusText}`);
    }

    const data: any = await response.json();
    return data.candidates?.[0]?.content?.parts?.[0]?.text || 'No response generated.';
  };

  // Quota & Rate Limit Checker
  const checkDailyAiLimit = async (userId: string): Promise<boolean> => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const count = await prisma.aiUsageLog.count({
      where: {
        userId,
        createdAt: { gte: today },
      },
    });

    // Max 50 AI requests per student per day
    return count < 50;
  };

  // 1. Concept Explanations
  router.post('/explain', async (req: Request, res: Response): Promise<void> => {
    try {
      const { prompt } = req.body;
      if (!prompt) {
        res.status(400).json({ error: 'Prompt is required' });
        return;
      }

      const allowed = await checkDailyAiLimit(req.user!.id);
      if (!allowed) {
        res.status(429).json({ error: 'Daily academic AI request limit reached. Resets at midnight.' });
        return;
      }

      const explanation = await callServerGemini(
        `You are a university academic tutor. Explain this topic clearly with theoretical foundations, algorithms or practical engineering examples:\n\n${prompt}`
      );

      await prisma.aiUsageLog.create({
        data: {
          userId: req.user!.id,
          promptType: 'EXPLAIN',
          tokensUsed: prompt.length + explanation.length,
        },
      });

      res.json({ explanation });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to generate explanation', details: err.message });
    }
  });

  // 2. Flashcards
  router.post('/flashcards', async (req: Request, res: Response): Promise<void> => {
    try {
      const { subject } = req.body;
      if (!subject) {
        res.status(400).json({ error: 'Subject is required' });
        return;
      }

      const prompt = `Generate 3 high-yield study flashcards for the academic subject '${subject}'. Format strictly as JSON array of objects with keys 'question' and 'answer'. Output only raw JSON without code blocks or backticks.`;
      let text = await callServerGemini(prompt);
      text = text.trim().replace(/^```json/, '').replace(/^```/, '').replace(/```$/, '').trim();

      let flashcards = [];
      try {
        flashcards = JSON.parse(text);
      } catch (_) {
        flashcards = [
          { question: `What is the core paradigm of ${subject}?`, answer: 'Foundational theory and practical engineering methodologies.' },
          { question: `Name key design principles for ${subject}.`, answer: 'Modularity, abstraction, and separation of concerns.' },
        ];
      }

      await prisma.aiUsageLog.create({
        data: {
          userId: req.user!.id,
          promptType: 'FLASHCARDS',
          tokensUsed: 250,
        },
      });

      res.json({ flashcards });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to generate flashcards', details: err.message });
    }
  });

  // 3. Quiz Questions
  router.post('/quiz', async (req: Request, res: Response): Promise<void> => {
    try {
      const { topic } = req.body;
      if (!topic) {
        res.status(400).json({ error: 'Topic is required' });
        return;
      }

      const prompt = `Generate 2 multiple-choice quiz questions for '${topic}'. Format strictly as a JSON array of objects with keys: 'question' (string), 'options' (array of 4 strings), 'correctIndex' (int 0-3), 'explanation' (string). Output only raw JSON without code fences.`;
      let text = await callServerGemini(prompt);
      text = text.trim().replace(/^```json/, '').replace(/^```/, '').replace(/```$/, '').trim();

      let quiz = [];
      try {
        quiz = JSON.parse(text);
      } catch (_) {
        quiz = [
          {
            question: `Which fundamental principle governs ${topic}?`,
            options: ['Deterministic architecture', 'Unverified random state', 'Monolithic coupling', 'Linear scan without indexing'],
            correctIndex: 0,
            explanation: 'Deterministic architecture guarantees predictable state transitions.',
          },
        ];
      }

      await prisma.aiUsageLog.create({
        data: {
          userId: req.user!.id,
          promptType: 'QUIZ',
          tokensUsed: 350,
        },
      });

      res.json({ quiz });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to generate quiz', details: err.message });
    }
  });

  // 4. Resume Placement Review
  router.post('/resume', async (req: Request, res: Response): Promise<void> => {
    try {
      const { resumeText } = req.body;
      if (!resumeText) {
        res.status(400).json({ error: 'Resume text is required' });
        return;
      }

      const prompt = `You are a university career placement director. Review this student resume text for software and engineering campus placements. Give 3 actionable bullet points with strengths, improvements, and keyword recommendations:\n\n${resumeText}`;
      const review = await callServerGemini(prompt);

      await prisma.aiUsageLog.create({
        data: {
          userId: req.user!.id,
          promptType: 'RESUME_ANALYSIS',
          tokensUsed: resumeText.length + review.length,
        },
      });

      res.json({ review });
    } catch (err: any) {
      res.status(500).json({ error: 'Failed to analyze resume', details: err.message });
    }
  });

  return router;
};
