package com.yesy.team.build_devloper;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BuildDevloperApplication {

    public static void main(String[] args) {
        // EnvUtil을 사용하여 .env 파일 로드
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir")) // 프로젝트 루트 디렉토리 기준
                .load();

        // 시스템 프로퍼티에 등록
        System.setProperty("spring.jwt.secret", dotenv.get("JWT_SECRET", "default_secret"));
        System.setProperty("spring.jwt.expiration", dotenv.get("JWT_EXPIRATION", "3600000"));
        System.setProperty("spring.jwt.refresh-expiration", dotenv.get("JWT_REFRESH_EXPIRATION", "604800000"));

        SpringApplication.run(BuildDevloperApplication.class, args);
    }
}
