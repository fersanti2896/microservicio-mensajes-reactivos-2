package com.aleph2.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication
public class MicroservicioMensajesReactivos2Application {

	public static void main(String[] args) {
		SpringApplication.run(MicroservicioMensajesReactivos2Application.class, args);
	}

}
