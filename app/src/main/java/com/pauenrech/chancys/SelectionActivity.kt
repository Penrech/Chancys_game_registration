package com.pauenrech.chancys


import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_selection.*
import android.support.v4.view.ViewPager
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pauenrech.chancys.adapters.selectionViewPager
import com.pauenrech.chancys.fragments.ThemeSelectionFragment
import com.pauenrech.chancys.model.Tema
import com.pauenrech.chancys.model.TemasList
import com.pauenrech.chancys.model.User
import kotlinx.android.synthetic.main.fragment_theme_selection.*


class SelectionActivity : AppCompatActivity(), ThemeSelectionFragment.clickListener {

    private lateinit var mPager: ViewPager
    var pagerAdapter: selectionViewPager? = null

    private var firebaseDatabase: FirebaseDatabase? = null
    private var temasRef: DatabaseReference? = null
    private var userRef: DatabaseReference? = null

    private var userTemasRefListener: ValueEventListener? = null

    private var listaTemas: TemasList = TemasList()
    private var userData: User = User()

    private var temasSet: Boolean = false
    private var temasSetterStarted: Boolean = false


    private var fragmentsList: MutableList<ThemeSelectionFragment> = mutableListOf()
    private var themeInFragmentId: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)

        selection_toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material)
        selection_toolbar.setTitle(R.string.selection_title)

        selection_toolbar.setNavigationOnClickListener { onBackPressed() }

        firebaseDatabase = FirebaseDatabase.getInstance()
        temasRef = firebaseDatabase!!.getReference("temas").child("ES_es")
        userRef = firebaseDatabase!!.getReference("usuarios").child(FirebaseAuth.getInstance().uid!!)


        mPager = findViewById(R.id.pager)

        window.navigationBarColor = getColor(R.color.colorGradientEnd)

        getTemas()

    }

    private fun getTemas(){
        temasSetterStarted = true
        temasRef!!.orderByChild("id").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!temasSet) {
                    val listaTemasTemp = mutableListOf<Tema>()
                    for (tema in dataSnapshot.children){
                        val tempTema = tema.getValue(Tema::class.java)
                        listaTemasTemp.add(tempTema!!)
                    }
                    listaTemas.temas = listaTemasTemp
                    getAndSetUserTemas()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getAndSetUserTemas(){
        userRef!!.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}
            override fun onDataChange(p0: DataSnapshot) {
                if (!temasSet){
                    userData = p0.getValue(User::class.java)!!
                    setListOfTemas()
                }
            }
        })
    }

    fun setListOfTemas(){
        userData.addOrUpdateTemas(listaTemas.temas)
        userRef!!.child("temas").updateChildren(userData.getTemasParaSubirAFirebase())
        /*
        for (tema in listaTemas.temas){
            userData.addTemaLista(tema.name,tema.id)
        }
        userData.deleteTema(listaTemas.temas)
        userRef!!.child("temas").setValue(userData.temas)*/
        temasSet = true
        fillPageViewer()
    }

    fun fillPageViewer(){
        val listaTemasUsuario = userData.getTemaListPorDificultad(userData.dificultad)
        listaTemasUsuario.forEach {userTheme->
            val theme = listaTemas.temas.filter { it.id == userTheme.id}[0]

            fragmentsList.add(ThemeSelectionFragment.newInstance(userTheme.name,userTheme.score,theme.colorStart,theme.colorEnd,theme.id))
            themeInFragmentId.add(theme.id)
        }

        pagerAdapter = selectionViewPager(supportFragmentManager,fragmentsList)
        mPager.adapter = pagerAdapter
    }

    fun refreshPageViewer(temaId: String, score: Int){
        val index = themeInFragmentId.indexOf(temaId)

        if (score != -1){
            val fragmentToChange = fragmentsList[index]
            fragmentToChange.selectionCardRatingBar.rating = (score / 2f)
        }
    }

    override fun onCardClicked(title: String, startColor: String, endColor: String, temaId: String) {

        val intent = Intent(this,GameActivity::class.java)
        intent.putExtra("title",title)
        intent.putExtra("temaID",temaId)
        intent.putExtra("startColor",startColor)
        intent.putExtra("endColor",endColor)
        intent.putExtra("dificultad",userData.dificultad)
        startActivityForResult(intent,101)

        overridePendingTransition(R.anim.slide_from_right,R.anim.slide_to_left)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 101 && resultCode == Activity.RESULT_OK){
            val temaId = data?.getStringExtra("temaID")
            val score = data?.getIntExtra("score",-1)
            refreshPageViewer(temaId!!,score!!)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }
}
