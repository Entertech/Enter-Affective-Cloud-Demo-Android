package cn.entertech.flowtimezh.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import cn.entertech.affectivecloudsdk.utils.MD5Encode
import cn.entertech.flowtimezh.R
import cn.entertech.flowtimezh.app.Application
import cn.entertech.flowtimezh.app.SettingManager
import cn.entertech.flowtimezh.entity.AuthEntity
import cn.entertech.flowtimezh.entity.UserNameEntity
import cn.entertech.flowtimezh.server.presenter.AuthPresenter
import cn.entertech.flowtimezh.server.view.AuthView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_experiment_setting.et_app_key
import kotlinx.android.synthetic.main.activity_experiment_setting.et_app_secret
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : BaseActivity() {
    private var authPresenter: AuthPresenter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initFullScreenDisplay()
        setStatusBarLight()
        initPresenter()
        initView()
    }

    fun initView(){
        btn_login.setOnClickListener {
            auth()
        }
        var oldAppKey = SettingManager.getInstance().appKey
        var oldAppSecret = SettingManager.getInstance().appSecret
        oldAppKey="9cd5f596-69bf-11ea-a76d-0242ac120002"
        oldAppSecret="861804d00467aa6cb002b31ddeb4b55f"
        if (oldAppKey != "" && oldAppSecret != "") {
            et_app_key.setText(oldAppKey)
            et_app_secret.setText(oldAppSecret)
            auth()
        }
    }

    fun initPresenter() {
        var authView = object : AuthView {
            override fun onSuccess(authEntity: AuthEntity?) {
                dismissLoading()
                SettingManager.getInstance().token = authEntity?.token
                startActivity(
                    Intent(
                        this@LoginActivity,
                        DeviceSelectActivity::class.java
                    )
                )
                overridePendingTransition(R.anim.anim_bottom_in,R.anim.anim_bottom_silent);
            }

            override fun onError(error: String) {
                dismissLoading()
                Toast.makeText(this@LoginActivity, error, Toast.LENGTH_SHORT).show()
            }

        }
        authPresenter = AuthPresenter(Application.getInstance())
        authPresenter?.onCreate()
        authPresenter?.attachView(authView)
    }


    private fun auth() {
        if (isEditTextNull()) {
            Toast.makeText(this, "请输入正确的app key 或 app secret!", Toast.LENGTH_SHORT).show()
        } else {
            var appKey = et_app_key.text.toString()
            var appSecret = et_app_secret.text.toString()
            var timestamp = "${System.currentTimeMillis()}"
            var md5Params =
                "app_key=$appKey&app_secret=$appSecret&timestamp=$timestamp&user_id=test"
            var sign = MD5Encode(md5Params).toUpperCase()
            var userNameEntity = UserNameEntity()
            userNameEntity.app_key = appKey
            userNameEntity.sign = sign
            userNameEntity.timestamp = timestamp
            userNameEntity.user_id = "test"
            var gson = Gson().toJson(userNameEntity)
            var userName = gson
            var password = sign
            SettingManager.getInstance().appKey = appKey
            SettingManager.getInstance().appSecret = appSecret
            showLoading()
            authPresenter?.auth(userName, password)
        }
    }

    private fun isEditTextNull(): Boolean {
        return (et_app_key.text == null || et_app_key.text.toString() == "" || et_app_secret.text == null || et_app_secret.text.toString() == "")
    }

    override fun onDestroy() {
        authPresenter?.onStop()
        super.onDestroy()
    }
}