package org.ole.planet.myplanet.ui.myhealth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_vital_sign.*
import org.ole.planet.myplanet.R
import org.ole.planet.myplanet.datamanager.DatabaseService
import org.ole.planet.myplanet.model.RealmMyHealth
import org.ole.planet.myplanet.model.RealmMyHealthPojo
import org.ole.planet.myplanet.model.RealmUserModel
import org.ole.planet.myplanet.service.UserProfileDbHandler
import org.ole.planet.myplanet.ui.userprofile.BecomeMemberActivity
import org.ole.planet.myplanet.utilities.AndroidDecrypter
import org.ole.planet.myplanet.utilities.Constants
import org.ole.planet.myplanet.utilities.Utilities
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class MyHealthFragment : Fragment() {
    var profileDbHandler: UserProfileDbHandler? = null
    var userId: String? = null
    var mRealm: Realm? = null
    var userModel: RealmUserModel? = null
    var dialog: AlertDialog? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_vital_sign, container, false)
        mRealm = DatabaseService(activity).realmInstance
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        add_new_record.setOnClickListener { startActivity(Intent(activity, AddExaminationActivity::class.java).putExtra("userId", userId)) }
        update_health.setOnClickListener { startActivity(Intent(activity, AddMyHealthActivity::class.java).putExtra("userId", userId)) }
        fab_add_member.setOnClickListener { view: View? -> startActivity(Intent(activity, BecomeMemberActivity::class.java)) }
        val v = layoutInflater.inflate(R.layout.alert_users_spinner, null)

        rv_records!!.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        profileDbHandler = UserProfileDbHandler(v.context)
        userId = if (TextUtils.isEmpty(profileDbHandler!!.userModel._id)) profileDbHandler!!.userModel.id else profileDbHandler!!.userModel._id
        getHealthRecords(userId)
        btnnew_patient.setOnClickListener { selectPatient() }
        btnnew_patient.visibility = if (Constants.showBetaFeature(Constants.KEY_HEALTHWORKER, activity)) View.VISIBLE else View.GONE
    }

    private fun getHealthRecords(memberId: String?) {
        userId = memberId
        userModel = mRealm!!.where(RealmUserModel::class.java).equalTo("id", userId).findFirst()
        lblHealthName!!.text = userModel!!.fullName
        showRecords()
    }

    private fun selectPatient() {
        val userModelList = mRealm!!.where(RealmUserModel::class.java).findAll()
        val memberFullNameList: MutableList<String> = ArrayList()
        val map = HashMap<String, String>()
        for (um in userModelList) {
            memberFullNameList.add(um.name)
            map[um.name] = if (TextUtils.isEmpty("_id")) um.id else um.id
        }
        val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, memberFullNameList)
        val alertHealth = LayoutInflater.from(activity).inflate(R.layout.alert_health_list, null)
        val etSearch = alertHealth.findViewById<EditText>(R.id.et_search)
        setTextWatcher(etSearch, adapter)
        val lv = alertHealth.findViewById<ListView>(R.id.list)
        lv.adapter = adapter
        lv.onItemClickListener = OnItemClickListener { adapterView: AdapterView<*>?, view: View, i: Int, l: Long ->
            val user = (view as TextView).text.toString()
            userId = map[user]
            Utilities.log(userId)
            getHealthRecords(userId)
            dialog!!.dismiss()
        }
        dialog = AlertDialog.Builder(activity!!).setTitle(getString(R.string.select_health_member))
                .setView(alertHealth)
                .setCancelable(false).setNegativeButton("Dismiss", null).create()
        dialog?.show()
    }

    private fun setTextWatcher(etSearch: EditText, adapter: ArrayAdapter<String>) {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                adapter.filter.filter(charSequence)
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

    override fun onResume() {
        super.onResume()
        showRecords()
    }

    private fun showRecords() {
        layout_user_detail.visibility = View.VISIBLE
        tv_message.visibility = View.GONE
        txt_full_name.text = """${userModel?.firstName} ${userModel?.middleName} ${userModel?.lastName}"""
        txt_email.text = if (TextUtils.isEmpty(userModel!!.email)) "N/A" else userModel!!.email
        txt_language.text = if (TextUtils.isEmpty(userModel!!.language)) "N/A" else userModel!!.language
        txt_dob.text = if (TextUtils.isEmpty(userModel!!.dob)) "N/A" else userModel!!.dob
        var mh = mRealm!!.where(RealmMyHealthPojo::class.java).equalTo("_id", userId).findFirst()
        if (mh != null) {
            val mm = getHealthProfile(mh)
            if (mm == null) {
                Utilities.toast(activity, "Health Record not available.")
                return
            }
            val myHealths = mm.profile
            txt_other_need.text = Utilities.checkNA(myHealths.notes)
            txt_special_needs.text = Utilities.checkNA(myHealths.specialNeeds)
            txt_birth_place.text = Utilities.checkNA(userModel?.birthPlace)
            txt_emergency_contact!!.text = """
                Name : ${Utilities.checkNA(myHealths.emergencyContactName)}
                Type : ${Utilities.checkNA(myHealths.emergencyContactName)}
                Contact : ${Utilities.checkNA(myHealths.emergencyContact)}
                """.trimIndent()
            val list = getExaminations(mm)

            val adap = AdapterHealthExamination(activity, list, mh, userModel)
            adap.setmRealm(mRealm)
            rv_records.apply {
                layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                isNestedScrollingEnabled = false
                adapter = adap
            }
            rv_records.post { rv_records!!.scrollToPosition(list!!.size - 1) }
        } else {
            txt_other_need!!.text = ""
            txt_special_needs!!.text = ""
            txt_birth_place!!.text = ""
            txt_emergency_contact!!.text = ""
            rv_records!!.adapter = null
        }
    }

    private fun getExaminations(mm: RealmMyHealth): List<RealmMyHealthPojo> {

        Utilities.log("User key " + mm.userKey)
        var healths = mRealm?.where(RealmMyHealthPojo::class.java)!!.findAll()
        Utilities.log("Examination size " + healths?.size)
        healths.forEach{
            Utilities.log(it.profileId)
        }
         healths = mRealm?.where(RealmMyHealthPojo::class.java)!!.equalTo("profileId", mm.userKey)!!.findAll()

        Utilities.log("Examination size " + healths?.size)
        return healths!!
    }

    private fun getHealthProfile(mh: RealmMyHealthPojo): RealmMyHealth? {
        val json = AndroidDecrypter.decrypt(mh.data, userModel!!.key, userModel!!.iv)
        return if (TextUtils.isEmpty(json)) {
            if (!userModel!!.realm!!.isInTransaction) {
                userModel!!.realm.beginTransaction()
            }
            userModel?.iv = ""
            userModel?.key = ""
            userModel?.realm?.commitTransaction()
            null
        } else {
            try {
                Gson().fromJson(json, RealmMyHealth::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}