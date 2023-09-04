package game.xnxnztsoafk.testaplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.onesignal.OneSignal;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Random;

public class PlaneActivity extends AppCompatActivity {
    private ImageView plane;
    private int[] imageResources = {R.drawable.back_tab_1_00, R.drawable.back_tab_2_00, R.drawable.back_tab_3_00};
    private static final String ONESIGNAL_APP_ID = "02af555d-79c7-4157-b0c9-7f30b80fbf1d";

    private WebView NAME_WEB_VIEW_SHOW;
    private FirebaseRemoteConfig remoteConfig;
    private PhoneStateListener phoneStateListener;
    private ConnectivityManager.NetworkCallback networkCallback;
    private RotateAnimation rotate;
    private boolean doubleBackToExitPressedOnce = false;
    private int screenHeight;
    private boolean isFalling = true;
    private Handler handler = new Handler();
    private Handler handlerForMinusMS = new Handler();
    private long fallDuration = 4000;

    private boolean isGameActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plane);

        plane = findViewById(R.id.plane);

        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);


        remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(40)
                .build();
        remoteConfig.setConfigSettingsAsync(settings);

        remoteConfig.setDefaultsAsync(R.xml.remote_config_def);
        NAME_WEB_VIEW_SHOW = findViewById(R.id.NAME_WEB_VIEW_SHOW);
        NAME_WEB_VIEW_SHOW.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        WebSettings webSettings = NAME_WEB_VIEW_SHOW.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                super.onServiceStateChanged(serviceState);

                boolean isSimActive = isExistsAndActiveSim(PlaneActivity.this);

                if (!isSimActive) {
                    NAME_WEB_VIEW_SHOW.setVisibility(View.INVISIBLE);
                    onFetchFail();
                }
            }
        };

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                boolean isSimActive = isExistsAndActiveSim(PlaneActivity.this);
                if (isSimActive) {
                    runOnUiThread(() -> NAME_WEB_VIEW_SHOW.setVisibility(View.VISIBLE));
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                runOnUiThread(() -> NAME_WEB_VIEW_SHOW.setVisibility(View.INVISIBLE));
                onFetchFail();
            }
        };

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                networkCallback
        );
        getFetch();

    }
    @Override
    protected void onResume() {
        super.onResume();
        hideUi();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }
    @Override
    public void onBackPressed() {
        if (NAME_WEB_VIEW_SHOW.canGoBack()) {
            NAME_WEB_VIEW_SHOW.goBack();
        } else if (!isExistsAndActiveSim(PlaneActivity.this) || !isInternetConnected()) {
            onFetchFail();
        } else {
        }
    }
    private void getFetch() {
        boolean isSimActive = isExistsAndActiveSim(this);
        if (isSimActive) {
            remoteConfig.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
                @Override
                public void onComplete(@NonNull Task<Boolean> task) {
                    if (task.isSuccessful()) {
                        if (isSimActive) onFetchSuccess();
                    } else {
                        NAME_WEB_VIEW_SHOW.setVisibility(View.INVISIBLE);

                        onFetchFail();
                    }
                }
            });
        }else{
            NAME_WEB_VIEW_SHOW.setVisibility(View.INVISIBLE);

            onFetchFail();
        }
    }
    private void onFetchSuccess() {
        NAME_WEB_VIEW_SHOW = findViewById(R.id.NAME_WEB_VIEW_SHOW);
        String mainLink = remoteConfig.getString("main_link");
        NAME_WEB_VIEW_SHOW.loadUrl(mainLink);
        NAME_WEB_VIEW_SHOW.setWebViewClient(new WebViewClient());
        NAME_WEB_VIEW_SHOW.setVisibility(View.VISIBLE);
    }
    private void  onFetchFail() {
        ImageView wheel = findViewById(R.id.wheel);

        ImageButton exit_button = findViewById(R.id.exit_game_btn);
        ImageButton pp_button = findViewById(R.id.info_game_btn);
        ImageButton restart_button = findViewById(R.id.restart_btn);
        ImageButton start_button = findViewById(R.id.start_game_btn);
        ImageView game_over = findViewById(R.id.game_over);
        ImageView bottom_img = findViewById(R.id.imageView);
        TextView score = findViewById(R.id.score_game);
        ImageView loading_img = findViewById(R.id.imageView7);

        rotate = new RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setDuration(20000);
        rotate.setFillAfter(false);
        rotate.setInterpolator(new LinearInterpolator());



        final Animation animation = AnimationUtils.loadAnimation(this, R.anim.button_scale);
        start_button.setOnClickListener(view -> {
            view.startAnimation(animation);
            start_button.setVisibility(View.INVISIBLE);
            bottom_img.setVisibility(View.INVISIBLE);
            wheel.startAnimation(rotate);
            plane.setEnabled(true);

            startFallingAnimationForOne();
            handlerForMinusMS.postDelayed(() -> {
                startFallingAnimationForTwo();
            }, 1000);
            handlerForMinusMS.postDelayed(() -> {
                startWinFallingAnimation();
            }, 2 * 1000);
        });
        restart_button.setOnClickListener(view -> {
            view.startAnimation(animation);

            game_over.setVisibility(View.INVISIBLE);
            restart_button.setVisibility(View.INVISIBLE);
            bottom_img.setVisibility(View.INVISIBLE);
            rotate.start();
            plane.setEnabled(true);
            wheel.startAnimation(rotate);

            fallDuration = 4000;
            score.setText("0");
            isFalling = true;
            startFallingAnimationForOne();
            startFallingAnimationForTwo();
            startWinFallingAnimation();
        });
        exit_button.setOnClickListener(view -> {
            if (!isExistsAndActiveSim(PlaneActivity.this) || !isInternetConnected()) {
                view.startAnimation(animation);
                onFetchFail();
            } else {
                finish();
            }
        });
        pp_button.setOnClickListener(view -> {
            view.startAnimation(animation);
            String policyLink = remoteConfig.getString("policy_link");

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(policyLink));
            startActivity(intent);
        });
        plane.setOnTouchListener(new View.OnTouchListener() {
            float initialX, initialY;
            float offsetX, offsetY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = plane.getX();
                        initialY = plane.getY();
                        offsetX = event.getRawX() - initialX;
                        offsetY = event.getRawY() - initialY;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() - offsetX;
                        float newY = event.getRawY() - offsetY;

                        if (newX >= 0 && newX + plane.getWidth() <= getWindowManager().getDefaultDisplay().getWidth()) {
                            plane.setX(newX);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                return true;

            }
        });
        NAME_WEB_VIEW_SHOW.setVisibility(View.INVISIBLE);

    }
    private void startFallingAnimationForOne() {
        handler.postDelayed(() -> {
            if (isFalling) {
                createFallingObjectForOne();
                startFallingAnimationForOne();
            }
        }, 4000);
    }
    private void startFallingAnimationForTwo() {
        handler.postDelayed(() -> {
            if (isFalling) {
                createFallingObjectForTwo();
                startFallingAnimationForTwo();
            }
        }, 5000);
    }
    private void startWinFallingAnimation() {
        handler.postDelayed(() -> {
            if (isFalling) {
                checkWin();
                startWinFallingAnimation();
            }
        }, 15000);
    }
    private void createFallingObjectForOne() {
        final ImageView pin_1 = findViewById(R.id.pin_1);
        pin_1.setVisibility(View.VISIBLE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int screenHeight = displayMetrics.heightPixels;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        Random random = new Random();
        int pinWidth = pin_1.getWidth();
        int randomX = random.nextInt(screenWidth - pinWidth);
        pin_1.setX(randomX);
        pin_1.setY(0);

        final int[] fallSpeedForTwo = {10};

        Runnable fallRunnable = new Runnable() {
            @Override
            public void run() {
                while (pin_1.getY() < screenHeight) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            float currentY = pin_1.getY();
                            pin_1.setY(currentY + fallSpeedForTwo[0]);
                            if (pin_1.getY() >= screenHeight) {
                                pin_1.setVisibility(View.INVISIBLE);
                            }
                            if (isCollision(pin_1, plane)) {
                                stopGame();
                            }
                        }
                    });

                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        new Thread(fallRunnable).start();

        handlerForMinusMS.postDelayed(() -> {
             fallSpeedForTwo[0] += 3;
        }, 15000);
    }


    private void createFallingObjectForTwo() {
        final ImageView pin_2 = findViewById(R.id.pin_2);
        pin_2.setVisibility(View.VISIBLE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int screenHeight = displayMetrics.heightPixels;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        Random random = new Random();
        int pinWidth = pin_2.getWidth();
        int randomX = random.nextInt(screenWidth - pinWidth);
        pin_2.setX(randomX);
        pin_2.setY(0);

        final int[] fallSpeedForTwo = {10}; // Швидкість падіння

        Runnable fallRunnable = new Runnable() {
            @Override
            public void run() {
                while (pin_2.getY() < screenHeight) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            float currentY = pin_2.getY();
                            pin_2.setY(currentY + fallSpeedForTwo[0]);
                            if (pin_2.getY() >= screenHeight) {
                                pin_2.setVisibility(View.INVISIBLE);
                            }
                            if (isCollision(pin_2, plane)) {
                                stopGame();
                            }
                        }
                    });

                    try {
                        Thread.sleep(16); // Пауза для зміни швидкості падіння
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        new Thread(fallRunnable).start();

        handlerForMinusMS.postDelayed(() -> {
            // Змінення швидкості падіння для pin_2
             fallSpeedForTwo[0] += 3;
        }, 15000);
    }


    private void checkWin() {
        final ImageView pin_2 = findViewById(R.id.pin_2);
        final ImageView pin_1 = findViewById(R.id.pin_1);
        final ImageView win = findViewById(R.id.win_1);
        TextView score = findViewById(R.id.score_game);
        win.setVisibility(View.VISIBLE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int screenHeight = displayMetrics.heightPixels;
        int currentScore = Integer.parseInt(score.getText().toString());
        Random random = new Random();

        int randomIndex = random.nextInt(imageResources.length);
        win.setImageResource(imageResources[randomIndex]);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int pinWidth = win.getWidth();
        int randomX;

        // Встановлюємо початкову позицію `win` тут
        randomX = random.nextInt(screenWidth - pinWidth);
        win.setX(randomX);
        win.setY(0);

        final int[] fallSpeedForTwo = {10};

        Runnable fallRunnable = new Runnable() {
            @Override
            public void run() {
                while (win.getY() < screenHeight) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            float currentY = win.getY();
                            win.setY(currentY + fallSpeedForTwo[0]);
                            if (win.getY() >= screenHeight) {
                                win.setVisibility(View.INVISIBLE);
                            }

                            // Перевіряємо колізію після встановлення позиції `win`
                            if (isCollision(win, plane)) {
                                int newScore = currentScore + randomIndex + 1;
                                score.setText(String.valueOf(newScore));
                                win.setVisibility(View.INVISIBLE);
                            }
                        }
                    });

                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        new Thread(fallRunnable).start();

        handlerForMinusMS.postDelayed(() -> {
            fallSpeedForTwo[0] += 3;
        }, 15000);
    }


    private boolean isCollisionWithPins(ImageView object, ImageView pin1, ImageView pin2, int newX) {
        Rect objectRect = new Rect();
        object.getHitRect(objectRect);

        Rect pin1Rect = new Rect();
        pin1.getHitRect(pin1Rect);

        Rect pin2Rect = new Rect();
        pin2.getHitRect(pin2Rect);

        return Rect.intersects(objectRect, pin1Rect) || Rect.intersects(objectRect, pin2Rect) || (newX >= pin1Rect.left && newX <= pin1Rect.right) || (newX >= pin2Rect.left && newX <= pin2Rect.right);
    }



    private void stopGame() {
        isGameActive = false;
        handler.removeCallbacksAndMessages(null);
        rotate.cancel();
        plane.setEnabled(false);
        showMessage();
    }


    private void showMessage() {
        ImageView bottom_img = findViewById(R.id.imageView);
        ImageView game_over = findViewById(R.id.game_over);
        ImageView restart_button = findViewById(R.id.restart_btn);
        game_over.setVisibility(View.VISIBLE);
        restart_button.setVisibility(View.VISIBLE);
        bottom_img.setVisibility(View.VISIBLE);
    }

    private boolean isCollision(ImageView fallingObject, ImageView plane) {
        Rect rect1 = new Rect();
        fallingObject.getHitRect(rect1);

        Rect rect2 = new Rect();
        plane.getHitRect(rect2);

        return Rect.intersects(rect1, rect2);
    }
    public static boolean isExistsAndActiveSim(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int simState = telephonyManager.getSimState();
            return simState == TelephonyManager.SIM_STATE_READY;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean isInternetConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }
    private void hideUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.systemBars());
            }
        } else {
            @SuppressWarnings("deprecation")
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

}