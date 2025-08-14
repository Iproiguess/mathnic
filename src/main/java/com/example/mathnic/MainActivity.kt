package com.example.mathnic

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible


class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var uiContentLayer: ConstraintLayout
    private lateinit var backgroundImageView: ImageView
    private lateinit var characterImageView: ImageView

    private lateinit var titleMathTextView: TextView
    private lateinit var titleNicTextView: TextView

    private lateinit var countingButtonContainer: ConstraintLayout
    private lateinit var matchButtonContainer: ConstraintLayout
    private lateinit var fillButtonContainer: ConstraintLayout
    private lateinit var quitButtonContainer: ConstraintLayout

    private lateinit var newButtonsContainer: LinearLayout

    private lateinit var mainScreenElementsToFade: List<View>

    companion object {
        private const val TAG = "MainActivity"
        private const val FADE_DURATION = 300L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupCharacterImage()
        setupClickListeners()


        mainScreenElementsToFade = listOfNotNull(
            characterImageView,
            titleMathTextView,
            titleNicTextView,
            countingButtonContainer,
            newButtonsContainer
        ).distinct()

        mainScreenElementsToFade.forEach {
            it.alpha = 1f
            it.isVisible = true
        }
    }

    private fun initializeViews() {
        uiContentLayer = findViewById(R.id.ui_content_layer)
        backgroundImageView = findViewById(R.id.backgroundImageView)
        characterImageView = findViewById(R.id.characterImageView)

        titleMathTextView = findViewById(R.id.titleMathTextView)
        titleNicTextView = findViewById(R.id.titleNicTextView)

        countingButtonContainer = findViewById(R.id.countingButtonContainer)
        matchButtonContainer = findViewById(R.id.matchButtonContainer)
        fillButtonContainer = findViewById(R.id.fillButtonContainer)
        quitButtonContainer = findViewById(R.id.quitButtonContainer)

        newButtonsContainer = findViewById(R.id.newButtonsContainer)
    }


    private fun setupCharacterImage() {
        characterImageView.setImageResource(R.drawable.title_sign)
    }

    private fun setupClickListeners() {
        countingButtonContainer.setOnClickListener {
            animateFadeOutViews(mainScreenElementsToFade) {
                val intent = Intent(this, CountingActivity::class.java)
                startActivity(intent)
            }
        }

        matchButtonContainer.setOnClickListener {
            animateFadeOutViews(mainScreenElementsToFade) {
                val intent = Intent(this, MatchingActivity::class.java)
                startActivity(intent)

            }
        }

        fillButtonContainer.setOnClickListener {
            animateFadeOutViews(mainScreenElementsToFade) {
                val intent = Intent(this, FillActivity::class.java)
                startActivity(intent)
            }
        }

        quitButtonContainer.setOnClickListener {
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()

        var needsFadeIn = false
        mainScreenElementsToFade.forEach { view ->
            if (view.isGone || view.alpha < 1.0f) {
                if (view.isGone) view.isVisible = true
                view.alpha = 0f
                needsFadeIn = true
            } else {
                view.alpha = 1f
                if (!view.isVisible) view.isVisible = true
            }
        }

        if (needsFadeIn) {
            animateFadeInViews(mainScreenElementsToFade) {
            }
        } else {
            Log.d(TAG, "onResume: All main screen elements are already visible and opaque. No fade-in needed.")
        }
    }

    private fun animateFadeOutViews(viewsToFade: List<View>, onEndAction: (() -> Unit)? = null) {
        if (viewsToFade.isEmpty()) {
            onEndAction?.invoke()
            return
        }

        val animators = mutableListOf<Animator>()
        viewsToFade.forEach { view ->
            if (view.isVisible && view.alpha > 0f) {
                val animator = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0f).apply {
                    duration = FADE_DURATION
                }
                animators.add(animator)
            }
        }

        if (animators.isEmpty()) {
            viewsToFade.forEach {
                if (it.isVisible) it.isGone = true
                it.alpha = 0f
            }
            onEndAction?.invoke()
            return
        }

        val animatorSet = android.animation.AnimatorSet().apply {
            playTogether(animators)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    viewsToFade.forEach {
                        it.isGone = true
                        it.alpha = 0f
                    }
                    onEndAction?.invoke()
                }
            })
        }
        animatorSet.start()
    }

    private fun animateFadeInViews(viewsToFade: List<View>, onEndAction: (() -> Unit)? = null) {
        if (viewsToFade.isEmpty()) {
            onEndAction?.invoke()
            return
        }

        val animators = mutableListOf<Animator>()
        viewsToFade.forEach { view ->
            if (view.isGone || !view.isVisible) {
                view.alpha = 0f
                view.isVisible = true
            }

            if (view.alpha < 1f) {
                val animator = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 1f).apply {
                    duration = FADE_DURATION
                }
                animators.add(animator)
            } else {
                view.alpha = 1.0f
                if (!view.isVisible) view.isVisible = true
            }

            if (animators.isEmpty()) {
                viewsToFade.forEach {
                    it.alpha = 1f
                    if (!it.isVisible) it.isVisible = true
                }
                onEndAction?.invoke()
                return
            }

            val animatorSet = android.animation.AnimatorSet().apply {
                playTogether(animators)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        viewsToFade.forEach {
                            it.alpha = 1f
                            if (!it.isVisible) it.isVisible = true
                        }
                        onEndAction?.invoke()
                    }
                })
            }
            animatorSet.start()
        }
    }
}
