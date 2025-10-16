package com.android.sample.auth

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for the authentication repository
 *
 * This interface allows you to decouple the authentication logic from the specific implementation
 * (MSAL, Firebase, etc.) and facilitates unit testing.
 *
 * Now supports multiple users (Multi-Account).
 */
interface AuthRepository {

    /** Current authentication status */
    val authState: StateFlow<AuthState>

    /** Currently selected user (null if no user) */
    val currentUser: StateFlow<User?>

    /** List of all connected users */
    val connectedUsers: StateFlow<List<User>>

    /**
     * Initializes the authentication system
     *
     * @param context The application context
     * @param configResourceId The ID of the configuration resource
     */
    fun initialize(context: Context, configResourceId: Int)

    /**
     * Launches the interactive login process
     *
     * @param activity The activity from which to launch the login
     * @param scopes The requested scopes/permissions
     */
    fun signIn(activity: Activity, scopes: Array<String> = arrayOf("User.Read"))

    /** Logs out all users */
    fun signOut()

    /**
     * Retrieves an access token silently (without user interaction) for the currently selected user
     *
     * @param scopes The requested scopes/permissions
     */
    fun acquireTokenSilently(scopes: Array<String> = arrayOf("User.Read"))

    /** Checks if users are already logged in. Useful when starting the application */
    fun checkSignedInUser()

    /** Force a new connection attempt after a network error */
    fun retryConnection()
}
