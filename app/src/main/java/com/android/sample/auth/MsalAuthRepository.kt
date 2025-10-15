package com.android.sample.auth

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MsalAuthRepository private constructor() : AuthRepository {

  companion object {
    private const val TAG = "MsalAuthRepository"

    @Volatile private var INSTANCE: MsalAuthRepository? = null

    /** Retrieves the singleton instance from the repository */
    fun getInstance(): MsalAuthRepository {
      return INSTANCE
          ?: synchronized(this) { INSTANCE ?: MsalAuthRepository().also { INSTANCE = it } }
    }
  }

  private var msalApp: IMultipleAccountPublicClientApplication? = null
  private var context: Context? = null
  private var configResourceId: Int? = null
  private var isInitialized = false

  private val _authState = MutableStateFlow<AuthState>(AuthState.NotInitialized)
  override val authState: StateFlow<AuthState> = _authState.asStateFlow()

  // Storage of connected users (username -> User)
  private val _connectedUsers = MutableStateFlow<Map<String, User>>(emptyMap())

  // Currently selected user
  private val _currentUser = MutableStateFlow<User?>(null)
  override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

  // List of all connected users
  override val connectedUsers: StateFlow<List<User>> =
      _connectedUsers
          .map { it.values.toList() }
          .stateIn(
              scope = CoroutineScope(Dispatchers.Main),
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = emptyList())

  override fun initialize(context: Context, configResourceId: Int) {
    this.context = context.applicationContext
    this.configResourceId = configResourceId
    try {
      PublicClientApplication.createMultipleAccountPublicClientApplication(
          context.applicationContext,
          configResourceId,
          object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
            override fun onCreated(application: IMultipleAccountPublicClientApplication) {
              msalApp = application
              isInitialized = true
              Log.d(TAG, "MSAL multi-account initialized successfully")

              // Check already connected users
              checkSignedInUsers()
            }

            override fun onError(exception: MsalException) {
              val errorMsg = "MSAL multi-account initialization error: ${exception.message}"
              _authState.value = AuthState.Error(errorMsg)
              Log.e(TAG, errorMsg, exception)
            }
          })
    } catch (e: Exception) {
      val errorMsg = "Exception during multi-account initialization: ${e.message}"
      _authState.value = AuthState.Error(errorMsg)
      Log.e(TAG, errorMsg, e)
    }
  }

  override fun checkSignedInUser() {
    checkSignedInUsers()
  }

  /** Checks all connected users (multi-account method) */
  private fun checkSignedInUsers() {
    val app = msalApp
    if (app == null) {
      Log.e(TAG, "MSAL not initialized")
      _authState.value = AuthState.Ready
      return
    }

    Log.d(TAG, "Checking connected users...")

    try {
      val accounts = app.accounts
      val usersMap = mutableMapOf<String, User>()

      if (accounts.isNotEmpty()) {
        Log.d(TAG, "Found ${accounts.size} connected user(s)")

        accounts.forEach { account ->
          val user =
              User(
                  username = account.username,
                  accessToken = "", // Token will be retrieved silently if needed
                  tenantId = account.tenantId)
          usersMap[account.username] = user
          Log.d(TAG, "Connected user: ${user.username}")
        }

        _connectedUsers.value = usersMap

        // If no current user selected, take the first one
        if (_currentUser.value == null && usersMap.isNotEmpty()) {
          _currentUser.value = usersMap.values.first()
        }

        _authState.value = AuthState.SignedIn
      } else {
        Log.d(TAG, "No connected users")
        _connectedUsers.value = emptyMap()
        _currentUser.value = null
        _authState.value = AuthState.Ready
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error checking accounts", e)
      _authState.value = AuthState.Ready
    }
  }

  override fun signIn(activity: Activity, scopes: Array<String>) {
    val app = msalApp
    if (!isInitialized || app == null) {
      val errorMsg = "MSAL not initialized. Call initialize() first."
      _authState.value = AuthState.Error(errorMsg)
      Log.e(TAG, errorMsg)

      // Attempt automatic reinitialization
      Log.d(TAG, "Attempting automatic reinitialization...")
      context?.let { ctx -> configResourceId?.let { configId -> initialize(ctx, configId) } }
      return
    }

    _authState.value = AuthState.SigningIn
    Log.d(TAG, "Starting multi-account connection with scopes: ${scopes.joinToString()}")

    app.acquireToken(
        activity,
        scopes,
        object : AuthenticationCallback {
          override fun onSuccess(result: IAuthenticationResult) {
            val user =
                User(
                    username = result.account.username,
                    accessToken = result.accessToken,
                    tenantId = result.account.tenantId)

            // Add user to connected users list
            val currentUsers = _connectedUsers.value.toMutableMap()
            currentUsers[user.username] = user
            _connectedUsers.value = currentUsers

            // Set as current user
            _currentUser.value = user
            _authState.value = AuthState.SignedIn

            Log.d(TAG, "Connection successful: ${user.username}")
            Log.d(TAG, "Total connected users: ${currentUsers.size}")
          }

          override fun onError(exception: MsalException) {
            val errorMsg = "Connection error: ${exception.message}"
            _authState.value = AuthState.Error(errorMsg)
            Log.e(TAG, errorMsg, exception)
            Log.e(TAG, "MSAL error code: ${exception.errorCode}")
          }

          override fun onCancel() {
            _authState.value = AuthState.Ready
            Log.d(TAG, "Connection cancelled by user")
          }
        })
  }

  override fun signOut() {
    val app = msalApp
    if (!isInitialized || app == null) {
      Log.w(TAG, "Attempting sign out while MSAL is not initialized")
      return
    }

    Log.d(TAG, "Signing out all users...")

    try {
      // Sign out all accounts
      val accounts = app.accounts
      accounts.forEach { account ->
        app.removeAccount(account)
        Log.d(TAG, "Account removed: ${account.username}")
      }

      // Clean local state
      _connectedUsers.value = emptyMap()
      _currentUser.value = null
      _authState.value = AuthState.Ready

      Log.d(TAG, "All users signed out successfully")
    } catch (e: Exception) {
      val errorMsg = "Error during sign out: ${e.message}"
      _authState.value = AuthState.Error(errorMsg)
      Log.e(TAG, errorMsg, e)
    }
  }

  /** Signs out a specific user */
  fun signOutUser(username: String) {
    val app = msalApp
    if (app == null) {
      Log.w(TAG, "MSAL not initialized for sign out of $username")
      return
    }

    Log.d(TAG, "Signing out user: $username")

    try {
      val accounts = app.accounts
      val accountToRemove = accounts.find { it.username == username }

      if (accountToRemove != null) {
        app.removeAccount(accountToRemove)

        // Update local state
        val currentUsers = _connectedUsers.value.toMutableMap()
        currentUsers.remove(username)
        _connectedUsers.value = currentUsers

        // If it was the current user, select another one or null
        if (_currentUser.value?.username == username) {
          _currentUser.value = currentUsers.values.firstOrNull()
        }

        // Update authentication state
        if (currentUsers.isEmpty()) {
          _authState.value = AuthState.Ready
        }

        Log.d(TAG, "Sign out successful for: $username")
      } else {
        Log.w(TAG, "User not found for sign out: $username")
      }
    } catch (e: Exception) {
      val errorMsg = "Error signing out $username: ${e.message}"
      _authState.value = AuthState.Error(errorMsg)
      Log.e(TAG, errorMsg, e)
    }
  }

  override fun acquireTokenSilently(scopes: Array<String>) {
    val app = msalApp
    val user = _currentUser.value

    if (!isInitialized || app == null) {
      Log.e(TAG, "MSAL not initialized")
      return
    }

    if (user == null) {
      val errorMsg = "No connected user to refresh token"
      Log.w(TAG, errorMsg)
      _authState.value = AuthState.Error(errorMsg)
      return
    }

    // Check network connectivity (more permissive)
    if (!isNetworkAvailable()) {
      Log.w(TAG, "No network connection detected, attempting fallback mode...")

      // Diagnose network issue
      diagnoseNetworkIssue()

      // Still try to refresh token in fallback mode
      attemptFallbackTokenRefresh(app, user, scopes)
      return
    }

    // Log to indicate token refresh attempt
    Log.d(TAG, "Network connection OK, attempting token refresh...")

    Log.d(TAG, "Silent token renewal for: ${user.username}")

    // Find corresponding account
    val accounts = app.accounts
    val account = accounts.find { it.username == user.username }

    if (account == null) {
      val errorMsg = "Account not found for user: ${user.username}"
      Log.e(TAG, errorMsg)
      _authState.value = AuthState.Error(errorMsg)
      return
    }

    val silentParameters =
        AcquireTokenSilentParameters.Builder()
            .forAccount(account)
            .fromAuthority(account.authority)
            .withScopes(scopes.toList())
            .withCallback(
                object : SilentAuthenticationCallback {
                  override fun onSuccess(result: IAuthenticationResult) {
                    val updatedUser =
                        User(
                            username = result.account.username,
                            accessToken = result.accessToken,
                            tenantId = result.account.tenantId)

                    // Update user in list
                    val currentUsers = _connectedUsers.value.toMutableMap()
                    currentUsers[updatedUser.username] = updatedUser
                    _connectedUsers.value = currentUsers

                    // Update current user
                    _currentUser.value = updatedUser

                    Log.d(TAG, "Token renewed successfully for: ${updatedUser.username}")
                  }

                  override fun onError(exception: MsalException) {
                    Log.w(
                        TAG,
                        "Unable to silently renew token for ${user.username}",
                        exception)

                    // Handle different types of errors
                    when {
                      exception.errorCode == "invalid_grant" -> {
                        Log.w(TAG, "Token expired, signing out user: ${user.username}")
                        signOutUser(user.username)
                      }
                      exception.message?.contains("network", ignoreCase = true) == true ||
                          exception.message?.contains("connection", ignoreCase = true) == true ||
                          exception.errorCode == "network_error" -> {
                        val errorMsg =
                            "Connection error: Connection is not available to refresh token"
                        _authState.value = AuthState.Error(errorMsg)
                        Log.e(
                            TAG,
                            "Network error during token renewal for ${user.username}",
                            exception)

                        // Schedule automatic reconnection attempt
                        retryAfterNetworkRestore()
                      }
                      else -> {
                        Log.w(
                            TAG,
                            "Error during token renewal for ${user.username}: ${exception.errorCode}",
                            exception)
                      }
                    }
                  }
                })
            .build()

    app.acquireTokenSilentAsync(silentParameters)
  }

  /**
   * Checks if a network connection is available
   *
   * @return true if a network connection is available, false otherwise
   */
  private fun isNetworkAvailable(): Boolean {
    return try {
      val ctx = context ?: return false
      val connectivityManager =
          ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val network = connectivityManager.activeNetwork ?: return false
      val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

      // Detailed logs for debugging
      Log.d(TAG, "Checking network connectivity:")
      Log.d(TAG, "- Active network: $network")
      Log.d(TAG, "- Network capabilities: $networkCapabilities")

      // Check if network is connected and has Internet connectivity
      val hasInternet =
          networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      val hasValidatedInternet =
          networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

      Log.d(TAG, "- Has Internet: $hasInternet")
      Log.d(TAG, "- Validated Internet: $hasValidatedInternet")

      val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
      val hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
      val hasEthernet = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

      Log.d(TAG, "- WiFi: $hasWifi, Cellular: $hasCellular, Ethernet: $hasEthernet")

      // Use NetworkHelper for decision logic
      val isConnected = NetworkHelper.isConnected(hasInternet, hasWifi, hasCellular, hasEthernet)

      Log.d(TAG, "- Network connection available: $isConnected")
      isConnected
    } catch (e: Exception) {
      Log.e(TAG, "Error checking network connectivity", e)
      false
    }
  }

  /** Automatically retries connection after network loss */
  private fun retryAfterNetworkRestore() {
    // Wait a bit for connection to stabilize
    android.os
        .Handler(android.os.Looper.getMainLooper())
        .postDelayed(
            {
              if (isNetworkAvailable()) {
                Log.d(TAG, "Network restored, attempting automatic reconnection...")
                // Retry checking signed in user
                checkSignedInUser()
              }
            },
            2000) // Wait 2 seconds
  }

  override fun retryConnection() {
    Log.d(TAG, "Manual reconnection attempt...")

    // Test connectivity to Microsoft servers
    testMicrosoftConnectivity()

    if (!isNetworkAvailable()) {
      Log.w(TAG, "No network connection detected, attempting fallback...")

      // Still try to check connected users
      // sometimes network detection can be incorrect
      attemptFallbackConnection()
      return
    }

    // If we have a connected user, try to refresh token
    val user = _currentUser.value
    if (user != null) {
      Log.d(TAG, "Connected user detected, attempting token refresh...")
      acquireTokenSilently(arrayOf("User.Read"))
    } else {
      Log.d(TAG, "No connected user, checking accounts...")
      checkSignedInUsers()
    }
  }

  /** Changes the currently selected user */
  fun switchUser(username: String) {
    val users = _connectedUsers.value
    val user = users[username]

    if (user != null) {
      _currentUser.value = user
      Log.d(TAG, "Current user changed to: $username")
    } else {
      Log.w(TAG, "User not found for switch: $username")
    }
  }

  /** Gets the number of connected users */
  fun getConnectedUsersCount(): Int {
    return _connectedUsers.value.size
  }

  /** Checks if a specific user is connected */
  fun isUserConnected(username: String): Boolean {
    return _connectedUsers.value.containsKey(username)
  }

  /** Attempts token refresh in fallback mode */
  private fun attemptFallbackTokenRefresh(
      app: IMultipleAccountPublicClientApplication,
      user: User,
      scopes: Array<String>
  ) {
    Log.d(TAG, "Attempting token refresh in fallback mode...")

    // Find corresponding account
    val accounts = app.accounts
    val account = accounts.find { it.username == user.username }

    if (account == null) {
      Log.e(TAG, "Account not found for fallback: ${user.username}")
      return
    }

    val silentParameters =
        AcquireTokenSilentParameters.Builder()
            .forAccount(account)
            .fromAuthority(account.authority)
            .withScopes(scopes.toList())
            .withCallback(
                object : SilentAuthenticationCallback {
                  override fun onSuccess(result: IAuthenticationResult) {
                    val updatedUser =
                        User(
                            username = result.account.username,
                            accessToken = result.accessToken,
                            tenantId = result.account.tenantId)

                    // Update user in list
                    val currentUsers = _connectedUsers.value.toMutableMap()
                    currentUsers[updatedUser.username] = updatedUser
                    _connectedUsers.value = currentUsers

                    // Update current user
                    _currentUser.value = updatedUser

                    Log.d(
                        TAG,
                        "✓ Token refreshed successfully in fallback mode for: ${updatedUser.username}")
                  }

                  override fun onError(exception: MsalException) {
                    val errorMsg = "Connection error: ${exception.message}"
                    Log.e(TAG, "✗ Token refresh failed in fallback mode", exception)
                    _authState.value = AuthState.Error(errorMsg)

                    // Schedule automatic reconnection attempt
                    retryAfterNetworkRestore()
                  }
                })
            .build()

    app.acquireTokenSilentAsync(silentParameters)
  }

  /** Attempts connection in fallback mode when network detection fails */
  private fun attemptFallbackConnection() {
    Log.d(TAG, "Attempting connection in fallback mode...")

    val user = _currentUser.value
    if (user == null) {
      val errorMsg = "No connected user for reconnection"
      Log.w(TAG, errorMsg)
      _authState.value = AuthState.Error(errorMsg)
      return
    }

    val app = msalApp
    if (app == null) {
      val errorMsg = "MSAL not initialized for reconnection"
      Log.e(TAG, errorMsg)
      _authState.value = AuthState.Error(errorMsg)
      return
    }

    // Try to refresh token even without network detection
    Log.d(TAG, "Attempting token refresh in fallback mode...")

    // Find corresponding account
    val accounts = app.accounts
    val account = accounts.find { it.username == user.username }

    if (account == null) {
      Log.e(TAG, "Account not found for connection fallback: ${user.username}")
      return
    }

    val silentParameters =
        AcquireTokenSilentParameters.Builder()
            .forAccount(account)
            .fromAuthority(account.authority)
            .withScopes(listOf("User.Read"))
            .withCallback(
                object : SilentAuthenticationCallback {
                  override fun onSuccess(result: IAuthenticationResult) {
                    val updatedUser =
                        User(
                            username = result.account.username,
                            accessToken = result.accessToken,
                            tenantId = result.account.tenantId)

                    // Update user in list
                    val currentUsers = _connectedUsers.value.toMutableMap()
                    currentUsers[updatedUser.username] = updatedUser
                    _connectedUsers.value = currentUsers

                    _currentUser.value = updatedUser
                    _authState.value = AuthState.SignedIn
                    Log.d(TAG, "✓ Reconnection successful in fallback mode")
                  }

                  override fun onError(exception: MsalException) {
                    val errorMsg = "Connection error: ${exception.message}"
                    Log.e(TAG, "✗ Reconnection failed in fallback mode", exception)
                    _authState.value = AuthState.Error(errorMsg)
                  }
                })
            .build()

    app.acquireTokenSilentAsync(silentParameters)
  }

  /** Tests connectivity to Microsoft servers */
  private fun testMicrosoftConnectivity() {
    Log.d(TAG, "Testing connectivity to Microsoft servers...")

    // Simple DNS resolution test
    try {
      val addresses = java.net.InetAddress.getAllByName("login.microsoftonline.com")
      Log.d(TAG, "✓ DNS resolved for login.microsoftonline.com: ${addresses.map { it.hostAddress }}")
    } catch (e: Exception) {
      Log.e(TAG, "✗ DNS resolution error for login.microsoftonline.com", e)
    }

    try {
      val addresses = java.net.InetAddress.getAllByName("graph.microsoft.com")
      Log.d(TAG, "✓ DNS resolved for graph.microsoft.com: ${addresses.map { it.hostAddress }}")
    } catch (e: Exception) {
      Log.e(TAG, "✗ DNS resolution error for graph.microsoft.com", e)
    }

    // Basic HTTP connectivity test
    testHttpConnectivity()
  }

  /** Basic HTTP connectivity test to Microsoft */
  private fun testHttpConnectivity() {
    Log.d(TAG, "Basic HTTP connectivity test...")

    try {
      val url =
          java.net.URL("https://login.microsoftonline.com/common/.well-known/openid_configuration")
      val connection = url.openConnection() as java.net.HttpURLConnection
      connection.requestMethod = "HEAD"
      connection.connectTimeout = 5000
      connection.readTimeout = 5000

      val responseCode = connection.responseCode
      Log.d(TAG, "✓ HTTP test to Microsoft: $responseCode")

      connection.disconnect()
    } catch (e: Exception) {
      Log.e(TAG, "✗ HTTP test to Microsoft failed", e)
    }
  }

  /** Diagnoses network issue in detail */
  private fun diagnoseNetworkIssue() {
    Log.d(TAG, "=== NETWORK DIAGNOSTIC ===")

    try {
      val ctx = context ?: return
      val connectivityManager =
          ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

      // Check all available networks
      val allNetworks = connectivityManager.allNetworks
      Log.d(TAG, "Number of available networks: ${allNetworks.size}")

      allNetworks.forEachIndexed { index, network ->
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        Log.d(TAG, "Network $index: $network")
        Log.d(TAG, "  - Capabilities: $capabilities")

        if (capabilities != null) {
          Log.d(
              TAG,
              "  - Internet: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}")
          Log.d(
              TAG,
              "  - Validated: ${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")
          Log.d(TAG, "  - WiFi: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}")
          Log.d(
              TAG,
              "  - Cellular: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)}")
          Log.d(
              TAG,
              "  - Ethernet: ${capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)}")
        }
      }

      // Check active network
      val activeNetwork = connectivityManager.activeNetwork
      Log.d(TAG, "Active network: $activeNetwork")

      // Basic Internet connectivity test
      testBasicInternetConnectivity()
    } catch (e: Exception) {
      Log.e(TAG, "Error during network diagnostic", e)
    }

    Log.d(TAG, "=== END DIAGNOSTIC ===")
  }

  /** Basic Internet connectivity test */
  private fun testBasicInternetConnectivity() {
    Log.d(TAG, "Basic Internet connectivity test...")

    // Test with known sites
    val testHosts =
        listOf(
            "8.8.8.8", // Google DNS
            "1.1.1.1", // Cloudflare DNS
            "login.microsoftonline.com",
            "www.microsoft.com")

    testHosts.forEach { host ->
      try {
        val addresses = java.net.InetAddress.getAllByName(host)
        Log.d(TAG, "✓ $host resolved: ${addresses.map { it.hostAddress }}")
      } catch (e: Exception) {
        Log.e(TAG, "✗ $host not resolved: ${e.message}")
      }
    }
  }
}
