package ng.name.servix.app

import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import ng.name.servix.app.data.model.ChatThreadDto
import ng.name.servix.app.data.model.ChatMessageDto
import ng.name.servix.app.data.realtime.ChatEvent
import ng.name.servix.app.ui.liveness.runForegroundRefresh
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.launch
import ng.name.servix.app.data.model.FeaturesDto
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.network.ServixApiService
import ng.name.servix.app.data.realtime.ChatEventStream
import ng.name.servix.app.data.repository.ChatRepository
import ng.name.servix.app.data.repository.FeaturesRepository
import ng.name.servix.app.data.repository.SessionState
import ng.name.servix.app.ui.chat.ChatsViewModel
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionChatsTest {
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun loginRetryAndAccountSwitchReloadWithoutGuestError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val api = Proxy.newProxyInstance(ServixApiService::class.java.classLoader,
            arrayOf(ServixApiService::class.java)) { _, method, _ ->
            if (method.name == "getFeatures") FeaturesDto() else error("Unexpected call ${method.name}")
        } as ServixApiService
        val session = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
        var calls = 0
        val vm = ChatsViewModel(ChatRepository(api), FeaturesRepository(api),
            ChatEventStream(OkHttpClient(), "https://example.com/community/events"), session) {
            calls++
            if (calls == 1) Result.failure(ServixApiException(401, message = "Authentication required"))
            else Result.success(listOf(ChatThreadDto(id = (session.value as SessionState.Authenticated).user.id)))
        }
        runCurrent()
        assertEquals(0, calls)
        session.value = SessionState.Authenticated(UserDto(id = "one"))
        runCurrent()
        assertTrue(vm.uiState.value.loading)
        assertNull(vm.uiState.value.error)
        advanceTimeBy(150); runCurrent()
        assertEquals("one", vm.uiState.value.threads.single().id)
        session.value = SessionState.Unauthenticated
        runCurrent()
        assertTrue(vm.uiState.value.threads.isEmpty())
        session.value = SessionState.Authenticated(UserDto(id = "two"))
        runCurrent()
        assertEquals("two", vm.uiState.value.threads.single().id)
        assertEquals(3, calls)
    }
    @Test fun messageUpdatesThreadAndUnreadWithoutConversation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val api = Proxy.newProxyInstance(ServixApiService::class.java.classLoader, arrayOf(ServixApiService::class.java)) { _, method, _ ->
            if (method.name == "getFeatures") FeaturesDto() else error("Unexpected call")
        } as ServixApiService
        val session = MutableStateFlow<SessionState>(SessionState.Authenticated(UserDto(id = "me")))
        val vm = ChatsViewModel(ChatRepository(api), FeaturesRepository(api),
            ChatEventStream(OkHttpClient(), "https://example.com/community/events"), session) {
            Result.success(listOf(ChatThreadDto(id = "thread")))
        }
        runCurrent()
        val message = ChatMessageDto(id = "m1", body = "Hello", senderId = "other", createdAt = "2026-01-01")
        vm.onEvent(ChatEvent.Message("thread", message))
        assertEquals("Hello", vm.uiState.value.threads.single().lastMessage)
        assertEquals(1, vm.uiState.value.unreadTotal)
        vm.onEvent(ChatEvent.Message("thread", message))
        vm.onEvent(ChatEvent.MessageUpdate("thread", message.copy(body = "Edited")))
        assertEquals(1, vm.uiState.value.unreadTotal)
        assertEquals("Edited", vm.uiState.value.threads.single().lastMessage)
    }

    @Test fun timerRunsOnlyWhileResumed() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        var ticks = 0
        owner.registry.currentState = Lifecycle.State.CREATED
        val job = launch { runForegroundRefresh(owner.lifecycle, intervalMs = 25_000) { ticks++ } }
        runCurrent()
        advanceTimeBy(50_000); runCurrent()
        assertEquals(0, ticks)
        owner.registry.currentState = Lifecycle.State.RESUMED
        runCurrent()
        assertEquals(1, ticks)
        advanceTimeBy(25_000); runCurrent()
        assertEquals(2, ticks)
        owner.registry.currentState = Lifecycle.State.CREATED
        runCurrent()
        advanceTimeBy(50_000); runCurrent()
        assertEquals(2, ticks)
        owner.registry.currentState = Lifecycle.State.RESUMED
        runCurrent()
        assertEquals(3, ticks)
        job.cancel()
    }

}
