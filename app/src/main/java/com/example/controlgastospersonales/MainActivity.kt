package com.example.controlgastospersonales

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
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
    private lateinit var txtCategoriaSeleccionada: TextView
    private lateinit var edtFecha: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var txtTotalMensual: TextView
    private lateinit var txtBienvenida: TextView
    private lateinit var listaGastos: ListView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val gastos = ArrayList<String>()
    private val gastosInfo = ArrayList<GastoInfo>()
    private lateinit var adapter: ArrayAdapter<String>
    private var totalMensual = 0.0

    private var categoriaSeleccionada = "Seleccionar categoría"

    private val categoriasGuardar = arrayOf(
        "Comida",
        "Transporte",
        "Estudios",
        "Casa",
        "Entretenimiento",
        "Salud",
        "Otros"
    )

    private val categoriasMostrar = arrayOf(
        "🍽️ Comida",
        "🚌 Transporte",
        "📚 Estudios",
        "🏠 Casa",
        "🎮 Entretenimiento",
        "❤️ Salud",
        "📦 Otros"
    )

    data class GastoInfo(
        val id: String,
        val nombre: String,
        val monto: Double,
        val categoria: String,
        val fecha: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        edtNombreGasto = findViewById(R.id.edtNombreGasto)
        edtMonto = findViewById(R.id.edtMonto)
        txtCategoriaSeleccionada = findViewById(R.id.txtCategoriaSeleccionada)
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

        txtCategoriaSeleccionada.setOnClickListener {
            mostrarSelectorCategorias()
        }

        edtFecha.setOnClickListener {
            mostrarCalendario(edtFecha)
        }

        btnGuardar.setOnClickListener {
            guardarGasto()
        }

        btnCerrarSesion.setOnClickListener {
            confirmarCerrarSesion()
        }

        listaGastos.setOnItemClickListener { _, _, position, _ ->
            mostrarOpcionesGasto(position)
        }
    }

    private fun mostrarBienvenida() {
        val usuarioActual = auth.currentUser

        if (usuarioActual != null) {
            db.collection("usuarios")
                .document(usuarioActual.uid)
                .get()
                .addOnSuccessListener { documento ->
                    val nombre = documento.getString("nombre")

                    if (!nombre.isNullOrEmpty()) {
                        val nombreFormateado = nombre.replaceFirstChar { letra -> letra.uppercase() }
                        txtBienvenida.text = "Bienvenido, $nombreFormateado"
                    } else {
                        val correo = usuarioActual.email ?: "usuario"
                        val nombreCorreo = correo.substringBefore("@")
                            .replaceFirstChar { letra -> letra.uppercase() }

                        txtBienvenida.text = "Bienvenido, $nombreCorreo"
                    }
                }
                .addOnFailureListener {
                    val correo = usuarioActual.email ?: "usuario"
                    val nombreCorreo = correo.substringBefore("@")
                        .replaceFirstChar { letra -> letra.uppercase() }

                    txtBienvenida.text = "Bienvenido, $nombreCorreo"
                }
        } else {
            txtBienvenida.text = "Bienvenido"
        }
    }

    private fun mostrarSelectorCategorias() {
        AlertDialog.Builder(this)
            .setTitle("Selecciona una categoría")
            .setItems(categoriasMostrar) { _, which ->
                categoriaSeleccionada = categoriasGuardar[which]
                txtCategoriaSeleccionada.text = categoriasMostrar[which]
            }
            .show()
    }

    private fun mostrarCalendario(campoFecha: EditText) {
        val calendario = Calendar.getInstance()

        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                campoFecha.setText(fechaSeleccionada)
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
        val categoria = categoriaSeleccionada
        val fecha = edtFecha.text.toString().trim()

        if (nombre.isEmpty() || montoTexto.isEmpty() || fecha.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoria == "Seleccionar categoría") {
            Toast.makeText(this, "Seleccione una categoría", Toast.LENGTH_SHORT).show()
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
                categoriaSeleccionada = "Seleccionar categoría"
                txtCategoriaSeleccionada.text = "🏷️ Seleccionar categoría"
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
                gastosInfo.clear()
                totalMensual = 0.0

                var contador = 1

                for (documento in resultado) {
                    val id = documento.id
                    val nombre = documento.getString("nombre") ?: ""
                    val categoria = documento.getString("categoria") ?: ""
                    val fecha = documento.getString("fecha") ?: ""
                    val monto = documento.getDouble("monto") ?: 0.0

                    val textoGasto =
                        "🧾 $contador. $nombre\n" +
                                "💵 Monto: \$${String.format("%.2f", monto)}\n" +
                                "🏷️ Categoría: $categoria\n" +
                                "📅 Fecha: $fecha"

                    gastos.add(textoGasto)
                    gastosInfo.add(GastoInfo(id, nombre, monto, categoria, fecha))

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

    private fun mostrarOpcionesGasto(position: Int) {
        val gasto = gastosInfo[position]

        val opciones = arrayOf(
            "✏️ Editar gasto",
            "🗑️ Borrar gasto",
            "❌ Cancelar"
        )

        AlertDialog.Builder(this)
            .setTitle("🧾 Opciones del gasto")
            .setItems(opciones) { dialog, which ->
                when (which) {
                    0 -> confirmarEditarGasto(gasto)
                    1 -> confirmarBorrarGasto(gasto)
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun confirmarEditarGasto(gasto: GastoInfo) {
        AlertDialog.Builder(this)
            .setTitle("✏️ Editar gasto")
            .setMessage(
                "¿Seguro que quieres editar este gasto?\n\n" +
                        "${gasto.nombre} - \$${String.format("%.2f", gasto.monto)}"
            )
            .setPositiveButton("Sí, editar") { _, _ ->
                mostrarFormularioEditar(gasto)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarFormularioEditar(gasto: GastoInfo) {
        val contenedor = LinearLayout(this)
        contenedor.orientation = LinearLayout.VERTICAL
        contenedor.setPadding(50, 20, 50, 10)

        val edtNombre = EditText(this)
        edtNombre.hint = "Nombre del gasto"
        edtNombre.setText(gasto.nombre)

        val edtMontoEditar = EditText(this)
        edtMontoEditar.hint = "Monto"
        edtMontoEditar.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        edtMontoEditar.setText(gasto.monto.toString())

        val spinnerCategoriaEditar = Spinner(this)

        val categoriasEdicion = arrayOf(
            "Seleccionar categoría",
            "Comida",
            "Transporte",
            "Estudios",
            "Casa",
            "Entretenimiento",
            "Salud",
            "Otros"
        )

        val categoriaAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoriasEdicion
        )

        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoriaEditar.adapter = categoriaAdapter

        val posicionCategoria = categoriasEdicion.indexOf(gasto.categoria)
        if (posicionCategoria >= 0) {
            spinnerCategoriaEditar.setSelection(posicionCategoria)
        }

        val edtFechaEditar = EditText(this)
        edtFechaEditar.hint = "Fecha"
        edtFechaEditar.setText(gasto.fecha)
        edtFechaEditar.isFocusable = false
        edtFechaEditar.isClickable = true
        edtFechaEditar.setOnClickListener {
            mostrarCalendario(edtFechaEditar)
        }

        contenedor.addView(edtNombre)
        contenedor.addView(edtMontoEditar)
        contenedor.addView(spinnerCategoriaEditar)
        contenedor.addView(edtFechaEditar)

        AlertDialog.Builder(this)
            .setTitle("Modificar gasto")
            .setView(contenedor)
            .setPositiveButton("Guardar cambios") { _, _ ->
                val nuevoNombre = edtNombre.text.toString().trim()
                val nuevoMontoTexto = edtMontoEditar.text.toString().trim()
                val nuevaCategoria = spinnerCategoriaEditar.selectedItem.toString()
                val nuevaFecha = edtFechaEditar.text.toString().trim()

                if (nuevoNombre.isEmpty() || nuevoMontoTexto.isEmpty() || nuevaFecha.isEmpty()) {
                    Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (nuevaCategoria == "Seleccionar categoría") {
                    Toast.makeText(this, "Seleccione una categoría", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val nuevoMonto = nuevoMontoTexto.toDoubleOrNull()

                if (nuevoMonto == null || nuevoMonto <= 0) {
                    Toast.makeText(this, "Ingrese un monto válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                editarGasto(gasto.id, nuevoNombre, nuevoMonto, nuevaCategoria, nuevaFecha)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editarGasto(
        id: String,
        nombre: String,
        monto: Double,
        categoria: String,
        fecha: String
    ) {
        db.collection("gastos")
            .document(id)
            .update(
                mapOf(
                    "nombre" to nombre,
                    "monto" to monto,
                    "categoria" to categoria,
                    "fecha" to fecha
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Gasto editado correctamente", Toast.LENGTH_SHORT).show()
                cargarGastos()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error al editar: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun confirmarBorrarGasto(gasto: GastoInfo) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Borrar gasto")
            .setMessage(
                "¿Seguro que quieres borrar este gasto?\n\n" +
                        "${gasto.nombre} - \$${String.format("%.2f", gasto.monto)}\n\n" +
                        "Esta acción no se puede deshacer."
            )
            .setPositiveButton("Sí, borrar") { _, _ ->
                borrarGasto(gasto.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun borrarGasto(id: String) {
        db.collection("gastos")
            .document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Gasto borrado correctamente", Toast.LENGTH_SHORT).show()
                cargarGastos()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error al borrar: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun confirmarCerrarSesion() {
        val vista = layoutInflater.inflate(R.layout.dialog_cerrar_sesion, null)

        val btnCancelarDialogo = vista.findViewById<Button>(R.id.btnCancelarDialogo)
        val btnCerrarDialogo = vista.findViewById<Button>(R.id.btnCerrarDialogo)

        val dialogo = AlertDialog.Builder(this)
            .setView(vista)
            .create()

        dialogo.setOnShowListener {
            dialogo.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        btnCancelarDialogo.setOnClickListener {
            dialogo.dismiss()
        }

        btnCerrarDialogo.setOnClickListener {
            dialogo.dismiss()
            cerrarSesion()
        }

        dialogo.show()
    }

    private fun cerrarSesion() {
        auth.signOut()

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}