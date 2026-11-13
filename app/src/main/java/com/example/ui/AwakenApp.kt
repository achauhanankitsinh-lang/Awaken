package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.data.repository.AwakenRepository
import com.example.domain.model.ActivityLog
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.FinancialTransaction
import com.example.domain.model.GoalItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TransactionType
import com.example.domain.util.AwakenDateUtils
import com.example.ui.components.ActivityEditorDialog
import com.example.ui.components.AwakenCard
import com.example.ui.components.GoalEditorDialog
import com.example.ui.components.TaskEditorDialog
import com.example.ui.components.TransactionEditorDialog
import com.example.ui.screens.ActivitiesScreen
import com.example.ui.screens.AttributeDetailScreen
import com.example.ui.screens.AttributesScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FinancesScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.OwnerGateScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.QuickActionType
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TrainingCalendarScreen
import com.example.ui.theme.AwakenThemeMode
import com.example.ui.theme.AwakenThemeTokens
import com.example.ui.viewmodel.AwakenChatViewModel
import com.example.ui.viewmodel.AwakenViewModel
import com.example.ui.viewmodel.OwnerGateState
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object AwakenRoutes {
  const val DASHBOARD = "dashboard"
  const val ATTRIBUTES = "attributes"
  const val ATTRIBUTE_DETAIL = "attribute_detail/{attributeKey}"
  const val TASKS = "tasks"
  const val ACTIVITIES = "activities"
  const val TRAINING = "training"
  const val GOALS = "goals"
  const val FINANCES = "finances"
  const val PROGRESS = "progress"
  const val COACH = "coach"
  const val SYSTEM_HUB = "system_hub"

  fun attributeDetail(attribute: AwakenAttribute) = "attribute_detail/${attribute.key}"
}

private data class NavDestinationItem(
  val route: String,
  val label: String,
  val icon: ImageVector
)

private sealed interface ActiveEditorModal {
  data class TaskEditor(
    val existing: TaskItem? = null,
    val defaultAttribute: AwakenAttribute = AwakenAttribute.PHYSICAL
  ) : ActiveEditorModal

  data class ActivityEditor(
    val existing: ActivityLog? = null,
    val presetTitle: String = "",
    val presetAttribute: AwakenAttribute? = AwakenAttribute.PHYSICAL,
    val presetUnit: String = "",
    val presetCategoryTag: String? = null
  ) : ActiveEditorModal

  data class GoalEditor(
    val existing: GoalItem? = null,
    val defaultAttribute: AwakenAttribute = AwakenAttribute.CAREER
  ) : ActiveEditorModal

  data class TransactionEditor(
    val existing: FinancialTransaction? = null,
    val defaultType: TransactionType = TransactionType.EXPENSE
  ) : ActiveEditorModal
}

@Composable
fun AwakenApp(
  themeMode: AwakenThemeMode = AwakenThemeMode.DARK,
  onThemeModeChange: (AwakenThemeMode) -> Unit = {},
  auth: FirebaseAuth = Firebase.auth
) {
  var currentUser by remember { mutableStateOf(auth.currentUser) }

  DisposableEffect(auth) {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      currentUser = firebaseAuth.currentUser
    }
    auth.addAuthStateListener(listener)
    onDispose {
      auth.removeAuthStateListener(listener)
    }
  }

  val user = currentUser
  if (user == null) {
    SignInScreen(
      themeMode = themeMode,
      onToggleTheme = {
        onThemeModeChange(if (themeMode == AwakenThemeMode.DARK) AwakenThemeMode.LIGHT else AwakenThemeMode.DARK)
      },
      onSignedIn = { currentUser = auth.currentUser }
    )
  } else {
    key(user.uid) {
      AuthenticatedAwakenRoot(
        currentUserId = user.uid,
        userDisplayName = user.displayName ?: user.email ?: "Owner",
        userEmail = user.email,
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange
      )
    }
  }
}

@Composable
private fun AuthenticatedAwakenRoot(
  currentUserId: String,
  userDisplayName: String,
  userEmail: String?,
  themeMode: AwakenThemeMode,
  onThemeModeChange: (AwakenThemeMode) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val viewModel: AwakenViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        AwakenViewModel(
          repository = AwakenRepository(db),
          currentUserId = currentUserId,
          userDisplayName = userDisplayName,
          userEmail = userEmail
        )
      }
    }
  )

  val chatViewModel: AwakenChatViewModel = viewModel(key = "chat_$currentUserId")

  val gateState by viewModel.gateState.collectAsStateWithLifecycle()
  val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

  val handleSignOut: () -> Unit = {
    GoogleAuthHelper.signOut(credentialManager = credentialManager, scope = scope)
  }

  when (gateState) {
    is OwnerGateState.Authorized -> {
      AwakenCommandShell(
        viewModel = viewModel,
        chatViewModel = chatViewModel,
        userDisplayName = userDisplayName,
        userEmail = userEmail,
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
        isSaving = isSaving,
        onSignOut = handleSignOut
      )
    }
    else -> {
      OwnerGateScreen(
        gateState = gateState,
        userEmail = userEmail,
        isSaving = isSaving,
        onClaimWorkspace = { viewModel.claimOwnerWorkspace() },
        onRetryCheck = { viewModel.verifyOwnerAccess() },
        onSignOut = handleSignOut
      )
    }
  }
}

@Composable
private fun AwakenCommandShell(
  viewModel: AwakenViewModel,
  chatViewModel: AwakenChatViewModel,
  userDisplayName: String,
  userEmail: String?,
  themeMode: AwakenThemeMode,
  onThemeModeChange: (AwakenThemeMode) -> Unit,
  isSaving: Boolean,
  onSignOut: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: AwakenRoutes.DASHBOARD
  val snackbarHostState = remember { SnackbarHostState() }

  val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
  val activitiesState by viewModel.activitiesState.collectAsStateWithLifecycle()
  val trainingState by viewModel.trainingDaysState.collectAsStateWithLifecycle()
  val goalsState by viewModel.goalsState.collectAsStateWithLifecycle()
  val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
  val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()

  var activeModal by remember { mutableStateOf<ActiveEditorModal?>(null) }

  LaunchedEffect(viewModel) {
    viewModel.feedbackMessages.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  val tasks = (tasksState as? UiState.Success)?.data ?: emptyList()
  val activities = (activitiesState as? UiState.Success)?.data ?: emptyList()
  val trainingDays = (trainingState as? UiState.Success)?.data ?: emptyList()
  val goals = (goalsState as? UiState.Success)?.data ?: emptyList()
  val transactions = (transactionsState as? UiState.Success)?.data ?: emptyList()

  val isInitialLoading = tasksState is UiState.Loading &&
    activitiesState is UiState.Loading &&
    trainingState is UiState.Loading &&
    goalsState is UiState.Loading &&
    transactionsState is UiState.Loading

  val primaryMobileNav = remember {
    listOf(
      NavDestinationItem(AwakenRoutes.DASHBOARD, "Command", Icons.Outlined.Dashboard),
      NavDestinationItem(AwakenRoutes.TASKS, "Tasks", Icons.Outlined.CheckCircleOutline),
      NavDestinationItem(AwakenRoutes.FINANCES, "Finances", Icons.Outlined.AccountBalanceWallet),
      NavDestinationItem(AwakenRoutes.COACH, "Coach", Icons.Outlined.SmartToy),
      NavDestinationItem(AwakenRoutes.SYSTEM_HUB, "System", Icons.Outlined.Apps)
    )
  }

  val desktopRailNav = remember {
    listOf(
      NavDestinationItem(AwakenRoutes.DASHBOARD, "Command", Icons.Outlined.Dashboard),
      NavDestinationItem(AwakenRoutes.ATTRIBUTES, "Attributes", Icons.Outlined.GridView),
      NavDestinationItem(AwakenRoutes.TASKS, "Tasks", Icons.Outlined.CheckCircleOutline),
      NavDestinationItem(AwakenRoutes.ACTIVITIES, "Logs", Icons.AutoMirrored.Outlined.TrendingUp),
      NavDestinationItem(AwakenRoutes.TRAINING, "Training", Icons.Outlined.CalendarMonth),
      NavDestinationItem(AwakenRoutes.GOALS, "Goals", Icons.Outlined.Flag),
      NavDestinationItem(AwakenRoutes.FINANCES, "Finances", Icons.Outlined.AccountBalanceWallet),
      NavDestinationItem(AwakenRoutes.PROGRESS, "Progress", Icons.AutoMirrored.Outlined.ShowChart),
      NavDestinationItem(AwakenRoutes.COACH, "Coach", Icons.Outlined.SmartToy)
    )
  }

  fun navigateSingleTop(route: String) {
    navController.navigate(route) {
      popUpTo(AwakenRoutes.DASHBOARD) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    val useExpandedLayout = maxWidth >= 840.dp

    Scaffold(
      containerColor = colors.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useExpandedLayout) {
          NavigationBar(
            containerColor = colors.surface,
            tonalElevation = 2.dp
          ) {
            primaryMobileNav.forEach { item ->
              val selected = when {
                item.route == AwakenRoutes.SYSTEM_HUB -> currentRoute in setOf(
                  AwakenRoutes.SYSTEM_HUB,
                  AwakenRoutes.ATTRIBUTES,
                  AwakenRoutes.ATTRIBUTE_DETAIL,
                  AwakenRoutes.ACTIVITIES,
                  AwakenRoutes.TRAINING,
                  AwakenRoutes.GOALS,
                  AwakenRoutes.PROGRESS
                )
                else -> currentRoute == item.route
              }
              NavigationBarItem(
                selected = selected,
                onClick = { navigateSingleTop(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = colors.accentBright,
                  selectedTextColor = colors.accentBright,
                  indicatorColor = colors.accentContainer,
                  unselectedIconColor = colors.textSecondary,
                  unselectedTextColor = colors.textSecondary
                ),
                modifier = Modifier.testTag("nav_${item.route}")
              )
            }
          }
        }
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (useExpandedLayout) {
          NavigationRail(
            containerColor = colors.surface,
            modifier = Modifier.fillMaxHeight()
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            desktopRailNav.forEach { item ->
              val selected = currentRoute == item.route ||
                (item.route == AwakenRoutes.ATTRIBUTES && currentRoute == AwakenRoutes.ATTRIBUTE_DETAIL)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateSingleTop(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = colors.accentBright,
                  selectedTextColor = colors.accentBright,
                  indicatorColor = colors.accentContainer,
                  unselectedIconColor = colors.textSecondary,
                  unselectedTextColor = colors.textSecondary
                )
              )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
              onClick = {
                onThemeModeChange(if (themeMode == AwakenThemeMode.DARK) AwakenThemeMode.LIGHT else AwakenThemeMode.DARK)
              },
              modifier = Modifier.testTag("rail_theme_toggle")
            ) {
              Icon(
                imageVector = if (themeMode == AwakenThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = "Switch theme",
                tint = colors.accent
              )
            }
            IconButton(
              onClick = onSignOut,
              modifier = Modifier
                .padding(bottom = 12.dp)
                .testTag("rail_sign_out_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = stringResource(R.string.sign_out),
                tint = colors.statusMissed
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          contentAlignment = Alignment.TopCenter
        ) {
          if (isInitialLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator(color = colors.accent)
            }
          } else {
            val contentModifier = if (useExpandedLayout) {
              Modifier
                .fillMaxHeight()
                .widthIn(max = 960.dp)
                .fillMaxWidth()
            } else {
              Modifier.fillMaxSize()
            }

            NavHost(
              navController = navController,
              startDestination = AwakenRoutes.DASHBOARD,
              modifier = contentModifier
            ) {
              composable(AwakenRoutes.DASHBOARD) {
                DashboardScreen(
                  userDisplayName = userDisplayName,
                  themeMode = themeMode,
                  onToggleTheme = {
                    onThemeModeChange(if (themeMode == AwakenThemeMode.DARK) AwakenThemeMode.LIGHT else AwakenThemeMode.DARK)
                  },
                  tasks = tasks,
                  activities = activities,
                  trainingDays = trainingDays,
                  goals = goals,
                  transactions = transactions,
                  onQuickAction = { qa ->
                    activeModal = when (qa) {
                      QuickActionType.LOG_WORKOUT -> ActiveEditorModal.ActivityEditor(
                        presetTitle = "Workout Session",
                        presetAttribute = AwakenAttribute.PHYSICAL,
                        presetUnit = "minutes",
                        presetCategoryTag = "workout"
                      )
                      QuickActionType.LOG_MEDITATION -> ActiveEditorModal.ActivityEditor(
                        presetTitle = "Meditation",
                        presetAttribute = AwakenAttribute.MENTAL,
                        presetUnit = "minutes",
                        presetCategoryTag = "meditation"
                      )
                      QuickActionType.LOG_LEARNING -> ActiveEditorModal.ActivityEditor(
                        presetTitle = "Study Session",
                        presetAttribute = AwakenAttribute.LEARNING,
                        presetUnit = "minutes",
                        presetCategoryTag = "learning"
                      )
                      QuickActionType.LOG_COMMUNICATION -> ActiveEditorModal.ActivityEditor(
                        presetTitle = "Speaking Practice",
                        presetAttribute = AwakenAttribute.COMMUNICATION,
                        presetUnit = "minutes",
                        presetCategoryTag = "communication"
                      )
                      QuickActionType.ADD_INCOME -> ActiveEditorModal.TransactionEditor(
                        defaultType = TransactionType.INCOME
                      )
                      QuickActionType.ADD_EXPENSE -> ActiveEditorModal.TransactionEditor(
                        defaultType = TransactionType.EXPENSE
                      )
                      QuickActionType.LOG_CAREER -> ActiveEditorModal.ActivityEditor(
                        presetTitle = "Career Achievement",
                        presetAttribute = AwakenAttribute.CAREER,
                        presetUnit = "sessions",
                        presetCategoryTag = "career"
                      )
                      QuickActionType.LOG_REFLECTION -> ActiveEditorModal.ActivityEditor(
                        presetTitle = "Daily Reflection",
                        presetAttribute = AwakenAttribute.MENTAL,
                        presetUnit = "",
                        presetCategoryTag = "reflection"
                      )
                    }
                  },
                  onSetTodayTrainingStatus = { status, existing ->
                    viewModel.setTrainingDayStatus(
                      dateKey = AwakenDateUtils.todayIst(),
                      status = status,
                      existing = existing
                    )
                  },
                  onToggleTaskStatus = { task, status -> viewModel.updateTaskStatus(task, status) },
                  onNewTask = { activeModal = ActiveEditorModal.TaskEditor() },
                  onOpenAttribute = { attr ->
                    navController.navigate(AwakenRoutes.attributeDetail(attr))
                  },
                  onNavigateTasks = { navigateSingleTop(AwakenRoutes.TASKS) },
                  onNavigateActivities = { navigateSingleTop(AwakenRoutes.ACTIVITIES) },
                  onNavigateTraining = { navigateSingleTop(AwakenRoutes.TRAINING) },
                  onNavigateGoals = { navigateSingleTop(AwakenRoutes.GOALS) },
                  onNavigateFinances = { navigateSingleTop(AwakenRoutes.FINANCES) },
                  onNavigateProgress = { navigateSingleTop(AwakenRoutes.PROGRESS) },
                  onNavigateCoach = { navigateSingleTop(AwakenRoutes.COACH) }
                )
              }

              composable(AwakenRoutes.ATTRIBUTES) {
                AttributesScreen(
                  tasks = tasks,
                  activities = activities,
                  goals = goals,
                  onSelectAttribute = { attr ->
                    navController.navigate(AwakenRoutes.attributeDetail(attr))
                  }
                )
              }

              composable(
                route = AwakenRoutes.ATTRIBUTE_DETAIL,
                arguments = listOf(navArgument("attributeKey") { type = NavType.StringType })
              ) { entry ->
                val attrKey = entry.arguments?.getString("attributeKey")
                val attr = AwakenAttribute.fromKey(attrKey)
                AttributeDetailScreen(
                  attribute = attr,
                  tasks = tasks,
                  activities = activities,
                  goals = goals,
                  onBack = { navController.popBackStack() },
                  onCreateTaskForAttribute = { target ->
                    activeModal = ActiveEditorModal.TaskEditor(defaultAttribute = target)
                  },
                  onLogActivityForAttribute = { target ->
                    activeModal = ActiveEditorModal.ActivityEditor(presetAttribute = target)
                  },
                  onToggleTaskStatus = { task, status -> viewModel.updateTaskStatus(task, status) }
                )
              }

              composable(AwakenRoutes.TASKS) {
                TasksScreen(
                  tasks = tasks,
                  onCreateTask = { activeModal = ActiveEditorModal.TaskEditor() },
                  onEditTask = { task -> activeModal = ActiveEditorModal.TaskEditor(existing = task) },
                  onChangeStatus = { task, status -> viewModel.updateTaskStatus(task, status) },
                  onDeleteTask = { id -> viewModel.deleteTask(id) }
                )
              }

              composable(AwakenRoutes.ACTIVITIES) {
                ActivitiesScreen(
                  activities = activities,
                  onCreateActivity = { activeModal = ActiveEditorModal.ActivityEditor() },
                  onEditActivity = { log -> activeModal = ActiveEditorModal.ActivityEditor(existing = log) },
                  onDeleteActivity = { id -> viewModel.deleteActivityLog(id) }
                )
              }

              composable(AwakenRoutes.TRAINING) {
                TrainingCalendarScreen(
                  trainingDays = trainingDays,
                  onSetTrainingStatus = { dateKey, status, existing ->
                    viewModel.setTrainingDayStatus(dateKey, status, existing)
                  },
                  onClearTrainingDay = { dateKey -> viewModel.clearTrainingDay(dateKey) }
                )
              }

              composable(AwakenRoutes.GOALS) {
                GoalsScreen(
                  goals = goals,
                  onCreateGoal = { activeModal = ActiveEditorModal.GoalEditor() },
                  onEditGoal = { goal -> activeModal = ActiveEditorModal.GoalEditor(existing = goal) },
                  onUpdateGoalStatus = { goal, status -> viewModel.updateGoalStatus(goal, status) },
                  onDeleteGoal = { id -> viewModel.deleteGoal(id) }
                )
              }

              composable(AwakenRoutes.FINANCES) {
                FinancesScreen(
                  transactions = transactions,
                  onAddIncome = {
                    activeModal = ActiveEditorModal.TransactionEditor(defaultType = TransactionType.INCOME)
                  },
                  onAddExpense = {
                    activeModal = ActiveEditorModal.TransactionEditor(defaultType = TransactionType.EXPENSE)
                  },
                  onEditTransaction = { tx ->
                    activeModal = ActiveEditorModal.TransactionEditor(existing = tx)
                  },
                  onDeleteTransaction = { id -> viewModel.deleteTransaction(id) }
                )
              }

              composable(AwakenRoutes.PROGRESS) {
                ProgressScreen(
                  tasks = tasks,
                  activities = activities,
                  trainingDays = trainingDays,
                  goals = goals,
                  transactions = transactions
                )
              }

              composable(AwakenRoutes.COACH) {
                ChatScreen(
                  uiState = chatUiState,
                  onSelectRole = { chatViewModel.selectRole(it) },
                  onSendMessage = { chatViewModel.sendMessage(it) },
                  onClearChat = { chatViewModel.clearConversation() },
                  onRetry = { chatViewModel.retryLastFailed() },
                  onDismissError = { chatViewModel.dismissError() }
                )
              }

              composable(AwakenRoutes.SYSTEM_HUB) {
                SystemHubScreen(
                  userDisplayName = userDisplayName,
                  userEmail = userEmail,
                  themeMode = themeMode,
                  onThemeModeChange = onThemeModeChange,
                  onNavigateAttributes = { navController.navigate(AwakenRoutes.ATTRIBUTES) },
                  onNavigateActivities = { navController.navigate(AwakenRoutes.ACTIVITIES) },
                  onNavigateTraining = { navController.navigate(AwakenRoutes.TRAINING) },
                  onNavigateGoals = { navController.navigate(AwakenRoutes.GOALS) },
                  onNavigateProgress = { navController.navigate(AwakenRoutes.PROGRESS) },
                  onSignOut = onSignOut
                )
              }
            }
          }
        }
      }
    }
  }

  when (val modal = activeModal) {
    is ActiveEditorModal.TaskEditor -> {
      TaskEditorDialog(
        initialTask = modal.existing,
        defaultAttribute = modal.defaultAttribute,
        isSaving = isSaving,
        onSave = { task ->
          viewModel.saveTask(modal.existing, task) { activeModal = null }
        },
        onDismiss = { activeModal = null }
      )
    }

    is ActiveEditorModal.ActivityEditor -> {
      ActivityEditorDialog(
        initialLog = modal.existing,
        presetTitle = modal.presetTitle,
        presetAttribute = modal.presetAttribute,
        presetUnit = modal.presetUnit,
        presetCategoryTag = modal.presetCategoryTag,
        isSaving = isSaving,
        onSave = { log ->
          viewModel.saveActivityLog(modal.existing, log) { activeModal = null }
        },
        onDismiss = { activeModal = null }
      )
    }

    is ActiveEditorModal.GoalEditor -> {
      GoalEditorDialog(
        initialGoal = modal.existing,
        defaultAttribute = modal.defaultAttribute,
        isSaving = isSaving,
        onSave = { goal ->
          viewModel.saveGoal(modal.existing, goal) { activeModal = null }
        },
        onDismiss = { activeModal = null }
      )
    }

    is ActiveEditorModal.TransactionEditor -> {
      TransactionEditorDialog(
        initialTx = modal.existing,
        defaultType = modal.defaultType,
        isSaving = isSaving,
        onSave = { tx ->
          viewModel.saveTransaction(modal.existing, tx) { activeModal = null }
        },
        onDismiss = { activeModal = null }
      )
    }

    null -> Unit
  }
}

@Composable
private fun SystemHubScreen(
  userDisplayName: String,
  userEmail: String?,
  themeMode: AwakenThemeMode,
  onThemeModeChange: (AwakenThemeMode) -> Unit,
  onNavigateAttributes: () -> Unit,
  onNavigateActivities: () -> Unit,
  onNavigateTraining: () -> Unit,
  onNavigateGoals: () -> Unit,
  onNavigateProgress: () -> Unit,
  onSignOut: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  val items = remember {
    listOf(
      Pair("Core Attributes", Icons.Outlined.GridView) to onNavigateAttributes,
      Pair("Activity Logs", Icons.AutoMirrored.Outlined.TrendingUp) to onNavigateActivities,
      Pair("Training Calendar", Icons.Outlined.CalendarMonth) to onNavigateTraining,
      Pair("Long-Term Goals", Icons.Outlined.Flag) to onNavigateGoals,
      Pair("Progress Reports", Icons.AutoMirrored.Outlined.ShowChart) to onNavigateProgress
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        text = "System",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.textPrimary
      )
    }

    // Appearance (Light / Dark) Selector
    item {
      AwakenCard {
        Text(
          text = "Appearance",
          style = MaterialTheme.typography.labelMedium,
          color = colors.accent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          AwakenThemeMode.entries.forEach { mode ->
            FilterChip(
              selected = themeMode == mode,
              onClick = { onThemeModeChange(mode) },
              label = { Text(mode.label) },
              leadingIcon = {
                Icon(
                  imageVector = if (mode == AwakenThemeMode.DARK) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("theme_option_${mode.wireValue}"),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accentContainer,
                selectedLabelColor = colors.accentBright,
                selectedLeadingIconColor = colors.accent
              )
            )
          }
        }
      }
    }

    items.forEach { (meta, onClick) ->
      item {
        AwakenCard(onClick = onClick) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = meta.second,
              contentDescription = null,
              tint = colors.accent,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = meta.first,
              style = MaterialTheme.typography.titleMedium,
              color = colors.textPrimary
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(4.dp))
      AwakenCard {
        Text(
          text = userDisplayName,
          style = MaterialTheme.typography.titleMedium,
          color = colors.textPrimary
        )
        if (!userEmail.isNullOrBlank()) {
          Text(
            text = userEmail,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = colors.border)
        Spacer(modifier = Modifier.height(6.dp))
        TextButton(
          onClick = onSignOut,
          modifier = Modifier.testTag("sign_out_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Logout,
            contentDescription = null,
            tint = colors.statusMissed
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.sign_out), color = colors.statusMissed)
        }
      }
    }
  }
}
