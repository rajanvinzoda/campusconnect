import { PrismaClient, UserRole, AccountStatus } from '@prisma/client';
import bcrypt from 'bcrypt';

const prisma = new PrismaClient();

async function main() {
  const args = process.argv.slice(2);
  const email = args[0] || 'vinzodarajan@gmail.com';
  const password = args[1] || 'SuperAdmin@Secure2026!';
  const name = args[2] || 'Vinzodarajan (Super Admin)';

  console.log('----------------------------------------------------');
  console.log('🔐 CampusConnect Super Admin CLI Bootstrap Tool');
  console.log(`Target Email: ${email}`);
  console.log('----------------------------------------------------');

  const passwordHash = await bcrypt.hash(password, 12);

  const admin = await prisma.user.upsert({
    where: { email: email.toLowerCase() },
    update: {
      role: UserRole.SUPER_ADMIN,
      status: AccountStatus.ACTIVE,
      passwordHash,
      name,
    },
    create: {
      email: email.toLowerCase(),
      passwordHash,
      name,
      role: UserRole.SUPER_ADMIN,
      status: AccountStatus.ACTIVE,
      department: 'University Administration',
      branch: 'Executive Security & System Operations',
      bio: 'Platform Owner & Primary Super Administrator.',
      isVerified: true,
    },
  });

  await prisma.auditLog.create({
    data: {
      actorId: admin.id,
      action: 'SUPER_ADMIN_CLI_BOOTSTRAPPED',
      targetType: 'SYSTEM',
      targetId: admin.id,
      ipAddress: '127.0.0.1 (CLI)',
      metadata: { email: admin.email, role: admin.role },
    },
  });

  console.log('✅ Super Admin account successfully created/updated:');
  console.log(`   ID:    ${admin.id}`);
  console.log(`   Name:  ${admin.name}`);
  console.log(`   Email: ${admin.email}`);
  console.log(`   Role:  ${admin.role}`);
  console.log('----------------------------------------------------');
}

main()
  .catch((e) => {
    console.error('❌ Super Admin bootstrap failed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
