package com.real.dono

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationUtils
import com.real.dono.databinding.ActivitySplashScreenBinding

class splashScreenActivity : AppCompatActivity() {
    lateinit var splashBinding:ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splashBinding = ActivitySplashScreenBinding.inflate(layoutInflater)
        val view = splashBinding.root
        setContentView(view)


        val alphaAnimation = AnimationUtils.loadAnimation(applicationContext,R.anim.splash)
        splashBinding.imageViewDonaicon.startAnimation(alphaAnimation)

        var handler = Handler(Looper.getMainLooper())

        handler.postDelayed(object :Runnable{
            override fun run() {
               val intent = Intent(this@splashScreenActivity,LoginPageActivity::class.java)
                startActivity(intent)
                finish()
            }

        },1000)
    }
}