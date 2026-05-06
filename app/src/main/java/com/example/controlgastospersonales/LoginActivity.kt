package com.example.controlgastospersonales

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var edtCorreoLogin: EditText
    private lateinit var edtPasswordLogin: EditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var btnIrRegistro: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        edtCorreoLogin = findViewById(R.id.edtCorreoLogin)
        edtPasswordLogin = findViewById(R.id.edtPasswordLogin)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        btnIrRegistro = findViewById(R.id.btnIrRegistro)

        btnIniciarSesion.setOnClickListener {
            iniciarSesion()
        }

        btnIrRegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun iniciarSesion() {
        val correo = edtCorreoLogin.text.toString().trim()
        val password = edtPasswordLogin.text.toString().trim()

        if (correo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(correo, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Inicio de sesión correcto", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}