import SocialLoginButton from '@/components/atoms/SocialLoginButton';

const API_URL = process.env.NEXT_PUBLIC_API_URL!;

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="w-full max-w-sm space-y-4 rounded-xl bg-white p-8 shadow">
        <h1 className="text-center text-2xl font-bold">로그인</h1>
        <SocialLoginButton
          provider="kakao"
          href={`${API_URL}/oauth2/authorization/kakao`}
        />
        <SocialLoginButton
          provider="google"
          href={`${API_URL}/oauth2/authorization/google`}
        />
      </div>
    </main>
  );
}
