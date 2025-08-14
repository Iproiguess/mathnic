package com.example.mathnic

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import kotlin.random.Random

class CountingActivity : AppCompatActivity() {

    private lateinit var backButtonImage: ImageView
    private lateinit var signContainer1: ConstraintLayout
    private lateinit var signContainer2: ConstraintLayout
    private lateinit var signContainer3: ConstraintLayout

    private lateinit var textViewSignText1: TextView
    private lateinit var textViewSignText2: TextView
    private lateinit var textViewSignText3: TextView
    private lateinit var backgroundImageView: ImageView

    // Sign
    private lateinit var feedbackImageView: ImageView

    // Answer Button
    private lateinit var answerButtonsContainer: LinearLayout
    private lateinit var answerButton1: ConstraintLayout
    private lateinit var answerButton2: ConstraintLayout
    private lateinit var answerButton3: ConstraintLayout
    private lateinit var answerButtonText1: TextView
    private lateinit var answerButtonText2: TextView
    private lateinit var answerButtonText3: TextView

    private var currentProblemAnimatorSet: AnimatorSet? = null

    // Animation & Layout
    private var screenHeight: Int = 0
    private var screenWidth: Int = 0
    private var signLandedY: Float = 0f
    private var targetSignHeightPx: Int = 0
    private var initialLayoutDone = false

    private var animationStep = 0
    private val animationPositionsX = mutableListOf<Float>()
    private var signTextsToDisplay = mutableListOf<String>()

    private lateinit var allSignContainers: List<ConstraintLayout>
    private lateinit var allTextViews: List<TextView>
    private lateinit var answerButtonTextViews: List<TextView>
    private lateinit var answerClickableViews: List<View>
    private var correctAnswer: Int = 0

    companion object {
        private const val TAG = "CountingActivity"
        private const val SIGN_ANIMATION_DURATION = 600L     // falling duration
        private const val DELAY_BETWEEN_SIGNS = 150L
        private const val ANSWER_BUTTON_FADE_IN_DURATION = 400L
        private const val ITEMS_FADE_OUT_DURATION = 300L
        private const val FEEDBACK_IMAGE_FADE_DURATION = 400L
        private const val FEEDBACK_IMAGE_DISPLAY_DURATION = 1200L

        private const val TARGET_SIGN_WIDTH_DP = 85
        private const val TARGET_SIGN_HEIGHT_DP = 140
        private const val TARGET_SIGN_LANDED_Y_PERCENTAGE = 0.55f // edit for adjustable y position

        // Size for the feedback image
        private const val LARGE_FEEDBACK_IMAGE_SIZE_DP = 180

        private const val WORD_PLUS_TEXT_SIZE_SP = 28f
        private const val WORD_MINUS_TEXT_SIZE_SP = 18f
        private const val NUMBER_TEXT_SIZE_SP = 46f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_counting)

        initializeViewsWithFindViewById()

        backButtonImage.setOnClickListener {
            finish()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.systemBars()
            )
            screenWidth = windowMetrics.bounds.width() - insets.left - insets.right
            screenHeight = windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenHeight = displayMetrics.heightPixels
            screenWidth = displayMetrics.widthPixels
        }

        backgroundImageView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (!initialLayoutDone && backgroundImageView.width > 0 && backgroundImageView.height > 0 && screenWidth > 0) {
                    initialLayoutDone = true
                    backgroundImageView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    val targetSignWidthPx =
                        (TARGET_SIGN_WIDTH_DP * resources.displayMetrics.density).toInt()
                    targetSignHeightPx =
                        (TARGET_SIGN_HEIGHT_DP * resources.displayMetrics.density).toInt() // Assign to class member

                    // Calculate landing (top of image)
                    signLandedY =
                        (backgroundImageView.height * TARGET_SIGN_LANDED_Y_PERCENTAGE) - (targetSignHeightPx / 2f)


                    allSignContainers.forEach { container ->
                        val params = container.layoutParams
                        params.width = targetSignWidthPx
                        params.height = targetSignHeightPx
                        container.layoutParams = params
                    }


                    val largeFeedbackImageSizePx =
                        (LARGE_FEEDBACK_IMAGE_SIZE_DP * resources.displayMetrics.density).toInt()
                    val feedbackParams =
                        feedbackImageView.layoutParams as ConstraintLayout.LayoutParams
                    feedbackParams.width = largeFeedbackImageSizePx

                    feedbackParams.height = largeFeedbackImageSizePx
                    feedbackImageView.layoutParams = feedbackParams


                    // x position, gap
                    animationPositionsX.clear()
                    val totalSignWidthArea = targetSignWidthPx * allSignContainers.size
                    val gapCount = allSignContainers.size + 1
                    val gap = (screenWidth - totalSignWidthArea) / gapCount.toFloat()

                    if (gap < 0) {
                        Log.e(
                            TAG,
                            "Calculated gap is negative. Signs might be too wide for the screen. ScreenW: $screenWidth, TotalSignW: $totalSignWidthArea"
                        )
                    }

                    var currentX = gap
                    for (i in allSignContainers.indices) {
                        animationPositionsX.add(currentX)
                        currentX += targetSignWidthPx + gap
                    }

                    prepareForNewProblemAnimation()
                    generateNewProblemData()
                    startProblemAnimationSequence()
                }
            }
        })
    }

    private fun initializeViewsWithFindViewById() {
        signContainer1 = findViewById(R.id.signContainer1)
        signContainer2 = findViewById(R.id.signContainer2)
        signContainer3 = findViewById(R.id.signContainer3)

        textViewSignText1 = findViewById(R.id.textViewSignText1)
        textViewSignText2 = findViewById(R.id.textViewSignText2)
        textViewSignText3 = findViewById(R.id.textViewSignText3)

        allSignContainers = listOf(signContainer1, signContainer2, signContainer3)
        allTextViews = listOf(textViewSignText1, textViewSignText2, textViewSignText3)

        backButtonImage = findViewById(R.id.BackButtonImage)
        backgroundImageView = findViewById(R.id.countingBackgroundImageView)

        // REMOVE resultSignBackgroundImageView initialization
        // resultSignBackgroundImageView = findViewById(R.id.resultSignBackgroundImageView)
        feedbackImageView = findViewById(R.id.feedbackImageView) // The one that shows correct/wrong

        answerButtonsContainer = findViewById(R.id.answerButtonsContainer)
        answerButton1 = findViewById(R.id.answerButton1)
        answerButton2 = findViewById(R.id.answerButton2)
        answerButton3 = findViewById(R.id.answerButton3)
        answerButtonText1 = findViewById(R.id.answerButtonText1)
        answerButtonText2 = findViewById(R.id.answerButtonText2)
        answerButtonText3 = findViewById(R.id.answerButtonText3)

        answerButtonTextViews = listOf(answerButtonText1, answerButtonText2, answerButtonText3)
        answerClickableViews = listOf(answerButton1, answerButton2, answerButton3)
    }

    private fun generateNewProblemData() {

        signTextsToDisplay.clear()
        var num1: Int
        var num2: Int
        val operatorIsPlus = Random.nextBoolean()
        var operatorSymbol: String

        while (true) {
            num1 = Random.nextInt(0, 100)
            if (operatorIsPlus) {
                operatorSymbol = "PLUS" // display

                val maxRangeForNum2 = (99 - num1).coerceAtLeast(0)
                num2 = Random.nextInt(0, maxRangeForNum2 + 1)
                correctAnswer = num1 + num2
            } else {
                operatorSymbol = "MINUS" // display

                val maxRangeForNum2 = num1
                num2 = Random.nextInt(0, maxRangeForNum2 + 1)
                correctAnswer = num1 - num2
            }
            if (correctAnswer in 0..99) break
        }

        signTextsToDisplay.add(num1.toString())
        signTextsToDisplay.add(operatorSymbol)
        signTextsToDisplay.add(num2.toString())

        // text sizes
        allTextViews[0].setTextSize(TypedValue.COMPLEX_UNIT_SP, NUMBER_TEXT_SIZE_SP)
        allTextViews[2].setTextSize(TypedValue.COMPLEX_UNIT_SP, NUMBER_TEXT_SIZE_SP)
        if (operatorSymbol == "MINUS") {
            allTextViews[1].setTextSize(TypedValue.COMPLEX_UNIT_SP, WORD_MINUS_TEXT_SIZE_SP)
        } else {
            allTextViews[1].setTextSize(TypedValue.COMPLEX_UNIT_SP, WORD_PLUS_TEXT_SIZE_SP)
        }
    }

    private fun setupAnswerButtons() {
        val options = mutableSetOf<Int>()
        options.add(correctAnswer)

        // wrong answer generation
        while (options.size < 3) {
            val randomOption = Random.nextInt(0, 100)
            if (randomOption !in options) {
                options.add(randomOption)
            }
        }
        val shuffledOptions = options.toList().shuffled()

        answerButtonTextViews.forEachIndexed { index, textView ->
            textView.text = shuffledOptions[index].toString()
        }

        answerClickableViews.forEachIndexed { index, buttonView ->
            buttonView.setOnClickListener(null)
            buttonView.setOnClickListener {
                if (answerButtonTextViews[index].text.isNotEmpty()) {
                    handleAnswerSelection(answerButtonTextViews[index].text.toString().toInt())
                }
            }
        }
        enableAnswerButtons()

        answerButtonsContainer.alpha = 0f
        answerButtonsContainer.isVisible = true
        ObjectAnimator.ofFloat(answerButtonsContainer, View.ALPHA, 1f).apply {
            duration = ANSWER_BUTTON_FADE_IN_DURATION
            start()
        }
    }


    private fun handleAnswerSelection(selectedAnswer: Int) {
        if (!isActivityValid()) return
        disableAnswerButtons()

        val isCorrect = selectedAnswer == correctAnswer

        // Animation
        val fadeOutProblemAnimatorSet = AnimatorSet()
        val problemElementAnimators = mutableListOf<Animator>()

        allSignContainers.forEach { container ->
            if (container.isVisible && container.alpha > 0f) {
                problemElementAnimators.add(
                    ObjectAnimator.ofFloat(container, View.ALPHA, 0f).apply {
                        duration = ITEMS_FADE_OUT_DURATION
                    })
            }
        }
        if (answerButtonsContainer.isVisible && answerButtonsContainer.alpha > 0f) {
            problemElementAnimators.add(
                ObjectAnimator.ofFloat(
                    answerButtonsContainer,
                    View.ALPHA,
                    0f
                ).apply {
                    duration = ITEMS_FADE_OUT_DURATION
                })
        }

        if (problemElementAnimators.isEmpty()) {
            showFeedbackImageAndContinue(isCorrect)
            return
        }

        fadeOutProblemAnimatorSet.playTogether(problemElementAnimators)
        fadeOutProblemAnimatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isActivityValid()) return                            // safety
                allSignContainers.forEach { it.isVisible = false }
                answerButtonsContainer.isVisible = false

                showFeedbackImageAndContinue(isCorrect)
            }
        })
        fadeOutProblemAnimatorSet.start()
    }

    private fun showFeedbackImageAndContinue(isCorrect: Boolean) {
        if (!isActivityValid()) return

        val feedbackResId = if (isCorrect) R.drawable.correct else R.drawable.wrong
        feedbackImageView.setImageResource(feedbackResId)
        feedbackImageView.alpha = 0f
        feedbackImageView.isVisible = true

        ObjectAnimator.ofFloat(feedbackImageView, View.ALPHA, 1f).apply {
            duration = FEEDBACK_IMAGE_FADE_DURATION
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isActivityValid()) return                      // safety


                    feedbackImageView.postDelayed({
                        if (!isActivityValid()) return@postDelayed

                        ObjectAnimator.ofFloat(feedbackImageView, View.ALPHA, 0f).apply {
                            duration = FEEDBACK_IMAGE_FADE_DURATION
                            addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    if (!isActivityValid()) return                    // safety
                                    feedbackImageView.isVisible = false

                                    prepareForNewProblemAnimation()
                                    generateNewProblemData()
                                    startProblemAnimationSequence()
                                }
                            })
                            start()
                        }
                    }, FEEDBACK_IMAGE_DISPLAY_DURATION)
                }
            })
            start()
        }
    }


    private fun prepareForNewProblemAnimation() {
        currentProblemAnimatorSet?.cancel()
        animationStep = 0

        allSignContainers.forEach {
            it.isVisible = false
            it.alpha = 0f

            if (targetSignHeightPx > 0) {
                it.translationY = -targetSignHeightPx.toFloat() * 1.5f
            } else {
                it.translationY = -300f
            }
        }
        answerButtonsContainer.isVisible = false
        answerButtonsContainer.alpha = 0f
        disableAnswerButtons()

        feedbackImageView.isVisible = false
        feedbackImageView.alpha = 0f
    }

    private fun startProblemAnimationSequence() {
        animationStep = 0

        if (signTextsToDisplay.isEmpty()) {
            generateNewProblemData()
            if (signTextsToDisplay.isEmpty()) {
                return
            }
        }
        if (animationPositionsX.isEmpty() && allSignContainers.isNotEmpty()) {
            backgroundImageView.postDelayed({
                if (isActivityValid() && animationPositionsX.isNotEmpty()) animateNextSign()
            }, 100)
            return
        }

        if (initialLayoutDone) {
            animateNextSign()
        } else {
            Log.w(TAG, "Initial layout not done.")
        }
    }


    private fun animateNextSign() {
        if (!isActivityValid()) return

        if (animationStep >= allSignContainers.size || animationStep >= signTextsToDisplay.size) {
            if (allSignContainers.isNotEmpty()) {
                val lastSign = allSignContainers.last()
                if (lastSign.isVisible && lastSign.alpha == 1f) {
                    setupAnswerButtons()
                } else if (animationStep >= allSignContainers.size) {
                    setupAnswerButtons()
                }
            } else {
                Log.e(TAG, "Cannot setup answer buttons: no sign containers.")
            }
            return
        }

        val currentSignContainer = allSignContainers[animationStep]
        val currentSignTextView = allTextViews[animationStep]

        currentSignTextView.text = signTextsToDisplay[animationStep]
        currentSignContainer.translationX = animationPositionsX.getOrElse(animationStep) {
            (screenWidth / 2f) - (currentSignContainer.width / 2f)
        }
        //position for falling
        if (targetSignHeightPx > 0) {
            currentSignContainer.translationY = -targetSignHeightPx.toFloat() * 1.5f
        } else {
            currentSignContainer.translationY =
                -((TARGET_SIGN_HEIGHT_DP * resources.displayMetrics.density) * 1.5f)
        }
        currentSignContainer.alpha = 0f
        currentSignContainer.isVisible = true

        val fallAnimator =
            ObjectAnimator.ofFloat(currentSignContainer, View.TRANSLATION_Y, signLandedY).apply {
                duration = SIGN_ANIMATION_DURATION
                interpolator = android.view.animation.DecelerateInterpolator(1.3f)
            }
        val fadeInAnimator = ObjectAnimator.ofFloat(currentSignContainer, View.ALPHA, 1f).apply {
            duration = SIGN_ANIMATION_DURATION * 2 / 3
        }

        val animatorSetForThisSign = AnimatorSet().apply {
            playTogether(fallAnimator, fadeInAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isActivityValid()) return               //safety
                    animationStep++
                    currentSignContainer.postDelayed({
                        if (!isActivityValid()) return@postDelayed
                        animateNextSign()
                    }, DELAY_BETWEEN_SIGNS)
                }

                override fun onAnimationCancel(animation: Animator) {
                    if (!isActivityValid()) return
                }
            })
        }
        currentProblemAnimatorSet = animatorSetForThisSign
        animatorSetForThisSign.start()
    }

    private fun disableAnswerButtons() {
        answerClickableViews.forEach { it.isClickable = false }
    }

    private fun enableAnswerButtons() {
        answerClickableViews.forEach { it.isClickable = true }
    }

    private fun isActivityValid(): Boolean {
        return !isFinishing && !isDestroyed
    }

    override fun onDestroy() {
        super.onDestroy()
        currentProblemAnimatorSet?.cancel()
        allSignContainers.forEach { it.removeCallbacks(null) }
        answerButtonsContainer.removeCallbacks(null)
        feedbackImageView.removeCallbacks(null)
        backgroundImageView.viewTreeObserver.removeOnGlobalLayoutListener(null)
    }
}
