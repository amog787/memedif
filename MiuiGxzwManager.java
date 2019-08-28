package com.android.keyguard.fod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.systemui.statusbar.phone.NotificationPanelView;

public class MiuiGxzwManager extends Binder {
    private static MiuiGxzwManager sService;
    private boolean mBouncer = false;
    private BroadcastReceiver mBroadcastReceiver = new 4(this);
    private Context mContext;
    private WakeLock mDrawWakeLock;
    private int mGxzwUnlockMode = 0;
    private Handler mHandler = new 2(this);
    private boolean mIgnoreFocusChange = false;
    private Runnable mIgnoreFocusRunnable = new 1(this);
    private IntentFilter mIntentFilter;
    private boolean mKeyguardAuthen = false;
    private boolean mKeyguardShow;
    private KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new 3(this);
    private MiuiGxzwIconView mMiuiGxzwIconView;
    private MiuiGxzwOverlayView mMiuiGxzwOverlayView;
    private NotificationPanelView mNotificationPanelView;
    private boolean mShouldShowGxzwIconInKeyguard = true;
    private boolean mShowed = false;

    public MiuiGxzwManager(Context context) {
        this.mContext = context;
        this.mMiuiGxzwOverlayView = new MiuiGxzwOverlayView(this.mContext);
        this.mMiuiGxzwIconView = new MiuiGxzwIconView(this.mContext);
        this.mMiuiGxzwIconView.setCollectGxzwListener(this.mMiuiGxzwOverlayView);
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        instance.registerCallback(this.mKeyguardUpdateMonitorCallback);
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        this.mDrawWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(128, "gxzw");
        if (instance.isFingerprintDetectionRunning()) {
            dealCallback(1, 0);
        }
    }

    private int dealCallback(int i, int i2) {
        Log.i("MiuiGxzwManager", "dealCallback, cmd: " + i + " param: " + i2);
        switch (i) {
            case 1:
                this.mHandler.removeMessages(1001);
                this.mHandler.sendMessage(this.mHandler.obtainMessage(1001, i2, 0));
                break;
            case 2:
                this.mHandler.removeMessages(1002);
                this.mHandler.sendEmptyMessage(1002);
                break;
            case 3:
                processVendorSucess(i2);
                break;
            case 4:
                this.mHandler.removeMessages(1006);
                this.mHandler.sendMessage(this.mHandler.obtainMessage(1006, i2, 0));
                break;
            case 5:
                this.mHandler.removeMessages(1003);
                this.mHandler.sendEmptyMessage(1003);
                break;
            case 101:
                this.mHandler.removeMessages(1005);
                this.mHandler.sendEmptyMessage(1005);
                break;
            case 102:
                this.mHandler.removeMessages(1004);
                this.mHandler.sendEmptyMessage(1004);
                break;
        }
        return 1;
    }

    private void dismissGxzwView() {
        Log.i("MiuiGxzwManager", "dismissGxzwView: mShowed = " + this.mShowed);
        if (this.mShowed) {
            this.mMiuiGxzwIconView.dismiss();
            this.mMiuiGxzwOverlayView.dismiss();
            this.mShowed = false;
            this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        }
    }

    public static MiuiGxzwManager getInstance() {
        return sService;
    }

    private synchronized boolean getKeyguardAuthen() {
        return this.mKeyguardAuthen;
    }

    private void ignoreFocusChangeForWhile() {
        this.mIgnoreFocusChange = true;
        this.mHandler.removeCallbacks(this.mIgnoreFocusRunnable);
        this.mHandler.postDelayed(this.mIgnoreFocusRunnable, 1000);
    }

    public static void init(Context context) {
        try {
            sService = new MiuiGxzwManager(context);
            ServiceManager.addService("service_name", sService);
            Log.d("MiuiGxzwManager", "add MiuiGxzwManager successfully");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MiuiGxzwManager", "add MiuiGxzwManager fail");
        }
    }

    private void processVendorSucess(int i) {
        if (i != 0) {
            if (getKeyguardAuthen()) {
                if (KeyguardUpdateMonitor.getInstance(this.mContext).isUnlockingWithFingerprintAllowed(MiuiKeyguardUtils.getAuthUserId(this.mContext, i))) {
                    if (MiuiKeyguardUtils.isDozing()) {
                        setGxzwUnlockMode(1);
                        this.mContext.sendBroadcast(new Intent("com.miui.keyguard.face_unlock_succeed"));
                    } else {
                        setGxzwUnlockMode(2);
                    }
                    this.mHandler.removeMessages(1002);
                    this.mHandler.sendEmptyMessage(1002);
                }
            } else {
                this.mHandler.removeMessages(1002);
                this.mHandler.sendEmptyMessage(1002);
            }
        }
    }

    private synchronized void setGxzwUnlockMode(int i) {
        this.mGxzwUnlockMode = i;
    }

    private synchronized void setKeyguardAuthen(boolean z) {
        boolean z2 = this.mKeyguardAuthen;
        this.mKeyguardAuthen = z;
        if (z2 != z) {
            this.mMiuiGxzwOverlayView.onKeyguardAuthen(z);
            this.mMiuiGxzwIconView.onKeyguardAuthen(z);
        }
    }

    private void showGxzwView(boolean z) {
        Log.i("MiuiGxzwManager", "showGxzwView: lightIcon = " + z + ", mShowed = " + this.mShowed + ", mShouldShowGxzwIconInKeyguard = " + this.mShouldShowGxzwIconInKeyguard + ", keyguardAuthen = " + getKeyguardAuthen());
        if (!this.mShowed) {
            MiuiGxzwUtils.caculateGxzwIconSize();
            this.mMiuiGxzwOverlayView.show();
            if (this.mShouldShowGxzwIconInKeyguard || !getKeyguardAuthen()) {
                this.mMiuiGxzwIconView.show(z);
            }
            this.mShowed = true;
            this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
        }
    }

    public void dismissGxzwIconView(boolean z) {
        boolean z2 = true;
        Log.i("MiuiGxzwManager", "dismissGxzwIconView: dismiss = " + z);
        if (this.mShouldShowGxzwIconInKeyguard != (!z) && this.mKeyguardShow) {
            if (z) {
                z2 = false;
            }
            this.mShouldShowGxzwIconInKeyguard = z2;
            if (this.mShowed && getKeyguardAuthen()) {
                if (z) {
                    this.mMiuiGxzwIconView.dismiss();
                } else {
                    this.mMiuiGxzwIconView.show(false);
                }
            }
        }
    }

    public synchronized int getGxzwUnlockMode() {
        return this.mGxzwUnlockMode;
    }

    public boolean isBouncer() {
        return this.mBouncer;
    }

    public boolean isIgnoreFocusChange() {
        return this.mIgnoreFocusChange;
    }

    public boolean isShouldShowGxzwIconInKeyguard() {
        return this.mShouldShowGxzwIconInKeyguard;
    }

    public synchronized boolean isUnlockByGxzw() {
        boolean z = true;
        synchronized (this) {
            if (!(this.mGxzwUnlockMode == 1 || this.mGxzwUnlockMode == 2)) {
                z = false;
            }
        }
        return z;
    }

    public void onKeyguardHide() {
        Log.d("MiuiGxzwManager", "onKeyguardHide");
        this.mKeyguardShow = false;
        dismissGxzwView();
        this.mShouldShowGxzwIconInKeyguard = true;
    }

    public void onKeyguardShow() {
        Log.d("MiuiGxzwManager", "onKeyguardShow");
        this.mKeyguardShow = true;
        setGxzwUnlockMode(0);
        if (KeyguardUpdateMonitor.getInstance(this.mContext).isFingerprintDetectionRunning() && !this.mShowed) {
            this.mHandler.removeMessages(1001);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1001, 0, 0));
        }
    }

    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        switch (i) {
            case 1:
                parcel.enforceInterface("interface_descriptor");
                int dealCallback = dealCallback(parcel.readInt(), parcel.readInt());
                parcel2.writeNoException();
                parcel2.writeInt(dealCallback);
                return true;
            default:
                return super.onTransact(i, parcel, parcel2, i2);
        }
    }

    public void releaseDrawWackLock() {
        this.mDrawWakeLock.release();
    }

    public void requestDrawWackLock() {
        this.mDrawWakeLock.acquire();
    }

    public void requestDrawWackLock(long j) {
        this.mDrawWakeLock.acquire(j);
    }

    public void setNotificationPanelView(NotificationPanelView notificationPanelView) {
        this.mNotificationPanelView = notificationPanelView;
    }

    public void setUnlockLockout(boolean z) {
        this.mMiuiGxzwIconView.setUnlockLockout(z);
    }

    public void showGxzwInKeyguardWhenLockout() {
        if (!this.mShowed) {
            setKeyguardAuthen(true);
            showGxzwView(false);
            this.mMiuiGxzwIconView.setEnrolling(false);
        }
    }

    public void stopDozing() {
        Log.i("MiuiGxzwManager", "stopDozing");
        ignoreFocusChangeForWhile();
        this.mMiuiGxzwOverlayView.stopDozing();
        this.mMiuiGxzwIconView.stopDozing();
    }

    public void updateGxzwState() {
        if (this.mNotificationPanelView != null) {
            this.mNotificationPanelView.updateGxzwState();
        }
    }
}
