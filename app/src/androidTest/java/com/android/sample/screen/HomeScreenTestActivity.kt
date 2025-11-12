package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.sample.home.HomeViewModel
import com.android.sample.llm.FakeLlmClient

/**
 * Test-only activity that supplies a [HomeViewModel] backed by [FakeLlmClient] so instrumentation
 * tests don't depend on Firebase.
 */
class HomeScreenTestActivity : ComponentActivity() {

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    val delegate = super.getDefaultViewModelProviderFactory()
    return object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
          @Suppress("UNCHECKED_CAST") return HomeViewModel(FakeLlmClient()) as T
        }
        return delegate.create(modelClass, extras)
      }

      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
          @Suppress("UNCHECKED_CAST") return HomeViewModel(FakeLlmClient()) as T
        }
        return delegate.create(modelClass)
      }
    }
  }
}
