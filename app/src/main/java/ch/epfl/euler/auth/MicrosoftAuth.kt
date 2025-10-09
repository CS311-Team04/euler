package ch.epfl.euler.auth

import android.app.Activity
import android.util.Base64
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.json.JSONObject

/**
 * Configuration de l’enforcement côté client.
 * - Mettez ici votre TENANT_ID Azure si vous voulez verrouiller sur votre tenant.
 * - Laissez vide pour ne pas vérifier.
 */
private const val EXPECTED_TENANT_ID: String = "7526e0f9-0dcf-4152-afc0-fed84b0c0360" // ex: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
private const val DOMAIN_HINT: String = "ch.epfl.euler"        // ex: "votre-domaine.com" (facilite l’UX)

fun signInWithMicrosoft(
    activity: Activity,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val builder = OAuthProvider.newBuilder("microsoft.com")
        // Scopes standards d’OIDC (Firebase n’a pas besoin de Microsoft Graph pour un simple login)
        .setScopes(listOf("openid", "email", "profile"))
        .addCustomParameter("prompt", "select_account")

    if (DOMAIN_HINT.isNotBlank()) {
        builder.addCustomParameter("domain_hint", DOMAIN_HINT)
    }

    val auth = Firebase.auth

    // Si un flux était déjà en cours (rotation, etc.)
    val pending = auth.pendingAuthResult
    if (pending != null) {
        pending.addOnSuccessListener { result ->
            if (enforceTenantIfNeeded(result.credential as? OAuthCredential)) {
                onSuccess()
            } else {
                // Déconnexion si mauvais tenant
                Firebase.auth.signOut()
                onError(IllegalAccessException("Wrong tenant"))
            }
        }.addOnFailureListener(onError)
        return
    }

    auth.startActivityForSignInWithProvider(activity, builder.build())
        .addOnSuccessListener { result ->
            if (enforceTenantIfNeeded(result.credential as? OAuthCredential)) {
                onSuccess()
            } else {
                Firebase.auth.signOut()
                onError(IllegalAccessException("Wrong tenant"))
            }
        }
        .addOnFailureListener(onError)
}

/**
 * Si EXPECTED_TENANT_ID est défini, vérifie que la claim 'tid' du JWT Microsoft correspond.
 */
private fun enforceTenantIfNeeded(cred: OAuthCredential?): Boolean {
    if (EXPECTED_TENANT_ID.isBlank()) return true
    val idToken = cred?.idToken ?: return false
    val tid = extractTidFromJwt(idToken) ?: return false
    return tid.equals(EXPECTED_TENANT_ID, ignoreCase = true)
}

/** Extraction minimaliste de la claim 'tid' depuis le JWT (base64url). */
private fun extractTidFromJwt(idToken: String): String? {
    val parts = idToken.split(".")
    if (parts.size < 2) return null
    val payloadB64url = parts[1]
    val payloadB64 = payloadB64url.replace('-', '+').replace('_', '/')
    val padded = when (payloadB64.length % 4) {
        2 -> "$payloadB64=="
        3 -> "$payloadB64="
        else -> payloadB64
    }
    val bytes = try { Base64.decode(padded, Base64.DEFAULT) } catch (_: Exception) { return null }
    val json = try { JSONObject(String(bytes)) } catch (_: Exception) { return null }
    return json.optString("tid", null)
}
