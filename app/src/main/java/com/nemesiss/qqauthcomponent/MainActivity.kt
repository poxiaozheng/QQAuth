package com.nemesiss.qqauthcomponent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.nemesiss.qqauthcomponent.Application.QQAuthApplication
import com.nemesiss.qqauthcomponent.Services.Auth.QQLoginActivity
import com.squareup.picasso.Picasso
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : QQLoginActivity() {
    private lateinit var LoginCbListener: IUiListener

    override fun SetLoginCallbackListener(listener: IUiListener) {
        LoginCbListener = listener
    }

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val qqAuth = (application as QQAuthApplication).qqAuth
        TryQQLoginBtn.setOnClickListener {
            qqAuth
                .TryLogin(this)
                .Then {
                    val OpenID = it.getString("openid")
                    val AccessToken = it.getString("access_token")
                    val Expires = it.getString("expires_in")
                    qqAuth.PersistLoginCredentials(OpenID, AccessToken, Expires)
                    Log.d(TAG, "$OpenID  $AccessToken  $Expires")
                }
                .But {
                    Log.w(TAG, "${it.errorMessage}  ${it.errorCode}")
                }
                .Canceled {
                    Log.w(TAG, "Login request is cancelled!")
                }
                .Go()
        }

        GetAvatarBtn.setOnClickListener {
            qqAuth
                .GetUserQQProfile()
                .Then {
                    val NickName = it.getString("nickname")
                    val AvatarURL = it.getString("figureurl_qq_2")
                    Log.d(TAG,"Nick Name is: $NickName, Avatar URL: $AvatarURL")
                    Picasso
                        .get()
                        .load(AvatarURL)
                        .into(Avatar)
                }
                .But {
                    Log.w(TAG,"Error occurred: ${it.errorMessage}")
                }
                .Go()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Tencent.onActivityResultData(requestCode, resultCode, data, LoginCbListener)
    }
}
