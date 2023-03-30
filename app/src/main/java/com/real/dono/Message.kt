package com.real.dono

import java.util.Date

data class ToDo(val id:String?=null,
                var message:String? = null,
                var date:String? = null,
                val time:String? = null,
                var notificationTime:Long = 0) {
}