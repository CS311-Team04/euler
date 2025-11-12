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

  override val defaultViewModelProviderFactory: ViewModelProvider.Factory
    get() {
      val delegate = super.defaultViewModelProviderFactory
      return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
          maybeCreate(modelClass)?.let {
            return it
          }
          return delegate.create(modelClass, extras)
        }

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          maybeCreate(modelClass)?.let {
            return it
          }
          return delegate.create(modelClass)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : ViewModel> maybeCreate(modelClass: Class<T>): T? {
          return if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            HomeViewModel(FakeLlmClient()) as T
          } else {
            null
          }
        }
      }
    }
}
