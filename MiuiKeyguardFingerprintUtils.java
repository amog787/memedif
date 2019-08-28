package com.android.keyguard;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.keyguard.analytics.AnalyticsHelper;
import java.io.RandomAccessFile;
import miui.os.Build;

public class MiuiKeyguardFingerprintUtils {
    private static Handler sHandler;

    private static void initHandler() {
        if (sHandler == null) {
            HandlerThread handlerThread = new HandlerThread("MiuiKeyguardFingerprintUtils");
            handlerThread.start();
            sHandler = new Handler(handlerThread.getLooper());
        }
    }

    public static void processFingerprintResultAnalytics(int i) {
        initHandler();
        sHandler.post(new 1(i));
    }

    public static void processFingerprintResultAnalyticsForA4(int i) {
        Throwable e;
        Throwable th;
        RandomAccessFile randomAccessFile = null;
        try {
            RandomAccessFile randomAccessFile2 = new RandomAccessFile("/sdcard/MIUI/debug_log/1.dat", "r");
            try {
                randomAccessFile2.seek(randomAccessFile2.length() - 8);
                int readInt = randomAccessFile2.readInt();
                Log.d(KeyguardUpdateMonitor.class.getSimpleName(), "value: " + readInt);
                AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE + "_" + (readInt == 0 ? "yinlan" : "default"), (long) i);
                if (randomAccessFile2 != null) {
                    try {
                        randomAccessFile2.close();
                    } catch (Throwable e2) {
                        Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e2.getMessage(), e2);
                    }
                }
                randomAccessFile = randomAccessFile2;
            } catch (Exception e3) {
                e2 = e3;
                randomAccessFile = randomAccessFile2;
                try {
                    Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e2.getMessage(), e2);
                    AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE, (long) i);
                    if (randomAccessFile != null) {
                        try {
                            randomAccessFile.close();
                        } catch (Throwable e22) {
                            Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e22.getMessage(), e22);
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (randomAccessFile != null) {
                        try {
                            randomAccessFile.close();
                        } catch (Throwable e222) {
                            Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e222.getMessage(), e222);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                randomAccessFile = randomAccessFile2;
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                throw th;
            }
        } catch (Exception e4) {
            e222 = e4;
            Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e222.getMessage(), e222);
            AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE, (long) i);
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
    }

    public static void processFingerprintResultAnalyticsForA7(int i) {
        Throwable e;
        Throwable th;
        RandomAccessFile randomAccessFile = null;
        try {
            RandomAccessFile randomAccessFile2 = new RandomAccessFile("/sdcard/MIUI/debug_log/1.dat", "r");
            try {
                randomAccessFile2.seek(randomAccessFile2.length() - 4);
                byte readByte = randomAccessFile2.readByte();
                Log.d(KeyguardUpdateMonitor.class.getSimpleName(), "temperature: " + readByte);
                AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE + "_" + (readByte / 10), (long) i);
                if (randomAccessFile2 != null) {
                    try {
                        randomAccessFile2.close();
                    } catch (Throwable e2) {
                        Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e2.getMessage(), e2);
                    }
                }
                randomAccessFile = randomAccessFile2;
            } catch (Exception e3) {
                e2 = e3;
                randomAccessFile = randomAccessFile2;
                try {
                    Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e2.getMessage(), e2);
                    AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE, (long) i);
                    if (randomAccessFile != null) {
                        try {
                            randomAccessFile.close();
                        } catch (Throwable e22) {
                            Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e22.getMessage(), e22);
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (randomAccessFile != null) {
                        try {
                            randomAccessFile.close();
                        } catch (Throwable e222) {
                            Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e222.getMessage(), e222);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                randomAccessFile = randomAccessFile2;
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                throw th;
            }
        } catch (Exception e4) {
            e222 = e4;
            Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e222.getMessage(), e222);
            AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE, (long) i);
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
    }
}
