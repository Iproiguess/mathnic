package com.example.mathnic

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import kotlin.random.Random

class MatchingActivity : AppCompatActivity() {

    private lateinit var activityRootView: ConstraintLayout
    private lateinit var backgroundImageView: ImageView
    private lateinit var backButtonImage: ImageView

    private lateinit var problemSignContainer: ConstraintLayout
    private lateinit var textViewProblemSignText: TextView

    private lateinit var feedbackImageView: ImageView

    private lateinit var answerButtonsContainer: LinearLayout
    private lateinit var answerButton1: ConstraintLayout
    private lateinit var answerButton2: ConstraintLayout
    private lateinit var answerButton3: ConstraintLayout
    private lateinit var answerButtonText1: TextView
    private lateinit var answerButtonText2: TextView
    private lateinit var answerButtonText3: TextView

    private lateinit var allAnimatedElements: List<View>
    private lateinit var answerClickableViews: List<View>
    private lateinit var answerButtonTextViews: List<TextView>

    // Animation & Logic
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var targetSignWidthPx: Int = 0
    private var targetSignHeightPx: Int = 0
    private var signLandedY: Float = 0f
    private val animationPositionsX = mutableListOf<Float>()

    private var currentNumber: Int = 0
    private val currentProblemString = mutableListOf<String>()

    private var initialFadeInComplete = false
    private var viewsMeasured = false

    // --- Constants ---
    companion object {
        private const val TAG = "MatchingActivity"
        private const val SIGN_ASPECT_RATIO = 1f
        private const val TARGET_SIGN_WIDTH_PERCENTAGE = 0.45f
        private const val TARGET_SIGN_LANDED_Y_PERCENTAGE = 0.525f

        private const val PROBLEM_TEXT_SIZE_SP = 39f // larger single numbers
        private const val ANSWER_WORD_TEXT_SIZE_SP = 18f

        private const val SIGN_DROP_DURATION = 700L
        private const val ANSWER_BUTTON_FADE_IN_DURATION = 400L
        private const val ITEMS_FADE_OUT_DURATION = 300L
        private const val FEEDBACK_IMAGE_FADE_DURATION = 250L
        private const val FEEDBACK_IMAGE_VISIBLE_DURATION = 800L


        private const val LARGE_FEEDBACK_IMAGE_SIZE_DP = 150
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_match)

        initializeViewsWithFindViewById()
        setupBackButton()

        allAnimatedElements = listOf(
            problemSignContainer,
            answerButtonsContainer,
        )

        allAnimatedElements.forEach {
            it.alpha = 0f
            it.isVisible = false
        }
        feedbackImageView.isVisible = false

        activityRootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (!viewsMeasured && activityRootView.width > 0 && activityRootView.height > 0) {
                    activityRootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    viewsMeasured = true

                    screenWidth = activityRootView.width
                    screenHeight = activityRootView.height

                    targetSignWidthPx = (screenWidth * TARGET_SIGN_WIDTH_PERCENTAGE).toInt()
                    targetSignHeightPx = (targetSignWidthPx / SIGN_ASPECT_RATIO).toInt()

                    val signParams = problemSignContainer.layoutParams
                    signParams.width = targetSignWidthPx
                    signParams.height = targetSignHeightPx
                    problemSignContainer.layoutParams = signParams

                    val referenceHeightForSignY = if (backgroundImageView.height > 0) backgroundImageView.height else screenHeight
                    signLandedY = (referenceHeightForSignY * TARGET_SIGN_LANDED_Y_PERCENTAGE) - (targetSignHeightPx / 2f)

                    val largeFeedbackImageSizePx = (LARGE_FEEDBACK_IMAGE_SIZE_DP * resources.displayMetrics.density).toInt()
                    val feedbackParams = feedbackImageView.layoutParams as ConstraintLayout.LayoutParams
                    feedbackParams.width = largeFeedbackImageSizePx
                    feedbackParams.height = largeFeedbackImageSizePx
                    feedbackImageView.layoutParams = feedbackParams

                    animationPositionsX.clear()
                    val signX = (screenWidth / 2f) - (targetSignWidthPx / 2f)
                    animationPositionsX.add(signX)
                    performInitialFadeIn()
                }
            }
        })
    }

    private fun initializeViewsWithFindViewById() {
        activityRootView = findViewById(R.id.counting_activity_root)
        backgroundImageView = findViewById(R.id.countingBackgroundImageView)
        backButtonImage = findViewById(R.id.BackButtonImage)
        problemSignContainer = findViewById(R.id.problemSignContainer)
        textViewProblemSignText = findViewById(R.id.textViewProblemSignText)
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

    private fun setupBackButton() {
        backButtonImage.setOnClickListener {
            finishAfterTransition()
        }
    }

    private fun performInitialFadeIn() {
        initialFadeInComplete = true
        prepareForNewProblemAnimation()
        generateNewProblemData()
        startProblemAnimationSequence()
    }

    //Number to Word
    private fun convertNumberToWords(number: Int): String {
        if (number < 0 || number > 99) {
            return "Error"
        }

        val units = arrayOf(
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
            "eighteen", "nineteen"
        )
        val tens = arrayOf(
            "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
        )

        return when {
            number == 0 -> "zero"
            number < 20 -> units[number]
            else -> {
                val tenPart = number / 10
                val unitPart = number % 10
                if (unitPart == 0) {
                    tens[tenPart]
                } else {
                    "${tens[tenPart]}-${units[unitPart]}"
                }
            }
        }
    }

    private fun generateNewProblemData() {
        currentProblemString.clear()

        currentNumber = Random.nextInt(0, 100)
        currentProblemString.add(currentNumber.toString())

        textViewProblemSignText.setTextSize(TypedValue.COMPLEX_UNIT_SP, PROBLEM_TEXT_SIZE_SP)
    }

    private fun prepareForNewProblemAnimation() {
        if (!viewsMeasured || animationPositionsX.isEmpty()) {
            return
        }
        problemSignContainer.isVisible = true
        problemSignContainer.alpha = 1f
        problemSignContainer.translationY = -targetSignHeightPx.toFloat()
        problemSignContainer.translationX = animationPositionsX[0]

        textViewProblemSignText.text = ""

        answerButtonsContainer.alpha = 0f
        answerButtonsContainer.isVisible = false
        disableAnswerButtons()
    }

    private fun startProblemAnimationSequence() {
        if (!isActivityValid() || currentProblemString.isEmpty()) {
            return
        }

        textViewProblemSignText.text = currentProblemString[0]

        val dropAnimator = ObjectAnimator.ofFloat(problemSignContainer, View.TRANSLATION_Y, signLandedY).apply {
            duration = SIGN_DROP_DURATION
            interpolator = android.view.animation.BounceInterpolator()
        }

        dropAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isActivityValid()) return
                setupAnswerButtons()
            }
        })
        dropAnimator.start()
    }

    private fun setupAnswerButtons() {
        if (!isActivityValid()) return

        val correctAnswerWord = convertNumberToWords(currentNumber)
        val optionsWords = mutableSetOf<String>()
        optionsWords.add(correctAnswerWord)

        // Generate two random words
        var attempts = 0
        while (optionsWords.size < 3 && attempts < 100) {
            val randomNumber = Random.nextInt(0, 100)
            val randomWord = convertNumberToWords(randomNumber)
            if (randomWord !in optionsWords) {
                optionsWords.add(randomWord)
            }
            attempts++
        }
        if (optionsWords.size < 3) {
            val backupNumbers = (0..99).toList().shuffled()
            for (num in backupNumbers) {
                if (optionsWords.size >= 3) break
                optionsWords.add(convertNumberToWords(num))
            }
        }


        val shuffledOptions = optionsWords.toList().shuffled()

        answerButtonTextViews.forEachIndexed { index, textView ->
            if (index < shuffledOptions.size) {
                textView.text = shuffledOptions[index]
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ANSWER_WORD_TEXT_SIZE_SP)
            } else {
                textView.text = "" //safety
            }
        }

        answerClickableViews.forEachIndexed { index, buttonView ->
            buttonView.setOnClickListener(null)
            if (index < shuffledOptions.size && answerButtonTextViews[index].text.isNotEmpty()) {
                buttonView.setOnClickListener {
                    handleAnswerSelection(answerButtonTextViews[index].text.toString())
                }
            }
        }

        val fadeInAnimator = ObjectAnimator.ofFloat(answerButtonsContainer, View.ALPHA, 0f, 1f).apply {
            duration = ANSWER_BUTTON_FADE_IN_DURATION
        }
        fadeInAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                answerButtonsContainer.isVisible = true
                enableAnswerButtons()
            }
            override fun onAnimationEnd(animation: Animator) {
                if (!isActivityValid()) return
            }
        })
        fadeInAnimator.start()
    }

    private fun handleAnswerSelection(selectedWord: String) {
        if (!isActivityValid()) return
        disableAnswerButtons()

        val correctAnswerWord = convertNumberToWords(currentNumber)
        val isCorrect = selectedWord.equals(correctAnswerWord, ignoreCase = true)

        showFeedbackAndProceed(isCorrect)
    }

    private fun showFeedbackAndProceed(isCorrect: Boolean) {
        if (!isActivityValid()) return

        feedbackImageView.setImageResource(
            if (isCorrect) R.drawable.correct else R.drawable.wrong
        )
        feedbackImageView.isVisible = true
        feedbackImageView.alpha = 0f

        val feedbackFadeIn = ObjectAnimator.ofFloat(feedbackImageView, View.ALPHA, 0f, 1f).setDuration(FEEDBACK_IMAGE_FADE_DURATION)
        val feedbackVisible = ObjectAnimator.ofFloat(feedbackImageView, View.ALPHA, 1f, 1f).setDuration(FEEDBACK_IMAGE_VISIBLE_DURATION)
        val feedbackFadeOut = ObjectAnimator.ofFloat(feedbackImageView, View.ALPHA, 1f, 0f).setDuration(FEEDBACK_IMAGE_FADE_DURATION)

        val feedbackSequence = AnimatorSet().apply {
            playSequentially(feedbackFadeIn, feedbackVisible, feedbackFadeOut)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isActivityValid()) return
                    feedbackImageView.isVisible = false
                    if (isCorrect) {
                        startNextProblemAnimation()
                    } else {
                        enableAnswerButtons()
                    }
                }
            })
        }
        feedbackSequence.start()
    }

    private fun startNextProblemAnimation() {
        if (!isActivityValid()) return

        val fadeOutElements = allAnimatedElements + listOfNotNull(textViewProblemSignText.parent as? View)
        val animators = fadeOutElements.map { view ->
            ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).setDuration(ITEMS_FADE_OUT_DURATION)
        }

        val fadeOutSet = AnimatorSet()
        fadeOutSet.playTogether(animators)
        fadeOutSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!isActivityValid()) return
                fadeOutElements.forEach { it.isVisible = false }
                prepareForNewProblemAnimation()
                generateNewProblemData()
                startProblemAnimationSequence()
            }
        })
        fadeOutSet.start()
    }


    private fun enableAnswerButtons() {
        answerClickableViews.forEach { it.isEnabled = true; it.isClickable = true }
    }

    private fun disableAnswerButtons() {
        answerClickableViews.forEach { it.isEnabled = false; it.isClickable = false }
    }

    private fun isActivityValid(): Boolean {
        return !isFinishing && !isDestroyed
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}
