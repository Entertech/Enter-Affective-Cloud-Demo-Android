package cn.entertech.flowtimezh.ui.activity

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.ContentObserver
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.text.Html
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import cn.entertech.affectivecloudsdk.entity.Error
import cn.entertech.affectivecloudsdk.entity.Label
import cn.entertech.affectivecloudsdk.entity.RecData
import cn.entertech.affectivecloudsdk.interfaces.Callback
import cn.entertech.affectivecloudsdk.utils.ConvertUtil
import cn.entertech.ble.cushion.CushionBleManager
import cn.entertech.ble.innerpeacepro.InnerpeaceProBleManager
import cn.entertech.ble.single.BiomoduleBleManager
import cn.entertech.bleuisdk.ui.activity.DeviceManagerActivity
import cn.entertech.flowtime.utils.reportfileutils.FragmentBuffer
import cn.entertech.flowtime.utils.reportfileutils.MeditaionInterruptManager
import cn.entertech.flowtimezh.R
import cn.entertech.flowtimezh.app.Application
import cn.entertech.flowtimezh.app.Constant
import cn.entertech.flowtimezh.app.Constant.Companion.DEVICE_TYPE_CUSHION
import cn.entertech.flowtimezh.app.Constant.Companion.DEVICE_TYPE_ENTERTECH_VR
import cn.entertech.flowtimezh.app.Constant.Companion.DEVICE_TYPE_HEADBAND
import cn.entertech.flowtimezh.app.Constant.Companion.DEVICE_TYPE_INNERPEACE_PRO
import cn.entertech.flowtimezh.app.Constant.Companion.EXTRA_LABEL_ID
import cn.entertech.flowtimezh.app.Constant.Companion.EXTRA_MEDITATION_ID
import cn.entertech.flowtimezh.app.SettingManager
import cn.entertech.flowtimezh.database.*
import cn.entertech.flowtimezh.database.model.MeditationLabelsModel
import cn.entertech.flowtimezh.entity.MeditationEntity
import cn.entertech.flowtimezh.entity.MessageEvent
import cn.entertech.flowtimezh.entity.RecDataRecord
import cn.entertech.flowtimezh.entity.UserLessonEntity
import cn.entertech.flowtimezh.entity.meditation.ReportMeditationDataEntity
import cn.entertech.flowtimezh.ui.adapter.MeditationLabelsListAdapter
import cn.entertech.flowtimezh.ui.fragment.MeditationFragment
import cn.entertech.flowtimezh.ui.service.AffectiveCloudService
import cn.entertech.flowtimezh.ui.view.LoadingDialog
import cn.entertech.flowtimezh.ui.view.scrolllayout.ScrollLayout
import cn.entertech.flowtimezh.utils.*
import cn.entertech.uicomponentsdk.utils.dp
import com.chad.library.adapter.base.BaseQuickAdapter
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.activity_meditation.*
import kotlinx.android.synthetic.main.layout_common_title.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.*

class MeditationActivity : BaseActivity() {
    private var adapter: MeditationLabelsListAdapter? = null
    private var connection: ServiceConnection? = null
    internal var affectiveCloudService: AffectiveCloudService? = null
    private var isRecordTime: Boolean = true
    private var userId: String = ""

    var animatorSet: AnimatorSet? = null
    var biomoduleBleManager = BiomoduleBleManager.getInstance(Application.getInstance())
    var cushionBleManager = CushionBleManager.getInstance(Application.getInstance())
    var innerpeaceProBleManager = InnerpeaceProBleManager.getInstance(Application.getInstance())
    private var meditaiton: MeditationEntity? = null
    private var userLessonEntity: UserLessonEntity? = null
    var handler: Handler = Handler()
    var lessonId: Int = 0
    var courseId: Int = 0
    var lessonName: String = ""
    var courseName: String? = ""
    lateinit var scrollLayout: ScrollLayout
    var meditationFragment: MeditationFragment? = null
//    var meditationEditFragment: MeditationEditFragment? = null

    //    var webSocketManager: WebSocketManager = WebSocketManager.getInstance()
    var fragmentBuffer = FragmentBuffer()
    var sessionId: String? = null
    var userLessonStartTime: String? = null
    var meditationStartTime: Long? = null
    var meditationEndTime: String? = null
    var loadingDialog: LoadingDialog? = null
    private var meditationId: Long = -1
    private var mRecordId: Long = -1

    var isFixingFirmware = false
    var isFirstReceiveData = true
    var isFirstReceiveHRData = true

    private var endTime: Long? = null
    private var startTime: Long? = null

    var isCheckContact = false
    var isSensorCheckShow = false
    var contactWellCount = 0
    var isContactWell = false
    var needToCheckSensor = false
    var isTimeEnd = false

    var lastReceiveDataTimeMs: Long? = null

    private var wl: PowerManager.WakeLock? = null
    private var airSoundMediaPlayer: MediaPlayer? = null

    var isMeditationPause = true
    var saveRootPath: String? = ""
    var saveRawDataPath: String = ""
    var saveRealtimeDataPath: String = ""
    var saveReportDataPath: String = ""
    var fileName: String = ""
    var rawEEGFileHelper = FileHelper()
    var rawPEPRFileHelper = FileHelper()
    var rawInnerpeaceEEGFileHelper = FileHelper()
    var rawHRFileHelper = FileHelper()
    var realtimeEEGLeftFileHelper = FileHelper()
    var realtimeEEGRightFileHelper = FileHelper()
    var realtimeGammaFileHelper = FileHelper()
    var realtimeBetaFileHelper = FileHelper()
    var realtimeAlphaFileHelper = FileHelper()
    var realtimeThetaFileHelper = FileHelper()
    var realtimeDeltaFileHelper = FileHelper()
    var reportFileHelper = FileHelper()
    var deviceType: String? = null
    var isStartRecord = false

    //    var canExit = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meditation)
        initFullScreenDisplay()
        setStatusBarLight()
        EventBus.getDefault().register(this)
        userLessonStartTime = getCurrentTimeFormat()
        loadingDialog = LoadingDialog(this)
        deviceType = SettingManager.getInstance().deviceType
        bindAffectiveService()
        initView()
        initBleManager()
        playAirSound()
        initPowerManager()
        playSleepNoise()
        initVoiceListener()
        if (deviceType == DEVICE_TYPE_CUSHION) {
            meditationFragment?.hideBrainwaveAndAttention()
        }
        if (SettingManager.getInstance().isSaveData) {
            initFileWritter()
        }
    }

    fun initBleManager() {
        when (deviceType) {
            DEVICE_TYPE_HEADBAND, DEVICE_TYPE_ENTERTECH_VR -> {
                initFlowtimeManager()
            }

            DEVICE_TYPE_CUSHION -> {
                initCushionManager()
            }

            DEVICE_TYPE_INNERPEACE_PRO -> {
                initInnerpeaceManager()
            }
        }
    }

    fun bindAffectiveService() {
        var serviceIntent = Intent(this, AffectiveCloudService::class.java)
        userId = intent.getStringExtra("userId") ?: ""
        var sex = intent.getStringExtra("sex")
        var age = intent.getStringExtra("age")
        var experimentDao = ExperimentDao(this)
        var modeDao = ExperimentModeDao(this)
        var experiment = experimentDao.findExperimentBySelected()
        var case = listOf(experiment.id).toIntArray()
        var mode = modeDao.findModeByExperimentId(experiment.id).map { it.id }.toIntArray()
        serviceIntent.putExtra("userId", userId)
        serviceIntent.putExtra("sex", sex)
        serviceIntent.putExtra("age", age)
        serviceIntent.putExtra("case", case)
        serviceIntent.putExtra("mode", mode)
        connection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {

            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                affectiveCloudService = (service as AffectiveCloudService.MyBinder).service
                initAffectiveCloudManager()
                meditationFragment?.initNetListener()
            }

        }
        bindService(serviceIntent, connection!!, Context.BIND_AUTO_CREATE)
    }

    fun initSensorCheckDialog() {
        initAnimate()
        iv_skip.setOnClickListener {
            isCheckContact = false
            card_2.visibility = View.GONE
            card_3.visibility = View.VISIBLE
            sensor_check_animate_layout.toSecondPage()
        }
        btn_begin.setOnClickListener {
            isCheckContact = false
            hideSensorCheckDialog()
        }
        btn_skip_cancel.setOnClickListener {
            isCheckContact = true
            sensor_check_animate_layout.toFirstPage()
        }
        tv_skip_ok.setOnClickListener {
            isCheckContact = false
            hideSensorCheckDialog()
        }
        rl_get_good_signal.setOnClickListener {
            startActivity(
                Intent(
                    this@MeditationActivity,
                    SensorContactCheckActivity::class.java
                )
            )
            if (!isMeditationPause) {
                pauseMeditation()
            }
        }
    }

    fun initAnimate() {
        var currentDialogY = sensor_check_animate_layout.translationY
        var dialogAnimator = ObjectAnimator.ofFloat(
            sensor_check_animate_layout,
            "translationY",
            currentDialogY,
            currentDialogY + 434f.dp()
        )
        dialogAnimator.duration = 1
        dialogAnimator.interpolator = AccelerateInterpolator()
        dialogAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                if (ConnectedDeviceHelper.isConnected(deviceType)) {
                    if (!isSensorCheckShow) {
                        showSensorCheckDialog()
                    }
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        dialogAnimator.start()
    }

    fun showSensorCheckDialog() {
        if (ConnectedDeviceHelper.isConnected(deviceType)) {
            ConnectedDeviceHelper.startCollection()
        }
        rl_cover_sensor.visibility = View.VISIBLE
        rl_cover_sensor.setOnClickListener {
        }
        var currentY = sl_statistics_container.translationY
        var objectAnimation = ObjectAnimator.ofFloat(
            sl_statistics_container,
            "translationY",
            currentY,
            currentY + sl_statistics_container.screenHeight - sl_statistics_container.top
        )
        objectAnimation.duration = 300
        objectAnimation.interpolator = AccelerateDecelerateInterpolator()
        var currentDialogY = sensor_check_animate_layout.translationY
        var dialogAnimator = ObjectAnimator.ofFloat(
            sensor_check_animate_layout,
            "translationY",
            currentDialogY,
            currentDialogY - 434f.dp()
        )
        dialogAnimator.duration = 300
        dialogAnimator.interpolator = AccelerateDecelerateInterpolator()
        dialogAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                isCheckContact = true
                isSensorCheckShow = true
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        animatorSet = AnimatorSet()
        animatorSet?.play(objectAnimation)?.before(dialogAnimator)
        animatorSet?.start()
    }


    fun hideSensorCheckDialog() {
        rl_cover_sensor.visibility = View.GONE
        var currentY = sl_statistics_container.translationY
        var objectAnimation = ObjectAnimator.ofFloat(
            sl_statistics_container,
            "translationY",
            currentY,
            0f
        )
        objectAnimation.duration = 300
        objectAnimation.interpolator = AccelerateDecelerateInterpolator()
        var currentDialogY = sensor_check_animate_layout.translationY
        var dialogAnimator = ObjectAnimator.ofFloat(
            sensor_check_animate_layout,
            "translationY",
            currentDialogY,
            currentDialogY + 434f.dp()
        )
        dialogAnimator.duration = 300
        dialogAnimator.interpolator = AccelerateDecelerateInterpolator()
        animatorSet = AnimatorSet()
        animatorSet?.play(dialogAnimator)?.before(objectAnimation)
        animatorSet?.start()
        isSensorCheckShow = false
        needToCheckSensor = false
        if (affectiveCloudService != null && !affectiveCloudService!!.isInited() && !affectiveCloudService!!.isConnected()) {
            connectWebSocket()
        }
    }

    fun resumeMeditation() {
        isMeditationPause = false
        ConnectedDeviceHelper.startCollection()
    }

    fun pauseMeditation() {
        isMeditationPause = true
        ConnectedDeviceHelper.stopCollection()
    }

    var mMainHanlder = Handler()
    var isToConnectDevice: Boolean = false
    override fun onResume() {
        super.onResume()
        if (isToConnectDevice && ConnectedDeviceHelper.isConnected(deviceType)) {
            mMainHanlder.postDelayed(Runnable {
                showSensorCheckDialog()
                isToConnectDevice = false
            }, 1000)
        }
        if (!needToCheckSensor && isMeditationPause) {
            resumeMeditation()
        }
    }

    fun connectWebSocket() {
        if (ConnectedDeviceHelper.isConnected(deviceType)) {
            affectiveCloudService?.connectCloud(object : Callback {
                override fun onError(error: Error?) {
                }

                override fun onSuccess() {
                    Logger.d("affectivecloudmanager init success:")

//                    biomoduleBleManager.startHeartAndBrainCollection()
                }
            })
        }
    }

    private var mContentObserver: SettingsContentObserver? = null
    private var labelRecordPlayer: LabelRecordPlayer? = null
    fun initVoiceListener() {
        mContentObserver = SettingsContentObserver(
            this, Handler(
                Looper.getMainLooper()
            ), ::onVoiceChange
        )
        contentResolver.registerContentObserver(
            android.provider.Settings.System.CONTENT_URI,
            true,
            mContentObserver!!
        )
        labelRecordPlayer = LabelRecordPlayer(Application.getInstance())
    }

    class SettingsContentObserver(context: Context, handler: Handler?, var callback: (() -> Unit)) :
        ContentObserver(handler) {
        private val audioManager: AudioManager
        override fun deliverSelfNotifications(): Boolean {
            return false
        }

        var lastVolume: Int? = null
        override fun onChange(selfChange: Boolean) {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (lastVolume == null || currentVolume != lastVolume) {
                callback.invoke()
            }
            lastVolume = currentVolume
        }

        init {
            audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        }
    }

    fun onVoiceChange() {
        if (!isMeditationInit()) {
            return
        }
        if (!isStartRecord) {
            labelRecordPlayer?.playStartRecord()
            toStartRecord()
        } else {
            labelRecordPlayer?.playEndRecord()
            toEndRecord()
        }
    }

    fun initFileWritter() {
        fileName = "${getCurrentTimeFormat()}_${userId}"
        initSaveFiledir()
        if (saveRootPath == null) {
            return
        }
        FileStoreHelper.getInstance()
            .setPath(saveRootPath + File.separator + "标签" + File.separator, "label.txt")
        reportFileHelper.setFilePath(saveReportDataPath + "report.txt")
        if (deviceType == DEVICE_TYPE_INNERPEACE_PRO) {
            rawInnerpeaceEEGFileHelper.setFilePath(saveRawDataPath + "eeg.txt")
        } else if (deviceType == DEVICE_TYPE_CUSHION) {
            rawPEPRFileHelper.setFilePath(saveRawDataPath + "pepr.txt")
        } else {
            realtimeEEGLeftFileHelper.setFilePath(saveRealtimeDataPath + "brainwave_left.txt")
            realtimeEEGRightFileHelper.setFilePath(saveRealtimeDataPath + "brainwave_right.txt")
            realtimeAlphaFileHelper.setFilePath(saveRealtimeDataPath + "rhythms_alpha.txt")
            realtimeBetaFileHelper.setFilePath(saveRealtimeDataPath + "rhythms_beta.txt")
            realtimeGammaFileHelper.setFilePath(saveRealtimeDataPath + "rhythms_gamma.txt")
            realtimeThetaFileHelper.setFilePath(saveRealtimeDataPath + "rhythms_theta.txt")
            realtimeDeltaFileHelper.setFilePath(saveRealtimeDataPath + "rhythms_delta.txt")
            rawEEGFileHelper.setFilePath(saveRawDataPath + "eeg.txt")
            rawHRFileHelper.setFilePath(saveRawDataPath + "hr.txt")
        }
    }

    fun initSaveFiledir() {
        saveRootPath = getExternalFilesDir(fileName)?.absolutePath
        if (saveRootPath == null) {
            return
        }
        saveRealtimeDataPath = saveRootPath + File.separator + "realtime" + File.separator
        saveReportDataPath = saveRootPath + File.separator + "report" + File.separator
        saveRawDataPath = saveRootPath + File.separator + "raw" + File.separator
        var file = File(saveRootPath)
        var rawDir = File(saveRawDataPath)
        var realtimeDir = File(saveRealtimeDataPath)
        var reportDir = File(saveReportDataPath)
        if (!file.exists()) {
            file.mkdirs()
        }
        if (!rawDir.exists()) {
            rawDir.mkdirs()
        }
        if (!realtimeDir.exists()) {
            realtimeDir.mkdirs()
        }
        if (!reportDir.exists()) {
            reportDir.mkdirs()
        }
    }

    fun list2String(data: ArrayList<out Number>): String {
        return "${Arrays.toString(data.toArray())}".replace("[", "").replace("]", "")
    }

    fun initAffectiveCloudManager() {
        affectiveCloudService?.addListener({
            runOnUiThread {
                if (isFirstReceiveData) {
                    MeditationTimeManager.getInstance().timeReset()
                    meditationStartTime = System.currentTimeMillis()
                    meditationId = -System.currentTimeMillis()
                    fragmentBuffer.fileName = getCurrentTimeFormat(meditationStartTime!!)
                    isFirstReceiveData = false
                }
                if (deviceType == DEVICE_TYPE_CUSHION) {
                    meditationFragment?.hideBrainwaveAndAttention()
                    if (it != null && it!!.realtimePEPRData != null) {
                        if (!isFirstReceiveData) {
                            MeditationTimeManager.getInstance().timeIncrease()
                        }
                        meditationFragment?.showHeart(
                            it?.realtimePEPRData?.hr?.toInt(),
                            it?.realtimePEPRData?.hrv
                        )
                    }
                } else {
                    if (it != null && it!!.realtimeEEGData != null) {
                        if (!isFirstReceiveData) {
                            MeditationTimeManager.getInstance().timeIncrease()
                        }
                        realtimeEEGLeftFileHelper.writeData(list2String(it.realtimeEEGData!!.leftwave!!) + ",")
                        realtimeEEGRightFileHelper.writeData(list2String(it.realtimeEEGData!!.rightwave!!) + ",")
                        realtimeAlphaFileHelper.writeData("${it.realtimeEEGData!!.alphaPower!!},")
                        realtimeBetaFileHelper.writeData("${it.realtimeEEGData!!.betaPower!!},")
                        realtimeGammaFileHelper.writeData("${it.realtimeEEGData!!.gammaPower!!},")
                        realtimeThetaFileHelper.writeData("${it.realtimeEEGData!!.thetaPower!!},")
                        realtimeDeltaFileHelper.writeData("${it.realtimeEEGData!!.deltaPower!!},")
                    }

                    meditationFragment?.showHeart(
                        it?.realtimeHrData?.hr?.toInt(),
                        it?.realtimeHrData?.hrv
                    )
                    meditationFragment?.showBrain(it?.realtimeEEGData)
                }
            }
        }, {
            runOnUiThread {
                meditationFragment?.showAttention(it?.realtimeAttentionData?.attention?.toFloat())
                meditationFragment?.showRelaxation(it?.realtimeRelaxationData?.relaxation?.toFloat())
                meditationFragment?.showPressure(it?.realtimePressureData?.pressure?.toFloat())
                meditationFragment?.showMood(it?.realtimePleasureData?.pleasure?.toFloat())
                meditationFragment?.showArousal(it?.realtimeArousalData?.arousal?.toFloat())
                meditationFragment?.showCoherence(it?.realtimeCoherenceData?.coherence?.toFloat())
                meditationFragment?.showPleasure(it?.realtimePleasureData?.pleasure?.toFloat())
                meditationFragment?.showSleep(it?.realtimeSleepData?.sleepDegree?.toFloat())
                isSleep(it?.realtimeSleepData?.sleepState?.toInt())
            }
        })
    }

    var isSleep = false
    fun isSleep(sleepState: Int?) {
        if (sleepState == null) {
            return
        }
        if (!isSleep && sleepState == 1) {
            isSleep = true
            SoundScapeAudioManager.getInstance(this).pause()
        }
    }

    fun initPowerManager() {
        var pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My:sdf")
        wl?.acquire()
    }

    fun initView() {
        btn_end_experiment.setOnClickListener {
            if (isStartRecord) {
                showTipError(getString(R.string.segment_not_end))
                return@setOnClickListener
            }
            var meditationLabelsDao = MeditationLabelsDao(this@MeditationActivity)
            var meditationLabels = meditationLabelsDao.findByMeditationId(meditationId)
            if (meditationLabels.isNullOrEmpty() || isLabelFilled()) {
                showDialog()
            } else {
                ToastUtil.toastShort(Application.getInstance(), getString(R.string.label_is_null))
            }
        }
        initTilte()
        initDataFragment()
        initScrollLayout()
        initSensorCheckDialog()
        initTimeRecordView()
        initLabelList()
    }

    fun isLabelFilled(): Boolean {
        var meditationLabelsDao = MeditationLabelsDao(this@MeditationActivity)
        var meditationLabels = meditationLabelsDao.findByMeditationId(meditationId)
        var isLabelFilled = true
        for (label in meditationLabels) {
            if (label.dimIds.isNullOrEmpty()) {
                isLabelFilled = false
            }
        }
        return isLabelFilled
    }

    fun initTimeRecordView() {
        tv_record_btn.setOnClickListener {
            if (!isMeditationInit()) {
                return@setOnClickListener
            }
            if (!isStartRecord) {
                toStartRecord()
            } else {
                toEndRecord()
            }
        }

        var experimentDao = ExperimentDao(this)
        var experimentName = experimentDao.findExperimentBySelected().nameCn
        tv_experiment_name.text = experimentName
    }

    fun isMeditationInit(): Boolean {
        if (meditationId == -1L) {
            Toast.makeText(this, getString(R.string.affective_cloud_init), Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    fun toStartRecord() {
        isStartRecord = true
        tv_record_btn.btnText = getString(R.string.label_end_record)
        chronometer.visibility = View.VISIBLE
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
        startTime = MeditationTimeManager.getInstance().currentTimeMs()
    }

    fun toEndRecord() {
        isStartRecord = false
        tv_record_btn.btnText = getString(R.string.label_start_record)
        endTime = MeditationTimeManager.getInstance().currentTimeMs()
        chronometer.stop()
        chronometer.visibility = View.GONE
        storeLabels(startTime!!, endTime!!)
        refreshLabelList()
    }


    fun initLabelList() {
        var meditationLabelsDao = MeditationLabelsDao(this)
        var meditationLabels = meditationLabelsDao.findByMeditationId(meditationId) ?: listOf()
        adapter = MeditationLabelsListAdapter(meditationLabels)
        rv_label_list.adapter = adapter
        rv_label_list.layoutManager = LinearLayoutManager(this)
        if (meditationLabels.isNotEmpty()) {
            rv_label_list.visibility = View.VISIBLE
            tv_label_list_title.visibility = View.VISIBLE
//            rv_label_list.smoothScrollToPosition(meditationLabels.size - 1)
        }
        tv_segment_name.text = "${getString(R.string.label_segments)}${meditationLabels.size + 1}"
    }

    fun refreshLabelList() {
        var meditationLabelsDao = MeditationLabelsDao(this)
        var meditationLabels = meditationLabelsDao.findByMeditationId(meditationId) ?: listOf()
        if (meditationLabels.isNotEmpty()) {
            rv_label_list.visibility = View.VISIBLE
            tv_label_list_title.visibility = View.VISIBLE
//            rv_label_list.smoothScrollToPosition(meditationLabels.size - 1)
        }
        adapter?.setNewData(meditationLabels)
        adapter?.onItemChildClickListener =
            BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
                var intent = Intent(
                    this@MeditationActivity,
                    MeditationLabelsRecordActivity::class.java
                )
                intent.putExtra(
                    EXTRA_LABEL_ID,
                    meditationLabels[position].id
                )
                startActivity(intent)
            }
        tv_segment_name.text = "片段${meditationLabels.size + 1}"
    }

    fun storeLabels(labelStartTime: Long, labelEndTime: Long) {
        var meditationLabelsDao = MeditationLabelsDao(this)
        var meditationLabelsModel = MeditationLabelsModel()
        meditationLabelsModel.id = System.currentTimeMillis()
        meditationLabelsModel.endTime = labelEndTime
        meditationLabelsModel.startTime = labelStartTime
        meditationLabelsModel.meditationId = meditationId
        meditationLabelsModel.meditationStartTime = meditationStartTime!!
        meditationLabelsDao.create(meditationLabelsModel)

    }

    fun playSleepNoise() {
        var experimentDao = ExperimentDao(this)
        var experimentName = experimentDao.findExperimentBySelected().nameCn
        if (experimentName == "午休实验") {
            SoundScapeAudioManager.getInstance(this).setFile(R.raw.sleep_lightrain)
            SoundScapeAudioManager.getInstance(this).start()
        }
    }

    private var rawListener: ((ByteArray) -> Unit)? = null
    private var heartRateListener: ((Int) -> Unit)? = null
    private var bleConnectListener: ((String) -> Unit)? = null
    private var contactListener: ((Int) -> Unit)? = null
    private var bleDisconnectListener: ((String) -> Unit)? = null

    var writeFileDataBuffer = ArrayList<Int>()

    //    var brainDataList = ArrayList<Int>()
    fun initFlowtimeManager() {

        if (biomoduleBleManager.isConnected()) {
            needToCheckSensor = true
        }
        rawListener = fun(bytes: ByteArray) {
            var currentTimeMs = System.currentTimeMillis()
            if (affectiveCloudService?.isInited() == true) {
                for (byte in bytes) {
                    var brainData = ConvertUtil.converUnchart(byte)
                    writeFileDataBuffer.add((brainData))
                    if (writeFileDataBuffer.size >= 20) {
                        rawEEGFileHelper.writeData("${list2String(writeFileDataBuffer)},")
                        writeFileDataBuffer.clear()
                    }
                }
                affectiveCloudService?.appendEEGData(bytes)
            }
            if (lastReceiveDataTimeMs != null) {
                if ((currentTimeMs - lastReceiveDataTimeMs!!) >= 15000L) {
                    lastReceiveDataTimeMs = null
                    affectiveCloudService?.closeWebSocket()
                }
            }

        }
        heartRateListener = fun(heartRate: Int) {
            if (affectiveCloudService?.isInited() == true) {
                rawHRFileHelper.writeData("$heartRate,")
                affectiveCloudService?.appendHeartRateData(heartRate)
            }
        }
        bleConnectListener = fun(mac: String) {
            needToCheckSensor = true
        }
        bleDisconnectListener = fun(error: String) {
            needToCheckSensor = true
        }
        contactListener = fun(state: Int) {
//            Log.d("######", "contact is $state")
            runOnUiThread {
                if (isCheckContact) {
                    if (state == 0) {
                        if (!isContactWell) {
                            contactWellCount++
//                            Log.d("######", "contact is well")
                            if (contactWellCount == 5) {
                                isContactWell = true
                                card_2.visibility = View.VISIBLE
                                card_3.visibility = View.GONE
                                sensor_check_animate_layout.toSecondPage()
                            }
                        }
                    } else {
                        isContactWell = false
                        contactWellCount = 0
                    }
                }
            }
        }
        rawListener?.apply {
            biomoduleBleManager.addRawDataListener(this)
        }
        heartRateListener?.apply {
            biomoduleBleManager.addHeartRateListener(this)
        }
        bleConnectListener?.apply {
            biomoduleBleManager.addConnectListener(this)
        }
        contactListener?.apply {
            biomoduleBleManager.addContactListener(this)
        }
        bleDisconnectListener?.apply {
            biomoduleBleManager.addDisConnectListener(this)
        }
        biomoduleBleManager.startHeartAndBrainCollection()

    }

    private var cushionRawListener: ((ByteArray) -> Unit)? = null
    private var cushionConnectListener: ((String) -> Unit)? = null
    private var cushionDisconnectListener: ((String) -> Unit)? = null
    private var cushionContactListener: ((Int) -> Unit)? = null
    fun initCushionManager() {
        if (cushionBleManager.isConnected()) {
//            meditationFragment?.hideBrainwaveAndAttention()
            needToCheckSensor = true
        }
        var packCount = 0
        cushionRawListener = fun(bytes: ByteArray) {
            for (byte in bytes) {
                var brainData = ConvertUtil.converUnchart(byte)
                writeFileDataBuffer.add((brainData))
                if (writeFileDataBuffer.size >= bytes.size) {
                    rawPEPRFileHelper.writeData("${list2String(writeFileDataBuffer)},")
                    writeFileDataBuffer.clear()
                }
            }
            packCount++
            var currentTimeMs = System.currentTimeMillis()
            if (affectiveCloudService?.isInited() == true) {
                affectiveCloudService?.appendPEPR(bytes)
            }
            if (lastReceiveDataTimeMs != null) {
                if ((currentTimeMs - lastReceiveDataTimeMs!!) >= 15000L) {
                    lastReceiveDataTimeMs = null
                    affectiveCloudService?.closeWebSocket()
                }
            }

        }
        cushionConnectListener = fun(mac: String) {
//            meditationFragment?.hideBrainwaveAndAttention()
            needToCheckSensor = true
        }
        cushionDisconnectListener = fun(error: String) {
            needToCheckSensor = true
        }
        cushionContactListener = fun(state: Int) {
            runOnUiThread {
                if (isCheckContact) {
                    if (state == 0) {
                        if (!isContactWell) {
                            contactWellCount++
//                            Log.d("######", "contact is well")
                            if (contactWellCount == 5) {
                                isContactWell = true
                                card_2.visibility = View.VISIBLE
                                card_3.visibility = View.GONE
                                sensor_check_animate_layout.toSecondPage()
                            }
                        }
                    } else {
                        isContactWell = false
                        contactWellCount = 0
                    }
                }
            }
        }
        cushionBleManager.addRawDataListener(cushionRawListener!!)
        cushionBleManager.addConnectListener(cushionConnectListener!!)
        cushionBleManager.addDisConnectListener(cushionDisconnectListener!!)
        cushionBleManager.addContactDataListener(cushionContactListener!!)
        if (cushionBleManager.isConnected()) {
            cushionBleManager.startCollection()
        }
    }


    private var innerpeaceRawListener: ((ByteArray) -> Unit)? = null
    private var innerpeaceConnectListener: ((String) -> Unit)? = null
    private var innerpeaceDisconnectListener: ((String) -> Unit)? = null
    private var innerpeaceContactListener: ((Int) -> Unit)? = null
    fun initInnerpeaceManager() {
        if (innerpeaceProBleManager.isConnected()) {
//            meditationFragment?.hideBrainwaveAndAttention()
            needToCheckSensor = true
        }
        var packCount = 0
        innerpeaceRawListener = fun(bytes: ByteArray) {
            for (byte in bytes) {
                var brainData = ConvertUtil.converUnchart(byte)
                writeFileDataBuffer.add((brainData))
                if (writeFileDataBuffer.size >= bytes.size) {
                    rawInnerpeaceEEGFileHelper.writeData("${list2String(writeFileDataBuffer)},")
                    writeFileDataBuffer.clear()
                }
            }
            packCount++
            var currentTimeMs = System.currentTimeMillis()
            if (affectiveCloudService?.isInited() == true) {
                affectiveCloudService?.appendPEPR(bytes)
            }
            if (lastReceiveDataTimeMs != null) {
                if ((currentTimeMs - lastReceiveDataTimeMs!!) >= 15000L) {
                    lastReceiveDataTimeMs = null
                    affectiveCloudService?.closeWebSocket()
                }
            }

        }
        innerpeaceConnectListener = fun(mac: String) {
//            meditationFragment?.hideBrainwaveAndAttention()
            needToCheckSensor = true
        }
        innerpeaceDisconnectListener = fun(error: String) {
            needToCheckSensor = true
        }
        innerpeaceContactListener = fun(state: Int) {
            runOnUiThread {
                if (isCheckContact) {
                    if (state == 0) {
                        if (!isContactWell) {
                            contactWellCount++
//                            Log.d("######", "contact is well")
                            if (contactWellCount == 5) {
                                isContactWell = true
                                card_2.visibility = View.VISIBLE
                                card_3.visibility = View.GONE
                                sensor_check_animate_layout.toSecondPage()
                            }
                        }
                    } else {
                        isContactWell = false
                        contactWellCount = 0
                    }
                }
            }
        }
        innerpeaceProBleManager.addRawDataListener(innerpeaceRawListener!!)
        innerpeaceProBleManager.addConnectListener(innerpeaceConnectListener!!)
        innerpeaceProBleManager.addDisConnectListener(innerpeaceDisconnectListener!!)
        innerpeaceProBleManager.addContactListener(innerpeaceContactListener!!)
        if (innerpeaceProBleManager.isConnected()) {
            innerpeaceProBleManager.startCollection()
        }
    }


    lateinit var reportMeditationData: ReportMeditationDataEntity

    var finishRunnable = {
        exitWithoutMeditation()
    }

    fun saveReportFile(
        reportMeditationData: ReportMeditationDataEntity,
        fileWriteComplete: ((String) -> Unit)?
    ) {
        reportFileHelper.writeData(reportMeditationData.toString())
        fragmentBuffer.addFileWriteCompleteCallback(fileWriteComplete)
        fragmentBuffer.appendMeditationReport(
            reportMeditationData,
            meditationStartTime!!,
            MeditaionInterruptManager.getInstance().interruptTimestampList
        )
    }

    fun saveCushionMeditationInDB(report: ReportMeditationDataEntity) {
        var meditationDao = MeditationDao(this)
        meditaiton = MeditationEntity()
        meditaiton!!.id = meditationId
        meditaiton!!.startTime = fragmentBuffer.fileName
        meditaiton!!.finishTime = meditationEndTime
//        meditaiton!!.attentionAvg = report.reportAttentionEnitty!!.attentionAvg!!.toFloat()
//        meditaiton!!.attentionMax =
//            java.util.Collections.max(report.reportAttentionEnitty?.attentionRec).toFloat()
//        meditaiton!!.attentionMin =
//            java.util.Collections.min(report.reportAttentionEnitty?.attentionRec).toFloat()
        meditaiton!!.heartRateAvg = report.reportPEPRDataEntity!!.hrAvg!!.toFloat()
        meditaiton!!.heartRateMax = report.reportPEPRDataEntity!!.hrMax!!.toFloat()
        meditaiton!!.heartRateMin = report.reportPEPRDataEntity!!.hrMin!!.toFloat()
        meditaiton!!.heartRateVariabilityAvg = report.reportPEPRDataEntity!!.hrvAvg!!.toFloat()
//        meditaiton!!.relaxationAvg = report.reportRelaxationEnitty!!.relaxationAvg!!.toFloat()
//        meditaiton!!.relaxationMax =
//            java.util.Collections.max(report.reportRelaxationEnitty?.relaxationRec).toFloat()
//        meditaiton!!.relaxationMin =
//            java.util.Collections.min(report.reportRelaxationEnitty?.relaxationRec).toFloat()
        meditaiton!!.user = 0
//        var reportFileUri =
//            "${SettingManager.getInstance().userId}/${courseId}/${lessonId}/${fragmentBuffer.fileName}"
        meditaiton!!.meditationFile = fragmentBuffer.fileName

        var experimentDao = ExperimentDao(this)
        var experiment = experimentDao.findExperimentBySelected()
        if (experiment != null) {
            meditaiton!!.experimentId = experiment.id
        }
        meditaiton!!.experimentUserId = userId
        meditaiton!!.deviceType = SettingManager.getInstance().deviceType
        meditationDao.create(meditaiton)
    }

    fun saveHeadbandMeditationInDB(report: ReportMeditationDataEntity) {
        var meditationDao = MeditationDao(this)
        meditaiton = MeditationEntity()
        meditaiton!!.id = meditationId
        meditaiton!!.startTime = fragmentBuffer.fileName
        meditaiton!!.finishTime = meditationEndTime
        meditaiton!!.attentionAvg = report.reportAttentionEnitty!!.attentionAvg!!.toFloat()
        meditaiton!!.attentionMax =
            java.util.Collections.max(report.reportAttentionEnitty?.attentionRec).toFloat()
        meditaiton!!.attentionMin =
            java.util.Collections.min(report.reportAttentionEnitty?.attentionRec).toFloat()
        meditaiton!!.heartRateAvg = report.reportHRDataEntity!!.hrAvg!!.toFloat()
        meditaiton!!.heartRateMax = report.reportHRDataEntity!!.hrMax!!.toFloat()
        meditaiton!!.heartRateMin = report.reportHRDataEntity!!.hrMin!!.toFloat()
        meditaiton!!.heartRateVariabilityAvg = report.reportHRDataEntity!!.hrvAvg!!.toFloat()
        meditaiton!!.relaxationAvg = report.reportRelaxationEnitty!!.relaxationAvg!!.toFloat()
        meditaiton!!.relaxationMax =
            java.util.Collections.max(report.reportRelaxationEnitty?.relaxationRec).toFloat()
        meditaiton!!.relaxationMin =
            java.util.Collections.min(report.reportRelaxationEnitty?.relaxationRec).toFloat()
        meditaiton!!.user = 0
//        var reportFileUri =
//            "${SettingManager.getInstance().userId}/${courseId}/${lessonId}/${fragmentBuffer.fileName}"
        meditaiton!!.meditationFile = fragmentBuffer.fileName
        meditaiton!!.deviceType = SettingManager.getInstance().deviceType

        var experimentDao = ExperimentDao(this)
        var experiment = experimentDao.findExperimentBySelected()
        if (experiment != null) {
            meditaiton!!.experimentId = experiment.id
        }
        meditaiton!!.experimentUserId = userId
        meditationDao.create(meditaiton)
    }


    fun saveMeditationInDB(report: ReportMeditationDataEntity) {
        if (deviceType == DEVICE_TYPE_CUSHION) {
            saveCushionMeditationInDB(report)
        } else {
            saveHeadbandMeditationInDB(report)
        }
    }

    fun saveUserLessonInDB() {
        var userLessonRecordDao = UserLessonRecordDao(this)
        userLessonEntity = UserLessonEntity()
        mRecordId = -System.currentTimeMillis()
        userLessonEntity!!.id = mRecordId
        userLessonEntity!!.lessonName = lessonName
        userLessonEntity!!.courseName = courseName
        userLessonEntity!!.lessonId = lessonId
        userLessonEntity!!.courseId = courseId
        userLessonEntity!!.startTime = userLessonStartTime
        userLessonEntity!!.finishTime = meditationEndTime
        userLessonEntity!!.user = 0
        userLessonEntity!!.meditation = meditationId
        userLessonRecordDao.create(userLessonEntity)
    }

    fun toDataActivity() {
        var intent = Intent(this, DataActivity::class.java)
        intent.putExtra(Constant.RECORD_ID, mRecordId)
        startActivity(intent)
        finish()
    }

    fun initDataFragment() {
        meditationFragment = MeditationFragment()
//        meditationEditFragment = MeditationEditFragment()
        meditationFragment?.setScrollTopListener(object : MeditationFragment.IScrollTopListener {
            override fun isScrollTop(flag: Boolean) {
                scrollLayout.setIsChildListTop(flag)
            }
        })
        var fragmentManager = supportFragmentManager
        var fragmentTransaction = fragmentManager.beginTransaction()
        if (meditationFragment != null) {
            fragmentTransaction?.add(R.id.container, meditationFragment!!).commit()
//            fragmentTransaction?.add(R.id.container, meditationEditFragment!!)?.show(meditationFragment!!)
//                ?.hide(meditationEditFragment!!)
//                ?.commit()
        }
    }

    private fun initScrollLayout() {
        scrollLayout = findViewById(R.id.sl_statistics_container)
        scrollLayout.setIsChildListTop(true)
        var coverView = findViewById<RelativeLayout>(R.id.rl_cover)
        scrollLayout.setCoverView(coverView)
        scrollLayout.setMinOffset(0)
        scrollLayout.setMaxOffset((ScreenUtil.getScreenHeight(this) * 0.5).toInt())
        scrollLayout.setIsSupportExit(true)
//        Log.d("###", "navigation height is " + ScreenUtil.getNavigationBarHeight(Application.getInstance()))
        scrollLayout.setExitOffset(
            ScreenUtil.dip2px(
                this,
                38f
            ) + ScreenUtil.getNavigationBarHeight(Application.getInstance())
        )
        scrollLayout.setTouchOffset(ScreenUtil.dip2px(this, 38f))
        scrollLayout.isAllowHorizontalScroll = true
        scrollLayout.setToOpen()
        scrollLayout.isDraggable = true
        scrollLayout.visibility = View.VISIBLE
        scrollLayout.setOnScrollChangedListener(object : ScrollLayout.OnScrollChangedListener {
            override fun onScrollProgressChanged(currentProgress: Float) {}

            override fun onScrollFinished(currentStatus: ScrollLayout.Status) {
                if (currentStatus.equals(ScrollLayout.Status.EXIT)) {
                    val myRotateAnimation_down = RotateAnimation(
                        180.0f,
                        +0.0f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f
                    )
                    myRotateAnimation_down.duration = 300
                    myRotateAnimation_down.fillAfter = true
                    meditationFragment?.showMiniBar()
                } else if (currentStatus === ScrollLayout.Status.CLOSED) {
                    val myRotateAnimation_up = RotateAnimation(
                        0.0f,
                        +180.0f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f
                    )
                    myRotateAnimation_up.duration = 300
                    myRotateAnimation_up.fillAfter = true
                    meditationFragment?.hideMiniBar()
                } else {
                    meditationFragment?.hideMiniBar()
                }

            }

            override fun onChildScroll(top: Int) {}
        })
        scrollLayout.setCoverView(findViewById<RelativeLayout>(R.id.rl_cover))
        if (ConnectedDeviceHelper.isConnected(deviceType)) {
            scrollLayout.scrollToOpen()
        } else {
            scrollLayout.scrollToExit()
        }
    }

    fun toDisconnected(error: String) {
        runOnUiThread {
            showTipError(error)
        }
    }

    fun toConnected() {
        runOnUiThread {
            showTipSuccess("设备已连接")
        }
    }

    fun connectDevice() {
        showLoading("Connecting")
        ConnectedDeviceHelper.scanNearDeviceAndConnect(
            SettingManager.getInstance().deviceType,
            fun(deviceType) {

            }, fun(e, deviceType) {
                toDisconnected(e.toString())
            }, fun(mac, deviceType) {
                toConnected()
            }, fun(error, deviceType) {
                toDisconnected(error)
            })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent) {
//        var fragmentManager = supportFragmentManager
//        var fragmentTransaction = fragmentManager.beginTransaction()
        when (event.messageCode) {
            MessageEvent.MESSAGE_CODE_TO_DEVICE_CONNECT -> {
                if (!ConnectedDeviceHelper.isConnected(deviceType)) {
                    isToConnectDevice = true
                }
                if (!isMeditationPause) {
                    pauseMeditation()
                }
//                scrollLayout.scrollToOpen()
                connectDevice()
            }

            MessageEvent.MESSAGE_CODE_TO_NET_RESTORE -> {
                if (affectiveCloudService!!.getSessionId() != null) {
                    affectiveCloudService?.restoreCloud(object : Callback {
                        override fun onError(error: Error?) {
                        }

                        override fun onSuccess() {
                        }

                    })
                } else {
                    affectiveCloudService?.connectCloud(object : Callback {
                        override fun onError(error: Error?) {
                        }

                        override fun onSuccess() {
                            ConnectedDeviceHelper.startCollection()
                        }

                    })
                }
            }
        }
    }


    fun showDialog() {
        var dialog = AlertDialog.Builder(this)
            .setTitle(
                Html.fromHtml(
                    "<font color='${
                        ContextCompat.getColor(
                            this,
                            R.color.colorDialogTitle
                        )
                    }'>${getString(R.string.dialogExitTitle)}</font>"
                )
            )
            .setMessage(
                Html.fromHtml(
                    "<font color='${
                        ContextCompat.getColor(
                            this,
                            R.color.colorDialogContent
                        )
                    }'>${getString(R.string.dialogExitContent)}</font>"
                )
            )
            .setPositiveButton(
                Html.fromHtml(
                    "<font color='${
                        ContextCompat.getColor(
                            this,
                            R.color.colorDialogExit
                        )
                    }'>${getString(R.string.dialogExit)}</font>"
                )
            ) { dialog, which ->
                dialog.dismiss()
                finishMeditation()
            }
            .setNegativeButton(
                Html.fromHtml(
                    "<font color='${
                        ContextCompat.getColor(
                            this,
                            R.color.colorDialogCancel
                        )
                    }'>${getString(R.string.dialogCancel)}</font>"
                )
            ) { dialog, which ->
                dialog.dismiss()
            }.create()
        dialog.show()
    }

    override fun onBackPressed() {
        if (scrollLayout.currentStatus == ScrollLayout.Status.OPENED || scrollLayout.currentStatus == ScrollLayout.Status.CLOSED) {
            scrollLayout.scrollToExit()
        } else {
//            showDialog()
        }
    }

    fun initTilte() {
//        iv_back.setImageResource(R.mipmap.ic_premium_close)
//        iv_back.setOnClickListener {
//            showDialog()
////            showExitView()
//        }
        tv_title.visibility = View.INVISIBLE
        rl_menu_ic.visibility = View.GONE
        rl_menu_ic.setOnClickListener {
            var meditationLabelsDao = MeditationLabelsDao(this@MeditationActivity)
            var meditationLabels = meditationLabelsDao.findByMeditationId(meditationId)
            if (meditationId == -1L || meditationLabels == null || meditationLabels.isEmpty()) {
                finishMeditation()
            } else {
                var intent = Intent(this, MeditationLabelsCommitActivity::class.java)
                intent.putExtra(EXTRA_MEDITATION_ID, meditationId)
                startActivity(intent)
            }
        }
    }

    fun getReportAndExit() {
        affectiveCloudService?.getBiodataReport {
            reportMeditationData = it
            exitWithMeditation(reportMeditationData)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        refreshLabelList()
    }

    fun finishMeditation() {
        var meditationLabelsDao = MeditationLabelsDao(this)
        var meditationLabels = meditationLabelsDao.findByMeditationId(meditationId)
        var experimentDimDao = ExperimentDimDao(this)
        var experimentTagDao = ExperimentTagDao(this)
        var recDatas = ArrayList<RecData>()
        if (meditationLabels != null && meditationLabels.isNotEmpty()) {
            for (meditationLabel in meditationLabels) {
                var recData = RecData()
                recData.note = listOf(meditationLabel.note)
                recData.st =
                    (meditationLabel.startTime) / 1000f
                recData.et =
                    (meditationLabel.endTime) / 1000f
                var tagMap = HashMap<String, Any>()
                var dimIdStrings = meditationLabel.dimIds.split(",")
                for (dimIdString in dimIdStrings) {
                    var dimModel = experimentDimDao.findByDimId(dimIdString)
                    var dimValue = dimModel.value
                    var tag = experimentTagDao.findTagById(dimModel.tagId)
                    var tagNameEn = tag.nameEn
                    tagMap[tagNameEn] = dimValue
                }
                recData.tag = tagMap
                recDatas.add(recData)
            }
            saveLabelInLocal(recDatas)
        }
        ConnectedDeviceHelper?.stopCollection()
        startFinishTimer()
        reportMeditationData = ReportMeditationDataEntity()
        meditationEndTime = getCurrentTimeFormat()
        if (meditationTimeError() || !affectiveCloudService!!.isConnected()) {
            exitWithoutMeditation()
        } else {
            if (meditationLabels == null || meditationLabels.isEmpty()) {
                getReportAndExit()
            } else {
                loadingDialog?.loading("正在提交数标签...")
                affectiveCloudService?.submit(recDatas, object : Callback {
                    override fun onSuccess() {
                        runOnUiThread {
                            loadingDialog?.dismiss()
                            Toast.makeText(
                                this@MeditationActivity,
                                "标签提交成功！",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            getReportAndExit()
                        }
                    }

                    override fun onError(error: Error?) {
                        runOnUiThread {
                            loadingDialog?.dismiss()
                            Toast.makeText(
                                this@MeditationActivity,
                                "标签提交失败！${error.toString()}",
                                Toast.LENGTH_SHORT
                            ).show()
                            getReportAndExit()
                        }
                    }
                })
            }

        }
    }

    fun saveLabelInLocal(recDatas: List<RecData>) {
        var recDataRecord = RecDataRecord()
        recDataRecord.recDatas = recDatas
        recDataRecord.session_id = affectiveCloudService?.getSessionId()
        var json = Gson().toJson(recDataRecord)
        FileStoreHelper.getInstance().writeData(json)
    }


    fun exitWithMeditation(reportMeditationData: ReportMeditationDataEntity) {
        handler.removeCallbacks(finishRunnable)
        saveReportFile(reportMeditationData, fun(filePath) {
            saveMeditationInDB(reportMeditationData)
            saveUserLessonInDB()
            toDataActivity()
        })
    }

    fun exitWithoutMeditation() {
        finish()
    }

    fun meditationTimeError(): Boolean {
        if (meditationStartTime == null) {
            return true
        } else {
            var meditationEndTime = System.currentTimeMillis()
            var duration = (meditationEndTime - meditationStartTime!!) / 1000 / 60
            if (duration < 1) {
                Toast.makeText(this, "体验时间过短", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return false
    }

    private fun startFinishTimer() {
        handler.postDelayed({
            finishRunnable
        }, 10000)
    }

    private fun unBindAffectiveService() {
        unbindService(connection!!)
    }


    private fun playAirSound() {
        airSoundMediaPlayer = MediaPlayer.create(
            this@MeditationActivity,
            R.raw.airsound
        )
        airSoundMediaPlayer?.setOnCompletionListener {
            airSoundMediaPlayer?.start()
        }
        airSoundMediaPlayer?.start()
    }

    private fun stopAirSound() {
        airSoundMediaPlayer?.stop()
        airSoundMediaPlayer?.release()
    }

    override fun onDestroy() {
        SoundScapeAudioManager.getInstance(this).release()
        affectiveCloudService?.stopForeground()
        unBindAffectiveService()
        stopAirSound()
        wl?.release()
        if (mContentObserver != null) {
            contentResolver?.unregisterContentObserver(mContentObserver!!)
        }
        handler.removeCallbacks(finishRunnable)
        sessionId = null
        releaseBleManager()
        if (affectiveCloudService!!.isInited()) {
            affectiveCloudService?.release()
        }
        labelRecordPlayer?.release()
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    private fun releaseBleManager() {
        if (deviceType == DEVICE_TYPE_CUSHION) {
            releaseCushionBleManager()
        } else if (deviceType == DEVICE_TYPE_HEADBAND || deviceType == DEVICE_TYPE_ENTERTECH_VR) {
            releaseHeadbandBleManager()
        } else {
            releaseInnerpeaceBleManager()
        }
    }

    private fun releaseHeadbandBleManager() {
        biomoduleBleManager.stopHeartAndBrainCollection()
        biomoduleBleManager.stopBrainCollection()
        rawListener?.apply {
            biomoduleBleManager.removeRawDataListener(this)
        }
        heartRateListener?.apply {
            biomoduleBleManager.removeHeartRateListener(this)
        }
        contactListener?.apply {
            biomoduleBleManager.removeContactListener(this)
        }
        bleConnectListener?.apply {
            biomoduleBleManager.removeConnectListener(this)
        }
        bleDisconnectListener?.apply {
            biomoduleBleManager.removeDisConnectListener(this)
        }
    }

    private fun releaseCushionBleManager() {
        cushionBleManager.stopCollection()
        cushionRawListener?.apply {
            cushionBleManager.removeRawDataListener(this)
        }
        cushionContactListener?.apply {
            cushionBleManager.removeContactDataListener(this)
        }
        cushionDisconnectListener?.apply {
            cushionBleManager.removeDisConnectListener(this)

        }
        cushionConnectListener?.apply {
            cushionBleManager.removeConnectListener(this)
        }
    }

    private fun releaseInnerpeaceBleManager() {
        innerpeaceProBleManager.stopCollection()
        innerpeaceRawListener?.apply {
            innerpeaceProBleManager.removeRawDataListener(this)

        }
        innerpeaceConnectListener?.apply {
            innerpeaceProBleManager.removeConnectListener(this)

        }
        innerpeaceDisconnectListener?.apply {
            innerpeaceProBleManager.removeDisConnectListener(this)

        }
        innerpeaceContactListener?.apply {
            innerpeaceProBleManager.removeContactListener(this)
        }
    }
}
