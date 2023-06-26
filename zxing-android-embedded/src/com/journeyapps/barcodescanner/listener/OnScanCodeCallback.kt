package com.journeyapps.barcodescanner.listener

/**
 * @author xiaoxiaoying
 * @date 3/3/21
 */
interface OnScanCodeCallback {
    /**
     *  扫描返回的内容
     */
    fun onScanResult(content: String)

    fun isFinishActivity(): Boolean = true
}