package com.real.dono

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.real.dono.databinding.FragmentUpdateDotoBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit


class UpdateDoto : BottomSheetDialogFragment() {
    lateinit var updateDotoBinding: FragmentUpdateDotoBinding
    var selectedTime:String = ""
    var selectedDate: String= ""
    var datePicker = MaterialDatePicker<Long>()
    var materialTimePicker = MaterialTimePicker()
    val database:FirebaseDatabase = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val myReference:DatabaseReference = database.reference.child("doto").child(uid.toString())
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        updateDotoBinding = FragmentUpdateDotoBinding.inflate(inflater, container, false)
        updateDotoBinding.updateEdit.setBackgroundResource(android.R.color.transparent)

        updateDotoBinding.updatesave.isEnabled = true
        updateDotoBinding.updatesave
            .setBackgroundResource(R.drawable.btnbackfullcolor)

        val updateMessage = arguments?.getString("message")
        val updateMessageEditable = Editable.Factory.getInstance().newEditable(updateMessage)
        val updateDate = arguments?.getString("formatDate")
        val inputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        val updateTime = arguments?.getString("formatTime")
        if (updateDate!! .isEmpty()){
            updateDotoBinding.selectdatetext.text = "Date"
     }else{
         val date = inputDateFormat.parse(updateDate)
         var dummyDate = outputDateFormat.format(date)
         updateDotoBinding.selectdatetext.text = dummyDate
         updateDotoBinding.selectdatetext.setTextColor(ContextCompat.getColor(requireContext(),R.color.savebtnbg))
     }
        if(updateTime !!.isEmpty()){
            updateDotoBinding.selectTimeTxt.text = "Time"
        }else{
            updateDotoBinding.selectTimeTxt.text = updateTime
            updateDotoBinding.selectTimeTxt.setTextColor(ContextCompat.getColor(requireContext(),R.color.savebtnbg))
        }
        updateDotoBinding.updateEdit.text = updateMessageEditable
        updateDotoBinding.updateEdit.requestFocus()
        updateDotoBinding.updateEdit.addTextChangedListener(textWatcher)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog!!.window!!.attributes.windowAnimations = R.style.dialogAnimation
        updateDotoBinding.updatesave.setOnClickListener {
            updateDataToDatabase()
        }
        updateDotoBinding.selectDateBtn.setOnClickListener {
            if(updateDate!!.isNotEmpty()){
                val updateFormatDate = arguments?.getString("formatDate").toString()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy")
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = dateFormat.parse(updateFormatDate)
                val calendar = Calendar.getInstance()
                calendar.time = date
                datePicker = MaterialDatePicker.Builder.datePicker()
                    .setSelection(calendar.timeInMillis)
                    .build()
                datePicker.addOnPositiveButtonClickListener { it->
                    val dummyFormat = SimpleDateFormat("EEE,dd MMM")
                    selectedDate = dateFormat.format(Date(it))
                    val originalDate = dateFormat.parse(selectedDate)
                    var dummyDate = dummyFormat.format(originalDate)
                    updateDotoBinding.selectdatetext.setText(dummyDate.toString())
                    updateDotoBinding.selectdatetext.setTextColor(ContextCompat.getColor(requireContext(),R.color.savebtnbg))
                }
                datePicker.show(parentFragmentManager,"materialDatePicker")
            }else{
                 datePicker= MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select date")
                     .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build()
                datePicker.addOnPositiveButtonClickListener { selection->
                    val dummyFormat = SimpleDateFormat("EEE,dd MMM")
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy")
                    selectedDate = dateFormat.format(Date(selection))
                    val originalDate = dateFormat.parse(selectedDate)
                    var dummyDate = dummyFormat.format(originalDate)
                    updateDotoBinding.selectdatetext.setText(dummyDate.toString())
                    updateDotoBinding.selectdatetext.setTextColor(ContextCompat.getColor(requireContext(),R.color.savebtnbg))

                }
                datePicker.show(parentFragmentManager,"NewtaskFragment")
            }
            datePicker.addOnDismissListener {
                updateDotoBinding.updateEdit.requestFocus()
                dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
        }
        updateDotoBinding.selectTimeBtn.setOnClickListener {
            if (updateTime!!.isNotEmpty()){
                val updateFormatTime = arguments?.getString("formatTime").toString()
                val timeParts = updateFormatTime.split(" ")
                val time = timeParts[0]
                val hourMinute = time.split(":")
                val hour = hourMinute[0].toInt()
                val minute = hourMinute[1].toInt()
                val amPm = timeParts[1]
                val hourOfDay = if (amPm.equals("PM", ignoreCase = true) && hour != 12) {
                    hour + 12
                } else if (amPm.equals("AM", ignoreCase = true) && hour == 12) {
                    0
                } else {
                    hour
                }
                 materialTimePicker = MaterialTimePicker.Builder()
                    .setTitleText("Update time")
                    .setHour(hourOfDay)
                    .setMinute(minute)
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .build()
                materialTimePicker.show(parentFragmentManager,"UpdateDotoFragment")
                materialTimePicker.addOnPositiveButtonClickListener { it ->
                    val pickedHour:Int = materialTimePicker.hour
                    val pickedMinute:Int = materialTimePicker.minute
                    val pickedCalendar = Calendar.getInstance()
                    pickedCalendar.set(Calendar.HOUR_OF_DAY,pickedHour)
                    pickedCalendar.set(Calendar.MINUTE,pickedMinute)
                    val timeFormat = SimpleDateFormat("HH:mm a",Locale.getDefault())
                    selectedTime = timeFormat.format(pickedCalendar.time)
                    selectedTime = when {
                        pickedHour > 12 -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour - 12}:0${materialTimePicker.minute} PM"
                            } else {
                                "${materialTimePicker.hour - 12}:${materialTimePicker.minute} PM"
                            }
                        }
                        pickedHour == 12 -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour}:0${materialTimePicker.minute} PM"
                            } else {
                                "${materialTimePicker.hour}:${materialTimePicker.minute} PM"
                            }
                        }
                        pickedHour == 0 -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour + 12}:0${materialTimePicker.minute} AM"
                            } else {
                                "${materialTimePicker.hour + 12}:${materialTimePicker.minute} AM"
                            }
                        }
                        else -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour}:0${materialTimePicker.minute} AM"
                            } else {
                                "${materialTimePicker.hour}:${materialTimePicker.minute} AM"
                            }
                        }
                    }
                    updateDotoBinding.selectTimeTxt.setText(selectedTime.toString())
                    updateDotoBinding.selectTimeTxt.setTextColor(ContextCompat.getColor(requireContext(),R.color.savebtnbg))
            }
            }else{
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                 materialTimePicker = MaterialTimePicker.Builder()
                    .setTitleText("Enter time")
                    .setHour(currentHour)
                    .setMinute(currentMinute)
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .build()

                materialTimePicker.show(parentFragmentManager,"NewtaskFragment")

                materialTimePicker.addOnPositiveButtonClickListener { it ->
                    val pickedHour:Int = materialTimePicker.hour
                    val pickedMinute:Int = materialTimePicker.minute
                    val pickedCalendar = Calendar.getInstance()
                    pickedCalendar.set(Calendar.HOUR_OF_DAY,pickedHour)
                    pickedCalendar.set(Calendar.MINUTE,pickedMinute)
                    val timeFormat = SimpleDateFormat("HH:mm a",Locale.getDefault())
                    selectedTime = timeFormat.format(pickedCalendar.time)
                    selectedTime = when {
                        pickedHour > 12 -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour - 12}:0${materialTimePicker.minute} PM"
                            } else {
                                "${materialTimePicker.hour - 12}:${materialTimePicker.minute} PM"
                            }
                        }
                        pickedHour == 12 -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour}:0${materialTimePicker.minute} PM"
                            } else {
                                "${materialTimePicker.hour}:${materialTimePicker.minute} PM"
                            }
                        }
                        pickedHour == 0 -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour + 12}:0${materialTimePicker.minute} AM"
                            } else {
                                "${materialTimePicker.hour + 12}:${materialTimePicker.minute} AM"
                            }
                        }
                        else -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour}:0${materialTimePicker.minute} AM"
                            } else {
                                "${materialTimePicker.hour}:${materialTimePicker.minute} AM"
                            }
                        }
                    }
                    updateDotoBinding.selectTimeTxt.setText(selectedTime.toString())
                    updateDotoBinding.selectTimeTxt.setTextColor(ContextCompat.getColor(requireContext(),R.color.savebtnbg))
                }
            }
            materialTimePicker.addOnDismissListener {
                updateDotoBinding.updateEdit.requestFocus()
                dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
        }
        return updateDotoBinding.root
    }

    fun updateDataToDatabase(){
        var updatedMessage = updateDotoBinding.updateEdit.text.toString()
        val notificationTime = requireArguments().getLong("notificationTime")
        val bundleSelectedDate =  arguments?.getString("formatDate").toString()
        val bundleSelectedTime =arguments?.getString("formatTime").toString()


        val updatedTime = if (selectedTime.isEmpty()) {
            bundleSelectedTime
        } else {
            selectedTime
        }
        val updatedDate = if (bundleSelectedDate.isEmpty() && updatedTime.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy")
            dateFormat.format(Date())
        } else if (selectedDate.isNotEmpty()) {
            selectedDate
        } else {
            bundleSelectedDate
        }

        val updateNotificationTime = if(updatedTime.isNullOrEmpty() || updatedDate.isNullOrEmpty()){
            notificationTime
        }else{
            calculateNotificationTime(updatedDate, updatedTime, notificationTime)
        }
        var updatedId = arguments?.getString("id").toString()
        val userMap = mutableMapOf<String,Any>()
            userMap["id"] = updatedId
            userMap["message"] = updatedMessage
            userMap["date"] = updatedDate
            userMap["time"] = updatedTime
            userMap["notificationTime"] = updateNotificationTime
        myReference.child(updatedId).updateChildren(userMap)
        dismiss()
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                val intent = Intent(requireContext(),MainActivity::class.java)
                startActivity(intent)
            }
        }, 200)
    }
    fun calculateNotificationTime(date: String, time: String, originalNotificationTime: Long): Long {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val updatedDateTime = dateFormat.parse("$date $time")
        val originalDateTime = Date(originalNotificationTime)
        val duration = updatedDateTime.time - originalDateTime.time
        return originalNotificationTime + duration
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s:Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s.isNullOrEmpty()) {
                updateDotoBinding.updatesave.isEnabled = false
                updateDotoBinding.updatesave
                    .setBackgroundResource(R.drawable.savebtnbg)
            } else {
                updateDotoBinding.updatesave.isEnabled = true
                updateDotoBinding.updatesave
                    .setBackgroundResource(R.drawable.btnbackfullcolor)
            }
        }
    }
    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }
    override fun onStart() {
        super.onStart()
        val bottomSheetDialog = dialog as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        updateDotoBinding.updateEdit.requestFocus()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(updateDotoBinding.updateEdit, InputMethodManager.SHOW_IMPLICIT)
    }
}




