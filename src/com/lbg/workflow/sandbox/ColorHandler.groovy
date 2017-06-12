/*
 * Author: vhedayati
 */

package com.lbg.workflow.sandbox

class ColorHandler {

    def black() {
        return "\\u001B[0;30m"
    }

    def drk_grey() {
        return "\\u001B[1;30m"
    }

    def grey() {
        return "\\u001B[0;37m"
    }

    def white() {
        return "\\u001B[1;37m"
    }

    def blue() {
        return "\\u001B[0;34m"
    }

    def cyan() {
        return "\\u001B[0;36m"
    }

    def green() {
        return "\\u001B[0;32m"
    }

    def redSimple() {
        return "\\u001B[31m"
    }

    def red() {
        return "\\u001B[0;4;31m"
    }

    def wht_red() {
        return "\\u001B[0;4;47;31m"
    }

    def lgt_red() {
        return "\\u001B[1;31m"
    }

    def wht_lgt_red() {
        return "\\u001B[1;4;47;31m"
    }

    def yellow() {
        return "\\u001B[1;33m"
    }

    def blk_ylw() {
        return "\\u001B[1;4;40;33m"
    }

    def reset() {
        return "\\u001B[0m"
    }
}
