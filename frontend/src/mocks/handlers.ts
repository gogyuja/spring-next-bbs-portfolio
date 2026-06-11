import { http, HttpResponse } from 'msw';

export const handlers = [
  http.post('*/api/auth/token', async ({ request }) => {
    const body = (await request.json()) as { code: string };
    if (body.code === 'valid-code') {
      return HttpResponse.json({
        success: true,
        data: { accessToken: 'test.jwt.token' },
        message: '토큰 발급 성공',
      });
    }
    return new HttpResponse(null, { status: 400 });
  }),
];
