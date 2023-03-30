package com.real.dono

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.real.dono.databinding.ActivityMainBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private val uid = user?.uid
    private var myReference: DatabaseReference = database.reference.child("doto").child(uid.toString())

    private val userDodo = LinkedList<ToDo>()
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        overridePendingTransition(0, 0)

        retriveDataFromDatabase()

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val emailId = user.email
                val firstLetter = emailId?.substring(0, 1)
                mainBinding.profileText.text = firstLetter
                val uid = user.uid
                myReference = database.reference.child("doto").child(uid.toString())
                retriveDataFromDatabase()
            } else {
                mainBinding.profileText.text = ""
            }
        }

        val popupView = LayoutInflater.from(applicationContext).inflate(R.layout.pop_up, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val xoff = 24.dpToPx()
        val yoff = mainBinding.profileText.bottom + 7.dpToPx()
        val margin = -24.dpToPx()

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val width = popupView.measuredWidth
        val x = mainBinding.profileText.left - margin - width

        mainBinding.profileText.setOnClickListener {
            popupWindow.showAsDropDown(mainBinding.profileText, x, yoff)
        }

        val signoutButton = popupView.findViewById<ConstraintLayout>(R.id.signoutbtn)
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)

        signoutButton.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.signOut()
                    val intent = Intent(this@MainActivity, LoginPageActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

        mainBinding.addFloatingActionButton.setOnClickListener {
            val fragmentManager: FragmentManager = supportFragmentManager
            val fragmentNewtask = NewtaskFragment()
            fragmentNewtask.show(fragmentManager, "NewtaskFragment")
        }

        userAdapter = UserAdapter(this@MainActivity, userDodo, mainBinding.linearLayout)
        mainBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = userAdapter
        }
    }
    val notificationSet = mutableMapOf<String, Boolean>()

    fun retriveDataFromDatabase() {
        myReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userDodo.clear()
                for (eachDoto in snapshot.children) {
                    val userEachDoto = eachDoto.getValue(ToDo::class.java) ?: continue
                    userDodo.addFirst(userEachDoto)
                    val userId = userEachDoto.id ?: continue
                    val isNotificationSet = notificationSet[userId] ?: false
                    when {
                        !isNotificationSet && !userEachDoto.date.isNullOrEmpty() && !userEachDoto.time.isNullOrEmpty() -> {
                            userEachDoto.notificationTime = System.currentTimeMillis()
                            notificationSet[userId] = true
                            myReference.child(userId).setValue(userEachDoto)
                            setNotification(userEachDoto)
                        }
                        isNotificationSet && !userEachDoto.date.isNullOrEmpty() && !userEachDoto.time.isNullOrEmpty() -> {
                            // Cancel the existing notification and set a new one with the updated values
                            val pendingIntent = getPendingIntent(userId)
                            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alarmManager.cancel(pendingIntent)
                            setNotification(userEachDoto)
                        }
                        isNotificationSet && (userEachDoto.date.isNullOrEmpty() || userEachDoto.time.isNullOrEmpty()) -> {
                            // Cancel the existing notification
                            val pendingIntent = getPendingIntent(userId)
                            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alarmManager.cancel(pendingIntent)
                            notificationSet[userId] = false
                            myReference.child(userId).child("notificationTime").setValue(null)
                        }
                    }
                }
                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Database Error", "Database error: ${error.message}")
            }
        })
    }

    private fun getPendingIntent(id: String): PendingIntent {
        val intent = Intent(applicationContext, Notification::class.java)
        val title = "Todo Remainder"
        intent.putExtra(titleExtra,title)
        intent.putExtra(messageExtra,userDodo.first { it.id == id }.message)
        return PendingIntent.getBroadcast(
            applicationContext,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun onItemDeleted(deletedItem: ToDo, cancelNotification: Boolean) {
        if (cancelNotification) {
            setNotification(deletedItem, cancelNotification = true)
        }
    }
    fun setNotification(toDo: ToDo, cancelNotification: Boolean = false) {
        val id = toDo.id ?: return
        val date = toDo.date
        val time = toDo.time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val calendar = Calendar.getInstance()

        try {
            calendar.time = dateFormat.parse("$date $time")
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        val currentTimeMillis = System.currentTimeMillis()
        if (calendar.timeInMillis < currentTimeMillis) {
            return
        }
        if (cancelNotification) {
            val intent = Intent(applicationContext, Notification::class.java)
            intent.putExtra(titleExtra, "Todo Remainder")
            intent.putExtra(messageExtra, toDo.message)
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        } else {
            val intent = Intent(applicationContext, Notification::class.java)
            intent.putExtra(titleExtra, "Todo Remainder")
            intent.putExtra(messageExtra, toDo.message)
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

        }
    }
}