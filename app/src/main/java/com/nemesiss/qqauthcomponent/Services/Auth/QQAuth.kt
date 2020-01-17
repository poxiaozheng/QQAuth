package com.nemesiss.qqauthcomponent.Services.Auth

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.tencent.connect.UserInfo
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import org.json.JSONObject
import java.lang.Exception


class QQAuth(private val context: Context) {

    var QQAuthSDKInstance: Tencent? = null
        get() {
            if (field == null) {
                val appId = GetQQAppID(context)
                Log.d(LOGTAG, "QQAuth module is initializing using AppID: $appId")
                QQAuthSDKInstance = Tencent.createInstance(appId, context)
                val credentials = GetLoginCredentials()
                if(credentials[0] != "") {
                    QQAuthSDKInstance?.openId = credentials[0]
                }
                if (credentials[1] != "" && credentials[2] != "") {
                    QQAuthSDKInstance?.setAccessToken(credentials[1],credentials[2])
                }
            }
            return field
        }

    private val SPInstance = context.getSharedPreferences(SP_TAG, Context.MODE_PRIVATE)

    companion object {

        val LOGTAG = "ZZM_QQAUTH_MODULE"
        val SP_TAG = "ZZM_QQAUTH_MODULE_SP_TAG"

        fun GetQQSecret(context: Context): String? {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            return appInfo.metaData["QQ_SECRET"] as String?
        }

        fun GetQQAppID(context: Context): String? {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            return appInfo.metaData["QQ_APPID"] as String?
        }

        fun <TReturn, TError> AuthCallbackFactory(CurrentPromise: QQAuthCallbackPromise<TReturn, TError>): IUiListener {
            return object : IUiListener {
                override fun onCancel() {
                    CurrentPromise.cancelHandler?.invoke()
                }

                override fun onError(p0: UiError?) {
                    CurrentPromise.butHandler?.invoke(p0 as TError)
                }

                override fun onComplete(p0: Any?) {
                    CurrentPromise.thenHandler?.invoke(p0 as TReturn)
                }
            }
        }
    }

    fun GetLoginCredentials(): List<String> =
        arrayOf("OpenID", "AccessToken","Expired").map { SPInstance.getString(it, "") as String }

    fun PersistLoginCredentials(OpenID: String, AccessToken: String, Expired: String) {
        // Persist OpenID and AccessToken to SharedPreferences.
        val Editor = SPInstance.edit()
        arrayOf("OpenID", "AccessToken","Expired")
            .zip(arrayOf(OpenID, AccessToken,Expired))
            .forEach { Editor.putString(it.first, it.second) }
        QQAuthSDKInstance?.setAccessToken(AccessToken,Expired)
        QQAuthSDKInstance?.openId = OpenID
        if (!Editor.commit()) {
            throw IllegalStateException("Cannot persist login credentials")
        }
    }

    fun GetUserQQProfile(): QQAuthCallbackPromise<JSONObject, UiError> {
        return object : QQAuthCallbackPromise<JSONObject, UiError>(context, QQAuthSDKInstance!!) {
            override fun Go() {
                try {
                    super.Go()
                    UserInfo(context, QQAuthSDKInstance?.qqToken)
                        .getUserInfo(AuthCallbackFactory(this))
                } catch (ex: Exception) {
                    Log.w(LOGTAG, ex.message)
                }
            }
        }
    }

    fun TryLogin(ActivityCtx: QQLoginActivity): QQAuthCallbackPromise<JSONObject, UiError> {
        return object : QQAuthCallbackPromise<JSONObject, UiError>(context, QQAuthSDKInstance!!) {
            override fun Go() {
                val listener = AuthCallbackFactory(this)
                ActivityCtx.SetLoginCallbackListener(listener)
                QQAuthSDKInstance?.login(ActivityCtx,"all", listener)
            }
        }
    }

    abstract class QQAuthCallbackPromise<TReturnValue, TErrorValue>(
        private val context: Context,
        private val tencent: Tencent
    ) {

        var thenHandler: ((TReturnValue) -> Unit)? = null
        var butHandler: ((TErrorValue) -> Unit)? = null
        var cancelHandler: (() -> Unit)? = null

        fun Then(Handler: (TReturnValue) -> Unit): QQAuthCallbackPromise<TReturnValue, TErrorValue> {
            thenHandler = Handler
            return this
        }

        fun But(Handler: (TErrorValue) -> Unit): QQAuthCallbackPromise<TReturnValue, TErrorValue> {
            // NOT IMPLEMENTED
            butHandler = Handler
            return this
        }

        fun Canceled(Handler: () -> Unit): QQAuthCallbackPromise<TReturnValue, TErrorValue> {
            // NOT IMPLEMENTED
            cancelHandler = Handler
            return this
        }

        open fun Go() {
            if (!tencent.isSessionValid) {
                throw Exception("You must Login first!")
            }
        }
    }
}

abstract class QQLoginActivity :  AppCompatActivity() {
    abstract fun SetLoginCallbackListener(listener: IUiListener)
}