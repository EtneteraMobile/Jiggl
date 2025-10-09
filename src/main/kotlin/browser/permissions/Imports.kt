@file:JsQualifier("browser.permissions")

package browser.permissions

import kotlin.js.Promise

external fun request(options: dynamic): Promise<Boolean>
external fun contains(options: dynamic): Promise<Boolean>

