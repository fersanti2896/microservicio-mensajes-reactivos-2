package com.aleph2.app;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.aleph2.app.handler.Manejador;

@Configuration
public class RouterFunctionConfig {
	@Bean
	public RouterFunction<ServerResponse> routes(Manejador handler){
		return route(GET("/mensaje"), handler::obtenerMensaje)
			   .andRoute(POST("/almacenarArchivo"), handler::almacenarArchivo);
	}
}
