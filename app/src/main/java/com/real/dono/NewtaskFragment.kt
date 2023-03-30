package com.real.dono
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.core.widget.NestedScrollView
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.real.dono.databinding.FragmentNewtaskBinding
import com.real.dono.databinding.ItemTodoBinding
import java.text.SimpleDateFormat
import java.util.*


@Suppress("UNREACHABLE_CODE")
open class NewtaskFragment : BottomSheetDialogFragment() {
    lateinit var newtaskBinding:FragmentNewtaskBinding
    var selectedDate: String= ""
    var selectedTime:String = ""
    var notificationTime:Long = 0
    var message:String = ""
    lateinit var userAdapter : UserAdapter
    val database:FirebaseDatabase = FirebaseDatabase.getInstance()
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val myReference:DatabaseReference = database.reference.child("doto").child(uid.toString())
    @SuppressLint("ResourceAsColor", "ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        newtaskBinding = FragmentNewtaskBinding.inflate(inflater, container, false)

        userAdapter = UserAdapter(requireContext(), LinkedList(), LinearLayout(requireContext()))
        newtaskBinding.newTaskEditText.requestFocus()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        newtaskBinding.newTaskEditText.addTextChangedListener(textWatcher)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.attributes?.windowAnimations = R.style.dialogAnimation

        newtaskBinding.saveButton.setOnClickListener {
            addMessagetoDatabase()
        }

        newtaskBinding.selectDateBtn.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy")
                val dummyFormat = SimpleDateFormat("EEE,dd MMM")
                selectedDate = dateFormat.format(Date(selection))
                val originalDate = dateFormat.parse(selectedDate)
                val dummyDate = dummyFormat.format(originalDate)
                newtaskBinding.selectdatetext.text = dummyDate.toString()
                newtaskBinding.selectdatetext.setTextColor(ContextCompat.getColor(requireContext(), R.color.savebtnbg))
            }

            datePicker.show(parentFragmentManager, "NewtaskFragment")

            // Request focus on the EditText again after dismissing the date picker
            datePicker.addOnDismissListener {
                newtaskBinding.newTaskEditText.requestFocus()
                dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
        }
         newtaskBinding.selectTimeBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            var materialTimePicker = MaterialTimePicker.Builder()
                 .setTitleText("Enter time")
                 .setHour(currentHour)
                 .setMinute(currentMinute)

                 .setTimeFormat(TimeFormat.CLOCK_12H)
                 .build()
             materialTimePicker.show(parentFragmentManager,"NewtaskFragment")

            materialTimePicker.addOnPositiveButtonClickListener { it ->
                val pickedHour:Int = materialTimePicker.hour
                val pickedMinute:Int = materialTimePicker.minute
                val pickedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                pickedCalendar.set(Calendar.HOUR_OF_DAY,pickedHour)
                pickedCalendar.set(Calendar.MINUTE,pickedMinute)

                val timeFormat = SimpleDateFormat("hh:mm a",Locale.getDefault())


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
                // then update the preview TextView
                 newtaskBinding.selectTimeTxt.setText(selectedTime.toString())
                newtaskBinding.selectTimeTxt.setTextColor(ContextCompat.getColor(requireContext(),R.color.savebtnbg))
            }
             materialTimePicker.addOnDismissListener {
                 newtaskBinding.newTaskEditText.requestFocus()
                 dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
             }
        }
        return newtaskBinding.root
    }
    fun addMessagetoDatabase(){
        message = newtaskBinding.newTaskEditText.text.toString()
        var id:String = myReference.push().key.toString()
        val currentDate = if (selectedDate.isNullOrEmpty() && !selectedTime.isNullOrEmpty()) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy")
            dateFormat.format(Date())
        } else {
            selectedDate
        }
        val userDoto = ToDo(id,message,currentDate,selectedTime, notificationTime)
        myReference.child(id).setValue(userDoto)
        dismiss()
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                val intent = Intent(requireContext(),MainActivity::class.java)
                startActivity(intent)
            }
        }, 200)


    }
    override fun onResume() {
        super.onResume()
        newtaskBinding.newTaskEditText.requestFocus()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        updateSaveButtonState()
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            updateSaveButtonState()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun updateSaveButtonState() {
        if (!newtaskBinding.newTaskEditText.text.isNullOrEmpty() || !selectedDate.isNullOrEmpty() || !selectedTime.isNullOrEmpty()) {
            newtaskBinding.saveButton.isClickable = true
            newtaskBinding.saveButton.setBackgroundResource(R.drawable.btnbackfullcolor)
        } else {
            newtaskBinding.saveButton.isClickable = false
            newtaskBinding.saveButton.setBackgroundResource(R.drawable.savebtnbg)
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

   //content screen bottomsheet is important
    override fun onStart() {
        super.onStart()
        val bottomSheetDialog = dialog as BottomSheetDialog
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    }

}






