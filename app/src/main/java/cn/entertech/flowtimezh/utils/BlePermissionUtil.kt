package cn.entertech.flowtimezh.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import cn.entertech.flowtimezh.app.Application
import com.tbruyelle.rxpermissions2.RxPermissions


fun requestPermission(activity:FragmentActivity,grantedCallback:()->Unit) {
    if (!isLocationEnable(activity)) {
        val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Application.getInstance().startActivity(intent)
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RxPermissions(activity).request(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        ).subscribe { granted ->
            if (!granted) {
                onPermissionNotGranted(activity)
            }else{
                grantedCallback.invoke()
            }
        }
    } else {
        RxPermissions(activity).request(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
            .subscribe { granted ->
                if (!granted) {
                    onPermissionNotGranted(activity)
                }else{
                    grantedCallback.invoke()
                }
            }
    }
}

fun isPermissionGranted(activity: FragmentActivity): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RxPermissions(activity).isGranted(Manifest.permission.BLUETOOTH_SCAN) && RxPermissions(
            activity
        ).isGranted(Manifest.permission.BLUETOOTH_CONNECT) && RxPermissions(activity).isGranted(
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        RxPermissions(activity).isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

fun isLocationEnable(activity:FragmentActivity): Boolean {
    val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    return gps || network
}

fun onPermissionNotGranted(activity: FragmentActivity) {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", activity.packageName, null)
    intent.data = uri
    activity.startActivity(intent)
}