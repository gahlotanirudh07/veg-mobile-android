package com.freshveg.app.features.auth.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.ui.theme.BackgroundSurface
import com.freshveg.app.core.ui.theme.ForestGreenPrimary
import com.freshveg.app.core.ui.theme.MintGreenTertiary
import com.freshveg.app.features.auth.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterBuyerScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buyer Registration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect to your Wholesale Vegetable Seller",
                style = MaterialTheme.typography.titleMedium,
                color = ForestGreenPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Enter your shop details and your seller's code",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Business / Shop Name
            OutlinedTextField(
                value = uiState.businessName,
                onValueChange = viewModel::onBusinessNameChange,
                label = { Text("Business / Shop Name *") },
                placeholder = { Text("e.g. John's Fresh Market") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Primary Contact Name
            OutlinedTextField(
                value = uiState.primaryContactName,
                onValueChange = viewModel::onContactNameChange,
                label = { Text("Primary Contact / Owner Name *") },
                placeholder = { Text("e.g. John Doe") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Shop / Delivery Address
            OutlinedTextField(
                value = uiState.address,
                onValueChange = viewModel::onAddressChange,
                label = { Text("Shop / Delivery Address *") },
                placeholder = { Text("e.g. Shop #14, Main Market, NY") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Seller Code
            OutlinedTextField(
                value = uiState.sellerCode,
                onValueChange = viewModel::onSellerCodeChange,
                label = { Text("Seller Code (SEL-XXXXXX) *") },
                placeholder = { Text("e.g. SEL-8A3F2C") },
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                trailingIcon = {
                    if (uiState.isSellerCodeValid) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = MintGreenTertiary)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Mobile Number
            OutlinedTextField(
                value = uiState.mobile,
                onValueChange = viewModel::onMobileChange,
                label = { Text("Mobile Number (+91) *") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Password
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password (min 6 characters) *") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            if (uiState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Confirm Password
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("Confirm Password *") },
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                        Icon(
                            if (uiState.isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle confirm password visibility"
                        )
                    }
                },
                visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { viewModel.registerBuyer(onRegisterSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Register & Link Seller", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Already registered? Login here", color = ForestGreenPrimary)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
