package com.jev.probe.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSessionTest {
    private fun conversation(name: String, signature: String = "same") =
        ConversationIdentity("com.example.chat", name, signature)

    @Test
    fun `离开聊天后旧分析结果失效`() {
        val session = ConversationSession()
        val chatA = conversation("A")
        session.enter(chatA)
        val generation = requireNotNull(session.beginAnalysis(chatA))

        assertTrue(session.scheduleLeave())
        assertTrue(session.confirmLeave())
        session.invalidate()

        assertFalse(session.isCurrent(generation, chatA))
    }

    @Test
    fun `切换会话后仅新会话分析结果可用`() {
        val session = ConversationSession()
        val chatA = conversation("A")
        val chatB = conversation("B")
        session.enter(chatA)
        val generationA = requireNotNull(session.beginAnalysis(chatA))

        assertTrue(session.enter(chatB).changed)
        session.invalidate()
        val generationB = requireNotNull(session.beginAnalysis(chatB))

        assertFalse(session.isCurrent(generationA, chatA))
        assertTrue(session.isCurrent(generationB, chatB))
    }

    @Test
    fun `短暂离开后重回聊天会取消离开`() {
        val session = ConversationSession()
        val chatA = conversation("A")
        session.enter(chatA)

        assertTrue(session.scheduleLeave())
        val entered = session.enter(chatA)

        assertTrue(entered.leaveCancelled)
        assertFalse(session.confirmLeave())
    }

    @Test
    fun `确认离开会清空当前会话`() {
        val session = ConversationSession()
        val chatA = conversation("A")
        session.enter(chatA)
        session.scheduleLeave()

        assertTrue(session.confirmLeave())
        assertTrue(session.conversation == null)
    }

    @Test
    fun `旧回复结果不能在新会话中生效`() {
        val session = ConversationSession()
        val chatA = conversation("A", "message-a")
        val chatB = conversation("B", "message-b")
        session.enter(chatA)
        val generationA = requireNotNull(session.beginAnalysis(chatA))

        session.enter(chatB)
        session.invalidate()

        assertFalse(session.isCurrent(generationA, chatA))
    }

    @Test
    fun `未知 App 仍保留手动 OCR 气泡`() {
        assertTrue(shouldShowManualOcrBubble(hasAdapter = false, hideForSurface = false))
        assertFalse(shouldShowManualOcrBubble(hasAdapter = true, hideForSurface = false))
        assertFalse(shouldShowManualOcrBubble(hasAdapter = false, hideForSurface = true))
    }
}
