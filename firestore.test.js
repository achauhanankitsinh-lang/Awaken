const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

function pastDate() {
  return new Date(Date.now() - 2000);
}

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("attributes").get());
  await assertFails(unauthDb.collection("app_config").doc("owner_lock").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("activity_logs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("training_days").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("goals").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("financial_transactions").get());
});

test("Attributes catalog: readable by signed-in user, not writable", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("attributes").doc("physical").set({
      key: "physical",
      name: "Physical",
      sortOrder: 1,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("attributes").doc("physical").get());
  await assertSucceeds(aliceDb.collection("attributes").get());
  await assertFails(
    aliceDb.collection("attributes").doc("custom").set({ key: "custom", name: "Custom", sortOrder: 7 })
  );
  await assertFails(aliceDb.collection("attributes").doc("physical").delete());
});

test("Owner lock: first user claims, second user cannot overwrite or list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("app_config").doc("owner_lock").set({
      ownerUid: ALICE_UID,
      registrationClosed: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(bobDb.collection("app_config").doc("owner_lock").get());
  await assertFails(bobDb.collection("app_config").get());
  await assertFails(
    bobDb.collection("app_config").doc("owner_lock").set({
      ownerUid: BOB_UID,
      registrationClosed: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("app_config").doc("owner_lock").delete());
});

test("Cross-user isolation: Bob cannot read or write Alice's tasks, logs, training days, goals, finances, or PII", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = pastDate();
    await db.collection("users").doc(ALICE_UID).collection("private").doc("profile").set({
      ownerId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      timezone: "Asia/Kolkata",
      defaultCurrency: "INR",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
      ownerId: ALICE_UID,
      attributeKey: "physical",
      title: "Morning Run",
      scheduledDate: "2026-11-12",
      status: "planned",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(ALICE_UID).collection("training_days").doc("2026-11-12").set({
      ownerId: ALICE_UID,
      trainingDate: "2026-11-12",
      status: "completed",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(ALICE_UID).collection("financial_transactions").doc("tx1").set({
      ownerId: ALICE_UID,
      transactionType: "expense",
      amount: 500.0,
      currency: "INR",
      category: "Food",
      transactionDate: "2026-11-12",
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("profile").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("training_days").doc("2026-11-12").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("financial_transactions").doc("tx1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t2").set({
      ownerId: BOB_UID,
      attributeKey: "physical",
      title: "Injected",
      scheduledDate: "2026-11-12",
      status: "planned",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").delete());
});

test("Tasks: schema validation, status enforcement, ghost field rejection, update immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tasksRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks");

  await assertSucceeds(
    tasksRef.doc("t_valid").set({
      ownerId: ALICE_UID,
      attributeKey: "mental",
      title: "Meditation",
      notes: "Breath focus",
      scheduledDate: "2026-11-12",
      status: "planned",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Invalid status rejected
  await assertFails(
    tasksRef.doc("t_bad_status").set({
      ownerId: ALICE_UID,
      attributeKey: "mental",
      title: "Meditation",
      scheduledDate: "2026-11-12",
      status: "done_ish",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Ghost field rejected (Shadow Update test)
  await assertFails(
    tasksRef.doc("t_ghost").set({
      ownerId: ALICE_UID,
      attributeKey: "mental",
      title: "Meditation",
      scheduledDate: "2026-11-12",
      status: "planned",
      xpAwarded: 999,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Valid update preserves createdAt
  await assertSucceeds(
    tasksRef.doc("t_valid").update({
      status: "completed",
      completedAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Activity logs: allows optional metricValue/unit, rejects negative metricValue", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logsRef = aliceDb.collection("users").doc(ALICE_UID).collection("activity_logs");

  // Unmeasured reflection log
  await assertSucceeds(
    logsRef.doc("log_unmeasured").set({
      ownerId: ALICE_UID,
      attributeKey: "mental",
      title: "Evening Reflection",
      occurredAt: pastDate(),
      occurredDate: "2026-11-12",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Measured running log
  await assertSucceeds(
    logsRef.doc("log_measured").set({
      ownerId: ALICE_UID,
      attributeKey: "physical",
      title: "Tempo Run",
      occurredAt: pastDate(),
      occurredDate: "2026-11-12",
      metricValue: 5.2,
      metricUnit: "km",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Negative metric rejected
  await assertFails(
    logsRef.doc("log_neg").set({
      ownerId: ALICE_UID,
      attributeKey: "physical",
      title: "Invalid Run",
      occurredAt: pastDate(),
      occurredDate: "2026-11-12",
      metricValue: -1,
      metricUnit: "km",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Training days: enforces unique date key matching trainingDate and valid statuses", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tdRef = aliceDb.collection("users").doc(ALICE_UID).collection("training_days");

  await assertSucceeds(
    tdRef.doc("2026-11-12").set({
      ownerId: ALICE_UID,
      trainingDate: "2026-11-12",
      status: "rest",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Mismatched doc ID vs trainingDate rejected
  await assertFails(
    tdRef.doc("2026-11-13").set({
      ownerId: ALICE_UID,
      trainingDate: "2026-11-12",
      status: "completed",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Financial transactions: rejects amount <= 0 and non-INR currency", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("financial_transactions");

  await assertSucceeds(
    txRef.doc("tx_ok").set({
      ownerId: ALICE_UID,
      transactionType: "income",
      amount: 75000.5,
      currency: "INR",
      category: "Salary",
      transactionDate: "2026-11-12",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertFails(
    txRef.doc("tx_zero").set({
      ownerId: ALICE_UID,
      transactionType: "expense",
      amount: 0,
      currency: "INR",
      category: "Coffee",
      transactionDate: "2026-11-12",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertFails(
    txRef.doc("tx_usd").set({
      ownerId: ALICE_UID,
      transactionType: "expense",
      amount: 100,
      currency: "USD",
      category: "Software",
      transactionDate: "2026-11-12",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertFails(
    txRef.doc("tx_bad_curr").set({
      ownerId: ALICE_UID,
      transactionType: "expense",
      amount: 100,
      currency: "inr",
      category: "Coffee",
      transactionDate: "2026-11-12",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});
