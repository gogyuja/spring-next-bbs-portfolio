'use client';

import { useEffect, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useExchangeToken } from '@/hooks/useAuth';

export default function CallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const code = searchParams.get('code');
  const { mutateAsync: exchangeToken, isPending } = useExchangeToken();
  const called = useRef(false);

  useEffect(() => {
    if (called.current) return;
    called.current = true;

    if (!code) {
      router.replace('/login');
      return;
    }
    exchangeToken(code)
      .then(() => router.replace('/'))
      .catch(() => router.replace('/login?error=token_exchange_failed'));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (isPending) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-gray-500">로그인 처리 중...</p>
      </div>
    );
  }

  return null;
}
