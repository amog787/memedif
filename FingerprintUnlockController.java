package com.android.systemui.statusbar.phone;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.MiuiSettings.System;
import android.util.Log;
import android.util.Slog;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.LatencyTracker;
import com.android.keyguard.MiuiKeyguardFingerprintUtils;
import com.android.keyguard.MiuiKeyguardFingerprintUtils.FingerprintIdentificationState;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.KeyguardViewMediator;

public class FingerprintUnlockController extends KeyguardUpdateMonitorCallback {
    private boolean mCancelingPendingLock = false;
    private final Context mContext;
    private DozeScrimController mDozeScrimController;
    private FingerprintIdentificationState mFpiState;
    private Handler mHandler = new Handler();
    private KeyguardViewMediator mKeyguardViewMediator;
    private int mMode;
    private int mPendingAuthenticatedUserId = -1;
    private PowerManager mPowerManager;
    private final Runnable mReleaseFingerprintWakeLockRunnable = new 1(this);
    private ScrimController mScrimController;
    private StatusBar mStatusBar;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarWindowManager mStatusBarWindowManager;
    private final UnlockMethodCache mUnlockMethodCache;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private WakeLock mWakeLock;

    public FingerprintUnlockController(Context context, DozeScrimController dozeScrimController, KeyguardViewMediator keyguardViewMediator, ScrimController scrimController, StatusBar statusBar, UnlockMethodCache unlockMethodCache) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mUpdateMonitor.registerCallback(this);
        this.mStatusBarWindowManager = (StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class);
        this.mDozeScrimController = dozeScrimController;
        this.mKeyguardViewMediator = keyguardViewMediator;
        this.mScrimController = scrimController;
        this.mStatusBar = statusBar;
        this.mUnlockMethodCache = unlockMethodCache;
    }

    private int calculateMode(int i) {
        boolean isUnlockingWithFingerprintAllowed = this.mUpdateMonitor.isUnlockingWithFingerprintAllowed(i);
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            return !this.mStatusBarKeyguardViewManager.isShowing() ? 4 : (this.mDozeScrimController.isPulsing() && isUnlockingWithFingerprintAllowed) ? 2 : (isUnlockingWithFingerprintAllowed || !this.mUnlockMethodCache.isMethodSecure()) ? 1 : 3;
        } else {
            if (this.mStatusBarKeyguardViewManager.isShowing()) {
                if (this.mStatusBarKeyguardViewManager.isBouncerShowing() && isUnlockingWithFingerprintAllowed) {
                    return 6;
                }
                if (isUnlockingWithFingerprintAllowed) {
                    return 5;
                }
                if (!this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    return 3;
                }
            }
            return 0;
        }
    }

    private void cleanup() {
        releaseFingerprintWakeLock();
    }

    private void keyguardDoneWithoutHomeAnim() {
        System.putBooleanForUser(this.mContext.getContentResolver(), "is_fingerprint_unlock", true, -2);
    }

    private void recordUnlockWay() {
        AnalyticsHelper.recordUnlockWay("fp");
        this.mKeyguardViewMediator.recordFingerprintUnlockState();
    }

    private void releaseFingerprintWakeLock() {
        if (this.mWakeLock != null) {
            this.mHandler.removeCallbacks(this.mReleaseFingerprintWakeLockRunnable);
            Log.i("FingerprintController", "releasing fp wakelock");
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
    }

    public void finishKeyguardFadingAway() {
        resetMode();
        this.mStatusBarWindowManager.setForceDozeBrightness(false);
        if (this.mStatusBar.getNavigationBarView() != null) {
            this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(false);
        }
        this.mStatusBar.notifyFpAuthModeChanged();
    }

    public int getMode() {
        return this.mMode;
    }

    public synchronized boolean isCancelingPendingLock() {
        return this.mCancelingPendingLock;
    }

    public void onFingerprintAcquired() {
        Trace.beginSection("FingerprintUnlockController#onFingerprintAcquired");
        releaseFingerprintWakeLock();
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            if (LatencyTracker.isEnabled(this.mContext)) {
                LatencyTracker.getInstance(this.mContext).onActionStart(2);
            }
            this.mWakeLock = this.mPowerManager.newWakeLock(1, "wake-and-unlock wakelock");
            Trace.beginSection("acquiring wake-and-unlock");
            this.mWakeLock.acquire();
            Trace.endSection();
            Log.i("FingerprintController", "fingerprint acquired, grabbing fp wakelock");
            this.mHandler.postDelayed(this.mReleaseFingerprintWakeLockRunnable, 15000);
            if (this.mDozeScrimController.isPulsing()) {
                this.mStatusBarWindowManager.setForceDozeBrightness(true);
            }
        }
        Trace.endSection();
    }

    public void onFingerprintAuthFailed() {
        cleanup();
        this.mFpiState = FingerprintIdentificationState.FAILED;
        MiuiKeyguardFingerprintUtils.processFingerprintResultAnalytics(0);
    }

    public void onFingerprintAuthenticated(int i) {
        Trace.beginSection("FingerprintUnlockController#onFingerprintAuthenticated");
        if (this.mUpdateMonitor.isGoingToSleep()) {
            int i2 = 0;
            boolean isUnlockingWithFingerprintAllowed = this.mUpdateMonitor.isUnlockingWithFingerprintAllowed(i);
            if (this.mDozeScrimController.isPulsing() && isUnlockingWithFingerprintAllowed) {
                i2 = 2;
            } else if (isUnlockingWithFingerprintAllowed || !this.mUnlockMethodCache.isMethodSecure()) {
                i2 = 1;
            }
            this.mUpdateMonitor.setFingerprintMode(i2);
            if (!this.mKeyguardViewMediator.isShowing() && KeyguardUpdateMonitor.getCurrentUser() == i && ((i2 == 2 || i2 == 1) && MiuiKeyguardUtils.isAodClockDisable(this.mContext))) {
                Slog.i("miui_keyguard_fingerprint", "Unlock by fingerprint, keyguard is not showing and wake up");
                recordUnlockWay();
                this.mKeyguardViewMediator.cancelPendingLock();
                synchronized (this) {
                    this.mCancelingPendingLock = true;
                }
                this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.policy:FINGERPRINT");
            } else {
                this.mPendingAuthenticatedUserId = i;
                this.mKeyguardViewMediator.recordFingerprintUnlockState();
            }
            Trace.endSection();
            return;
        }
        boolean isDeviceInteractive = this.mUpdateMonitor.isDeviceInteractive();
        this.mMode = calculateMode(i);
        if (!(KeyguardUpdateMonitor.getCurrentUser() == i || this.mMode == 3 || this.mMode == 0 || this.mMode == 4)) {
            if (MiuiKeyguardUtils.canSwitchUser(this.mContext, i)) {
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    MiuiGxzwManager.getInstance().onKeyguardHide();
                }
                try {
                    ActivityManagerNative.getDefault().switchUser(i);
                } catch (Throwable e) {
                    Log.e("FingerprintController", "switchUser failed", e);
                }
            } else {
                this.mMode = 3;
            }
        }
        this.mUpdateMonitor.setFingerprintMode(this.mMode);
        PanelBar.LOG(getClass(), "calculateMode userid=" + i + ";mode=" + this.mMode);
        if (!isDeviceInteractive) {
            Log.i("FingerprintController", "fp wakelock: Authenticated, waking up...");
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.policy:FINGERPRINT");
        }
        Trace.beginSection("release wake-and-unlock");
        releaseFingerprintWakeLock();
        Trace.endSection();
        switch (this.mMode) {
            case 1:
            case 2:
                if (this.mMode == 2) {
                    Trace.beginSection("MODE_WAKE_AND_UNLOCK_PULSING");
                    this.mStatusBar.updateMediaMetaData(false, true);
                } else {
                    Trace.beginSection("MODE_WAKE_AND_UNLOCK");
                    this.mDozeScrimController.abortDoze();
                }
                keyguardDoneWithoutHomeAnim();
                this.mStatusBarWindowManager.setStatusBarFocusable(false);
                this.mKeyguardViewMediator.onWakeAndUnlocking();
                this.mScrimController.setWakeAndUnlocking();
                this.mDozeScrimController.setWakeAndUnlocking();
                if (this.mStatusBar.getNavigationBarView() != null) {
                    this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(true);
                }
                recordUnlockWay();
                Trace.endSection();
                break;
            case 3:
                Trace.beginSection("MODE_SHOW_BOUNCER");
                if (!isDeviceInteractive) {
                    this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
                }
                this.mStatusBarKeyguardViewManager.animateCollapsePanels(0.0f);
                Trace.endSection();
                break;
            case 5:
                Trace.beginSection("MODE_UNLOCK");
                keyguardDoneWithoutHomeAnim();
                if (!isDeviceInteractive) {
                    this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
                }
                this.mStatusBarWindowManager.setStatusBarFocusable(false);
                this.mKeyguardViewMediator.keyguardDone();
                recordUnlockWay();
                Trace.endSection();
                break;
            case 6:
                Trace.beginSection("MODE_DISMISS");
                this.mStatusBarKeyguardViewManager.notifyKeyguardAuthenticated(false);
                recordUnlockWay();
                Trace.endSection();
                break;
        }
        if (this.mMode != 2) {
            this.mStatusBarWindowManager.setForceDozeBrightness(false);
        }
        this.mStatusBar.notifyFpAuthModeChanged();
        this.mFpiState = FingerprintIdentificationState.SUCCEEDED;
        MiuiKeyguardFingerprintUtils.processFingerprintResultAnalytics(1);
        Trace.endSection();
    }

    public void onFingerprintError(int i, String str) {
        cleanup();
        if (FingerprintIdentificationState.ERROR != this.mFpiState && (i == 7 || i == 9)) {
            this.mStatusBarKeyguardViewManager.animateCollapsePanels(0.0f);
        }
        this.mFpiState = FingerprintIdentificationState.ERROR;
    }

    public void onFinishedGoingToSleep(int i) {
        Trace.beginSection("FingerprintUnlockController#onFinishedGoingToSleep");
        if (this.mPendingAuthenticatedUserId != -1) {
            this.mHandler.post(new 2(this, this.mPendingAuthenticatedUserId));
        }
        this.mPendingAuthenticatedUserId = -1;
        Trace.endSection();
    }

    public void onScreenTurnedOn() {
        if (this.mUpdateMonitor.isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser()) && (!this.mUpdateMonitor.isUnlockingWithFingerprintAllowed() || this.mUpdateMonitor.isFingerprintTemporarilyLockout())) {
            this.mStatusBarKeyguardViewManager.animateCollapsePanels(0.0f);
        }
        synchronized (this) {
            if (this.mCancelingPendingLock) {
                this.mCancelingPendingLock = false;
                resetMode();
            }
        }
    }

    public void onStartedGoingToSleep(int i) {
        this.mPendingAuthenticatedUserId = -1;
    }

    public synchronized void resetCancelingPendingLock() {
        if (this.mCancelingPendingLock) {
            this.mCancelingPendingLock = false;
            this.mHandler.post(new 4(this));
        }
    }

    public void resetMode() {
        this.mMode = 0;
        this.mUpdateMonitor.setFingerprintMode(this.mMode);
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    public void startKeyguardFadingAway() {
        this.mHandler.postDelayed(new 3(this), 96);
    }
}
