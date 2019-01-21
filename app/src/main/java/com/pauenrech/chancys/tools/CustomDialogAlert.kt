package com.pauenrech.chancys.tools

import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import com.pauenrech.chancys.R
import kotlinx.android.synthetic.main.custom_alert_dialog.view.*
import android.content.DialogInterface.OnClickListener
import android.net.sip.SipSession
import android.util.Log

class CustomDialogAlert(val context: Context){
    private val dialog = AlertDialog.Builder(context).create()

    // Si no se introduce null como root, da error ya que la vista de un dialog no tiene que tener parent root
    lateinit var customView: View

    init {
        customView = LayoutInflater.from(context).inflate(R.layout.custom_alert_dialog,null)
    }


    fun setTitle(title: String){
        customView.customDialogTitle.text = title
    }

    fun setTitle(title: Int){
        customView.customDialogTitle.text = context.getText(title)
    }

    fun setMessage(message: String){
        customView.customDialogMessage.text = message
    }

    fun setMessage(message: Int){
        customView.customDialogMessage.text = context.getText(message)
    }

    fun setPossitiveButton(text: String,listener: View.OnClickListener){
        customView.customDialogPrimaryButton.text = text
        customView.customDialogPrimaryButton.setOnClickListener(listener)
    }

    fun setPossitiveButton(text: Int,listener: View.OnClickListener){
        customView.customDialogPrimaryButton.text = context.getText(text)
        customView.customDialogPrimaryButton.setOnClickListener(listener)
    }

    fun setNegativeButton(text: String,listener: View.OnClickListener){
        customView.customDialogSecundaryButton.text = text
        customView.customDialogSecundaryButton.setOnClickListener(listener)
        customView.customDialogSecundaryButton.visibility = View.VISIBLE
    }

    fun setNegativeButton(text: Int,listener: View.OnClickListener){
        customView.customDialogSecundaryButton.text = context.getText(text)
        customView.customDialogSecundaryButton.setOnClickListener(listener)
        customView.customDialogSecundaryButton.visibility = View.VISIBLE
    }

    fun setDismissCallback(callback: DialogInterface.OnDismissListener){
        dialog.setOnDismissListener(callback)
    }

    fun dismiss(){
        dialog.dismiss()
    }

    fun setCancelCallback(callback: DialogInterface.OnCancelListener){
        dialog.setOnCancelListener(callback)
    }

    fun show(){
        dialog.setView(customView)

        dialog.window?.setBackgroundDrawable(context.getDrawable(android.R.color.transparent))

        dialog.show()
    }

}