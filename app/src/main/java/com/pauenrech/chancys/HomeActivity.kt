package com.pauenrech.chancys

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.transition.Scene
import android.support.transition.TransitionInflater
import android.support.transition.TransitionManager

import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.pauenrech.chancys.model.Tema
import com.pauenrech.chancys.model.User

import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_home.*

class HomeActivity : AppCompatActivity() {

    /**
     *
     * @param loadingScene Escena inicial, cuando la app está cargando
     * @param homeScene Escena de la actividad home una vez cargada
     *
     */
    private var loadingScene : Scene? = null
    private var homeScene: Scene? = null

    private var mAuth: FirebaseAuth? = null
    private var firebaseDatabase: FirebaseDatabase? = null
    private var userRef: DatabaseReference? = null
    private var loggedUserPointsRef: DatabaseReference? = null
    private var temasRef: DatabaseReference? = null
    private var preguntasRef: DatabaseReference? = null
    private var conectionRef: DatabaseReference? = null

    private var temasRefListener: ChildEventListener? = null
    private var conectionListener: ValueEventListener? = null
    private var loggedUserPointsListener: ValueEventListener? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private var temasRefHasListener: Boolean = false
    private var conectionRefHasListener: Boolean = false
    private var mAuthHasListener: Boolean = false
    private var loggedUserPointsHasListener: Boolean = false

    private var currentUser: FirebaseUser? = null
    private var userData: User? = null

    private var userUID: String? = null

    /**
     *
     * @param mainUILoaded es un booleano que indica si la UI de home ha sido cargada o si todavía está en
     * la escena loadingScene
     *
     * */
    var mainUILoaded: Boolean = false

    companion object {
        var conectionState: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        homeScene = Scene.getSceneForLayout(rootHome,R.layout.activity_home,this)
        loadingScene = Scene.getSceneForLayout(rootHome,R.layout.loading_home,this)
        loadingScene?.enter()

        rootHome.background = getDrawable(android.R.color.background_light)

        userUID = intent.getStringExtra("userUID")
        if (userUID.isNullOrEmpty())
            goBackToLogin()

        firebaseDatabase = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()


        Log.i("USERID","$userUID")
        userRef = firebaseDatabase!!.reference.child("usuarios").child(userUID!!)
        loggedUserPointsRef = userRef!!.child("puntuacion")
        temasRef = firebaseDatabase!!.reference.child("temas").child("ES_es")
        preguntasRef = firebaseDatabase!!.reference.child("preguntas").child("ES_es")
        conectionRef = firebaseDatabase!!.reference.child(".info/connected")
        userRef!!.child("conectado").onDisconnect().setValue(false)
        preguntasRef!!.keepSynced(true)
        temasRef!!.keepSynced(true)
        userRef!!.keepSynced(true)


        getConexionFromFirebase()
        getUserData()
        setPointsCounterListener()


    }


    private fun getConexionFromFirebase(){
        conectionListener = conectionRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java)!!
                if (connected) {
                    conectionState = connected
                    userRef!!.child("conectado").setValue(true)
                    userRef!!.child("conectado").onDisconnect().setValue(false)
                    Log.i("CONECTION","Conectado")
                } else {
                    conectionState = connected
                    userRef!!.child("conectado").setValue(false)
                    Log.i("CONECTION","No conectado")
                }
                if (!mainUILoaded){
                    loadHomeUi()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        conectionRefHasListener = true
    }

    private fun setAuthListener(){
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.i("TAG","user: $user")
            if (user == null) {
               goBackToLogin()
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener!!)
        mAuthHasListener = true
    }

    private fun getUserData(){
        userRef!!.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                if (!mainUILoaded){
                    loadHomeUi()
                }
                for (i in snapshot.children){
                    Log.i("BUCLE","Elemento bucle, key: ${i.key} valor ${i.value}")
                }
                userData = snapshot.getValue(User::class.java)!!
                getTemasFromFirebase()
            }
        })
    }

    private fun getTemasFromFirebase(){

        temasRef!!.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                if (!mainUILoaded){
                    loadHomeUi()
                }
                val listaTemas = mutableListOf<Tema>()
                for (tema in snapshot.children){
                    val tempTema = tema.getValue(Tema::class.java)
                    listaTemas.add(tempTema!!)
                }
                userData!!.addOrUpdateTemas(listaTemas)
                userRef!!.child("temas").updateChildren(userData!!.getTemasParaSubirAFirebase())
            }
        })

        /*
        temasRefListener = temasRef!!.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                val tema = p0.getValue(Tema::class.java)
                userData.addTemaLista(tema!!.name,tema.id)
                userRef!!.child("temas").setValue(userData.temas)
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val tema = p0.getValue(Tema::class.java)
                userData.addTemaLista(tema!!.name,tema.id)
                userRef!!.child("temas").setValue(userData.temas)
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                val tema = p0.getValue(Tema::class.java)
                val list = mutableListOf(tema!!)
                userData.deleteTema(list)
                userRef!!.child("temas").setValue(userData.temas)
            }

        })
        temasRefHasListener = true*/
    }

    private fun setPointsCounterListener(){
        Log.i("TAG","Setpointscounter llamado")
        loggedUserPointsListener = loggedUserPointsRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!mainUILoaded){
                    loadHomeUi()
                }
                val puntuacion = snapshot.value
               homeHighScore.text = "$puntuacion"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        loggedUserPointsHasListener = true
    }

    private fun goBackToLogin(){
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showProfile(){
        val intent = Intent(this,ProfileActivity::class.java)
        startActivity(intent)

        overridePendingTransition(R.anim.slide_from_right,R.anim.slide_to_left)
    }

    fun loadGameSelectionActivity(view: View){
        val singleListener = temasRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
               val numeroDeTemas = snapshot.childrenCount
                if (numeroDeTemas < 0){
                    setErrorResetAppSnackbar()
                }
                else{
                    val intent = Intent(this@HomeActivity,SelectionActivity::class.java)
                    startActivity(intent)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadHomeUi(){
        rootHome.background = getDrawable(R.drawable.gradient_background)

        val transition = TransitionInflater.from(this).inflateTransition(R.transition.no_transition)
        transition.duration = 0
        TransitionManager.go(homeScene!!,transition)

        setSupportActionBar(home_toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        mainUILoaded = true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                showProfile()
                true
            }
            R.id.action_ranking ->{
                Toast.makeText(this,getString(R.string.development_not_implemented),Toast.LENGTH_LONG).show()
                true
            }
            R.id.action_credentials ->{
                Toast.makeText(this,getString(R.string.development_not_implemented),Toast.LENGTH_LONG).show()
                true
            }
            R.id.action_logout ->{
                userRef!!.child("conectado").setValue(false)
                FirebaseAuth.getInstance().signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setErrorResetAppSnackbar(){
        val snack = Snackbar.make(rootHome,getString(R.string.error_no_game_data),Snackbar.LENGTH_LONG)

        val snackbarView = snack.view
        snackbarView.setBackgroundColor(getColor(R.color.colorPrimary))

        val snackTextView = snackbarView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        snackTextView.setTextColor(getColor(android.R.color.background_light))

        snack.setActionTextColor(getColor(android.R.color.background_light))
        snack.setAction(getString(R.string.error_restart_app)) {
            val i = baseContext.packageManager
                .getLaunchIntentForPackage(baseContext.packageName)
            i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
        }
        snack.show()
    }

    override fun onStart() {
        setAuthListener()
        super.onStart()
    }

    override fun onPause() {
        FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener!!)
        mAuthHasListener = false

        loggedUserPointsRef!!.removeEventListener(loggedUserPointsListener!!)
        loggedUserPointsHasListener = false

      /* temasRef!!.removeEventListener(temasRefListener!!)
        temasRefHasListener = false
*/
        super.onPause()
    }


    override fun onResume() {
        if (!conectionRefHasListener){
            getConexionFromFirebase()
        }
        if (!mAuthHasListener){
            setAuthListener()
        }
        if (!loggedUserPointsHasListener){
            setPointsCounterListener()
        }
       /*if (!temasRefHasListener){
            getTemasFromFirebase()
        }*/
        super.onResume()
    }

    override fun onDestroy() {
        conectionRef!!.removeEventListener(conectionListener!!)
        conectionRefHasListener = false
        super.onDestroy()
    }
}
