package com.example.stepbuystep.ActivityCommon;

import android.content.Context;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.stepbuystep.R;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executors;

/**
 * Modern Google Sign-In helper, built on:
 *   - AndroidX Credential Manager
 *   - Google Identity Services (GoogleIdTokenCredential)
 *   - Firebase Auth (GoogleAuthProvider.getCredential)
 *
 * This replaces the deprecated com.google.android.gms.auth.api.signin.GoogleSignIn flow.
 *
 * Requires the auto-generated string resource R.string.default_web_client_id
 * (produced by the google-services Gradle plugin from google-services.json).
 */
public final class GoogleAuthHelper {

    private static final String TAG = "GoogleAuthHelper";

    public interface Callback {
        void onSuccess(@NonNull AuthResult result);
        void onError(@NonNull Throwable error);
        default void onCancelled() { /* optional */ }
    }

    private GoogleAuthHelper() { /* no-instance */ }

    /** Trigger the Google Sign-In bottom sheet and, on success, sign in to Firebase. */
    public static void signIn(@NonNull ComponentActivity activity,
                              @NonNull Callback callback) {

        final Context ctx = activity.getApplicationContext();
        final CredentialManager credentialManager = CredentialManager.create(ctx);

        // Use the "Sign in with Google" button flow. Server client ID must be the
        // OAuth 2.0 Web client ID from your Firebase project (auto-injected as
        // default_web_client_id by the google-services plugin).
        GetSignInWithGoogleOption googleOption =
                new GetSignInWithGoogleOption.Builder(
                        activity.getString(R.string.default_web_client_id))
                        .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        credentialManager.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleCredentialResponse(response, callback);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "CredentialManager error", e);
                        activity.runOnUiThread(() -> {
                            if ("android.credentials.GetCredentialException.TYPE_USER_CANCELED"
                                    .equals(e.getType())) {
                                callback.onCancelled();
                            } else {
                                callback.onError(e);
                            }
                        });
                    }
                });
    }

    private static void handleCredentialResponse(@NonNull GetCredentialResponse response,
                                                 @NonNull Callback callback) {
        try {
            if (!(response.getCredential() instanceof CustomCredential)) {
                callback.onError(new IllegalStateException("Unexpected credential type"));
                return;
            }

            CustomCredential custom = (CustomCredential) response.getCredential();
            if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(custom.getType())) {
                callback.onError(new IllegalStateException(
                        "Unexpected credential: " + custom.getType()));
                return;
            }

            GoogleIdTokenCredential googleCredential =
                    GoogleIdTokenCredential.createFrom(custom.getData());

            String idToken = googleCredential.getIdToken();
            if (idToken == null || idToken.isEmpty()) {
                callback.onError(new IllegalStateException("Missing Google ID token"));
                return;
            }

            AuthCredential fbCredential =
                    GoogleAuthProvider.getCredential(idToken, null);

            FirebaseAuth.getInstance()
                    .signInWithCredential(fbCredential)
                    .addOnSuccessListener(callback::onSuccess)
                    .addOnFailureListener(callback::onError);

        } catch (Exception e) {
            Log.e(TAG, "Failed parsing Google credential", e);
            callback.onError(e);
        }
    }
}
