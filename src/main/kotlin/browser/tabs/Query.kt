@file:JsQualifier("browser.tabs")

package browser.tabs

import kotlin.js.Promise

external fun query(queryInfo: QueryInfo): Promise<Array<Tab>>
external fun sendMessage(tabId: Int, message: dynamic): Promise<dynamic>

external interface QueryInfo {
    var url: dynamic /* String | Array<String> */
}

external interface Tab {
    val active: Boolean?
    val discarded: Boolean?
    val frozen: Boolean?
    var id: Int?
    var url: String?
}

