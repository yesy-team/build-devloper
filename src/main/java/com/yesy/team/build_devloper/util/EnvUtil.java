package com.yesy.team.build_devloper.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Component;

@Component
public class EnvUtil {

    private final Dotenv dotenv;

    public EnvUtil() {
        this.dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir")) // 프로젝트 루트 디렉토리 설정
                .load();
    }

    public String get(String key) {
        return dotenv.get(key);
    }
}