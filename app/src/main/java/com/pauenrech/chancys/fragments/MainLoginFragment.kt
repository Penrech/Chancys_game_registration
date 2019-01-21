package com.pauenrech.chancys.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pauenrech.chancys.R
import kotlinx.android.synthetic.main.main_fragment_login.view.*

class MainLoginFragment : Fragment() {
    private var listener: onMainLoginFragmentInteraction? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.main_fragment_login, container, false)

        rootView.loginWithChancy.setOnClickListener {
            listener?.onLoginWithChancyClick()
        }
        rootView.loginWithPlayGames.setOnClickListener {
            listener?.onLoginWithPlayGamesClick()
        }

        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is onMainLoginFragmentInteraction) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface onMainLoginFragmentInteraction {
        fun onLoginWithChancyClick()
        fun onLoginWithPlayGamesClick()
    }

}
