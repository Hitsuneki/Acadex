package com.example.acadex.data.repository

import android.util.Log
import com.example.acadex.data.model.UserProfile
import com.example.acadex.data.result.RepoResult
import com.example.acadex.data.result.userMessage
import com.example.acadex.data.supabase.ProfileRow
import com.example.acadex.data.supabase.ProfileUpdate
import com.example.acadex.data.supabase.ProfileUpsert
import com.example.acadex.data.supabase.SupabaseClient
import com.example.acadex.util.UserIdentity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object ProfileRepository {

    private const val TAG = "ProfileRepository"
    private const val NETWORK = "No connection. Please check your internet."
    private const val SERVER = "Something went wrong. Please try again."

    private val _cachedProfile = MutableStateFlow<UserProfile?>(null)
    val cachedProfile: StateFlow<UserProfile?> = _cachedProfile.asStateFlow()

    suspend fun ensureProfileExists(): RepoResult<UserProfile> = withContext(Dispatchers.IO) {
        val uid = UserIdentity.requireUid()
        val displayName = UserIdentity.displayName()
        runRepo {
            val existing = runCatching {
                client().postgrest.from("profiles").select {
                    filter { eq("id", uid) }
                }.decodeSingle<ProfileRow>()
            }.getOrNull()

            val profile = if (existing != null) {
                existing.toUserProfile()
            } else {
                client().postgrest.from("profiles").upsert(
                    ProfileUpsert(id = uid, displayName = displayName, section = "")
                ) {
                    select()
                }.decodeSingle<ProfileRow>().toUserProfile()
            }
            _cachedProfile.value = profile
            profile
        }
    }

    suspend fun loadProfile(force: Boolean = false): RepoResult<UserProfile> = withContext(Dispatchers.IO) {
        if (!force) {
            _cachedProfile.value?.let { return@withContext RepoResult.Success(it) }
        }
        val uid = UserIdentity.requireUid()
        runRepo {
            val row = client().postgrest.from("profiles").select {
                filter { eq("id", uid) }
            }.decodeSingle<ProfileRow>()
            val profile = row.toUserProfile()
            _cachedProfile.value = profile
            profile
        }
    }

    suspend fun updateProfile(displayName: String, section: String): RepoResult<UserProfile> =
        withContext(Dispatchers.IO) {
            val uid = UserIdentity.requireUid()
            runRepo {
                client().postgrest.from("profiles").update(
                    ProfileUpdate(displayName = displayName, section = section)
                ) {
                    filter { eq("id", uid) }
                }
                FirebaseAuth.getInstance().currentUser
                    ?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build())
                    ?.await()
                val profile = UserProfile(uid, displayName, section)
                _cachedProfile.value = profile
                profile
            }
        }

    private inline fun <T> runRepo(block: () -> T): RepoResult<T> {
        if (!SupabaseClient.isConfigured) return RepoResult.Error(SERVER)
        return try {
            RepoResult.Success(block())
        } catch (e: Exception) {
            Log.e(TAG, "Profile error", e)
            RepoResult.Error(e.userMessage(NETWORK, SERVER), e)
        }
    }

    private fun client() = SupabaseClient.instance

    private fun ProfileRow.toUserProfile() = UserProfile(
        id = id,
        displayName = displayName,
        section = section
    )
}
