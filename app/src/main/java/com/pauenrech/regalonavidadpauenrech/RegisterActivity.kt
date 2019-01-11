package com.pauenrech.regalonavidadpauenrech

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import kotlinx.android.synthetic.main.activity_register.*
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.custom_alert_dialog.view.*


class RegisterActivity : AppCompatActivity() {



    var database : FirebaseDatabase? = null
    var usersRef: DatabaseReference? = null
    var newUserId : String? = null
    var dataGet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        database = FirebaseDatabase.getInstance()
        usersRef = database?.getReference("usuarios")
    }

    fun registrarse(view: View){
        registerBtn.visibility = View.INVISIBLE
        registerSpinner.visibility = View.VISIBLE
        registerNicknameErrorLabel.visibility = View.GONE
        if (registerNickName.text.length < 2){
            registerNicknameErrorLabel.text = getString(R.string.profile_nickname_error_too_short)
            registerNicknameErrorLabel.visibility = View.VISIBLE
            registerSpinner.visibility = View.GONE
            registerBtn.visibility = View.VISIBLE

        }
        else{
            validateWithFirebase(registerNickName.text.toString())
        }
    }

    fun validateWithFirebase(newNickname: String){
        setTimer(2)
        newUserId = usersRef?.push()?.key
        usersRef?.orderByChild("nickname")?.equalTo(newNickname)?.addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataGet == false){
                    if (dataSnapshot.childrenCount > 0){
                        registerNicknameErrorLabel.text = getString(R.string.error_nickname_already_in_use)
                        registerNicknameErrorLabel.visibility = View.VISIBLE
                        registerSpinner.visibility = View.GONE
                        registerBtn.visibility = View.VISIBLE
                    }
                    else{
                        finish()
                    }
                    dataGet = true

                }
            }
            override fun onCancelled(error: DatabaseError) {
                if (dataGet == false) {
                    Toast.makeText(this@RegisterActivity, getString(R.string.error_conexion), Toast.LENGTH_LONG).show()
                    dataGet = true
                    registerSpinner.visibility = View.GONE
                    registerBtn.visibility = View.VISIBLE
                }
                Log.i("TAG", "Failed to read value.", error.toException())
            }
        })
    }

    private fun showCreateListDialog(){

        val dialogLista = AlertDialog.Builder(this).create()

        val customView = LayoutInflater.from(this).inflate(R.layout.custom_alert_dialog,null)
        customView.customDialogTitle.text = getString(R.string.dialog_title_exitApp)
        customView.customDialogMessage.text = getString(R.string.dialog_message_need_to_be_register)

        customView.customDialogPrimaryButton.text = getString(R.string.dialog_exit_button_text)
        customView.customDialogSecundaryButton.text = getString(R.string.dialog_cancel_button_text)
        customView.customDialogPrimaryButton.setOnClickListener {
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
            dialogLista.dismiss()
        }
        customView.customDialogSecundaryButton.setOnClickListener {
            dialogLista.dismiss()
        }
        dialogLista.setView(customView)
        dialogLista.window.setBackgroundDrawable(getDrawable(android.R.color.transparent))
        dialogLista.show()


    }

    fun setTimer(seconds: Int){
        dataGet = false
        val miliseconds = (seconds * 1000).toLong()
        Handler().postDelayed(
            {
                if (!dataGet){
                    Toast.makeText(this,getString(R.string.error_no_conection_no_data),Toast.LENGTH_LONG).show()
                    registerNicknameErrorLabel.visibility = View.GONE
                    registerSpinner.visibility = View.GONE
                    registerBtn.visibility = View.VISIBLE
                    dataGet = true
                }
            },
            miliseconds
        )
    }

    override fun finish() {
        val intentRetorno = Intent()
        intentRetorno.putExtra("nickname",registerNickName.text.toString())
        intentRetorno.putExtra("uid",newUserId)
        setResult(Activity.RESULT_OK,intentRetorno)
        super.finish()
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        showCreateListDialog()
    }
}
