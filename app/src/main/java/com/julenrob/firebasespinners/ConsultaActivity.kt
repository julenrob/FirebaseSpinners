package com.julenrob.firebasespinners

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.julenrob.firebasespinners.databinding.ActivityConsultaBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ConsultaActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    lateinit var binding : ActivityConsultaBinding
    val db = FirebaseFirestore.getInstance()
    var listaPaises : MutableList<String> = mutableListOf()
    var listaCiudades : MutableList<String> = mutableListOf()
    var selectedPais : Objetos.Pais? = Objetos.Pais()
    var selectedCiudad : Objetos.Ciudad? = Objetos.Ciudad()
    var listaMonumentos : MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fillFirstSpinner()
    }


    private fun adaptadorPaises(listaPaises : MutableList<String>){
        println("Entramos en el adaptador Paises")
        var adaptadorPaises = ArrayAdapter(this,
        android.R.layout.simple_spinner_dropdown_item, listaPaises)
        binding.spinnerPaises.adapter = adaptadorPaises
        binding.spinnerPaises.onItemSelectedListener = this
        println("Salimos del adaptador Paises")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        binding.spinnerPaises.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                println("Entramos en el onItemSelected")
                //var pais = binding.spinnerCiudades.onItemSelectedListener.toString()
                //var id = db.collection("ciudades").document(pais).toString()

                var pais = parent!!.getItemAtPosition(position).toString()
                listaCiudades = mutableListOf()

                println("Seleccionado: " + pais)

                var cod = ""

                GlobalScope.launch{
                    var document = getDocumentSnapshotPais(pais)

                    println("DOC: " + document)

                    for (doc in document){
                        if (doc.get("nombre") == pais){
                            cod = doc.id
                            selectedPais = doc.toObject(Objetos.Pais::class.java)
                            println("--> selectedPais: " + selectedPais)
                            break
                        }
                    }

                    // SEGUNDO SPINNER
                    db.collection("ciudades").whereEqualTo("cod_pais", cod).get()
                        .addOnSuccessListener { query ->
                            listaCiudades = mutableListOf()
                            for (document in query) {
                                var objCiudad: Objetos.Ciudad = Objetos.Ciudad(
                                    document.id,
                                    "${document.data["nombre"]}",
                                    "${document.data["poblacion"]}".toInt()
                                )
                                listaCiudades.add(objCiudad.nombre)
                                //println("---Ciudades: " + objCiudad.id + " " + objCiudad.nombre + " " + objCiudad.poblacion)

                                binding.tvPoblacionPais.text =  "Población de " + selectedPais?.nombre.toString() + " es " + selectedPais?.poblacion.toString() + " habitantes."
                            }

                            //adaptadorCiudades(listaCiudades)

                            val adapter2 = object : ArrayAdapter<String>(
                                view!!.context,
                                android.R.layout.simple_spinner_item,
                                listaCiudades
                            ) {
                                //override fun isEnabled(position: Int): Boolean {
                                //    return position != 0
                                //}
                            }

                            adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            adapter2.notifyDataSetChanged()
                            binding.spinnerCiudades.adapter = adapter2
                        }

                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }

        binding.spinnerCiudades.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                var ciudad = parent!!.getItemAtPosition(position).toString()
                listaMonumentos = mutableListOf()

                GlobalScope.launch {
                    var document = getDocumentSnapshotCiudad(ciudad, db)
                    var codCiudad = ""

                    for (doc in document){
                        println("COD CIUDAD: " + doc.id)
                        if (doc.get("nombre") == ciudad){
                            codCiudad = doc.id
                            var monumentos = getSnapshotMonumentosCiudad(codCiudad)

                            for (mon in monumentos){
                                println("MONUMENTOS: " + mon.get("nombre"))
                                listaMonumentos.add(mon.get("nombre").toString())
                            }

                            selectedCiudad = doc.toObject(Objetos.Ciudad::class.java)
                            var porcentaje = getPorcentaje(selectedCiudad!!.poblacion.toDouble(), selectedPais?.poblacion!!.toDouble())
                            binding.tvCiudad.text = "Ciudad seleccionada " + selectedCiudad?.nombre.toString() + "."
                            binding.tvPorcentaje.text = "% de la población de la ciudad respecto de la población total del país " + String.format("%.1f", porcentaje)
                            binding.tvPoblacionCiudad.text = "Población de " + selectedCiudad?.nombre.toString() + " " + selectedCiudad!!.poblacion + " habitantes."
                            binding.tvMonumentos.text = "Monumentos de " + selectedCiudad?.nombre.toString() + " " + listaMonumentos + "."

                        }
                    }


                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }


    private fun fillFirstSpinner() {

        // PRIMER SPINNER
        db.collection("paises").get().addOnSuccessListener { result ->
            for (document in result) {
                var objPais: Objetos.Pais = Objetos.Pais(document.id, "${document.data["nombre"]}", "${document.data["poblacion"]}".toInt())
                listaPaises.add(objPais.nombre)
                println(listaPaises)
            }

            adaptadorPaises(listaPaises)

        }

    }

    private suspend fun getDocumentSnapshotPais(pais:String): List<DocumentSnapshot> {
        val snapshot = db.collection("paises").get().await()
        return snapshot.documents

    }

    private suspend fun getDocumentSnapshotCiudad(ciudad: String, db: FirebaseFirestore): List<DocumentSnapshot> {
        val snapshot = db.collection("ciudades").get().await()
        return snapshot.documents
    }

    private fun getPorcentaje(poblacionCiudad: Double, poblacionPais: Double): Double{
        var result = (poblacionCiudad/poblacionPais)*100

        return result
    }

    private suspend fun getSnapshotMonumentosCiudad(cod:String): List<DocumentSnapshot> {
        val snapshot = db.collection("monumentos").whereEqualTo("cod_poblacion", cod).get().await()
        return snapshot.documents
    }

}