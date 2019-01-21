package com.pauenrech.chancys

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.util.Log

import android.view.View
import kotlinx.android.synthetic.main.activity_profile.*
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

import com.pauenrech.chancys.adapters.profileSpecificListAdapter
import com.pauenrech.chancys.model.User
import com.pauenrech.chancys.layoutManagers.NoScrollLinearLayoutManager

class ProfileActivity : AppCompatActivity() {

    private var layoutManager: NoScrollLinearLayoutManager? = null
    private var adapter: RecyclerView.Adapter<profileSpecificListAdapter.SpecificListViewHolder>? = null

    private var firebaseDatabase: FirebaseDatabase? = null
    private var userRef: DatabaseReference? = null
    private var userNamesRef: DatabaseReference? = null
    private var userNameRef: DatabaseReference? = null

    private var userNameListener: ValueEventListener? = null

    private var userNameHasListener: Boolean = false

    private var userUID: String? = null
    private var userData: User = User()

    var nicknameChanged: Boolean = true

    var imm : InputMethodManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseDatabase = FirebaseDatabase.getInstance()
        userUID = FirebaseAuth.getInstance().uid!!
        userRef = firebaseDatabase!!.getReference("usuarios").child(userUID!!)
        userNamesRef = firebaseDatabase!!.getReference("usernames")
        userNameRef = userRef!!.child("username")

        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        getUserData()
        setUserNamelistener()

        setChangeButtonOnClick()


        profile_toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material)
        profile_toolbar.setTitle(R.string.profile_activity_label)

        profile_toolbar.setNavigationOnClickListener { onBackPressed() }

    }

    fun getUserData(){
        userRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userData = snapshot.getValue(User::class.java)!!
                setUserDataUI(userData)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.i("CONECTION","Error detectando conexion ${error.toException()}")
            }
        })
    }

    fun setUserDataUI(user: User){
        profileNickName.text = user.username
        profile_seekBar.progress = user.dificultad
        profilePuntuacionGlobalText.text = user.puntuacion.toString()

        if (user.ranking == -1)
            profileRanking.text = "-"
        else
            profileRanking.text = user.ranking.toString()

        setSpecificRecyclerView()
    }

    fun setChangeButtonOnClick(){
        profileChangeNicknameBtn.setOnClickListener {
            if (profileNickName.visibility == View.VISIBLE){
                nicknameChanged = false

                profileNickNameEdit.setText(profileNickName.text.toString())
                profileChangeNicknameBtn.setImageResource(R.drawable.ic_round_done_24px_white)

                profileNickName.visibility = View.GONE
                profileNickNameEdit.visibility = View.VISIBLE
                profileNickNameEdit.requestFocus()

                imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }

            else{
                validarTextoNickname()
            }
        }
    }

    fun setSpecificRecyclerView(){
        Log.i("TAG","longitud temas ${userData.mapaTemas[userData.convertDiffToString(0)]!!.size} " +
                "probando con values ${userData.mapaTemas[userData.convertDiffToString(0)]!!.containsKey("geografia")}")
        if (userData.mapaTemas[userData.convertDiffToString(0)]!!.size > 0){
            layoutManager = NoScrollLinearLayoutManager(this)
            adapter = profileSpecificListAdapter(userData.getTemaListPorDificultad(userData.dificultad))

            profileSpecificsRV.adapter = adapter
            profileSpecificsRV.layoutManager = layoutManager
        }
        else{
            profileSpecificScoreLabel.visibility = View.GONE
            profileSpecificCardView.visibility = View.GONE
        }

        setSeekBarListener()
    }

    fun setSeekBarListener(){
        profile_seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                userRef!!.child("dificultad").setValue(i)
                if (profileSpecificCardView.visibility == View.VISIBLE){
                    val recyclerAdapter = profileSpecificsRV.adapter!! as profileSpecificListAdapter
                    recyclerAdapter.changeData(userData.getTemaListPorDificultad(i))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }


    fun validarTextoNickname(){
        if (profileNickNameEdit.text.length < 2){
           profileNicknameErrorLabel.visibility = View.VISIBLE
           profileNicknameErrorLabel.text = getString(R.string.profile_nickname_error_too_short)
        }
        else if (profileNickNameEdit.text.toString() == profileNickName.text.toString()){
           setNicknameNoEditable(true)
        }
        else{
            validateWithFirebase(profileNickNameEdit.text.toString())
        }
    }

    fun setNicknameNoEditable(error: Boolean){
        if (profileNicknameErrorLabel.visibility == View.VISIBLE){
            profileNicknameErrorLabel.visibility = View.GONE
        }

        profileChangeNicknameBtn.visibility = View.VISIBLE
        profileNicknameLoading.visibility = View.GONE

        if (!error) {
            val newUsername = profileNickNameEdit.text.toString()
            val newUsernameLowerCase = newUsername.toLowerCase()
            userRef!!.child("username").setValue(newUsername)
            userNamesRef!!.child(userData.username.toLowerCase()).removeValue()
            userNamesRef!!.child(newUsernameLowerCase).setValue(FirebaseAuth.getInstance().uid)
            //userRef!!.child("usernameLowerCase").setValue(profileNickNameEdit.text.toString().toLowerCase())
        }

        profileChangeNicknameBtn.setImageResource(R.drawable.ic_round_edit_24px_white)

        profileNickName.visibility = View.VISIBLE
        profileNickNameEdit.visibility = View.GONE

        imm?.hideSoftInputFromWindow(profileNickNameEdit.windowToken, 0);

        nicknameChanged = true
    }

    fun validateWithFirebase(newNickname: String){
        profileChangeNicknameBtn.visibility = View.INVISIBLE
        profileNicknameLoading.visibility = View.VISIBLE

        val lowerCaseNickname = newNickname.toLowerCase()

        userNamesRef!!.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (nicknameChanged == false) {
                    if (dataSnapshot.hasChild(lowerCaseNickname)) {
                        profileChangeNicknameBtn.visibility = View.VISIBLE
                        profileNicknameLoading.visibility = View.GONE
                        profileNicknameErrorLabel.text = getString(R.string.error_nickname_already_in_use)
                        profileNicknameErrorLabel.visibility = View.VISIBLE
                    } else {
                        setNicknameNoEditable(false)
                    }
                }
            }
        })

        if (!HomeActivity.conectionState){
            if (nicknameChanged == false) {
                Toast.makeText(this@ProfileActivity, getString(R.string.error_conexion), Toast.LENGTH_LONG).show()
                setNicknameNoEditable(true)
            }
        }
    }

    private fun setUserNamelistener(){
        userNameListener = userNameRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                profileNickName.text = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i("CONECTION","Error detectando conexion ${error.toException()}")
            }
        })
        userNameHasListener = true
    }

    override fun onPause() {
        userNameRef!!.removeEventListener(userNameListener!!)
        userNameHasListener = false

        imm?.hideSoftInputFromWindow(profileNickNameEdit.windowToken, 0);

        super.onPause()
    }

    override fun onResume() {
        if (!userNameHasListener){
            setUserNamelistener()
        }

        super.onResume()
    }

    override fun onBackPressed() {
        if (nicknameChanged == false){
            setNicknameNoEditable(true)
        }
        else{
            super.onBackPressed()
        }
    }

    override fun finish() {
        imm?.hideSoftInputFromWindow(profileNickNameEdit.windowToken, 0);

        super.finish()

        overridePendingTransition(R.anim.slide_from_left,R.anim.slide_to_right)
    }

}
