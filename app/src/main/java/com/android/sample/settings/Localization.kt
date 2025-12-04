package com.android.sample.settings

/**
 * Type-safe localization helper for multiple languages. Reads `AppSettings.language` so Compose
 * will automatically recompose when language changes.
 */
object Localization {
  /**
   * Translate a key to the current language string. Falls back to English if the key is not found
   * in the selected language.
   */
  fun t(key: String): String {
    return when (AppSettings.language) {
      Language.FR -> french()[key] ?: english()[key] ?: key
      Language.DE -> german()[key] ?: english()[key] ?: key
      Language.ES -> spanish()[key] ?: english()[key] ?: key
      Language.IT -> italian()[key] ?: english()[key] ?: key
      Language.PT -> portuguese()[key] ?: english()[key] ?: key
      Language.ZH -> chinese()[key] ?: english()[key] ?: key
      Language.EN -> english()[key] ?: key
    }
  }

  fun appearanceLabel(mode: AppearanceMode): String {
    val key =
        when (mode) {
          AppearanceMode.SYSTEM -> "appearance_system"
          AppearanceMode.LIGHT -> "appearance_light"
          AppearanceMode.DARK -> "appearance_dark"
        }
    return t(key)
  }

  private fun edTranslationsEn() =
      mapOf(
          "ed_connect_generic_error" to "Failed to connect to ED. Please try again.",
          "ed_connect_invalid_credentials" to "Invalid credentials. Please check your API token.",
          "ed_connect_api_unreachable" to "ED API is unreachable. Please check your connection.",
          "settings_connectors_ed_title" to "Connect to ED",
          "settings_connectors_ed_api_token_label" to "ED API token",
          "settings_connectors_ed_base_url_label" to "Base URL (optional)",
          "settings_connectors_ed_token_instructions" to
              "To connect ED, you need a personal API token. We'll store it securely and you won't have to paste it again.",
          "settings_connectors_ed_get_token_button" to "Open ED Token Page",
          "settings_connectors_ed_description" to "Connect to the Q&A platform",
          "settings_connectors_ed_how_to_connect_title" to "How to connect",
          "settings_connectors_ed_step_1" to "Tap the button below to open your ED API token page",
          "settings_connectors_ed_step_2" to "Copy the token to clipboard",
          "settings_connectors_ed_step_3" to "Come back here and paste it below",
          "settings_connectors_ed_paste_token_label" to "Paste your token",
          "settings_connectors_ed_paste_token_placeholder" to "Paste your token",
          "settings_connectors_ed_base_url_placeholder" to "Base URL (optional)",
          "settings_connectors_ed_load_error" to "Failed to load ED connector status",
          "settings_connectors_ed_status_error" to
              "Unable to load ED connector status. Please try again later.",
          "settings_connectors_ed_token_detected_title" to "ED token detected!",
          "settings_connectors_ed_use_token_button" to "Use this token",
          "settings_connectors_ed_not_now_button" to "Not now",
          "paste" to "Paste")

  private fun edTranslationsFr() =
      mapOf(
          "ed_connect_generic_error" to "La connexion à ED a échoué. Veuillez réessayer.",
          "ed_connect_invalid_credentials" to
              "Identifiants invalides. Veuillez vérifier votre jeton API.",
          "ed_connect_api_unreachable" to
              "L'API ED est inaccessible. Veuillez vérifier votre connexion.",
          "settings_connectors_ed_title" to "Se connecter à ED",
          "settings_connectors_ed_api_token_label" to "Jeton API ED",
          "settings_connectors_ed_base_url_label" to "URL de base (optionnel)",
          "settings_connectors_ed_token_instructions" to
              "Pour vous connecter à ED, vous avez besoin d'un jeton API personnel. Nous le stockerons de manière sécurisée et vous n'aurez plus à le coller.",
          "settings_connectors_ed_get_token_button" to "Ouvrir la page de jeton ED",
          "settings_connectors_ed_description" to "Se connecter à la plateforme Q&A",
          "settings_connectors_ed_how_to_connect_title" to "Comment se connecter",
          "settings_connectors_ed_step_1" to
              "Appuyez sur le bouton ci-dessous pour ouvrir votre page de jeton API ED",
          "settings_connectors_ed_step_2" to "Copiez le jeton dans le presse-papiers",
          "settings_connectors_ed_step_3" to "Revenez ici et collez-le ci-dessous",
          "settings_connectors_ed_paste_token_label" to "Collez votre jeton",
          "settings_connectors_ed_paste_token_placeholder" to "Collez votre jeton",
          "settings_connectors_ed_base_url_placeholder" to "URL de base (optionnel)",
          "settings_connectors_ed_load_error" to "Échec du chargement du statut du connecteur ED",
          "settings_connectors_ed_status_error" to
              "Impossible de charger le statut du connecteur ED. Veuillez réessayer plus tard.",
          "settings_connectors_ed_token_detected_title" to "Jeton ED détecté !",
          "settings_connectors_ed_use_token_button" to "Utiliser ce jeton",
          "settings_connectors_ed_not_now_button" to "Pas maintenant",
          "paste" to "Coller")

  private fun edTranslationsDe() =
      mapOf(
          "ed_connect_generic_error" to
              "Verbindung zu ED fehlgeschlagen. Bitte versuchen Sie es erneut.",
          "ed_connect_invalid_credentials" to
              "Ungültige Anmeldedaten. Bitte überprüfen Sie Ihr API-Token.",
          "ed_connect_api_unreachable" to
              "ED-API ist nicht erreichbar. Bitte überprüfen Sie Ihre Verbindung.",
          "settings_connectors_ed_title" to "Mit ED verbinden",
          "settings_connectors_ed_api_token_label" to "ED API-Token",
          "settings_connectors_ed_base_url_label" to "Basis-URL (optional)",
          "settings_connectors_ed_token_instructions" to
              "Um sich mit ED zu verbinden, benötigen Sie einen persönlichen API-Token. Wir speichern ihn sicher und Sie müssen ihn nicht erneut einfügen.",
          "settings_connectors_ed_get_token_button" to "Token abrufen",
          "settings_connectors_ed_description" to "Mit der Q&A-Plattform verbinden",
          "settings_connectors_ed_how_to_connect_title" to "So verbinden Sie sich",
          "settings_connectors_ed_step_1" to
              "Öffnen Sie https://eu.edstem.org/settings/api-tokens in Ihrem Browser",
          "settings_connectors_ed_step_2" to "Kopieren Sie das Token in die Zwischenablage",
          "settings_connectors_ed_step_3" to "Kommen Sie hierher zurück und fügen Sie es unten ein",
          "settings_connectors_ed_paste_token_label" to "Fügen Sie Ihr Token ein",
          "settings_connectors_ed_paste_token_placeholder" to "Fügen Sie Ihr Token ein",
          "settings_connectors_ed_base_url_placeholder" to "Basis-URL (optional)",
          "settings_connectors_ed_load_error" to "Fehler beim Laden des ED-Connector-Status",
          "settings_connectors_ed_status_error" to
              "ED-Connector-Status konnte nicht geladen werden. Bitte versuchen Sie es später erneut.",
          "paste" to "Einfügen")

  private fun edTranslationsEs() =
      mapOf(
          "ed_connect_generic_error" to "Error al conectar con ED. Por favor, inténtelo de nuevo.",
          "ed_connect_invalid_credentials" to
              "Credenciales inválidas. Por favor, verifique su token API.",
          "ed_connect_api_unreachable" to
              "La API de ED no es accesible. Por favor, verifique su conexión.",
          "settings_connectors_ed_title" to "Conectar a ED",
          "settings_connectors_ed_api_token_label" to "Token API de ED",
          "settings_connectors_ed_base_url_label" to "URL base (opcional)",
          "settings_connectors_ed_token_instructions" to
              "Para conectarse a ED, necesita un token API personal. Lo almacenaremos de forma segura y no tendrá que pegarlo de nuevo.",
          "settings_connectors_ed_get_token_button" to "Obtener token",
          "settings_connectors_ed_description" to "Conectarse a la plataforma Q&A",
          "settings_connectors_ed_how_to_connect_title" to "Cómo conectarse",
          "settings_connectors_ed_step_1" to
              "Abra https://eu.edstem.org/settings/api-tokens en su navegador",
          "settings_connectors_ed_step_2" to "Copie el token al portapapeles",
          "settings_connectors_ed_step_3" to "Vuelva aquí y péguelo a continuación",
          "settings_connectors_ed_paste_token_label" to "Pegue su token",
          "settings_connectors_ed_paste_token_placeholder" to "Pegue su token",
          "settings_connectors_ed_base_url_placeholder" to "URL base (opcional)",
          "settings_connectors_ed_load_error" to "Error al cargar el estado del conector ED",
          "settings_connectors_ed_status_error" to
              "No se pudo cargar el estado del conector ED. Por favor, inténtelo más tarde.",
          "paste" to "Pegar")

  private fun edTranslationsIt() =
      mapOf(
          "ed_connect_generic_error" to "Connessione a ED fallita. Riprova.",
          "ed_connect_invalid_credentials" to "Credenziali non valide. Controlla il tuo token API.",
          "ed_connect_api_unreachable" to
              "L'API ED non è raggiungibile. Controlla la tua connessione.",
          "settings_connectors_ed_title" to "Connetti a ED",
          "settings_connectors_ed_api_token_label" to "Token API ED",
          "settings_connectors_ed_base_url_label" to "URL base (opzionale)",
          "settings_connectors_ed_token_instructions" to
              "Per connetterti a ED, hai bisogno di un token API personale. Lo memorizzeremo in modo sicuro e non dovrai più incollarlo.",
          "settings_connectors_ed_get_token_button" to "Ottieni token",
          "settings_connectors_ed_description" to "Connettiti alla piattaforma Q&A",
          "settings_connectors_ed_how_to_connect_title" to "Come connettersi",
          "settings_connectors_ed_step_1" to
              "Apri https://eu.edstem.org/settings/api-tokens nel tuo browser",
          "settings_connectors_ed_step_2" to "Copia il token negli appunti",
          "settings_connectors_ed_step_3" to "Torna qui e incollalo qui sotto",
          "settings_connectors_ed_paste_token_label" to "Incolla il tuo token",
          "settings_connectors_ed_paste_token_placeholder" to "Incolla il tuo token",
          "settings_connectors_ed_base_url_placeholder" to "URL base (opzionale)",
          "settings_connectors_ed_load_error" to
              "Errore nel caricamento dello stato del connettore ED",
          "settings_connectors_ed_status_error" to
              "Impossibile caricare lo stato del connettore ED. Riprova più tardi.",
          "paste" to "Incolla")

  private fun edTranslationsPt() =
      mapOf(
          "ed_connect_generic_error" to "Falha ao conectar ao ED. Tente novamente.",
          "ed_connect_invalid_credentials" to "Credenciais inválidas. Verifique seu token API.",
          "ed_connect_api_unreachable" to "A API ED não está acessível. Verifique sua conexão.",
          "settings_connectors_ed_title" to "Conectar ao ED",
          "settings_connectors_ed_api_token_label" to "Token API do ED",
          "settings_connectors_ed_base_url_label" to "URL base (opcional)",
          "settings_connectors_ed_token_instructions" to
              "Para conectar ao ED, você precisa de um token API pessoal. Vamos armazená-lo com segurança e você não precisará colá-lo novamente.",
          "settings_connectors_ed_get_token_button" to "Obter token",
          "settings_connectors_ed_description" to "Conectar-se à plataforma Q&A",
          "settings_connectors_ed_how_to_connect_title" to "Como conectar",
          "settings_connectors_ed_step_1" to
              "Abra https://eu.edstem.org/settings/api-tokens no seu navegador",
          "settings_connectors_ed_step_2" to "Copie o token para a área de transferência",
          "settings_connectors_ed_step_3" to "Volte aqui e cole abaixo",
          "settings_connectors_ed_paste_token_label" to "Cole seu token",
          "settings_connectors_ed_paste_token_placeholder" to "Cole seu token",
          "settings_connectors_ed_base_url_placeholder" to "URL base (opcional)",
          "settings_connectors_ed_load_error" to "Falha ao carregar o status do conector ED",
          "settings_connectors_ed_status_error" to
              "Não foi possível carregar o status do conector ED. Tente novamente mais tarde.",
          "paste" to "Colar")

  private fun edTranslationsZh() =
      mapOf(
          "ed_connect_generic_error" to "连接到 ED 失败。请重试。",
          "ed_connect_invalid_credentials" to "凭据无效。请检查您的 API 令牌。",
          "ed_connect_api_unreachable" to "ED API 无法访问。请检查您的连接。",
          "settings_connectors_ed_title" to "连接到 ED",
          "settings_connectors_ed_api_token_label" to "ED API 令牌",
          "settings_connectors_ed_base_url_label" to "基础 URL（可选）",
          "settings_connectors_ed_token_instructions" to
              "要连接到 ED，您需要一个个人 API 令牌。我们将安全地存储它，您无需再次粘贴。",
          "settings_connectors_ed_get_token_button" to "获取令牌",
          "settings_connectors_ed_description" to "连接到 Q&A 平台",
          "settings_connectors_ed_how_to_connect_title" to "如何连接",
          "settings_connectors_ed_step_1" to "在浏览器中打开 https://eu.edstem.org/settings/api-tokens",
          "settings_connectors_ed_step_2" to "将令牌复制到剪贴板",
          "settings_connectors_ed_step_3" to "返回此处并在下方粘贴",
          "settings_connectors_ed_paste_token_label" to "粘贴您的令牌",
          "settings_connectors_ed_paste_token_placeholder" to "粘贴您的令牌",
          "settings_connectors_ed_base_url_placeholder" to "基础 URL（可选）",
          "settings_connectors_ed_load_error" to "加载 ED 连接器状态失败",
          "settings_connectors_ed_status_error" to "无法加载 ED 连接器状态。请稍后再试。",
          "paste" to "粘贴")

  private fun english() =
      mapOf(
          // Settings
          "settings_title" to "Settings",
          "profile" to "Profile",
          "connectors" to "Connectors",
          "Connect_your_academic_services" to "Connect your academic services",
          "appearance" to "Appearance",
          "appearance_system" to "System default",
          "appearance_light" to "Light",
          "appearance_dark" to "Dark",
          "speech_language" to "Speech language",
          "log_out" to "Log out",
          "close" to "Close",
          "info" to "Info",
          "by_epfl" to "BY EPFL",
          "connected" to "Connected",
          "not_connected" to "Not connected",
          "connect" to "Connect",
          "disconnect" to "Disconnect",
          "disconnect_confirm_title" to "Disconnect?",
          "disconnect_confirm_message" to "Are you sure you want to disconnect %s?",
          "cancel" to "Cancel",

          // Home Screen - Navigation
          "menu" to "Menu",
          "euler" to "Euler",
          "more" to "More",
          "send" to "Send",
          "dictate" to "Dictate",
          "voice_mode" to "Voice mode",

          // Home Screen - Messages
          "message_euler" to "Message EULER",
          "euler_thinking" to "Euler is thinking",
          "ask_euler_anything" to "Ask Euler Anything",

          // Home Screen - Suggestions
          "suggestion_what_is_epfl" to "What is EPFL",
          "suggestion_check_ed" to "Check Ed Discussion",
          "suggestion_show_schedule" to "Show my schedule",
          "suggestion_library" to "Find library resources",
          "suggestion_check_grades" to "Check grades on IS-Academia",
          "suggestion_search_moodle" to "Search Moodle courses",
          "suggestion_whats_due" to "What's due this week?",
          "suggestion_study_help" to "Help me study for CS220",

          // Home Screen - Animated Intro Suggestions
          "intro_suggestion_1" to "Find CS220 past exams",
          "intro_suggestion_2" to "Check my Moodle assignments",
          "intro_suggestion_3" to "What's on Ed Discussion?",
          "intro_suggestion_4" to "Show my IS-Academia schedule",
          "intro_suggestion_5" to "Search EPFL Drive files",

          // Home Screen - Actions
          "share" to "Share",
          "delete" to "Delete",
          "clear_chat" to "Delete chat?",
          "clear_chat_message" to "This will delete all messages. This action cannot be undone.",
          "cancel" to "Cancel",

          // Drawer
          "euler_logo" to "Euler Logo",
          "new_chat" to "New chat",
          "recents" to "RECENTS",
          "view_all_chats" to "View all chats",
          "powered_by" to "Powered by Apertus",
          "settings" to "Settings",
          "all_chats" to "ALL CHATS",
          "no_conversations_yet" to "No conversations yet",
          "default_user_name" to "Student",
          "untitled_conversation" to "Untitled",

          // Drawer - Recent Items
          "recent_cs220_exam" to "CS220 Final Exam retrieval",
          "recent_linear_algebra" to "Linear Algebra help",
          "recent_deadline" to "Project deadline query",
          "recent_registration" to "Course registration info",

          // EPFL Campus Connector
          "epfl_campus_title" to "EPFL Campus",
          "epfl_campus_subtitle" to "Connect your schedule",
          "epfl_instructions_title" to "How to connect",
          "epfl_step_1" to "Open EPFL Campus app → My Schedule",
          "epfl_step_2" to "Tap 📤 → 'Copy .ics link to clipboard' ",
          "epfl_step_3" to "Come back here and paste it below",
          "epfl_open_campus" to "Open EPFL Campus",
          "epfl_paste_url" to "Paste your ICS URL",
          "epfl_connect" to "Connect Schedule",
          "epfl_syncing" to "Syncing...",
          "epfl_connected" to "Connected",
          "epfl_weekly_classes" to "weekly classes",
          "epfl_exams" to "exams",
          "epfl_last_sync" to "Last sync",
          "epfl_disconnect" to "Disconnect",
          "epfl_connected_info" to "Your schedule is now connected to the AI assistant.",
          "epfl_clipboard_detected" to "ICS URL detected!",
          "not_now" to "Not now",
          "use_this_url" to "Use this URL") +
          edTranslationsEn() +
          mapOf(
              "moodle_connect_generic_error" to "Failed to connect to Moodle. Please try again.",
              "moodle_connect_api_unreachable" to
                  "Moodle API is unreachable. Please check your connection.",
              "settings_connectors_moodle_title" to "Connect to Moodle",
              "settings_connectors_moodle_base_url_label" to "Moodle URL",
              "settings_connectors_moodle_username_label" to "Username",
              "settings_connectors_moodle_password_label" to "Password",
              "settings_connectors_moodle_login_instructions" to
                  "Enter your Moodle credentials to connect. Your password will be securely sent to your Moodle server.",
              "settings_connectors_moodle_load_error" to "Failed to load Moodle connector status",
              "settings_connectors_moodle_status_error" to
                  "Unable to load Moodle connector status. Please try again later.")

  private fun french() =
      mapOf(
          // Settings
          "settings_title" to "Paramètres",
          "profile" to "Profil",
          "connectors" to "Connecteurs",
          "Connect_your_academic_services" to "Connectez vos services académiques",
          "appearance" to "Apparence",
          "appearance_system" to "Défaut système",
          "appearance_light" to "Clair",
          "appearance_dark" to "Sombre",
          "speech_language" to "Langue de synthèse",
          "log_out" to "Se déconnecter",
          "close" to "Fermer",
          "info" to "Info",
          "by_epfl" to "PAR EPFL",
          "connected" to "Connecté",
          "not_connected" to "Non connecté",
          "connect" to "Connecter",
          "disconnect" to "Déconnecter",
          "disconnect_confirm_title" to "Déconnecter?",
          "disconnect_confirm_message" to "Êtes-vous sûr de vouloir déconnecter %s?",
          "cancel" to "Annuler",

          // Home Screen - Navigation
          "menu" to "Menu",
          "euler" to "Euler",
          "more" to "Plus",
          "send" to "Envoyer",
          "dictate" to "Dicter",
          "voice_mode" to "Mode vocal",

          // Home Screen - Messages
          "message_euler" to "Message EULER",
          "euler_thinking" to "Euler réfléchit",
          "ask_euler_anything" to "Demandez n'importe quoi à Euler",

          // Home Screen - Suggestions
          "suggestion_what_is_epfl" to "Qu'est-ce que l'EPFL",
          "suggestion_check_ed" to "Vérifier Ed Discussion",
          "suggestion_show_schedule" to "Afficher mon horaire",
          "suggestion_library" to "Trouver des ressources de bibliothèque",
          "suggestion_check_grades" to "Vérifier les notes sur IS-Academia",
          "suggestion_search_moodle" to "Rechercher des cours Moodle",
          "suggestion_whats_due" to "Qu'est-ce qui est dû cette semaine ?",
          "suggestion_study_help" to "Aide-moi à étudier pour CS220",

          // Home Screen - Animated Intro Suggestions
          "intro_suggestion_1" to "Trouver les examens passés de CS220",
          "intro_suggestion_2" to "Vérifier mes devoirs Moodle",
          "intro_suggestion_3" to "Quoi de neuf sur Ed Discussion ?",
          "intro_suggestion_4" to "Afficher mon horaire IS-Academia",
          "intro_suggestion_5" to "Rechercher des fichiers EPFL Drive",

          // Home Screen - Actions
          "share" to "Partager",
          "delete" to "Supprimer",
          "clear_chat" to "Effacer le chat ?",
          "clear_chat_message" to
              "Cela supprimera tous les messages. Cette action ne peut pas être annulée.",
          "cancel" to "Annuler",

          // Drawer
          "euler_logo" to "Logo Euler",
          "new_chat" to "Nouveau chat",
          "recents" to "RÉCENTS",
          "view_all_chats" to "Voir tous les chats",
          "powered_by" to "Propulsé par Apertus",
          "settings" to "Paramètres",
          "all_chats" to "TOUS LES CHATS",
          "no_conversations_yet" to "Aucune conversation pour l'instant",
          "default_user_name" to "Étudiant",
          "untitled_conversation" to "Sans titre",

          // Drawer - Recent Items
          "recent_cs220_exam" to "Récupération examen final CS220",
          "recent_linear_algebra" to "Aide en algèbre linéaire",
          "recent_deadline" to "Requête date limite projet",
          "recent_registration" to "Info inscription aux cours",

          // EPFL Campus Connector
          "epfl_campus_title" to "EPFL Campus",
          "epfl_campus_subtitle" to "Connectez votre emploi du temps",
          "epfl_instructions_title" to "Comment se connecter",
          "epfl_step_1" to "Ouvrez EPFL Campus → Horaire",
          "epfl_step_2" to "Tapez ⚙️ → Copier le lien .ics",
          "epfl_step_3" to "Revenez et collez le ici.",
          "epfl_open_campus" to "Ouvrir EPFL Campus",
          "epfl_paste_url" to "Collez votre URL ICS",
          "epfl_connect" to "Connecter l'emploi du temps",
          "epfl_syncing" to "Synchronisation...",
          "epfl_connected" to "Connecté",
          "epfl_weekly_classes" to "cours/semaine",
          "epfl_exams" to "examens",
          "epfl_last_sync" to "Dernière sync",
          "epfl_disconnect" to "Déconnecter",
          "epfl_connected_info" to
              "Votre emploi du temps est maintenant connecté à l'assistant IA.",
          "epfl_clipboard_detected" to "URL ICS détectée !",
          "not_now" to "Plus tard",
          "use_this_url" to "Utiliser cette URL") +
          edTranslationsFr() +
          mapOf(
              "moodle_connect_generic_error" to
                  "La connexion à Moodle a échoué. Veuillez réessayer.",
              "moodle_connect_api_unreachable" to
                  "L'API Moodle est inaccessible. Veuillez vérifier votre connexion.",
              "settings_connectors_moodle_title" to "Se connecter à Moodle",
              "settings_connectors_moodle_base_url_label" to "URL Moodle",
              "settings_connectors_moodle_username_label" to "Nom d'utilisateur",
              "settings_connectors_moodle_password_label" to "Mot de passe",
              "settings_connectors_moodle_login_instructions" to
                  "Entrez vos identifiants Moodle pour vous connecter. Votre mot de passe sera envoyé de manière sécurisée à votre serveur Moodle.",
              "settings_connectors_moodle_load_error" to
                  "Échec du chargement du statut du connecteur Moodle",
              "settings_connectors_moodle_status_error" to
                  "Impossible de charger le statut du connecteur Moodle. Veuillez réessayer plus tard.")

  private fun german() =
      mapOf(
          // Settings
          "settings_title" to "Einstellungen",
          "profile" to "Profil",
          "connectors" to "Konnektoren",
          "Connect_your_academic_services" to "Verbinden Sie Ihre akademischen Dienste",
          "appearance" to "Erscheinungsbild",
          "appearance_system" to "Systemstandard",
          "appearance_light" to "Hell",
          "appearance_dark" to "Dunkel",
          "speech_language" to "Sprachsprache",
          "log_out" to "Abmelden",
          "close" to "Schließen",
          "info" to "Info",
          "by_epfl" to "VON EPFL",
          "connected" to "Verbunden",
          "not_connected" to "Nicht verbunden",
          "connect" to "Verbinden",
          "disconnect" to "Trennen",
          "disconnect_confirm_title" to "Trennen?",
          "disconnect_confirm_message" to "Möchten Sie %s wirklich trennen?",
          "cancel" to "Abbrechen",
          "ed_connect_generic_error" to
              "Verbindung zu ED fehlgeschlagen. Bitte versuchen Sie es erneut.",
          "ed_connect_invalid_credentials" to
              "Ungültige Anmeldedaten. Bitte überprüfen Sie Ihr API-Token.",
          "ed_connect_api_unreachable" to
              "ED-API ist nicht erreichbar. Bitte überprüfen Sie Ihre Verbindung.",
          "settings_connectors_ed_title" to "Mit ED verbinden",
          "settings_connectors_ed_api_token_label" to "ED API-Token",
          "settings_connectors_ed_base_url_label" to "Basis-URL (optional)",
          "settings_connectors_ed_load_error" to "Fehler beim Laden des ED-Connector-Status",
          "settings_connectors_ed_status_error" to
              "ED-Connector-Status konnte nicht geladen werden. Bitte versuchen Sie es später erneut.",

          // Home Screen - Navigation
          "menu" to "Menü",
          "euler" to "Euler",
          "more" to "Mehr",
          "send" to "Senden",
          "dictate" to "Diktieren",
          "voice_mode" to "Sprachmodus",

          // Home Screen - Messages
          "message_euler" to "Nachricht an EULER",
          "euler_thinking" to "Euler denkt nach",
          "ask_euler_anything" to "Fragen Sie Euler alles",

          // Home Screen - Suggestions
          "suggestion_what_is_epfl" to "Was ist EPFL",
          "suggestion_check_ed" to "Ed Discussion prüfen",
          "suggestion_show_schedule" to "Meinen Stundenplan anzeigen",
          "suggestion_library" to "Bibliotheksressourcen finden",
          "suggestion_check_grades" to "Noten auf IS-Academia prüfen",
          "suggestion_search_moodle" to "Moodle-Kurse suchen",
          "suggestion_whats_due" to "Was ist diese Woche fällig?",
          "suggestion_study_help" to "Hilf mir für CS220 zu lernen",

          // Home Screen - Animated Intro Suggestions
          "intro_suggestion_1" to "CS220 frühere Prüfungen finden",
          "intro_suggestion_2" to "Meine Moodle-Aufgaben prüfen",
          "intro_suggestion_3" to "Was gibt's Neues auf Ed Discussion?",
          "intro_suggestion_4" to "Meinen IS-Academia-Stundenplan anzeigen",
          "intro_suggestion_5" to "EPFL Drive-Dateien durchsuchen",

          // Home Screen - Actions
          "share" to "Teilen",
          "delete" to "Löschen",
          "clear_chat" to "Chat löschen?",
          "clear_chat_message" to
              "Dies wird alle Nachrichten löschen. Diese Aktion kann nicht rückgängig gemacht werden.",
          "cancel" to "Abbrechen",

          // Drawer
          "euler_logo" to "Euler-Logo",
          "new_chat" to "Neuer Chat",
          "recents" to "NEUESTE",
          "view_all_chats" to "Alle Chats anzeigen",
          "powered_by" to "Bereitgestellt von Apertus",
          "settings" to "Einstellungen",
          "all_chats" to "ALLE CHATS",
          "no_conversations_yet" to "Noch keine Konversationen",
          "default_user_name" to "Student",
          "untitled_conversation" to "Ohne Titel",

          // Drawer - Recent Items
          "recent_cs220_exam" to "CS220 Abschlussprüfung Abruf",
          "recent_linear_algebra" to "Lineare Algebra Hilfe",
          "recent_deadline" to "Projekt-Frist Anfrage",
          "recent_registration" to "Kursanmeldung Info") +
          edTranslationsDe() +
          mapOf(
              "moodle_connect_generic_error" to
                  "Verbindung zu Moodle fehlgeschlagen. Bitte versuchen Sie es erneut.",
              "moodle_connect_api_unreachable" to
                  "Moodle-API ist nicht erreichbar. Bitte überprüfen Sie Ihre Verbindung.",
              "settings_connectors_moodle_title" to "Mit Moodle verbinden",
              "settings_connectors_moodle_base_url_label" to "Moodle-URL",
              "settings_connectors_moodle_username_label" to "Benutzername",
              "settings_connectors_moodle_password_label" to "Passwort",
              "settings_connectors_moodle_login_instructions" to
                  "Geben Sie Ihre Moodle-Anmeldedaten ein, um sich zu verbinden. Ihr Passwort wird sicher an Ihren Moodle-Server gesendet.",
              "settings_connectors_moodle_load_error" to
                  "Fehler beim Laden des Moodle-Connector-Status",
              "settings_connectors_moodle_status_error" to
                  "Moodle-Connector-Status konnte nicht geladen werden. Bitte versuchen Sie es später erneut.")

  private fun spanish() =
      mapOf(
          // Settings
          "settings_title" to "Configuración",
          "profile" to "Perfil",
          "connectors" to "Conectores",
          "Connect_your_academic_services" to "Conecta tus servicios académicos",
          "appearance" to "Apariencia",
          "appearance_system" to "Sistema",
          "appearance_light" to "Claro",
          "appearance_dark" to "Oscuro",
          "speech_language" to "Idioma de voz",
          "log_out" to "Cerrar sesión",
          "close" to "Cerrar",
          "info" to "Info",
          "by_epfl" to "POR EPFL",
          "connected" to "Conectado",
          "not_connected" to "No conectado",
          "connect" to "Conectar",
          "disconnect" to "Desconectar",
          "disconnect_confirm_title" to "¿Desconectar?",
          "disconnect_confirm_message" to "¿Está seguro de que desea desconectar %s?",
          "cancel" to "Cancelar",

          // Home Screen - Navigation
          "menu" to "Menú",
          "euler" to "Euler",
          "more" to "Más",
          "send" to "Enviar",
          "dictate" to "Dictar",
          "voice_mode" to "Modo de voz",

          // Home Screen - Messages
          "message_euler" to "Mensaje a EULER",
          "euler_thinking" to "Euler está pensando",
          "ask_euler_anything" to "Pregunta a Euler cualquier cosa",

          // Home Screen - Suggestions
          "suggestion_what_is_epfl" to "Qué es EPFL",
          "suggestion_check_ed" to "Verificar Ed Discussion",
          "suggestion_show_schedule" to "Mostrar mi horario",
          "suggestion_library" to "Encontrar recursos de biblioteca",
          "suggestion_check_grades" to "Verificar calificaciones en IS-Academia",
          "suggestion_search_moodle" to "Buscar cursos de Moodle",
          "suggestion_whats_due" to "¿Qué vence esta semana?",
          "suggestion_study_help" to "Ayúdame a estudiar para CS220",

          // Home Screen - Animated Intro Suggestions
          "intro_suggestion_1" to "Encontrar exámenes pasados de CS220",
          "intro_suggestion_2" to "Verificar mis tareas de Moodle",
          "intro_suggestion_3" to "¿Qué hay en Ed Discussion?",
          "intro_suggestion_4" to "Mostrar mi horario de IS-Academia",
          "intro_suggestion_5" to "Buscar archivos de EPFL Drive",

          // Home Screen - Actions
          "share" to "Compartir",
          "delete" to "Eliminar",
          "clear_chat" to "¿Borrar chat?",
          "clear_chat_message" to
              "Esto eliminará todos los mensajes. Esta acción no se puede deshacer.",
          "cancel" to "Cancelar",

          // Drawer
          "euler_logo" to "Logo de Euler",
          "new_chat" to "Nuevo chat",
          "recents" to "RECIENTES",
          "view_all_chats" to "Ver todos los chats",
          "powered_by" to "Impulsado por Apertus",
          "settings" to "Configuración",
          "all_chats" to "TODOS LOS CHATS",
          "no_conversations_yet" to "Aún no hay conversaciones",
          "default_user_name" to "Estudiante",
          "untitled_conversation" to "Sin título",

          // Drawer - Recent Items
          "recent_cs220_exam" to "Recuperación examen final CS220",
          "recent_linear_algebra" to "Ayuda con álgebra lineal",
          "recent_deadline" to "Consulta fecha límite proyecto",
          "recent_registration" to "Info inscripción cursos") +
          edTranslationsEs() +
          mapOf(
              "moodle_connect_generic_error" to
                  "Error al conectar con Moodle. Por favor, inténtelo de nuevo.",
              "moodle_connect_api_unreachable" to
                  "La API de Moodle no es accesible. Por favor, verifique su conexión.",
              "settings_connectors_moodle_title" to "Conectar a Moodle",
              "settings_connectors_moodle_base_url_label" to "URL de Moodle",
              "settings_connectors_moodle_username_label" to "Nombre de usuario",
              "settings_connectors_moodle_password_label" to "Contraseña",
              "settings_connectors_moodle_login_instructions" to
                  "Ingrese sus credenciales de Moodle para conectarse. Su contraseña se enviará de forma segura a su servidor Moodle.",
              "settings_connectors_moodle_load_error" to
                  "Error al cargar el estado del conector Moodle",
              "settings_connectors_moodle_status_error" to
                  "No se pudo cargar el estado del conector Moodle. Por favor, inténtelo más tarde.")

  private fun italian() =
      mapOf(
          // Settings
          "settings_title" to "Impostazioni",
          "profile" to "Profilo",
          "connectors" to "Connettori",
          "Connect_your_academic_services" to "Connetti i tuoi servizi accademici",
          "appearance" to "Aspetto",
          "appearance_system" to "Sistema",
          "appearance_light" to "Chiaro",
          "appearance_dark" to "Scuro",
          "speech_language" to "Lingua vocale",
          "log_out" to "Disconnetti",
          "close" to "Chiudi",
          "info" to "Info",
          "by_epfl" to "DA EPFL",
          "connected" to "Connesso",
          "not_connected" to "Non connesso",
          "connect" to "Connetti",
          "disconnect" to "Disconnetti",
          "disconnect_confirm_title" to "Disconnettere?",
          "disconnect_confirm_message" to "Sei sicuro di voler disconnettere %s?",
          "cancel" to "Annulla",

          // Home Screen - Navigation
          "menu" to "Menu",
          "euler" to "Euler",
          "more" to "Altro",
          "send" to "Invia",
          "dictate" to "Dettare",
          "voice_mode" to "Modalità vocale",

          // Home Screen - Messages
          "message_euler" to "Messaggio a EULER",
          "euler_thinking" to "Euler sta pensando",
          "ask_euler_anything" to "Chiedi qualsiasi cosa a Euler",

          // Home Screen - Suggestions
          "suggestion_what_is_epfl" to "Cos'è EPFL",
          "suggestion_check_ed" to "Controlla Ed Discussion",
          "suggestion_show_schedule" to "Mostra il mio orario",
          "suggestion_library" to "Trova risorse della biblioteca",
          "suggestion_check_grades" to "Controlla i voti su IS-Academia",
          "suggestion_search_moodle" to "Cerca corsi Moodle",
          "suggestion_whats_due" to "Cosa scade questa settimana?",
          "suggestion_study_help" to "Aiutami a studiare per CS220",

          // Home Screen - Animated Intro Suggestions
          "intro_suggestion_1" to "Trova esami passati di CS220",
          "intro_suggestion_2" to "Controlla i miei compiti Moodle",
          "intro_suggestion_3" to "Cosa c'è su Ed Discussion?",
          "intro_suggestion_4" to "Mostra il mio orario IS-Academia",
          "intro_suggestion_5" to "Cerca file EPFL Drive",

          // Home Screen - Actions
          "share" to "Condividi",
          "delete" to "Elimina",
          "clear_chat" to "Cancellare chat?",
          "clear_chat_message" to
              "Questo eliminerà tutti i messaggi. Questa azione non può essere annullata.",
          "cancel" to "Annulla",

          // Drawer
          "euler_logo" to "Logo Euler",
          "new_chat" to "Nuova chat",
          "recents" to "RECENTI",
          "view_all_chats" to "Visualizza tutte le chat",
          "powered_by" to "Fornito da Apertus",
          "settings" to "Impostazioni",
          "all_chats" to "TUTTE LE CHAT",
          "no_conversations_yet" to "Nessuna conversazione al momento",
          "default_user_name" to "Studente",
          "untitled_conversation" to "Senza titolo",

          // Drawer - Recent Items
          "recent_cs220_exam" to "Recupero esame finale CS220",
          "recent_linear_algebra" to "Aiuto con algebra lineare",
          "recent_deadline" to "Richiesta scadenza progetto",
          "recent_registration" to "Info iscrizione corsi") +
          edTranslationsIt() +
          mapOf(
              "moodle_connect_generic_error" to "Connessione a Moodle fallita. Riprova.",
              "moodle_connect_api_unreachable" to
                  "L'API Moodle non è raggiungibile. Controlla la tua connessione.",
              "settings_connectors_moodle_title" to "Connetti a Moodle",
              "settings_connectors_moodle_base_url_label" to "URL Moodle",
              "settings_connectors_moodle_username_label" to "Nome utente",
              "settings_connectors_moodle_password_label" to "Password",
              "settings_connectors_moodle_login_instructions" to
                  "Inserisci le tue credenziali Moodle per connetterti. La tua password verrà inviata in modo sicuro al tuo server Moodle.",
              "settings_connectors_moodle_load_error" to
                  "Errore nel caricamento dello stato del connettore Moodle",
              "settings_connectors_moodle_status_error" to
                  "Impossibile caricare lo stato del connettore Moodle. Riprova più tardi.")

  private fun portuguese() =
      mapOf(
          // Settings
          "settings_title" to "Configurações",
          "profile" to "Perfil",
          "connectors" to "Conectores",
          "Connect_your_academic_services" to "Conecte seus serviços acadêmicos",
          "appearance" to "Aparência",
          "appearance_system" to "Sistema",
          "appearance_light" to "Claro",
          "appearance_dark" to "Escuro",
          "speech_language" to "Idioma de voz",
          "log_out" to "Sair",
          "close" to "Fechar",
          "info" to "Info",
          "by_epfl" to "POR EPFL",
          "connected" to "Conectado",
          "not_connected" to "Não conectado",
          "connect" to "Conectar",
          "disconnect" to "Desconectar",
          "disconnect_confirm_title" to "Desconectar?",
          "disconnect_confirm_message" to "Tem certeza de que deseja desconectar %s?",
          "cancel" to "Cancelar",

          // Home Screen - Navigation
          "menu" to "Menu",
          "euler" to "Euler",
          "more" to "Mais",
          "send" to "Enviar",
          "dictate" to "Ditar",
          "voice_mode" to "Modo de voz",

          // Home Screen - Messages
          "message_euler" to "Mensagem para EULER",
          "euler_thinking" to "Euler está pensando",
          "ask_euler_anything" to "Pergunte qualquer coisa ao Euler",

          // Home Screen - Suggestions
          "suggestion_what_is_epfl" to "O que é EPFL",
          "suggestion_check_ed" to "Verificar Ed Discussion",
          "suggestion_show_schedule" to "Mostrar minha agenda",
          "suggestion_library" to "Encontrar recursos da biblioteca",
          "suggestion_check_grades" to "Verificar notas no IS-Academia",
          "suggestion_search_moodle" to "Pesquisar cursos Moodle",
          "suggestion_whats_due" to "O que vence esta semana?",
          "suggestion_study_help" to "Ajude-me a estudar para CS220",

          // Home Screen - Animated Intro Suggestions
          "intro_suggestion_1" to "Encontrar exames anteriores de CS220",
          "intro_suggestion_2" to "Verificar minhas tarefas do Moodle",
          "intro_suggestion_3" to "O que há no Ed Discussion?",
          "intro_suggestion_4" to "Mostrar minha agenda IS-Academia",
          "intro_suggestion_5" to "Pesquisar arquivos EPFL Drive",

          // Home Screen - Actions
          "share" to "Compartilhar",
          "delete" to "Excluir",
          "clear_chat" to "Limpar chat?",
          "clear_chat_message" to
              "Isso excluirá todas as mensagens. Esta ação não pode ser desfeita.",
          "cancel" to "Cancelar",

          // Drawer
          "euler_logo" to "Logo Euler",
          "new_chat" to "Novo chat",
          "recents" to "RECENTES",
          "view_all_chats" to "Ver todos os chats",
          "powered_by" to "Desenvolvido por Apertus",
          "settings" to "Configurações",
          "all_chats" to "TODOS OS CHATS",
          "no_conversations_yet" to "Ainda não há conversas",
          "default_user_name" to "Estudante",
          "untitled_conversation" to "Sem título",

          // Drawer - Recent Items
          "recent_cs220_exam" to "Recuperação exame final CS220",
          "recent_linear_algebra" to "Ajuda com álgebra linear",
          "recent_deadline" to "Consulta prazo projeto",
          "recent_registration" to "Info inscrição cursos") +
          edTranslationsPt() +
          mapOf(
              "moodle_connect_generic_error" to "Falha ao conectar ao Moodle. Tente novamente.",
              "moodle_connect_api_unreachable" to
                  "A API Moodle não está acessível. Verifique sua conexão.",
              "settings_connectors_moodle_title" to "Conectar ao Moodle",
              "settings_connectors_moodle_base_url_label" to "URL do Moodle",
              "settings_connectors_moodle_username_label" to "Nome de usuário",
              "settings_connectors_moodle_password_label" to "Senha",
              "settings_connectors_moodle_login_instructions" to
                  "Digite suas credenciais do Moodle para conectar. Sua senha será enviada com segurança para o servidor Moodle.",
              "settings_connectors_moodle_load_error" to
                  "Falha ao carregar o status do conector Moodle",
              "settings_connectors_moodle_status_error" to
                  "Não foi possível carregar o status do conector Moodle. Tente novamente mais tarde.")

  private fun chinese() =
      mapOf(
          // Settings
          "settings_title" to "设置",
          "profile" to "个人资料",
          "connectors" to "连接器",
          "Connect_your_academic_services" to "连接您的学术服务",
          "appearance" to "外观",
          "appearance_system" to "系统默认",
          "appearance_light" to "浅色",
          "appearance_dark" to "深色",
          "speech_language" to "语音语言",
          "log_out" to "登出",
          "close" to "关闭",
          "info" to "信息",
          "by_epfl" to "由 EPFL 提供",
          "connected" to "已连接",
          "not_connected" to "未连接",
          "connect" to "连接",
          "disconnect" to "断开连接",
          "disconnect_confirm_title" to "断开连接?",
          "disconnect_confirm_message" to "您确定要断开 %s 的连接吗?",
          "cancel" to "取消",

          // Home Screen - Navigation
          "menu" to "菜单",
          "euler" to "Euler",
          "more" to "更多",
          "send" to "发送",
          "dictate" to "口述",
          "voice_mode" to "语音模式",

          // Home Screen - Messages
          "message_euler" to "发送消息给 EULER",
          "euler_thinking" to "Euler 正在思考",
          "ask_euler_anything" to "向 Euler 提问任何问题",

          // Home Screen - Suggestions
          "suggestion_what_is_epfl" to "什么是 EPFL",
          "suggestion_check_ed" to "查看 Ed Discussion",
          "suggestion_show_schedule" to "显示我的日程",
          "suggestion_library" to "查找图书馆资源",
          "suggestion_check_grades" to "在 IS-Academia 上查看成绩",
          "suggestion_search_moodle" to "搜索 Moodle 课程",
          "suggestion_whats_due" to "本周有什么截止日期？",
          "suggestion_study_help" to "帮我学习 CS220",

          // Home Screen - Animated Intro Suggestions
          "intro_suggestion_1" to "查找 CS220 往年考试",
          "intro_suggestion_2" to "查看我的 Moodle 作业",
          "intro_suggestion_3" to "Ed Discussion 上有什么？",
          "intro_suggestion_4" to "显示我的 IS-Academia 日程",
          "intro_suggestion_5" to "搜索 EPFL Drive 文件",

          // Home Screen - Actions
          "share" to "分享",
          "delete" to "删除",
          "clear_chat" to "清除聊天？",
          "clear_chat_message" to "这将删除所有消息。此操作无法撤消。",
          "cancel" to "取消",

          // Drawer
          "euler_logo" to "Euler 标志",
          "new_chat" to "新聊天",
          "recents" to "最近",
          "view_all_chats" to "查看所有聊天",
          "powered_by" to "由 Apertus 提供支持",
          "settings" to "设置",
          "all_chats" to "所有聊天",
          "no_conversations_yet" to "暂时没有会话",
          "default_user_name" to "学生",
          "untitled_conversation" to "未命名",

          // Drawer - Recent Items
          "recent_cs220_exam" to "CS220 期末考试检索",
          "recent_linear_algebra" to "线性代数帮助",
          "recent_deadline" to "项目截止日期查询",
          "recent_registration" to "课程注册信息") +
          edTranslationsZh() +
          mapOf(
              "moodle_connect_generic_error" to "连接到 Moodle 失败。请重试。",
              "moodle_connect_api_unreachable" to "Moodle API 无法访问。请检查您的连接。",
              "settings_connectors_moodle_title" to "连接到 Moodle",
              "settings_connectors_moodle_base_url_label" to "Moodle URL",
              "settings_connectors_moodle_username_label" to "用户名",
              "settings_connectors_moodle_password_label" to "密码",
              "settings_connectors_moodle_login_instructions" to
                  "输入您的 Moodle 凭据以连接。您的密码将安全地发送到您的 Moodle 服务器。",
              "settings_connectors_moodle_load_error" to "加载 Moodle 连接器状态失败",
              "settings_connectors_moodle_status_error" to "无法加载 Moodle 连接器状态。请稍后再试。")
}
