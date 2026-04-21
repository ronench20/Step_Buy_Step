package com.example.stepbuystep.ActivityCoach.CoachSettingsScreen;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.stepbuystep.ActivityCoach.BaseCoachActivity;
import com.example.stepbuystep.ActivityTrainee.TraineeReg.ProfilePicturePickerBottomSheet;
import com.example.stepbuystep.R;
import com.example.stepbuystep.model.CoachSubscriptionHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CoachSettingsActivity extends BaseCoachActivity
        implements ProfilePicturePickerBottomSheet.Listener {

    private static final String TAG = "CoachSettings";
    private static final String STORAGE_BUCKET = "gs://step-but-step.firebasestorage.app";
    private static final String PROFILE_PIC_PATH = "profile_pictures/";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Profile card
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvCoachId;
    private TextView avatarInitial;          // initials fallback
    private ShapeableImageView ivCoachPic;   // profile picture (hidden until loaded)
    private View btnChangeCoachPhoto;

    // Subscription card
    private TextView tvPlanName;
    private TextView badgeStatus;
    private TextView tvAthletesUsage;

    // Rows
    private TextView tvAthletesCount;
    private LinearLayout rowManageTeam, rowEditGroup, rowChangeSubscription;

    private long coachIdValue = 0;
    private Uri pendingCameraUri;

    // --------------------- Activity result launchers ----------------------

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) launchCamera();
                        else Toast.makeText(this, "Camera permission denied",
                                Toast.LENGTH_SHORT).show();
                    });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success && pendingCameraUri != null) {
                            handlePickedImage(pendingCameraUri);
                        }
                    });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                    uri -> { if (uri != null) handlePickedImage(uri); });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_settings);

        auth    = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance(STORAGE_BUCKET);

        initViews();
        setupListeners();
        setupNavigationBar(NavItem.SETTINGS);
        loadCoachData();
    }

    private void initViews() {

        // Profile card (match XML ids)
        tvName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvCoachId = findViewById(R.id.badgeCoachId);
        avatarInitial = findViewById(R.id.avatar);
        ivCoachPic = findViewById(R.id.ivCoachProfilePic);
        btnChangeCoachPhoto = findViewById(R.id.btnChangeCoachPhoto);

        // Subscription card (match XML ids)
        tvPlanName = findViewById(R.id.tvTier);
        badgeStatus = findViewById(R.id.badgeActive);
        tvAthletesUsage = findViewById(R.id.tvAthletesUsage);

        // Rows (match XML ids)
        tvAthletesCount = findViewById(R.id.tvAthletesCount);
        rowManageTeam = findViewById(R.id.rowManageTeam);
        rowEditGroup = findViewById(R.id.rowEditGroup);
        rowChangeSubscription = findViewById(R.id.rowChangeSubscription);
    }

    private void setupListeners() {
        // Click coach id badge to copy
        if (tvCoachId != null) {
            tvCoachId.setOnClickListener(v -> {
                if (coachIdValue == 0) return;

                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Coach ID", String.valueOf(coachIdValue));
                clipboard.setPrimaryClip(clip);

                Toast.makeText(this, "Copied ID: " + coachIdValue, Toast.LENGTH_SHORT).show();
            });
        }

        if (rowManageTeam != null) {
            rowManageTeam.setOnClickListener(v ->
                    startActivity(new Intent(this, ManageTeamMembersActivity. class)));
        }

        if (rowEditGroup != null) {
            rowEditGroup.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateSubGroupActivity.class)));
        }

        if (rowChangeSubscription != null) {
            rowChangeSubscription.setOnClickListener(v ->
                    showSubscriptionDialog());
        }

        // Profile picture — tapping the picture, the initials circle, or the small
        // camera badge all open the picker (same behaviour the trainee dashboard has).
        View.OnClickListener openPicker = v -> openProfilePicturePicker();
        if (ivCoachPic != null)          ivCoachPic.setOnClickListener(openPicker);
        if (avatarInitial != null)       avatarInitial.setOnClickListener(openPicker);
        if (btnChangeCoachPhoto != null) btnChangeCoachPhoto.setOnClickListener(openPicker);
    }

    private void showSubscriptionDialog() {
        SubscriptionSelectionDialog dialog = new SubscriptionSelectionDialog();

        // Get current tier from tvPlanName
        String currentTier = "basic";
        if (tvPlanName != null && tvPlanName.getText() != null) {
            currentTier = tvPlanName.getText().toString().toLowerCase();
        }

        // Get current athlete count
        String athletesText = tvAthletesUsage != null ? tvAthletesUsage.getText().toString() : "0 / 20";
        int currentCount = 0;
        try {
            currentCount = Integer.parseInt(athletesText.split(" / ")[0].trim());
        } catch (Exception e) {
            currentCount = 0;
        }

        dialog.setCurrentTier(currentTier, currentCount);

        dialog.setListener(new SubscriptionSelectionDialog.OnTierSelectedListener() {
            @Override
            public void onTierSelected(String newTier) {
                // Reload data to update UI
                loadCoachData();
            }

            @Override
            public void onError(String error) {
                if (error.startsWith("NEED_REMOVE_ATHLETES")) {
                    // Parse the message
                    String[] parts = error.split("\\|");
                    String targetTier = parts.length > 1 ? parts[1] : "basic";
                    // Store tier to switch to after removal
                    Toast.makeText(CoachSettingsActivity.this,
                            "Go to Manage Team Members to remove athletes", Toast.LENGTH_LONG).show();
                }
            }
        });

        dialog.show(getSupportFragmentManager(), "SubscriptionDialog");
    }

    // --------------------- Profile picture picker flow ---------------------

    private void openProfilePicturePicker() {
        new ProfilePicturePickerBottomSheet()
                .show(getSupportFragmentManager(), ProfilePicturePickerBottomSheet.TAG);
    }

    @Override
    public void onCameraSelected() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onGallerySelected() {
        pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void launchCamera() {
        try {
            File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            File photo = File.createTempFile("coach_profile_", ".jpg", dir);
            pendingCameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photo);
            takePictureLauncher.launch(pendingCameraUri);
        } catch (IOException e) {
            Log.e(TAG, "Camera file error", e);
            Toast.makeText(this, "Couldn't start camera: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Show an immediate Glide preview, then upload to Storage and update the
     * {@code profileImageUrl} field on the coach's user document — the exact same
     * convention used by trainees so every screen reads from one field.
     */
    private void handlePickedImage(@NonNull Uri uri) {
        String uid = auth.getUid();
        if (uid == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Optimistic preview — hide the initials fallback and show the new picture.
        if (ivCoachPic != null) {
            ivCoachPic.setVisibility(View.VISIBLE);
            Glide.with(this).load(uri).circleCrop().into(ivCoachPic);
        }
        if (avatarInitial != null) avatarInitial.setVisibility(View.GONE);

        StorageReference ref = storage.getReference().child(PROFILE_PIC_PATH + uid);
        Log.d(TAG, "Uploading coach profile picture to: " + ref.getPath());

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        ref.putFile(uri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String url = downloadUri.toString();
                    Log.d(TAG, "Coach upload success, URL: " + url);
                    db.collection("users").document(uid)
                            .update("profileImageUrl", url)
                            .addOnSuccessListener(u -> Toast.makeText(this,
                                    "Profile picture updated", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update Firestore with URL", e);
                                Toast.makeText(this,
                                        "Couldn't save URL: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Storage upload failed", e);
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("404")) {
                        msg = "Storage bucket not found (404). Check Firebase Console.";
                    }
                    Toast.makeText(this, "Upload failed: " + msg, Toast.LENGTH_LONG).show();
                });
    }

    // ------------------------------- Data -------------------------------

    private void loadCoachData() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String email = doc.getString("email");
                    String name = doc.getString("name");
                    if (name == null && email != null && email.contains("@")) {
                        name = email.substring(0, email.indexOf('@'));
                    }

                    Long cid = doc.getLong("coachID");

                    if (tvName != null) tvName.setText(name != null ? name : "Coach");
                    if (tvEmail != null) tvEmail.setText(email != null ? email : "");

                    // Profile picture — fall back to initials when no URL is set.
                    String url = doc.getString("profileImageUrl");
                    applyProfileImage(url, name, email);

                    if (cid != null) {
                        coachIdValue = cid;
                        if (tvCoachId != null) tvCoachId.setText("Coach ID: " + cid);
                        fetchAthletesCount(cid);

                        Map<String, Object> subscription = (Map<String, Object>) doc.get("subscription");
                        String currentTier = "basic";  // Default

                        if (subscription != null) {
                            Object tierObj = subscription.get("tier");
                            if (tierObj != null) {
                                currentTier = tierObj.toString();
                            }
                        }

                        if (tvPlanName != null) {
                            tvPlanName.setText(currentTier);
                        }

                    } else {
                        coachIdValue = 0;
                        if (tvCoachId != null) tvCoachId.setText("Coach ID: -");
                        if (tvAthletesUsage != null) tvAthletesUsage.setText("0 / 20 athletes");
                        if (tvAthletesCount != null) tvAthletesCount.setText("0 athletes");
                        if (tvPlanName != null) tvPlanName.setText("basic");
                    }

                    // Subscription UI
                    if (badgeStatus != null) badgeStatus.setText("Active");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load coach data", Toast.LENGTH_SHORT).show());
    }

    /**
     * Swaps between showing the Glide-loaded profile image and the initials fallback.
     * If {@code url} is empty, the initials TextView is shown and seeded with the
     * first letter of the coach's name (or email username as a fallback).
     */
    private void applyProfileImage(String url, String name, String email) {
        boolean hasUrl = !TextUtils.isEmpty(url);

        if (ivCoachPic != null) {
            if (hasUrl) {
                ivCoachPic.setVisibility(View.VISIBLE);
                Glide.with(this).load(url).circleCrop().into(ivCoachPic);
            } else {
                ivCoachPic.setVisibility(View.GONE);
                Glide.with(this).clear(ivCoachPic);
            }
        }

        if (avatarInitial != null) {
            avatarInitial.setVisibility(hasUrl ? View.GONE : View.VISIBLE);
            String source = !TextUtils.isEmpty(name)
                    ? name
                    : (email != null ? email : "");
            if (!source.isEmpty()) {
                avatarInitial.setText(source.substring(0, 1).toUpperCase());
            }
        }
    }

    private void fetchAthletesCount(long coachId) {
        String uid = auth.getCurrentUser().getUid();

        CoachSubscriptionHelper.loadCoachSubscriptionAndCount(
                uid, coachId, db,
                new CoachSubscriptionHelper.OnSubscriptionLoadListener() {
                    @Override
                    public void onSubscriptionLoaded(int maxAthletes, int currentAthletes) {
                        // Update UI with subscription info
                        if (tvAthletesUsage != null) {
                            tvAthletesUsage.setText(currentAthletes + " / " + maxAthletes + " athletes");
                        }
                        if (tvAthletesCount != null) {
                            tvAthletesCount.setText(currentAthletes + " athletes");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CoachSettingsActivity.this,
                                "Failed to load athletes count", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
