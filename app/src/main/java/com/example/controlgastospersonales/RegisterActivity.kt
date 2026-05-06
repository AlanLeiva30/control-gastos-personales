package com.example.controlgastospersonales

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var edtCorreoRegistro: EditText
    private lateinit var edtPasswordRegistro: EditText
    private lateinit var edtConfirmarPassword: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var btnVolverLogin: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        edtCorreoRegistro = findViewById(R.id.edtCorreoRegistro)
        edtPasswordRegistro = findViewById(R.id.edtPasswordRegistro)
        edtConfirmarPassword = findViewById(R.id.edtConfirmarPassword)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnVolverLogin = findViewById(R.id.btnVolverLogin)

        btnRegistrar.setOnClickListener {
            registrarUsuario()
        }

        btnVolverLogin.setOnClickListener {
            finish()
        }
    }

    private fun registrarUsuario() {
        val correo = edtCorreoRegistro.text.toString().trim()
        val password = edtPasswordRegistro.text.toString().trim()
        val confirmarPassword = edtConfirmarPassword.text.toString().trim()

        if (correo.isEmpty() || password.isEmpty() || confirmarPassword.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!correo.contains("@")) {
            Toast.makeText(this, "Ingrese un correo válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmarPassword) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Cuenta creada correctamente", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}