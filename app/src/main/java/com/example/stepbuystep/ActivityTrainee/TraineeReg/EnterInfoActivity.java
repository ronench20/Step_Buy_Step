package com.example.stepbuystep.ActivityTrainee.TraineeReg;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.stepbuystep.R;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Trainee onboarding screen.
 *
 * Profile picture handling:
 *   - Tap avatar -> {@link ProfilePicturePickerBottomSheet}
 *     (Camera with runtime permission + FileProvider, or Gallery via PickVisualMedia).
 *   - The chosen Bitmap is shown in {@code ivProfilePic} and, on save, uploaded to
 *     Firebase Storage at {@code profile_pictures/{uid}}. The resulting download URL
 *     is persisted to the user's Firestore document under {@code profileImageUrl} —
 *     same convention used by {@code TraineeHomeActivity}, {@code CoachSettingsActivity},
 *     and the Leaderboard/Team/Pending adapters, so the image appears everywhere
 *     the trainee is shown.
 *
 * NOTE: switched base class from ComponentActivity to AppCompatActivity to enable
 * Material BottomSheetDialogFragment + Fragment manager.
 */
public class EnterInfoActivity extends AppCompatActivity
        implements ProfilePicturePickerBottomSheet.Listener {

    private static final String TAG = "EnterInfoActivity";
    private static final String STORAGE_BUCKET = "gs://step-but-step.firebasestorage.app";
    private static final String PROFILE_PIC_PATH = "profile_pictures/";

    private EditText etAge, etCity, etCoachId;
    private AutoCompleteTextView spGender;
    private Button btnSave;
    private ImageView ivProfilePic, btnChangePhoto;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    /** Holds the most recently chosen profile picture, or null if none. */
    private Bitmap profileBitmap;

    /** URI created with FileProvider where the camera writes its output. */
    private Uri pendingCameraUri;

    // ------------------- Activity result launchers -------------------

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            launchCamera();
                        } else {
                            Toast.makeText(this,
                                    "Camera permission denied",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success && pendingCameraUri != null) {
                            displayBitmapFromUri(pendingCameraUri);
                        }
                    });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        if (uri != null) displayBitmapFromUri(uri);
                    });

    // ----------------------------- Lifecycle -----------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enter_info);

        auth    = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();
        // Explicit bucket URL to avoid 404s in environments where default isn't resolved.
        storage = FirebaseStorage.getInstance(STORAGE_BUCKET);

        etAge          = findViewById(R.id.etAge);
        spGender       = findViewById(R.id.spGender);
        etCity         = findViewById(R.id.etCity);
        etCoachId      = findViewById(R.id.etCoachId);
        btnSave        = findViewById(R.id.btnSaveProfile);
        ivProfilePic   = findViewById(R.id.ivProfilePic);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.gender_options,
                android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);

        // Profile picture triggers
        ivProfilePic.setOnClickListener(v -> openPicturePicker());
        btnChangePhoto.setOnClickListener(v -> openPicturePicker());

        btnSave.setOnClickListener(v -> saveProfile());
    }

    // ---------------------- Profile picture handling ---------------------

    private void openPicturePicker() {
        new ProfilePicturePickerBottomSheet()
                .show(getSupportFragmentManager(), ProfilePicturePickerBottomSheet.TAG);
    }

    @Override
    public void onCameraSelected() {
        // Runtime permission — CAMERA is dangerous; request only when needed.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onGallerySelected() {
        // PickVisualMedia does not require READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE.
        pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void launchCamera() {
        try {
            File photoFile = createTempImageFile();
            pendingCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile);
            takePictureLauncher.launch(pendingCameraUri);
        } catch (IOException e) {
            Log.e(TAG, "Camera file error", e);
            Toast.makeText(this, "Couldn't start camera: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private File createTempImageFile() throws IOException {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile("profile_" + stamp, ".jpg", dir);
    }

    private void displayBitmapFromUri(@NonNull Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new IOException("openInputStream returned null");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) throw new IOException("decodeStream returned null");
            // Downscale keeps the upload small + fast.
            profileBitmap = downscale(bmp, 512);
            ivProfilePic.setImageBitmap(profileBitmap);
        } catch (IOException e) {
            Log.e(TAG, "Error reading picked image", e);
            Toast.makeText(this, "Couldn't load image: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Downscale a bitmap so its longest side <= maxPx. */
    private static Bitmap downscale(Bitmap src, int maxPx) {
        int w = src.getWidth(), h = src.getHeight();
        if (Math.max(w, h) <= maxPx) return src;
        float ratio = (float) maxPx / Math.max(w, h);
        return Bitmap.createScaledBitmap(src,
                Math.round(w * ratio), Math.round(h * ratio), true);
    }

    // ----------------------------- Save flow -----------------------------

    private void saveProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String ageStr = etAge.getText().toString().trim();
        String gender = spGender.getText() != null ? spGender.getText().toString().trim() : "";
        String city   = etCity.getText().toString().trim();
        String coachId = etCoachId.getText().toString().trim();

        if (TextUtils.isEmpty(ageStr)) { etAge.setError("Age required"); return; }
        if (TextUtils.isEmpty(gender) || "Select gender".equals(gender)) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(city))    { etCity.setError("City required"); return; }
        if (TextUtils.isEmpty(coachId)) { etCoachId.setError("Coach ID required"); return; }

        int age;
        try { age = Integer.parseInt(ageStr); }
        catch (NumberFormatException e) { etAge.setError("Invalid age"); return; }

        Map<String, Object> data = new HashMap<>();
        data.put("email",  user.getEmail());
        data.put("age",    age);
        data.put("gender", gender);
        data.put("city",   city);

        // Explicitly clear the legacy Base64 field if it was set by a previous version,
        // so the image field is canonical (profileImageUrl only).
        data.put("profilePicture", FieldValue.delete());

        validateCoachAndSave(user.getUid(), coachId, data);
    }

    private void validateCoachAndSave(String uid, String coachIdText, Map<String, Object> data) {
        long coachIdNumber;
        try { coachIdNumber = Long.parseLong(coachIdText.trim()); }
        catch (NumberFormatException e) { etCoachId.setError("Coach ID must be a number"); return; }

        db.collection("coaches")
                .whereEqualTo("coachID", coachIdNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        String coachDocId = qs.getDocuments().get(0).getId();
                        data.put("coachID",    coachIdNumber);
                        data.put("coachDocId", coachDocId);
                        saveUser(uid, data);
                    } else {
                        etCoachId.setError("Invalid coach ID");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error checking coach ID: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private void saveUser(String uid, Map<String, Object> data) {
        data.put("role",   "trainee");
        data.put("status", "pending");

        // Disable save button to avoid double-submits while the upload runs.
        btnSave.setEnabled(false);

        db.collection("users").document(uid).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (profileBitmap != null) {
                        // Upload picture then go to pending approval. If upload fails
                        // we still proceed — user data is saved and the image can be
                        // retried from the dashboard's picker.
                        uploadProfilePicture(uid, profileBitmap,
                                () -> navigateToPendingApproval(true));
                    } else {
                        navigateToPendingApproval(true);
                    }
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this,
                            "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Upload the chosen profile bitmap to Firebase Storage and save the download URL
     * into the user's Firestore document under {@code profileImageUrl}.
     *
     * <p>Uses a deterministic path {@code profile_pictures/{uid}} so a new upload
     * simply overwrites the previous object — no orphan files left behind in Storage.
     *
     * @param onDone callback invoked once the whole chain is done (success or failure).
     */
    private void uploadProfilePicture(@NonNull String uid,
                                      @NonNull Bitmap bitmap,
                                      @NonNull Runnable onDone) {
        StorageReference ref = storage.getReference().child(PROFILE_PIC_PATH + uid);
        Log.d(TAG, "Uploading registration profile picture to: " + ref.getPath());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos);
        byte[] bytes = bos.toByteArray();

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        ref.putBytes(bytes, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String url = downloadUri.toString();
                    Log.d(TAG, "Registration upload success, URL: " + url);
                    db.collection("users").document(uid)
                            .update("profileImageUrl", url)
                            .addOnCompleteListener(t -> onDone.run());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Registration profile upload failed", e);
                    Toast.makeText(this,
                            "Profile saved, but image upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    onDone.run();
                });
    }

    private void navigateToPendingApproval(boolean showToast) {
        if (showToast) {
            Toast.makeText(this,
                    "Registration submitted! Waiting for coach approval.",
                    Toast.LENGTH_LONG).show();
        }
        startActivity(new Intent(this, PendingApprovalActivity.class));
        finish();
    }
}
