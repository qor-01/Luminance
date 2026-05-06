package com.example.luminance.ui.vision

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.example.luminance.databinding.ActivityVisionBinding

/**
 * VisionActivity
 *
 * 시각 보조 화면 - 카메라 프리뷰 위에 실시간 안내 텍스트 카드를 표시한다.
 * 접근성 기준: 최소 18sp 이상 폰트, 고대비 색상 (#191C1D on #FFFFFF)
 *
 * 현재: 더미 안내 문구 표시
 * 추후: InferenceManager / DepthManager 결과를 받아 실시간 갱신
 */
class VisionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisionBinding

    // 더미 안내 문구 목록 (추후 AI 추론 결과로 교체)
    private val guidanceMessages = listOf(
        "오른쪽에서 자전거가 빠르게 접근 중입니다",
        "전방 2미터에 계단이 있습니다",
        "왼쪽에 사람이 서 있습니다",
        "전방이 안전합니다. 계속 진행하세요",
        "신호등이 빨간불입니다. 멈춰 주세요"
    )

    private var currentMessageIndex = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // 안내 문구 자동 순환 Runnable (실제 구현에서는 AI 결과로 교체)
    private val cycleRunnable = object : Runnable {
        override fun run() {
            updateGuidanceText(guidanceMessages[currentMessageIndex])
            currentMessageIndex = (currentMessageIndex + 1) % guidanceMessages.size
            handler.postDelayed(this, 3000L) // 3초마다 갱신
        }
    }

    // ───────────────────────────────────────────────────────────
    // Lifecycle
    // ───────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisionBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setupBottomNav()
        setupMicButton()
        startGuidanceCycle()

        // TODO: CameraX 프리뷰 연결
        // setupCamera()

        // TODO: 실제 AI 추론 결과 연동
        // inferenceManager.result.observe(this) { result ->
        //     updateGuidanceText(result.guidanceMessage)
        // }
    }

    override fun onResume() {
        super.onResume()
        handler.post(cycleRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(cycleRunnable)
    }

    // ───────────────────────────────────────────────────────────
    // 실시간 안내 텍스트 업데이트
    // ───────────────────────────────────────────────────────────

    /**
     * 안내 카드의 텍스트를 갱신한다.
     * 접근성: 18sp 이상, 고대비(#191C1D on #FFFFFF) 는 XML에서 보장
     * TalkBack 을 위해 contentDescription 도 함께 갱신
     */
    fun updateGuidanceText(message: String) {
        binding.tvGuidanceText.text = message
        binding.tvGuidanceText.contentDescription = "실시간 안내: $message"
    }

    // ───────────────────────────────────────────────────────────
    // 더미 순환 시작
    // ───────────────────────────────────────────────────────────

    private fun startGuidanceCycle() {
        // 첫 메시지 즉시 표시
        updateGuidanceText(guidanceMessages[0])
    }

    // ───────────────────────────────────────────────────────────
    // 하단 네비게이션
    // ───────────────────────────────────────────────────────────

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = com.example.luminance.R.id.nav_vision

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.example.luminance.R.id.nav_vision -> true
                com.example.luminance.R.id.nav_hazard -> {
                    startActivity(
                        android.content.Intent(this,
                            com.example.luminance.ui.hazard.HazardActivity::class.java)
                    )
                    false
                }
                com.example.luminance.R.id.nav_settings -> {
                    startActivity(
                        android.content.Intent(this,
                            com.example.luminance.ui.settings.SettingsActivity::class.java)
                    )
                    false
                }
                else -> false
            }
        }
    }

    // ───────────────────────────────────────────────────────────
    // 마이크 버튼 (음성 명령 - 추후 구현)
    // ───────────────────────────────────────────────────────────

    private fun setupMicButton() {
        binding.btnMic.setOnClickListener {
            // TODO: 음성 명령 인식 연동
        }
        binding.fabMic.setOnClickListener {
            // TODO: 음성 명령 인식 연동
        }
    }
}