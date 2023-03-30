package com.real.dono

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.real.dono.databinding.ActivityLoginPageBinding

class LoginPageActivity : AppCompatActivity() {
    lateinit var loginBinding:ActivityLoginPageBinding
    val auth:FirebaseAuth = FirebaseAuth.getInstance()
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loginBinding = ActivityLoginPageBinding.inflate(layoutInflater)
        val view = loginBinding.root
        setContentView(view)

        registerActivityForGoogleSignin()

        loginBinding.buttonSignin.setOnClickListener {
            googleSignin()
           }

    }

    private fun googleSignin(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("239762821436-vi5tt4dhdamlmnipsdqnnfp82k99h1gk.apps.googleusercontent.com")
            .requestEmail().build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)
        signin()
    }

    private fun signin(){
       val signinIntent : Intent = googleSignInClient.signInIntent
        activityResultLauncher.launch(signinIntent)
    }

    private fun registerActivityForGoogleSignin(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts
            .StartActivityForResult(),
        ActivityResultCallback { result ->
            val resultCode = result.resultCode
            val data = result.data
            if(resultCode == RESULT_OK && data != null){
                val task:Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
                firebaseSigninwithGoogle(task)
        }
        })
    }

    private fun firebaseSigninwithGoogle(task:Task<GoogleSignInAccount>){
       try{
           val account:GoogleSignInAccount = task.getResult(ApiException::class.java)
           val intent = Intent(this@LoginPageActivity,MainActivity::class.java)
           startActivity(intent)
           finish()
           firebaseGoogleAccount(account)
       }catch (e:ApiException){
           Toast.makeText(applicationContext,  e.localizedMessage,Toast.LENGTH_SHORT).show()
       }
    }

    private fun firebaseGoogleAccount(account: GoogleSignInAccount){
        val authCredential = GoogleAuthProvider.getCredential(account.idToken,null)
        auth.signInWithCredential(authCredential).addOnCompleteListener { task ->
            if(task.isSuccessful){
            }else{
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        if(user != null){
            val intent = Intent(this@LoginPageActivity,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}