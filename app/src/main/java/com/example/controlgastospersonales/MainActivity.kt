package com.example.controlgastospersonales

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var edtNombreGasto: EditText
    private lateinit var edtMonto: EditText
    private lateinit var edtCategoria: EditText
    private lateinit var edtFecha: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var txtTotalMensual: TextView
    private lateinit var txtBienvenida: TextView
    private lateinit var listaGastos: ListView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val gastos = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var totalMensual = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        edtNombreGasto = findViewById(R.id.edtNombreGasto)
        edtMonto = findViewById(R.id.edtMonto)
        edtCategoria = findViewById(R.id.edtCategoria)
        edtFecha = findViewById(R.id.edtFecha)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        txtTotalMensual = findViewById(R.id.txtTotalMensual)
        txtBienvenida = findViewById(R.id.txtBienvenida)
        listaGastos = findViewById(R.id.listaGastos)

        adapter = ArrayAdapter(this, R.layout.item_gasto, R.id.txtItemGasto, gastos)
        listaGastos.adapter = adapter

        mostrarBienvenida()
        cargarGastos()

        edtFecha.setOnClickListener {
            mostrarCalendario()
        }

        btnGuardar.setOnClickListener {
            guardarGasto()
        }

        btnCerrarSesion.setOnClickListener {
            confirmarCerrarSesion()
        }
    }

    private fun mostrarBienvenida() {
        val usuarioActual = auth.currentUser

        if (usuarioActual != null) {
            val correo = usuarioActual.email ?: "usuario"
            val nombreUsuario = correo.substringBefore("@")
                .replaceFirstChar { letra -> letra.uppercase() }

            txtBienvenida.text = "Bienvenido, $nombreUsuario"
        } else {
            txtBienvenida.text = "Bienvenido"
        }
    }

    private fun mostrarCalendario() {
        val calendario = Calendar.getInstance()

        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                edtFecha.setText(fechaSeleccionada)
            },
            anio,
            mes,
            dia
        )

        datePicker.show()
    }

    private fun guardarGasto() {
        val nombre = edtNombreGasto.text.toString().trim()
        val montoTexto = edtMonto.text.toString().trim()
        val categoria = edtCategoria.text.toString().trim()
        val fecha = edtFecha.text.toString().trim()

        if (nombre.isEmpty() || montoTexto.isEmpty() || categoria.isEmpty() || fecha.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val monto = montoTexto.toDoubleOrNull()

        if (monto == null || monto <= 0) {
            Toast.makeText(this, "Ingrese un monto válido", Toast.LENGTH_SHORT).show()
            return
        }

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val gasto = hashMapOf(
            "nombre" to nombre,
            "monto" to monto,
            "categoria" to categoria,
            "fecha" to fecha,
            "userId" to usuarioActual.uid
        )

        db.collection("gastos")
            .add(gasto)
            .addOnSuccessListener {
                Toast.makeText(this, "Gasto guardado en Firebase", Toast.LENGTH_SHORT).show()

                edtNombreGasto.text.clear()
                edtMonto.text.clear()
                edtCategoria.text.clear()
                edtFecha.text.clear()

                cargarGastos()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun cargarGastos() {
        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("gastos")
            .whereEqualTo("userId", usuarioActual.uid)
            .get()
            .addOnSuccessListener { resultado ->
                gastos.clear()
                totalMensual = 0.0

                var contador = 1

                for (documento in resultado) {
                    val nombre = documento.getString("nombre") ?: ""
                    val categoria = documento.getString("categoria") ?: ""
                    val fecha = documento.getString("fecha") ?: ""
                    val monto = documento.getDouble("monto") ?: 0.0

                    val textoGasto =
                        "$contador. $nombre\nMonto: \$${String.format("%.2f", monto)}\nCategoría: $categoria\nFecha: $fecha"

                    gastos.add(textoGasto)

                    totalMensual += monto
                    contador++
                }

                txtTotalMensual.text = "Total mensual: \$${String.format("%.2f", totalMensual)}"
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error al cargar: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun confirmarCerrarSesion() {
        val vista = layoutInflater.inflate(R.layout.dialog_cerrar_sesion, null)

        val btnCancelarDialogo = vista.findViewById<Button>(R.id.btnCancelarDialogo)
        val btnCerrarDialogo = vista.findViewById<Button>(R.id.btnCerrarDialogo)

        val dialogo = AlertDialog.Builder(this)
            .setView(vista)
            .create()

        btnCancelarDialogo.setOnClickListener {
            dialogo.dismiss()
        }

        btnCerrarDialogo.setOnClickListener {
            dialogo.dismiss()
            cerrarSesion()
        }

        dialogo.show()

        dialogo.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun cerrarSesion() {
        auth.signOut()

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}