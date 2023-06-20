package cn.entertech.flowtimezh.ui.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import cn.entertech.ble.multiple.MultipleBiomoduleBleManager
import cn.entertech.bleuisdk.ui.DeviceUIConfig
import cn.entertech.bleuisdk.ui.activity.DeviceManagerActivity

import cn.entertech.flowtimezh.R
import cn.entertech.flowtimezh.app.Application
import cn.entertech.flowtimezh.app.SettingManager
import cn.entertech.flowtimezh.database.ExperimentDao
import cn.entertech.flowtimezh.ui.activity.BaseActivity
import cn.entertech.flowtimezh.ui.activity.MeditationActivity
import cn.entertech.flowtimezh.ui.activity.PersonInfoActivity
import cn.entertech.flowtimezh.utils.ConnectedDevice
import cn.entertech.flowtimezh.utils.ConnectedDeviceHelper
import cn.entertech.flowtimezh.utils.ToastUtil
import cn.entertech.flowtimezh.utils.isPermissionGranted
import kotlinx.android.synthetic.main.fragment_hone.*

class HomeFragment : Fragment() {

    private var bleManager: MultipleBiomoduleBleManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_hone, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        initDeviceIcon()
        initView()
    }
    fun initView(){
        iv_device.setOnClickListener {
            if (ConnectedDeviceHelper.currentConnectedDeviceType() != ConnectedDevice.NONE){
                ToastUtil.toastShort(requireActivity(),getString(R.string.device_is_connected))
            }else{
                if (isPermissionGranted(requireActivity())){
                    connectDevice()
                }else{
                    connectDevice()
                }
            }
        }
        btn_start_meditation.setOnClickListener {
            if (ConnectedDeviceHelper.currentConnectedDeviceType() != ConnectedDevice.NONE){
                startActivity(Intent(activity, PersonInfoActivity::class.java))
            }else{
                Toast.makeText(activity,getString(R.string.device_disconnected),Toast.LENGTH_LONG).show()
            }
        }
        setExperimentName()
    }

    fun setExperimentName(){
        val experimentDao = ExperimentDao(Application.getInstance())
        val experimentModel = experimentDao.findExperimentBySelected()
        if (experimentModel != null){
            val experimentName = experimentModel.nameCn
            tv_experiment_name.text = experimentName
        }
    }
    fun toDisconnected(error:String?){
        requireActivity().runOnUiThread {
            (requireActivity() as BaseActivity).showTipError(error?:"unknow error")
        }
    }

    fun toConnected(){
        requireActivity().runOnUiThread {
            (requireActivity() as BaseActivity).showTipSuccess(getString(R.string.device_connect_success))
        }
    }

    fun connectDevice(){
        (requireActivity() as BaseActivity).showLoading(getString(R.string.connecting))
        ConnectedDeviceHelper.scanNearDeviceAndConnect(
            SettingManager.getInstance().deviceType,
            fun(deviceType) {

            }, fun(e, deviceType) {
                toDisconnected("$e $deviceType")
            }, fun(mac, deviceType) {
                toConnected()
            }, fun(error, deviceType) {
                toDisconnected("$error  last  $deviceType")
            })
    }
//    var connectListener = fun(str: String) {
//        activity?.runOnUiThread {
//            iv_device.setImageResource(R.mipmap.ic_battery)
//        }
//    }
//    var disconnectListener = fun(str: String) {
//        activity?.runOnUiThread {
//            iv_device.setImageResource(R.mipmap.ic_device_disconnect_color)
//        }
//    }
//    private fun initDeviceIcon() {
//        bleManager = DeviceUIConfig.getInstance(activity!!).managers[0]
//
//        bleManager?.addConnectListener(connectListener)
//        bleManager?.addDisConnectListener(disconnectListener)
//        if (bleManager!!.isConnected()) {
//            iv_device.setImageResource(R.mipmap.ic_battery)
//        } else {
//            iv_device.setImageResource(R.mipmap.ic_device_disconnect_color)
//        }
//    }

    override fun onDestroy() {
//        bleManager?.removeConnectListener(connectListener)
//        bleManager?.removeDisConnectListener(disconnectListener)
        super.onDestroy()

    }
}
