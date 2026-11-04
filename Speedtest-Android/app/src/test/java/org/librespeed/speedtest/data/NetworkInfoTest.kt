package org.librespeed.speedtest.data

import android.Manifest
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(AndroidJUnit4::class)
//robolectric does not support SDK 36 yet
@Config(sdk = [35])
class NetworkInfoTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    private fun setTransport(transport: Int) {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).addTransportType(transport)
        shadowOf(connectivity).setNetworkCapabilities(connectivity.activeNetwork, capabilities)
    }

    @Test
    fun `wifi is reported as wifi`() {
        setTransport(NetworkCapabilities.TRANSPORT_WIFI)
        assertEquals("Wi-Fi", NetworkInfo.describe(context))
    }

    @Test
    fun `ethernet is reported as ethernet`() {
        setTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        assertEquals("Ethernet", NetworkInfo.describe(context))
    }

    @Test
    fun `cellular without the phone permission stays generic`() {
        setTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        assertEquals("Cellular", NetworkInfo.describe(context))
    }

    @Test
    fun `cellular with the permission reports the generation`() {
        setTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        shadowOf(context).grantPermissions(Manifest.permission.READ_PHONE_STATE)
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        shadowOf(telephony).setDataNetworkType(TelephonyManager.NETWORK_TYPE_NR)
        assertEquals("5G", NetworkInfo.describe(context))
        shadowOf(telephony).setDataNetworkType(TelephonyManager.NETWORK_TYPE_LTE)
        assertEquals("4G LTE", NetworkInfo.describe(context))
        shadowOf(telephony).setDataNetworkType(TelephonyManager.NETWORK_TYPE_UMTS)
        assertEquals("3G", NetworkInfo.describe(context))
        shadowOf(telephony).setDataNetworkType(TelephonyManager.NETWORK_TYPE_EDGE)
        assertEquals("2G", NetworkInfo.describe(context))
        shadowOf(telephony).setDataNetworkType(TelephonyManager.NETWORK_TYPE_UNKNOWN)
        assertEquals("Cellular", NetworkInfo.describe(context))
    }

    @Test
    fun `no active network means no description`() {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowOf(connectivity).setNetworkCapabilities(connectivity.activeNetwork, null)
        assertNull(NetworkInfo.describe(context))
    }

    @Test
    fun `cellular detection helper matches the transport`() {
        setTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        assertEquals(true, NetworkInfo.isCellular(context))
        setTransport(NetworkCapabilities.TRANSPORT_WIFI)
        assertEquals(false, NetworkInfo.isCellular(context))
    }

}
