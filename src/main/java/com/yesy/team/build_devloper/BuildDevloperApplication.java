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

        // .env 값을 강제로 Spring Environment에 추가 (이미 존재하는 값은 덮어쓰지 않음)
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        SpringApplication.run(BuildDevloperApplication.class, args);
    }
}