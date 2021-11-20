package am.ik.lab.jwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests(authorizeRequests ->
                authorizeRequests
                        .mvcMatchers("/oauth/token").permitAll()
                        .antMatchers(HttpMethod.GET, "/todos/**").hasAuthority("SCOPE_todo:read")
                        .antMatchers(HttpMethod.POST, "/todos/**").hasAuthority("SCOPE_todo:write")
                        .antMatchers(HttpMethod.PUT, "/todos/**").hasAuthority("SCOPE_todo:write")
                        .antMatchers(HttpMethod.DELETE, "/todos/**").hasAuthority("SCOPE_todo:write")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(r -> r.jwt())
                .csrf(csrf -> csrf.disable())
                .cors();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("demo").password("{noop}demo").roles("USER");
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
