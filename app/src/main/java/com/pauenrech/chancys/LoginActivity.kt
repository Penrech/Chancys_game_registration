package com.pauenrech.chancys

import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog

import android.util.Log
import android.view.LayoutInflater

import android.view.View
import android.widget.Toast
import com.google.firebase.auth.*
import com.pauenrech.chancys.adapters.selectionViewPager
import com.pauenrech.chancys.fragments.LoginWithChancyFragment
import com.pauenrech.chancys.fragments.MainLoginFragment
import com.pauenrech.chancys.fragments.RegisterWithChancyFragment
import com.pauenrech.chancys.tools.CustomDialogAlert
import com.pauenrech.chancys.tools.CustomViewPager

import com.google.firebase.database.*
import com.pauenrech.chancys.model.User
import java.lang.Exception
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn

import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseUser

import com.google.firebase.auth.PlayGamesAuthProvider

import com.google.android.gms.auth.api.signin.GoogleSignInAccount








class LoginActivity : AppCompatActivity(),
    LoginWithChancyFragment.OnLoginWithChancyInteraction,
    RegisterWithChancyFragment.OnRegisterWithChancyInteraction,
    MainLoginFragment.onMainLoginFragmentInteraction{

    private var mAuth: FirebaseAuth? = null
    private var firebaseDatabase: FirebaseDatabase? = null
    private var userNamesRef: DatabaseReference? = null
    private var usersRef: DatabaseReference? = null
    private var conectionRef: DatabaseReference? = null

    private var conectionListener: ValueEventListener? = null

    private var conectionRefHasListener: Boolean = false

    private var conectionState: Boolean = false

    private var usernameCheck: Boolean = false

    private val RC_SIGN_IN = 100

    enum class LoginError{
        WRONG_MAIL,WRONG_PASSWORD,WRONG_ACCOUNT, UNKNOW_ERROR
    }

    enum class RegisterError{
        EXISTING_MAIL, WEAK_PASSWORD, EXISTING_USERNAME, UNKNOW_ERROR
    }

    enum class LoginWithPlayGamesError{
        ERROR_LOGIN_PLAY_GAMES, ERROR_REGISTERING_TO_FIREBASE
    }


    private lateinit var mPager: CustomViewPager
    var pagerAdapter: selectionViewPager? = null
    var position: Int = 0

    var loadingDialog: AlertDialog? = null

    var mainFragment: MainLoginFragment? = null
    var loginFragment: LoginWithChancyFragment? = null
    var registerFragment: RegisterWithChancyFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mPager = findViewById(R.id.loginViewPager)
        mPager.setPagingEnabled(false)

        mAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        try {
            firebaseDatabase!!.setPersistenceEnabled(true)
        } catch (e: DatabaseException){
            e.printStackTrace()
        }
        userNamesRef = firebaseDatabase!!.getReference("usernames")
        usersRef = firebaseDatabase!!.getReference("usuarios")
        conectionRef = firebaseDatabase!!.reference.child(".info/connected")

        fillViewPager()

        adapterChangeListener()

        /*database = FirebaseDatabase.getInstance()
        usersRef = database?.getReference("usuarios")*/
    }

    private fun fillViewPager(){
        mainFragment = MainLoginFragment()
        loginFragment = LoginWithChancyFragment()
        registerFragment = RegisterWithChancyFragment()

        val fragmentList = listOf(mainFragment!!,loginFragment!!,registerFragment!!)

        pagerAdapter = selectionViewPager(supportFragmentManager,fragmentList)
        mPager.adapter = pagerAdapter
    }

    private fun adapterChangeListener(){
        mPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                this@LoginActivity.position = position
            }
        })
    }

    private fun showCreateListDialog(){

        val dialog = CustomDialogAlert(this)
        dialog.setTitle(R.string.dialog_title_exitApp)
        dialog.setMessage(R.string.dialog_message_need_to_be_register)
        dialog.setPossitiveButton(R.string.dialog_exit_button_text,View.OnClickListener {
            finish()
            dialog.dismiss()
        })
        dialog.setNegativeButton(R.string.dialog_cancel_button_text, View.OnClickListener {
            dialog.dismiss()
        })
        dialog.setCancelCallback(DialogInterface.OnCancelListener {
            Log.i("DISMISS","Canceled")
        })
        dialog.show()

    }

    override fun onLoginWithChancyClick() {
        mPager.currentItem++
    }

    override fun onLoginWithPlayGamesClick() {
        loginWithPlayGames()

    }

    override fun onLoginButtonClick(email: String, password: String) {
        //state = LoginState.Logging
        LogInWithMailAndPassword(email,password)
    }

    override fun onToRegisterButtonClick() {
        mPager.currentItem++
    }

    override fun onRegisterButtonClick(email: String, username: String, password: String) {
        validateWithFirebase(email,username,password)
    }

    private fun updateUI(currentUser: FirebaseUser?){
        if (currentUser != null){
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("userUID",currentUser.uid)
            startActivity(intent)
            stopLoadingDialog()
            finish()

            overridePendingTransition(0,0)
        }
    }

    private fun startLoadingDialog(){
        loadingDialog = AlertDialog.Builder(this).create()

        val customView = LayoutInflater.from(this).inflate(R.layout.loading_login,null)
        loadingDialog?.setView(customView)
        loadingDialog?.setCancelable(false)

        loadingDialog?.window?.setBackgroundDrawable(getDrawable(android.R.color.transparent))
        loadingDialog?.show()
    }

    private fun stopLoadingDialog(){
        if (loadingDialog != null){
            loadingDialog!!.dismiss()
        }
    }

    private fun manageErrorLoginWithChancy(errorType: LoginError){
        loginFragment?.initialState()
        when(errorType){
            LoginError.WRONG_ACCOUNT ->{
                loginFragment?.errorWithEmail("Email y/o contraseña incorrectos")
            }
            LoginError.WRONG_PASSWORD ->{
                loginFragment?.errorWithPassword("Contraseña incorrecta")
            }
            LoginError.WRONG_MAIL ->{
                loginFragment?.errorWithEmail("Email incorrecto")
            }
            else ->{
                loginFragment?.initialState()
                Toast.makeText(this,"Error iniciando sesión",Toast.LENGTH_LONG).show()
            }
        }
        //state = LoginState.Normal
        stopLoadingDialog()
    }

    private fun LogInWithMailAndPassword(email: String, password: String){
        startLoadingDialog()
        mAuth!!.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Log.i("TAG", "signInWithEmail:success")
                    val user = mAuth!!.currentUser
                    updateUI(user)
                }else{
                    try {
                        throw task.exception?.fillInStackTrace()!!
                    } catch (e: FirebaseAuthInvalidUserException){
                        manageErrorLoginWithChancy(LoginError.WRONG_ACCOUNT)
                    } catch (e: FirebaseAuthInvalidCredentialsException){
                        manageErrorLoginWithChancy(LoginError.WRONG_PASSWORD)
                    } catch (e: Exception){
                        Log.i("ERROR","Excepcion loggeando : ${e.cause} , ${e.message}")
                        manageErrorLoginWithChancy(LoginError.UNKNOW_ERROR)
                    }
                }
            }
    }

    private fun manageErrorRegisteringWithChancy(errorType: RegisterError){
        registerFragment?.initialState()
        when(errorType){
            RegisterError.EXISTING_MAIL ->{
                registerFragment?.errorWithEmail("Email ya registrado")
            }
            RegisterError.EXISTING_USERNAME ->{
                registerFragment?.errorWithUsername(getString(R.string.error_nickname_already_in_use))
            }
            RegisterError.WEAK_PASSWORD ->{
                registerFragment?.errorWithPassword(getString(R.string.form_password_too_short))
            }
            else -> {
                registerFragment?.initialState()
                Toast.makeText(this,"Error registrando usuario",Toast.LENGTH_LONG).show()
            }
        }
        stopLoadingDialog()
    }

    private fun createUserWithMailAndPassword(email: String, username: String, password: String){
        mAuth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Log.d("TAG", "createUserWithEmail:success")
                    val user = mAuth!!.getCurrentUser()
                    createUserWithRegisterData(user!!,username)
                } else {
                    try {
                        throw task.exception?.fillInStackTrace()!!
                    } catch (e: FirebaseAuthUserCollisionException){
                        manageErrorRegisteringWithChancy(RegisterError.EXISTING_MAIL)
                    } catch (e: FirebaseAuthWeakPasswordException){
                        manageErrorRegisteringWithChancy(RegisterError.WEAK_PASSWORD)
                    } catch (e: Exception){
                        Log.i("ERROR","Excepcion registrando: ${e.cause} , ${e.message}")
                        manageErrorRegisteringWithChancy(RegisterError.UNKNOW_ERROR)
                    }
                }
            }
    }

    private fun loginWithPlayGames(){
        startLoadingDialog()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .requestServerAuthCode(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        val signInClient = GoogleSignIn.getClient(
            this,
            gso
        )
        val intent = signInClient.signInIntent
        startActivityForResult(intent, RC_SIGN_IN)
    }

    private fun firebaseAuthWithPlayGames(acct: GoogleSignInAccount) {

        val credential = PlayGamesAuthProvider.getCredential(acct.serverAuthCode!!)
        val email = acct.email
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("TAG", "signInWithCredential:success")
                    val user = mAuth!!.getCurrentUser()
                    checkUsuarioEnDatabase(user!!,email!!)
                } else {
                    manageErrorLoginWithPlayGames(LoginWithPlayGamesError.ERROR_REGISTERING_TO_FIREBASE)
                }

            }
    }

    private fun checkUsuarioEnDatabase(user: FirebaseUser, googleEmail: String){
        var checked = false
        usersRef!!.child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(p0: DataSnapshot) {
                if (!checked){
                    if (p0.exists()){
                        updateUI(user)
                    }
                    else{
                        formatUserToDatabase(user,googleEmail)
                    }
                    checked = true
                }
            }
        })/*
        userNamesRef!!.orderByValue().equalTo(user.uid).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (checked == false) {
                    if (dataSnapshot.childrenCount > 0) {
                        updateUI(user)
                    } else {
                        formatUserToDatabase(user, googleEmail)
                    }
                    checked = true
                }
            }
        })*/
        if (!conectionState){
            if (!checked){
                manageErrorLoginWithPlayGames(LoginWithPlayGamesError.ERROR_REGISTERING_TO_FIREBASE)
                checked = true
            }
        }
    }

    private fun formatUserToDatabase(user: FirebaseUser, email: String){
        val newUser = User()
        newUser.email = email
        newUser.type = "play_games"
        newUser.uid = user.uid
        var username = email.substringBefore('@')
        if (username.length >= 12)
            username = username.slice(0 until 12)
        newUser.username = username

        checkAndRepeatAddingUser(newUser,user,1)

    }

    private fun checkAndRepeatAddingUser(user: User, firebaseUser: FirebaseUser, pruebaNum: Int){
        var check = false
        userNamesRef?.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(datasnapShot: DataSnapshot) {
                if (!check) {
                    if (datasnapShot.hasChild(user.username)) {
                        if (user.username.length <= 12){
                            val newUsername = user.username + pruebaNum
                            user.username = newUsername
                        }else{
                            var newUsername = user.username.slice(0 until 12)
                            newUsername += pruebaNum
                            user.username = newUsername
                        }
                        checkAndRepeatAddingUser(user,firebaseUser,pruebaNum + 1)
                    } else {
                        addNewPlayGamesUserDataToDatabase(user, firebaseUser)
                    }
                    check = true
                }
            }
        })
        if (!conectionState){
            if (!check){
                manageErrorLoginWithPlayGames(LoginWithPlayGamesError.ERROR_REGISTERING_TO_FIREBASE)
                check = true
            }
        }
    }

    private fun addNewPlayGamesUserDataToDatabase(user: User, firebaseUser: FirebaseUser){
        usersRef!!.child(user.uid).setValue(user)
            .addOnSuccessListener {
                userNamesRef!!.child(user.username).setValue(user.uid)
                    .addOnSuccessListener {
                        updateUI(firebaseUser)
                    }
                    .addOnFailureListener {
                        FirebaseAuth.getInstance().currentUser?.delete()
                        manageErrorLoginWithPlayGames(LoginWithPlayGamesError.ERROR_REGISTERING_TO_FIREBASE)
                    }
            }
            .addOnFailureListener {
                FirebaseAuth.getInstance().currentUser?.delete()
                manageErrorLoginWithPlayGames(LoginWithPlayGamesError.ERROR_REGISTERING_TO_FIREBASE)
            }

    }

    fun manageErrorLoginWithPlayGames(errorType: LoginWithPlayGamesError){
        when(errorType){
            LoginWithPlayGamesError.ERROR_LOGIN_PLAY_GAMES ->{
                Toast.makeText(this,"Error entrando en cuenta de Play Games",Toast.LENGTH_LONG).show()
            }
            LoginWithPlayGamesError.ERROR_REGISTERING_TO_FIREBASE ->{
                Toast.makeText(this,"Error registrando cuenta de Play Games en Chancys",Toast.LENGTH_LONG).show()
            }
        }
        stopLoadingDialog()
    }

    fun createUserWithRegisterData(user: FirebaseUser,username: String){
        val newUser = User()
        newUser.email = user.email!!
        newUser.username = username
        newUser.uid = user.uid
        usersRef!!.child(user.uid).setValue(newUser)
            .addOnSuccessListener {
                userNamesRef!!.child(username).setValue(user.uid)
                    .addOnSuccessListener {
                        updateUI(user)
                    }
                    .addOnFailureListener {
                        FirebaseAuth.getInstance().currentUser?.delete()
                        manageErrorRegisteringWithChancy(RegisterError.UNKNOW_ERROR)
                    }
            }
            .addOnFailureListener {
                FirebaseAuth.getInstance().currentUser?.delete()
                manageErrorRegisteringWithChancy(RegisterError.UNKNOW_ERROR)
            }

    }

    fun validateWithFirebase(email: String, username: String, password: String){
        startLoadingDialog()
        usernameCheck = false
        val lowerCaseNickname = username.toLowerCase()
        var result = false

        userNamesRef!!.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!usernameCheck) {
                    result = dataSnapshot.hasChild(lowerCaseNickname)
                    if (result){
                        manageErrorRegisteringWithChancy(RegisterError.EXISTING_USERNAME)
                    }
                    else{
                        createUserWithMailAndPassword(email,username,password)
                    }
                }
                usernameCheck = true
            }
        })
        if (!conectionState){
            if (!usernameCheck){
                manageErrorRegisteringWithChancy(RegisterError.UNKNOW_ERROR)
                usernameCheck = true
            }
        }
    }

    private fun getConexionFromFirebase(){
        conectionListener = conectionRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java)!!
                if (connected) {
                    conectionState = connected
                    Log.i("CONECTION","Conectado")
                } else {
                    conectionState = connected
                    Log.i("CONECTION","No conectado")
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        conectionRefHasListener = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val signedInAccount = result.signInAccount
                Log.i("PLAYGAMES","cuenta $signedInAccount , ${signedInAccount!!.email}")
                firebaseAuthWithPlayGames(signedInAccount!!)
            } else {
                Log.i("PLAYGAMES","Error ${result.status.statusCode}")
               manageErrorLoginWithPlayGames(LoginWithPlayGamesError.ERROR_LOGIN_PLAY_GAMES)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth?.currentUser
        updateUI(currentUser)
    }

    override fun onResume() {
        if (!conectionRefHasListener){
            getConexionFromFirebase()
        }
        super.onResume()
    }

    override fun onPause() {
        conectionRef!!.removeEventListener(conectionListener!!)
        conectionRefHasListener = false
        super.onPause()
    }

    override fun onBackPressed() {
        if (mPager.currentItem == 0) {
            showCreateListDialog()
        } else {
            when (mPager.currentItem) {
                1 -> loginFragment?.clearAll()
                2 -> registerFragment?.clearAll()
            }
            mPager.currentItem = mPager.currentItem - 1
        }
    }

}
