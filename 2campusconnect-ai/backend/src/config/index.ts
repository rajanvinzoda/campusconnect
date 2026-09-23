import dotenv from 'dotenv';
dotenv.config();

const isProduction = (process.env.NODE_ENV || 'development') === 'production';

export const config = {
  port: parseInt(process.env.PORT || '4000', 10),
  nodeEnv: process.env.NODE_ENV || 'development',
  databaseUrl: process.env.DATABASE_URL || '',
  jwt: {
    accessSecret: process.env.JWT_ACCESS_SECRET || (isProduction ? '' : 'campus_dev_access_secret_development_only'),
    refreshSecret: process.env.JWT_REFRESH_SECRET || (isProduction ? '' : 'campus_dev_refresh_secret_development_only'),
    accessExpiresIn: '15m',
    refreshExpiresIn: '7d',
  },
  bootstrapSecret: process.env.BOOTSTRAP_SECRET || '',
  geminiApiKey: process.env.GEMINI_API_KEY || '',
  corsOrigin: process.env.CORS_ORIGIN || (isProduction ? 'https://campusconnect.edu' : '*'),
};
