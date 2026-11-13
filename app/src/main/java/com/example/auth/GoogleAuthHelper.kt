package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleAuthHelper {
  private const val TAG = "AwakenAuth"

  fun attemptSilentSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onDoneUnauthenticated: () -> Unit
  ) {
    if (Firebase.auth.currentUser != null) {
      onSuccess()
      return
    }
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onDoneUnauthenticated()
      return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
          Firebase.auth.signInWithCredential(firebaseCred).await()
          onSuccess()
        } else {
          onDoneUnauthenticated()
        }
      } catch (_: Exception) {
        onDoneUnauthenticated()
      }
    }
  }

  fun signInWithGoogleInteractive(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onCancelled: () -> Unit,
    onError: (String) -> Unit
  ) {
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onError("Google Sign-In configuration missing.")
      return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val activityContext = context as? Activity ?: context
        val result = credentialManager.getCredential(activityContext, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
          Firebase.auth.signInWithCredential(firebaseCred).await()
          onSuccess()
        } else {
          onError("Unexpected credential type received.")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
        onCancelled()
      } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        onError(e.localizedMessage ?: "Sign-in failed. Please try again.")
      }
    }
  }

  fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onComplete: () -> Unit = {}
  ) {
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w(TAG, "Failed to clear credential state: ${e.message}")
        } finally {
          Firebase.auth.signOut()
          onComplete()
        }
      }
    }
  }
}
