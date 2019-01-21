package com.pauenrech.chancys.fragments

import android.content.Context
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
import kotlinx.android.synthetic.main.fragment_login.view.*

class LoginWithChancyFragment : Fragment() {
    private var listener: OnLoginWithChancyInteraction? = null
    private var rootView: View? = null
    private var transition: Transition? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_login, container, false)

        loadTransition()
        setBackButton()
        loginButtonClick()
        registerButtonClick()

        return rootView
    }

    fun loadTransition(){
        transition = TransitionInflater.from(activity?.applicationContext)
            .inflateTransition(R.transition.correction_text_appear)
    }

    fun loginButtonClick(){
        rootView?.loginButton?.setOnClickListener {
            initialState()
            val email = rootView?.loginEmail?.text.toString()
            val password = rootView?.loginPassword?.text.toString()
            val validEmail = isValidEmail(email)
            val validPassword = isValidPassword(password)

            if (!validEmail)
                errorWithEmail(getString(R.string.form_error_not_valid_email))
            if (!validPassword)
                errorWithPassword(getString(R.string.form_password_too_short))
            if (validEmail && validPassword){
                setLoading(true)
                listener?.onLoginButtonClick(email,password)

            }
        }
    }

    fun registerButtonClick(){
        rootView?.loginToRegisterButton?.setOnClickListener {
            clearAll()
            listener?.onToRegisterButtonClick()
        }
    }

    fun setBackButton(){
        rootView?.login_toolbar?.setNavigationIcon(R.drawable.abc_ic_ab_back_material)
        rootView?.login_toolbar?.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    fun clearAll(){
        rootView?.loginToRegisterButton?.visibility = View.VISIBLE
        rootView?.loginSpinner?.visibility = View.GONE
        rootView?.loginButton?.visibility = View.VISIBLE
        rootView?.loginEmail?.text?.clear()
        rootView?.loginEmailErrorLabel?.visibility = View.GONE
        rootView?.loginPassword?.text?.clear()
        rootView?.loginPasswordErrorLabel?.visibility = View.GONE
    }

    fun initialState(){
        TransitionManager.beginDelayedTransition(rootView?.rootLogin,transition)
        rootView?.loginEmailErrorLabel?.visibility = View.GONE
        rootView?.loginPasswordErrorLabel?.visibility = View.GONE
        setLoading(false)
    }

    fun errorWithEmail(errorMessage: String){
        TransitionManager.beginDelayedTransition(rootView?.rootLogin,transition)
        rootView?.loginEmailErrorLabel?.text = errorMessage
        rootView?.loginEmailErrorLabel?.visibility = View.VISIBLE
    }

    fun errorWithPassword(errorMessage: String){
        TransitionManager.beginDelayedTransition(rootView?.rootLogin, transition)
        rootView?.loginPasswordErrorLabel?.text = errorMessage
        rootView?.loginPasswordErrorLabel?.visibility = View.VISIBLE
    }

    fun isValidPassword(target: String): Boolean{
        return target.length > 7
    }

    fun isValidEmail(target : String): Boolean{
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    fun setLoading(loading: Boolean){
        TransitionManager.beginDelayedTransition(rootView?.rootLogin, transition)
        when(loading){
            true -> {
                rootView?.loginButton?.visibility = View.INVISIBLE
                rootView?.loginSpinner?.visibility = View.VISIBLE
                rootView?.loginToRegisterButton?.visibility = View.INVISIBLE
            }
            false ->{
                rootView?.loginSpinner?.visibility = View.GONE
                rootView?.loginButton?.visibility = View.VISIBLE
                rootView?.loginToRegisterButton?.visibility = View.VISIBLE
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnLoginWithChancyInteraction) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }


    interface OnLoginWithChancyInteraction {
        fun onLoginButtonClick(email: String, password: String)
        fun onToRegisterButtonClick()
    }

}
