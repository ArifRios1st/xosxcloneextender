package com.arifrios1st.xosxcloneextender;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    public static final String TAG = "XClone-Extender";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.transsion.dualapp")) {
            return;
        }
        try{
            XposedHelpers.findAndHookMethod("com.transsion.dualapp.BootReceiver", lpparam.classLoader, "onReceive", android.content.Context.class, android.content.Intent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    modifyDualSupportList(lpparam.classLoader,context);
                }
            });

            XposedHelpers.findAndHookMethod("com.transsion.dualapp.MainActivity", lpparam.classLoader, "onCreate", android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = AndroidAppHelper.currentApplication();
                    if (context != null) {
                        modifyDualSupportList(lpparam.classLoader, context);
                    }
                }
            });

            XposedHelpers.findAndHookMethod("com.transsion.dualapp.utils.Utils", lpparam.classLoader, "getSupportApps", android.content.ContentResolver.class, java.util.Set.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = AndroidAppHelper.currentApplication();
                    if (context != null) {
                        modifyDualSupportList(lpparam.classLoader, context);
                    }
                }
            });

            XposedHelpers.findAndHookMethod("com.transsion.dualapp.utils.Utils", lpparam.classLoader, "getLaunchableApps", android.content.pm.PackageManager.class, int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = AndroidAppHelper.currentApplication();
                    if (context != null) {
                        modifyDualSupportList(lpparam.classLoader, context);
                    }
                }
            });
        } catch (NoSuchMethodError e) {
            Log.e(TAG, "Method not found: " + e.getMessage(), e);
        } catch (XposedHelpers.ClassNotFoundError e) {
            Log.e(TAG, "Class not found: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Caught an exception: " + e.getMessage(), e);
        } catch (Throwable t) {
            Log.e(TAG, "Unexpected error in handleLoadPackage: ", t);
        }
    }

    private void modifyDualSupportList(ClassLoader classLoader, Context context) throws Throwable {
        // Retrieve the DUAL_SUPPORT_LIST field
        Class<?> staticListsClass = XposedHelpers.findClass("com.transsion.dualapp.StaticLists", classLoader);
        Field DUAL_SUPPORT_LIST_FIELD = staticListsClass.getDeclaredField("DUAL_SUPPORT_LIST");
        DUAL_SUPPORT_LIST_FIELD.setAccessible(true);
        Set<String> DUAL_SUPPORT_LIST = (Set<String>) DUAL_SUPPORT_LIST_FIELD.get(null);

        if (DUAL_SUPPORT_LIST == null) {
            throw new IllegalStateException("DUAL_SUPPORT_LIST is null. Cannot modify it.");
        }

        Field DUAL_MAYBE_SUPPORT_LIST_FIELD = staticListsClass.getDeclaredField("DUAL_MAYBE_SUPPORT_LIST");
        DUAL_MAYBE_SUPPORT_LIST_FIELD.setAccessible(true);
        Set<String> DUAL_MAYBE_SUPPORT_LIST = (Set<String>) DUAL_MAYBE_SUPPORT_LIST_FIELD.get(null);


        // Get the PackageManager to retrieve installed apps
        PackageManager packageManager = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // Query for all apps that have launch intents
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo resolveInfo : resolveInfos) {
            ApplicationInfo appInfo = resolveInfo.activityInfo.applicationInfo;
            String packageName = appInfo.packageName;

            // Check if it's a system app (not a user app)
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                // Add package name to DUAL_SUPPORT_LIST if not already present
                if (!DUAL_SUPPORT_LIST.contains(packageName)) {
                    DUAL_SUPPORT_LIST.add(packageName);
                    Log.d(TAG, "Added " + packageName + " to DUAL_SUPPORT_LIST");
                }
                if (DUAL_MAYBE_SUPPORT_LIST != null) {
                    DUAL_MAYBE_SUPPORT_LIST.remove(packageName);
                }
            }
        }
    }
}
