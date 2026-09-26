package com.freshveg.app.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.network.AuthModeType
import com.freshveg.app.core.network.LoginRequest
import com.freshveg.app.core.network.RegisterBuyerRequest
import com.freshveg.app.core.network.RegisterSellerRequest
import com.freshveg.app.core.network.VegApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val mobile: String = "",
    val businessName: String = "",
    val primaryContactName: String = "",
    val sellerCode: String = "",
    val address: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val otp: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val authMode: AuthModeType = AuthModeType.BOTH,
    val isSellerCodeValid: Boolean = false,
    val isSeller: Boolean = false,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchAuthConfig()
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        viewModelScope.launch {
            try {
                val remember = sessionManager.rememberMe.firstOrNull() ?: false
                if (remember) {
                    val mobile = sessionManager.savedMobile.firstOrNull() ?: ""
                    val password = sessionManager.savedPassword.firstOrNull() ?: ""
                    _uiState.update {
                        it.copy(
                            mobile = mobile,
                            password = password,
                            rememberMe = true
                        )
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun fetchAuthConfig() {
        viewModelScope.launch {
            try {
                val response = apiService.getAuthConfig()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update { it.copy(authMode = response.body()!!.authMode) }
                }
            } catch (e: Exception) {
                // Fallback to default
            }
        }
    }

    fun onMobileChange(mobile: String) {
        val clean = mobile.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(mobile = clean, errorMessage = null) }
    }

    fun onBusinessNameChange(name: String) = _uiState.update { it.copy(businessName = name, errorMessage = null) }
    fun onContactNameChange(name: String) = _uiState.update { it.copy(primaryContactName = name, errorMessage = null) }
    fun onSellerCodeChange(code: String) {
        val upper = code.uppercase().trim()
        _uiState.update { it.copy(sellerCode = upper, isSellerCodeValid = upper.length >= 6, errorMessage = null) }
    }
    fun onAddressChange(address: String) = _uiState.update { it.copy(address = address, errorMessage = null) }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, errorMessage = null) }
    fun onConfirmPasswordChange(confirm: String) = _uiState.update { it.copy(confirmPassword = confirm, errorMessage = null) }
    fun onOtpChange(otp: String) {
        val clean = otp.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otp = clean, errorMessage = null) }
    }
    fun togglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun toggleConfirmPasswordVisibility() = _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    fun toggleRememberMe() = _uiState.update { it.copy(rememberMe = !it.rememberMe) }

    fun login(onSuccess: (role: String, isSeller: Boolean, isBuyer: Boolean) -> Unit) {
        val state = _uiState.value
        if (state.mobile.length != 10) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid 10-digit mobile number") }
            return
        }
        if (state.authMode != AuthModeType.OTP && state.password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val req = LoginRequest(
                    mobile = state.mobile,
                    password = if (state.password.isNotEmpty()) state.password else null,
                    otp = if (state.otp.isNotEmpty()) state.otp else null
                )
                val res = apiService.login(req)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!
                    val isSeller = data.customer.isSeller
                    val isBuyer = data.customer.isBuyer
                    val role = data.user.role
                    val perms = data.user.permissions

                    sessionManager.saveSession(
                        accessToken = data.accessToken,
                        refreshToken = data.refreshToken,
                        userId = data.user.id,
                        name = data.user.name,
                        mobile = data.user.mobile,
                        role = role,
                        sellerCode = data.customer.sellerCode,
                        businessName = data.customer.businessName,
                        isSeller = isSeller,
                        isBuyer = isBuyer,
                        isStaff = data.user.isStaff ?: false,
                        canManageRates = perms?.canManageRates ?: true,
                        canManageOrders = perms?.canManageOrders ?: true,
                        canManageBilling = perms?.canManageBilling ?: true,
                        canManageStaff = perms?.canManageStaff ?: true
                    )

                    // Persist Remember Me credentials
                    sessionManager.saveRememberMe(state.mobile, state.password, state.rememberMe)

                    _uiState.update { it.copy(isLoading = false, isSuccess = true, isSeller = isSeller) }
                    onSuccess(role, isSeller, isBuyer)
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = extractErrorMessage(res.errorBody()?.string()) ?: "Login failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Connection error") }
            }
        }
    }

    fun registerBuyer(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.businessName.trim().isEmpty() ||
            state.primaryContactName.trim().isEmpty() ||
            state.address.trim().isEmpty() ||
            state.sellerCode.trim().isEmpty() ||
            state.mobile.trim().isEmpty() ||
            state.password.trim().isEmpty()
        ) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields including Seller Code.") }
            return
        }
        if (state.mobile.length != 10) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid 10-digit mobile number.") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters long.") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val req = RegisterBuyerRequest(
                    businessName = state.businessName.trim(),
                    primaryContactName = state.primaryContactName.trim(),
                    address = state.address.trim(),
                    sellerCode = state.sellerCode.trim().toUpperCase(),
                    mobile = state.mobile.trim(),
                    password = state.password.trim()
                )
                val res = apiService.registerBuyer(req)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!
                    sessionManager.saveSession(
                        accessToken = data.accessToken,
                        refreshToken = data.refreshToken,
                        userId = data.user.id,
                        name = data.user.name,
                        mobile = data.user.mobile,
                        sellerCode = data.buyer.sellerCode,
                        businessName = data.buyer.businessName,
                        isSeller = false,
                        isBuyer = true
                    )
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, isSeller = false) }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = extractErrorMessage(res.errorBody()?.string()) ?: "Buyer Registration failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Connection error") }
            }
        }
    }

    fun registerSeller(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.businessName.trim().isEmpty() ||
            state.primaryContactName.trim().isEmpty() ||
            state.address.trim().isEmpty() ||
            state.mobile.trim().isEmpty() ||
            state.password.trim().isEmpty()
        ) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields.") }
            return
        }
        if (state.mobile.length != 10) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid 10-digit mobile number.") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters long.") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val req = RegisterSellerRequest(
                    businessName = state.businessName.trim(),
                    primaryContactName = state.primaryContactName.trim(),
                    address = state.address.trim(),
                    mobile = state.mobile.trim(),
                    password = state.password.trim()
                )
                val res = apiService.registerSeller(req)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!
                    sessionManager.saveSession(
                        accessToken = data.accessToken,
                        refreshToken = data.refreshToken,
                        userId = data.user.id,
                        name = data.user.name,
                        mobile = data.user.mobile,
                        sellerCode = data.seller.sellerCode,
                        businessName = data.seller.businessName,
                        isSeller = true,
                        isBuyer = false
                    )
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, isSeller = true) }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = extractErrorMessage(res.errorBody()?.string()) ?: "Seller Registration failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Connection error") }
            }
        }
    }

    private fun extractErrorMessage(rawJson: String?): String {
        if (rawJson.isNullOrBlank()) return "An error occurred. Please try again."
        return try {
            val json = org.json.JSONObject(rawJson)
            if (json.has("error")) json.getString("error")
            else if (json.has("message")) json.getString("message")
            else rawJson
        } catch (_: Exception) {
            rawJson
        }
    }
}
