package com.hkapps.messagepro.ads.native_ads_event

import java.util.*

class NotifierFactoryApp private constructor() {
    fun getNotifier(eventCategory: Int): EventNotifierApp {
        val eventNotifier = findNotifier(eventCategory)
        if (eventNotifier != null) {
            return eventNotifier
        }
        val objEventNotifier = EventNotifierApp(eventCategory)
        mEventNotifiers!!.addElement(objEventNotifier)
        return objEventNotifier
    }

    companion object {
        private var mEventNotifiers: Vector<EventNotifierApp>? = null
        private var mNotifierFactoryInstance: NotifierFactoryApp? = null
        const val EVENT_NOTIFIER_AD_STATUS = 4
        val instance: NotifierFactoryApp?
            get() {
                if (mNotifierFactoryInstance == null) {
                    mNotifierFactoryInstance = NotifierFactoryApp()
                }
                return mNotifierFactoryInstance
            }

        private fun findNotifier(eventCategory: Int): EventNotifierApp? {
            var eventNotifierObject: EventNotifierApp? = null
            val length = mEventNotifiers!!.size
            for (index in 0 until length) {
                eventNotifierObject = mEventNotifiers!!.elementAt(index) as EventNotifierApp
                val category = eventNotifierObject.eventCategory
                if (eventCategory == category) {
                    return eventNotifierObject
                }
                eventNotifierObject = null
            }
            return eventNotifierObject
        }
    }

    init {
        mEventNotifiers = Vector()
    }
}
