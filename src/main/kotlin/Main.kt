package org.example

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.InputMismatchException
import java.util.Scanner

interface ICoche {
    val id_coche: Int
    val nombre_modelo: String
    val nombre_marca: String
    val consumo: Double
    val HP: Int
}

data class CocheCSV(
    override val id_coche: Int,
    override val nombre_modelo: String,
    override val nombre_marca: String,
    override val consumo: Double,
    override val HP: Int
): ICoche

data class CocheXML(
    @JacksonXmlProperty(localName = "id_coche")
    override val id_coche: Int,
    @JacksonXmlProperty(localName = "nombre_modelo")
    override val nombre_modelo: String,
    @JacksonXmlProperty(localName = "nombre_marca")
    override val nombre_marca: String,
    @JacksonXmlProperty(localName = "consumo")
    override val consumo: Double,
    @JacksonXmlProperty(localName = "HP")
    override val HP: Int
): ICoche

@JacksonXmlRootElement(localName = "coches")
data class CocheXmlRoot(@JacksonXmlElementWrapper(useWrapping = false)
                  @JacksonXmlProperty(localName = "coche")
                  val listaCochesXML: List<CocheXML> = emptyList()
)

@Serializable
data class CocheJSON(
    override val id_coche: Int,
    override val nombre_modelo: String,
    override val nombre_marca: String,
    override val consumo: Double,
    override val HP: Int
): ICoche


data class CocheBinario(
    override val id_coche: Int,
    override val nombre_modelo: String,
    override val nombre_marca: String,
    override val consumo: Double,
    override val HP: Int
): ICoche

const val TAMANO_ID = Int.SIZE_BYTES //4Bytes
const val TAMANO_MODELO = 40 //40bytes
const val TAMANO_MARCA = 40
const val TAMANO_CONSUMO = Double.SIZE_BYTES //8 bytes
const val TAMANO_HP = Int.SIZE_BYTES

const val TAMANO_REGISTRO = TAMANO_ID + TAMANO_MODELO + TAMANO_MARCA + TAMANO_CONSUMO + TAMANO_HP //96 bytes


fun mostrarData(coches: List<ICoche>){
    for (coche in coches) {
        println(" - ID: ${coche.id_coche}, Modelo: ${coche.nombre_modelo}, " +
                "Marca: ${coche.nombre_marca}, Consumo: " +
                "${coche.consumo} caballos: ${coche.HP} ")
    }
}

fun leerCSV(ruta: Path): List<ICoche> {
    var coches: List<ICoche> =emptyList()
    if (!Files.isReadable(ruta)) {
        println("Error: No se puede leer el fichero en la ruta: $ruta")
    } else{
        val reader = csvReader {
            delimiter = ';'
        }
        val filas: List<List<String>> = reader.readAll(ruta.toFile())
        coches = filas.mapNotNull { columnas ->
            if (columnas.size >= 5) {
                try {
                    val id_coche = columnas[0].toInt()
                    val nombre_modelo = columnas[1]
                    val nombre_marca = columnas[2]
                    val consumo = columnas[3].toDouble()
                    val hp = columnas[4].toInt()
                    CocheCSV(id_coche,nombre_modelo, nombre_marca, consumo, hp)
                } catch (e: Exception) {

                    println("Fila inválida ignorada: $columnas -> Error: ${e.message}")
                    null
                }
            } else {
                println("Fila con formato incorrecto ignorada: $columnas")
                null
            }
        }
    }
    return coches
}
fun leerJSON(ruta: Path): List<ICoche> {
    var coches: List<ICoche> =emptyList()
    try {
        val jsonString = Files.readString(ruta)
        coches = Json.decodeFromString<List<CocheJSON>>(jsonString)
    }catch (e: Exception) {
        println("Error: ${e.message}")
    }
    return coches
}

fun leerXML(ruta: Path): List<ICoche> {
    val fichero: File = ruta.toFile()
    val xmlMapper = XmlMapper().registerKotlinModule()
    val cochesWrapper: CocheXmlRoot = xmlMapper.readValue(fichero)
    return cochesWrapper.listaCochesXML
}

fun leerBinario(path: Path): List<ICoche> {
    val coches = mutableListOf<ICoche>()
    try {

        FileChannel.open(
            path,
            StandardOpenOption.READ,
        ).use { canal ->
            val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)

            while (canal.read(buffer) > 0) {
                buffer.flip()

                val id_coche = buffer.getInt()

                val bytesNombre_Modelo = ByteArray(TAMANO_MODELO)
                buffer.get(bytesNombre_Modelo)
                val nombre_modelo = String(bytesNombre_Modelo, Charsets.UTF_8).trim()

                val bytesNombre_Marca = ByteArray(TAMANO_MODELO)
                buffer.get(bytesNombre_Marca)
                val nombre_marca = String(bytesNombre_Marca, Charsets.UTF_8).trim()

                val consumo = buffer.getDouble()

                val HP = buffer.getInt()

                coches.add(CocheBinario(id_coche, nombre_modelo, nombre_marca, consumo, HP))

                buffer.clear()
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
    return coches

}

fun escribirCSV(ruta: Path, coches: List<ICoche>){
    vaciarCrearFichero(ruta)
    try {
        val fichero: File = ruta.toFile()
        csvWriter {
            delimiter = ';'
        }.writeAll(
            coches.map { coche ->
                listOf(coche.id_coche.toString(),
                    coche.nombre_modelo,
                    coche.nombre_marca,
                    coche.consumo.toString(),
                    coche.HP.toString())
            },
            fichero
        )
        println("\nInformación guardada en: $fichero")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
fun escribirJSON(ruta: Path, coches: List<ICoche>) {
    vaciarCrearFichero(ruta)

    val cochesJson: List<CocheJSON> = coches.map { coche ->
        CocheJSON(
            coche.id_coche,
            coche.nombre_modelo,
            coche.nombre_marca,
            coche.consumo,
            coche.HP
        )
    }

    try {
        val json = Json { prettyPrint = true }.encodeToString(cochesJson)
        Files.writeString(ruta, json)
        println("\nInformación guardada en: $ruta")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}

fun escribirXML(ruta: Path, coches: List<ICoche>) {
    vaciarCrearFichero(ruta)

    val cochesXml: List<CocheXML> = coches.map { coche ->
        CocheXML(
            coche.id_coche,
            coche.nombre_modelo,
            coche.nombre_marca,
            coche.consumo,
            coche.HP
        )
    }
    try {
        val fichero: File = ruta.toFile()
        val contenedorXml = CocheXmlRoot(cochesXml)
        val xmlMapper = XmlMapper().registerKotlinModule()

        val xmlString =
            xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contenedorXml)
        fichero.writeText(xmlString)
        println("\nInformación guardada en: $fichero")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}

fun escribirBIN(ruta: Path, coches: List<ICoche>){
    vaciarCrearFichero(ruta)
    coches.forEach { coche->
        val nuevaCocche = CocheBinario(coche.id_coche,  coche.nombre_modelo,  coche.nombre_marca,  coche.consumo,  coche.HP)

        try {
            FileChannel.open(ruta, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND).use { canal ->
                val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)

                buffer.putInt(nuevaCocche.id_coche)

                val modeloBytes = nuevaCocche.nombre_modelo
                    .padEnd(40, ' ')
                    .toByteArray(Charset.defaultCharset())
                buffer.put(modeloBytes, 0, 40)

                val marcaBytes = nuevaCocche.nombre_marca
                    .padEnd(40, ' ')
                    .toByteArray(Charset.defaultCharset())
                buffer.put(marcaBytes, 0, 40)

                buffer.putDouble(nuevaCocche.consumo)

                buffer.putInt(nuevaCocche.HP)

                buffer.flip()
                while (buffer.hasRemaining()) {
                    canal.write(buffer)
                }
                println("Coche ${nuevaCocche.nombre_marca} '${nuevaCocche.nombre_modelo}' añadido con éxito.")
            }
        } catch (e: Exception) {
            println("Error al añadir la coche: ${e.message}")
        }

    }

}

fun mostrarBIN(path: Path): List<ICoche> {
    //como en la documentacion de referencia esta funcion devolvia una lista para luego ser tratada
    //lo he deado igual salvo que printeo tambien lo que se ha leido
    val coches = mutableListOf<ICoche>()
    FileChannel.open(path, StandardOpenOption.READ).use { canal ->
        val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)
        while (canal.read(buffer) > 0) {
            buffer.flip()

            val id = buffer.getInt()

            val modeloBytes = ByteArray(TAMANO_MODELO)
            buffer.get(modeloBytes)
            val modelo = String(modeloBytes,
                Charset.defaultCharset()).trim()

            val marcaBytes = ByteArray(TAMANO_MARCA)
            buffer.get(marcaBytes)
            val marca = String(marcaBytes,
                Charset.defaultCharset()).trim()


            val consumo = buffer.getDouble()
            val HP = buffer.getInt()

            coches.add(CocheBinario(id_coche = id, nombre_modelo = modelo,
                nombre_marca = marca,consumo = consumo,HP = HP))
            println("ID: ${id}, Modelo: ${modelo}, Marca: ${marca}," +
                    " Consumo: ${consumo}, Potencia: ${HP}")
            buffer.clear()
        }
    }
    return coches
}

fun modificar(path: Path, idCoche: Int, nuevoHP: Int)
{       //modificamos HP horsepower
    FileChannel.open(path, StandardOpenOption.READ,
        StandardOpenOption.WRITE).use { canal ->
        val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)
        var encontrado = false
        while (canal.read(buffer) > 0 && !encontrado) {
            val posicionActual = canal.position()
            buffer.flip()
            val id = buffer.getInt()
            if (id == idCoche) {
                val posicionConsumo = posicionActual - TAMANO_REGISTRO + TAMANO_ID + TAMANO_MODELO + TAMANO_MARCA + TAMANO_CONSUMO

                val bufferHP = ByteBuffer.allocate(TAMANO_HP)
                bufferHP.putInt(nuevoHP)

                bufferHP.flip()
                canal.write(bufferHP, posicionConsumo)
                encontrado = true
            }
            buffer.clear()
        }
        if (encontrado) {
            println("Potencia del coche con ID $idCoche modificado a $nuevoHP")
        } else {
            println("No se encontró el coche con ID $idCoche")
        }
    }
}

fun eliminar(path: Path, idCoche: Int) {
    val pathTemporal = Paths.get(path.toString() + ".tmp")
    var cocheEncontrado = false
    FileChannel.open(path, StandardOpenOption.READ).use { canalLectura ->
        FileChannel.open(pathTemporal, StandardOpenOption.WRITE,
            StandardOpenOption.CREATE).use { canalEscritura ->
            val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)
            while (canalLectura.read(buffer) > 0) {
                buffer.flip()
                val id = buffer.getInt()
                if (id == idCoche) {
                    cocheEncontrado = true
                } else {
                    buffer.rewind()
                    canalEscritura.write(buffer)
                }
                buffer.clear()
            }
        }
    }
    if (cocheEncontrado) {
        Files.move(pathTemporal, path, StandardCopyOption.REPLACE_EXISTING)
        println("Coche con ID $idCoche eliminado con éxito.")
    } else {
        Files.delete(pathTemporal)
        println("No se encontró ningun conche con ID $idCoche.")
    }
}

fun anadir(path: Path, id_coche: Int, nombre_modelo: String, nombre_marca: String, consumo: Double, HP: Int) {
    val nuevaCocche = CocheBinario(id_coche,  nombre_modelo,  nombre_marca,  consumo,  HP)

    try {
        FileChannel.open(path, StandardOpenOption.WRITE,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND).use { canal ->
            val buffer = ByteBuffer.allocate(TAMANO_REGISTRO)

            buffer.putInt(nuevaCocche.id_coche)

            val modeloBytes = nuevaCocche.nombre_modelo
                .padEnd(40, ' ')
                .toByteArray(Charset.defaultCharset())
            buffer.put(modeloBytes, 0, 40)
            val marcaBytes = nuevaCocche.nombre_marca
                .padEnd(40, ' ')
                .toByteArray(Charset.defaultCharset())
            buffer.put(marcaBytes, 0, 40)

            buffer.putDouble(nuevaCocche.consumo)
            buffer.putInt(nuevaCocche.HP)

            buffer.flip()
            while (buffer.hasRemaining()) {
                canal.write(buffer)
            }
            println("Coche ${nuevaCocche.nombre_marca} '${nuevaCocche.nombre_modelo}' añadido con éxito.")
        }
    } catch (e: Exception) {
        println("Error al añadir la coche: ${e.message}")
    }
}

fun vaciarCrearFichero(path: Path) {
    try {
        FileChannel.open(path, StandardOpenOption.WRITE,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).close()
        println("El fichero '${path.fileName}' existe y está vacío.")
    } catch (e: Exception) {
        println("Error al vaciar o crear el fichero: ${e.message}")
    }
}
fun comprobarCrearCarpetas(path: Path){
    try {
        if (Files.notExists(path)) {
            println("-> Creando nueva carpeta " + path.fileName)
            Files.createDirectories(path.fileName)
        }
    } catch (e: Exception) {
        println("\n--- Ocurrió un error durante la organización ---")
        e.printStackTrace()
    }
}

fun main() {
    val carpeta_ini = Path.of("datos_ini")
    val carpeta_fin = Path.of("datos_fin")





//    mostrarData(leerCSV(Paths.get("datos_ini/coches.csv")))
//    println()
//    mostrarData(leerJSON(Paths.get("datos_ini/coches.json")))
//    println()
//    mostrarData(leerXML(Paths.get("datos_ini/coches.xml")))
//    println()
//    mostrarData(leerBinario(Paths.get("datos_ini/coches.bin")))
    val rutas= listOf(
        leerCSV(Paths.get("datos_ini/coches.csv")),
        leerXML(Paths.get("datos_ini/coches.xml")),
        leerJSON(Paths.get("datos_ini/coches.json")),
        leerBinario(Paths.get("datos_ini/coches.bin"))
    )
    val prefijos= listOf("CSV","XML","JSON","BIN")
    for (i in 0.. 3){
        escribirCSV(Paths.get("datos_fin/coches${prefijos[i]}.csv"),rutas[i])
        escribirJSON(Paths.get("datos_fin/coches${prefijos[i]}.json"),rutas[i])
        escribirXML(Paths.get("datos_fin/coches${prefijos[i]}.xml"),rutas[i])
        escribirBIN(Paths.get("datos_fin/coches${prefijos[i]}.bin"),rutas[i])
    }
    mostrarData(mostrarBIN(Paths.get("datos_ini/coches.bin")))






    val pathini: Path = Paths.get("copias/pruebas.bin")
    val pathfin: Path = Paths.get("copias/pruebas.bin")

    val scanner = Scanner(System.`in`)
    var itera = true

    do {
        println()
        println("   Selecciona una opcion: ")
        println("1. Mostrar todos los registros")
        println("2. Añadir un nuevo registro")
        println("3. Modificar un registro (por ID)")
        println("4. Eliminar un registro (por ID)")
        println("5. Salir")

        try {
            val select = scanner.nextInt()
            scanner.nextLine()
            when (select) {
                1 -> {
                    mostrarBIN(pathini)
                }
                2 -> {
                    println(" Añadir coche")
                    print("ID: ")
                    val id = scanner.nextInt()
                    scanner.nextLine()
                    print("Modelo: ")
                    val Modelo = scanner.nextLine()
                    print("Marca: ")
                    val Marca = scanner.nextLine()
                    print("Consumo (double): ")
                    val Consumo = scanner.nextDouble()
                    print("Potencia (int): ")
                    val Potencia = scanner.nextInt()
                    scanner.nextLine()

                    anadir(pathini, id, Modelo, Marca, Consumo, Potencia)
                }
                3 -> {
                    print("ID del coche a modificar: ")
                    val id = scanner.nextInt()
                    print("Nueva Potencia: ")
                    val nuevaPotencia = scanner.nextInt()
                    scanner.nextLine()

                    modificar(pathini, id, nuevaPotencia)
                }
                4 -> {

                    print("ID del coche a eliminar: ")
                    val id = scanner.nextInt()
                    scanner.nextLine()
                    eliminar(pathini, id)
                }
                5 -> {
                    itera = false
                }

                else -> {
                    println("Opcion no valida. Por favor, selecciona una opcion del 1 al 5.")
                }
            }

        } catch (e: InputMismatchException) {
            println("Error: Debes introducir un numero valido.")
            scanner.nextLine()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            scanner.nextLine()
        }
    } while (itera)
    scanner.close()

}