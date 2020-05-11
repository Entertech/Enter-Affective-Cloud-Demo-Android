package cn.entertech.affectiveclouddemo.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import cn.entertech.affectiveclouddemo.R
import kotlinx.android.synthetic.main.card_heart_rate.view.*

class MeditationHeartView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attributeSet, defStyleAttr) {
    var minHeart: Int = 0
    var maxHeart: Int = 0
    var isFirstLoad = true
    var mSelfView: View = LayoutInflater.from(context).inflate(R.layout.card_heart_rate, null)

    init {
        var layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        mSelfView.layoutParams = layoutParams
        initView()
        addView(mSelfView)
    }

    fun initView() {
    }

    fun setHeartValue(heartRate: Int?) {
        heart_rate.setHeartValue(heartRate)
    }

    fun setHRV(hrv:Double?){
        realtime_hrv.appendHrv(hrv)
    }

    fun showHRLoadingCover() {
        heart_rate.showLoading()
    }

    fun hindHRLoadingCover() {
        heart_rate.hideLoading()
    }

    fun showHRSampleData() {
        heart_rate.showDisconnectTip()
    }

    fun showErrorMessage(error:String){
        heart_rate.showErrorMessage(error)
    }

    fun hideHRSampleData() {
        heart_rate.hideDisconnectTip()
    }

    fun showHRVLoadingCover() {
        realtime_hrv.showLoadingCover()
    }

    fun hindHRVLoadingCover() {
        realtime_hrv.hindLoadingCover()
    }

    fun showHRVSampleData() {
        realtime_hrv.showSampleData()
    }

    fun hideHRVSampleData() {
        realtime_hrv.hideSampleData()
    }
}