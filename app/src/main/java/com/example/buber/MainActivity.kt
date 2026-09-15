package com.example.buber

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BuberFirebaseTest()
        }
    }
}

@Composable
fun BuberFirebaseTest() {

    var status by remember {
        mutableStateOf("Aguardando teste...")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "BUBER TRACKER",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Teste de conexão com o Firebase"
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {

                    status = "Enviando..."

                    val database =
                        FirebaseDatabase.getInstance()

                    val referencia =
                        database.getReference("teste_conexao")

                    val dados = mapOf(
                        "mensagem" to "BUBER conectado!",
                        "aplicativo" to "BUBER Tracker",
                        "timestamp" to System.currentTimeMillis()
                    )

                    referencia.setValue(dados)
                        .addOnSuccessListener {

                            status = "✅ Firebase conectado!"
                        }
                        .addOnFailureListener { erro ->

                            status =
                                "❌ Erro: ${erro.message}"
                        }
                }
            ) {
                Text("TESTAR FIREBASE")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}