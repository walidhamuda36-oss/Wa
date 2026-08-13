package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.fragment.compose.AndroidFragment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.AddEditItemDialog
import com.example.ui.components.CategoryChipSelector
import com.example.ui.components.CategoryManagerDialog
import com.example.ui.components.ItemDetailDialog
import com.example.ui.components.LockScreen
import com.example.ui.components.PersonalProfileHeader
import com.example.ui.components.PinSetupDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.VaultItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVaultScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val items by viewModel.vaultItems.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var isSearchActive by remember { mutableStateOf(false) }

    // If Vault is Locked, display LockScreen
    if (!uiState.isUnlocked) {
        LockScreen(
            isPinSetup = uiState.isPinSetup,
            isBiometricAvailable = uiState.isBiometricEnabled,
            pinInput = uiState.pinInput,
            errorMessage = uiState.authErrorMessage,
            onDigitClick = { viewModel.enterPinDigit(it) },
            onDeleteClick = { viewModel.deletePinDigit() },
            onBiometricClick = { activity?.let { viewModel.unlockWithBiometrics(it) } },
            onSetupPinSubmit = { viewModel.setupMasterPin(it) }
        )

        if (uiState.showPinSetupDialog) {
            PinSetupDialog(
                onPinSubmitted = { viewModel.setupMasterPin(it) }
            )
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search vault items, notes, IDs...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_input")
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "VaultGuard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${items.size} Encrypted Records",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.updateSearchQuery("")
                        },
                        modifier = Modifier.testTag("search_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    IconButton(
                        onClick = { viewModel.showSettings() },
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }

                    IconButton(
                        onClick = { viewModel.lockVault() },
                        modifier = Modifier.testTag("lock_vault_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddEditDialog(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_record_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Personal Vault Record",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Personal Profile Header Card
            PersonalProfileHeader(
                profileData = uiState.profileData,
                onProfileUpdate = { viewModel.updateProfile(it) }
            )

            // Category Chips Selector
            CategoryChipSelector(
                categories = categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = { viewModel.selectCategory(it) },
                onAddCategoryClick = { viewModel.showCategoryManager() }
            )

            // Items List
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Records Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap '+' button below to add your profile details, documents, phone numbers, ID numbers, or notes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        val cat = categories.find { it.id == item.categoryId }
                        VaultItemCard(
                            item = item,
                            category = cat,
                            onClick = { viewModel.openItemDetail(item) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) }
                        )
                    }
                }
            }
        }
    }

    // Dialog Overlays
    if (uiState.selectedItemForDetail != null) {
        val detailItem = uiState.selectedItemForDetail!!
        val category = categories.find { it.id == detailItem.categoryId }
        ItemDetailDialog(
            item = detailItem,
            category = category,
            onDismiss = { viewModel.closeItemDetail() },
            onEditClick = {
                viewModel.closeItemDetail()
                viewModel.showAddEditDialog(detailItem)
            },
            onDeleteClick = { viewModel.deleteVaultItem(detailItem) },
            onToggleFavorite = { viewModel.toggleFavorite(detailItem) }
        )
    }

    if (uiState.isAddEditDialogVisible) {
        AddEditItemDialog(
            itemToEdit = uiState.itemToEdit,
            categories = categories,
            onDismiss = { viewModel.hideAddEditDialog() },
            onSave = { viewModel.saveVaultItem(it) }
        )
    }

    if (uiState.isCategoryManagerVisible) {
        CategoryManagerDialog(
            categories = categories,
            onDismiss = { viewModel.hideCategoryManager() },
            onAddCategory = { name, icon, color -> viewModel.addCustomCategory(name, icon, color) },
            onDeleteCategory = { viewModel.deleteCategory(it) }
        )
    }

    if (uiState.isSettingsVisible) {
        SettingsDialog(
            isBiometricEnabled = uiState.isBiometricEnabled,
            autoLockMinutes = uiState.autoLockMinutes,
            onDismiss = { viewModel.hideSettings() },
            onBiometricToggle = { viewModel.updateBiometricSetting(it) },
            onAutoLockChange = { viewModel.updateAutoLockSetting(it) },
            onChangePinSubmit = { viewModel.setupMasterPin(it) },
            onLockVaultImmediately = {
                viewModel.hideSettings()
                viewModel.lockVault()
            }
        )
    }
}
