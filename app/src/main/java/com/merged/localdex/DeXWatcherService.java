package com.merged.localdex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Display;
import androidx.core.app.NotificationCompat;
import java.io.OutputStream;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;

public class DeXWatcherService extends Service implements DisplayManager.DisplayListener {

    private DisplayManager displayManager;
    private static final String CHANNEL_ID = "LocalDeXChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LocalDeX Auto đang chạy ngầm")
                .setContentText("Đang lắng nghe kết nối màn hình ngoài/xe ô tô")
                .setSmallIcon(android.R.drawable.ic_menu_slideshow)
                .build();
        startForeground(1, notification);

        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(this, null);
        }
    }

    @Override
    public void onDisplayAdded(int displayId) {
        if (displayId == Display.DEFAULT_DISPLAY) return;
        applyDeXConfig(displayId);
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        if (displayId != Display.DEFAULT_DISPLAY) {
            resetConfig();
        }
    }

    @Override
    public void onDisplayChanged(int displayId) {}

    private void applyDeXConfig(int displayId) {
        if (!Shizuku.pingBinder()) return;

        new Thread(() -> {
            try {
                Method newProcessMethod = Shizuku.class.getMethod("newProcess", String[].class, String[].class, String.class);
                newProcessMethod.setAccessible(true);
                Process process = (Process) newProcessMethod.invoke(null, new String[]{"sh"}, null, null);

                OutputStream os = process.getOutputStream();
                String[] cmds = new String[]{
                    "wm size 1920x1200 -d " + displayId,
                    "wm density 180 -d " + displayId,
                    "wm user-rotation -d " + displayId + " lock 1",
                    "settings put global force_desktop_mode_on_external_displays 1",
                    "settings put global enable_freeform_support 1",
                    "sleep 0.5",
                    "am start --display " + displayId + " com.sec.android.app.desktoplauncher/.activity.LauncherActivity"
                };

                for (String cmd : cmds) {
                    os.write((cmd + "\n").getBytes());
                    os.flush();
                }
                os.write("exit\n".getBytes());
                os.flush();
                os.close();
                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void resetConfig() {
        if (!Shizuku.pingBinder()) return;
        new Thread(() -> {
            try {
                Method newProcessMethod = Shizuku.class.getMethod("newProcess", String[].class, String[].class, String.class);
                newProcessMethod.setAccessible(true);
                Process process = (Process) newProcessMethod.invoke(null, new String[]{"sh"}, null, null);
                OutputStream os = process.getOutputStream();
                os.write("wm size reset\nwm density reset\nexit\n".getBytes());
                os.flush();
                os.close();
                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (displayManager != null) {
            displayManager.unregisterDisplayListener(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "LocalDeX Service Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
