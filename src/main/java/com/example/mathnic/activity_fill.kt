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
import kotlin.ranges.coerceIn

class FillActivity : AppCompatActivity() {

    private lateinit var backButtonImage: ImageView

    private lateinit var signContainer1: ConstraintLayout
    private lateinit var signContainer2: ConstraintLayout
    private lateinit var signContainer3: ConstraintLayout
    private lateinit var signContainer4: ConstraintLayout


    private lateinit var textViewSignText1: TextView
    private lateinit var textViewSignText2: TextView
    private lateinit var textViewSignText3: TextView
    private lateinit var textViewSignText4: TextView

    private lateinit var backgroundImageView: ImageView

    private lateinit var feedbackImageView: ImageView

    private lateinit var answerButtonsContainer: LinearLayout
    private lateinit var answerButton1: ConstraintLayout
    private lateinit var answerButton2: ConstraintLayout
    private lateinit var answerButton3: ConstraintLayout
    private lateinit var answerButtonText1: TextView
    private lateinit var answerButtonText2: TextView
    private lateinit var answerButtonText3: TextView

    private var currentProblemAnimatorSet: AnimatorSet? = null

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

    private lateinit var currentSequence: List<Int?>
    private var correctAnswer: Int = 0
    companion object {
        private const val TAG = "FillActivity"
        private const val SIGN_ANIMATION_DURATION = 600L
        private const val DELAY_BETWEEN_SIGNS = 150L
        private const val ANSWER_BUTTON_FADE_IN_DURATION = 400L
        private const val ITEMS_FADE_OUT_DURATION = 300L
        private const val FEEDBACK_IMAGE_FADE_DURATION = 400L
        private const val FEEDBACK_IMAGE_DISPLAY_DURATION = 1200L

        private const val TARGET_SIGN_WIDTH_DP = 80
        private const val TARGET_SIGN_HEIGHT_DP = 145

        private const val LARGE_FEEDBACK_IMAGE_SIZE_DP = 180
        private const val NUMBER_TEXT_SIZE_SP = 38f // 2 digit number

        // Constants
        private const val SEQUENCE_LENGTH = 4
        private const val MAX_START_NUMBER = 30
        private const val MIN_DIFFERENCE = 1
        private const val MAX_DIFFERENCE = 12
        private const val MAX_ANSWER_OPTION_VALUE = 99
        private const val MIN_ANSWER_OPTION_VALUE = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fill)

        initializeViewsWithFindViewById()

        if (::backButtonImage.isInitialized) {
            backButtonImage.setOnClickListener {
                finish()
            }
        } else {
            Log.e(TAG, "onCreate: backButtonImage was not initialized. This is a critical error.")
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

        backgroundImageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (!initialLayoutDone && backgroundImageView.width > 0 && backgroundImageView.height > 0 && screenWidth > 0) {
                    initialLayoutDone = true
                    backgroundImageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Log.d(TAG, "GlobalLayout: bg W=${backgroundImageView.width}, H=${backgroundImageView.height}")

                    targetSignHeightPx = (TARGET_SIGN_HEIGHT_DP * resources.displayMetrics.density).toInt()
                    val targetSignWidthPx = (TARGET_SIGN_WIDTH_DP * resources.displayMetrics.density).toInt()


                    val guidelineYPosition = screenHeight * 0.72f
                    val spaceAboveGuideline = 20 * resources.displayMetrics.density
                    signLandedY = guidelineYPosition - targetSignHeightPx - spaceAboveGuideline

                    if (signLandedY < 50 * resources.displayMetrics.density) {
                        signLandedY = 50 * resources.displayMetrics.density
                        Log.w(TAG, "Adjusted signLandedY to prevent it from being too high.")
                    }


                    allSignContainers.forEach { container ->
                        val params = container.layoutParams
                        params.width = targetSignWidthPx
                        params.height = targetSignHeightPx
                        container.layoutParams = params
                    }

                    val largeFeedbackImageSizePx = (LARGE_FEEDBACK_IMAGE_SIZE_DP * resources.displayMetrics.density).toInt()
                    val feedbackParams = feedbackImageView.layoutParams as ConstraintLayout.LayoutParams
                    feedbackParams.width = largeFeedbackImageSizePx
                    feedbackParams.height = largeFeedbackImageSizePx
                    feedbackImageView.layoutParams = feedbackParams

                    // X positions
                    animationPositionsX.clear()
                    val totalSignWidthArea = targetSignWidthPx * allSignContainers.size

                    val usableSignAreaWidth = screenWidth * 1f
                    val minGapPx = 1 * resources.displayMetrics.density

                    val sideMargin = screenWidth * 0.01f

                    val gapCount = allSignContainers.size + 0.5
                    var gap = (usableSignAreaWidth - totalSignWidthArea) / gapCount.toFloat()

                    //val originalSideMarginSpace = screenWidth - usableSignAreaWidth
                    val leftOffset = sideMargin * 0.05f
                    var currentX = leftOffset + gap

                    if (gap < minGapPx) {
                        gap = minGapPx
                    }

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

        backButtonImage = findViewById(R.id.BackButtonImage)

        signContainer1 = findViewById(R.id.signContainer1)
        signContainer2 = findViewById(R.id.signContainer2)
        signContainer3 = findViewById(R.id.signContainer3)
        signContainer4 = findViewById(R.id.signContainer4)

        textViewSignText1 = findViewById(R.id.textViewSignText1)
        textViewSignText2 = findViewById(R.id.textViewSignText2)
        textViewSignText3 = findViewById(R.id.textViewSignText3)
        textViewSignText4 = findViewById(R.id.textViewSignText4)


        allSignContainers = listOf(signContainer1, signContainer2, signContainer3, signContainer4)
        allTextViews = listOf(textViewSignText1, textViewSignText2, textViewSignText3, textViewSignText4)

        backgroundImageView = findViewById(R.id.countingBackgroundImageView)
        feedbackImageView = findViewById(R.id.feedbackImageView)

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

        var isValidSequence = false
        var attempts = 0
        val maxAttempts = 100

        var generatedSequence: List<Int?> = emptyList()
        var generatedCorrectAnswer: Int = 0

        while (!isValidSequence && attempts < maxAttempts) {
            attempts++
            val startNumber = Random.nextInt(MIN_ANSWER_OPTION_VALUE, MAX_START_NUMBER + 1)
            var difference = Random.nextInt(MIN_DIFFERENCE, MAX_DIFFERENCE + 1)
            if (difference == 0) difference =
                if (Random.nextBoolean()) 1 else -1

            if (Random.nextBoolean()) difference *= -1

            val missingIndex = Random.nextInt(SEQUENCE_LENGTH)
            val tempSequence = MutableList<Int?>(SEQUENCE_LENGTH) { null }
            var currentNum = startNumber
            var tempCorrectAnswer = 0
            var sequencePossibleThisAttempt = true

            for (i in 0 until SEQUENCE_LENGTH) {
                if (currentNum < MIN_ANSWER_OPTION_VALUE || currentNum > MAX_ANSWER_OPTION_VALUE) {
                    sequencePossibleThisAttempt = false
                    break
                }

                if (i == missingIndex) {
                    tempSequence[i] = null
                    tempCorrectAnswer = currentNum
                } else {
                    tempSequence[i] = currentNum
                }
                currentNum += difference
            }

            if (sequencePossibleThisAttempt) {
                generatedSequence = tempSequence.toList()
                generatedCorrectAnswer = tempCorrectAnswer
                isValidSequence = true
            }
        }

        if (!isValidSequence) {

            val tempFallbackSequence = MutableList<Int?>(SEQUENCE_LENGTH) { null }
            val fallbackMissingIndex = Random.nextInt(SEQUENCE_LENGTH)

            val fallbackStart = Random.nextInt(
                MIN_ANSWER_OPTION_VALUE,
                (MAX_ANSWER_OPTION_VALUE - (SEQUENCE_LENGTH * MIN_DIFFERENCE)) / 2
            )
            var fallbackDifference = Random.nextInt(MIN_DIFFERENCE, MAX_DIFFERENCE / 2 + 1)
            if (fallbackDifference == 0) fallbackDifference = 1
            if (Random.nextBoolean() && fallbackStart > MAX_ANSWER_OPTION_VALUE / 2) fallbackDifference *= -1

            var currentFallbackNum = fallbackStart
            var fallbackCorrectAnswer = 0

            for (i in 0 until SEQUENCE_LENGTH) {

                currentFallbackNum =
                    currentFallbackNum.coerceIn(MIN_ANSWER_OPTION_VALUE, MAX_ANSWER_OPTION_VALUE)

                if (i == fallbackMissingIndex) {
                    tempFallbackSequence[i] = null
                    fallbackCorrectAnswer = currentFallbackNum
                } else {
                    tempFallbackSequence[i] = currentFallbackNum
                }
                currentFallbackNum += fallbackDifference
            }
            // Final coercion for the answer
            fallbackCorrectAnswer =
                fallbackCorrectAnswer.coerceIn(MIN_ANSWER_OPTION_VALUE, MAX_ANSWER_OPTION_VALUE)

            generatedSequence = tempFallbackSequence.toList()
            generatedCorrectAnswer = fallbackCorrectAnswer
        }

        currentSequence = generatedSequence
        correctAnswer = generatedCorrectAnswer

        // Prepare texts
        signTextsToDisplay.clear()
        currentSequence.forEach { num ->
            signTextsToDisplay.add(num?.toString() ?: "?")
        }

        allTextViews.forEach { textView ->
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, NUMBER_TEXT_SIZE_SP)
        }
    }
    private fun setupAnswerButtons() {
        val options = mutableSetOf<Int>()
        options.add(correctAnswer)

        var generationAttempts = 0
        val maxOptionGenAttempts = 100

        // Generate 2 wrong
        while (options.size < 3 && generationAttempts < maxOptionGenAttempts) {
            generationAttempts++
            var randomOption: Int

            if (Random.nextBoolean() && currentSequence.filterNotNull().isNotEmpty()) {
                val knownNumber = currentSequence.filterNotNull().random()
                val offset = Random.nextInt(-MAX_DIFFERENCE * 2, MAX_DIFFERENCE * 2 + 1)
                randomOption = knownNumber + offset
            } else {
                val offset = Random.nextInt(-MAX_DIFFERENCE - 5, MAX_DIFFERENCE + 6)
                randomOption = correctAnswer + offset
            }
            randomOption = randomOption.coerceIn(MIN_ANSWER_OPTION_VALUE, MAX_ANSWER_OPTION_VALUE)

            if (randomOption !in options) {
                options.add(randomOption)
            } else if (options.size < 3 && generationAttempts > maxOptionGenAttempts / 2) {
                // If struggling, generate a completely random valid number
                val fallbackOption = Random.nextInt(MIN_ANSWER_OPTION_VALUE, MAX_ANSWER_OPTION_VALUE + 1)
                if (fallbackOption !in options) {
                    options.add(fallbackOption)
                }
            }
        }

        var emergencyAttempts = 0
        while (options.size < 3 && emergencyAttempts < 50) {
            emergencyAttempts++
            val emergencyOption = Random.nextInt(MIN_ANSWER_OPTION_VALUE, MAX_ANSWER_OPTION_VALUE + 1)
            if (emergencyOption !in options) {
                options.add(emergencyOption)
            }
        }
        var placeholderValue = MIN_ANSWER_OPTION_VALUE
        while (options.size < 3) {
            while(options.contains(placeholderValue) && placeholderValue <= MAX_ANSWER_OPTION_VALUE) {
                placeholderValue++
            }
            if (placeholderValue <= MAX_ANSWER_OPTION_VALUE) {
                options.add(placeholderValue)
            } else {
                options.add(options.size - 100)
            }
        }

        val shuffledOptions = options.toList().shuffled()

        answerButtonTextViews.forEachIndexed { index, textView ->
            if (index < shuffledOptions.size) {
                textView.text = shuffledOptions[index].toString()
                answerClickableViews[index].isEnabled = true
                answerClickableViews[index].alpha = 1f
            } else {
                textView.text = ""                                // Should not happen
                answerClickableViews[index].isEnabled = false
                answerClickableViews[index].alpha = 0.5f
            }
        }

        answerClickableViews.forEachIndexed { index, buttonView ->
            buttonView.setOnClickListener(null)
            if (index < shuffledOptions.size) {
                buttonView.setOnClickListener {
                    if (answerButtonTextViews[index].text.isNotEmpty()) {
                        try {
                            handleAnswerSelection(answerButtonTextViews[index].text.toString().toInt())
                        } catch (e: NumberFormatException) {
                        }
                    }
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
        val fadeOutProblemAnimatorSet = AnimatorSet()
        val problemElementAnimators = mutableListOf<Animator>()

        allSignContainers.forEach { container ->
            if (container.isVisible && container.alpha > 0f) {
                problemElementAnimators.add(ObjectAnimator.ofFloat(container, View.ALPHA, 0f).apply {
                    duration = ITEMS_FADE_OUT_DURATION
                })
            }
        }
        if (answerButtonsContainer.isVisible && answerButtonsContainer.alpha > 0f) {
            problemElementAnimators.add(ObjectAnimator.ofFloat(answerButtonsContainer, View.ALPHA, 0f).apply {
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
                if (!isActivityValid()) return
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
                    if (!isActivityValid()) return
                    feedbackImageView.postDelayed({
                        if (!isActivityValid()) return@postDelayed
                        ObjectAnimator.ofFloat(feedbackImageView, View.ALPHA, 0f).apply {
                            duration = FEEDBACK_IMAGE_FADE_DURATION
                            addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    if (!isActivityValid()) return
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
            val initialYTranslation = if (targetSignHeightPx > 0) {
                -targetSignHeightPx.toFloat() * 1.5f
            } else {
                -( (TARGET_SIGN_HEIGHT_DP * resources.displayMetrics.density) * 1.5f)
            }
            it.translationY = initialYTranslation
        }

        answerButtonsContainer.isVisible = false
        answerButtonsContainer.alpha = 0f
        disableAnswerButtons()

        feedbackImageView.isVisible = false
        feedbackImageView.alpha = 0f
    }

    private fun startProblemAnimationSequence() {

        if (signTextsToDisplay.isEmpty()) {
            generateNewProblemData()
            if (signTextsToDisplay.isEmpty()) {
                return
            }
        }

        if (animationPositionsX.isEmpty() && allSignContainers.isNotEmpty()) {
            val signWidthPx = (TARGET_SIGN_WIDTH_DP * resources.displayMetrics.density).toInt()
            var currentXFallback = (screenWidth - (allSignContainers.size * signWidthPx + (allSignContainers.size -1) * (5*resources.displayMetrics.density))) / 2f
            if(animationPositionsX.isEmpty() && currentXFallback > 0) {
                for(i in allSignContainers.indices) {
                    animationPositionsX.add(currentXFallback)
                    currentXFallback += signWidthPx + (5*resources.displayMetrics.density)
                }
            }
        }

        if (allSignContainers.isEmpty()){
            return
        }

        animateNextSign()
    }


    private fun animateNextSign() {
        if (!isActivityValid()) return

        if (animationStep >= allSignContainers.size || animationStep >= signTextsToDisplay.size) {
           if (animationStep >= allSignContainers.size) {
                setupAnswerButtons()
            } else {
                setupAnswerButtons()
            }
            return
        }

        val currentSignContainer = allSignContainers[animationStep]
        val currentSignTextView = allTextViews[animationStep]

        currentSignTextView.text = signTextsToDisplay[animationStep]

        currentSignContainer.translationX = animationPositionsX.getOrElse(animationStep) {
            (screenWidth / 2f) - (currentSignContainer.width / 2f)
        }

        val startY = if (targetSignHeightPx > 0) -targetSignHeightPx.toFloat() * 1.5f else -300f
        currentSignContainer.translationY = startY
        currentSignContainer.alpha = 0f
        currentSignContainer.isVisible = true


        val fallAnimator = ObjectAnimator.ofFloat(currentSignContainer, View.TRANSLATION_Y, signLandedY).apply {
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
                    if (!isActivityValid()) return
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
        answerClickableViews.forEach {
            it.isClickable = false
            it.alpha = 0.6f
        }
    }

    private fun enableAnswerButtons() {
        answerClickableViews.forEach {
            it.isClickable = true
            it.alpha = 1.0f
        }
    }

    private fun isActivityValid(): Boolean {
        return !isFinishing && !isDestroyed
    }

    override fun onDestroy() {
        super.onDestroy()
        currentProblemAnimatorSet?.cancel()
        allSignContainers.forEach { container ->

            container.removeCallbacks(null)
        }
        feedbackImageView.removeCallbacks(null)
    }
}
