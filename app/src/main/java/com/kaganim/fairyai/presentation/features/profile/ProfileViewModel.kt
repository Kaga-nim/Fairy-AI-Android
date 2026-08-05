package com.kaganim.fairyai.presentation.features.profile

import android.content.SharedPreferences
import com.kaganim.fairyai.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class FairyPersona(val displayName: String, val promptInstruction: String) {
    SANTAI(
        "Santai",
        "Gunakan bahasa Indonesia yang santai, kasual, gaul, dan akrab layaknya seorang teman."
    ),
    FORMAL(
        "Formal",
        "Gunakan bahasa Indonesia yang baku, sopan, profesional, dan tertata dengan baik."
    ),
    CERIA(
        "Ceria",
        "Gunakan bahasa Indonesia yang penuh semangat, ramah, ceria, dan sertakan banyak emoji yang relevan."
    )
}

data class ProfileState(
    val persona: FairyPersona = FairyPersona.SANTAI
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : BaseViewModel<ProfileState>(ProfileState()) {

    companion object {
        private const val KEY_PERSONA = "fairy_persona"
    }

    init {
        val savedPersona = sharedPreferences.getString(KEY_PERSONA, FairyPersona.SANTAI.name)
        val persona = try {
            FairyPersona.valueOf(savedPersona ?: FairyPersona.SANTAI.name)
        } catch (e: Exception) {
            FairyPersona.SANTAI
        }
        updateState { copy(persona = persona) }
    }

    fun onPersonaChange(persona: FairyPersona) {
        sharedPreferences.edit().putString(KEY_PERSONA, persona.name).apply()
        updateState { copy(persona = persona) }
    }
}
