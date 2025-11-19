package com.android.sample.home

import com.android.sample.profile.ProfileDataSource
import com.android.sample.profile.UserProfile

class FakeProfileRepository : ProfileDataSource {
  var savedProfile: UserProfile? = null

  override suspend fun saveProfile(profile: UserProfile) {
    savedProfile = profile
  }

  override suspend fun loadProfile(): UserProfile? = savedProfile
}
