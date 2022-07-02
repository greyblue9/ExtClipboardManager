package com.hhvvg.ecm.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.hhvvg.ecm.R
import com.hhvvg.ecm.getSystemExtClipboardService
import com.hhvvg.ecm.themeColor
import com.hhvvg.ecm.ui.view.InputBottomSheetDialog
import kotlinx.coroutines.launch

/**
 * @author hhvvg
 */
class MainFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    private lateinit var enableSwitchPreference: SwitchPreferenceCompat
    private lateinit var autoClearSwitchPreference: SwitchPreferenceCompat
    private lateinit var timeoutClearSwitchPref: SwitchPreferenceCompat
    private lateinit var autoClearStrategyPreference: Preference
    private lateinit var readStrategyPreference: Preference
    private lateinit var writeStrategyPreference: Preference
    private val extService by lazy {
        requireContext().getSystemExtClipboardService()
    }
    private val navController by lazy {
        findNavController()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().invalidateOptionsMenu()
        //Disable scrollbar
        (view.findViewById(android.R.id.list) as ListView?)?.isVerticalScrollBarEnabled = false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_prefs, rootKey)
        setupEnablePref()
        setupReadStrategyPref()
        setupWriteStrategyPref()
        setupAutoClearPref()
        setupAutoClearStrategyPref()
        setupTimeoutClearPref()
    }

    private fun setupTimeoutClearPref() {
        timeoutClearSwitchPref = findPreference("auto_clear_timeout")!!
        timeoutClearSwitchPref.isChecked = (extService?.autoClearTimeout ?: -1L) > 0
        timeoutClearSwitchPref.onPreferenceChangeListener = this
    }

    private fun showInputDialogForTimeout() {
        val dialog = InputBottomSheetDialog.Builder(requireContext())
            .setMaxLines(1)
            .setTitle(requireContext().getString(R.string.timeout_input_title))
            .setHint(requireContext().getString(R.string.timeout_input_hint))
            .setOnCancelResult("")
            .setInputType(InputType.TYPE_CLASS_NUMBER)
            .build()
        lifecycleScope.launch {
            val result = dialog.showDialog().toString().toLongOrNull() ?: -1L
            if (result != -1L) {
                extService?.autoClearTimeout = result
            } else {
                timeoutClearSwitchPref.onPreferenceChangeListener = null
                timeoutClearSwitchPref.isChecked = false
                timeoutClearSwitchPref.onPreferenceChangeListener = this@MainFragment
            }
        }
    }

    private fun setupAutoClearStrategyPref() {
        autoClearStrategyPreference = findPreference("auto_clear_strategy_key")!!
        autoClearStrategyPreference.setOnPreferenceClickListener {
            val action = MainFragmentDirections.actionMainFragmentToAutoClearStrategyFragment()
            navController.navigate(action)
            true
        }
    }

    private fun setupAutoClearPref() {
        autoClearSwitchPreference = findPreference("auto_clear_key")!!
        autoClearSwitchPreference.isChecked = extService?.isAutoClearEnable ?: false
        autoClearSwitchPreference.apply {
            isChecked = extService?.isAutoClearEnable ?: false
            onPreferenceChangeListener = this@MainFragment
        }
    }

    private fun setupReadStrategyPref() {
        readStrategyPreference = findPreference("filter_app_read_strategy")!!
        readStrategyPreference.setOnPreferenceClickListener {
            val action = MainFragmentDirections.actionMainFragmentToReadStrategyFragment()
            navController.navigate(action)
            true
        }
    }

    private fun setupWriteStrategyPref() {
        writeStrategyPreference = findPreference("filter_app_write_strategy")!!
        writeStrategyPreference.setOnPreferenceClickListener {
            val action = MainFragmentDirections.actionMainFragmentToWriteStrategyFragment()
            navController.navigate(action)
            true
        }
    }

    private fun setupEnablePref() {
        enableSwitchPreference = findPreference("enable_management")!!
        enableSwitchPreference.apply {
            isEnabled = extService != null
            summary = getStatusString()
            isChecked = extService?.isEnable ?: false
            onPreferenceChangeListener = this@MainFragment
        }
    }

    private fun getStatusString(): SpannableString {
        val string =  if (extService == null) {
            requireContext().getString(R.string.unavailable)
        } else {
            requireContext().getString(R.string.available)
        }
        val statusString = requireContext().getString(R.string.current_service_status, string)
        val startSpan = statusString.length - string.length
        val endSpan = statusString.length
        val flag = Spanned.SPAN_INCLUSIVE_INCLUSIVE
        val sp = SpannableString(statusString)
        val colorSpan = ForegroundColorSpan(requireContext().themeColor(androidx.appcompat.R.attr.colorPrimary))
        val sizeSpan = RelativeSizeSpan(1.1F)
        sp.setSpan(colorSpan, startSpan, endSpan, flag)
        sp.setSpan(sizeSpan, startSpan, endSpan, flag)
        return sp
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "https://github.com/gitofleonardo/ExtClipboardManager")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return when(preference.key) {
            "auto_clear_timeout" -> {
                val isOn = newValue as Boolean
                if (isOn) {
                    showInputDialogForTimeout()
                } else {
                    extService?.autoClearTimeout = -1
                }
                true
            }
            "auto_clear_key" -> {
                extService?.isAutoClearEnable = newValue as Boolean
                true
            }
            "enable_management" -> {
                extService?.isEnable = newValue as Boolean
                true
            }
            else -> {
                false
            }
        }
    }
}
