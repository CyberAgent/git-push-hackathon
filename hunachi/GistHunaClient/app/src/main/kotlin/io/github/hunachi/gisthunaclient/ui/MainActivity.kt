package io.github.hunachi.gisthunaclient.ui

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import io.github.hunachi.gisthunaclient.R
import io.github.hunachi.gisthunaclient.databinding.ActivityMainBinding
import io.github.hunachi.gisthunaclient.flux.FragmentState
import io.github.hunachi.gisthunaclient.flux.actionCreator.MainActionCreator
import io.github.hunachi.gisthunaclient.ui.gistCreate.CreateGistFragment
import io.github.hunachi.gisthunaclient.ui.gistList.GistListFragment
import io.github.hunachi.oauth.OauthActivity
import io.github.hunachi.shared.*
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val preference: SharedPreferences by inject()
    private val mainActionCreator: MainActionCreator by inject()

    val binding by lazyFast {
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.fab.setOnClickListener {
            mainActionCreator.clickedFav()
        }

        // todo あとで認証できるようにする．
        if (preference.isFirstUser()) {
            startActivity(OauthActivity.newInstance())
            preference.firstCheckIn()
        } else if (preference.token() == null) {
            preference.token() ?: tokenIsDuplicatedOrFailed()
        }
    }

    override fun onStart() {
        super.onStart()
        replaceFragment(FragmentState.GIST_LIST)
    }

    override fun onBackPressed() {
        replaceFragment(FragmentState.GIST_LIST)
    }

    fun replaceFragment(fragmentState: FragmentState) {
        supportFragmentManager.inTransaction {
            replace(binding.listContainer.id, when (fragmentState) {
                FragmentState.GIST_LIST -> GistListFragment.newInstance()
                FragmentState.CREATE_GIST -> CreateGistFragment.newInstance()
            }.checkAllMatched)
        }
    }

    fun tokenIsDuplicatedOrFailed() {
        Toast.makeText(this, getString(R.string.sorry_oauth_toast_text), Toast.LENGTH_SHORT).show()
        startActivity(OauthActivity.newInstance())
    }
}
