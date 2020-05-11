package cn.entertech.affectiveclouddemo.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import cn.entertech.affectiveclouddemo.R
import cn.entertech.affectiveclouddemo.app.Constant.Companion.MEDITATION_TYPE
import cn.entertech.ble.multiple.MultipleBiomoduleBleManager
import cn.entertech.bleuisdk.ui.DeviceUIConfig
import kotlinx.android.synthetic.main.activity_sensor_contact_check.*

class SensorContactCheckActivity : BaseActivity() {

    private var bleManager: MultipleBiomoduleBleManager? = null
    private var meditationType: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_contact_check)
        initFullScreenDisplay()
        meditationType = intent.getStringExtra(MEDITATION_TYPE)
        bleManager = DeviceUIConfig.getInstance(this!!).managers[0]
        if (bleManager!!.isConnected()) {
            bleManager!!.startHeartAndBrainCollection()
        } else {
            if (meditationType != null) {
                toMeditaitionActivity()
            }
        }
        initView()
    }

    fun toMeditaitionActivity() {
        var intent = Intent(
            this@SensorContactCheckActivity,
            MeditationActivity::class.java
        )
        startActivity(intent)
        finish ()
    }

    var runnable = Runnable {
        toMeditaitionActivity()
    }
    var mMainHandler = Handler(Looper.getMainLooper())
    var isShowWellDone = false
    fun initView() {
        if (meditationType == null) {
            tv_skip.visibility = View.GONE
        } else {
            tv_skip.visibility = View.VISIBLE
        }
        tv_skip.setOnClickListener {
            toMeditaitionActivity()
        }
        device_contact_indicator.addContactListener(fun() {
            if (!isShowWellDone) {
                isShowWellDone = true
//                showWellDoneTip()
                toMeditationPageWithDelay()
            }
        }, fun() {
            if (isShowWellDone) {
                isShowWellDone = false
                cancelToMeditationPage()
//                showContactIndicator()
            }
        })
    }

    fun toMeditationPageWithDelay() {
        mMainHandler.postDelayed(runnable, 1500)
    }

    fun cancelToMeditationPage() {
        mMainHandler.removeCallbacks(runnable)
    }

    fun showWellDoneTip() {
        ll_well_done_tip.visibility = View.VISIBLE
        device_contact_indicator.visibility = View.GONE
    }

    fun showContactIndicator() {
        ll_well_done_tip.visibility = View.GONE
        device_contact_indicator.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        cancelToMeditationPage()
        device_contact_indicator.release()
        super.onDestroy()
    }
}
