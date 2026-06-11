package com.example.bbs.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ProblemDetail> handle(BusinessException e) {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(e.getErrorCode().getStatus(), e.getMessage());
    pd.setTitle(e.getErrorCode().name());
    return ResponseEntity.status(e.getErrorCode().getStatus()).body(pd);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handle(Exception e) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류");
    return ResponseEntity.internalServerError().body(pd);
  }
}
