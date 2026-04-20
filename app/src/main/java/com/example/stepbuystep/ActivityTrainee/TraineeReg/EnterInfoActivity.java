package com.example.stepbuystep.ActivityTrainee.TraineeReg;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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
 * Adds:
 *   - Profile picture: tap avatar -> ProfilePicturePickerBottomSheet
 *     (Camera with runtime permission + FileProvider, or Gallery via PickVisualMedia).
 *   - The chosen Bitmap is shown in {@code ivProfilePic} and persisted as
 *     a Base64 string in the user's Firestore document.
 *
 * NOTE: switched base class from ComponentActivity to AppCompatActivity to enable
 * Material BottomSheetDialogFragment + Fragment manager.
 */
public class EnterInfoActivity extends AppCompatActivity
        implements ProfilePicturePickerBottomSheet.Listener {

    private static final String TAG = "EnterInfoActivity";

    private EditText etAge, etCity, etCoachId;
    private AutoCompleteTextView spGender;
    private Button btnSave;
    private ImageView ivProfilePic, btnChangePhoto;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

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

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

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
            profileBitmap = downscale(bmp, 512);
            ivProfilePic.setImageBitmap(profileBitmap);
        } catch (IOException e) {
            Log.e(TAG, "Error reading picked image", e);
            Toast.makeText(this, "Couldn't load image: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Downscale a bitmap so its longest side <= maxPx — keeps Firestore writes small. */
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

        if (profileBitmap != null) {
            data.put("profilePicture", encodeBitmapBase64(profileBitmap));
        }

        validateCoachAndSave(user.getUid(), coachId, data);
    }

    private static String encodeBitmapBase64(@NonNull Bitmap bmp) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
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

        db.collection("users").document(uid).set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Registration submitted! Waiting for coach approval.",
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, PendingApprovalActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
