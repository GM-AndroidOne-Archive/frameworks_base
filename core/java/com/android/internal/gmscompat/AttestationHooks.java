package com.android.internal.gmscompat;

import android.app.Application;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;

/** @hide */
public final class AttestationHooks {
    private static final String TAG = "GmsCompat/Attestation";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PROCESS_UNSTABLE = "com.google.android.gms.unstable";

    private static volatile boolean sIsGms = false;
    private static volatile boolean sIsFinsky = false;

    private static final String[] sCertifiedProps = new String[] {
        "angler",                                               // 0: PRODUCT
        "angler",                                               // 1: DEVICE
        "Huawei",                                               // 2: MANUFACTURER
        "google",                                               // 3: BRAND
        "Nexus 6P",                                             // 4: MODEL
        "google/angler/angler:8.1.0/OPM7.181205.001/5080180:user/release-keys", // 5: FINGERPRINT
        "2018-12-05",                                           // 6: SECURITY_PATCH
        "23",                                                   // 7: DEVICE_INITIAL_SDK_INT
        "OPM7.181205.001",                                      // 8: ID
        "user",                                                 // 9: TYPE
        "release-keys"                                          // 10: TAGS
    };

    private AttestationHooks() { }

    private static void setPropValue(String key, String value) {
        try {
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void setVersionFieldString(String key, String value) {
        try {
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build.VERSION." + key, e);
        }
    }

    private static void setVersionFieldInt(String key, int value) {
        try {
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build.VERSION." + key, e);
        }
    }

    private static void spoofBuildGms() {
        if (sCertifiedProps == null || sCertifiedProps.length == 0) return;
        Log.i(TAG, "Spoofing GMS fields using certified properties array.");
        
        setPropValue("PRODUCT", sCertifiedProps[0]);
        setPropValue("DEVICE", sCertifiedProps[1]);
        setPropValue("MANUFACTURER", sCertifiedProps[2]);
        setPropValue("BRAND", sCertifiedProps[3]);
        setPropValue("MODEL", sCertifiedProps[4]);
        setPropValue("FINGERPRINT", sCertifiedProps[5]);
        
        if (!sCertifiedProps[6].isEmpty()) {
            setVersionFieldString("SECURITY_PATCH", sCertifiedProps[6]);
        }
        if (!sCertifiedProps[7].isEmpty() && sCertifiedProps[7].matches("\\d+")) {
            setVersionFieldInt("DEVICE_INITIAL_SDK_INT", Integer.parseInt(sCertifiedProps[7]));
        }
        
        setPropValue("ID", sCertifiedProps[8]);
        setPropValue("TYPE", sCertifiedProps[9]);
        setPropValue("TAGS", sCertifiedProps[10]);
    }

    public static void initApplicationBeforeOnCreate(Application app) {
        if (PACKAGE_GMS.equals(app.getPackageName()) &&
                PROCESS_UNSTABLE.equals(Application.getProcessName())) {
            sIsGms = true;
            spoofBuildGms();
        }

        if (PACKAGE_FINSKY.equals(app.getPackageName())) {
            sIsFinsky = true;
            spoofBuildGms();
        }
    }

    public static void onEngineGetCertificateChain() {
        if (sIsGms) {
            Log.i(TAG, "Blocked hardware attestation call for GMS Unstable.");
            throw new UnsupportedOperationException();
        }
        if (sIsFinsky) {
            Log.i(TAG, "Blocked hardware attestation call for Play Store.");
            throw new UnsupportedOperationException();
        }
    }
}