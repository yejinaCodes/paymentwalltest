package org.example.paymentwalltest_back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//security는 일단 pingback 해결하고 해보기

//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        return http
//                .csrf(csrf -> csrf
//                        .ignoringAntMatchers("/pingback")
//                )
//                .authorizeRequests(auth -> auth
//                        .antMatchers("/api/**").hasOrigin("http://localhost:5173")
//                        .antMatchers("/pingback").hasIpAddress("216.127.71.0/24")
//                        .anyRequest().denyAll()  // 그 외 모든 요청 거부
//                )
//                .build();
//    }
//}