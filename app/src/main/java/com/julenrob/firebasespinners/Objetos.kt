package com.julenrob.firebasespinners

class Objetos {
    data class Pais (var id:String = "id_def", var nombre:String = "nombre_def", var poblacion:Int = 0) {
    }

    data class Ciudad (var id:String = "id_def", var nombre:String = "nombre_def", var poblacion:Int = 0){
    }

    data class Monumento (var id:String, var cod_poblacion:String, var nombre:String){
    }
}