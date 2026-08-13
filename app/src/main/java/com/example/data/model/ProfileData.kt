package com.example.data.model

data class ProfileData(
    val fullName: String = "John Doe",
    val titleOrRole: String = "Personal Vault Owner",
    val avatarUri: String? = null,
    val phoneNumber: String = "+1 (555) 019-2834",
    val email: String = "john.doe@example.com",
    val nationalIdOrPassport: String = "US-98234-1029",
    val address: String = "742 Evergreen Terrace, Springfield",
    val emergencyContactName: String = "Jane Doe (Spouse)",
    val emergencyContactPhone: String = "+1 (555) 019-9988",
    val bloodType: String = "O+",
    val medicalNotes: String = "No known allergies. Penicillin sensitive."
)
