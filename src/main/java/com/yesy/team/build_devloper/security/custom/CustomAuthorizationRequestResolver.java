package com.yesy.team.build_devloper.security.custom;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;
    private final String baseUrl;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, String baseUrl) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        this.baseUrl = baseUrl;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest defaultRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(defaultRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest defaultRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(defaultRequest);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest request) {
        if (request != null) {
            return OAuth2AuthorizationRequest.from(request)
                    .redirectUri(baseUrl + "/login/oauth2/code/google") // redirect-uri를 명시적으로 설정
                    .build();
        }
        return null;
    }
}
