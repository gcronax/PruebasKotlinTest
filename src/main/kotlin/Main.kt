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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

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



    val pathini: Path = Paths.get("datos_ini/coches.json")
    val pathfin: Path = Paths.get("datos_ini/coches2.bin")

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


    }







}