package com.chessbot.assist;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_CAPTURE = 1001;
    private static final int REQ_NOTIF = 1002;

    private TextView tvOverlayStatus;
    private TextView tvAccStatus;
    private TextView tvNotifStatus;
    private LinearLayout rowNotif;
    private Button btnOverlay;
    private Button btnCalibrate;
    private Button btnShutdown;
    private Button btnOverlayPerm;
    private Button btnAccPerm;
    private Button btnNotifPerm;
    private Spinner spSide;
    private Spinner spOrient;
    private Spinner spTap;
    private SeekBar sbSkill;
    private SeekBar sbTime;
    private TextView tvSkill;
    private TextView tvTime;
    private Button btnMaxTime;
    private TextView tvLog;
    private ScrollView svLog;
    private TextView tvHelp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.migrate(this);
        setContentView(R.layout.activity_main);

        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        tvAccStatus = findViewById(R.id.tvAccStatus);
        tvNotifStatus = findViewById(R.id.tvNotifStatus);
        rowNotif = findViewById(R.id.rowNotif);
        btnOverlay = findViewById(R.id.btnOverlay);
        btnCalibrate = findViewById(R.id.btnCalibrate);
        btnShutdown = findViewById(R.id.btnShutdown);
        btnOverlayPerm = findViewById(R.id.btnOverlayPerm);
        btnAccPerm = findViewById(R.id.btnAccPerm);
        btnNotifPerm = findViewById(R.id.btnNotifPerm);
        spSide = findViewById(R.id.spSide);
        spOrient = findViewById(R.id.spOrient);
        spTap = findViewById(R.id.spTap);
        sbSkill = findViewById(R.id.sbSkill);
        sbTime = findViewById(R.id.sbTime);
        tvSkill = findViewById(R.id.tvSkill);
        tvTime = findViewById(R.id.tvTime);
        btnMaxTime = findViewById(R.id.btnMaxTime);
        tvLog = findViewById(R.id.tvLog);
        svLog = findViewById(R.id.svLog);
        tvHelp = findViewById(R.id.tvHelp);

        setupSpinners();
        setupSeekBars();
        setupButtons();
        setupHelp();

        if (Build.VERSION.SDK_INT < 33) {
            rowNotif.setVisibility(View.GONE);
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
    }

    private void setupSpinners() {
        String[] sides = new String[]{"Trắng", "Đen"};
        ArrayAdapter<String> a1 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sides);
        a1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSide.setAdapter(a1);
        // This spinner stores the colour the human plays; the bot plays the
        // same colour (it is an assistant that moves your pieces).
        spSide.setSelection(Prefs.isBotWhite(this) ? 0 : 1);
        spSide.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Prefs.setBotWhite(MainActivity.this, position == 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        String[] orients = new String[]{"Tự động", "Trắng ở dưới", "Trắng ở trên"};
        ArrayAdapter<String> a2 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, orients);
        a2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOrient.setAdapter(a2);
        spOrient.setSelection(Math.max(0, Math.min(2, Prefs.getOrientation(this))));
        spOrient.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Prefs.setOrientation(MainActivity.this, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        String[] taps = new String[]{"Chạm 2 lần", "Kéo"};
        ArrayAdapter<String> a3 = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, taps);
        a3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTap.setAdapter(a3);
        spTap.setSelection(Prefs.getTapMode(this));
        spTap.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Prefs.setTapMode(MainActivity.this, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSeekBars() {
        int skill = Math.max(0, Math.min(20, Prefs.getSkill(this)));
        sbSkill.setProgress(skill);
        updateSkillLabel(skill);
        sbSkill.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSkillLabel(progress);
                Prefs.setSkill(MainActivity.this, progress);
            }
        });

        int time = Math.max(200, Math.min(10000, Prefs.getMovetime(this)));
        sbTime.setMax(98);
        sbTime.setProgress(Math.max(0, Math.min(98, (time - 200) / 100)));
        updateTimeLabel(time);
        sbTime.setOnSeekBarChangeListener(new SimpleSeek() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int ms = 200 + progress * 100;
                updateTimeLabel(ms);
                Prefs.setMovetime(MainActivity.this, ms);
            }
        });
        btnMaxTime.setOnClickListener(v -> {
            sbSkill.setProgress(20);
            sbTime.setProgress(98);
            toast("Đã đặt sức mạnh tối đa: Skill 20 + 10000 ms");
        });
    }

    private void updateSkillLabel(int v) {
        tvSkill.setText("Độ mạnh: " + v + "/20"
                + (v >= 20 ? " (tối đa, không giới hạn)" : ""));
    }

    private void updateTimeLabel(int ms) {
        tvTime.setText("Thời gian nghĩ: " + ms + " ms" + (ms >= 10000 ? " (tối đa)" : ""));
    }

    private void setupButtons() {
        btnOverlayPerm.setOnClickListener(v -> openOverlaySettings());
        btnAccPerm.setOnClickListener(v -> openAccessibilitySettings());
        btnNotifPerm.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
            }
        });

        btnOverlay.setOnClickListener(v -> {
            if (BotService.isPrepared()) {
                toast("Nút nổi đang bật. Chuyển sang app cờ vua và bấm ▶.");
            } else {
                requestPrepare();
            }
        });

        btnCalibrate.setOnClickListener(v -> {
            if (!BotService.isPrepared()) {
                toast("Bật nút nổi trước");
                return;
            }
            // Đưa ChessBot xuống dưới để lộ app cờ vua vừa dùng, rồi mới mở
            // khung hiệu chỉnh sau 3 giây để người dùng kịp chuyển app.
            toast("Chuyển sang app cờ vua — khung hiệu chỉnh hiện sau 3 giây");
            moveTaskToBack(true);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent i = new Intent(this, BotService.class);
                    i.setAction(BotService.ACTION_CALIBRATE);
                    startService(i);
                } catch (Throwable t) {
                    toast("Không mở được khung hiệu chỉnh — hãy bật lại nút nổi");
                }
            }, 3000);
        });

        btnShutdown.setOnClickListener(v -> {
            Intent i = new Intent(this, BotService.class);
            i.setAction(BotService.ACTION_SHUTDOWN);
            startService(i);
            toast("Đã tắt nút nổi");
            btnOverlay.postDelayed(this::refreshStatus, 300);
        });
    }

    private void setupHelp() {
        tvHelp.setText(
                "1. Cấp quyền “Nút nổi trên app khác” và “Trợ năng”.\n" +
                "2. Bấm BẬT NÚT NỔI → cho phép quay màn hình.\n" +
                "3. Mở app cờ vua, vào một ván mới (phải ở thế cờ bắt đầu).\n" +
                "4. Giữ nút nổi khoảng 0,5 giây → kéo 4 góc cho khung khớp mép bàn cờ → " +
                "chọn “Bạn cầm quân” ngay trong khung → Lưu vùng.\n" +
                "5. Nếu đã hiệu chỉnh rồi: bấm ▶ → chọn Trắng/Đen cho ván này.\n" +
                "6. Bot tự đi, chờ đối thủ rồi đi tiếp. Bấm ⏸ để dừng.\n\n" +
                "Lưu ý: bot bắt đầu từ thế cờ chuẩn và bám theo từng nước đi, nên phải bật từ đầu ván. " +
                "Từ thế cờ này, app học hình của đủ 12 loại quân trong bộ/theme đang dùng rồi nhận diện " +
                "các khung hình sau. Nước nhận diện vẫn phải hợp lệ theo luật cờ; nếu hình quân không rõ, " +
                "app dùng cách so ô cũ hoặc chờ, không đoán bừa. Nếu app hỏi phong cấp, hãy chọn Hậu. " +
                "Nút nổi nên đặt ngoài vùng bàn cờ.");
    }

    private void requestPrepare() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Cấp quyền “Nút nổi trên app khác” trước");
            openOverlaySettings();
            return;
        }
        if (!isAccessibilityEnabled()) {
            toast("Bật Trợ năng cho ChessBot trước");
            openAccessibilitySettings();
            return;
        }
        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_CAPTURE) {
            return;
        }
        if (resultCode != RESULT_OK || data == null) {
            toast("Bạn đã từ chối quyền quay màn hình");
            return;
        }
        Intent svc = new Intent(this, BotService.class);
        svc.setAction(BotService.ACTION_PREPARE);
        svc.putExtra("code", resultCode);
        svc.putExtra("data", data);
        startForegroundService(svc);
        toast("Đang bật nút nổi…");
        btnOverlay.postDelayed(this::refreshStatus, 600);
    }

    private void openOverlaySettings() {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Throwable t) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            } catch (Throwable t2) {
                toast("Không mở được cài đặt quyền nút nổi");
            }
        }
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Throwable t) {
            toast("Không mở được cài đặt Trợ năng");
        }
    }

    private boolean isAccessibilityEnabled() {
        if (BotAccessibilityService.isConnected()) {
            return true;
        }
        try {
            String enabled = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled == null) {
                return false;
            }
            ComponentName cn = new ComponentName(this, BotAccessibilityService.class);
            return enabled.contains(cn.flattenToString())
                    || enabled.contains(cn.flattenToShortString())
                    || enabled.contains(getPackageName() + "/" + BotAccessibilityService.class.getName());
        } catch (Throwable t) {
            return false;
        }
    }

    private void refreshStatus() {
        boolean overlay = Settings.canDrawOverlays(this);
        boolean acc = isAccessibilityEnabled();
        boolean notif = Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;

        setStatus(tvOverlayStatus, overlay);
        setStatus(tvAccStatus, acc);
        setStatus(tvNotifStatus, notif);

        boolean prepared = BotService.isPrepared();
        btnOverlay.setText(prepared ? "NÚT NỔI ĐANG BẬT" : "BẬT NÚT NỔI");
        btnOverlay.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                prepared ? 0xFF3F7A31 : 0xFF5FAE4A));
        btnCalibrate.setEnabled(prepared);
        btnCalibrate.setAlpha(prepared ? 1f : 0.45f);
    }

    private void setStatus(TextView tv, boolean ok) {
        tv.setText(ok ? "✓" : "✗");
        tv.setTextColor(ok ? 0xFF6FCF6F : 0xFFE05A5A);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        EventLog.setListener(line -> runOnUiThread(() -> appendLog(line)));
        StringBuilder sb = new StringBuilder();
        for (String l : EventLog.snapshot()) {
            sb.append(l).append('\n');
        }
        tvLog.setText(sb.toString());
        scrollLog();
    }

    @Override
    protected void onPause() {
        EventLog.setListener(null);
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshStatus();
    }

    private void appendLog(String line) {
        tvLog.append(line);
        tvLog.append("\n");
        scrollLog();
    }

    private void scrollLog() {
        svLog.post(() -> svLog.fullScroll(View.FOCUS_DOWN));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private abstract static class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
