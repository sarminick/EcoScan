"use strict";

// Run only from a trusted workstation. The service-account JSON is read from an
// environment variable and is never written to stdout/stderr or to disk here.
// PowerShell: $env:FIREBASE_SERVICE_ACCOUNT=(Get-Content service-account.json -Raw)
// node bootstrap-superadmin.js <uid>

const admin = require("firebase-admin");

const PROJECT_ID = "ecoscan-81b76";
const uid = process.argv[2];

function fail(message) {
  throw new Error(message);
}

function loadServiceAccount() {
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (!raw) fail("FIREBASE_SERVICE_ACCOUNT is required.");

  let serviceAccount;
  try {
    serviceAccount = JSON.parse(raw);
  } catch (_) {
    fail("FIREBASE_SERVICE_ACCOUNT must contain valid JSON.");
  }

  if (!serviceAccount || serviceAccount.project_id !== PROJECT_ID) {
    fail("The service account does not belong to the expected Firebase project.");
  }
  return serviceAccount;
}

function validUid(value) {
  return typeof value === "string" && value.length >= 1 && value.length <= 128 && !/\s/.test(value);
}

function safeName(user) {
  const value = typeof user.displayName === "string" ? user.displayName : "";
  if (value.length > 80) fail("The authentication display name is too long for the user profile.");
  return value;
}

function profileForNewUser(user) {
  if (typeof user.email !== "string" || user.email.length < 3 || user.email.length > 254) {
    fail("The authenticated user must have a valid email before becoming superadministrator.");
  }
  return {
    uid: user.uid,
    name: safeName(user),
    email: user.email,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    role: "superadmin",
    roleUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
  };
}

async function restoreProfile(profileRef, previousProfile) {
  if (previousProfile.exists) {
    await profileRef.set(previousProfile.data());
  } else {
    await profileRef.delete();
  }
}

async function main() {
  if (!validUid(uid)) fail("Provide one valid Firebase Authentication UID.");

  const serviceAccount = loadServiceAccount();
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: PROJECT_ID
  });

  if (admin.app().options.projectId !== PROJECT_ID) {
    fail("Firebase Admin was not initialized for the expected project.");
  }

  const auth = admin.auth();
  const db = admin.firestore();
  const profileRef = db.collection("users").doc(uid);

  // getUser verifies that the UID exists in Authentication before anything changes.
  let user;
  try {
    user = await auth.getUser(uid);
  } catch (error) {
    if (error && error.code === "auth/user-not-found") fail("The supplied UID does not exist in Firebase Authentication.");
    throw error;
  }

  const previousClaims = { ...(user.customClaims || {}) };
  const updatedClaims = { ...previousClaims, role: "superadmin" };
  const previousProfile = await profileRef.get();
  let claimsChanged = false;
  let profileChanged = false;

  try {
    // setCustomUserClaims replaces the claim map, so preserve all existing claims explicitly.
    await auth.setCustomUserClaims(uid, updatedClaims);
    claimsChanged = true;

    // Admin SDK bypasses Firestore rules. Existing profiles retain their client-owned
    // fields; a missing profile is created with the schema expected by the Android app.
    if (previousProfile.exists) {
      await profileRef.set({
        role: "superadmin",
        roleUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
    } else {
      await profileRef.set(profileForNewUser(user));
    }
    profileChanged = true;

    const [verifiedUser, verifiedProfile] = await Promise.all([auth.getUser(uid), profileRef.get()]);
    if (verifiedUser.customClaims?.role !== "superadmin") fail("Final Authentication verification failed.");
    for (const [claim, value] of Object.entries(previousClaims)) {
      if (claim !== "role" && verifiedUser.customClaims?.[claim] !== value) {
        fail("Final Authentication claim verification failed.");
      }
    }
    if (!verifiedProfile.exists || verifiedProfile.get("role") !== "superadmin") {
      fail("Final Firestore profile verification failed.");
    }

    console.log("Superadministrator bootstrap completed. The user must sign out and sign in again to refresh the token.");
  } catch (error) {
    // Authentication and Firestore cannot share one transaction; restore the prior
    // state when a later operation fails so the account is not left partially changed.
    const rollbackErrors = [];
    if (profileChanged) {
      try { await restoreProfile(profileRef, previousProfile); } catch (_) { rollbackErrors.push("profile"); }
    }
    if (claimsChanged) {
      try { await auth.setCustomUserClaims(uid, previousClaims); } catch (_) { rollbackErrors.push("claims"); }
    }
    const rollbackStatus = rollbackErrors.length ? " Recovery was incomplete." : " Previous state was restored.";
    fail(`Superadministrator bootstrap failed.${rollbackStatus}`);
  }
}

main().catch((error) => {
  // Never log credential objects, raw environment variables, or error stacks.
  console.error(error && error.message ? error.message : "Superadministrator bootstrap failed.");
  process.exitCode = 1;
});
