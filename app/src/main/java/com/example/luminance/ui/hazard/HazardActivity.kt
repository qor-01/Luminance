package com.example.luminance.ui.hazard

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.luminance.R
import com.example.luminance.ui.settings.SettingsActivity
import com.example.luminance.ui.vision.VisionActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator

class HazardActivity : AppCompatActivity() {

    // ── HA-002: 퀵 액션 토글 상태 ───────────────────────────────
    private var ttsEnabled = true
    private var hapticEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hazard)
        setupPulseDot()
        setupQuickActions()   // HA-002
        setupMapView()        // HA-005
        setupBottomNav()      // 네비게이션 수정
    }

    // ── HA-002: 퀵 액션 버튼 (TTS / 햅틱 토글) ─────────────────
    private fun setupQuickActions() {
        val btnTts    = findViewById<ImageButton>(R.id.btnTtsToggle)
        val btnHaptic = findViewById<ImageButton>(R.id.btnHapticToggle)

        btnTts.setOnClickListener {
            ttsEnabled = !ttsEnabled
            updateTtsButton(btnTts)
            val msg = if (ttsEnabled) "음성 안내 켜짐" else "음성 안내 꺼짐"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            // TODO: TtsManager.setEnabled(ttsEnabled)
        }

        btnHaptic.setOnClickListener {
            hapticEnabled = !hapticEnabled
            updateHapticButton(btnHaptic)
            val msg = if (hapticEnabled) "진동 피드백 켜짐" else "진동 피드백 꺼짐"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            // TODO: HapticManager.setEnabled(hapticEnabled)
        }

        // 초기 상태 반영
        updateTtsButton(btnTts)
        updateHapticButton(btnHaptic)
    }

    private fun updateTtsButton(btn: ImageButton) {
        val iconRes = if (ttsEnabled) R.drawable.ic_volume_on else R.drawable.ic_volume_off
        val tint    = if (ttsEnabled) R.color.md_primary else R.color.md_on_surface_variant
        btn.setImageResource(iconRes)
        btn.imageTintList = ContextCompat.getColorStateList(this, tint)
        btn.alpha = if (ttsEnabled) 1f else 0.5f
    }

    private fun updateHapticButton(btn: ImageButton) {
        val iconRes = if (hapticEnabled) R.drawable.ic_vibration_on else R.drawable.ic_vibration_off
        val tint    = if (hapticEnabled) R.color.md_primary else R.color.md_on_surface_variant
        btn.setImageResource(iconRes)
        btn.imageTintList = ContextCompat.getColorStateList(this, tint)
        btn.alpha = if (hapticEnabled) 1f else 0.5f
    }

    // ── 실시간 분석 펄스 점 애니메이션 ──────────────────────────
    private fun setupPulseDot() {
        val dot = findViewById<View>(R.id.pulseDot)
        val scaleX = ObjectAnimator.ofFloat(dot, "scaleX", 1f, 1.4f, 1f).apply{repeatCount=ValueAnimator.INFINITE}
        val scaleY = ObjectAnimator.ofFloat(dot, "scaleY", 1f, 1.4f, 1f).apply{repeatCount=ValueAnimator.INFINITE}
        val alpha  = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.4f, 1f).apply{repeatCount=ValueAnimator.INFINITE}

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    // ── HA-005: CustomMapView 핀 설정 ────────────────────────────
    private fun setupMapView() {
        val mapView = findViewById<CustomMapView>(R.id.customMapView)

        val pins = listOf(
            CustomMapView.HazardPin(
                id = "1", label = "열려 있는 맨홀", detail = "경로상에 있습니다. 멈춰서 왼쪽으로 우회하세요.",
                level = CustomMapView.HazardLevel.IMMEDIATE,
                relX = 0.15f, relY = -0.25f
            ),
            CustomMapView.HazardPin(
                id = "2", label = "빠른 자전거", detail = "2시 방향에서 접근 중. 빠르게 이동 중.",
                level = CustomMapView.HazardLevel.NEAR,
                relX = 0.45f, relY = 0.10f
            ),
            CustomMapView.HazardPin(
                id = "3", label = "계단 아래", detail = "앞에 지하철 입구. 점자 블록 있음.",
                level = CustomMapView.HazardLevel.AHEAD,
                relX = -0.10f, relY = 0.50f
            )
        )

        mapView.setPins(pins)

        // 핀 탭 → 해당 위험 카드 강조 or 토스트
        mapView.setOnPinTappedListener { pin ->
            Toast.makeText(this, "${pin.label}: ${pin.detail}", Toast.LENGTH_LONG).show()
            // TODO: 해당 카드로 스크롤 이동
        }

        // 확대 버튼 → 전체화면 지도 (추후 FullMapActivity로 전환)
        findViewById<View>(R.id.btnExpandMap).setOnClickListener {
            Toast.makeText(this, "전체 지도 보기 (준비 중)", Toast.LENGTH_SHORT).show()
        }
    }

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
        bottomNav.selectedItemId = R.id.nav_hazard

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_hazard -> {
                    // 현재 화면 — 아무 동작 없음
                    true
                }
                R.id.nav_vision -> {
                    startActivity(
                        Intent(this, VisionActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    if (android.os.Build.VERSION.SDK_INT >= 34) {
                        overrideActivityTransition(
                            OVERRIDE_TRANSITION_OPEN,
                            0,
                            0
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    } // 탭 전환 애니메이션 제거
                    true
                }
                R.id.nav_settings -> {
                    startActivity(
                        Intent(this, SettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    if (android.os.Build.VERSION.SDK_INT >= 34) {
                        overrideActivityTransition(
                            OVERRIDE_TRANSITION_OPEN,
                            0,
                            0
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                else -> false
            }
        }
    }
}