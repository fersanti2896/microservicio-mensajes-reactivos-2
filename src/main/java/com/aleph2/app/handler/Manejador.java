package com.aleph2.app.handler;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.aleph2.app.entities.Hacha;
import com.aleph2.app.entities.Mensajes;

import reactor.core.publisher.Mono;

@Component
public class Manejador {
	@Value("${ruta.archivos}")
	private String pathOrigen;

	@Value("${ruta.archivos.destino}")
	private String pathDestino;
	
	public Mono<ServerResponse> obtenerMensaje(ServerRequest request) {
		Mensajes mensaje = new Mensajes();
		mensaje.setMensaje("Hola Mundo Reactivo 2");

		Mono<Mensajes> mensajeMono = Mono.just(mensaje);
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mensajeMono, Mensajes.class);
	}

	public Mono<ServerResponse> almacenarArchivo(ServerRequest request) {

		Mono<Mensajes> mensajeMono = request.multipartData().map(multipart -> multipart.toSingleValueMap().get("file"))
				.cast(FilePart.class).flatMap(file -> {
					file.transferTo(new File(pathOrigen + file.filename()));
					
					try {
						/*128*1024*1024*/
						Hacha hacha = new Hacha(pathOrigen, pathDestino, 35*1024*1024, file.filename());
						hacha.crearCortes();
					} catch (IOException e) {
						Logger.getLogger(Hacha.class.getName()).log(Level.SEVERE, null, e);
					} 
					Mensajes mensaje = new Mensajes();
					mensaje.setMensaje("El nombre del archivo recibido en el server 2 fue: " + file.filename());

					return Mono.just(mensaje);
				});

		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mensajeMono, Mensajes.class);

	}

}
