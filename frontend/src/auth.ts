import NextAuth from 'next-auth';

export const { auth, handlers, signIn, signOut } = NextAuth({
  providers: [],
  secret: process.env.AUTH_SECRET,
  // AUTH-002/003에서 Zustand AT ↔ next-auth 세션 연동 전략 결정 후 완성
});
