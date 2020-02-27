package org.ole.planet.myplanet.ui.team

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.realm.Case
import io.realm.Realm
import io.realm.RealmQuery
import kotlinx.android.synthetic.main.alert_create_team.view.*
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmMyTeam
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.utilities.Utilities
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class TeamFragment : Fragment() {
    var mRealm: Realm? = null
    var rvTeamList: RecyclerView? = null
    var etSearch: EditText? = null
    var type: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            type = arguments!!.getString("type")
            Utilities.log("Team fragment")
        }
        Utilities.log("Team fragment")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? { // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_team, container, false)
        rvTeamList = v.findViewById(R.id.rv_team_list)
        etSearch = v.findViewById(R.id.et_search)
        mRealm = DatabaseService(activity).realmInstance
        v.findViewById<View>(R.id.add_team).setOnClickListener { view: View? -> createTeamAlert() }
        return v
    }

    private fun createTeamAlert() {
        val v = LayoutInflater.from(activity).inflate(R.layout.alert_create_team, null)
        if (type != null) {
            v.spn_team_type.visibility = View.GONE
            v.et_description.hint = "What is your enterprise's Mission?"
        } else {
            v.et_services.visibility = View.GONE
            v.et_rules.visibility = View.GONE
            v.et_description.hint = "Description"
        }
        AlertDialog.Builder(activity!!).setTitle(String.format("Enter %s Detail", if (type == null) "Team" else "Enterprise")).setView(v).setPositiveButton("Save") { dialogInterface: DialogInterface?, i: Int ->
            val map = HashMap<String, String>()
            val name = v.et_name.text.toString().trim()
            map["desc"] = v.et_description.text.toString()
            map["services"] = v.et_services.text.toString()
            map["rules"] = v.et_rules.text.toString()
            val isPublic = v.switch_public.isChecked
            val type = if (v.spn_team_type.selectedItemPosition == 0) "local" else "sync"
            when {
                name.isEmpty() -> Utilities.toast(activity, "Name is required")
                type.isEmpty() -> Utilities.toast(activity, "Type is required")
                else -> {
                    createTeam(name, type, map, isPublic)
                    Utilities.toast(activity, "Team Created")
                    setTeamList()
                }
            }
        }.setNegativeButton("Cancel", null).show()
    }

    fun createTeam(name: String?, type: String?, map: HashMap<String, String>, isPublic: Boolean) {
        val user = UserProfileDbHandler(activity).userModel
        if (!mRealm!!.isInTransaction) mRealm!!.beginTransaction()
        val team = mRealm!!.createObject(RealmMyTeam::class.java, UUID.randomUUID().toString())
        team.status = "active"
        team.createdDate = Date().time
        if (type != null) {
            team.services = map["services"]
            team.rules = map["rules"]
        } else {
            team.teamType = type
        }
        team.name = name
        team.description = map["desc"]
        team.teamId = ""
        team.isPublic = isPublic
        team.type = if (this.type == null) "team" else "enterprise"
        team.user_id = user.id
        team.parentCode = user.parentCode
        team.teamPlanetCode = user.planetCode
        mRealm!!.commitTransaction()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mRealm != null && !mRealm!!.isClosed) mRealm!!.close()
    }

    override fun onResume() {
        super.onResume()
        setTeamList()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        rvTeamList!!.layoutManager = LinearLayoutManager(activity)
        setTeamList()
        etSearch!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val query = mRealm!!.where(RealmMyTeam::class.java).isEmpty("teamId").notEqualTo("status", "archived").contains("name", charSequence.toString(), Case.INSENSITIVE)
                val adapterTeamList = AdapterTeamList(activity, getList(query), mRealm, childFragmentManager)
                Utilities.log("Adapter size " + adapterTeamList.itemCount)
                rvTeamList!!.adapter = adapterTeamList
            }
            override fun afterTextChanged(editable: Editable) {}
        })
    }

    private fun getList(query: RealmQuery<RealmMyTeam>): List<RealmMyTeam> {
        var query = query
        query = if (type == null) {
            query.notEqualTo("type", "enterprise")
        } else {
            query.equalTo("type", "enterprise")
        }
        return query.findAll()
    }

    private fun setTeamList() {
        val query = mRealm!!.where(RealmMyTeam::class.java).isEmpty("teamId").notEqualTo("status", "archived")
        val adapterTeamList = AdapterTeamList(activity, getList(query), mRealm, childFragmentManager)
        adapterTeamList.setType(type)
        view!!.findViewById<View>(R.id.type).visibility = if (type == null) View.VISIBLE else View.GONE
        rvTeamList!!.adapter = adapterTeamList
    }
}