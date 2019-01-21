package com.pauenrech.chancys.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.transition.Transition
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pauenrech.chancys.R
import kotlinx.android.synthetic.main.fragment_register.view.*


class RegisterWithChancyFragment : Fragment() {

    private var listener: OnRegisterWithChancyInteraction? = null
    private var rootView: View? = null
    private var transition: Transition? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_register, container, false)
        loadTransition()
        setBackButton()
        registerButtonClick()

        return rootView
    }

    fun loadTransition(){
        transition = TransitionInflater.from(activity?.applicationContext)
            .inflateTransition(R.transition.correction_text_appear)
    }

    fun registerButtonClick(){
        rootView?.registerButton?.setOnClickListener {
            initialState()
            val email = rootView?.registerEmail?.text.toString()
            val username = rootView?.registerUserName?.text.toString()
            val password = rootView?.registerPassword?.text.toString()
            val repitedPassword = rootView?.registerRepitePassword?.text.toString()
            val validEmail = isValidEmail(email)
            val validUsername = isValidUsername(username)
            val validPassword = isValidPassword(password)
            val samePassword = arePasswordEqual(password, repitedPassword)

            if (!validEmail)
                errorWithEmail(getString(R.string.form_error_not_valid_email))
            if (!validUsername)
                errorWithUsername(getString(R.string.format_error_too_short_username))
            if (!validPassword)
                errorWithPassword(getString(R.string.form_password_too_short))
            if (!samePassword)
                errorWithPasswords()
            if (validEmail && validPassword && samePassword && validUsername){
                setLoading(true)
                listener?.onRegisterButtonClick(email,username, password)

            }
        }
    }


    fun setBackButton(){
        rootView?.register_toolbar?.setNavigationIcon(R.drawable.abc_ic_ab_back_material)
        rootView?.register_toolbar?.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    fun clearAll(){
        rootView?.registerSpinner?.visibility = View.GONE
        rootView?.registerButton?.visibility = View.VISIBLE
        rootView?.registerEmail?.text?.clear()
        rootView?.registerEmailErrorLabel?.visibility = View.GONE
        rootView?.registerUserName?.text?.clear()
        rootView?.registerUsernameErrorLabel?.visibility = View.GONE
        rootView?.registerPassword?.text?.clear()
        rootView?.registerPasswordErrorLabel?.visibility = View.GONE
        rootView?.registerRepitePassword?.text?.clear()
        rootView?.registerRepitePasswordErrorLabel?.visibility = View.GONE
    }

    fun initialState(){
        TransitionManager.beginDelayedTransition(rootView?.rootRegister,transition)
        rootView?.registerEmailErrorLabel?.visibility = View.GONE
        rootView?.registerUsernameErrorLabel?.visibility = View.GONE
        rootView?.registerPasswordErrorLabel?.visibility = View.GONE
        rootView?.registerRepitePasswordErrorLabel?.visibility = View.GONE
        setLoading(false)
    }

    fun errorWithEmail(errorMessage: String){
        TransitionManager.beginDelayedTransition(rootView?.rootRegister,transition)
        rootView?.registerEmailErrorLabel?.text = errorMessage
        rootView?.registerEmailErrorLabel?.visibility = View.VISIBLE
    }

    fun errorWithUsername(errorMessage: String){
        TransitionManager.beginDelayedTransition(rootView?.rootRegister,transition)
        rootView?.registerUsernameErrorLabel?.text = errorMessage
        rootView?.registerUsernameErrorLabel?.visibility = View.VISIBLE
    }

    fun errorWithPassword(errorMessage: String){
        TransitionManager.beginDelayedTransition(rootView?.rootRegister, transition)
        rootView?.registerPasswordErrorLabel?.text = errorMessage
        rootView?.registerPasswordErrorLabel?.visibility = View.VISIBLE
    }

    fun errorWithPasswords(){
        TransitionManager.beginDelayedTransition(rootView?.rootRegister, transition)
        rootView?.registerRepitePasswordErrorLabel?.text = getString(R.string.form_password_not_equal)
        rootView?.registerRepitePasswordErrorLabel?.visibility = View.VISIBLE
    }

    fun isValidPassword(target: String): Boolean{
        return target.length > 7
    }

    fun isValidUsername(target: String): Boolean{
        return target.length > 2
    }

    fun arePasswordEqual(pass1: String, pass2: String): Boolean{
        return pass1.equals(pass2)
    }

    fun isValidEmail(target : String): Boolean{
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    fun setLoading(loading: Boolean){
        TransitionManager.beginDelayedTransition(rootView?.rootRegister, transition)
        when(loading){
            true -> {
                rootView?.registerButton?.visibility = View.INVISIBLE
                rootView?.registerSpinner?.visibility = View.VISIBLE
            }
            false ->{
                rootView?.registerSpinner?.visibility = View.GONE
                rootView?.registerButton?.visibility = View.VISIBLE
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnRegisterWithChancyInteraction) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnRegisterWithChancyInteraction {
        fun onRegisterButtonClick(email: String, username: String, password: String)
    }

}
