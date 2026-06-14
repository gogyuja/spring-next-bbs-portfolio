// AUTH-001: 모든 경로 통과. AUTH-002/003에서 보호 라우트 추가.
export { auth as middleware } from '@/auth';
export const config = { matcher: [] };
