package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AwakenLevel(val code: String, val title: String) {
  CORE_DEVELOPMENT("LEVEL 01", "Core Development"),
  LIFE_ADVANCEMENT("LEVEL 02", "Life Advancement")
}

enum class AwakenAttribute(
  val key: String,
  val displayName: String,
  val sortOrder: Int,
  val level: AwakenLevel,
  val purpose: String,
  val examples: List<String>
) {
  PHYSICAL(
    key = "physical",
    displayName = "Physical",
    sortOrder = 1,
    level = AwakenLevel.CORE_DEVELOPMENT,
    purpose = "Improve physical strength, fitness, and health-related habits.",
    examples = listOf("Running (km)", "Walking", "Push-ups", "Pull-ups", "Squats", "Workouts")
  ),
  MENTAL(
    key = "mental",
    displayName = "Mental",
    sortOrder = 2,
    level = AwakenLevel.CORE_DEVELOPMENT,
    purpose = "Develop focus, self-control, awareness, and mental discipline.",
    examples = listOf("Meditation", "Focus sessions", "Deep work", "Journaling", "Daily reflection")
  ),
  LEARNING(
    key = "learning",
    displayName = "Learning",
    sortOrder = 3,
    level = AwakenLevel.CORE_DEVELOPMENT,
    purpose = "Continuously develop knowledge and practical skills.",
    examples = listOf("Reading", "Courses", "Skill development", "Studying", "Personal projects")
  ),
  COMMUNICATION(
    key = "communication",
    displayName = "Communication",
    sortOrder = 4,
    level = AwakenLevel.LIFE_ADVANCEMENT,
    purpose = "Improve communication and interpersonal abilities.",
    examples = listOf("English speaking", "Listening exercises", "Presentations", "Public speaking", "Writing")
  ),
  FINANCIAL(
    key = "financial",
    displayName = "Financial",
    sortOrder = 5,
    level = AwakenLevel.LIFE_ADVANCEMENT,
    purpose = "Develop financial awareness and responsibility.",
    examples = listOf("Income", "Expenses", "Budget tracking", "Spending categories", "Financial goals")
  ),
  CAREER(
    key = "career",
    displayName = "Career",
    sortOrder = 6,
    level = AwakenLevel.LIFE_ADVANCEMENT,
    purpose = "Develop professional capabilities and career opportunities.",
    examples = listOf("Professional projects", "Freelance work", "Portfolio", "Client acquisition", "Milestones")
  );

  companion object {
    fun fromKey(key: String?): AwakenAttribute {
      return entries.firstOrNull { it.key == key } ?: PHYSICAL
    }
  }
}

enum class TaskStatus(val wireValue: String, val label: String) {
  PLANNED("planned", "Planned"),
  IN_PROGRESS("in_progress", "In Progress"),
  COMPLETED("completed", "Completed"),
  CANCELLED("cancelled", "Cancelled"),
  MISSED("missed", "Missed");

  companion object {
    fun fromWire(value: String?): TaskStatus {
      return entries.firstOrNull { it.wireValue == value } ?: PLANNED
    }
  }
}

enum class TrainingStatus(val wireValue: String, val label: String) {
  PLANNED("planned", "Planned"),
  REST("rest", "Rest"),
  COMPLETED("completed", "Completed"),
  MISSED("missed", "Missed");

  companion object {
    fun fromWire(value: String?): TrainingStatus? {
      return entries.firstOrNull { it.wireValue == value }
    }
  }
}

enum class GoalStatus(val wireValue: String, val label: String) {
  ACTIVE("active", "Active"),
  COMPLETED("completed", "Completed"),
  CANCELLED("cancelled", "Cancelled");

  companion object {
    fun fromWire(value: String?): GoalStatus {
      return entries.firstOrNull { it.wireValue == value } ?: ACTIVE
    }
  }
}

enum class TransactionType(val wireValue: String, val label: String) {
  INCOME("income", "Income"),
  EXPENSE("expense", "Expense");

  companion object {
    fun fromWire(value: String?): TransactionType {
      return entries.firstOrNull { it.wireValue == value } ?: EXPENSE
    }
  }
}

enum class ReportPeriod(val label: String) {
  DAILY("Daily"),
  WEEKLY("Weekly"),
  MONTHLY("Monthly")
}

@IgnoreExtraProperties
data class OwnerLock(
  @DocumentId val id: String = "",
  val ownerUid: String = "",
  val registrationClosed: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OwnerProfile(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val displayName: String = "",
  val email: String? = null,
  val timezone: String = "Asia/Kolkata",
  val defaultCurrency: String = "INR",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TaskItem(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val attributeKey: String = AwakenAttribute.PHYSICAL.key,
  val title: String = "",
  val notes: String? = null,
  val scheduledDate: String = "",
  val status: String = TaskStatus.PLANNED.wireValue,
  val completedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val attribute: AwakenAttribute get() = AwakenAttribute.fromKey(attributeKey)
  val taskStatus: TaskStatus get() = TaskStatus.fromWire(status)

  fun toCreateMap(uid: String): Map<String, Any> {
    return mapOf(
      "ownerId" to uid,
      "attributeKey" to attributeKey,
      "title" to title.trim(),
      "notes" to notes?.trim()?.takeIf { it.isNotEmpty() },
      "scheduledDate" to scheduledDate,
      "status" to status,
      "completedAt" to if (status == TaskStatus.COMPLETED.wireValue) FieldValue.serverTimestamp() else null,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
  }

  fun toUpdateMap(uid: String): Map<String, Any> {
    val existingCreatedAt = requireNotNull(createdAt) { "Task createdAt must be non-null for update" }
    val resolvedCompletedAt: Any? = when {
      status == TaskStatus.COMPLETED.wireValue -> completedAt ?: FieldValue.serverTimestamp()
      else -> null
    }
    return mapOf(
      "ownerId" to uid,
      "attributeKey" to attributeKey,
      "title" to title.trim(),
      "notes" to notes?.trim()?.takeIf { it.isNotEmpty() },
      "scheduledDate" to scheduledDate,
      "status" to status,
      "completedAt" to resolvedCompletedAt,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
  }
}

@IgnoreExtraProperties
data class ActivityLog(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val attributeKey: String? = null,
  val title: String = "",
  val details: String? = null,
  val occurredAt: Timestamp? = null,
  val occurredDate: String = "",
  val metricValue: Double? = null,
  val metricUnit: String? = null,
  val categoryTag: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val attribute: AwakenAttribute? get() = attributeKey?.let { AwakenAttribute.fromKey(it) }

  fun toCreateMap(uid: String): Map<String, Any> {
    return mapOf(
      "ownerId" to uid,
      "attributeKey" to attributeKey,
      "title" to title.trim(),
      "details" to details?.trim()?.takeIf { it.isNotEmpty() },
      "occurredAt" to (occurredAt ?: Timestamp.now()),
      "occurredDate" to occurredDate,
      "metricValue" to metricValue,
      "metricUnit" to metricUnit?.trim()?.takeIf { it.isNotEmpty() && metricValue != null },
      "categoryTag" to categoryTag?.trim()?.takeIf { it.isNotEmpty() },
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
  }

  fun toUpdateMap(uid: String): Map<String, Any> {
    val existingCreatedAt = requireNotNull(createdAt) { "ActivityLog createdAt must be non-null for update" }
    return mapOf(
      "ownerId" to uid,
      "attributeKey" to attributeKey,
      "title" to title.trim(),
      "details" to details?.trim()?.takeIf { it.isNotEmpty() },
      "occurredAt" to (occurredAt ?: Timestamp.now()),
      "occurredDate" to occurredDate,
      "metricValue" to metricValue,
      "metricUnit" to metricUnit?.trim()?.takeIf { it.isNotEmpty() && metricValue != null },
      "categoryTag" to categoryTag?.trim()?.takeIf { it.isNotEmpty() },
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
  }
}

@IgnoreExtraProperties
data class TrainingDay(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val trainingDate: String = "",
  val status: String = TrainingStatus.PLANNED.wireValue,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val trainingStatus: TrainingStatus get() = TrainingStatus.fromWire(status) ?: TrainingStatus.PLANNED
}

@IgnoreExtraProperties
data class GoalItem(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val attributeKey: String = AwakenAttribute.PHYSICAL.key,
  val title: String = "",
  val description: String? = null,
  val targetDate: String? = null,
  val status: String = GoalStatus.ACTIVE.wireValue,
  val completedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val attribute: AwakenAttribute get() = AwakenAttribute.fromKey(attributeKey)
  val goalStatus: GoalStatus get() = GoalStatus.fromWire(status)

  fun toCreateMap(uid: String): Map<String, Any> {
    return mapOf(
      "ownerId" to uid,
      "attributeKey" to attributeKey,
      "title" to title.trim(),
      "description" to description?.trim()?.takeIf { it.isNotEmpty() },
      "targetDate" to targetDate?.trim()?.takeIf { it.isNotEmpty() },
      "status" to status,
      "completedAt" to if (status == GoalStatus.COMPLETED.wireValue) FieldValue.serverTimestamp() else null,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
  }

  fun toUpdateMap(uid: String): Map<String, Any> {
    val existingCreatedAt = requireNotNull(createdAt) { "Goal createdAt must be non-null for update" }
    val resolvedCompletedAt: Any? = when {
      status == GoalStatus.COMPLETED.wireValue -> completedAt ?: FieldValue.serverTimestamp()
      else -> null
    }
    return mapOf(
      "ownerId" to uid,
      "attributeKey" to attributeKey,
      "title" to title.trim(),
      "description" to description?.trim()?.takeIf { it.isNotEmpty() },
      "targetDate" to targetDate?.trim()?.takeIf { it.isNotEmpty() },
      "status" to status,
      "completedAt" to resolvedCompletedAt,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
  }
}

@IgnoreExtraProperties
data class FinancialTransaction(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val transactionType: String = TransactionType.EXPENSE.wireValue,
  val amount: Double = 0.0,
  val currency: String = "INR",
  val category: String = "",
  val description: String? = null,
  val transactionDate: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val type: TransactionType get() = TransactionType.fromWire(transactionType)

  fun toCreateMap(uid: String): Map<String, Any> {
    return mapOf(
      "ownerId" to uid,
      "transactionType" to transactionType,
      "amount" to amount,
      "currency" to "INR",
      "category" to category.trim(),
      "description" to description?.trim()?.takeIf { it.isNotEmpty() },
      "transactionDate" to transactionDate,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
  }

  fun toUpdateMap(uid: String): Map<String, Any> {
    val existingCreatedAt = requireNotNull(createdAt) { "Transaction createdAt must be non-null for update" }
    return mapOf(
      "ownerId" to uid,
      "transactionType" to transactionType,
      "amount" to amount,
      "currency" to "INR",
      "category" to category.trim(),
      "description" to description?.trim()?.takeIf { it.isNotEmpty() },
      "transactionDate" to transactionDate,
      "createdAt" to existingCreatedAt,
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
  }
}
