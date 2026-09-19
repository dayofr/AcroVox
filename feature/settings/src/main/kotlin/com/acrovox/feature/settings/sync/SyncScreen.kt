package com.acrovox.feature.settings.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acrovox.core.designsystem.format.formatRelativeDate
import com.acrovox.core.designsystem.icon.AcroVoxIcons
import com.acrovox.core.designsystem.theme.AcroVoxShape
import com.acrovox.core.designsystem.theme.AcroVoxTheme
import com.acrovox.core.designsystem.theme.Spacing
import com.acrovox.core.sync.SyncAccount

@Composable
fun SyncScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SyncViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AcroVoxTheme.colors
    Column(modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(AcroVoxIcons.Back, contentDescription = "Retour", tint = colors.textPrimary)
            }
            Text("Synchronisation gPodder", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenMargin)
                .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                "AcroVox envoie vos abonnements et votre écoute (épisodes écoutés, ignorés, position) " +
                    "vers un serveur gPodder. Rien n'est récupéré du serveur.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
            if (state.loading) return@Column
            val account = state.account
            if (account == null) {
                LoginForm(busy = state.busy, onConnect = viewModel::connect)
            } else {
                AccountCard(account, state.pendingActions)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Button(
                        onClick = viewModel::syncNow,
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brand,
                            contentColor = colors.onBrand
                        )
                    ) { Text("Envoyer maintenant") }
                    OutlinedButton(onClick = viewModel::disconnect, enabled = !state.busy) {
                        Text("Se déconnecter", color = colors.textPrimary)
                    }
                }
            }
            if (state.busy) CircularProgressIndicator(Modifier.size(24.dp), color = colors.brand, strokeWidth = 2.dp)
            state.message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun LoginForm(busy: Boolean, onConnect: (String, String, String) -> Unit) {
    val colors = AcroVoxTheme.colors
    var server by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        server,
        onValueChange = { server = it },
        label = { Text("Adresse du serveur") },
        placeholder = { Text("gpodder.example.org") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        username,
        onValueChange = { username = it },
        label = { Text("Nom d'utilisateur") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        password,
        onValueChange = { password = it },
        label = { Text("Mot de passe") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        "La connexion passe toujours en https. Le mot de passe est chiffré sur l'appareil.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary
    )
    Button(
        onClick = { onConnect(server, username, password) },
        enabled = !busy && server.isNotBlank() && username.isNotBlank() && password.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = colors.onBrand)
    ) { Text("Se connecter") }
}

@Composable
private fun AccountCard(account: SyncAccount, pending: Int) {
    val colors = AcroVoxTheme.colors
    Surface(
        shape = AcroVoxShape.Artwork,
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.outlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(account.username, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(account.server, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Text(
                "Appareil : ${account.deviceId}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Text(
                account.lastSyncAt?.let { "Dernier envoi : ${formatRelativeDate(it).lowercase()}" }
                    ?: "Pas encore d'envoi",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = Spacing.sm)
            )
            Text(
                if (pending == 0) "Rien en attente" else "$pending action${if (pending > 1) "s" else ""} en attente",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
            account.lastError?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
