package com.tech.BookStore;

import com.tech.BookStore.role.Role;
import com.tech.BookStore.role.RoleRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
public class BookStoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookStoreApplication.class, args);
	}

	@Bean
	public CommandLineRunner runner(RoleRepo repo)
	{
		return args ->
		{
			if(repo.findByName("USER").isEmpty())
			{
				repo.save(
						Role.builder().name("USER").build()
				);
			}
		};
	}

}
