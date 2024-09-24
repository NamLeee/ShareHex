package com.namlee.sharehex

data class HexData(val id: String?, val hex: String?, val time: Long?, val model: String?) {
    override fun toString(): String {
        val stringReturn =
            " - Link: ${hex?.let { Utils.hexToText(it) }} \n" + " - Time: ${time?.let { Utils.convertLongToDateTime(it) }}\n" + " - Model: $model"
        return stringReturn
    }
}
