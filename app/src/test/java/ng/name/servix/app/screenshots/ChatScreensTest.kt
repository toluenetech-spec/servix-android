package ng.name.servix.app.screenshots

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ng.name.servix.app.ui.navigation.ServixBottomBar
import ng.name.servix.app.ui.navigation.ServixTab
import ng.name.servix.app.ui.chat.SendFailure
import ng.name.servix.app.ui.theme.WarmIvory
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ng.name.servix.app.data.model.ChatBookingRefDto
import ng.name.servix.app.data.model.ChatMessageDto
import ng.name.servix.app.data.model.ChatPersonDto
import ng.name.servix.app.data.model.ChatPersonProfileDto
import ng.name.servix.app.data.model.ChatThreadDto
import ng.name.servix.app.data.realtime.ChatConnection
import ng.name.servix.app.ui.chat.ChatTimeline
import ng.name.servix.app.ui.chat.ChatsScreen
import ng.name.servix.app.ui.chat.ChatsUiState
import ng.name.servix.app.ui.chat.ConversationScreen
import ng.name.servix.app.ui.chat.ConversationUiState

/**
 * Release 1.1 chat surfaces, rendered from the production composables with fake data — these are the
 * screens the Stage-1 report points at. No network: every DTO below is a literal.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = PIXEL_7_QUALIFIERS,
    instrumentedPackages = ["androidx.loader.content"],
)
class ChatScreensTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun installTestImageLoader() {
        TestAvatarImages.install(ApplicationProvider.getApplicationContext<Context>())
    }

    private val me = Fixtures.USER_ID

    private val ada = ChatPersonDto(
        id = "22222222-2222-4222-8222-222222222222",
        fullName = "Ada Nwosu",
        professionalProfile = ChatPersonProfileDto(slug = "ada-nwosu", title = "Brand designer"),
    )

    private val tunde = ChatPersonDto(
        id = "33333333-3333-4333-8333-333333333333",
        fullName = "Tunde Bakare",
        professionalProfile = ChatPersonProfileDto(slug = "tunde-bakare", title = "Logo and identity"),
    )

    private val merchant = ChatPersonDto(id = "44444444-4444-4444-8444-444444444444", fullName = "Chika Obi")

    private val threads = listOf(
        ChatThreadDto(
            id = "thread-1",
            person = ada,
            booking = ChatBookingRefDto(reference = "SX-4821", serviceTitle = "Brand identity package"),
            bookingId = Fixtures.BOOKING_ID,
            lastMessage = "I've shared the first draft of the logo.",
            lastMessageKind = "text",
            lastMessageAt = "2026-11-07T09:42:00.000Z",
            lastMessageSenderId = ada.id,
            unreadCount = 2,
            unread = true,
            online = true,
            otherReadAt = "2026-11-07T09:30:00.000Z",
            updatedAt = "2026-11-07T09:42:00.000Z",
        ),
        ChatThreadDto(
            id = "thread-2",
            person = tunde,
            booking = ChatBookingRefDto(reference = "SX-4790", serviceTitle = "Logo suite"),
            lastMessage = "Thanks, that works for me. See you Saturday.",
            lastMessageKind = "text",
            lastMessageAt = "2026-11-06T18:05:00.000Z",
            lastMessageSenderId = me,
            unreadCount = 0,
            otherReadAt = "2026-11-06T18:06:00.000Z",
            otherDeliveredAt = "2026-11-06T18:05:30.000Z",
            lastSeenAt = "2026-11-06T18:20:00.000Z",
            updatedAt = "2026-11-06T18:05:00.000Z",
        ),
        ChatThreadDto(
            id = "thread-3",
            person = merchant,
            connectionId = "connection-1",
            lastMessage = "Sent a photo",
            lastMessageKind = "image",
            lastMessageAt = "2026-11-05T12:00:00.000Z",
            lastMessageSenderId = merchant.id,
            unreadCount = 1,
            unread = true,
            lastSeenAt = "2026-11-05T12:10:00.000Z",
            updatedAt = "2026-11-05T12:00:00.000Z",
        ),
    )

    private fun chatsState() = ChatsUiState(
        loading = false,
        threads = threads,
        unreadTotal = threads.sumOf { it.unreadCount },
        connection = ChatConnection.LIVE,
        aiEnabled = true,
    )

    /** Minutes ago, so a render always reads "Today" instead of the CI run's yesterday. */
    private fun ago(minutes: Long): String =
        java.time.Instant.now().minusSeconds(minutes * 60).toString()

    private fun conversationMessages() = listOf(
        ChatMessageDto(
            id = "m1",
            body = "Hi Ada — I've approved the brief and paid the deposit.",
            senderId = me,
            createdAt = ago(30),
        ),
        ChatMessageDto(
            id = "m2",
            body = "Thank you. I'll send the first draft before noon.",
            senderId = ada.id,
            createdAt = ago(28),
        ),
        ChatMessageDto(
            id = "m3",
            body = "Sounds good.",
            senderId = me,
            createdAt = ago(12),
        ),
        ChatMessageDto(
            id = "m4",
            body = "I've shared the first draft of the logo. 📍 12 Bodija Road, Ibadan",
            senderId = ada.id,
            createdAt = ago(10),
        ),
        ChatMessageDto(
            id = "m5",
            body = "Looking now — the mark reads well at small sizes.",
            senderId = me,
            createdAt = ago(8),
        ),
    )

    private fun conversationState(): ConversationUiState {
        val messages = conversationMessages()
        return ConversationUiState(
            loading = false,
            messages = messages,
            rows = ChatTimeline.build(messages, me),
            input = "",
            hasMore = true,
            online = false,
            lastSeenAt = "2026-11-07T09:45:00.000Z",
            // Later than the last message, so the render shows the seen state, not just sent.
            otherReadAt = ago(5),
            otherDeliveredAt = ago(7),
            myUserId = me,
        )
    }

    @Test
    fun chatsList() = rule.capture("40_chats_list") {
        ChatsScreen(
            uiState = chatsState(),
            myUserId = me,
            onOpenThread = {},
            onOpenAi = {},
            onRetry = {},
        )
    }

    @Test
    fun conversation() = rule.capture("41_chat_conversation") {
        ConversationScreen(
            uiState = conversationState(),
            thread = threads.first(),
            onBack = {},
            onInput = {},
            onSend = {},
            onRetry = {},
            onLoadOlder = {},
            onOpenContactCard = {},
        )
    }

    /**
     * One frame, all four states a bubble can be in: seen (coral ✓✓), delivered (muted ✓✓),
     * sent (single ✓) and a failed optimistic row whose text names the 429 limit.
     */
    @Test
    fun ticksAndFailure() = rule.capture("44_chat_ticks_and_failure") {
        val messages = listOf(
            ChatMessageDto(
                id = "t1",
                body = "Seen: the brief is approved, thank you.",
                senderId = me,
                createdAt = ago(60),
            ),
            ChatMessageDto(
                id = "t2",
                body = "Delivered but not opened yet.",
                senderId = me,
                createdAt = ago(20),
            ),
            ChatMessageDto(
                id = "t3",
                body = "Only sent — still travelling.",
                senderId = me,
                createdAt = ago(5),
            ),
            ChatMessageDto(
                id = "",
                body = "This one hit the 30-a-minute limit.",
                senderId = me,
                createdAt = ago(3),
                clientId = "client-failed-1",
            ),
        )
        ConversationScreen(
            uiState = ConversationUiState(
                loading = false,
                messages = messages,
                rows = ChatTimeline.build(messages, me),
                online = true,
                // Read up to 09:30 (t1 only), delivered up to 09:45 (t1 + t2, not t3).
                otherReadAt = ago(30),
                otherDeliveredAt = ago(10),
                myUserId = me,
                failures = mapOf("client-failed-1" to SendFailure.RATE_LIMITED),
            ),
            thread = threads.first(),
            onBack = {},
            onInput = {},
            onSend = {},
            onRetry = {},
            onLoadOlder = {},
            onOpenContactCard = {},
        )
    }

    /** Shell-level evidence that Chats is the centre, prominent tab and carries the unread badge. */
    @Test
    fun shellChatsTab() = rule.capture("43_shell_chats_tab") {
        Scaffold(
            containerColor = WarmIvory,
            bottomBar = {
                ServixBottomBar(
                    tabs = listOf(
                        ServixTab("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
                        ServixTab("Explore", Icons.Filled.Search, Icons.Outlined.Search, "nav_tab_search"),
                        ServixTab(
                            label = "Chats",
                            selectedIcon = Icons.Filled.Chat,
                            unselectedIcon = Icons.Outlined.Chat,
                            testTag = "nav_tab_chats",
                            badgeCount = threads.sumOf { it.unreadCount },
                            prominent = true,
                        ),
                        ServixTab("Bookings", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_bookings"),
                        ServixTab("Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_tab_account"),
                    ),
                    selectedIndex = 2,
                    onSelect = {},
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                ChatsScreen(
                    uiState = chatsState(),
                    myUserId = me,
                    onOpenThread = {},
                    onOpenAi = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun typingConversation() = rule.capture("42_chat_typing") {
        ConversationScreen(
            uiState = conversationState().copy(otherTyping = true, input = "On my way"),
            thread = threads.first(),
            onBack = {},
            onInput = {},
            onSend = {},
            onRetry = {},
            onLoadOlder = {},
            onOpenContactCard = {},
        )
    }
}
