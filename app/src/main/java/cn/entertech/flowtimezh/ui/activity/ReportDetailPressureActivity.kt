package cn.entertech.flowtimezh.ui.activity

import android.animation.Animator
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import cn.entertech.flowtimezh.R
import cn.entertech.flowtimezh.app.Constant
import cn.entertech.flowtimezh.app.Constant.Companion.RECORD_ID
import cn.entertech.flowtimezh.app.SettingManager
import cn.entertech.flowtimezh.database.UserLessonRecordDao
import cn.entertech.flowtimezh.mvp.model.FirebaseRemoteConfigShare
import cn.entertech.flowtimezh.utils.ShotShareUtil
import cn.entertech.flowtimezh.utils.getExperienceStartTime
import cn.entertech.flowtimezh.utils.reportfileutils.MeditationReportDataAnalyzed
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_report_detail_pressure.*
import kotlinx.android.synthetic.main.activity_report_detail_pressure.ll_bg
import kotlinx.android.synthetic.main.activity_report_detail_pressure.scroll_view
import kotlinx.android.synthetic.main.layout_common_title.*

class ReportDetailPressureActivity : BaseActivity() {

    private var lastAverage: Double = 0.0
    private var lastValue: Int = 0
    private var popupWindow: PopupWindow? = null
    private var userLessonRecordDao: UserLessonRecordDao? = null
    private var meditationReportDataAnalyzed: MeditationReportDataAnalyzed? = null
    private var fileName: String? = null
    private var mRecordId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail_pressure)
        initFullScreenDisplay()
        mRecordId = intent.getLongExtra(RECORD_ID, -1L)
        initTitle()
        initData()
        initLineChart()
        initLastAverageChart()
        initShareView()
    }

    fun initShareView() {
        if (isShareCondition()&& !SettingManager.getInstance().isReportShared("${mRecordId}_${Constant.REPORT_SHARE_PAGE_PRESSURE}")) {
            showShareView()
        } else {
            hideShareView()
        }
    }

    fun showShareView() {
        SettingManager.getInstance().setIsReportShared("${mRecordId}_${Constant.REPORT_SHARE_PAGE_PRESSURE}",true)
        iv_menu_icon.visibility = View.GONE
        lottie_view.visibility = View.VISIBLE
        lottie_view.setOnClickListener {
            shareReport()
            hideShareView()
        }
        var popView =
            LayoutInflater.from(this@ReportDetailPressureActivity).inflate(R.layout.pop_share_tip, null)
        popupWindow = PopupWindow(popView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lottie_view.post {
            popupWindow?.showAsDropDown(lottie_view, 0, 0, Gravity.BOTTOM)
        }

        lottie_view.addAnimatorListener(object: Animator.AnimatorListener{
            override fun onAnimationStart(animation: Animator?) {

            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                iv_menu_icon.visibility = View.VISIBLE
                lottie_view.visibility = View.GONE
            }

        })

        Handler().postDelayed(object:Runnable{
            override fun run() {
                if (popupWindow != null && popupWindow!!.isShowing){
                    popupWindow!!.dismiss()
                }
            }

        },5000)
    }

    fun hideShareView() {
        iv_menu_icon.visibility = View.VISIBLE
        lottie_view.visibility = View.GONE
        popupWindow?.dismiss()
    }
    fun initTitle() {
        window.statusBarColor = getColorInDarkMode(R.color.common_bg_z1_color_light,R.color.common_bg_z1_color_dark)
        rl_title_bg.setBackgroundColor(getColorInDarkMode(R.color.common_bg_z1_color_light,R.color.common_bg_z1_color_dark))
        iv_back.setOnClickListener {
            finish()
        }
        tv_title.text = getString(R.string.pressure)
        iv_menu_icon.visibility = View.VISIBLE
        iv_menu_icon.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.vector_drawable_share
            )
        )
        iv_menu_icon.setOnClickListener {
            shareReport()
        }
    }

    fun shareReport(){
        var shareHeadView =
            LayoutInflater.from(this).inflate(R.layout.layout_share_head_view, null)
        var tvTime = shareHeadView.findViewById<TextView>(R.id.tv_time)
        tvTime.text = getExperienceStartTime(this@ReportDetailPressureActivity, mRecordId)
        var tvUserName = shareHeadView.findViewById<TextView>(R.id.tv_user_name)
        tvUserName.text = "${SettingManager.getInstance().socialUserName}'s"
        var shareFootView =
            LayoutInflater.from(this).inflate(R.layout.layout_product_share_view, null)
        var scrollView = getShareView() as NestedScrollView
        var llBg = getShareViewBg() as LinearLayout
        llBg.setBackgroundColor(getColorInDarkMode(R.color.common_share_bg_color_light,R.color.common_share_bg_color_dark))
        ShotShareUtil.shotScrollView(this, scrollView, shareHeadView, shareFootView)
        llBg.setBackgroundColor(getColorInDarkMode(R.color.common_bg2_color_light,R.color.common_bg2_color_dark))

    }
    fun initData() {
        if (mRecordId == null || mRecordId == 0L || mRecordId == -1L) {
            return
        }
        userLessonRecordDao = UserLessonRecordDao(this)
        var userLessonRecord =
            userLessonRecordDao?.findRecordById(SettingManager.getInstance().userId, mRecordId!!)
        if (userLessonRecord != null) {
            meditationReportDataAnalyzed =
                userLessonRecordDao?.getReportDataFromFile(userLessonRecord)
        }
    }

    fun initLineChart() {
        var pressureLine = meditationReportDataAnalyzed?.pressureRec
        if (meditationReportDataAnalyzed != null && meditationReportDataAnalyzed!!.pressureAvg != null){
            chart_pressure.setAverage(meditationReportDataAnalyzed!!.pressureAvg.toInt())
        }
        chart_pressure.setAverageLineColor(getColorInDarkMode(R.color.common_line_hard_color_light,R.color.common_line_hard_color_dark))
        chart_pressure.setData(pressureLine)
    }


    fun initLastAverageChart() {
        pressure_average_chart.setTipTextColor(getColorInDarkMode(R.color.common_text_lv1_base_color_light,R.color.common_text_lv1_base_color_dark))
        pressure_average_chart.setTipBg(getColorInDarkMode(R.color.common_bg_z2_color_light,R.color.common_bg_z2_color_dark))
        if (mRecordId == -2L && meditationReportDataAnalyzed != null) {
            pressure_average_chart.setValues(listOf(meditationReportDataAnalyzed!!.pressureAvg.toInt()))
        } else {
            if (meditationReportDataAnalyzed == null){
                return
            }
            var lastTimesEffectiveRecord = userLessonRecordDao?.findLastEffectiveRecordById(
                SettingManager.getInstance().userId,
                mRecordId!!,
                7
            )
            var lastTimesEffectiveHRAverage = lastTimesEffectiveRecord?.map {
                userLessonRecordDao?.getReportDataFromFile(it)!!.pressureAvg!!.toInt()
            }
            if (lastTimesEffectiveHRAverage != null && lastTimesEffectiveHRAverage.size > 2) {
                lastAverage =
                    lastTimesEffectiveHRAverage.subList(1, lastTimesEffectiveHRAverage.size)
                        .average()
                lastValue = lastTimesEffectiveHRAverage[1]
            }
            if (lastTimesEffectiveHRAverage != null) {
                pressure_average_chart.setValues(lastTimesEffectiveHRAverage!!.asReversed())
            }
        }
    }

    fun isShareCondition(): Boolean {
        if (meditationReportDataAnalyzed == null) {
            return false
        }
        var shareJson = SettingManager.getInstance().remoteConfigShareCondition
        var shareCondition = Gson().fromJson<FirebaseRemoteConfigShare>(
            shareJson,
            FirebaseRemoteConfigShare::class.java
        )
        var isAverageLarge = false
        var isDataEffective = false
        var isValueLarge = false
        var isValueSmall = false
        var dataEffectiveCount = 0
        isAverageLarge = meditationReportDataAnalyzed!!.pressureAvg < lastAverage
        isValueLarge = meditationReportDataAnalyzed!!.pressureAvg < lastValue
        for (data in meditationReportDataAnalyzed!!.pressureRec) {
            if (data > 0) {
                dataEffectiveCount++
            }
        }
        isValueSmall = meditationReportDataAnalyzed!!.pressureAvg<shareCondition.pressure_page_share_conditions.max_pressure_value
        isDataEffective =
            dataEffectiveCount * 1.0 / meditationReportDataAnalyzed!!.pressureRec.size > shareCondition.pressure_page_share_conditions.min_data_valid_ratio
        return isDataEffective && isValueLarge && isAverageLarge && isValueSmall
    }

    open fun getShareView(): View {
        return scroll_view
    }

    open fun getShareViewBg(): View {
        return ll_bg
    }
}
