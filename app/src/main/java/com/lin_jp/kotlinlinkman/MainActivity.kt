package com.lin_jp.kotlinlinkman

import android.graphics.Canvas
import android.graphics.Rect
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.addres_item.view.*
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recycler.setHasFixedSize(true)    //不需要初始化控件，直接用ID来用
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = object : RecyclerView.Adapter<ViewHolder>() {
            val calendar = CalendarNX(this@MainActivity)
            override fun getItemCount(): Int {
                return 200 * 12
            }//长度200年

            override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ViewHolder {
                return object : ViewHolder(layoutInflater.inflate(R.layout.addres_item, viewGroup, false)) {}
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val month = position % 12 + 1
                val list = calendar.getCalendarList(pos2Year(position), month)//日历列表
                holder.itemView.month.text = "$month 月"
                for (i in 0..41) {
                    val tv = holder.itemView.layout.getChildAt(i) as TextView
                    if (i < list.size) {
                        if (tv.visibility != View.VISIBLE) tv.visibility = View.VISIBLE
                        tv.tag = list[i]
                        tv.text = getSpannable("${list[i]["sd"]}\n${calendar.getString(list[i])}")
                        tv.setTextColor(
                            if (list[i]["isThisMonth"] as Boolean) resources.getColor(android.R.color.black, theme)
                            else resources.getColor(android.R.color.tab_indicator_text, theme)
                        )
                    } else if (tv.visibility != View.GONE) tv.visibility = View.GONE
                }
            }
        }
        recycler.addItemDecoration(object : RecyclerView.ItemDecoration() { //悬浮关键在这里
            var headOly: TextView? = null

            /**
             * 创建分区头视图
             * @param parent
             * @return
             */
            private fun getHeader(parent: RecyclerView): TextView {
                if (headOly == null) {
                    headOly = layoutInflater.inflate(R.layout.addres_item_head, parent, false) as TextView?
                    val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
                    val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)
                    val childWidth = ViewGroup.getChildMeasureSpec(
                        widthSpec,
                        parent.paddingLeft + parent.paddingRight,
                        headOly!!.layoutParams.width
                    )
                    val childHeight = ViewGroup.getChildMeasureSpec(
                        heightSpec,
                        parent.paddingTop + parent.paddingBottom,
                        headOly!!.layoutParams.height
                    )
                    headOly!!.measure(childWidth, childHeight)
                    headOly!!.layout(0, 0, headOly!!.measuredWidth, headOly!!.measuredHeight)
                }
                return headOly!!
            }

            /**
             * 插入分区头空间
             * @param outRect
             * @param view
             * @param parent
             * @param state
             */
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                if (hasHeader(parent.getChildAdapterPosition(view))) outRect.top = getHeader(parent).height
            }

            /**
             * 绘制分区头
             * @param c
             * @param parent
             * @param state
             */
            override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {//这里画出的东西是复盖在item之上的
                val count = parent.childCount//列表数量
                for (layoutPos in 0 until count) {
                    val child = parent.getChildAt(layoutPos)//根据位置取到视图
                    val adapterPos = parent.getChildAdapterPosition(child)//在适配器中的位置
                    if (layoutPos == 0 || hasHeader(adapterPos)) {
                        val header = getHeader(parent)
                        header.text ="${pos2Year(adapterPos)}年"
                        c.save()
                        c.translate(0f, getHeaderTop(parent, child, header, adapterPos, layoutPos))//平移
                        header.draw(c)
                        c.restore()
                    }
                }
            }

            /**
             * @param parent     RecyclerView
             * @param child      布局中的子View
             * @param header       分区头
             * @param adapterPos   在适配器中的下标
             * @param layoutPos    布局中的下标
             * @return
             */
            private fun getHeaderTop(
                parent: RecyclerView,
                child: View,
                header: View,
                adapterPos: Int,
                layoutPos: Int
            ): Float {
                val offset: Float
                if (layoutPos == 0) {
                    val flag = pos2Year(adapterPos)//拿到分类标识
                    for (i in 1 until parent.childCount) {
                        val next = parent.getChildAt(i)//遍历全部子View
                        if (flag != pos2Year(parent.getChildAdapterPosition(next))) {
                            offset = next.y - header.height * 2
                            return if (offset < 0) offset else 0f
                        }
                    }
                }
                offset = child.y - header.height
                return if (offset > 0) offset else 0f
            }

            /**
             * 是否有分区头
             * @param adapterPos
             * @return
             */
            private fun hasHeader(adapterPos: Int): Boolean {
                return pos2Year(adapterPos) != pos2Year(adapterPos - 1)
            }
        })
        val cal = Calendar.getInstance()//获取当前时间
        recycler.scrollToPosition((cal.get(Calendar.YEAR) - 1901) * 12 + cal.get(Calendar.MONTH))//滚动到当前月份
    }

    /**
     * 把适配器把下标转成年
     * @param adapterPos
     * @return
     */
    private fun pos2Year(adapterPos: Int): Int {
        return adapterPos / 12 + 1901
    }

    private fun getSpannable(str: String): SpannableString {
        val ss=SpannableString(str)
        val start = str.indexOf('\n')+1
        ss.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorAccent, theme)), start, str.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        ss.setSpan(AbsoluteSizeSpan(10, true), start, str.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return ss
    }

    fun onClick(view: View) {
        val item = view.tag as Map<*, *>
        Toast.makeText(
            this,
            "农历${item["ny"]}年${item["nm"] as Int + 1}月${item["nd"] as Int + 1}日\n西历${item["sy"]}年${item["sm"]}月${item["sd"]}日",
            Toast.LENGTH_SHORT
        ).show()
    }
}