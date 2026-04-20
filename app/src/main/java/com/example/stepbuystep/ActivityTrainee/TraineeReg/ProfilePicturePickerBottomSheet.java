package com.example.stepbuystep.ActivityTrainee.TraineeReg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.stepbuystep.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet offering "Take photo" or "Pick from gallery".
 * The host Activity must implement {@link Listener}.
 */
public class ProfilePicturePickerBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ProfilePicPicker";

    public interface Listener {
        void onCameraSelected();
        void onGallerySelected();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_profile_picture_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Listener host = (getActivity() instanceof Listener)
                ? (Listener) getActivity() : null;

        view.findViewById(R.id.optionCamera).setOnClickListener(v -> {
            if (host != null) host.onCameraSelected();
            dismiss();
        });
        view.findViewById(R.id.optionGallery).setOnClickListener(v -> {
            if (host != null) host.onGallerySelected();
            dismiss();
        });
        view.findViewById(R.id.optionCancel).setOnClickListener(v -> dismiss());
    }
}
