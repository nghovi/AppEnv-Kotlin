package com.sollyu.android.appenv;

import android.app.Application;
import android.os.StrictMode;

import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.sollyu.android.appenv.helper.PhoneHelper;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.crashreport.CrashReport;
import com.umeng.analytics.MobclickAgent;

import org.apache.commons.io.FileUtils;
import org.xutils.common.util.IOUtil;
import org.xutils.x;

import java.io.File;

/**
 * 作者: Sollyu
 * 时间: 16/10/23
 * 联系: sollyu@qq.com
 * 说明:
 */
public class MainApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static MainApplication instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        MainApplication.instance = this;

        // Android-Bootstrap 图标注册
        TypefaceProvider.registerDefaultIconSets();

        LogConfiguration logConfiguration = new LogConfiguration.Builder().b().tag("AppEnv").logLevel(BuildConfig.DEBUG ? LogLevel.ALL : LogLevel.ERROR).build();
        XLog.init(logConfiguration);
        Thread.setDefaultUncaughtExceptionHandler(this);

        x.Ext.init(getInstance());
        x.Ext.setDebug(BuildConfig.DEBUG);

        MobclickAgent.startWithConfigure(new MobclickAgent.UMAnalyticsConfig(this, "558a1cb667e58e7649000228", BuildConfig.FLAVOR));
        MobclickAgent.setCatchUncaughtExceptions(false);
        MobclickAgent.enableEncrypt(true);

        CrashReport.UserStrategy userStrategy = new CrashReport.UserStrategy(getApplicationContext());
        userStrategy.setAppChannel(BuildConfig.FLAVOR);
        userStrategy.setAppVersion(BuildConfig.VERSION_NAME);
        userStrategy.setAppPackageName(BuildConfig.APPLICATION_ID);
        CrashReport.initCrashReport(getApplicationContext(), BuildConfig.BUGLY_APPID, BuildConfig.DEBUG);

        Bugly.init(getApplicationContext(), BuildConfig.BUGLY_APPID, BuildConfig.DEBUG);
        Beta.init(getApplicationContext(), BuildConfig.DEBUG);

        MainConfig.getInstance().init();

        // 释放文件
        try {
            File releaseFile = new File(this.getFilesDir(), "phone.json");
            if (!releaseFile.exists()) {
                FileUtils.writeByteArrayToFile(releaseFile, IOUtil.readBytes(getAssets().open("phone.json")));
            }

            PhoneHelper.getInstance().reload(this);
        } catch (Exception e) {
            MobclickAgent.reportError(this, e);
        }
        StrictMode.enableDefaults();
         StrictMode.ThreadPolicy policyRead = StrictMode.allowThreadDiskReads();
        StrictMode.setThreadPolicy(policyRead);
    }

    public synchronized static MainApplication getInstance() {
        return instance;
    }

    /**
     * @return 检查XPOSED是否工作
     */
    public boolean isXposedWork() {
        return false;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        XLog.e(e.getMessage(), e);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
