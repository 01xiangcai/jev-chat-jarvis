package com.jev.probe.capture

/** 不记录到日志的会话身份；signature 只用于内存中的 stale-result 判断。 */
internal data class ConversationIdentity(
    val packageName: String,
    val title: String?,
    val signature: String
)

/**
 * 管理聊天页生命周期和分析代际。所有方法由服务主线程调用；网络任务只持有
 * 返回的 generation，并在回到主线程后通过 [isCurrent] 校验。
 */
internal class ConversationSession {
    enum class PageState { NONE, CHAT, LEAVING }

    data class EnterResult(val changed: Boolean, val leaveCancelled: Boolean)

    var pageState: PageState = PageState.NONE
        private set
    var conversation: ConversationIdentity? = null
        private set
    private var nextGeneration = 0L
    private var activeGeneration: Long? = null

    fun enter(identity: ConversationIdentity): EnterResult {
        val leaveCancelled = pageState == PageState.LEAVING
        val changed = conversation != identity
        pageState = PageState.CHAT
        if (changed) {
            conversation = identity
            activeGeneration = null
        }
        return EnterResult(changed, leaveCancelled)
    }

    fun scheduleLeave(): Boolean {
        if (pageState != PageState.CHAT) return false
        pageState = PageState.LEAVING
        return true
    }

    fun confirmLeave(): Boolean {
        if (pageState != PageState.LEAVING) return false
        endConversation()
        return true
    }

    fun endConversation(): Boolean {
        val hadConversation = pageState != PageState.NONE || conversation != null || activeGeneration != null
        pageState = PageState.NONE
        conversation = null
        activeGeneration = null
        return hadConversation
    }

    fun invalidate(): Long {
        activeGeneration = null
        return ++nextGeneration
    }

    fun beginAnalysis(identity: ConversationIdentity): Long? {
        if (pageState != PageState.CHAT || conversation != identity) return null
        val generation = ++nextGeneration
        activeGeneration = generation
        return generation
    }

    fun isCurrent(generation: Long, identity: ConversationIdentity): Boolean =
        pageState == PageState.CHAT && conversation == identity && activeGeneration == generation

    fun isConversationCurrent(identity: ConversationIdentity): Boolean =
        pageState == PageState.CHAT && conversation == identity

    fun finishAnalysis(generation: Long) {
        if (activeGeneration == generation) activeGeneration = null
    }
}

/** 未适配 App 仅保留手动 OCR 入口；启动器、系统界面和本应用设置页例外。 */
internal fun shouldShowManualOcrBubble(hasAdapter: Boolean, hideForSurface: Boolean): Boolean =
    !hasAdapter && !hideForSurface
