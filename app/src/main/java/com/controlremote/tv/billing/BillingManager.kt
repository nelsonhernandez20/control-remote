package com.controlremote.tv.billing

import androidx.activity.ComponentActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Compra in-app para quitar anuncios. Crea en Play Console un producto in-app (no consumible)
 * con id [PRODUCT_REMOVE_ADS] y asígnale el precio deseado (p. ej. 1 USD).
 */
class BillingManager(
    private val activity: ComponentActivity
) : PurchasesUpdatedListener {

    private val _adsRemoved = MutableStateFlow(false)
    val adsRemoved: StateFlow<Boolean> = _adsRemoved.asStateFlow()

    private var billingClient: BillingClient? = null
    private var cachedProductDetails: ProductDetails? = null

    private val client: BillingClient
        get() = billingClient ?: error("Billing no iniciado")

    fun start() {
        if (billingClient != null) return
        billingClient = BillingClient.newBuilder(activity)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchasesAndUpdateState()
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
        cachedProductDetails = null
    }

    private fun queryPurchasesAndUpdateState() {
        val bc = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        bc.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val list = purchases ?: emptyList()
            val hasRemoveAds = list.any { purchase ->
                purchase.products.contains(PRODUCT_REMOVE_ADS) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            _adsRemoved.value = hasRemoveAds
        }
    }

    private fun queryProductDetails() {
        val bc = billingClient ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_REMOVE_ADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        bc.queryProductDetailsAsync(params) { billingResult, list ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            cachedProductDetails = list?.firstOrNull()
        }
    }

    fun launchRemoveAdsFlow() {
        val bc = billingClient ?: return
        if (!bc.isReady) return
        cachedProductDetails?.let { launchBilling(it); return }
        bc.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PRODUCT_REMOVE_ADS)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()
        ) { billingResult, list ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val details = list?.firstOrNull() ?: return@queryProductDetailsAsync
            cachedProductDetails = details
            activity.runOnUiThread { launchBilling(details) }
        }
    }

    private fun launchBilling(details: ProductDetails) {
        val bc = billingClient ?: return
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        bc.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases ?: return
                for (p in purchases) {
                    if (!p.products.contains(PRODUCT_REMOVE_ADS)) continue
                    if (p.purchaseState != Purchase.PurchaseState.PURCHASED) continue
                    if (!p.isAcknowledged) {
                        val ack = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(p.purchaseToken)
                            .build()
                        client.acknowledgePurchase(ack) { ackResult ->
                            if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                _adsRemoved.value = true
                            }
                        }
                    } else {
                        _adsRemoved.value = true
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {}
            else -> {}
        }
    }

    companion object {
        /** Debe coincidir con el id del producto en Play Console. */
        const val PRODUCT_REMOVE_ADS = "remove_ads"
    }
}
