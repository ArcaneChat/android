package org.thoughtcrime.securesms;

import static org.thoughtcrime.securesms.connect.DcHelper.CONFIG_VERIFIED_ONE_ON_ONE_CHATS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.b44t.messenger.DcAccounts;
import com.b44t.messenger.DcContext;
import com.b44t.messenger.DcEvent;
import com.b44t.messenger.DcEventEmitter;
import com.b44t.messenger.rpc.Rpc;
import com.b44t.messenger.rpc.RpcException;

import org.thoughtcrime.securesms.connect.AccountManager;
import org.thoughtcrime.securesms.connect.DcEventCenter;
import org.thoughtcrime.securesms.connect.DcHelper;
import org.thoughtcrime.securesms.connect.FetchWorker;
import org.thoughtcrime.securesms.connect.ForegroundDetector;
import org.thoughtcrime.securesms.connect.KeepAliveService;
import org.thoughtcrime.securesms.connect.NetworkStateReceiver;
import org.thoughtcrime.securesms.crypto.DatabaseSecret;
import org.thoughtcrime.securesms.crypto.DatabaseSecretProvider;
import org.thoughtcrime.securesms.geolocation.DcLocationManager;
import org.thoughtcrime.securesms.jobmanager.JobManager;
import org.thoughtcrime.securesms.notifications.FcmReceiveService;
import org.thoughtcrime.securesms.notifications.InChatSounds;
import org.thoughtcrime.securesms.notifications.NotificationCenter;
import org.thoughtcrime.securesms.util.AndroidSignalProtocolLogger;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.Prefs;
import org.thoughtcrime.securesms.util.SignalProtocolLoggerProvider;
import org.thoughtcrime.securesms.util.Util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

public class ApplicationContext extends MultiDexApplication {
  private static final String TAG = ApplicationContext.class.getSimpleName();

  public static DcAccounts      dcAccounts;
  public Rpc                    rpc;
  public DcContext              dcContext;
  public DcLocationManager      dcLocationManager;
  public DcEventCenter          eventCenter;
  public NotificationCenter     notificationCenter;
  private JobManager            jobManager;

  private int                   debugOnAvailableCount;
  private int                   debugOnBlockedStatusChangedCount;
  private int                   debugOnCapabilitiesChangedCount;
  private int                   debugOnLinkPropertiesChangedCount;

  public static ApplicationContext getInstance(@NonNull Context context) {
    return (ApplicationContext)context.getApplicationContext();
  }

  @Override
  public void onCreate() {
    super.onCreate();

    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
      StringWriter stringWriter = new StringWriter();
      throwable.printStackTrace(new PrintWriter(stringWriter, true));
      String errorMsg = "Android " + Build.VERSION.RELEASE +":\n" + stringWriter.getBuffer().toString();
      String subject = "ArcaneChat " + BuildConfig.VERSION_NAME + " Crash Report";
      Intent intent = new Intent(android.content.Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
      intent.putExtra(android.content.Intent.EXTRA_TEXT, subject + "\n\n" + errorMsg);
      intent.putExtra(Intent.EXTRA_EMAIL, "adb@merlinux.eu");
      Intent chooser = Intent.createChooser(intent, subject);
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      chooser.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
      startActivity(chooser);

      try {
        ApplicationContext.this.finalize();
      } catch (Throwable e) {}
    });

    // if (LeakCanary.isInAnalyzerProcess(this)) {
    //   // This process is dedicated to LeakCanary for heap analysis.
    //   // You should not init your app in this process.
    //   return;
    // }
    // LeakCanary.install(this);

    Log.i("DeltaChat", "++++++++++++++++++ ApplicationContext.onCreate() ++++++++++++++++++");

    System.loadLibrary("native-utils");

    dcAccounts = new DcAccounts(new File(getFilesDir(), "accounts").getAbsolutePath());
    rpc = new Rpc(dcAccounts.getJsonrpcInstance());
    rpc.start();
    AccountManager.getInstance().migrateToDcAccounts(this);
    int[] allAccounts = dcAccounts.getAll();
    for (int accountId : allAccounts) {
      DcContext ac = dcAccounts.getAccount(accountId);
      if (!ac.isOpen()) {
        try {
          DatabaseSecret secret = DatabaseSecretProvider.getOrCreateDatabaseSecret(this, accountId);
          boolean res = ac.open(secret.asString());
          if (res) Log.i(TAG, "Successfully opened account " + accountId + ", path: " + ac.getBlobdir());
          else Log.e(TAG, "Error opening account " + accountId + ", path: " + ac.getBlobdir());
        } catch (Exception e) {
          Log.e(TAG, "Failed to open account " + accountId + ", path: " + ac.getBlobdir() + ": " + e);
          e.printStackTrace();
        }
      }
    }
    if (allAccounts.length == 0) {
      try {
        rpc.addAccount();
      } catch (RpcException e) {
        e.printStackTrace();
      }
    }
    dcContext = dcAccounts.getSelectedAccount();
    notificationCenter = new NotificationCenter(this);
    eventCenter = new DcEventCenter(this);
    new Thread(() -> {
      DcEventEmitter emitter = dcAccounts.getEventEmitter();
      while (true) {
        DcEvent event = emitter.getNextEvent();
        if (event==null) {
          break;
        }
        eventCenter.handleEvent(event);
      }
      Log.i("DeltaChat", "shutting down event handler");
    }, "eventThread").start();

    // migrating global notifications pref. to per-account config, added  10/July/24
    final String NOTIFICATION_PREF = "pref_key_enable_notifications";
    boolean isMuted = !Prefs.getBooleanPreference(this, NOTIFICATION_PREF, true);
    if (isMuted) {
      for (int accId : dcAccounts.getAll()) {
        dcAccounts.getAccount(accId).setMuted(true);
      }
      Prefs.removePreference(this, NOTIFICATION_PREF);
    }
    // /migrating global notifications

    for (int accountId : allAccounts) {
      dcAccounts.getAccount(accountId).setConfig(CONFIG_VERIFIED_ONE_ON_ONE_CHATS, "1");
    }

    // set translations before starting I/O to avoid sending untranslated MDNs (issue #2288)
    DcHelper.setStockTranslations(this);

    dcAccounts.startIo();

    new ForegroundDetector(ApplicationContext.getInstance(this));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      ConnectivityManager connectivityManager =
        (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
      connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull android.net.Network network) {
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onAvailable() #" + debugOnAvailableCount++);
          dcAccounts.maybeNetwork();
        }

        @Override
        public void onBlockedStatusChanged(@NonNull android.net.Network network, boolean blocked) {
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onBlockedStatusChanged() #" + debugOnBlockedStatusChangedCount++);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull android.net.Network network, NetworkCapabilities networkCapabilities) {
          // usually called after onAvailable(), so a maybeNetwork seems contraproductive
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onCapabilitiesChanged() #" + debugOnCapabilitiesChangedCount++);
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull android.net.Network network, LinkProperties linkProperties) {
          Log.i("DeltaChat", "++++++++++++++++++ NetworkCallback.onLinkPropertiesChanged() #" + debugOnLinkPropertiesChangedCount++);
        }
      });
    } // no else: use old method for debugging
    BroadcastReceiver networkStateReceiver = new NetworkStateReceiver();
    registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

    KeepAliveService.maybeStartSelf(this);

    initializeLogging();
    initializeJobManager();
    InChatSounds.getInstance(this);

    dcLocationManager = new DcLocationManager(this);
    DynamicTheme.setDefaultDayNightMode(this);

    IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
    registerReceiver(new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Util.localeChanged();
            DcHelper.setStockTranslations(context);
        }
    }, filter);

    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

    if (Prefs.isPushEnabled(this)) {
      FcmReceiveService.register(this);
    } else {
      Log.i(TAG, "FCM disabled at build time");
      // MAYBE TODO: i think the ApplicationContext is also created
      // when the app is stated by FetchWorker timeouts.
      // in this case, the normal threads shall not be started.
      Constraints constraints = new Constraints.Builder()
              .setRequiredNetworkType(NetworkType.CONNECTED)
              .build();
      PeriodicWorkRequest fetchWorkRequest = new PeriodicWorkRequest.Builder(
              FetchWorker.class,
              PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, // usually 15 minutes
              TimeUnit.MILLISECONDS,
              PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, // the start may be preferred by up to 5 minutes, so we run every 10-15 minutes
              TimeUnit.MILLISECONDS)
              .setConstraints(constraints)
              .build();
      WorkManager.getInstance(this).enqueueUniquePeriodicWork(
              "FetchWorker",
              ExistingPeriodicWorkPolicy.KEEP,
              fetchWorkRequest);
    }
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  private void initializeLogging() {
    SignalProtocolLoggerProvider.setProvider(new AndroidSignalProtocolLogger());
  }

  private void initializeJobManager() {
    this.jobManager = new JobManager(this, 5);
  }
}
