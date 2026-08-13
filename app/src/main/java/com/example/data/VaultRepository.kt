package com.example.data

import com.example.data.model.Category
import com.example.data.model.DecryptedVaultItem
import com.example.data.model.VaultField
import com.example.data.model.VaultItem
import com.example.security.CryptoManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultRepository(
    private val vaultDao: VaultDao,
    private val cryptoManager: CryptoManager
) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val fieldsListType = Types.newParameterizedType(List::class.java, VaultField::class.java)
    private val fieldsAdapter = moshi.adapter<List<VaultField>>(fieldsListType)

    val allCategories: Flow<List<Category>> = vaultDao.getAllCategories()

    fun getAllDecryptedItems(): Flow<List<DecryptedVaultItem>> {
        return vaultDao.getAllItems().map { items ->
            items.map { decryptItem(it) }
        }
    }

    fun getItemsByCategory(categoryId: String): Flow<List<DecryptedVaultItem>> {
        return vaultDao.getItemsByCategory(categoryId).map { items ->
            items.map { decryptItem(it) }
        }
    }

    fun searchItems(query: String): Flow<List<DecryptedVaultItem>> {
        return vaultDao.searchItems(query).map { items ->
            items.map { decryptItem(it) }
        }
    }

    suspend fun saveItem(item: DecryptedVaultItem): Long {
        val jsonFields = fieldsAdapter.toJson(item.fields)
        val encryptedFields = cryptoManager.encrypt(jsonFields)
        val encryptedNotes = cryptoManager.encrypt(item.notes)

        val rawEntity = VaultItem(
            id = item.id,
            title = item.title,
            categoryId = item.categoryId,
            itemType = item.itemType,
            encryptedFieldsJson = encryptedFields,
            encryptedNotes = encryptedNotes,
            photoUri = item.photoUri,
            isFavorite = item.isFavorite,
            isSensitive = item.isSensitive,
            createdAt = if (item.id == 0L) System.currentTimeMillis() else item.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        return if (item.id == 0L) {
            vaultDao.insertItem(rawEntity)
        } else {
            vaultDao.updateItem(rawEntity)
            item.id
        }
    }

    suspend fun deleteItem(item: DecryptedVaultItem) {
        vaultDao.deleteItemById(item.id)
    }

    suspend fun toggleFavorite(item: DecryptedVaultItem) {
        val updated = item.copy(isFavorite = !item.isFavorite)
        saveItem(updated)
    }

    suspend fun addCategory(category: Category) {
        vaultDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        vaultDao.deleteCategory(category)
    }

    private fun decryptItem(item: VaultItem): DecryptedVaultItem {
        val decryptedJson = cryptoManager.decrypt(item.encryptedFieldsJson)
        val decryptedNotes = cryptoManager.decrypt(item.encryptedNotes)

        val fieldsList = try {
            if (decryptedJson.isNotEmpty()) {
                fieldsAdapter.fromJson(decryptedJson) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return DecryptedVaultItem(
            id = item.id,
            title = item.title,
            categoryId = item.categoryId,
            itemType = item.itemType,
            fields = fieldsList,
            notes = decryptedNotes,
            photoUri = item.photoUri,
            isFavorite = item.isFavorite,
            isSensitive = item.isSensitive,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt
        )
    }
}
