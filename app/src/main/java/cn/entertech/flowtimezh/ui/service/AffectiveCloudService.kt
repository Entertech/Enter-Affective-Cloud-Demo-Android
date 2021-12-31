package cn.entertech.flowtimezh.ui.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cn.entertech.affectivecloudsdk.*
import cn.entertech.affectivecloudsdk.entity.*
import cn.entertech.affectivecloudsdk.interfaces.Callback
import cn.entertech.affectivecloudsdk.interfaces.Callback2
import cn.entertech.flowtimezh.R
import cn.entertech.flowtimezh.app.SettingManager
import cn.entertech.flowtimezh.entity.meditation.*
import com.orhanobut.logger.Logger
import java.util.*


internal class AffectiveCloudService : Service() {
    private var enterAffectiveCloudManager: EnterAffectiveCloudManager? = null
    private var affectiveListener: ((RealtimeAffectiveData?) -> Unit)? = null
    private var biodataListener: ((RealtimeBioData?) -> Unit)? = null

    override fun onBind(intent: Intent): IBinder? {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setSmallIcon(R.mipmap.ic_notification_logo)
            .setContentTitle("心流实验")
            .setContentText("心流实验正在后台运行")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        createNotificationChannel()
        val notificationManager = NotificationManagerCompat.from(this)

        // notificationId is a unique int for each notification that you must define

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, builder.build())
        startForeground(1, builder.build())

        initAffectiveCloudManager(intent)
        return MyBinder()
    }

    internal inner class MyBinder : Binder() {
        val service: AffectiveCloudService
            get() = this@AffectiveCloudService
    }

    override fun onCreate() {
        super.onCreate()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "name"
            val description = "description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("CHANNEL_ID", name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun stopForeground() {
        stopForeground(true)
    }

    fun addListener(
        biodataListener: (RealtimeBioData?) -> Unit,
        affectiveListener: (RealtimeAffectiveData?) -> Unit
    ) {
        this.biodataListener = biodataListener
        this.affectiveListener = affectiveListener
    }

    fun initAffectiveCloudManager(intent:Intent) {
        var userId = intent.getStringExtra("userId")
        var sex = intent.getStringExtra("sex")
        var age = intent.getStringExtra("age")
        var case = intent.getIntArrayExtra("case")
        var mode = intent.getIntArrayExtra("mode")
        var storageSettings = StorageSettings.Builder()
            .age(
                if (age == "") {
                    0
                } else {
                    Integer.parseInt(age)
                }
            ).sex(
                if (sex == "m") {
                    StorageSettings.Sex.MALE
                } else {
                    StorageSettings.Sex.FEMALE
                }
            ).case(case.toList())
            .mode(mode.toList())
            .build()

        var availableAffectiveServices =
            listOf(
                cn.entertech.affectivecloudsdk.entity.Service.PRESSURE,
                cn.entertech.affectivecloudsdk.entity.Service.COHERENCE
            )
        var availableBioServices = listOf(cn.entertech.affectivecloudsdk.entity.Service.PEPR)
        var biodataSubscribeParams = BiodataSubscribeParams.Builder()
            .requestPEPR()
            .build()

        var affectiveSubscribeParams = AffectiveSubscribeParams.Builder()
            .requestPressure()
            .requestCoherence()
            .build()
        var url = "wss://${SettingManager.getInstance().affectiveCloudServer}/ws/algorithm/v2/"
        var enterAffectiveCloudConfig = EnterAffectiveCloudConfig.Builder(
            SettingManager.getInstance().appKey,
            SettingManager.getInstance().appSecret,
            userId
        )
            .url(url)
            .timeout(10000)
            .availableBiodataServices(availableBioServices)
            .availableAffectiveServices(availableAffectiveServices)
            .biodataSubscribeParams(biodataSubscribeParams!!)
            .affectiveSubscribeParams(affectiveSubscribeParams!!)
            .storageSettings(storageSettings)
            .uploadCycle(1)
            .build()
        enterAffectiveCloudManager = EnterAffectiveCloudManager(enterAffectiveCloudConfig)
        enterAffectiveCloudManager!!.addBiodataRealtimeListener {
            biodataListener?.invoke(it)
        }
        enterAffectiveCloudManager!!.addAffectiveDataRealtimeListener {
            //            Logger.d("affective realtime data is " + it.toString())
            affectiveListener?.invoke(it)
        }
    }

    fun connectCloud(callback: Callback) {
        enterAffectiveCloudManager?.init(callback)
    }

    fun restoreCloud(callback: Callback) {
        enterAffectiveCloudManager?.restore(callback)
    }

    fun isInited(): Boolean {
        return enterAffectiveCloudManager != null && enterAffectiveCloudManager!!.isInit
    }

    fun setInit(isInit: Boolean) {
        if (enterAffectiveCloudManager != null) {
            enterAffectiveCloudManager!!.isInit = isInit
        }
    }

    fun getSessionId(): String? {
        if (enterAffectiveCloudManager != null && enterAffectiveCloudManager!!.mApi != null) {
            return enterAffectiveCloudManager!!.mApi!!.getSessionId()
        }
        return null
    }

    fun appendHeartRateData(heartRateData: Int, triggerCount: Int = 2) {
        enterAffectiveCloudManager?.appendHeartRateData(heartRateData)
    }

    fun appendEEGData(brainData: ByteArray, triggerCount: Int = 600) {
        enterAffectiveCloudManager?.appendEEGData(brainData)
    }

    fun appendPEPRData(peprData:ByteArray){
        enterAffectiveCloudManager?.appendPEPRData(peprData)
    }

    fun getBiodataReport(listener: (ReportMeditationDataEntity) -> Unit) {
        var reportMeditationData = ReportMeditationDataEntity()
        enterAffectiveCloudManager?.getBiodataReport(object : Callback2<HashMap<Any, Any?>> {
            override fun onError(error: Error?) {
            }

            override fun onSuccess(t: HashMap<Any, Any?>?) {
                Logger.d("report bio is " + t.toString())
                if (t == null) {
                    return
                }
                var reportHRDataEntity = ReportHRDataEntity()
                var hrMap = t!!["pepr"] as Map<Any, Any?>
                if (hrMap!!.containsKey("hr_avg")) {
                    reportHRDataEntity.hrAvg = hrMap["hr_avg"] as Double
                }
                if (hrMap!!.containsKey("hr_max")) {
                    reportHRDataEntity.hrMax = hrMap["hr_max"] as Double
                }
                if (hrMap!!.containsKey("hr_min")) {
                    reportHRDataEntity.hrMin = hrMap["hr_min"] as Double
                }
                if (hrMap!!.containsKey("hr_rec")) {
                    reportHRDataEntity.hrRec = hrMap["hr_rec"] as ArrayList<Double>
                }
                if (hrMap!!.containsKey("hrv_rec")) {
                    reportHRDataEntity.hrvRec = hrMap["hrv_rec"] as ArrayList<Double>
                }
                if (hrMap!!.containsKey("hrv_avg")) {
                    reportHRDataEntity.hrvAvg = hrMap["hrv_avg"] as Double
                }
                reportMeditationData.reportHRDataEntity = reportHRDataEntity

                enterAffectiveCloudManager?.getAffectiveDataReport(object :
                    Callback2<HashMap<Any, Any?>> {
                    override fun onError(error: Error?) {
                    }

                    override fun onSuccess(t: HashMap<Any, Any?>?) {
                        Logger.d("report affectve is " + t.toString())
                        if (t == null) {
                            return
                        }

                        var reportPressureEnitty = ReportPressureEnitty()
                        var pressureMap = t["pressure"]
                        if (pressureMap != null) {
                            pressureMap = pressureMap as Map<Any, Any?>
                            if (pressureMap.containsKey("pressure_avg")) {
                                reportPressureEnitty.pressureAvg =
                                    pressureMap["pressure_avg"] as Double
                            }
                            if (pressureMap.containsKey("pressure_rec")) {
                                reportPressureEnitty.pressureRec =
                                    pressureMap["pressure_rec"] as ArrayList<Double>
                            }
                        }
                        reportMeditationData.reportPressureEnitty = reportPressureEnitty

                        var  reportCoherenceEntity = ReportCoherenceEntity()
                        var coherenceMap = t["coherence"]
                        if (coherenceMap != null) {
                            coherenceMap = coherenceMap as Map<Any, Any?>
                            if (coherenceMap.containsKey("coherence_avg")) {
                                reportCoherenceEntity.coherenceAvg =
                                    coherenceMap["coherence_avg"] as Double
                            }
                            if (coherenceMap.containsKey("coherence_rec")) {
                                reportCoherenceEntity.coherenceRec =
                                    coherenceMap["coherence_rec"] as ArrayList<Double>
                            }
                            if (coherenceMap.containsKey("coherence_flag")) {
                                reportCoherenceEntity.coherenceFlag =
                                    coherenceMap["coherence_flag"] as ArrayList<Double>
                            }
                            if (coherenceMap.containsKey("coherence_duration")) {
                                reportCoherenceEntity.coherenceDuration =
                                    coherenceMap["coherence_duration"] as Double
                            }
                        }
                        reportMeditationData.reportCoherenceEntity = reportCoherenceEntity
                        listener.invoke(reportMeditationData)
                    }

                })
            }

        })
    }

    fun submit(remark: List<RecData>, callback: Callback){
        enterAffectiveCloudManager?.submit(remark,callback)
    }

    fun release() {
        enterAffectiveCloudManager?.release(object : Callback {
            override fun onError(error: Error?) {
                enterAffectiveCloudManager?.closeWebSocket()
            }
            override fun onSuccess() {
                enterAffectiveCloudManager?.closeWebSocket()
            }
        })
    }

    fun closeWebSocket(){
        enterAffectiveCloudManager?.closeWebSocket()
    }

    fun isConnected(): Boolean {
        return enterAffectiveCloudManager != null && enterAffectiveCloudManager!!.isWebSocketOpen()
    }

    fun addWebSocketConnectListener(listener: () -> Unit) {
        enterAffectiveCloudManager?.addWebSocketConnectListener(listener)
    }

    fun addWebSocketDisconnectListener(listener: (String) -> Unit) {
        enterAffectiveCloudManager?.addWebSocketDisconnectListener(listener)
    }

    fun removeWebSocketConnectListener(listener: () -> Unit) {
        enterAffectiveCloudManager?.removeWebSocketConnectListener(listener)
    }

    fun removeWebSocketDisconnectListener(listener: (String) -> Unit) {
        enterAffectiveCloudManager?.removeWebSocketDisconnectListener(listener)
    }
}