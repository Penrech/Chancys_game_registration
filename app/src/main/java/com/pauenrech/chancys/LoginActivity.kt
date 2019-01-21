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
import com.pauenrech.chancys.adapters.selectionViewPager
import com.pauenrech.chancys.fragments.LoginWithChancyFragment
import com.pauenrech.chancys.fragments.MainLoginFragment
import com.pauenrech.chancys.fragments.RegisterWithChancyFragment
import com.pauenrech.chancys.tools.CustomDialogAlert
import com.pauenrech.chancys.tools.CustomViewPager

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.*
import java.lang.Exception
import kotlinx.android.synthetic.main.activity_profile.*


class LoginActivity : AppCompatActivity(),
    LoginWithChancyFragment.OnLoginWithChancyInteraction,
    RegisterWithChancyFragment.OnRegisterWithChancyInteraction,
    MainLoginFragment.onMainLoginFragmentInteraction{

    private var mAuth: FirebaseAuth? = null
    private var firebaseDatabase: FirebaseDatabase? = null
    private var allUsersRef: DatabaseReference? = null
    private var conectionRef: DatabaseReference? = null

    private var conectionListener: ValueEventListener? = null

    private var conectionRefHasListener: Boolean = false

    private var conectionState: Boolean = false
    private var nicknameCheck: Boolean = false

    enum class LoginError{
        WRONG_MAIL,WRONG_PASSWORD,WRONG_ACCOUNT, UNKNOW_ERROR
    }

    enum class LoginState{
       Normal, Registering, Logging, LoggingWithPlay
    }
    var state = LoginState.Normal

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
        allUsersRef = firebaseDatabase!!.getReference("usuarios")
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
            Toast.makeText(this,"Le doy a positve",Toast.LENGTH_SHORT).show()
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
        state = LoginState.LoggingWithPlay
        Toast.makeText(this,"Probaria login with play games",Toast.LENGTH_SHORT).show()
    }

    override fun onLoginButtonClick(email: String, password: String) {
        state = LoginState.Logging
        LogInWithMailAndPassword(email,password)
    }

    override fun onToRegisterButtonClick() {
        mPager.currentItem++
    }

    override fun onRegisterButtonClick(email: String, username: String, password: String) {
        state = LoginState.Registering
        Toast.makeText(this,"Mandaria register al server",Toast.LENGTH_SHORT).show()
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
        state = LoginState.Normal
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
                        Log.i("ERROR","Excepcion : ${e.cause} , ${e.message}")
                        manageErrorLoginWithChancy(LoginError.UNKNOW_ERROR)
                    }
                }
            }
    }

    private fun createUserWithMailAndPassword(email: String, username: String, password: String){
        mAuth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "createUserWithEmail:success")
                    val user = mAuth!!.getCurrentUser()
                    updateUI(user)
                } else {

                }
            }
    }

    fun validateWithFirebase(newNickname: String): Boolean{
        nicknameCheck = false
        val lowerCaseNickname = newNickname.toLowerCase()
        var result = false

        allUsersRef!!.orderByChild("usernameLowerCase").equalTo(lowerCaseNickname).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (nicknameCheck == false){
                    result = dataSnapshot.childrenCount <= 0
                }
                nicknameCheck = true
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        return result
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
        if (state != LoginState.Normal){
            Toast.makeText(this,"No se podría pasar ya que el estado es: $state",Toast.LENGTH_SHORT).show()
        }
        else {
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

}
