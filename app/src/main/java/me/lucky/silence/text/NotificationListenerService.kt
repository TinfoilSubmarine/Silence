package me.lucky.silence.text

import android.app.Notification
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberUtil
import me.lucky.silence.AllowNumber
import me.lucky.silence.AllowNumberDao
import me.lucky.silence.AppDatabase
import me.lucky.silence.Message
import me.lucky.silence.Preferences
import java.util.concurrent.TimeUnit

class NotificationListenerService : NotificationListenerService() {
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()
    private lateinit var prefs: Preferences
    private lateinit var db: AllowNumberDao
    private var telephonyManager: TelephonyManager? = null
    private var jobScheduler: JobScheduler? = null

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        prefs = Preferences(this)
        db = AppDatabase.getInstance(this).allowNumberDao()
        telephonyManager = getSystemService(TelephonyManager::class.java)
        jobScheduler = getSystemService(JobScheduler::class.java)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null
            || !prefs.isMessagesChecked
            || prefs.messages.and(Message.TEXT.value) == 0) return
        var hasNumber = false
        for (number in phoneNumberUtil
            .findNumbers(
                sbn.notification.extras[Notification.EXTRA_TEXT]?.toString() ?: return,
                telephonyManager?.networkCountryIso?.uppercase(),
            )
            .asSequence()
            .map { it.number() }
            .filter { phoneNumberUtil.getNumberType(it) == PhoneNumberUtil.PhoneNumberType.MOBILE }
            .map { AllowNumber.new(it, prefs.messagesTextTtl) }
        ) {
            try { db.insert(number) } catch (_: SQLiteConstraintException) { db.update(number) }
            hasNumber = true
        }
        if (hasNumber) schedule()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            migrateNotificationFilter(0, null)
    }

    private fun schedule() =
        jobScheduler?.schedule(
            JobInfo.Builder(
                CleanJobService.JOB_ID,
                ComponentName(this, CleanJobService::class.java),
            )
                .setMinimumLatency(
                    TimeUnit
                        .MINUTES
                        .toMillis(prefs.messagesTextTtl.toLong() + 5))
                .setPersisted(true)
                .build()
        )
}