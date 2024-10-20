package com.supply.config;

import com.supply.filter.JwtAuthenticationTokenFilter;
import com.supply.handler.AccessDeniedHandlerImpl;
import com.supply.handler.AuthenticationEntryPointImpl;
import com.supply.service.impl.DrugAuthenticationProvider;
import com.supply.service.DrugUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    private final AccessDeniedHandlerImpl accessDeniedHandler;

    private final AuthenticationEntryPointImpl authenticationEntryPoint;

    private final DrugUserDetailService drugUserDetailService;

    private final BCryptPasswordEncoder passwordEncoder;

    // 配置自定义的 AuthenticationProvider
    @Bean
    public AuthenticationProvider drugAuthenticationProvider() {
        DrugAuthenticationProvider provider = new DrugAuthenticationProvider();
        provider.setDrugUserDetailsService(drugUserDetailService); // 设置自定义的 UserDetailsService
        provider.setPasswordEncoder(passwordEncoder); // 设置密码加密器
        return provider;
    }

    // 手动配置 AuthenticationManager 并注册自定义 AuthenticationProvider
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(drugAuthenticationProvider()); // 注册自定义 provider
        return authenticationManagerBuilder.build();
    }

    // 配置安全过滤链
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护
                .csrf(AbstractHttpConfigurer::disable)
                // 设置会话创建策略为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/user/login").anonymous()
                        .requestMatchers("/user/register").anonymous()
                        .requestMatchers("/user/resetPassword").anonymous()
                        .requestMatchers("/user/verifyCode/send").anonymous()
                        .requestMatchers("/user/reUpload").anonymous()
                        .requestMatchers("/doc.html").anonymous()
                        .requestMatchers("/webjars/**").anonymous()
                        .requestMatchers("/v3/**").anonymous()
                        .requestMatchers("/swagger-resources/**").anonymous()
                        .anyRequest().authenticated()  // 任何其他请求都需要认证
                )
                // 开启跨域访问
                .cors(AbstractHttpConfigurer::disable)
                // 添加自定义过滤器
                .addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // 配置异常处理
                .exceptionHandling(exception -> exception.accessDeniedHandler(accessDeniedHandler).authenticationEntryPoint(authenticationEntryPoint));

        // 构建并返回安全过滤链
        return http.build();
    }
}
