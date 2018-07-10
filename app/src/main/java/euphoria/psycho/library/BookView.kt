package euphoria.psycho.library

import android.content.Context
import android.graphics.Point
import android.os.Parcel
import android.os.Parcelable
import android.view.MotionEvent
import android.view.View

class BookView(context: Context) : View(context) {

    val mScreenWidth: Int
    val mScreenHeight: Int

    init {
        mScreenWidth = resources.displayMetrics.widthPixels
        mScreenHeight = resources.displayMetrics.heightPixels

    }

    var et = 0L
    var dx = 0
    var dy = 0
    var touch = Point()
    var actionDownX = 0
    var actionDownY = 0
    var center = false

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                et = System.currentTimeMillis()
                dx = event.getX().toInt()
                dy = event.getY().toInt()
                touch.x = dx;
                touch.y = dy
                actionDownX = dx
                actionDownY = dy
                if (actionDownX >= mScreenWidth / 3 && actionDownX <= mScreenWidth * 2 / 3
                        && actionDownY >= mScreenHeight / 3 && actionDownY <= mScreenHeight * 2 / 3) {
                    center = true
                } else {
                    center = false
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}