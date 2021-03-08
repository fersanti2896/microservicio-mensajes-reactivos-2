package com.aleph2.app.entities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;

public class Hacha {
	private String ruta;
	private int UMA;
	private String rutaOrigen;
	private String rutaDestino;
	private String nombreArchivo;
	private String nombreCorte;

	public Hacha() {
	}

	public Hacha(String rutaOrigen, String rutaDestino, int UMA, String nombreArchivo) {
		this.UMA = UMA;
		this.rutaOrigen = rutaOrigen;
		this.rutaDestino = rutaDestino;
		this.nombreArchivo = nombreArchivo;
	}

	public String getRutaDestino() {
		return rutaDestino;
	}

	public void setRutaDestino(String rutaDestino) {
		this.rutaDestino = rutaDestino;
	}

	public String getRutaOrigen() {
		return rutaOrigen;
	}

	public void setRutaOrigen(String rutaOrigen) {
		this.rutaOrigen = rutaOrigen;
	}

	/**
	 * Metodo que se encarga de realizar los cortes de un archivo dependiendo del
	 * tamanio del buffer.
	 *
	 * @throws java.io.IOException
	 */
	public void crearCortes() throws IOException {
		/* Inicia algoritmo de compresion LZ4 */
		InputStream in = Files.newInputStream(Paths.get(rutaOrigen + nombreArchivo));
		OutputStream fout = Files.newOutputStream(Paths.get(rutaDestino + nombreArchivo + ".lz4"));
		BufferedOutputStream out = new BufferedOutputStream(fout);
		FramedLZ4CompressorOutputStream lzOut = new FramedLZ4CompressorOutputStream(out);
		final byte[] buffer = new byte[1024];
		int n = 0;
		while (-1 != (n = in.read(buffer))) {
			lzOut.write(buffer, 0, n);
		}

		lzOut.close();
		in.close();

		/* Una vez que se comprimio, se le pasa la ruta para procesarla con Hacha */
		Path pathArchivoOriginal = Paths.get(rutaDestino + nombreArchivo + ".lz4");

		// El UMA se usa para definir el tamanio del buffer que obtendra datos del
		// disco duro.
		ByteBuffer byteBuffer = ByteBuffer.allocate(UMA);
		SeekableByteChannel seekableBCLectura = Files.newByteChannel(pathArchivoOriginal,
				EnumSet.of(StandardOpenOption.READ));

		int bytesLeidos = 0;
		int contadorChunks = 0;

		String nombreCorte = crearNombreCorte(nombreArchivo, File.separator);
		System.out.println("NombreCorte es: " + nombreCorte);

		// Se obtiene la ruta para que sea el lugar donde se guardaran los cortes
		// del archivo original.
		String rutaArchivo = rutaDestino;

		while ((bytesLeidos = seekableBCLectura.read(byteBuffer)) > 0) {

			byteBuffer.flip();

			if (bytesLeidos < UMA) { // Quedan posiciones sin ocupar en el byteBuffer
				byte[] byteArrayRestante = new byte[byteBuffer.limit()];
				byte[] arrayByteBuffer = byteBuffer.array();

				// Con el siguiente ciclo eliminamos los espacios en blanco del byteBuffer
				for (int i = 0; i < byteBuffer.limit(); i++) {
					byteArrayRestante[i] = arrayByteBuffer[i];
				}

				System.out.println("byteArrayRestante tiene un length de: " + byteArrayRestante.length);
				byteBuffer = ByteBuffer.allocate(byteArrayRestante.length);
				byteBuffer.put(byteArrayRestante);
				byteBuffer.flip();
			}

			System.out.println("Parte: " + contadorChunks);

			Path pathCorte = Paths.get(rutaArchivo + File.separator + nombreCorte + "." + contadorChunks);
			SeekableByteChannel seekableBCEscritura = Files.newByteChannel(pathCorte,
					EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE));
			seekableBCEscritura.write(byteBuffer);
			contadorChunks++;
			byteBuffer.clear(); // Prepara el buffer para la siguiente lectura
			seekableBCEscritura.close();
		}
	}

	/**
	 * Metodo que permitira recuperar el archivo original a partir de todos sus
	 * cortes.
	 *
	 * @throws IOException
	 */
	public void unirCortes() throws IOException {

		Path pathOrigen = Paths.get(rutaOrigen);

		String soloNombreArchivoCorte = obtenerNombreSinExtension(nombreArchivo, ".");

		DirectoryStream<Path> ds = Files.newDirectoryStream(pathOrigen, soloNombreArchivoCorte + ".[0-9]*");
		Iterator<Path> iteraPaths = ds.iterator();

		ArrayList<String> listaCortes = new ArrayList<>();

		// El siguiente Comparator, se crea a la medida para ordenar cada uno de los
		// fragmentos creados por el hacha, para la reconstruccion correcta de una
		// archivo.
		Comparator<String> comparador = (String cadena1, String cadena2) -> {
			int numeroCorte = Integer.parseInt(cadena1.substring(cadena1.lastIndexOf(".") + 1));
			int numeroCorte2 = Integer.parseInt(cadena2.substring(cadena2.lastIndexOf(".") + 1));

			return new Integer(numeroCorte).compareTo(numeroCorte2);
		};

		while (iteraPaths.hasNext()) {
			listaCortes.add(iteraPaths.next().getFileName().toString());
		}

		listaCortes.sort(comparador);

		ByteBuffer byteBufferLectura = ByteBuffer.allocate(UMA);
		// String nombreArchivoOriginal = unirNombreArchivo(nombreArchivo, ".", "_");
		String nombreArchivoOriginal = unirNombreArchivo(nombreArchivo, ".");
		System.out.println("nombreArchivoOriginal es: " + nombreArchivoOriginal);

		Path pathDestino = Paths.get(rutaDestino, nombreArchivoOriginal);
		SeekableByteChannel seekableBCEscritura = Files.newByteChannel(pathDestino,
				EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE));

		for (String nombreChunk : listaCortes) {
			Path pathCorte = Paths.get(rutaOrigen, nombreChunk);
			SeekableByteChannel seekableBCLectura = Files.newByteChannel(pathCorte,
					EnumSet.of(StandardOpenOption.READ));

			int bytesLeidos = seekableBCLectura.read(byteBufferLectura);
			byteBufferLectura.flip();

			if (bytesLeidos < UMA) { // Quedan posiciones sin ocupar en el byteBuffer
				byte[] byteArrayRestante = new byte[byteBufferLectura.limit()];
				byte[] arrayByteBuffer = byteBufferLectura.array();

				// Con el siguiente ciclo eliminamos los espacios en blanco del byteBuffer
				for (int indice = 0; indice < byteBufferLectura.limit(); indice++) {
					byteArrayRestante[indice] = arrayByteBuffer[indice];
				}
				System.out.println("byteArrayRestante tiene un length de: " + byteArrayRestante.length);
				byteBufferLectura = ByteBuffer.allocate(byteArrayRestante.length);
				byteBufferLectura.put(byteArrayRestante);
				byteBufferLectura.flip();
			}

			seekableBCEscritura.write(byteBufferLectura);
			byteBufferLectura.clear();
			seekableBCLectura.close(); // Cada lectura debe cerrarse para abrir la siguiente.
		}

		seekableBCEscritura.close();

	}

	/**
	 * Este metodo devuelve el nombre que llevaran cada uno de los cortes (chunks).
	 * 
	 * @param direccionArchivo la ruta que incluye el nombre del archivo.
	 * @param separador1       separador de ruta asignado por el sistema operativo.
	 * @return devuelve el nombre de los dispersos.
	 */
	public String crearNombreCorte(String direccionArchivo, String separador1) {

		StringTokenizer st = new StringTokenizer(direccionArchivo);

		while (st.hasMoreTokens()) {
			nombreArchivo = st.nextToken(separador1);
		}
		System.out.println("El nombreArchivo en el dispersor vale: " + nombreArchivo);

		return nombreArchivo;
	}

	/**
	 * Este metodo permite obtener dinamicamente el nombre del Archivo de la ruta
	 * asignada.
	 *
	 * @param ruta      variable que guarda la ruta seleccionada
	 * @param separador variable que muestra el separador asignado por el sistema
	 *                  operativo.
	 *
	 * @return nombreArchivo variable que contiene solo el nombre del archivo
	 */
	public String obtenerNombreArchivo(String ruta, String separador) {
		String nombreArchivo = null;

		StringTokenizer st = new StringTokenizer(ruta);

		while (st.hasMoreTokens()) {
			nombreArchivo = st.nextToken(separador);
		}
		return nombreArchivo;
	}

	/**
	 * Este metodo permite recrear el nombre del archivo original a partir del
	 * nombre de uno de los archivos de corte (chunks).
	 *
	 * @param nombreArchivo es el nombre de un disperso
	 * @param separador1    separador que debe valer ='.'
	 * @return
	 */
	public String unirNombreArchivo(String nombreArchivo, String separador1) {
		StringTokenizer st = new StringTokenizer(nombreArchivo);

		int posicion = nombreArchivo.lastIndexOf(separador1);
		nombreArchivo = nombreArchivo.substring(0, posicion);

		System.out.println("nombreArchivo en el racuperador es: " + nombreArchivo);

		return nombreArchivo;
	}

	/**
	 * * Metodo para obtener el nombre del archivo sin extension.
	 *
	 * @param nombreCorte nombre del archivo junto con su extension.
	 * @param separador   que contiene seguramente el valor = ".".
	 * @return solo el nombre del archivo.
	 */
	public String obtenerNombreSinExtension(String nombreCorte, String separador) {
		String soloNombreArchivo = null;

		int posicion = nombreCorte.lastIndexOf(separador);

		if (posicion != -1) {
			soloNombreArchivo = nombreCorte.substring(0, posicion);
		}
		return soloNombreArchivo;
	}
}
