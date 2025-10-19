package org.example

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import kotlinx.serialization.Serializable
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

data class ICocheCSV(
    override val id_coche: Int,
    override val nombre_modelo: String,
    override val nombre_marca: String,
    override val consumo: Double,
    override val HP: Int
): ICoche

data class ICocheXML(
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
                  val listaCochesXML: List<ICocheXML> = emptyList()
)

@Serializable
data class ICocheJSON(
    override val id_coche: Int,
    override val nombre_modelo: String,
    override val nombre_marca: String,
    override val consumo: Double,
    override val HP: Int
): ICoche


data class ICocheBinario(
    override val id_coche: Int,
    override val nombre_modelo: String,
    override val nombre_marca: String,
    override val consumo: Double,
    override val HP: Int
): ICoche


fun mostrasData(coches: List<ICoche>){
    for (coche in coches) {
        println(" - ID: ${coche.id_coche}, Modelo: ${coche.nombre_modelo}, " +
                "Marca: ${coche.nombre_marca}, Consumo:" +
                "${coche.consumo} cavallos: ${coche.HP} ")
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



    val pathini: Path = Paths.get("datos_ini/coches.csv")

    val pathfin: Path = Paths.get("datos_ini/coches.bin")
}