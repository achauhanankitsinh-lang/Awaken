package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.FinancialTransaction
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskStatus
import com.example.domain.model.TransactionType
import com.example.domain.util.AwakenDateUtils
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinancesScreen
import com.example.ui.screens.OwnerGateScreen
import com.example.ui.theme.AwakenTheme
import com.example.ui.theme.AwakenThemeMode
import com.example.ui.viewmodel.ChatUiState
import com.example.ui.viewmodel.OwnerGateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun appName_matchesAwaken() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AWAKEN", appName)
  }

  @Test
  fun ownerGate_lockedToOtherOwner_displaysLockAndSignOut() {
    composeTestRule.setContent {
      AwakenTheme {
        OwnerGateScreen(
          gateState = OwnerGateState.LockedToOtherOwner,
          userEmail = "intruder@example.com",
          isSaving = false,
          onClaimWorkspace = {},
          onRetryCheck = {},
          onSignOut = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Private Workspace Locked").assertIsDisplayed()
    composeTestRule.onNodeWithTag("locked_sign_out_button").assertIsDisplayed()
  }

  @Test
  fun dashboardScreen_rendersCommandCenterAndQuickActions() {
    val today = AwakenDateUtils.todayIst()
    composeTestRule.setContent {
      AwakenTheme {
        DashboardScreen(
          userDisplayName = "Owner",
          tasks = listOf(
            TaskItem(
              id = "t1",
              attributeKey = AwakenAttribute.PHYSICAL.key,
              title = "5km Tempo Run",
              scheduledDate = today,
              status = TaskStatus.PLANNED.wireValue
            )
          ),
          activities = emptyList(),
          trainingDays = emptyList(),
          goals = emptyList(),
          transactions = emptyList(),
          onQuickAction = {},
          onSetTodayTrainingStatus = { _, _ -> },
          onToggleTaskStatus = { _, _ -> },
          onNewTask = {},
          onOpenAttribute = {},
          onNavigateTasks = {},
          onNavigateActivities = {},
          onNavigateTraining = {},
          onNavigateGoals = {},
          onNavigateFinances = {},
          onNavigateProgress = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("AWAKEN COMMAND").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dashboard_quote_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Winners Never Quit").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Quitters never WIN").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("qa_log_workout").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("today_training_rest").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("dashboard_list").performScrollToNode(hasText("5km Tempo Run"))
    composeTestRule.onNodeWithText("5km Tempo Run").assertIsDisplayed()
  }

  @Test
  fun dashboardScreen_lightTheme_togglesThemeAndNavigatesCoach() {
    var toggled = false
    var coachClicked = false
    composeTestRule.setContent {
      AwakenTheme(themeMode = AwakenThemeMode.LIGHT) {
        DashboardScreen(
          userDisplayName = "Owner",
          themeMode = AwakenThemeMode.LIGHT,
          onToggleTheme = { toggled = true },
          tasks = emptyList(),
          activities = emptyList(),
          trainingDays = emptyList(),
          goals = emptyList(),
          transactions = emptyList(),
          onQuickAction = {},
          onSetTodayTrainingStatus = { _, _ -> },
          onToggleTaskStatus = { _, _ -> },
          onNewTask = {},
          onOpenAttribute = {},
          onNavigateTasks = {},
          onNavigateActivities = {},
          onNavigateTraining = {},
          onNavigateGoals = {},
          onNavigateFinances = {},
          onNavigateProgress = {},
          onNavigateCoach = { coachClicked = true }
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_theme_toggle").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("dashboard_coach_button").assertIsDisplayed().performClick()
    assertTrue(toggled)
    assertTrue(coachClicked)
  }

  @Test
  fun financesScreen_displaysInrOnlyWithoutOtherCurrencies() {
    val today = AwakenDateUtils.todayIst()
    composeTestRule.setContent {
      AwakenTheme(themeMode = AwakenThemeMode.DARK) {
        FinancesScreen(
          transactions = listOf(
            FinancialTransaction(
              id = "tx1",
              transactionType = TransactionType.INCOME.wireValue,
              amount = 25000.0,
              currency = "INR",
              category = "Salary",
              transactionDate = today
            )
          ),
          onAddIncome = {},
          onAddExpense = {},
          onEditTransaction = {},
          onDeleteTransaction = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Finances (INR)").assertIsDisplayed()
    composeTestRule.onNodeWithText("Salary").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("USD").assertDoesNotExist()
    composeTestRule.onNodeWithText("EUR").assertDoesNotExist()
    composeTestRule.onNodeWithText("GBP").assertDoesNotExist()
  }

  @Test
  fun chatScreen_displaysRolesStarterPromptsAndComposer() {
    composeTestRule.setContent {
      AwakenTheme {
        ChatScreen(
          uiState = ChatUiState(),
          onSelectRole = {},
          onSendMessage = {},
          onClearChat = {},
          onRetry = {},
          onDismissError = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("AWAKEN Coach").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_role_general").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_role_fitness").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_role_focus").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_role_finance_career").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_send_button").assertIsDisplayed()
  }
}
