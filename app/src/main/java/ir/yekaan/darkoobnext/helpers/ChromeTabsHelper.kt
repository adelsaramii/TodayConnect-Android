package ir.yekaan.darkoobnext.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import androidx.core.content.ContextCompat
import ir.yekaan.darkoobnext.R

class CustomTabActivityHelper {

    fun openCustomTab(context: Context, url: String) {
        // Create a CustomTabsIntent.Builder
        val builder = CustomTabsIntent.Builder().apply {
            addDefaultShareMenuItem() // Add default share menu item
            setStartAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            setExitAnimations(context, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
        val darkParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.defaultColorPrimaryDark))
            .setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.defaultColorAccent))
            .setNavigationBarColor(ContextCompat.getColor(context, R.color.defaultColorPrimary))
            .build()
        builder.setColorScheme(COLOR_SCHEME_LIGHT)
        builder.setColorSchemeParams(COLOR_SCHEME_LIGHT, darkParams)
        val customTabsIntent = builder.build()

        try {
            // Check if Chrome is installed
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            // Chrome not installed or other error; open in default browser
            Log.w("CustomTabActivityHelper", "Chrome not installed or other error; open in default browser")
            context.packageManager.getPackageInfo("com.android.chrome", 0)
            openInDefaultBrowser(context, url)
        }
    }

    private fun getDefaultBrowserPackageName(packageManager: PackageManager, url: String): String? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val resolveInfo: ResolveInfo? = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun openInDefaultBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val browserPackageName = getDefaultBrowserPackageName(context.packageManager, url)
            if (browserPackageName != null) {
                intent.setPackage(browserPackageName)
                context.startActivity(intent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(browserIntent)
            }
        }
    }
}