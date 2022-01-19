package com.Sujal_Industries.wallpapers.WALLisWALL;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements RecyclerAdapter.OnImageListener {

    private RecyclerView recyclerView;
    private ArrayList<StorageReference> images;
    private RecyclerAdapter adapter;
    private ShimmerFrameLayout container;
    private NavigationBarView bottomNavBar;
    private ChipGroup themeToggle;
    private Chip darkChip;
    private Chip lightChip;
    private MaterialCardView settings;
    private MaterialButton clearFavsButton;
    private static final String spFileKey = "WallisWall.SECRET_FILE";
    private boolean isNight;
    private FavouritesHelper helper;

    @SuppressLint({"NotifyDataSetChanged", "NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setExitSharedElementCallback(new MaterialContainerTransformSharedElementCallback());
        getWindow().setSharedElementsUseOverlay(false);

        super.onCreate(savedInstanceState);

        SplashScreen.installSplashScreen(this);

        setContentView(R.layout.activity_main);

        images = new ArrayList<>();
        container = findViewById(R.id.shimmerContainer);
        recyclerView = findViewById(R.id.recyclerView);
        bottomNavBar = findViewById(R.id.bottom_navigation);
        themeToggle = findViewById(R.id.themeToggle);
        darkChip = findViewById(R.id.turnDark);
        lightChip = findViewById(R.id.turnLight);
        settings = findViewById(R.id.settingsCard);
        clearFavsButton = findViewById(R.id.clear_fav_button);

        SharedPreferences sharedPreferences = getSharedPreferences(spFileKey, MODE_PRIVATE);
        helper = new FavouritesHelper(sharedPreferences);
        isNight = sharedPreferences.getBoolean("isNight", false);
        if (isNight) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            darkChip.setChecked(true);
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.navColorDark));
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            lightChip.setChecked(true);
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.navColorLight));
        }

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new RecyclerAdapter(images, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.INVISIBLE);
        container.startShimmer();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference listRef = storage.getReference().child("Wallpapers");

        listRef.listAll()
                .addOnSuccessListener(listResult -> {
                    // All the items under listRef.
                    images.addAll(listResult.getItems());
                    container.stopShimmer();
                    container.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    bottomNavBar.setVisibility(View.VISIBLE);

                    adapter.notifyDataSetChanged();
                    animateRecyclerView();

                    bottomNavBar.setOnItemSelectedListener(item -> {
                        switch (item.getItemId()) {
                            case R.id.main_page: {
                                setUpMainPage(listResult);
                                return true;
                            }
                            case R.id.fav_page: {
                                setUpFavPage();
                                return true;
                            }
                            case R.id.set_page: {
                                setUpSettingsPage(listResult);
                                return true;
                            }
                            default:
                                return false;
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // Uh-oh, an error occurred!
                    container.stopShimmer();
                    container.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }

    @SuppressLint("NotifyDataSetChanged")
    public void setUpMainPage(ListResult listResult) {
        recyclerView.setVisibility(View.VISIBLE);
        settings.setVisibility(View.GONE);
        images.clear();
        images.addAll(listResult.getItems());
        adapter.notifyDataSetChanged();
        animateRecyclerView();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setUpFavPage() {
        recyclerView.setVisibility(View.VISIBLE);
        settings.setVisibility(View.GONE);
        int i = 0;
        HashMap<String, Boolean> favs = helper.getFavourites();
        ArrayList<StorageReference> nonFavs = new ArrayList<>();
        for (StorageReference image : images) {
            String wall_name = image.getName();
            boolean fav = (favs.containsKey(wall_name)) ? (favs.get(wall_name)) : false;
            if (!fav) {
                nonFavs.add(images.get(i));
            }
            i++;
        }
        images.removeAll(nonFavs);
        adapter.notifyDataSetChanged();
        animateRecyclerView();
        if (images.isEmpty()) {
            Toast.makeText(this, "Nothing here yet :)", Toast.LENGTH_SHORT).show();
        }
    }

    public void setUpSettingsPage(ListResult listResult) {
        recyclerView.setVisibility(View.GONE);
        settings.setVisibility(View.VISIBLE);
        images.clear();
        images.addAll(listResult.getItems());
        observeChoice();
    }

    @Override
    public void onImageClick(int position) {
        StorageReference sr = images.get(position);
        Intent i = new Intent(getApplicationContext(), Wallpaper.class);
        i.putExtra("StorageRef", sr.getPath());
        i.putExtra("Position", position);
        i.putExtra("isNight", isNight);
        View startView = Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(position)).itemView;
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, startView, ViewCompat.getTransitionName(startView));
        startActivity(i, options.toBundle());
    }

    public void observeChoice() {
        //Setting(1) below
        themeToggle.setOnCheckedChangeListener((group, checkedId) -> {
            bottomNavBar.setSelectedItemId(R.id.main_page);
            SharedPreferences sharedPreferences = getSharedPreferences(spFileKey, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (!isNight) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putBoolean("isNight", true);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putBoolean("isNight", false);
            }
            editor.apply();
        });

        //Setting(2) below
        clearFavsButton.setOnClickListener(view -> {
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Clear Favourites?")
                    .setMessage("Note: You won't be able to undo this task.")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        helper.clearFavs();
                        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", (dialogInterface, i) -> {

                    })
                    .create();
            dialog.show();
        });
    }

    public void animateRecyclerView() {
        recyclerView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);

                        for (int i = 0; i < recyclerView.getChildCount(); i++) {
                            View v = recyclerView.getChildAt(i);
                            v.setAlpha(0.0f);
                            v.animate().alpha(1.0f)
                                    .setDuration(300)
                                    .setStartDelay(i * 101L)
                                    .start();
                        }

                        return true;
                    }
                });
    }
}