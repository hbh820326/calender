package com.lin_jp.kotlinlinkman

import android.content.Context
import java.util.*

class CalendarNX(context: Context) {
    private val jqSample = "abcdefghijklmnopqrstuvwxyz"  //仓库26进取样串
    private val sample = "一二三四五六七八九十冬腊"// 农历月份取样列表
    private fun Any?.int() = this as Int //追加转int函数
    private val jq = context.resources.getStringArray(R.array.jie_qi_name)//节气名
    private val jqStore = context.resources.getStringArray(R.array.jie_qi_store)//节气仓库  26进数字
    private var days = context.resources.getIntArray(R.array.days)// 每月份对应的天数
    private var store = context.resources.getIntArray(R.array.nl_store)//农历仓库 二进数字
    private val festival = mapOf(
        "N1/1" to "春节", "N1/15" to "元宵节", "N2/2" to "龙抬头", "N5/5" to "端午节", "N7/7" to "七夕", "N7/15" to "中元节",
        "N8/15" to "中秋节", "N9/9" to "重阳节", "N12/8" to "腊八节", "N12/16" to "尾牙", "N12/23" to "小年", "X1/1" to "元旦",
        "X2/14" to "情人节", "X3/8" to "妇女节", "X3/12" to "植树节", "X5/1" to "劳动节", "X5/4" to "青年节", "X6/1" to "儿童节",
        "X8/1" to "建军节", "X9/10" to "教师节", "X10/1" to "国庆节", "X12/25" to "圣诞节"
    )//农历和西历常见节日表

    fun getString(nx: Map<*, *>): String {
        return when {
            nx["nd"].int() < 0 -> ""//小于零表示超出农历范围
            nx["nm"] == 11 && nx["nds"] == nx["nd"].int() + 1 -> "除夕"
            nx["sm"] == 5 && (nx["sd"] == 8 && nx["index"] == 7 || nx["sd"] != 15 && nx["index"] == 14) -> "母亲节"
            nx["sm"] == 6 && (nx["sd"] == 15 && nx["index"] == 14 || nx["sd"] != 22 && nx["index"] == 21) -> "父亲节"
            nx["sm"] == 11 && nx["sd"].int() > 21 && nx["sd"].int() < 29 && (nx["index"] == 25 || nx["index"] == 32) -> "感恩节"
            festival.keys.contains("N${nx["nm"].int() + 1}/${nx["nd"].int() + 1}") -> festival.getValue("N${nx["nm"].int() + 1}/${nx["nd"].int() + 1}")//农历节日
            festival.keys.contains("X${nx["sm"]}/${nx["sd"]}") -> festival.getValue("X${nx["sm"]}/${nx["sd"]}")//西历节日
            jqSample.indexOf(jqStore[nx["sy"].int() - 1900][nx["sm"].int() * 2 - 2]) == nx["sd"] -> jq[nx["sm"].int() * 2 - 2]//上节气
            jqSample.indexOf(jqStore[nx["sy"].int() - 1900][nx["sm"].int() * 2 - 1]) == nx["sd"] -> jq[nx["sm"].int() * 2 - 1]//下节气
            nx["nd"] == 0 -> if ((nx["isRY"] as Boolean)) "闰${sample[nx["nm"].int()]}月" else "${sample[nx["nm"].int()]}月"//月初用几月代替
            else -> (if (nx["nd"].int() > 9) if (nx["nd"].int() > 18) if (nx["nd"].int() > 19) if (nx["nd"].int() > 28) "三" else "廿" else "二" else "十" else "初") + sample[nx["nd"].int() % 10]
        }
    }//显示优先级从上到下

    fun getCalendarList(year: Int, month: Int): ArrayList<Map<*, *>> {
        return lunar(
            getDays(year, month, 0), when (month) {
                1 -> arrayOf(
                    intArrayOf(year - 1, 12, getMonthDays(year - 1, 12)),
                    intArrayOf(year, month, getMonthDays(year, month)),
                    intArrayOf(year, 2)
                )
                12 -> arrayOf(
                    intArrayOf(year, 11, getMonthDays(year, 11)),
                    intArrayOf(year, month, getMonthDays(year, month)),
                    intArrayOf(year + 1, 1)
                )
                else -> arrayOf(
                    intArrayOf(year, month - 1, getMonthDays(year, month - 1)),
                    intArrayOf(year, month, getMonthDays(year, month)),
                    intArrayOf(year, month + 1)
                )
            }
        )
    }

    /**
     * @param y  年
     * @param m 月
     * @param d 日
     * @return 从0年1月0日起算天数
     */
    private fun getDays(y: Int, m: Int, d: Int): Int {
        val day = 365 * y + days[m - 1] + d + (y - 1) / 4 - (y - 1) / 100 + (y - 1) / 400
        return if ((y % 4 == 0 && y % 100 != 0 || y % 400 == 0) && m > 2) day + 1
        else day
    }

    /**
     * @param year  年
     * @param month 月
     * @return 月份天数
     */
    private fun getMonthDays(year: Int, month: Int): Int {
        return when (month) {
            2 -> if (year % 4 == 0 && year % 100 != 0 || year % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    /**
     * @param distances 间隔天数
     * @param date     月份信息：（年，月，当月天数，包含：上月，当前月，下月）
     * @return
     */
    private fun lunar(distances: Int, date: Array<IntArray>): ArrayList<Map<*, *>> {
        val blank = distances % 7//上月格数
        val flag= (Math.ceil((date[1][2] + blank)/7.0)*7).toInt()// （包含：上月格数，当月格数，下月格数）
        var distance = distances - blank - 693990//农历开始的第一天到当月1日天数
        val list = ArrayList<Map<*, *>>(flag)
        for ((i, value) in store.withIndex()) {// 遍历 农历仓库
            val yDays = getYDays(value)
            if (distance < yDays) {
                var leap = value shr 13//  闰月份，没有闰月为0（右移13位）
                var j = 0
                while (j < 12) {// 遍历位标记
                    var index = j//月份标记位
                    if (j == leap && leap > 0) {//发现闰月份，进行插入
                        index = 12// 闰月份标记位
                        leap = -leap// 标记闰月份已插入
                        j--//还原正常月份
                    }
                    val nDays = if (value shr index and 1 > 0) 30 else 29//农历当月天数
                    if (distance < nDays) {// 不够减时进去，把剩下的直接当起点起算
                        for (day in distance until nDays)
                            if (flag == list.size) return list
                            else list.setData(i + 1900, j, day, nDays, j + leap < 0, flag, blank, date)
                        distance = 0// 完成后起点归零
                    } else distance -= nDays//一个月一个月的减去
                    j++
                }
            } else distance -= yDays//一年一年减去
        }
        return list.setData(0, 0, -99, 0, false, flag, blank, date)
    }

    private fun getYDays(x: Int): Int {
        val e = if (x shr 13 > 0) 13 else 12
        var days = 30 * e
        for (i in 0 until e)
            if (x shr i and 1 < 1) days--
        return days
    }

    /**
     * @param y     农历年
     * @param m     农历月
     * @param d     农历天
     * @param ds    农历当月天数
     * @param isRY  是否农历闰月份
     * @param flag  月历总格数
     * @param blank 上月格数
     * @param date  上中下月的年月和当月天数
     * @return 当前容器
     */
    private fun ArrayList<Map<*, *>>.setData(
        y: Int,
        m: Int,
        d: Int,
        ds: Int,
        isRY: Boolean,
        flag: Int,
        blank: Int,
        date: Array<IntArray>
    ): ArrayList<Map<*, *>> {
        do {
            val sd = size - blank + 1
            add(
                when {
                    sd > 0 && sd > date[1][2] -> mapOf(
                        "ny" to y, "nm" to m, "nd" to d, "nds" to ds, "isRY" to isRY,
                        "sy" to date[2][0], "sm" to date[2][1], "sd" to sd - date[1][2],
                        "index" to size, "isThisMonth" to false
                    )
                    sd > 0 -> mapOf(
                        "ny" to y, "nm" to m, "nd" to d, "nds" to ds, "isRY" to isRY,
                        "sy" to date[1][0], "sm" to date[1][1], "sd" to sd,
                        "index" to size, "isThisMonth" to true
                    )
                    else -> mapOf(
                        "ny" to y, "nm" to m, "nd" to d, "nds" to ds, "isRY" to isRY,
                        "sy" to date[0][0], "sm" to date[0][1], "sd" to sd + date[0][2],
                        "index" to size, "isThisMonth" to false
                    )
                }
            )
        } while (d == -99 && flag > size)//发现超出农历范围，循环赋值西历
        return this
    }
}


