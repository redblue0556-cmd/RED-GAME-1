const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("leaderboard").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("match_rooms").where("status", "==", "waiting").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Anon",
      avatarTag: "AN",
      totalPoints: 0,
      coins: 150,
      gamesPlayed: 0,
      wins: 0,
      level: 1,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("UserProfile & Encrypted Private Vault: owner CRUD works; cross-user & shadow fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Create Alice profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Pro",
      avatarTag: "AP",
      totalPoints: 0,
      coins: 150,
      gamesPlayed: 0,
      wins: 0,
      level: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update succeeds (preserving createdAt and userId)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalPoints: 120,
      coins: 170,
      gamesPlayed: 1,
      level: 2,
      dailyClaimedDate: "2026-11-22",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow field attack fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immutable createdAt attack fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user profile read/write fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      totalPoints: 99999,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Encrypted private vault create & update (Alice only)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("vault").set({
      userId: ALICE_UID,
      cipherText: "U2FsdGVkX1+encryptedPayload==",
      iv: "MTIzNDU2Nzg5MDEy",
      algorithm: "AES-256-GCM",
      integrityHash: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read Alice's encrypted vault
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("vault").get()
  );
});

test("GameScore history: owner write & list succeed; update/delete/cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scores").doc("score_1").set({
      userId: ALICE_UID,
      gameId: "cyber_tunnel_3d",
      gameTitle: "Neon Tunnel 3D",
      mode: "offline",
      dimension: "3D",
      score: 480,
      pointsEarned: 96,
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("scores").get()
  );

  // Immutable score history cannot be edited or deleted
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("scores").doc("score_1").update({
      score: 999999,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("scores").doc("score_1").delete()
  );

  // Cross-user cannot read or write
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("scores").get()
  );
});

test("Global Leaderboard: public filtered query succeeds; unfiltered query & spoofing fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Pro",
      avatarTag: "AP",
      totalPoints: 250,
      wins: 3,
      gamesPlayed: 5,
      favoriteGame: "Neon Tunnel 3D",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered public query matches Kotlin repository
  await assertSucceeds(
    bobDb.collection("leaderboard").where("visibility", "==", "public").get()
  );

  // Unfiltered list query fails
  await assertFails(bobDb.collection("leaderboard").get());

  // Bob cannot overwrite Alice's leaderboard entry
  await assertFails(
    bobDb.collection("leaderboard").doc(ALICE_UID).update({
      totalPoints: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    bobDb.collection("leaderboard").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      avatarTag: "BO",
      totalPoints: 999,
      wins: 1,
      gamesPlayed: 1,
      favoriteGame: "Neon Snake 2D",
      visibility: "public",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Mabar MatchRoom & Encrypted Chat: create, join, move, finish lock, and participant chat isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const roomRef = aliceDb.collection("match_rooms").doc("room_abc123");
  await assertSucceeds(
    roomRef.set({
      roomCode: "ABC123",
      gameType: "TICTACTOE_3X3",
      hostUid: ALICE_UID,
      hostName: "Alice Pro",
      memberUids: [ALICE_UID],
      status: "waiting",
      boardState: ".........",
      turnUid: ALICE_UID,
      roomSalt: "a1b2c3d4e5f60718293a4b5c",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Lobby waiting rooms query & memberUids query succeed
  await assertSucceeds(
    bobDb.collection("match_rooms").where("status", "==", "waiting").get()
  );
  await assertSucceeds(
    aliceDb.collection("match_rooms").where("memberUids", "array-contains", ALICE_UID).get()
  );
  // Unfiltered match_rooms query fails
  await assertFails(charlieDb.collection("match_rooms").get());

  // Bob joins the waiting room
  await assertSucceeds(
    bobDb.collection("match_rooms").doc("room_abc123").update({
      guestUid: BOB_UID,
      guestName: "Bob Gamer",
      memberUids: [ALICE_UID, BOB_UID],
      status: "playing",
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // Encrypted chat messages by Alice and Bob succeed
  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_abc123").collection("messages").doc("msg_1").set({
      roomId: "room_abc123",
      senderUid: ALICE_UID,
      senderName: "Alice Pro",
      cipherText: "ZW5jcnlwdGVkSGVsbG8=",
      iv: "MTIzNDU2Nzg5MDEy",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(Date.now() - 1200),
    })
  );

  // Non-member Charlie cannot read Alice/Bob's messages via unfiltered list, Alice filter, or direct get, nor send messages
  await assertFails(
    charlieDb.collection("match_rooms").doc("room_abc123").collection("messages").get()
  );
  await assertFails(
    charlieDb.collection("match_rooms").doc("room_abc123").collection("messages")
      .where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    charlieDb.collection("match_rooms").doc("room_abc123").collection("messages").doc("msg_1").get()
  );
  await assertFails(
    charlieDb.collection("match_rooms").doc("room_abc123").collection("messages").doc("msg_2").set({
      roomId: "room_abc123",
      senderUid: CHARLIE_UID,
      senderName: "Charlie",
      cipherText: "aGFja2Vk",
      iv: "MTIzNDU2Nzg5MDEy",
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Alice finishes match with winnerUid
  await assertSucceeds(
    roomRef.update({
      boardState: "XXXOO....",
      turnUid: BOB_UID,
      status: "finished",
      winnerUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 800),
    })
  );

  // Terminal state lock: finished match cannot be modified
  await assertFails(
    bobDb.collection("match_rooms").doc("room_abc123").update({
      boardState: "OOOXX....",
      turnUid: ALICE_UID,
      status: "playing",
      winnerUid: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
