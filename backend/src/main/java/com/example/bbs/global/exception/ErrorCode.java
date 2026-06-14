package com.example.bbs.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  OTC_INVALID(HttpStatus.BAD_REQUEST, "코드가 없거나 만료되었습니다"),
  LOGIN_BANNED(HttpStatus.FORBIDDEN, "로그인이 금지된 계정입니다"),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류");

  private final HttpStatus status;
  private final String message;
}
