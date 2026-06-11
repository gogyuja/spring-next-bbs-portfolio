interface SocialLoginButtonProps {
  provider: 'kakao' | 'google';
  href: string;
}

const PROVIDER_STYLE = {
  kakao: 'bg-yellow-400 hover:bg-yellow-500 text-black',
  google: 'bg-white hover:bg-gray-100 text-gray-700 border border-gray-300',
};

const PROVIDER_LABEL = {
  kakao: '카카오로 로그인',
  google: 'Google로 로그인',
};

export default function SocialLoginButton({ provider, href }: SocialLoginButtonProps) {
  return (
    <a
      href={href}
      className={`flex items-center justify-center w-full py-3 px-4 rounded-lg font-medium transition-colors ${PROVIDER_STYLE[provider]}`}
    >
      {PROVIDER_LABEL[provider]}
    </a>
  );
}
