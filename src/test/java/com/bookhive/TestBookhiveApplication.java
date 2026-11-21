package com.bookhive;

import com.bookhive.support.TestFixtures;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

/**
 * Runs the whole website on an in-memory H2 database with the test fixtures,
 * so the UI can be tried without installing MySQL:
 *
 * <pre>mvn spring-boot:test-run</pre>
 *
 * Then open http://localhost:8080 and sign in with one of the TestFixtures accounts
 * (password {@value TestFixtures#PASSWORD}). Data is lost when the app stops.
 */
public class TestBookhiveApplication {

    public static void main(String[] args) {
        String[] withProfile = Arrays.copyOf(args, args.length + 1);
        withProfile[args.length] = "--spring.profiles.active=test";
        SpringApplication.from(BookhiveApplication::main)
            .with(DemoData.class)
            .run(withProfile);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import(TestFixtures.class)
    static class DemoData {
        @Bean
        ApplicationRunner seedDemoData(TestFixtures fixtures) {
            return args -> fixtures.seed();
        }
    }
}
