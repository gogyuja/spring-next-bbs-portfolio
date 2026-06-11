import { create } from 'zustand';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;
  role: string;
}

interface AuthState {
  accessToken: string | null;
  userId: number | null;
  role: string | null;
  setAuth: (token: string) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  userId: null,
  role: null,
  setAuth: (token) => {
    const payload = jwtDecode<JwtPayload>(token);
    set({ accessToken: token, userId: Number(payload.sub), role: payload.role });
  },
  clearAuth: () => set({ accessToken: null, userId: null, role: null }),
}));
