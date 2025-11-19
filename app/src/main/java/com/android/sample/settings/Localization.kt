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

  private fun english() =
      mapOf(
          // Settings
          "settings_title" to "Settings",
          "profile" to "Profile",
          "connectors" to "Connectors",
          "appearance" to "Appearance",
          "appearance_system" to "System default",
          "appearance_light" to "Light",
          "appearance_dark" to "Dark",
          "speech_language" to "Speech language",
          "log_out" to "Log out",
          "close" to "Close",
          "info" to "Info",
          "by_epfl" to "BY EPFL",

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
          "clear_chat" to "Clear Chat?",
          "clear_chat_message" to "This will delete all messages. This action cannot be undone.",
          "cancel" to "Cancel",

          // Drawer
          "euler_logo" to "Euler Logo",
          "new_chat" to "New chat",
          "recents" to "RECENTS",
          "view_all_chats" to "View all chats",
          "powered_by" to "Powered by Apertus",
          "settings" to "Settings",

          // Drawer - Recent Items
          "recent_cs220_exam" to "CS220 Final Exam retrieval",
          "recent_linear_algebra" to "Linear Algebra help",
          "recent_deadline" to "Project deadline query",
          "recent_registration" to "Course registration info")

  private fun french() =
      mapOf(
          // Settings
          "settings_title" to "Paramètres",
          "profile" to "Profil",
          "connectors" to "Connecteurs",
          "appearance" to "Apparence",
          "appearance_system" to "Défaut système",
          "appearance_light" to "Clair",
          "appearance_dark" to "Sombre",
          "speech_language" to "Langue de synthèse",
          "log_out" to "Se déconnecter",
          "close" to "Fermer",
          "info" to "Info",
          "by_epfl" to "PAR EPFL",

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

          // Drawer - Recent Items
          "recent_cs220_exam" to "Récupération examen final CS220",
          "recent_linear_algebra" to "Aide en algèbre linéaire",
          "recent_deadline" to "Requête date limite projet",
          "recent_registration" to "Info inscription aux cours")

  private fun german() =
      mapOf(
          // Settings
          "settings_title" to "Einstellungen",
          "profile" to "Profil",
          "connectors" to "Konnektoren",
          "appearance" to "Erscheinungsbild",
          "appearance_system" to "Systemstandard",
          "appearance_light" to "Hell",
          "appearance_dark" to "Dunkel",
          "speech_language" to "Sprachsprache",
          "log_out" to "Abmelden",
          "close" to "Schließen",
          "info" to "Info",
          "by_epfl" to "VON EPFL",

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

          // Drawer - Recent Items
          "recent_cs220_exam" to "CS220 Abschlussprüfung Abruf",
          "recent_linear_algebra" to "Lineare Algebra Hilfe",
          "recent_deadline" to "Projekt-Frist Anfrage",
          "recent_registration" to "Kursanmeldung Info")

  private fun spanish() =
      mapOf(
          // Settings
          "settings_title" to "Configuración",
          "profile" to "Perfil",
          "connectors" to "Conectores",
          "appearance" to "Apariencia",
          "appearance_system" to "Sistema",
          "appearance_light" to "Claro",
          "appearance_dark" to "Oscuro",
          "speech_language" to "Idioma de voz",
          "log_out" to "Cerrar sesión",
          "close" to "Cerrar",
          "info" to "Info",
          "by_epfl" to "POR EPFL",

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

          // Drawer - Recent Items
          "recent_cs220_exam" to "Recuperación examen final CS220",
          "recent_linear_algebra" to "Ayuda con álgebra lineal",
          "recent_deadline" to "Consulta fecha límite proyecto",
          "recent_registration" to "Info inscripción cursos")

  private fun italian() =
      mapOf(
          // Settings
          "settings_title" to "Impostazioni",
          "profile" to "Profilo",
          "connectors" to "Connettori",
          "appearance" to "Aspetto",
          "appearance_system" to "Sistema",
          "appearance_light" to "Chiaro",
          "appearance_dark" to "Scuro",
          "speech_language" to "Lingua vocale",
          "log_out" to "Disconnetti",
          "close" to "Chiudi",
          "info" to "Info",
          "by_epfl" to "DA EPFL",

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

          // Drawer - Recent Items
          "recent_cs220_exam" to "Recupero esame finale CS220",
          "recent_linear_algebra" to "Aiuto con algebra lineare",
          "recent_deadline" to "Richiesta scadenza progetto",
          "recent_registration" to "Info iscrizione corsi")

  private fun portuguese() =
      mapOf(
          // Settings
          "settings_title" to "Configurações",
          "profile" to "Perfil",
          "connectors" to "Conectores",
          "appearance" to "Aparência",
          "appearance_system" to "Sistema",
          "appearance_light" to "Claro",
          "appearance_dark" to "Escuro",
          "speech_language" to "Idioma de voz",
          "log_out" to "Sair",
          "close" to "Fechar",
          "info" to "Info",
          "by_epfl" to "POR EPFL",

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

          // Drawer - Recent Items
          "recent_cs220_exam" to "Recuperação exame final CS220",
          "recent_linear_algebra" to "Ajuda com álgebra linear",
          "recent_deadline" to "Consulta prazo projeto",
          "recent_registration" to "Info inscrição cursos")

  private fun chinese() =
      mapOf(
          // Settings
          "settings_title" to "设置",
          "profile" to "个人资料",
          "connectors" to "连接器",
          "appearance" to "外观",
          "appearance_system" to "系统默认",
          "appearance_light" to "浅色",
          "appearance_dark" to "深色",
          "speech_language" to "语音语言",
          "log_out" to "登出",
          "close" to "关闭",
          "info" to "信息",
          "by_epfl" to "由 EPFL 提供",

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

          // Drawer - Recent Items
          "recent_cs220_exam" to "CS220 期末考试检索",
          "recent_linear_algebra" to "线性代数帮助",
          "recent_deadline" to "项目截止日期查询",
          "recent_registration" to "课程注册信息")
}
