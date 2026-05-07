package com.example.luminance.ui.vision

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.example.luminance.R
import com.example.luminance.databinding.ActivityVisionBinding
import com.example.luminance.ui.hazard.HazardActivity
import com.example.luminance.ui.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

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
        checkAndRequestPermissions()

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

    // ── 하단 네비게이션 ─────────────────────────────────────────
    /**
     * 핵심 수정 포인트:
     * - 각 Activity에서 setSelectedItemId()로 현재 탭을 표시
     * - Intent에 FLAG_ACTIVITY_REORDER_TO_FRONT 사용 → 백스택 중복 방지
     * - HazardActivity 자신 탭은 아무 동작 안 함 (중복 생성 방지)
     */
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // 현재 화면 탭 선택 표시
        bottomNav.selectedItemId = R.id.nav_vision

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_hazard -> {
                    startActivity(
                        Intent(this, HazardActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    true
                }
                R.id.nav_vision -> {
                    true
                }
                R.id.nav_settings -> {
                    startActivity(
                        Intent(this, SettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    overridePendingTransition(0, 0)
                    true
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

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val deniedPermissions = result.filterValues { !it }.keys

            if (deniedPermissions.isNotEmpty()) {
                showPermissionDialog(deniedPermissions)
            }
        }

    private fun checkAndRequestPermissions() {

        val deniedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (deniedPermissions.isNotEmpty()) {
            permissionLauncher.launch(deniedPermissions.toTypedArray())
        }
    }

    private fun showPermissionDialog(deniedPermissions: Set<String>) {

        val message = buildString {

            if (Manifest.permission.CAMERA in deniedPermissions) {
                append("• 카메라 권한이 없어 전방 위험 감지가 제한됩니다.\n")
            }

            if (Manifest.permission.RECORD_AUDIO in deniedPermissions) {
                append("• 마이크 권한이 없어 음성 기능이 제한됩니다.\n")
            }

            if (Manifest.permission.ACCESS_FINE_LOCATION in deniedPermissions) {
                append("• 위치 권한이 없어 길 안내 기능이 제한됩니다.\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage(message)
            .setPositiveButton("설정으로 이동") { _, _ ->

                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )

                startActivity(intent)
            }
            .setNegativeButton("닫기", null)
            .show()
    }


}