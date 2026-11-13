package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.ui.components.AwakenCard
import com.example.ui.theme.AwakenThemeMode
import com.example.ui.theme.AwakenThemeTokens
import com.example.ui.viewmodel.OwnerGateState

@Composable
fun SignInScreen(
  themeMode: AwakenThemeMode = AwakenThemeMode.DARK,
  onToggleTheme: () -> Unit = {},
  onSignedIn: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_hero_backdrop),
      contentDescription = stringResource(R.string.cd_hero_backdrop),
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .alpha(if (colors.isDark) 0.30f else 0.10f)
    )

    IconButton(
      onClick = onToggleTheme,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .safeDrawingPadding()
        .padding(12.dp)
        .testTag("auth_theme_toggle")
    ) {
      Icon(
        imageVector = if (themeMode == AwakenThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
        contentDescription = "Toggle theme",
        tint = colors.accent
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()
        .padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        modifier = Modifier.widthIn(max = 440.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.card.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, colors.border)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_awaken_emblem),
            contentDescription = stringResource(R.string.cd_awaken_emblem),
            modifier = Modifier
              .size(68.dp)
              .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = colors.textPrimary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.titleMedium,
            color = colors.accentBright,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(24.dp))

          Button(
            onClick = {
              isLoading = true
              errorMessage = null
              GoogleAuthHelper.signInWithGoogleInteractive(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onSuccess = {
                  isLoading = false
                  onSignedIn()
                },
                onCancelled = {
                  isLoading = false
                },
                onError = { err ->
                  isLoading = false
                  errorMessage = err
                }
              )
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = colors.accent,
              contentColor = colors.onAccent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("google_sign_in_button")
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = colors.onAccent,
                strokeWidth = 2.dp
              )
            } else {
              Text(
                text = stringResource(R.string.sign_in_with_google),
                style = MaterialTheme.typography.labelLarge
              )
            }
          }

          if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = errorMessage!!,
              style = MaterialTheme.typography.bodySmall,
              color = colors.statusMissed,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }
  }
}

@Composable
fun OwnerGateScreen(
  gateState: OwnerGateState,
  userEmail: String?,
  isSaving: Boolean,
  onClaimWorkspace: () -> Unit,
  onRetryCheck: () -> Unit,
  onSignOut: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.background)
      .safeDrawingPadding()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    AwakenCard(modifier = Modifier.widthIn(max = 440.dp)) {
      when (gateState) {
        is OwnerGateState.Checking -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator(color = colors.accent)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Verifying workspace...",
              style = MaterialTheme.typography.bodyMedium,
              color = colors.textSecondary
            )
          }
        }

        is OwnerGateState.NeedsInitialClaim -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(50.dp)
                .background(colors.accentContainer, RoundedCornerShape(14.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Initialize Owner Account",
              style = MaterialTheme.typography.headlineSmall,
              color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Bind AWAKEN to ${userEmail ?: "this account"}.",
              style = MaterialTheme.typography.bodyMedium,
              color = colors.textSecondary,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
              onClick = onClaimWorkspace,
              enabled = !isSaving,
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("claim_workspace_button")
            ) {
              Text(if (isSaving) "Initializing..." else stringResource(R.string.claim_workspace))
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = onSignOut,
              modifier = Modifier.fillMaxWidth(),
              border = BorderStroke(1.dp, colors.border)
            ) {
              Text(stringResource(R.string.sign_out), color = colors.textSecondary)
            }
          }
        }

        is OwnerGateState.LockedToOtherOwner -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(50.dp)
                .background(colors.accentContainer, RoundedCornerShape(14.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = colors.statusMissed,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = stringResource(R.string.workspace_locked_title),
              style = MaterialTheme.typography.headlineSmall,
              color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.workspace_locked_desc),
              style = MaterialTheme.typography.bodyMedium,
              color = colors.textSecondary,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
              onClick = onSignOut,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("locked_sign_out_button"),
              border = BorderStroke(1.dp, colors.border)
            ) {
              Text(stringResource(R.string.sign_out), color = colors.textPrimary)
            }
          }
        }

        is OwnerGateState.Error -> {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Verification Error",
              style = MaterialTheme.typography.titleLarge,
              color = colors.statusMissed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = gateState.message,
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onRetryCheck,
              colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Retry")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = onSignOut,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(stringResource(R.string.sign_out), color = colors.textSecondary)
            }
          }
        }

        is OwnerGateState.Authorized -> Unit
      }
    }
  }
}
