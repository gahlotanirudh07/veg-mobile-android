package com.freshveg.app.features.auth.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.network.AuthModeType
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.features.auth.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegisterBuyer: () -> Unit,
    onNavigateToRegisterSeller: () -> Unit,
    onLoginSuccess: (role: String, isSeller: Boolean, isBuyer: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo & Title
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = "FreshVeg Logo",
                tint = ActionGreen,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("FreshVeg", style = MaterialTheme.typography.headlineMedium, color = MainInk, fontWeight = FontWeight.Bold)
            Text("Wholesale Vegetable Marketplace", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)

            Spacer(modifier = Modifier.height(28.dp))

            // Sign In Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Sign In to Your Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.mobile,
                        onValueChange = viewModel::onMobileChange,
                        label = { Text("Mobile Number (+91)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (uiState.authMode == AuthModeType.PASSWORD || uiState.authMode == AuthModeType.BOTH) {
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = viewModel::togglePasswordVisibility) {
                                    Icon(
                                        if (uiState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle visibility"
                                    )
                                }
                            },
                            visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = uiState.otp,
                            onValueChange = viewModel::onOtpChange,
                            label = { Text("6-Digit OTP") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Remember Me & Save Password Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleRememberMe() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.rememberMe,
                            onCheckedChange = { viewModel.toggleRememberMe() },
                            colors = CheckboxDefaults.colors(checkedColor = ActionGreen)
                        )
                        Text(
                            text = "Save password & keep me signed in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.login(onLoginSuccess) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Sign In", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Registration Selection Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                Text(
                    "  New to FreshVeg? Choose your role  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Register as Buyer Card
            OutlinedCard(
                onClick = onNavigateToRegisterBuyer,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                colors = CardDefaults.outlinedCardColors(containerColor = NeutralSurface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ActionGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = ActionGreen)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Register as a Buyer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MainInk
                        )
                        Text(
                            "Connect to your seller code & place daily orders",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = InkSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Register as Seller Card
            OutlinedCard(
                onClick = onNavigateToRegisterSeller,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                colors = CardDefaults.outlinedCardColors(containerColor = NeutralSurface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ActionGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = ActionGreen)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Register as a Seller",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MainInk
                        )
                        Text(
                            "Set up your wholesale business & get a Seller Code",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = InkSecondary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
