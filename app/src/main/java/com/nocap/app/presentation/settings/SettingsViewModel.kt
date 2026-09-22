package com.nocap.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nocap.app.R
import com.nocap.app.core.datastore.DevicePreferencesDataStore
import com.nocap.app.core.datastore.ReaderFontFamily
import com.nocap.app.core.datastore.ReaderPreferences
import com.nocap.app.core.datastore.ReaderPreferencesDataStore
import com.nocap.app.core.datastore.ReaderTextAlignment
import com.nocap.app.core.datastore.ReaderTheme
import com.nocap.app.data.auth.BackendAccountRepository
import com.nocap.app.data.auth.CloudAuthRepository
import com.nocap.app.data.auth.GoogleSignInHelper
import com.nocap.app.data.auth.LocalDeviceRepository
import com.nocap.app.data.cloud.CloudBackupRepository
import com.nocap.app.data.cloud.CloudBackupSnapshot
import com.nocap.app.domain.model.AuthState
import com.nocap.app.domain.model.AuthUser
import com.nocap.app.domain.model.UserProfile
import com.nocap.app.domain.repository.AccountRepository
import com.nocap.app.domain.repository.AuthRepository
import com.nocap.app.domain.repository.DeviceRepository
import com.nocap.app.presentation.auth.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesDataStore: ReaderPreferencesDataStore,
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val deviceRepository: DeviceRepository,
    private val googleSignInHelper: GoogleSignInHelper = GoogleSignInHelper(),
    private val cloudBackup: CloudBackupRepository? = null
) : ViewModel() {

    val preferences: StateFlow<ReaderPreferences> = preferencesDataStore.readerPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReaderPreferences()
        )

    val authState: StateFlow<AuthState> = authRepository.authState
    val cloudBusy = MutableStateFlow(false)
    val cloudMessage = MutableStateFlow<String?>(null)
    private val _snapshots = MutableStateFlow<List<CloudBackupSnapshot>>(emptyList())
    val snapshots: StateFlow<List<CloudBackupSnapshot>> = _snapshots.asStateFlow()

    fun backupLibrary() = runCloud { repo ->
        val res = repo.backup()
        loadSnapshots()
        res
    }
    fun restoreLibrary(snapshotId: String? = null) = runCloud { repo ->
        val res = repo.restore(snapshotId)
        loadSnapshots()
        res
    }
    fun deleteCloudBackup(snapshotId: String? = null) = runCloud { repo ->
        val res = repo.deleteBackup(snapshotId)
        loadSnapshots()
        res
    }

    fun loadSnapshots() {
        val repository = cloudBackup ?: return
        viewModelScope.launch {
            val result = repository.listBackups()
            result.onSuccess { list ->
                _snapshots.value = list
            }
        }
    }

    private fun runCloud(action: suspend (CloudBackupRepository) -> Result<String>) {
        if (cloudBusy.value) return
        val repository = cloudBackup ?: return
        cloudBusy.value = true
        viewModelScope.launch {
            try { val result = action(repository); cloudMessage.value = result.getOrElse { it.localizedMessage ?: "Thao tác thất bại" } }
            finally { cloudBusy.value = false }
        }
    }

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // Dialog state
    var showLoginDialog = MutableStateFlow(false)
        private set
    var showRegisterDialog = MutableStateFlow(false)
        private set
    var showForgotPasswordDialog = MutableStateFlow(false)
        private set
    var showEditProfileDialog = MutableStateFlow(false)
        private set
    var showDeleteAccountDialog = MutableStateFlow(false)
        private set

    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    syncWithBackendAfterAuth(state.user)
                    loadSnapshots()
                } else {
                    _userProfile.value = null
                    _snapshots.value = emptyList()
                }
            }
        }
    }

    suspend fun syncWithBackendAfterAuth(user: AuthUser) {
        val profileResult = accountRepository.getProfile()
        profileResult.onSuccess { profile ->
            _userProfile.value = profile
            _authUiState.value = _authUiState.value.copy(
                isServerUnavailable = false,
                serverStatusMessage = null
            )
            // Register device after successful profile fetch/auto-provision
            runCatching { deviceRepository.registerDevice() }
        }.onFailure { err ->
            // Keep the local session on transient network errors
            _authUiState.value = _authUiState.value.copy(
                isServerUnavailable = true,
                serverStatusMessage = "Máy chủ backend chưa khả dụng: ${err.localizedMessage ?: "Mất kết nối"}"
            )
        }
    }

    fun retryBackendSync() {
        viewModelScope.launch {
            val currentAuth = authState.value
            if (currentAuth is AuthState.Authenticated) {
                _authUiState.value = _authUiState.value.copy(isLoading = true)
                syncWithBackendAfterAuth(currentAuth.user)
                _authUiState.value = _authUiState.value.copy(isLoading = false)
            }
        }
    }

    fun openLogin() {
        _authUiState.value = AuthUiState()
        showLoginDialog.value = true
        showRegisterDialog.value = false
        showForgotPasswordDialog.value = false
    }

    fun dismissLogin() {
        showLoginDialog.value = false
        _authUiState.value = AuthUiState()
    }

    fun openRegister() {
        _authUiState.value = AuthUiState()
        showRegisterDialog.value = true
        showLoginDialog.value = false
    }

    fun dismissRegister() {
        showRegisterDialog.value = false
        _authUiState.value = AuthUiState()
    }

    fun openForgotPassword() {
        _authUiState.value = AuthUiState()
        showForgotPasswordDialog.value = true
        showLoginDialog.value = false
    }

    fun dismissForgotPassword() {
        showForgotPasswordDialog.value = false
        _authUiState.value = AuthUiState()
    }

    fun openEditProfile() {
        showEditProfileDialog.value = true
    }

    fun dismissEditProfile() {
        showEditProfileDialog.value = false
    }

    fun openDeleteAccount() {
        showDeleteAccountDialog.value = true
    }

    fun dismissDeleteAccount() {
        showDeleteAccountDialog.value = false
    }

    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = authRepository.loginWithEmail(email, pass)
            result.onSuccess { user ->
                _authUiState.value = AuthUiState()
                showLoginDialog.value = false
                // Flow: Auth success -> GET /me -> PUT /devices
                syncWithBackendAfterAuth(user)
            }.onFailure { error ->
                _authUiState.value = AuthUiState(
                    errorMessage = error.localizedMessage ?: "Đăng nhập thất bại"
                )
            }
        }
    }

    fun registerWithEmail(email: String, pass: String, confirmPass: String) {
        if (pass != confirmPass) {
            _authUiState.value = AuthUiState(errorMessage = "Mật khẩu xác nhận không khớp")
            return
        }
        if (pass.length < 12) {
            _authUiState.value = AuthUiState(errorMessage = "Mật khẩu phải có ít nhất 12 ký tự")
            return
        }

        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val result = authRepository.registerWithEmail(email, pass)
            result.onSuccess {
                _authUiState.value = AuthUiState(
                    successMessage = "Tạo tài khoản thành công! Vui lòng kiểm tra email để xác thực."
                )
                showRegisterDialog.value = false
            }.onFailure { error ->
                _authUiState.value = AuthUiState(
                    errorMessage = error.localizedMessage ?: "Đăng ký thất bại"
                )
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        val serverClientId = activityContext.getString(R.string.default_web_client_id)

        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            val tokenResult = googleSignInHelper.getGoogleIdToken(activityContext, serverClientId)
            tokenResult.onSuccess { idToken ->
                val authResult = authRepository.signInWithGoogle(idToken)
                authResult.onSuccess { user ->
                    _authUiState.value = AuthUiState()
                    showLoginDialog.value = false
                    syncWithBackendAfterAuth(user)
                }.onFailure { err ->
                    _authUiState.value = AuthUiState(
                        errorMessage = err.localizedMessage ?: "Đăng nhập Google thất bại"
                    )
                }
            }.onFailure { err ->
                _authUiState.value = AuthUiState(
                    errorMessage = err.localizedMessage ?: "Không thể kết nối tài khoản Google"
                )
            }
        }
    }

    fun sendEmailVerification() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            authRepository.sendEmailVerification()
                .onSuccess {
                    _authUiState.value = AuthUiState(
                        successMessage = "Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư!"
                    )
                }
                .onFailure { err ->
                    _authUiState.value = AuthUiState(
                        errorMessage = err.localizedMessage ?: "Không thể gửi email xác thực"
                    )
                }
        }
    }

    fun reloadVerification() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            authRepository.reloadUser()
                .onSuccess { newState ->
                    _authUiState.value = AuthUiState()
                    if (newState is AuthState.Authenticated) {
                        syncWithBackendAfterAuth(newState.user)
                    }
                }
                .onFailure { err ->
                    _authUiState.value = AuthUiState(
                        errorMessage = err.localizedMessage ?: "Không thể làm mới trạng thái"
                    )
                }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            authRepository.sendPasswordReset(email)
                .onSuccess {
                    _authUiState.value = AuthUiState(
                        successMessage = "Đã gửi email khôi phục mật khẩu. Vui lòng kiểm tra hộp thư!"
                    )
                }
                .onFailure { err ->
                    _authUiState.value = AuthUiState(
                        errorMessage = err.localizedMessage ?: "Không thể gửi email khôi phục"
                    )
                }
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)
            // Primary source of truth: backend UserProfile
            accountRepository.updateProfile(displayName = name)
                .onSuccess { updatedProfile ->
                    _userProfile.value = updatedProfile
                    // Refresh the cached authentication profile
                    authRepository.updateProfile(displayName = name)
                    _authUiState.value = AuthUiState()
                    showEditProfileDialog.value = false
                }
                .onFailure { err ->
                    _authUiState.value = AuthUiState(
                        errorMessage = err.localizedMessage ?: "Không thể cập nhật tên hiển thị trên máy chủ"
                    )
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { deviceRepository.unregisterDevice() }
            authRepository.signOut()
            _userProfile.value = null
            _authUiState.value = AuthUiState()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _authUiState.value = AuthUiState(isLoading = true)

            val result = authRepository.deleteAccount()
            if (result.isFailure) {
                _authUiState.value = AuthUiState(errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Không thể xóa tài khoản")
                return@launch
            }
            // 3. Only on complete success: transition to Guest
            showDeleteAccountDialog.value = false
            _userProfile.value = null
            _authUiState.value = AuthUiState(successMessage = "Tài khoản đã được xóa thành công")
        }
    }

    fun continueAsGuest() {
        authRepository.continueAsGuest()
        showLoginDialog.value = false
        showRegisterDialog.value = false
        _authUiState.value = AuthUiState()
    }

    // Reader Preferences methods
    fun updateTheme(theme: ReaderTheme) {
        viewModelScope.launch { preferencesDataStore.updateTheme(theme) }
    }

    fun updateFontFamily(fontFamily: ReaderFontFamily) {
        viewModelScope.launch { preferencesDataStore.updateFontFamily(fontFamily) }
    }

    fun updateFontSize(multiplier: Float) {
        viewModelScope.launch { preferencesDataStore.updateFontSize(multiplier) }
    }

    fun updateLineHeight(multiplier: Float) {
        viewModelScope.launch { preferencesDataStore.updateLineHeight(multiplier) }
    }

    fun updateTextAlignment(alignment: ReaderTextAlignment) {
        viewModelScope.launch { preferencesDataStore.updateTextAlignment(alignment) }
    }

    fun updateScrollMode(isScrollMode: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateScrollMode(isScrollMode) }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            preferencesDataStore.updateTheme(ReaderTheme.LIGHT)
            preferencesDataStore.updateFontFamily(ReaderFontFamily.SYSTEM_DEFAULT)
            preferencesDataStore.updateFontSize(1.0f)
            preferencesDataStore.updateLineHeight(1.4f)
            preferencesDataStore.updateTextAlignment(ReaderTextAlignment.JUSTIFY)
            preferencesDataStore.updateScrollMode(false)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val authRepo = CloudAuthRepository.getInstance(appContext)
                val accountRepo = BackendAccountRepository(tokenProvider = authRepo)
                val deviceDataStore = DevicePreferencesDataStore(appContext)
                val deviceRepo = LocalDeviceRepository(
                    devicePreferencesDataStore = deviceDataStore,
                    tokenProvider = authRepo
                )

                return SettingsViewModel(
                    preferencesDataStore = ReaderPreferencesDataStore(appContext),
                    authRepository = authRepo,
                    accountRepository = accountRepo,
                    deviceRepository = deviceRepo,
                    googleSignInHelper = GoogleSignInHelper(),
                    cloudBackup = CloudBackupRepository(appContext)
                ) as T
            }
        }
    }
}
