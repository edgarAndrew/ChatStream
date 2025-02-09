package com.chatstream.core.storageService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication(
		exclude = {
				// Exclude these if you're configuring MongoDB manually
				MongoReactiveDataAutoConfiguration.class,
				MongoReactiveRepositoriesAutoConfiguration.class
		}
)
@EnableReactiveMongoRepositories
@EnableScheduling
public class StorageServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(StorageServiceApplication.class, args);
	}

	// If you need any beans that should be created at startup, add them here
	// For example:
    /*
    @Bean
    public CommandLineRunner initializeConsumerGroups(ReactiveRedisTemplate<String, String> redisTemplate) {
        return args -> {
            // Initialize your Redis consumer groups here if needed
        };
    }
    */

}
