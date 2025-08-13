package com.hkapps.messagepro.ads.native_ads_event

import android.util.Log
import java.lang.Exception
import java.util.*

class EventNotifierApp(category: Int) {
    val eventCategory: Int
    private val _registeredListener: Vector<ListenerObject>
    fun registerListener(eventListener: EventListner, listenerPriority: Int) {
        try {
            if (_checkAlreadyRegistered(eventListener)) {
                return
            }
            val listenerObj = ListenerObject(eventListener, listenerPriority)
            val length = _registeredListener.size
            if (length == 0) {
                _registeredListener.addElement(listenerObj)
                return
            }
            for (index in 0 until length) {
                val tempObj1 = _registeredListener.elementAt(index)
                if (listenerPriority <= tempObj1.priority) {
                    _registeredListener.insertElementAt(listenerObj, index)
                    return
                }
            }
            _registeredListener.addElement(listenerObj)
        } catch (ignored: Exception) {
        }
    }

    fun eventNotify(eventType: Int, eventObject: Any?) {
        var eventState: Int
        try {
            val length = _registeredListener.size
            if (length == 0) {
                return
            }
            for (index in _registeredListener.indices.reversed()) {
                val listenerObj = _registeredListener.elementAt(index).iEventListener
                Log.e("Event: ", "eventNotify : "+eventType)
                eventState = listenerObj.eventNotify(eventType, eventObject)
                if (eventState == EventConstants.EVENT_CONSUMED) {
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Event: ", "Exception : "+e.printStackTrace())
        }
    }

    private fun _checkAlreadyRegistered(eventListener: EventListner): Boolean {
        try {
            val length = _registeredListener.size
            for (index in 0 until length) {
                if (eventListener == (_registeredListener.elementAt(index) as ListenerObject)
                        .iEventListener
                ) {
                    return true
                }
            }
        } catch (e: Exception) {
        }
        return false
    }

    class ListenerObject(val iEventListener: EventListner, val priority: Int)

    init {
        _registeredListener = Vector()
        eventCategory = category
    }
}
