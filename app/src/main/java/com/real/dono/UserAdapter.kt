package com.real.dono

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.ClipData.Item
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.real.dono.databinding.ItemTodoBinding
import kotlinx.coroutines.handleCoroutineException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class UserAdapter(
    val context: Context,
    val userDodo: LinkedList<ToDo>,
    var linearLayout: LinearLayout
    ):RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    inner class UserViewHolder(val adapterBinding:ItemTodoBinding) :RecyclerView.ViewHolder(adapterBinding.root)
    private val database:FirebaseDatabase = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    var myReference: DatabaseReference? = null
    var auth:FirebaseAuth = FirebaseAuth.getInstance()
    var dummyDate:String = ""
    init {
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.let { user ->
                myReference = database.reference.child("doto").child(user.uid)
            } ?: run {
                myReference = null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return UserViewHolder(binding)

    }

    override fun onBindViewHolder(holder: UserViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val currentItem = userDodo[position]
        var dummyTime = currentItem.time
        holder.adapterBinding.dotoMessage.text = currentItem.message

        if (currentItem.date.isNullOrEmpty() && currentItem.time.isNullOrEmpty()) {
            val layoutParams = holder.adapterBinding.dotoMessage.layoutParams as RelativeLayout.LayoutParams
            layoutParams.setMargins(8.dpToPx(), 20.dpToPx(), 22.dpToPx(), 22.dpToPx())
            holder.adapterBinding.dotoMessage.layoutParams = layoutParams
            holder.adapterBinding.dateTimeText.isVisible = false
        }
        else {
            val layoutParams = holder.adapterBinding.dotoMessage.layoutParams as RelativeLayout.LayoutParams
            layoutParams.setMargins(8.dpToPx(), 14.dpToPx(), 22.dpToPx(), 2.dpToPx())
            holder.adapterBinding.dateTimeText.visibility = View.VISIBLE

            if (!currentItem.date.isNullOrEmpty()) {
                val inputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val outputDateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                val date = inputDateFormat.parse(currentItem.date)

                if (date != null) {
                    val currentDate = inputDateFormat.parse(inputDateFormat.format(Date()))  // Get the current date in the same format as the selected date
                    val selectedDate = inputDateFormat.parse(inputDateFormat.format(date))  // Get the selected date in the same format as the current date

                    val isNextDay = (selectedDate.time - currentDate.time) / (24 * 60 * 60 * 1000) == 1L

                    when {
                        selectedDate.before(currentDate) -> {
                            val diffInMillis = currentDate.time - selectedDate.time
                            val diffInYears = TimeUnit.MILLISECONDS.toDays(diffInMillis) / 365  // Convert the difference to years

                            when {
                                diffInYears > 1 -> dummyDate = "$diffInYears years ago"
                                diffInYears == 1L -> dummyDate = "1 year ago"
                                else -> {
                                    val diffInWeeks = TimeUnit.MILLISECONDS.toDays(diffInMillis) / 7  // Convert the difference to weeks
                                    when {
                                        diffInWeeks > 1 -> dummyDate = "$diffInWeeks weeks ago"
                                        diffInWeeks == 1L -> dummyDate = "1 week ago"
                                        diffInWeeks == 0L -> {
                                            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)  // Convert the difference to days
                                            when {
                                                diffInDays > 1 -> dummyDate = "$diffInDays days ago"
                                                diffInDays == 1L -> dummyDate = "Yesterday"
                                                diffInDays == 0L -> dummyDate = "Today"
                                            }
                                        }
                                        else -> dummyDate = outputDateFormat.format(date)  // If the date is more than a week ago, return the formatted date string
                                    }
                                }
                            }
                        }
                        selectedDate.after(currentDate) -> {
                            if (isNextDay) {
                                dummyDate = "Tomorrow"
                            } else {
                                dummyDate = outputDateFormat.format(date)
                            }
                        }
                        else -> dummyDate = "Today"
                    }
                }
                holder.adapterBinding.dateTimeText.text = if (currentItem.time.isNullOrEmpty()) {
                    dummyDate
                } else {
                    if (dummyDate == "Tomorrow") {
                        "Tomorrow at $dummyTime"
                    } else {
                        "$dummyDate at $dummyTime"
                    }
                }
            } else if (!currentItem.time.isNullOrEmpty()) {
                holder.adapterBinding.dateTimeText.text = dummyTime
            }
        }
        holder.itemView.setOnClickListener {
            val activity = holder.itemView.context as AppCompatActivity
            val bundle = Bundle()
            bundle.putString("message",currentItem.message)
            bundle.putString("id",currentItem.id)
            bundle.putString("formatDate",currentItem.date)
            bundle.putString("formatTime",currentItem.time)
            bundle.putLong("notificationTime",currentItem.notificationTime)
            val updateDoto = UpdateDoto()
            updateDoto.arguments = bundle
            updateDoto.show(activity.supportFragmentManager,updateDoto.tag)

        }


        holder.adapterBinding.paddingCheck.setOnClickListener {
            holder.adapterBinding.checkImage.setImageResource(R.drawable.checkedfill)
            holder.adapterBinding.dotoMessage.paintFlags = holder.adapterBinding.dotoMessage.paintFlags or
                    Paint.STRIKE_THRU_TEXT_FLAG
            val slideUpAnimation = AnimationUtils.loadAnimation(context,R.anim.slide_up)
            holder.adapterBinding.cardview.startAnimation(slideUpAnimation)
            Handler(Looper.getMainLooper()).postDelayed({
                deleteItem(position ,deleteFromDatabase = true,holder.adapterBinding)
                holder.adapterBinding.checkImage.setImageResource(R.drawable.rectangle_5)
                holder.adapterBinding.dotoMessage.paintFlags = 0
                                                        }, 500)

        }
    }

    override fun getItemCount(): Int {
        return userDodo.size
    }

    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    fun deleteItem(position: Int, deleteFromDatabase: Boolean, adapterBinding: ItemTodoBinding) {
        val deletedItem = userDodo.getOrNull(position)
        if (deletedItem == null) {
            return
        }

        userDodo.removeAt(position)
        notifyItemRemoved(position)

        val snackbar = Snackbar.make(linearLayout, "Task is Completed", Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo") {
            userDodo.add(position, deletedItem)
            notifyItemInserted(position)
            updateDatabase(deletedItem,  false)
        }

        if (deleteFromDatabase) {
            deletedItem.id?.let { myReference!!.child(it).removeValue() }
            (context as MainActivity).onItemDeleted(deletedItem, true)
        }

        snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (event != DISMISS_EVENT_ACTION && deleteFromDatabase) {
                    updateDatabase(deletedItem,  true)
                }
            }
        })
        snackbar.show()
    }

    fun updateDatabase(deletedItem: ToDo?, deleteFromDatabase: Boolean) {
        deletedItem?.let {
            val itemId = it.id ?: return
            if (deleteFromDatabase) {
                myReference?.child(itemId)?.removeValue()
                Log.d("TAG", "Item deleted from database")
            } else {
                myReference?.child(itemId)?.setValue(it)
                Log.d("TAG", "Item added to database")
            }
        }
    }
    }