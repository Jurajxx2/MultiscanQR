package com.juraj.multiscanqr.scanner

import com.juraj.multiscanqr.data.ScanHistoryRepository
import com.juraj.multiscanqr.domain.ScanContentType
import com.juraj.multiscanqr.domain.ScanResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeScanHistoryRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeScanHistoryRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(clock: () -> Long = { 1_000L }) =
        ScannerViewModel(repository, clock)

    @Test
    fun detectionPausesScanningAndSavesToHistory() = runTest(dispatcher) {
        val vm = viewModel()

        vm.onEvent(ScannerEvent.QrDetected("https://example.com"))

        val state = vm.state.value
        assertFalse(state.isScanning)
        assertEquals("https://example.com", state.activeResult?.content)
        assertEquals(ScanContentType.URL, state.activeResult?.type)
        assertEquals(listOf("https://example.com"), repository.saved.map { it.content })
    }

    @Test
    fun detectionsAreIgnoredWhileResultIsShown() = runTest(dispatcher) {
        val vm = viewModel()

        vm.onEvent(ScannerEvent.QrDetected("first"))
        vm.onEvent(ScannerEvent.QrDetected("second"))

        assertEquals("first", vm.state.value.activeResult?.content)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun sameCodeIsDebouncedRightAfterDismissal() = runTest(dispatcher) {
        var now = 1_000L
        val vm = viewModel(clock = { now })

        vm.onEvent(ScannerEvent.QrDetected("code"))
        now += 500
        vm.onEvent(ScannerEvent.ResultDismissed)
        now += 500 // still inside the cooldown window
        vm.onEvent(ScannerEvent.QrDetected("code"))

        assertNull(vm.state.value.activeResult)
        assertEquals(1, repository.saved.size)

        now += ScannerViewModel.DUPLICATE_COOLDOWN_MS // past the window
        vm.onEvent(ScannerEvent.QrDetected("code"))
        assertEquals("code", vm.state.value.activeResult?.content)
        assertEquals(2, repository.saved.size)
    }

    @Test
    fun dismissResumesScanning() = runTest(dispatcher) {
        val vm = viewModel()

        vm.onEvent(ScannerEvent.QrDetected("anything"))
        vm.onEvent(ScannerEvent.ResultDismissed)

        assertTrue(vm.state.value.isScanning)
        assertNull(vm.state.value.activeResult)
    }

    @Test
    fun openLinkEmitsEffectOnlyForUrls() = runTest(dispatcher) {
        val vm = viewModel()
        val effects = mutableListOf<ScannerEffect>()
        val job = launch { vm.effects.collect { effects.add(it) } }

        vm.onEvent(ScannerEvent.QrDetected("plain text"))
        vm.onEvent(ScannerEvent.OpenLinkClicked)
        assertTrue(effects.isEmpty())

        vm.onEvent(ScannerEvent.ResultDismissed)
        vm.onEvent(ScannerEvent.QrDetected("https://example.com"))
        vm.onEvent(ScannerEvent.OpenLinkClicked)
        assertEquals(listOf<ScannerEffect>(ScannerEffect.OpenUrl("https://example.com")), effects)

        job.cancel()
    }

    @Test
    fun copyEmitsClipboardEffect() = runTest(dispatcher) {
        val vm = viewModel()
        val effects = mutableListOf<ScannerEffect>()
        val job = launch { vm.effects.collect { effects.add(it) } }

        vm.onEvent(ScannerEvent.QrDetected("payload"))
        vm.onEvent(ScannerEvent.CopyClicked)

        assertEquals(listOf<ScannerEffect>(ScannerEffect.CopyToClipboard("payload")), effects)
        job.cancel()
    }
}

private class FakeScanHistoryRepository : ScanHistoryRepository {
    val saved = mutableListOf<ScanResult>()
    private val history = MutableStateFlow<List<ScanResult>>(emptyList())

    override fun observeHistory(): Flow<List<ScanResult>> = history

    override suspend fun save(content: String, type: ScanContentType, scannedAtEpochMillis: Long) {
        saved += ScanResult(saved.size + 1L, content, type, scannedAtEpochMillis)
        history.value = saved.sortedByDescending { it.scannedAtEpochMillis }
    }

    override suspend fun delete(id: Long) {
        saved.removeAll { it.id == id }
        history.value = saved.toList()
    }

    override suspend fun clearAll() {
        saved.clear()
        history.value = emptyList()
    }
}
