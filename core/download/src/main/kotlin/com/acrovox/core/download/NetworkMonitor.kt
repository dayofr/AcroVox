package com.acrovox.core.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Réseau actuel. « Wi-Fi » veut dire ici réseau non facturé à l'usage, comme pour WorkManager. */
interface NetworkMonitor {
    fun isUnmetered(): Boolean

    val unmetered: Flow<Boolean>
}

internal class ConnectivityNetworkMonitor @Inject constructor(@param:ApplicationContext context: Context) :
    NetworkMonitor {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    override fun isUnmetered(): Boolean =
        connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.isUnmetered() == true

    override val unmetered: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.isUnmetered())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(isUnmetered())
        connectivity.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun NetworkCapabilities.isUnmetered() = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
