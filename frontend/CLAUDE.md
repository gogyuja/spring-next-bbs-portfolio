# frontend/CLAUDE.md

> 프론트엔드(TypeScript/Next.js) 코드 규칙. FE 파일을 작업할 때만 로드된다.
> 전체 공통 규칙은 루트 CLAUDE.md 참조.

---

## 네이밍

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 컴포넌트 | PascalCase | `PostCard.tsx` |
| 훅 | camelCase, `use` 접두 | `usePostList.ts` |
| 일반 함수/변수 | camelCase | `formatDate` |
| 상수 | UPPER_SNAKE | `MAX_TITLE_LENGTH` |
| 타입/인터페이스 | PascalCase | `PostDetailResponse` |
| 폴더 | kebab-case 또는 atomic 계층명 | `post-list/`, `atoms/` |

---

## Atomic Design 배치 기준

| 계층 | 기준 | 예 |
| --- | --- | --- |
| atoms | 더 못 쪼갬 | Button, Input, Avatar |
| molecules | atoms 2~3 조합 | SearchBar, CommentInput |
| organisms | 독립 영역 | Header, PostCard, CommentList |
| templates | 레이아웃만 (데이터 없음) | BoardLayout |
| pages | 데이터 주입 (App Router) | app/boards/page.tsx |

---

## 핵심 규칙

- **API 타입은 손으로 쓰지 않는다.** `openapi.yaml` → `openapi-typescript`로 생성된 `types/api.ts`만 사용
- 서버 컴포넌트가 기본. `'use client'`는 상태/이벤트/브라우저 API가 필요할 때만
- 서버 상태 = React Query, 클라이언트 상태 = Zustand. **둘을 섞지 않음**
- React Query 키: `['도메인', 식별자]` — `['posts', id]`, `['posts', { boardId, page }]`
- API 호출은 `hooks/`의 useQuery/useMutation 래퍼를 통해서만. 컴포넌트에서 axios 직접 호출 금지
- 폼은 React Hook Form + Zod. 유효성 스키마는 별도 파일로 분리
- `any` 금지. 모르면 `unknown` 후 좁히기

---

## 폴더 구조

```
src/
├── app/             # App Router (폴더=URL)
├── components/
│   ├── atoms/ molecules/ organisms/ templates/
├── hooks/           # useQuery/useMutation 래퍼
├── lib/             # axios 인스턴스(인터셉터: 토큰 첨부 + 401 재발급), 설정
├── stores/          # Zustand
├── types/           # api.ts (openapi 자동 생성 — 수정 금지)
└── mocks/           # MSW 핸들러
```

---

## import 순서

```
1. 외부 라이브러리 (react, next, 외부 패키지)
2. 내부 절대경로 (@/components, @/hooks, @/lib)
3. 상대경로 (./ ../)
4. 타입 import
5. 스타일
```

---

## 도구 (자동 강제)

`.editorconfig` (TS): UTF-8, LF, 들여쓰기 2칸.

`.prettierrc`:

```json
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "printWidth": 100,
  "trailingComma": "all",
  "arrowParens": "always"
}
```

ESLint: `next/core-web-vitals` + `@typescript-eslint`. 주요 규칙

```
@typescript-eslint/no-explicit-any: error
@typescript-eslint/no-unused-vars: error
no-console: warn (error/warn 제외)
import/order: 위 import 순서 강제
```

`tsconfig.json`: `"strict": true` 필수

커밋 전 자동 검사 — Husky + lint-staged:

```json
"lint-staged": { "*.{ts,tsx}": ["eslint --fix", "prettier --write"] }
```
