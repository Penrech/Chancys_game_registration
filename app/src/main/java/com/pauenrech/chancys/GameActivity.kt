package com.pauenrech.chancys

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.gesture.GestureLibraries
import android.gesture.GestureLibrary
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.transition.TransitionManager
import android.view.View
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.fragment_game_controller.*
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.transition.Transition
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pauenrech.chancys.adapters.selectionViewPager
import com.pauenrech.chancys.fragments.*
import com.pauenrech.chancys.model.Pregunta
import com.pauenrech.chancys.model.PreguntasTema
import com.pauenrech.chancys.model.User
import com.pauenrech.chancys.tools.CustomDialogAlert
import com.pauenrech.chancys.tools.CustomViewPager
import com.pauenrech.chancys.tools.WaitSecondsAsyncTask
import kotlinx.android.synthetic.main.custom_alert_dialog.view.*
import kotlinx.android.synthetic.main.fragment_end_game.*
import kotlinx.android.synthetic.main.fragment_start_game.*


class GameActivity : AppCompatActivity(),
    StartGameFragment.clickListener,
    EndGameFragment.clickListener,
    QuestionFragment.gestureDetectorListener,
    WaitSecondsAsyncTask.TaskCompleted{

    private lateinit var mPager: CustomViewPager

    enum class TypeAnswer{
        Timeout, Correct, Incorrect, StartGame
    }

    enum class GameState{
        PreInit, Running, Paused, Finish
    }

    enum class TimerState{
        Stopped, Paused, Running
    }

    companion object {
        var gestureLibrary: GestureLibrary? = null
    }

    var textTransition : Transition? = null

    var asyncTask: WaitSecondsAsyncTask? = null

    var controlFragmentReference: GameControllerFragment? = null

    private var firebaseDatabase: FirebaseDatabase? = null
    private var preguntasRef: DatabaseReference? = null
    private var userRef: DatabaseReference? = null

    var timer : CountDownTimer? = null
    var timerState = TimerState.Stopped
    var gameState = GameState.PreInit

    var segundosQueQuedan: Long = 5
    var pointsGained: Int = 0
    var highScore: Int = -1
    var actualPosition: Int = 0

    var fragmentsList: MutableList<Fragment> = mutableListOf()
    var pagerAdapter: selectionViewPager? = null

    var questionAnswersList: MutableList<Boolean> = mutableListOf()
/*
    var userReference = HomeActivity.userData
    var preguntasReference = HomeActivity.preguntasData.listaPreguntasTotal.totalPreguntas*/

    var userData: User = User()
    var preguntasData: PreguntasTema = PreguntasTema()

    var temaID : String? = null
    var dificultadText: String? = null

    var startFragment: Fragment? = null
    var endFragment: Fragment? = null

    var valorPregunta: Int? = null
    var preguntaContestada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        controlFragmentReference = gameControllerFragment as GameControllerFragment
        cargarGestos()

        temaID = intent.getStringExtra("temaID")
        val title = intent.getStringExtra("title")
        val startColorString = intent.getStringExtra("startColor")
        val endColorString = intent.getStringExtra("endColor")
        val dificultad = intent.getIntExtra("dificultad",0)
        when(dificultad){
            0 -> dificultadText = "facil"
            1 -> dificultadText = "medio"
            else -> dificultadText = "dificil"
        }

        firebaseDatabase = FirebaseDatabase.getInstance()
        userRef = firebaseDatabase!!.getReference("usuarios").child(FirebaseAuth.getInstance().uid!!)
        preguntasRef = firebaseDatabase!!.getReference("preguntas").child("Es_es").child(dificultadText!!)

        val startColor = Color.parseColor(startColorString)
        val endColor = Color.parseColor(endColorString)
        val colorsArray = intArrayOf(startColor, endColor)

        val backgroundGradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colorsArray)
        backgroundGradientDrawable.cornerRadius = 0f
        rootGame.background = backgroundGradientDrawable

        window.navigationBarColor = endColor

        controlFragmentReference?.gameControllerRatingBar?.rating = (pointsGained / 2f)

        updateCountdownUI()

        mPager = findViewById(R.id.gamePager)
        mPager.setPagingEnabled(false)

        mPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}


            override fun onPageSelected(position: Int) {
                if (position > 0 && position < 11){
                    hideCorrectionLabel()
                }
                if (position <= 10){
                    preguntaContestada = false
                    actualPosition = position
                    controlFragmentReference?.gameControllerPreguntas?.text = "$position/10"
                }
                if (position > 10){
                    actualPosition = position

                    timerState = TimerState.Stopped
                    gameState = GameState.Finish

                    lastPageLabel()
                }
            }
        })

        textTransition = TransitionInflater.from(this)
            .inflateTransition(R.transition.correction_text_appear)

        game_toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material)
        game_toolbar.setTitle(title)

        game_toolbar.setNavigationOnClickListener { onBackPressed() }

        //fillPageViewer()

        getUserData()
        getQuestions()
    }

    fun getUserData(){
        userRef!!.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
               userData = p0.getValue(User::class.java)!!
            }
        })
    }

    fun getQuestions(){
        preguntasRef!!.orderByChild("id_tema").equalTo(temaID).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.childrenCount < 10){
                    Toast.makeText(this@GameActivity, getString(R.string.error_loading_gestures), Toast.LENGTH_LONG).show()
                    finish()
                }
                else {
                    val preguntasTema = PreguntasTema()
                    for (pregunta in p0.children) {
                        val preguntaTemp = pregunta.getValue(Pregunta::class.java)
                        preguntasTema.preguntas.add(preguntaTemp!!)
                    }
                    preguntasData = preguntasTema
                    fillPageViewer()
                }
            }
        })
    }

    fun cargarGestos() {
        gestureLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures)

        if (gestureLibrary?.load() == false) {
            Toast.makeText(this, getString(R.string.error_loading_gestures), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    fun fillPageViewer(){
        startFragment = StartGameFragment()

        fragmentsList.add(startFragment!!)

        val listaPreguntasAleatorias = preguntasData.getRandomQuestions()

        listaPreguntasAleatorias.forEachIndexed { index, pregunta ->
            if (index == 0){
                valorPregunta = pregunta.puntuacion
            }

            val randomAnswer = pregunta.getRandomAnswer()
            questionAnswersList.add(randomAnswer.second)

            fragmentsList.add(QuestionFragment.newInstance(pregunta.id!!,randomAnswer.first,randomAnswer.second,index + 1))
        }

        endFragment = EndGameFragment()
        fragmentsList.add(endFragment!!)

        pagerAdapter = selectionViewPager(supportFragmentManager,fragmentsList)
        mPager.adapter = pagerAdapter
    }

    fun startTimer(){
        timerState = TimerState.Running
        gameState = GameState.Running

        timer = object : CountDownTimer(segundosQueQuedan * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                segundosQueQuedan = millisUntilFinished / 1000
                updateCountdownUI()
            }

            override fun onFinish() = onTimerFinished()
        }.start()
    }

    fun updateCountdownUI(){
        if (segundosQueQuedan < 10){
            controlFragmentReference?.gameControllerTimer?.text = "00:0$segundosQueQuedan"
        }
        else{
            controlFragmentReference?.gameControllerTimer?.text = "00:$segundosQueQuedan"
        }
    }

    fun updateStarRatingUI(){
        pointsGained++
        controlFragmentReference?.gameControllerRatingBar?.rating = (pointsGained / 2f)
    }

    fun onTimerFinished(){
        timerState = TimerState.Paused

        if (actualPosition == 0){
            showCorrectionLabel(R.string.counter_start_game,TypeAnswer.StartGame)
        }
        else{
            showCorrectionLabel(R.string.counter_timeout,TypeAnswer.Timeout)
        }

        wait1secondAndPass()
    }

    fun onUserAswer(isUserRight: Boolean){
        if (timerState == TimerState.Running && gameState == GameState.Running){
            timerState = TimerState.Paused
            timer?.cancel()

            if (isUserRight) {
                showCorrectionLabel(R.string.counter_correct,TypeAnswer.Correct)
                updateStarRatingUI()
            }
            else{
                showCorrectionLabel(R.string.counter_incorrect,TypeAnswer.Incorrect)
            }

            wait1secondAndPass()

            preguntaContestada = true
        }
    }

    fun showCorrectionLabel(message: Int, answerType: TypeAnswer){
        when(answerType){
            TypeAnswer.Timeout ->{
                controlFragmentReference?.gameControllerTimer?.setTextColor(getColor(android.R.color.background_light))
                controlFragmentReference?.gameControllerCorrection?.setTextColor(getColor(android.R.color.background_light))
            }
            TypeAnswer.Correct ->{
                controlFragmentReference?.gameControllerTimer?.setTextColor(getColor(R.color.colorGradientEnd))
                controlFragmentReference?.gameControllerCorrection?.setTextColor(getColor(R.color.colorGradientEnd))
            }
            TypeAnswer.Incorrect ->{
                controlFragmentReference?.gameControllerTimer?.setTextColor(getColor(R.color.colorNoTick))
                controlFragmentReference?.gameControllerCorrection?.setTextColor(getColor(R.color.colorNoTick))
            }
            TypeAnswer.StartGame ->{
                controlFragmentReference?.gameControllerTimer?.setTextColor(getColor(android.R.color.background_light))
                controlFragmentReference?.gameControllerCorrection?.setTextColor(getColor(android.R.color.background_light))
            }
        }

        TransitionManager.beginDelayedTransition(rootGame,textTransition)

        controlFragmentReference?.gameControllerCorrection?.text = getString(message)
        controlFragmentReference?.gameControllerCorrection?.visibility = View.VISIBLE

        val params = controlFragmentReference?.gameControllerTimer?.getLayoutParams() as ConstraintLayout.LayoutParams
        params.horizontalBias = 0f
        controlFragmentReference?.gameControllerTimer?.setLayoutParams(params)
    }

    fun hideCorrectionLabel(){
        controlFragmentReference?.gameControllerTimer?.setTextColor(getColor(android.R.color.background_light))
        controlFragmentReference?.gameControllerCorrection?.setTextColor(getColor(android.R.color.background_light))

        TransitionManager.beginDelayedTransition(rootGame,textTransition)

        controlFragmentReference?.gameControllerCorrection?.visibility = View.GONE

        val params = controlFragmentReference?.gameControllerTimer?.getLayoutParams() as ConstraintLayout.LayoutParams
        params.horizontalBias = 0.5f
        controlFragmentReference?.gameControllerTimer?.setLayoutParams(params)

        segundosQueQuedan = 10

        updateCountdownUI()

        startTimer()
    }

    fun lastPageLabel(){
        controlFragmentReference?.gameControllerTimer?.setTextColor(getColor(android.R.color.background_light))
        controlFragmentReference?.gameControllerCorrection?.setTextColor(getColor(android.R.color.background_light))

        segundosQueQuedan = 0

        updateCountdownUI()

        TransitionManager.beginDelayedTransition(rootGame,textTransition)

        endFragment?.endGameCardRatingBar?.rating = (pointsGained / 2f)
        endFragment?.endGameCorrectAnswers?.text = "$pointsGained/10"

        val fragmentRef = fragmentsList[actualPosition]
        fragmentRef.endGameCardRatingBar.rating = (pointsGained / 2f)

        controlFragmentReference?.gameControllerCorrection?.text = getString(R.string.game_gameover_text)
        controlFragmentReference?.gameControllerCorrection?.visibility = View.VISIBLE

        val params = controlFragmentReference?.gameControllerTimer?.getLayoutParams() as ConstraintLayout.LayoutParams
        params.horizontalBias = 0f
        controlFragmentReference?.gameControllerTimer?.setLayoutParams(params)

        setUserPoints(fragmentRef)
    }

    fun setUserPoints(fragment: Fragment){
        val oldScore = userData.getTemaScore(temaID!!)

        if(oldScore < pointsGained){
            TransitionManager.beginDelayedTransition(rootGame,textTransition)

            fragment.endGameNewRecordGroup.visibility = View.VISIBLE

            val diff = pointsGained - oldScore
            val points = diff * valorPregunta!!
            fragment.endGamePointsObtined.text = "+$points"

            userData.modifyTemaScore(temaID!!,pointsGained)
            userData.puntuacion += points

            userRef!!.child("puntuacion").setValue(userData.puntuacion)
            userRef!!.child("temas").setValue(userData.temas)

            highScore = pointsGained
        }
    }

    fun wait1secondAndPass(){
        asyncTask = WaitSecondsAsyncTask(this)
        asyncTask!!.execute(1)
    }

    fun createDialog(){
        pauseGameAndTimer()

        if (asyncTask?.status == AsyncTask.Status.RUNNING || asyncTask?.status == AsyncTask.Status.RUNNING){
            asyncTask?.cancel(true)
        }

        val dialog = CustomDialogAlert(this)
        dialog.setTitle(R.string.custom_dialog_leave_game)
        dialog.setMessage(R.string.custom_dialog_leave_game_message)
        dialog.setPossitiveButton(R.string.dialog_exit_button_text,View.OnClickListener {
            finish()
            dialog.dismiss()
        })
        dialog.setNegativeButton(R.string.dialog_cancel_button_text,View.OnClickListener {
            resumeGameAndTimer()
            dialog.dismiss()
        })
        dialog.setCancelCallback(DialogInterface.OnCancelListener {
            resumeGameAndTimer()
        })
        dialog.show()
    }

    fun pauseGameAndTimer(){
        if (timerState == TimerState.Running){
            timer?.cancel()
            timerState = TimerState.Paused
        }
        if (gameState != GameState.PreInit){
            gameState = GameState.Paused
        }
    }

    fun resumeGameAndTimer(){
        if (timerState == TimerState.Paused && gameState == GameState.Paused){
            if (preguntaContestada){
                timerState = TimerState.Running
                gameState = GameState.Running

                wait1secondAndPass()
            }
            else{
                segundosQueQuedan = 0

                updateCountdownUI()

                startTimer()
            }
        }
        else{
            if (gameState != GameState.PreInit){
                startTimer()
            }
        }
    }

    override fun onEndGameButtonClick() = finish()

    override fun onStartGameButtonClick() {
        if (gameState == GameState.PreInit){
            TransitionManager.beginDelayedTransition(rootGame,textTransition)

            startFragment?.startGameButton?.visibility = View.INVISIBLE
            startFragment?.startGameSpinner?.visibility = View.VISIBLE

            gameState = GameState.Running

            segundosQueQuedan = 5

            startTimer()
        }
    }

    override fun onGestureDetected(isTrue: Boolean) = onUserAswer(isTrue)

    override fun asyncTaskCompleted() {
        if (gameState != GameState.Paused) {
            gameState = GameState.Running
            timerState = TimerState.Running

            mPager.currentItem = actualPosition + 1
        }
    }

    override fun onPause() {
        pauseGameAndTimer()

        super.onPause()
    }

    override fun onResume() {
        resumeGameAndTimer()

        super.onResume()
    }

    override fun onBackPressed() {
        if (gameState == GameState.Finish || gameState == GameState.PreInit){
            super.onBackPressed()
        }
        else{
            createDialog()
        }
    }

    override fun finish() {
        val intentRetorno = Intent()
        intentRetorno.putExtra("temaID",temaID)
        intentRetorno.putExtra("score",highScore)
        setResult(Activity.RESULT_OK,intentRetorno)

        super.finish()

        overridePendingTransition(R.anim.slide_from_left,R.anim.slide_to_right)
    }
}


