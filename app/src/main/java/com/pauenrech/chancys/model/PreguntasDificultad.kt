package com.pauenrech.chancys.model

/**
 *
 * Esta clase tiene como parámetro
 * @property preguntas que es una lista de
 * @see Pregunta
 *
 * Todas las propiedades de esta clase están inicializados para emular a un constructor vacio necesario para servir
 * de modelo para la BBDD de Firebase
 *
 * */
class PreguntasDificultad(
    var preguntas: MutableList<Pregunta> = mutableListOf()) {

    fun getRandomQuestions(): List<Pregunta>{

        /**
         *
         * Se define una variable
         * @param randomQuestions que es una copia de la lista
         * @see preguntas
         *
         * Se utiliza el método
         * @see MutableList.shuffle para mezclar aleatoriamente las preguntas de la lista
         *
         * Finalmente se utiliza el método
         * @see MutableList.slice para extraer las primeras diez preguntas de la lista aleatoria y se devuelven como
         * resultado
         *
         * */
        val randomQuestions = preguntas.toMutableList()
        randomQuestions.shuffle()
        return randomQuestions.slice(0 until 10)
    }

}