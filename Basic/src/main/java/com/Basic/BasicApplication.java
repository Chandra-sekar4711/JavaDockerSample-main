package com.Basic;

import com.Basic.Config.AwsSecretsLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication

public class BasicApplication {

	public static void main(String[] args) {
		AwsSecretsLoader.loadSecrets();
		SpringApplication.run(BasicApplication.class, args);
	}

}
