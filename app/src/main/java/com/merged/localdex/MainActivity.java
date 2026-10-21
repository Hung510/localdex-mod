package com.merged.localdex;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;

    private final Shizuku.OnRequestPermissionResultListener PERMISSION_LISTENER = (requestCode, grantResult) -> {
        updateShizukuStatus();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Shizuku.addRequestPermissionResultListener(PERMISSION_LISTENER);
        } catch (Throwable ignored) {}

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        statusText = new TextView(this);
        statusText.setTextSize(16f);
        layout.addView(statusText);

        TextView info = new TextView(this);
        info.setText("\nCấu hình cố định:\n- Độ phân giải: 1920x1200\n- Mật độ DPI: 180\n- Chế độ: LocalDeX tự động\n");
        info.setTextSize(14f);
        layout.addView(info);

        Button btnStartService = new Button(this);
        btnStartService.setText("Bật Tự Động Chạy Ngầm Khi Cắm Màn Hình");
        btnStartService.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, DeXWatcherService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(this, "Đã kích hoạt dịch vụ nền!", Toast.LENGTH_SHORT).show();
        });
        layout.addView(btnStartService);

        Button btnReset = new Button(this);
        btnReset.setText("Reset Màn Hình Về Mặc Định");
        btnReset.setOnClickListener(v -> {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Chưa kết nối Shizuku!", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                try {
                    java.lang.reflect.Method m = Shizuku.class.getMethod("newProcess", String[].class, String[].class, String.class);
                    m.setAccessible(true);
                    Process p = (Process) m.invoke(null, new String[]{"sh"}, null, null);
                    java.io.OutputStream os = p.getOutputStream();
                    os.write("wm size reset\nwm density reset\nexit\n".getBytes());
                    os.flush();
                    os.close();
                    p.waitFor();
                } catch (Exception ignored) {}
            }).start();
            Toast.makeText(this, "Đã reset màn hình gốc!", Toast.LENGTH_SHORT).show();
        });
        layout.addView(btnReset);

        setContentView(layout);
        checkAndRequestShizuku();
    }

    private void checkAndRequestShizuku() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(1001);
                }
            }
        } catch (Throwable ignored) {}
        updateShizukuStatus();
    }

    private void updateShizukuStatus() {
        try {
            if (!Shizuku.pingBinder()) {
                statusText.setText("Trạng thái Shizuku: CHƯA CHẠY");
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                statusText.setText("Trạng thái Shizuku: SẴN SÀNG CẤP QUYỀN");
            } else {
                statusText.setText("Trạng thái Shizuku: CHƯA CẤP QUYỀN");
            }
        } catch (Throwable e) {
            statusText.setText("Trạng thái Shizuku: Lỗi kết nối");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Shizuku.removeRequestPermissionResultListener(PERMISSION_LISTENER);
        } catch (Throwable ignored) {}
    }
}
