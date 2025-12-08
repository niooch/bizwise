package com.example.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.data.LoginRequest
import com.example.app.data.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var nicknameInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Zaloguj się", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nicknameInput,
            onValueChange = { nicknameInput = it },
            label = { Text("Nickname") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            val request = LoginRequest(nickname = nicknameInput, username = nicknameInput, password = password) //TODO: fixError - delete username
                            val response = RetrofitClient.api.loginUser(request)

                            if (response.isSuccessful && response.body() != null) {
                                val token = response.body()!!.access
                                Toast.makeText(context, "Zalogowano! Token: ${token.take(10)}...", Toast.LENGTH_LONG).show()
                                onLoginSuccess(token)
                            } else {
                                Toast.makeText(context, "Błąd logowania (złe hasło?)", Toast.LENGTH_LONG).show()
                                //Toast.makeText(context, "DEBUG CZY TO TU", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Błąd sieci: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wejdź")
            }
        }
        TextButton(onClick = onBack) { Text("Wróć") }
    }
}
