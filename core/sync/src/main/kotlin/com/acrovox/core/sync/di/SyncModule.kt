package com.acrovox.core.sync.di

import com.acrovox.core.sync.KeystoreSecretCipher
import com.acrovox.core.sync.SecretCipher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SyncModule {
    @Binds
    abstract fun secretCipher(impl: KeystoreSecretCipher): SecretCipher
}
