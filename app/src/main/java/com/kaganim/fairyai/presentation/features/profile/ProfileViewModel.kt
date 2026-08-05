package com.kaganim.fairyai.presentation.features.profile

import android.content.SharedPreferences
import com.kaganim.fairyai.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

class ProfileState

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : BaseViewModel<ProfileState>(ProfileState())
