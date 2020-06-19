package cn.entertech.flowtimezh.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import cn.entertech.flowtimezh.R
import cn.entertech.flowtimezh.app.Constant.Companion.EXTRA_IS_FROM_REPORT
import cn.entertech.flowtimezh.app.Constant.Companion.EXTRA_MEDITATION_ID
import cn.entertech.flowtimezh.database.MeditationLabelsDao
import cn.entertech.flowtimezh.ui.adapter.MeditationLabelsListAdapter
import cn.entertech.flowtimezh.utils.TimeUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import kotlinx.android.synthetic.main.activity_meditation_labels_commit.*
import kotlinx.android.synthetic.main.layout_common_title.*

class MeditationLabelsCommitActivity : BaseActivity() {
    private var isFromReport: Boolean = false
    private var meditationId: Long = -1L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meditation_labels_commit)
        initFullScreenDisplay()
        setStatusBarLight()
        initView()
    }

    fun initTitle() {
        tv_title.visibility = View.VISIBLE
        tv_back.visibility = View.VISIBLE
        tv_title.text = "数据片段和标签"
        ll_back.visibility = View.VISIBLE
        ll_back.setOnClickListener {
            finish()
        }
    }

    fun initView() {
        initTitle()
        meditationId = intent.getLongExtra(EXTRA_MEDITATION_ID, -1L)
        isFromReport = intent.getBooleanExtra(EXTRA_IS_FROM_REPORT, false)
        if (isFromReport) {
            btn_commit.visibility = View.INVISIBLE
        } else {
            btn_commit.visibility = View.VISIBLE
        }
        var meditationLabelsDao = MeditationLabelsDao(this)
        var meditationLabels = meditationLabelsDao.findByMeditationId(meditationId)
        if (meditationLabels == null || meditationLabels.isEmpty()) {
            btn_commit.visibility = View.GONE
            return
        }
        var adapter = MeditationLabelsListAdapter(meditationLabels)
        adapter!!.onItemChildClickListener =
            BaseQuickAdapter.OnItemChildClickListener { adapter, view, position ->
                var duration = if (meditationLabels[position].startTime > meditationLabels[position].meditationStartTime) {
                        "${TimeUtils.getFormatTime(meditationLabels[position].startTime - meditationLabels[position].meditationStartTime, "mm:ss")}" +
                                "-${TimeUtils.getFormatTime(
                                    meditationLabels[position].endTime - meditationLabels[position].meditationStartTime,
                                    "mm:ss"
                                )}"
                    } else {
                        "${TimeUtils.getFormatTime(
                            meditationLabels[position].startTime,
                            "mm:ss"
                        )}" +
                                "-${TimeUtils.getFormatTime(
                                    meditationLabels[position].endTime,
                                    "mm:ss"
                                )}"
                    }
                var intent = Intent(
                    this@MeditationLabelsCommitActivity,
                    MeditationDimListActivity::class.java
                )
                intent.putExtra("dimIds", meditationLabels[position].dimIds)
                intent.putExtra("duration", duration)
                intent.putExtra("meditationLabelId", meditationLabels[position].id)
                intent.putExtra(EXTRA_IS_FROM_REPORT, isFromReport)
                startActivity(intent)
            }
        rv_list.adapter = adapter
        rv_list.layoutManager = LinearLayoutManager(this)
        btn_commit.setOnClickListener {
            showCommitDialog()
        }
    }


    fun showCommitDialog() {
        var dialog = AlertDialog.Builder(this)
            .setMessage(
                Html.fromHtml(
                    "<font color='${ContextCompat.getColor(
                        this,
                        R.color.colorDialogContent
                    )}'>结束前请确认各项数据标签无误</font>"
                )
            )
            .setPositiveButton(
                Html.fromHtml(
                    "<font color='${ContextCompat.getColor(
                        this,
                        R.color.colorDialogExit
                    )}'>确定</font>"
                )
            ) { dialog, which ->
                dialog.dismiss()
                startActivity(Intent(this, MeditationActivity::class.java))
                finish()
            }
            .setNegativeButton(
                Html.fromHtml(
                    "<font color='${ContextCompat.getColor(
                        this,
                        R.color.colorDialogCancel
                    )}'>取消</font>"
                )
            ) { dialog, which ->
                dialog.dismiss()
            }.create()
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        initView()
    }
}
