package com.zagbor.bookingservice.config;

import com.zagbor.bookingservice.rep.UserRep;
import com.zagbor.bookingservice.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRep repo, UserService userService) {
        return args -> {
            // создаём администратора, если его нет
            repo.findByUsername("admin").orElseGet(() ->
                    userService.createUser("admin", "admin", true));

            // создаём обычного пользователя, если его нет
            repo.findByUsername("user").orElseGet(() ->
                    userService.createUser("user", "user", false));
        };
    }
}
