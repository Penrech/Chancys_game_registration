package com.pauenrech.chancys.model

import android.util.Log
import com.google.android.gms.common.collect.Sets
import com.google.android.gms.common.util.MapUtils
import com.google.gson.annotations.SerializedName

/**
 *
 * Esta clase implementa el modelo de básico de usuario, donde se guardan los datos de un usuario en concreto.
 * Todas su propiedades están inicializadas de alguna manera para crear un constructor sin parámetros de entrada
 * obligatorios. Esto es necesario para rellenar esta clase con datos de Firebase en caso de que fuera necesario
 *
 * Cuenta con las siguientes propiedades:
 * @property uid que representa la id única del usuario, se obtiene de la base de datos Firebase.
 * @property nickname que representa el nickname del usuario, este es también es único en la base de datos Firebase
 * para ello se introduce la siguiente propiedad
 * @property nicknameLowerCase que guarda el nickname to_do en minisculas para poder hacer comprobaciones de este con
 * Firebase más correctamente. UsuarioPruebas != usuariopruebas -> usuariopruebas == usuariopruebas
 * @property dificultad que representa que dificultad a seleccionado el usuario, es de la clase Int ya que de esta
 * forma es más sencillo determinar la dificultad en cada momento y en las diferentes situaciones
 * @property puntuacion que representa la puntuación total del usuario
 * @property ranking que representa el ranking del usuario respecto a otros usuarios de Firebase (No implementado actualmente)
 * @property temas que representa una lista de listas de la clase
 * @see ThemeScore , Esta lista tiene de longitud fija tiene 3 elementos, los 3 elementos que representan la dificultad
 *
 */
class User(
    var uid: String = "",
    var email: String = "",
    var username: String = "Username",
    var dificultad: Int = 0,
    var puntuacion: Int = 0,
    var ranking: Int = -1,
    var conectado: Boolean = false,
    @SerializedName("temas")
    //var temas : List<MutableList<@JvmSuppressWildcards ThemeScore>> = listOf()
    var temas: Map<String,Temas_dificultad> = mapOf(),
    var preguntas: Map<String,PreguntaUser> = mapOf())
{

    var mapaTemas: MutableMap<String,MutableMap<String,@JvmSuppressWildcards ThemeScore>> = mutableMapOf()
    var numeroDeTemas : Int = 0

    init {
        Log.i("TAGLOOP","tamaño de temas ${temas.containsKey("facil")}")
        if (temas.size < 1){
            Log.i("TAGLOOP","Entro en if")
            mapaTemas = mutableMapOf(Pair("facil", mutableMapOf()),
            Pair("medio", mutableMapOf()),Pair("dificil", mutableMapOf()))
        }
        else {
            temas.forEach {dificultad ->
                Log.i("TAGLOOP","Key dificultad: ${dificultad.key}, valor de dificultad: ${dificultad.value}" +
                        "Dificultad temasdif: ${dificultad.value.temasDif}")
                mapaTemas[dificultad.key] = dificultad.value.temasDif

            }
        }
    }

    enum class DificultadEnum (val value: Int) {
        FACIL(0),
        MEDIO(1),
        DIFICIL(2)
    }

    fun convertDiffToString(dificultad: Int): String{
        when(dificultad){
            0 -> return "facil"
            1 -> return "medio"
            else -> return "dificil"
        }
    }


    fun getTemasParaSubirAFirebase(): Map<String,MutableMap<String,@JvmSuppressWildcards ThemeScore>>{
        return mapaTemas
    }

    fun addOrUpdateTemas(listaTemas: MutableList<Tema>){
        val newTemas : MutableMap<String,MutableMap<String,ThemeScore>> = mutableMapOf(Pair("facil", mutableMapOf()),
            Pair("medio", mutableMapOf()),Pair("dificil", mutableMapOf()))

        listaTemas.forEach { temaWeb ->
            mapaTemas.forEach { dificultad ->
                if (dificultad.value.contains(temaWeb.id)){
                    val newThemescore = dificultad.value[temaWeb.id]
                    newThemescore!!.name = temaWeb.name
                    newTemas[dificultad.key]!!.put(temaWeb.id,newThemescore)
                }
                else{
                    val newThemeScore = ThemeScore(temaWeb.name,temaWeb.id,dificultad.key,0)
                    newTemas[dificultad.key]!!.put(temaWeb.id,newThemeScore)
                }
            }
        }

        mapaTemas = newTemas
    }

    fun modifyTemaScore(id: String, score: Int){
        mapaTemas[convertDiffToString(dificultad)]!![id]!!.score = score
    }

    fun getTemaScore(id: String): Int{
        return mapaTemas[convertDiffToString(dificultad)]!![id]!!.score
    }

    fun getTemaListPorDificultad(dificultad: Int): MutableList<User.ThemeScore>{
        val list = mutableListOf<User.ThemeScore>()
        mapaTemas[convertDiffToString(dificultad)]!!.forEach {
            list.add(it.value)
        }
        return list
    }

    class Temas_dificultad(
        var temasDif: MutableMap<String,@JvmSuppressWildcards ThemeScore> = mutableMapOf()
    )

    class ThemeScore(
        var name: String = "",
        var id: String = "",
        var dificultadid: String = "",
        var score: Int = 0)

    class PreguntaUser(
        var id: String = "",
        var id_tema: String = "",
        var veces_respondida: Int = 0,
        var veces_respondida_correcto: Int = 0,
        var veces_respondida_incorrecto: Int = 0,
        var veces_respondida_timeout: Int = 0
    )
}