const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("unauthenticated user cannot read or write user profiles", async () => {
  const unauthContext = testEnv.unauthenticatedContext();
  const db = unauthContext.firestore();
  await assertFails(db.collection("users").doc("alice").get());
  await assertFails(db.collection("users").doc("alice").set({
    userId: "alice",
    displayName: "Alice",
    email: "alice@example.com",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("authenticated user can read and write own profile, cannot access another user's", async () => {
  const aliceContext = testEnv.authenticatedContext("alice");
  const db = aliceContext.firestore();
  await assertSucceeds(db.collection("users").doc("alice").set({
    userId: "alice",
    displayName: "Alice",
    email: "alice@example.com",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
  await assertSucceeds(db.collection("users").doc("alice").get());

  const bobContext = testEnv.authenticatedContext("bob");
  const bobDb = bobContext.firestore();
  await assertFails(bobDb.collection("users").doc("alice").get());
  await assertFails(bobDb.collection("users").doc("alice").set({
    userId: "alice",
    displayName: "Hacker Bob",
    email: "bob@example.com",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("user can create conversations and messages under their own path", async () => {
  const aliceContext = testEnv.authenticatedContext("alice");
  const db = aliceContext.firestore();
  await assertSucceeds(db.collection("users").doc("alice").collection("conversations").doc("c1").set({
    id: "c1",
    userId: "alice",
    title: "New Chat",
    model: "gemini-3.8-flash",
    createdAt: new Date(),
    updatedAt: new Date()
  }));

  await assertSucceeds(db.collection("users").doc("alice").collection("conversations").doc("c1").collection("messages").doc("m1").set({
    id: "m1",
    userId: "alice",
    conversationId: "c1",
    sender: "user",
    content: "Hello Lona",
    timestamp: new Date()
  }));

  const bobContext = testEnv.authenticatedContext("bob");
  const bobDb = bobContext.firestore();
  await assertFails(bobDb.collection("users").doc("alice").collection("conversations").doc("c1").get());
});

test("user can create and update projects under their own path", async () => {
  const aliceContext = testEnv.authenticatedContext("alice");
  const db = aliceContext.firestore();
  await assertSucceeds(db.collection("users").doc("alice").collection("projects").doc("p1").set({
    id: "p1",
    userId: "alice",
    name: "Login Page",
    templateType: "login",
    htmlCode: "<h1>Login</h1>",
    cssCode: "body { background: #000; }",
    jsCode: "console.log('init');",
    createdAt: new Date(),
    updatedAt: new Date()
  }));

  const bobContext = testEnv.authenticatedContext("bob");
  const bobDb = bobContext.firestore();
  await assertFails(bobDb.collection("users").doc("alice").collection("projects").doc("p1").get());
});
