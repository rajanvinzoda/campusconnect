import dotenv from 'dotenv';
dotenv.config();

export const config = {
  port: parseInt(process.env.PORT || '4000', 10),
  nodeEnv: process.env.NODE_ENV || 'development',
  databaseUrl: process.env.DATABASE_URL || '',
  jwt: {
    accessSecret: process.env.JWT_ACCESS_SECRET || 'campus_access_secret_super_secure_key_2026',
    refreshSecret: process.env.JWT_REFRESH_SECRET || 'campus_refresh_secret_super_secure_key_2026',
    accessExpiresIn: '15m',
    refreshExpiresIn: '7d',
  },
  bootstrapSecret: process.env.BOOTSTRAP_SECRET || 'super_admin_bootstrap_secret_init_2026',
  geminiApiKey: process.env.GEMINI_API_KEY || '',
  corsOrigin: process.env.CORS_ORIGIN || '*',
};
