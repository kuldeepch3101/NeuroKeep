package com.example.learnkeep;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.learnkeep.api.ApiClient;
import com.example.learnkeep.api.ApiService;
import com.example.learnkeep.api.ProfileResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    TextView txtName, txtEmail;
    Button btnLogin;
    LinearLayout btnLogout;
    View profileLayout;
    CircleImageView imgProfile;
    LinearLayout btnChangePhoto;
    TextView txtTopicsCount, txtReviewsCount;
    View guestLayout;

    private static final int PICK_IMAGE = 101;
    SessionManager session;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // ── Wire all views ──────────────────────────────────────────
        txtName          = view.findViewById(R.id.txtName);
        txtEmail         = view.findViewById(R.id.txtEmail);
        btnLogin         = view.findViewById(R.id.btnLogin);
        btnLogout        = view.findViewById(R.id.btnLogout);
        profileLayout    = view.findViewById(R.id.profileLayout);
        guestLayout      = view.findViewById(R.id.guestLayout);
        imgProfile       = view.findViewById(R.id.imgProfile);
        btnChangePhoto   = view.findViewById(R.id.btnChangePhoto);
        txtTopicsCount   = view.findViewById(R.id.txtTopicsCount);
        txtReviewsCount  = view.findViewById(R.id.txtReviewsCount);

        session = new SessionManager(requireContext());

        if (session.isLoggedIn()) {
            showProfile();
        } else {
            showGuest();
        }

        return view;
    }

    private void showProfile() {
        profileLayout.setVisibility(View.VISIBLE);
        guestLayout.setVisibility(View.GONE);
        if (btnLogin != null) btnLogin.setVisibility(View.GONE);

        txtName.setText(session.getName());
        txtEmail.setText(session.getEmail());

//        loadProfilePicture();
//        loadStats();

        btnChangePhoto.setOnClickListener(v -> openImagePicker());

        btnLogout.setOnClickListener(v -> {
            session.logout();
            requireActivity().runOnUiThread(() -> {
                showGuest();
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
            });
        });

        fetchProfileFromServer();
    }

    // ── SHOW GUEST VIEW ──────────────────────────────────────────────
    private void showGuest() {
        profileLayout.setVisibility(View.GONE);
        guestLayout.setVisibility(View.VISIBLE);

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoginActivity.class)));
    }

    // ── LOAD PROFILE PICTURE ─────────────────────────────────────────
    private void loadProfilePicture() {
        String localPath = session.getProfilePicPath();
        if (localPath != null && !localPath.isEmpty()) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(localPath);
                if (bmp != null) {
                    imgProfile.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {}
        }


        String base64 = session.getProfilePicBase64();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    imgProfile.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {}
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 200);

            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 200);

            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE
                && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {

            Uri imageUri = data.getData();
            try {
                InputStream is = requireContext().getContentResolver()
                        .openInputStream(imageUri);
                Bitmap original = BitmapFactory.decodeStream(is);

                Bitmap scaled = Bitmap.createScaledBitmap(original, 300, 300, true);
                imgProfile.setImageBitmap(scaled);


                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                session.saveProfilePicBase64(base64);

                uploadProfilePicture(base64);

            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        "Could not load image", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void uploadProfilePicture(String base64) {
        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("profilePic", base64);

        api.updateProfilePic(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call,
                                   Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            "✅ Profile picture updated", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(requireContext(),
                        "Saved locally (will sync when online)", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadStats() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<KnowledgeEntity> all = db.knowledgeDao().getAll();

                int topicsCount  = all != null ? all.size() : 0;
                int reviewsCount = 0;
                if (all != null) {
                    for (KnowledgeEntity e : all) {
                        // Each topic contributes its review count (defaulting to 1 per topic)
                        reviewsCount += (e.reviewCount > 0) ? e.reviewCount : 1;
                    }
                }

                final int finalTopics  = topicsCount;
                final int finalReviews = reviewsCount;

                requireActivity().runOnUiThread(() -> {
                    txtTopicsCount.setText(String.valueOf(finalTopics));
                    txtReviewsCount.setText(String.valueOf(finalReviews));
                });
            } catch (Exception ignored) {
                // DB not ready yet — leave at 0
            }
        }).start();
    }
    private void fetchProfileFromServer() {
        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);

        api.getProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    String base64 = response.body().profilePic;

                    if (base64 != null && !base64.isEmpty()) {
                        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        imgProfile.setImageBitmap(bmp);

                        // ✅ Save locally for offline
                        session.saveProfilePicBase64(base64);
                    }
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                // fallback to local
                loadProfilePicture();
            }
        });
    }
}
