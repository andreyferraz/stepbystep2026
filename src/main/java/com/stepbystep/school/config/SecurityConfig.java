package com.stepbystep.school.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.stepbystep.school.model.Usuario;
import com.stepbystep.school.repository.UsuarioRepository;

@Configuration
public class SecurityConfig {

    @Value("${app.security.redirect.admin:/admin/dashboard}")
    private String adminRedirectPath;

    @Value("${app.security.redirect.aluno:/aluno/dashboard}")
    private String alunoRedirectPath;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler authenticationSuccessHandler) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/escola",
                    "/blog/**",
                    "/galeria",
                    "/livros",
                    "/contato",
                    "/pre-inscricao",
                    "/login",
                    "/acesso-negado",
                    "/api/webhooks/**",
                    "/css/**",
                    "/js/**",
                    "/img/**",
                    "/uploads/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/aluno/**").hasRole("ALUNO")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(authenticationSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/webhooks/**", "/pre-inscricao")
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/acesso-negado")
            );

        return http.build();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String redirectPath = "/";

            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

            boolean isAluno = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ALUNO".equals(authority.getAuthority()));

            if (isAdmin) {
                redirectPath = adminRedirectPath;
            } else if (isAluno) {
                redirectPath = alunoRedirectPath;
            }

            response.sendRedirect(request.getContextPath() + redirectPath);
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return username -> {
            String email = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);

            Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado ou inativo."));

            return User.withUsername(usuario.getEmail())
                .password(usuario.getSenha())
                .authorities("ROLE_" + usuario.getRole().name())
                .build();
        };
    }
}
