import { render, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const { mockReplace, mockPost } = vi.hoisted(() => ({
  mockReplace: vi.fn(),
  mockPost: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
  useSearchParams: vi.fn(),
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (s: { setAuth: ReturnType<typeof vi.fn> }) => unknown) =>
    selector({ setAuth: vi.fn() }),
}));

vi.mock('@/lib/axios', () => ({
  default: { post: mockPost },
}));

import { useSearchParams } from 'next/navigation';
import CallbackPage from '../page';

beforeEach(() => {
  mockReplace.mockClear();
  mockPost.mockClear();
});

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe('CallbackPage', () => {
  it('code가 없으면 /login으로 이동한다', async () => {
    vi.mocked(useSearchParams).mockReturnValue(
      new URLSearchParams('') as unknown as ReturnType<typeof useSearchParams>,
    );
    renderWithQuery(<CallbackPage />);
    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith('/login'));
  });

  it('유효한 code로 교환 성공 시 /로 이동한다', async () => {
    vi.mocked(useSearchParams).mockReturnValue(
      new URLSearchParams('code=valid-code') as unknown as ReturnType<typeof useSearchParams>,
    );
    mockPost.mockResolvedValueOnce({
      data: { success: true, data: { accessToken: 'test.jwt.token' }, message: '토큰 발급 성공' },
    });

    renderWithQuery(<CallbackPage />);
    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith('/'));
  });

  it('교환 실패 시 /login?error=token_exchange_failed로 이동한다', async () => {
    vi.mocked(useSearchParams).mockReturnValue(
      new URLSearchParams('code=invalid-code') as unknown as ReturnType<typeof useSearchParams>,
    );
    mockPost.mockRejectedValueOnce(new Error('400'));

    renderWithQuery(<CallbackPage />);
    await waitFor(() =>
      expect(mockReplace).toHaveBeenCalledWith('/login?error=token_exchange_failed'),
    );
  });
});
