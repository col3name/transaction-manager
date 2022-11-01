package com.anti.fraud.system

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@SpringBootApplication
class AntiFraudSystemApplication {
    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain? {
        http.csrf().disable()
        return http.build()
    }
//    @Bean
//    @Throws(Exception::class)
//    fun configure(http: HttpSecurity) {
//        http.httpBasic()
//            .authenticationEntryPoint(restAuthenticationEntryPoint) // Handles auth error
//            .and()
//            .csrf().disable().headers().frameOptions().disable() // for Postman, the H2 console
//            .and()
//            .authorizeRequests() // manage access
//            .antMatchers(HttpMethod.POST, "/api/auth/user").permitAll()
//            .antMatchers("/actuator/shutdown").permitAll() // needs to run test
//            // other matchers
//            .and()
//            .sessionManagement()
//            .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // no session
//    }
}

fun main(args: Array<String>) {
    runApplication<AntiFraudSystemApplication>(*args)
}
