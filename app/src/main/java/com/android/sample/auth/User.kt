package ch.epfl.euler.auth

/**
 * Modèle de données utilisateur
 *
 * @property username Le nom d'utilisateur (généralement l'email)
 * @property accessToken Le token d'accès pour les appels API
 * @property tenantId L'identifiant du tenant Azure AD (peut être null)
 */
data class User(val username: String, val accessToken: String, val tenantId: String? = null)
