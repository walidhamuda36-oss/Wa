package com.example.ui

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import com.example.data.model.Category
import com.example.data.model.DecryptedVaultItem
import com.example.data.model.ProfileData
import com.example.data.model.VaultField
import com.example.security.AuthManager
import com.example.security.BiometricHelper
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val isPinSetup: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val autoLockMinutes: Int = 1,
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val categories: List<Category> = emptyList(),
    val items: List<DecryptedVaultItem> = emptyList(),
    val profileData: ProfileData = ProfileData(),
    val selectedItemForDetail: DecryptedVaultItem? = null,
    val isAddEditDialogVisible: Boolean = false,
    val itemToEdit: DecryptedVaultItem? = null,
    val isCategoryManagerVisible: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val authErrorMessage: String? = null,
    val pinInput: String = "",
    val newPinInput: String = "",
    val confirmPinInput: String = "",
    val isSettingUpNewPin: Boolean = false,
    val showPinSetupDialog: Boolean = false
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val cryptoManager = CryptoManager()
    private val database = VaultDatabase.getDatabase(application)
    private val repository = VaultRepository(database.vaultDao(), cryptoManager)
    private val biometricHelper = BiometricHelper(application)

    private val _uiState = MutableStateFlow(
        VaultUiState(
            isPinSetup = authManager.isPinSetup,
            isBiometricEnabled = authManager.isBiometricEnabled,
            autoLockMinutes = authManager.autoLockTimeoutMinutes
        )
    )
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Category.DEFAULT_CATEGORIES
    )

    val vaultItems: StateFlow<List<DecryptedVaultItem>> = combine(
        repository.getAllDecryptedItems(),
        _searchQuery,
        _selectedCategory
    ) { items, query, categoryId ->
        items.filter { item ->
            val matchesCategory = categoryId == null || item.categoryId == categoryId
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.fields.any { it.label.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true) } ||
                    item.notes.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Load stored profile data from prefs if available
        loadProfileData()

        // Check if PIN is not setup, ask user to setup PIN first or auto unlock demo
        if (!authManager.isPinSetup) {
            _uiState.update { it.copy(showPinSetupDialog = true) }
        }
    }

    fun unlockWithBiometrics(activity: FragmentActivity) {
        if (!biometricHelper.isBiometricAvailable()) {
            _uiState.update { it.copy(authErrorMessage = "Biometric authentication not supported on this device. Use PIN.") }
            return
        }
        biometricHelper.showBiometricPrompt(
            activity = activity,
            title = "VaultGuard Security Verification",
            subtitle = "Touch sensor or use face recognition to unlock your personal vault",
            onSuccess = {
                _uiState.update { it.copy(isUnlocked = true, authErrorMessage = null, pinInput = "") }
                checkAndSeedSampleData()
            },
            onError = { error ->
                _uiState.update { it.copy(authErrorMessage = error) }
            }
        )
    }

    fun enterPinDigit(digit: String) {
        val currentPin = _uiState.value.pinInput
        if (currentPin.length < 6) {
            val newPin = currentPin + digit
            _uiState.update { it.copy(pinInput = newPin, authErrorMessage = null) }
            if (newPin.length == 4 || newPin.length == 6) {
                if (authManager.verifyMasterPin(newPin)) {
                    _uiState.update { it.copy(isUnlocked = true, pinInput = "", authErrorMessage = null) }
                    checkAndSeedSampleData()
                } else if (newPin.length == 6) {
                    _uiState.update { it.copy(pinInput = "", authErrorMessage = "Incorrect Master PIN. Try again.") }
                }
            }
        }
    }

    fun deletePinDigit() {
        val currentPin = _uiState.value.pinInput
        if (currentPin.isNotEmpty()) {
            _uiState.update { it.copy(pinInput = currentPin.dropLast(1)) }
        }
    }

    fun verifyPinManually() {
        val pin = _uiState.value.pinInput
        if (authManager.verifyMasterPin(pin)) {
            _uiState.update { it.copy(isUnlocked = true, pinInput = "", authErrorMessage = null) }
            checkAndSeedSampleData()
        } else {
            _uiState.update { it.copy(pinInput = "", authErrorMessage = "Incorrect PIN code.") }
        }
    }

    fun setupMasterPin(pin: String) {
        if (pin.length in 4..6) {
            authManager.setMasterPin(pin)
            _uiState.update {
                it.copy(
                    isPinSetup = true,
                    isUnlocked = true,
                    showPinSetupDialog = false,
                    newPinInput = "",
                    confirmPinInput = "",
                    authErrorMessage = null
                )
            }
            checkAndSeedSampleData()
        } else {
            _uiState.update { it.copy(authErrorMessage = "PIN must be 4 to 6 digits long.") }
        }
    }

    fun lockVault() {
        _uiState.update {
            it.copy(
                isUnlocked = false,
                selectedItemForDetail = null,
                pinInput = "",
                authErrorMessage = null
            )
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openItemDetail(item: DecryptedVaultItem) {
        _uiState.update { it.copy(selectedItemForDetail = item) }
    }

    fun closeItemDetail() {
        _uiState.update { it.copy(selectedItemForDetail = null) }
    }

    fun showAddEditDialog(itemToEdit: DecryptedVaultItem? = null) {
        _uiState.update { it.copy(isAddEditDialogVisible = true, itemToEdit = itemToEdit) }
    }

    fun hideAddEditDialog() {
        _uiState.update { it.copy(isAddEditDialogVisible = false, itemToEdit = null) }
    }

    fun saveVaultItem(item: DecryptedVaultItem) {
        viewModelScope.launch {
            repository.saveItem(item)
            hideAddEditDialog()
            // If viewing detail of modified item, update it
            if (_uiState.value.selectedItemForDetail?.id == item.id) {
                _uiState.update { it.copy(selectedItemForDetail = item) }
            }
        }
    }

    fun deleteVaultItem(item: DecryptedVaultItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            closeItemDetail()
        }
    }

    fun toggleFavorite(item: DecryptedVaultItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun showCategoryManager() {
        _uiState.update { it.copy(isCategoryManagerVisible = true) }
    }

    fun hideCategoryManager() {
        _uiState.update { it.copy(isCategoryManagerVisible = false) }
    }

    fun addCustomCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val newCategory = Category(
                id = "cat_custom_${System.currentTimeMillis()}",
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                isCustom = true,
                sortOrder = 10
            )
            repository.addCategory(newCategory)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun showSettings() {
        _uiState.update { it.copy(isSettingsVisible = true) }
    }

    fun hideSettings() {
        _uiState.update { it.copy(isSettingsVisible = false) }
    }

    fun updateBiometricSetting(enabled: Boolean) {
        authManager.setBiometricEnabled(enabled)
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }

    fun updateAutoLockSetting(minutes: Int) {
        authManager.setAutoLockTimeout(minutes)
        _uiState.update { it.copy(autoLockMinutes = minutes) }
    }

    fun updateProfile(newProfile: ProfileData) {
        _uiState.update { it.copy(profileData = newProfile) }
        val prefs = getApplication<Application>().getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("fullName", newProfile.fullName)
            .putString("titleOrRole", newProfile.titleOrRole)
            .putString("avatarUri", newProfile.avatarUri)
            .putString("phoneNumber", newProfile.phoneNumber)
            .putString("email", newProfile.email)
            .putString("nationalIdOrPassport", newProfile.nationalIdOrPassport)
            .putString("address", newProfile.address)
            .putString("emergencyContactName", newProfile.emergencyContactName)
            .putString("emergencyContactPhone", newProfile.emergencyContactPhone)
            .putString("bloodType", newProfile.bloodType)
            .putString("medicalNotes", newProfile.medicalNotes)
            .apply()
    }

    private fun loadProfileData() {
        val prefs = getApplication<Application>().getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
        if (prefs.contains("fullName")) {
            val loaded = ProfileData(
                fullName = prefs.getString("fullName", "John Doe") ?: "John Doe",
                titleOrRole = prefs.getString("titleOrRole", "Personal Vault Owner") ?: "Personal Vault Owner",
                avatarUri = prefs.getString("avatarUri", null),
                phoneNumber = prefs.getString("phoneNumber", "+1 (555) 019-2834") ?: "+1 (555) 019-2834",
                email = prefs.getString("email", "john.doe@example.com") ?: "john.doe@example.com",
                nationalIdOrPassport = prefs.getString("nationalIdOrPassport", "US-98234-1029") ?: "US-98234-1029",
                address = prefs.getString("address", "742 Evergreen Terrace, Springfield") ?: "742 Evergreen Terrace, Springfield",
                emergencyContactName = prefs.getString("emergencyContactName", "Jane Doe (Spouse)") ?: "Jane Doe (Spouse)",
                emergencyContactPhone = prefs.getString("emergencyContactPhone", "+1 (555) 019-9988") ?: "+1 (555) 019-9988",
                bloodType = prefs.getString("bloodType", "O+") ?: "O+",
                medicalNotes = prefs.getString("medicalNotes", "No known allergies.") ?: "No known allergies."
            )
            _uiState.update { it.copy(profileData = loaded) }
        }
    }

    private fun checkAndSeedSampleData() {
        viewModelScope.launch {
            val items = vaultItems.value
            if (items.isEmpty()) {
                // Seed sample entries so user has initial rich personal data records
                val samplePassport = DecryptedVaultItem(
                    title = "US Passport (National ID)",
                    categoryId = "cat_ids",
                    itemType = "ID_NUMBERS",
                    fields = listOf(
                        VaultField("Passport Number", "A982341029", isMasked = true, fieldType = "ID"),
                        VaultField("Issue Date", "2022-04-15", isMasked = false, fieldType = "DATE"),
                        VaultField("Expiry Date", "2032-04-14", isMasked = false, fieldType = "DATE"),
                        VaultField("Issuing Authority", "US Dept of State", isMasked = false, fieldType = "TEXT")
                    ),
                    notes = "Keep stored in fireproof home safe. Used for international travel.",
                    isFavorite = true
                )

                val sampleDriverLicense = DecryptedVaultItem(
                    title = "Driver's License",
                    categoryId = "cat_ids",
                    itemType = "ID_NUMBERS",
                    fields = listOf(
                        VaultField("DL Number", "DL-8371920-CA", isMasked = true, fieldType = "ID"),
                        VaultField("Class", "Class C Standard", isMasked = false, fieldType = "TEXT"),
                        VaultField("Expiration", "2028-11-20", isMasked = false, fieldType = "DATE"),
                        VaultField("State", "California", isMasked = false, fieldType = "TEXT")
                    ),
                    notes = "Organ donor listed. Class C motor vehicle endorsement.",
                    isFavorite = true
                )

                val samplePhoneContact = DecryptedVaultItem(
                    title = "Primary Emergency Contacts",
                    categoryId = "cat_contacts",
                    itemType = "PHONE_CONTACT",
                    fields = listOf(
                        VaultField("Spouse Mobile", "+1 (555) 019-9988", isMasked = false, fieldType = "PHONE"),
                        VaultField("Family Physician", "+1 (555) 321-4567", isMasked = false, fieldType = "PHONE"),
                        VaultField("Insurance Agent Hotline", "+1 (800) 555-7890", isMasked = false, fieldType = "PHONE")
                    ),
                    notes = "Call Spouse first in any emergency scenario.",
                    isFavorite = true
                )

                val sampleHealthCard = DecryptedVaultItem(
                    title = "BlueCross Health Insurance",
                    categoryId = "cat_medical",
                    itemType = "FINANCIAL",
                    fields = listOf(
                        VaultField("Member ID", "XYZ-99201384", isMasked = true, fieldType = "ID"),
                        VaultField("Group Number", "GRP-77102", isMasked = false, fieldType = "TEXT"),
                        VaultField("RxBIN", "004336", isMasked = false, fieldType = "TEXT"),
                        VaultField("PCP Doctor", "Dr. Robert Vance, MD", isMasked = false, fieldType = "TEXT")
                    ),
                    notes = "Full coverage dental and vision included.",
                    isFavorite = false
                )

                val sampleNote = DecryptedVaultItem(
                    title = "Home Wi-Fi & Smart Lock Access Notes",
                    categoryId = "cat_notes",
                    itemType = "NOTE",
                    fields = listOf(
                        VaultField("Network SSID", "Evergreen_5G_Secure", isMasked = false, fieldType = "TEXT"),
                        VaultField("Router Admin IP", "192.168.1.1", isMasked = false, fieldType = "TEXT")
                    ),
                    notes = "Garage door access code: 4892. Main water shutoff valve is located behind the laundry room access panel.",
                    isFavorite = false
                )

                repository.saveItem(samplePassport)
                repository.saveItem(sampleDriverLicense)
                repository.saveItem(samplePhoneContact)
                repository.saveItem(sampleHealthCard)
                repository.saveItem(sampleNote)
            }
        }
    }
}
