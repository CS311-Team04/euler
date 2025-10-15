package com.android.sample.data

object DbPaths {
    const val USERS = "users"

    fun userDoc(uid: String) = "$USERS/$uid"

    fun userSettings(uid: String) = "$USERS/$uid/settings/app"

    fun userQueries(uid: String) = "$USERS/$uid/queries"

    fun userLogs(uid: String) = "$USERS/$uid/logs"
}
