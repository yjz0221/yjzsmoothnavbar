package com.github.yjz.widget.nav

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MenuInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.abs
import kotlin.math.roundToInt


/**
 * 作者: cl
 * 创建日期: 2026/1/6
 * 描述: 一个普通的底部导航栏
 *
 * 核心特性：
 * 1. 永远保持水平排列 (Row Layout)。
 * 2. 支持两种内部样式 (通过 ssb_orientation 属性控制):
 * - horizontal : 经典的胶囊左右结构 (图标在左，文字在右)
 * - vertical : 传统的上下堆叠结构 (图标在上，文字在下)
 * 3. 交互增强:
 * - 支持点击切换，带有平滑的果冻弹跳动画。
 * - 支持按住胶囊(指示器)进行拖拽滑动，图标和文字会根据拖拽进度实时形变。
 * - 手指释放后自动吸附到最近的选项。
 * 4. 视觉增强:
 * - 支持自定义外层背景圆角
 * - 空间不足时自动缩小文字字号。
 */
class SuperSmoothBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // 对应 XML 中的 horizontal (0) 表现为：选中时图标左移，文字显示在右侧
        const val STYLE_SIDE_BY_SIDE = 0
        // 对应 XML 中的 vertical (1) 表现为：选中时图标上移，文字显示在下方
        const val STYLE_STACKED = 1
    }

    // --- XML配置属性 ---
    private var barBackgroundColor = Color.WHITE
    private var barCornerRadius = 0f // 外层背景的大圆角

    private var indicatorColor = Color.parseColor("#FFD600")
    private var indicatorRadius = 10f.dp() // 内部游标(胶囊)的圆角

    private var itemTextSize = 12f.sp()
    private var itemIconSize = 24f.dp()
    private var itemIconTint = Color.GRAY
    private var itemIconTintActive = Color.BLACK
    private var sideMargins = 10f.dp() // 左右两侧留白
    private var itemPadding = 10f.dp() // 图标与文字的间距
    private var indicatorMarginVertical = 5f.dp() // 指示器上下的边距，默认给个 5dp
    private var roundCornersMode = 15 //圆角控制位，默认全圆角 (15 = 1|2|4|8)
    private var alwaysShowText = false // 选中和未选中时一直显示图标和文字

    // 内部样式控制 (默认左右结构)
    private var itemStyle = STYLE_SIDE_BY_SIDE

    // --- 内部核心变量 ---
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var items = mutableListOf<BarItem>()

    // 状态记录
    private var activeItemIndex = 0
    // 指示器的中心 X 坐标 (这是动画的核心驱动值)
    private var indicatorLocation = 0f
    // 记录上一个选中的位置，用于动画过滤
    private var previousItemIndex = -1
    // 标记是否正在执行点击切换动画
    private var isSnapping = false



    // --- 触摸交互相关 ---
    private var isDragging = false // 是否正在拖拽中
    private var downX = 0f // 按下时的X坐标
    // 系统最小滑动距离阈值，用于区分“点击”和“拖拽”
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    // --- 动画相关 ---
    private var animator: ValueAnimator? = null
    // 弹性插值器 (Overshoot)，让动画结束时有“回弹”效果
    private val animInterpolator = OvershootInterpolator(1.0f)

    // --- 回调接口 ---
    var onItemSelected: ((Int) -> Unit)? = null // 选中新项
    var onItemReselected: ((Int) -> Unit)? = null // 重复点击

    // --- 绘制辅助 ---
    // 用于裁剪外层圆角的路径
    private val backgroundPath = Path()
    private val backgroundRect = RectF()

    init {
        // 读取自定义属性
        context.theme.obtainStyledAttributes(attrs, R.styleable.SuperSmoothBottomBar, 0, 0).apply {
            try {
                barBackgroundColor = getColor(R.styleable.SuperSmoothBottomBar_ssb_backgroundColor, barBackgroundColor)
                barCornerRadius = getDimension(R.styleable.SuperSmoothBottomBar_ssb_barCornerRadius, barCornerRadius)

                indicatorColor = getColor(R.styleable.SuperSmoothBottomBar_ssb_indicatorColor, indicatorColor)
                indicatorRadius = getDimension(R.styleable.SuperSmoothBottomBar_ssb_indicatorRadius, indicatorRadius)

                itemTextSize = getDimension(R.styleable.SuperSmoothBottomBar_ssb_textSize, itemTextSize)

                itemIconSize = getDimension(R.styleable.SuperSmoothBottomBar_ssb_iconSize, itemIconSize)
                itemIconTint = getColor(R.styleable.SuperSmoothBottomBar_ssb_iconTint, itemIconTint)
                itemIconTintActive = getColor(R.styleable.SuperSmoothBottomBar_ssb_iconTintActive, itemIconTintActive)

                sideMargins = getDimension(R.styleable.SuperSmoothBottomBar_ssb_sideMargins, sideMargins)
                itemPadding = getDimension(R.styleable.SuperSmoothBottomBar_ssb_itemPadding, itemPadding)
                indicatorMarginVertical = getDimension(R.styleable.SuperSmoothBottomBar_ssb_indicatorMarginVertical, indicatorMarginVertical)
                roundCornersMode = getInt(R.styleable.SuperSmoothBottomBar_ssb_roundCorners, roundCornersMode)
                alwaysShowText = getBoolean(R.styleable.SuperSmoothBottomBar_ssb_alwaysShowText, alwaysShowText)

                // 获取 orientation 属性并映射为 itemStyle
                itemStyle = getInt(R.styleable.SuperSmoothBottomBar_ssb_orientation, itemStyle)

                // 加载 Menu 资源
                val menuResId = getResourceId(R.styleable.SuperSmoothBottomBar_ssb_menu, -1)
                if (menuResId != -1) inflateMenu(menuResId)
            } finally {
                recycle()
            }
        }

        // 初始化文字画笔
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = itemTextSize
    }

    /**
     * 通过 Menu 资源填充数据
     */
    fun inflateMenu(@MenuRes menuRes: Int) {
        val popupMenu = PopupMenu(context, null)
        MenuInflater(context).inflate(menuRes, popupMenu.menu)
        items.clear()
        val menu = popupMenu.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            items.add(BarItem(item.title.toString(), item.icon, item.itemId))
        }
        invalidate()
    }


    /**
     * 代码设置选中项 (带动画)
     */
    fun setActiveItem(index: Int) {
        if (index in items.indices) {
            snapToItem(index)
        }
    }

    /**
     * 当 View 尺寸变化时，重新计算裁剪路径
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 设置外层圆角裁剪区域
        backgroundRect.set(0f, 0f, w.toFloat(), h.toFloat())
        backgroundPath.reset()

        // 准备一个 8 位的数组，每 2 个数代表一个角的 X 和 Y 半径
        // 顺序：左上、右上、右下、左下
        val radii = FloatArray(8)
        val r = barCornerRadius

        // 1 = topLeft
        if ((roundCornersMode and 1) != 0) {
            radii[0] = r; radii[1] = r
        }
        // 2 = topRight
        if ((roundCornersMode and 2) != 0) {
            radii[2] = r; radii[3] = r
        }
        // 4 = bottomRight (注意 Android Path 的顺序是: TL, TR, BR, BL)
        if ((roundCornersMode and 4) != 0) {
            radii[4] = r; radii[5] = r
        }
        // 8 = bottomLeft
        if ((roundCornersMode and 8) != 0) {
            radii[6] = r; radii[7] = r
        }

        // 使用带数组参数的方法添加圆角矩形
        backgroundPath.addRoundRect(backgroundRect, radii, Path.Direction.CW)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) return

        // 1. 裁剪画布以实现外层圆角 (clipPath)
        val saveCount = canvas.save()
        canvas.clipPath(backgroundPath)

        // 2. 绘制背景颜色
        canvas.drawColor(barBackgroundColor)

        // 3. 计算基础布局参数
        // 无论何种样式，Item 总是水平平分宽度的
        val totalLength = width - (sideMargins * 2)
        val itemWidth = totalLength / items.size

        // 初始化指示器位置 (仅在第一次绘制时)
        if (indicatorLocation == 0f && activeItemIndex >= 0) {
            indicatorLocation = sideMargins + (activeItemIndex * itemWidth) + (itemWidth / 2)
        }

        // 4. 绘制黄色游标 (背景指示器)
        drawIndicator(canvas, itemWidth)
        // 5. 绘制图标和文字
        drawItems(canvas, itemWidth)
        // 恢复画布状态
        canvas.restoreToCount(saveCount)
    }


    /**
     * 绘制指示器
     */
    private fun drawIndicator(canvas: Canvas, itemWidth: Float) {
        paint.color = indicatorColor

        // 计算顶部和底部的坐标 (高度 = View高度 - 2 * 边距)
        val top = indicatorMarginVertical
        val bottom = height - indicatorMarginVertical

        // 根据样式决定指示器的宽度 (W)
        val w = if (itemStyle == STYLE_STACKED) {
            itemWidth * 0.85f // 垂直堆叠模式宽度
        } else {
            itemWidth * 0.85f // 水平胶囊模式宽度
        }
        // 创建矩形：左右位置由动画值 indicatorLocation 决定，上下位置由 margin 决定
        val rect = RectF(
            indicatorLocation - (w / 2),
            top,
            indicatorLocation + (w / 2),
            bottom
        )
        canvas.drawRoundRect(rect, indicatorRadius, indicatorRadius, paint)
    }


    /**
     * 绘制所有 Item 的内容
     */
    private fun drawItems(canvas: Canvas, itemWidth: Float) {
        val centerY = height / 2f

        items.forEachIndexed { index, item ->
            val itemCenterX = sideMargins + (index * itemWidth) + (itemWidth / 2)
            val distance = abs(itemCenterX - indicatorLocation)
            var fraction = (1f - (distance / itemWidth)).coerceIn(0f, 1f)

            // 过滤逻辑
            if (isSnapping) {
                // 如果是点击跳转模式 (Snapping)：
                // 只有 "目标项(active)" 和 "起始项(previous)" 允许有动画。
                // 任何中间路过的 Item，强制 fraction = 0 (保持未选中平静状态)。
                if (index != activeItemIndex && index != previousItemIndex) {
                    fraction = 0f
                }
            }
            // 如果是拖拽模式 (!isSnapping)，则不做处理，保留波浪效果

            // === 1. 没有图标：纯文字模式 (逻辑不变，始终居中) ===
            if (item.icon == null) {
                val textColor = evaluateColor(fraction, itemIconTint, itemIconTintActive)
                textPaint.color = textColor

                val maxTextWidth = itemWidth - itemPadding * 2
                textPaint.textSize = calculateAutoTextSize(item.title, maxTextWidth, itemTextSize)

                val fontMetrics = textPaint.fontMetrics
                val baseline = centerY - (fontMetrics.descent + fontMetrics.ascent) / 2
                canvas.drawText(item.title, itemCenterX, baseline, textPaint)

            } else {
                // === 2. 有图标：根据样式绘制 ===

                // 图标颜色计算
                val iconColor = evaluateColor(fraction, itemIconTint, itemIconTintActive)

                // 文字起始颜色：如果总是显示，则从 Tint 色开始；否则从透明开始
                val textStartColor = if (alwaysShowText) itemIconTint else Color.TRANSPARENT
                val textColor = evaluateColor(fraction, textStartColor, itemIconTintActive)

                if (itemStyle == STYLE_STACKED) {
                    // ============================================
                    // 样式 1: Stacked (上下结构)
                    // ============================================

                    // 垂直偏移量计算：
                    // 如果 alwaysShowText，图标位置应该固定在上方，不随 fraction 移动，防止文字忽隐忽现导致跳动
                    // 否则，保持原有的弹跳动画
                    val fixedOffset = itemTextSize / 1.4f
                    val verticalOffset = if (alwaysShowText) fixedOffset else (fraction * fixedOffset)

                    val iconCenterY = centerY - verticalOffset

                    // 绘制图标
                    item.icon.let { drawable ->
                        DrawableCompat.setTint(drawable, iconColor)
                        val l = (itemCenterX - itemIconSize / 2).toInt()
                        val t = (iconCenterY - itemIconSize / 2).toInt()
                        drawable.setBounds(l, t, l + itemIconSize.toInt(), t + itemIconSize.toInt())
                        drawable.draw(canvas)
                    }

                    // 绘制文字
                    // 如果 alwaysShowText 为 true，则无视 fraction 阈值，始终绘制
                    if (alwaysShowText || fraction > 0.2f) {
                        textPaint.color = textColor

                        val maxTextW = itemWidth * 0.95f
                        val targetFontSize = calculateAutoTextSize(item.title, maxTextW, itemTextSize)

                        // 字号动画逻辑：
                        // 如果 alwaysShowText，字号变化不要太剧烈 (0.85 -> 1.0)，保持平稳
                        // 否则，字号从 0 -> 1.0 (这也是原有的弹跳效果)
                        val sizeFraction = if (alwaysShowText) (0.85f + 0.15f * fraction) else fraction
                        textPaint.textSize = targetFontSize * sizeFraction

                        // 计算文字基线
                        val textY = iconCenterY + (itemIconSize / 2) + itemPadding
                        val fontMetrics = textPaint.fontMetrics
                        val baseline = textY - fontMetrics.top / 2

                        canvas.drawText(item.title, itemCenterX, baseline, textPaint)
                    }

                } else {
                    // ============================================
                    // 样式 0: Side-by-Side (左右结构)
                    // ============================================

                    val maxTextWidth = itemWidth - itemIconSize - itemPadding
                    val finalFontSize = calculateAutoTextSize(item.title, maxTextWidth, itemTextSize)
                    textPaint.textSize = finalFontSize
                    val textRealWidth = textPaint.measureText(item.title)

                    // 宽度计算：左右结构依赖宽度展开动画。
                    // 如果 alwaysShowText，我们依然让宽度随 fraction 变化(保持挤压动画)，但让文字提前可见
                    val currentContentWidth = itemIconSize + (itemPadding + textRealWidth) * fraction
                    val startX = itemCenterX - (currentContentWidth / 2)

                    // 绘制图标
                    item.icon.let { drawable ->
                        DrawableCompat.setTint(drawable, iconColor)
                        val l = startX.toInt()
                        val t = (centerY - itemIconSize / 2).toInt()
                        drawable.setBounds(l, t, l + itemIconSize.toInt(), t + itemIconSize.toInt())
                        drawable.draw(canvas)
                    }

                    // 绘制文字
                    // Side-by-Side 模式下，如果 fraction 太小，文字会和图标重叠，所以即使 alwaysShow 也要保留一点阈值
                    if (alwaysShowText || fraction > 0.05f) {
                        textPaint.color = textColor
                        // 稍微调整一下 alwaysShow 下的透明度，避免重叠时太难看
                        if (alwaysShowText) {
                            textPaint.alpha = (255 * (0.3f + 0.7f * fraction)).toInt()
                        }

                        val textX = startX + itemIconSize + (itemPadding * fraction) + (textRealWidth / 2)
                        val fontMetrics = textPaint.fontMetrics
                        val textY = centerY - (fontMetrics.descent + fontMetrics.ascent) / 2

                        canvas.drawText(item.title, textX, textY, textPaint)
                    }
                }
            }
        }
    }

    /**
     * 触摸事件处理：负责点击检测和拖拽跟随
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 只关心 X 轴
        val currentX = event.x
        // 计算每个 Item 的理论宽度
        val totalLength = width - (sideMargins * 2)
        val itemWidth = totalLength / items.size

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                animator?.cancel() // 停止之前的动画，防止冲突
                isDragging = false // 重置状态
                isSnapping = false // 确保手动接管时，退出 Snapping 模式
                downX = currentX   // 记录按下位置
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // 判断是否超过了系统认定的“点击”范围，超过则视为“拖拽”
                if (!isDragging && abs(currentX - downX) > touchSlop) {
                    isDragging = true
                }

                // 如果是拖拽模式，指示器跟随手指移动
                if (isDragging) {
                    // 限制移动范围，防止划出边界
                    val minLimit = sideMargins + itemWidth / 2
                    val maxLimit = sideMargins + totalLength - itemWidth / 2
                    indicatorLocation = currentX.coerceIn(minLimit, maxLimit)

                    // 请求重绘 -> 触发 onDraw -> 更新 fraction -> 更新图标文字形态
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    // 情况 A: 拖拽释放
                    // 计算手指松开位置离哪个 Item 最近
                    isDragging = false
                    val relativePos = indicatorLocation - sideMargins
                    val targetIndex = (relativePos / itemWidth - 0.5f).roundToInt().coerceIn(0, items.size - 1)
                    snapToItem(targetIndex)
                } else {
                    // 情况 B: 点击
                    // 根据点击坐标计算目标 Item
                    val relativeClickPos = currentX - sideMargins
                    val clickIndex = (relativeClickPos / itemWidth).toInt().coerceIn(0, items.size - 1)
                    snapToItem(clickIndex)
                }
            }
        }
        return super.onTouchEvent(event)
    }


    /**
     * 执行吸附动画 (Snap Animation)
     */
    private fun snapToItem(index: Int) {
        // 如果点击的是同一个，可能只需要简单的回弹，不需要过滤逻辑
        if (activeItemIndex == index) {
            onItemReselected?.invoke(index)
            // 虽然位置没变，但也跑一下动画让它归位（防止拖拽一半松手的情况）
        } else {
            // 记录起点和终点，并标记开始 Snapping
            previousItemIndex = activeItemIndex
            activeItemIndex = index
            isSnapping = true

            onItemSelected?.invoke(index)
        }


        val totalLength = width - (sideMargins * 2)
        val itemWidth = totalLength / items.size

        // 目标位置：Item 的中心 X 坐标
        val targetLocation = sideMargins + (index * itemWidth) + (itemWidth / 2)

        animator?.cancel()
        animator = ValueAnimator.ofFloat(indicatorLocation, targetLocation).apply {
            duration = 300 // 动画时长 300ms
            interpolator = animInterpolator // 使用弹性插值器
            addUpdateListener {
                indicatorLocation = it.animatedValue as Float
                invalidate()
            }

            // 动画结束或取消时，重置 Snapping 标记
            // 使用 doOnEnd 需要引入 ktx 库，这里用原生 listener
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isSnapping = false
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    isSnapping = false
                }
            })


            start()
        }

        // 触发选中回调
        if (activeItemIndex != index) {
            activeItemIndex = index
            onItemSelected?.invoke(index)
        } else {
            onItemReselected?.invoke(index)
        }
    }


    /**
     * 辅助方法：自动缩小文字以适应宽度
     */
    private fun calculateAutoTextSize(text: String, maxWidth: Float, targetSize: Float): Float {
        var currentSize = targetSize
        textPaint.textSize = currentSize
        // 如果测量宽度大于最大可用宽度，且字号还大于8sp，则循环减小
        while (textPaint.measureText(text) > maxWidth && currentSize > 8f.sp()) {
            currentSize -= 1f
            textPaint.textSize = currentSize
        }
        return currentSize
    }


    /**
     * 辅助方法：颜色渐变插值器
     */
    private fun evaluateColor(fraction: Float, startValue: Int, endValue: Int): Int {
        val startA = Color.alpha(startValue);
        val endA = Color.alpha(endValue)
        val startR = Color.red(startValue);
        val endR = Color.red(endValue)
        val startG = Color.green(startValue);
        val endG = Color.green(endValue)
        val startB = Color.blue(startValue);
        val endB = Color.blue(endValue)

        return Color.argb(
            (startA + (fraction * (endA - startA))).toInt(),
            (startR + (fraction * (endR - startR))).toInt(),
            (startG + (fraction * (endG - startG))).toInt(),
            (startB + (fraction * (endB - startB))).toInt()
        )
    }

    // 扩展函数：dp 和 sp 转 px
    private fun Float.dp(): Float = this * resources.displayMetrics.density
    private fun Float.sp(): Float = this * resources.displayMetrics.scaledDensity

    /**
     * 数据实体类
     */
    data class BarItem(val title: String, val icon: Drawable?, val id: Int)
}