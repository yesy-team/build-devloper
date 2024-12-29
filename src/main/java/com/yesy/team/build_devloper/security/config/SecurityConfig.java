package com.yesy.team.build_devloper.security.config;

import com.yesy.team.build_devloper.member.repository.MemberRepository;
import com.yesy.team.build_devloper.redis.service.RedisRefreshTokenService;
import com.yesy.team.build_devloper.security.custom.*;
import com.yesy.team.build_devloper.security.custom.Service.CustomOAuth2UserService;
import com.yesy.team.build_devloper.security.custom.handler.CustomLogoutHandler;
import com.yesy.team.build_devloper.security.custom.handler.CustomOAuth2FailureHandler;
import com.yesy.team.build_devloper.security.custom.handler.CustomOAuth2SuccessHandler;
import com.yesy.team.build_devloper.security.jwt.JwtAuthenticationFilter;
import com.yesy.team.build_devloper.security.jwt.JwtUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity(debug = true)  // 개발 단계이므로 디버깅 모드 활성화
public class SecurityConfig {

    @Value("${spring.application.base-url}")
    private String baseUrl;

    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenService redisRefreshTokenService; // Redis 서비스 추가
    private final OAuth2AuthorizedClientRepository authorizedClientRepository; // OAuth2 클라이언트 리포지토리 추가
    private final MemberRepository memberRepository;

    // 생성자 주입을 통한 의존성 주입
    @Autowired
    public SecurityConfig(CustomOAuth2SuccessHandler customOAuth2SuccessHandler, CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                          @Lazy CustomOAuth2UserService customOAuth2UserService,
                          JwtUtil jwtUtil,
                          @Lazy RedisRefreshTokenService redisRefreshTokenService,
                          @Lazy OAuth2AuthorizedClientRepository authorizedClientRepository, MemberRepository memberRepository) {
        this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customOAuth2UserService = customOAuth2UserService;
        this.jwtUtil = jwtUtil;
        this.redisRefreshTokenService = redisRefreshTokenService;
        this.authorizedClientRepository = authorizedClientRepository;
        this.memberRepository = memberRepository;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomOAuth2FailureHandler customOAuth2FailureHandler) throws Exception {
        http
                .headers(headers -> headers
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                )
                .cors(Customizer.withDefaults())  // CORS 설정
//                .cors(AbstractHttpConfigurer::disable)  // CORS 비활성화
                .csrf(AbstractHttpConfigurer::disable)  // CSRF 비활성화
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()  // FORWARD 요청은 인증 없이 허용
                                .requestMatchers("/actuator/health").permitAll() // 서버 검증 확인.
                                .requestMatchers("/", "/**", "/static/**", "/favicon.ico", "/oauth2/**","/login/oauth2/**",
                                        "\"/api/oauth2/callback\"").permitAll()  // React 경로 추가
                                .requestMatchers("/login").anonymous() // 로그인되지 않은 사용자만 접근 가능
                                .requestMatchers("/api/auth/refresh", "/oauth2/**", "/oauth2/authorization/**").permitAll()
                                .requestMatchers("/static/**", "/media/**", "/js/**", "/css/**", "/img/**", "/fontawesome-free-6.5.1-web/**", "/particle.png").permitAll()
                                .requestMatchers("/api/**", "/auth/**").authenticated() // API 경로는 명확히 구분
                                .requestMatchers("/api/location/save").authenticated()
                                .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfoEndpoint ->
                                userInfoEndpoint.userService(customOAuth2UserService)
                        )
                        .successHandler(customOAuth2SuccessHandler)
                )
                // 특정 경로에만 JwtAuthenticationFilter 추가
                .addFilterBefore(
                        new JwtAuthenticationFilter(authenticationManager(http.getSharedObject(AuthenticationConfiguration.class)), jwtUtil, memberRepository) {
                            @Override
                            protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
                                // 필터링을 제외할 경로 정의
                                String path = request.getRequestURI();
                                return path.startsWith("/oauth2/**");
                            }
                        },
                        LogoutFilter.class
                )
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // JWT 인증 시 무상태 설정
                )
                .logout(logout -> logout
                        .logoutUrl("/api/member/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK); // 성공 상태 반환
                            response.getWriter().write("Logout successful");
                        })
                        .addLogoutHandler(customLogoutHandler(authorizedClientRepository, redisRefreshTokenService)) // 제대로 된 핸들러 등록
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())  // Spring이 제공하는 공통 정적 리소스 경로
                .requestMatchers("/static/**", "/js/**", "/css/**", "/img/**", "/index.html", "/react/**");        // 추가적으로 정의한 정적 리소스 경로
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public CustomLogoutHandler customLogoutHandler(@Lazy OAuth2AuthorizedClientRepository authorizedClientRepository,
                                                   @Lazy RedisRefreshTokenService redisRefreshTokenService) {
        return new CustomLogoutHandler(authorizedClientRepository, redisRefreshTokenService);
    }
}