package com.lifecycles.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration
// @EnableWebSecurity
public class OAuth2LoginSecurityConfig {
  @Bean
  public ClientRegistrationRepository clientRegistrationRepository() {
    return new InMemoryClientRegistrationRepository(this.googleClientRegistration());
  }

  private ClientRegistration googleClientRegistration() {
    return ClientRegistration.withRegistrationId("google")
      .clientId("777231217858-hn9eqo5fq7iq68vuahc9irtujsto9g15.apps.googleusercontent.com")
      .clientSecret("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")
      .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .redirectUri("http://localhost:8080/login/oauth2/code/google")
      .scope("openid", "profile", "email", "address", "phone")
      .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
      .tokenUri("https://www.googleapis.com/oauth2/v4/token")
      .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
      .userNameAttributeName(IdTokenClaimNames.SUB)
      .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
      .clientName("Google")
      .build();
  }

  // not mandatory

  // @Bean
  // public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  //   http
  //       .authorizeRequests(authorize -> authorize
  //         .anyRequest().authenticated()
  //   )
  //   .oauth2Login(Customizer.withDefaults());
  //   return http.build();
  // }
}
