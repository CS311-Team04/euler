# Module d'Authentification

Ce module fournit une architecture propre et modulaire pour gérer l'authentification Microsoft (Azure AD) dans l'application.

## Architecture

L'architecture suit le pattern **Repository** et respecte les principes **SOLID** :

```
┌─────────────┐
│  MainActivity│
│    (UI)     │
└──────┬──────┘
       │
       ↓
┌─────────────┐
│AuthViewModel│  ← Gère l'état pour l'UI
└──────┬──────┘
       │
       ↓
┌─────────────┐
│AuthRepository│  ← Interface (abstraction)
└──────┬──────┘
       │
       ↓
┌──────────────────┐
│MsalAuthRepository│  ← Implémentation MSAL
└──────────────────┘
```

## Fichiers

### `AuthState.kt`
Définit les états possibles de l'authentification :
- `NotInitialized` : État initial
- `Ready` : Prêt pour la connexion
- `SigningIn` : Connexion en cours
- `SignedIn` : Utilisateur connecté
- `Error` : Une erreur s'est produite

### `User.kt`
Modèle de données représentant un utilisateur connecté.

### `AuthRepository.kt`
Interface définissant le contrat pour l'authentification. Permet de :
- Découpler la logique d'authentification de son implémentation
- Faciliter les tests unitaires (mock/fake)
- Changer facilement de fournisseur d'authentification

### `MsalAuthRepository.kt`
Implémentation concrète utilisant **Microsoft MSAL**.
- Pattern Singleton
- Gestion complète du cycle de vie MSAL
- Vérification automatique de l'utilisateur connecté

### `AuthViewModel.kt` (dans viewmodel/)
ViewModel qui :
- Expose les états d'authentification à l'UI
- Fournit des états calculés (`isLoading`, `errorMessage`, etc.)
- Gère le cycle de vie avec `viewModelScope`

## Utilisation

### 1. Initialisation dans l'Activity

```kotlin
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialiser l'authentification
        authViewModel.initializeAuth(this, R.raw.msal_config)
        
        setContent {
            YourApp(authViewModel)
        }
    }
}
```

### 2. Utilisation dans un Composable

```kotlin
@Composable
fun AuthScreen(authViewModel: AuthViewModel, activity: Activity) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    
    when (authState) {
        is AuthState.Ready -> {
            Button(onClick = { authViewModel.signIn(activity) }) {
                Text("Se connecter")
            }
        }
        is AuthState.SignedIn -> {
            Text("Bonjour ${currentUser?.username}")
            Button(onClick = { authViewModel.signOut() }) {
                Text("Se déconnecter")
            }
        }
        is AuthState.Error -> {
            Text("Erreur : ${(authState as AuthState.Error).message}")
        }
        else -> CircularProgressIndicator()
    }
}
```

### 3. Rafraîchir le token

```kotlin
// Rafraîchir le token d'accès silencieusement
authViewModel.refreshToken()

// Avec des scopes personnalisés
authViewModel.refreshToken(arrayOf("User.Read", "Mail.Read"))
```

## Avantages de cette architecture

### 🎯 Séparation des responsabilités
- **UI (Activity/Composable)** : Affichage
- **ViewModel** : Gestion de l'état UI
- **Repository** : Logique d'authentification
- **Models** : Données

### 🧪 Testabilité
```kotlin
class AuthViewModelTest {
    @Test
    fun `test sign in success`() {
        val fakeRepo = FakeAuthRepository()
        val viewModel = AuthViewModel(fakeRepo)
        // ... tests
    }
}
```

### 🔄 Flexibilité
Facile de changer de fournisseur d'authentification :
```kotlin
// Passer de MSAL à Firebase
class FirebaseAuthRepository : AuthRepository {
    // Implémentation Firebase
}

val viewModel = AuthViewModel(FirebaseAuthRepository.getInstance())
```

### 📦 Réutilisabilité
Le même repository peut être utilisé dans plusieurs ViewModels ou activités.

### 🛡️ Type-safe
Utilisation de `sealed class` pour les états garantit l'exhaustivité des `when` expressions.

## Configuration MSAL

Le fichier `res/raw/msal_config.json` doit contenir :

```json
{
    "client_id": "votre-client-id",
    "authorization_user_agent": "DEFAULT",
    "redirect_uri": "msauth://com.votre.package/hash",
    "authorities": [
        {
            "type": "AAD",
            "audience": {
                "type": "AzureADMyOrg",
                "tenant_id": "votre-tenant-id"
            }
        }
    ]
}
```

## Logs

Tous les logs utilisent le tag approprié :
- `MsalAuthRepository` : Logs de l'authentification MSAL

## Gestion des erreurs

Les erreurs sont propagées via `AuthState.Error` et peuvent être affichées dans l'UI :

```kotlin
val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

errorMessage?.let { error ->
    ErrorCard(message = error)
}
```

## Future améliorations possibles

- [ ] Ajouter un cache local pour l'utilisateur
- [ ] Implémenter un système de retry automatique
- [ ] Ajouter des analytics pour tracker les événements d'auth
- [ ] Support de l'authentification biométrique
- [ ] Ajouter des tests unitaires complets
