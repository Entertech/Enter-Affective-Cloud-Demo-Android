package cn.entertech.flowtimezh.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import cn.entertech.flowtimezh.R
import cn.entertech.flowtimezh.app.Constant.Companion.DEVICE_TYPE_CUSHION
import cn.entertech.flowtimezh.app.Constant.Companion.DEVICE_TYPE_ENTERTECH_VR
import cn.entertech.flowtimezh.app.Constant.Companion.DEVICE_TYPE_HEADBAND
import cn.entertech.flowtimezh.app.SettingManager
import cn.entertech.flowtimezh.utils.ConnectedDeviceHelper
import cn.entertech.flowtimezh.utils.isPermissionGranted
import cn.entertech.flowtimezh.utils.requestPermission
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_device_connect.*

class DeviceConnectActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_connect)
        initFullScreenDisplay()
        setStatusBarLight()
        initView()
    }

    fun initTitle(){
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    fun initView() {
        initTitle()
        btn_connect.setOnClickListener {
            if (!isPermissionGranted(this)) {
                requestPermission(this){
                    connectDevice()
                }
            } else {
                connectDevice()
            }
        }
        initTip()
    }

    fun initTip(){
        val deviceType = SettingManager.getInstance().deviceType
        when(deviceType){
            DEVICE_TYPE_CUSHION->{
                tv_device_name.text = getString(R.string.flowtime_cushion_name)
                tv_device_tip.text = getString(R.string.flowtime_cushion_tip)
            }
            DEVICE_TYPE_HEADBAND->{
                tv_device_name.text = getString(R.string.flowtime_headband_name)
                tv_device_tip.text = getString(R.string.flowtime_headband_tip)
            }
            DEVICE_TYPE_ENTERTECH_VR->{
                tv_device_name.text = getString(R.string.flowtime_vr_name)
                tv_device_tip.text = getString(R.string.flowtime_vr_tip)
            }
        }
    }

    fun connectDevice(){
        showLoading(getString(R.string.device_connecting))
        ConnectedDeviceHelper.scanNearDeviceAndConnect(SettingManager.getInstance().deviceType,
            fun(deviceType) {

            }, fun(e, deviceType) {
                toDisconnected("$e $deviceType")
            }, fun(mac, deviceType) {
                toConnected()
            }, fun(error, deviceType) {
                toDisconnected("$error  last  $deviceType")
            })
    }

    fun toConnected() {
        runOnUiThread {
            dismissLoading()
            showTipSuccess(getString(R.string.device_connect_success)){
                val intent = Intent(this@DeviceConnectActivity,MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
        }
    }

    fun toDisconnected(error: String) {
        runOnUiThread {
            dismissLoading()
            showTipError(error)
        }
    }
}