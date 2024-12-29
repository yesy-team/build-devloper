package com.yesy.team.build_devloper.security.custom.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yesy.team.build_devloper.security.exception.EmailAlreadyExistsException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // 예외 메시지를 추출
        String errorMessage;

        if (exception instanceof EmailAlreadyExistsException) {
            errorMessage = exception.getMessage();  // 이메일 중복 메시지 사용
        } else {
            errorMessage = "알 수 없는 이유로 로그인에 실패했습니다.";
        }

        // JSON 형식으로 응답
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태 코드
        response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of(
                "status", "failure",
                "message", errorMessage
        )));
    }
}
