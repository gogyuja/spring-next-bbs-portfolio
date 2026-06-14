import { useMutation } from '@tanstack/react-query';
import api from '@/lib/axios';
import { useAuthStore } from '@/stores/authStore';

interface TokenResponse {
  success: boolean;
  data: { accessToken: string };
  message: string;
}

export function useExchangeToken() {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: (code: string) =>
      api.post<TokenResponse>('/api/auth/token', { code }).then((r) => r.data),
    onSuccess: (data) => {
      setAuth(data.data.accessToken);
    },
  });
}
