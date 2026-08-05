package com.kaganim.fairyai.di

import com.kaganim.fairyai.data.repository.AuthRepositoryImpl
import com.kaganim.fairyai.domain.repository.AuthRepository
import com.kaganim.fairyai.domain.usecase.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideAuthUseCases(repository: AuthRepository): AuthUseCases {
            return AuthUseCases(
                login = LoginUseCase(repository),
                register = RegisterUseCase(repository),
                logout = LogoutUseCase(repository),
                getCurrentUser = GetCurrentUserUseCase(repository),
                isUserLoggedIn = IsUserLoggedInUseCase(repository)
            )
        }
    }
}
