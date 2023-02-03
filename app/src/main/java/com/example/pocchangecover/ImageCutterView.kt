package com.example.pocchangecover

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.*
import kotlin.concurrent.schedule


class ImageCutterView : ConstraintLayout {
    private lateinit var imageView: ZoomImageView
    private lateinit var cutterFrame: View

    private var limitRect = RectF()
    private var resultBitmap: Bitmap? = null

    private var inDrag = false
    private var lazyCheck: TimerTask? = null
    private var inCheck = false

    var centreX: Float = 0F
    var centreY: Float = 0F

    private var _src: String = ""
    private var _ratio: String = "1:1"
    private val defaultMinScale = 0.05f
    private val defaultMaxScale = 2.95f
    private var _minScale: Float = defaultMinScale
    private var _maxScale: Float = defaultMaxScale
    var src: String
        get() = _src
        set(value) {
            _src = value
            resolveOrigin()
        }
    var ratio: String
        get() = _ratio
        set(value) {
            _ratio = if (value.matches(Regex("""[0-9]+:[0-9]+"""))) {
                value
            } else "1:1"
            (cutterFrame.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio = _ratio
        }
//    var maxScale: Float
//        get() = _maxScale
//        set(value) {
//            _maxScale = if (value > 0) value else defaultMaxScale
//            imageView.maxScale = _maxScale
//        }
//    var minScale: Float
//        get() = _minScale
//        set(value) {
//            _minScale = if (value > 0) value else defaultMinScale
//            imageView.minScale = _minScale
//        }


    // For logging purposes
    private val TAG = ZoomImageView::class.java.simpleName


    // these matrices will be used to move and zoom image
    private var _matrix = Matrix()
    private val savedMatrix = Matrix()

    // we can be in one of these 3 states
    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2
    private var mode = NONE

    // remember some things for zooming
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    private var d = 0f
    private var newRot = 0f
    private var lastEvent: FloatArray? = null
    var rotation = true
    var scaledown = true
    var initStatus = false
    var init_scale = 0f

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init(attrs: AttributeSet?, defStyle: Int) {
        FrameLayout.inflate(context, R.layout.image_cutter_view, this)

        cutterFrame = findViewById(R.id.ymage_cutter_frame)
        imageView = findViewById(R.id.ymage_cutter_origin)

        imageView.scaleType = ScaleType.MATRIX
        _matrix = imageView.imageMatrix


         centreX = imageView.x + imageView.width / 2
         centreY = imageView.y + imageView.height / 2


//        imageView.setOnTouchListener { view, event ->
//            when (event.action and MotionEvent.ACTION_MASK) {
//                MotionEvent.ACTION_DOWN -> {
//                    savedMatrix.set(matrix)
//                    start[event.x] = event.y
//                    mode = DRAG
//                    lastEvent = null
//
//                    inDrag = true
//                }
//                MotionEvent.ACTION_POINTER_DOWN -> {
//                    oldDist = spacing(event)
//                    if (oldDist > 10f) {
//                        savedMatrix.set(matrix)
//                        midPoint(mid, event)
//                        mode = ZOOM
//                    }
//                    lastEvent = FloatArray(4)
//
//                    lastEvent!![0] = event.getX(0)
//                    lastEvent!![1] = event.getX(1)
//                    lastEvent!![2] = event.getY(0)
//                    lastEvent!![3] = event.getY(1)
//                    d = rotation(event)
//                }
//                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
//                    mode = NONE
//                    lastEvent = null
//
//                    inDrag = false
//                    inCheck = false
//                    lazyCheck?.cancel()
//                    lazyCheck = null
//                    lazyCheck = Timer().schedule(100) {
//                        if (!inDrag && !inCheck) {
//                            inCheck = true
//                            resolvePositionCheck()
//                        }
//                    }
//                }
//                MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
//                    matrix.set(savedMatrix)
//                    val dx: Float = event.x - start.x
//                    val dy: Float = event.y - start.y
//                    matrix.postTranslate(dx, dy)
//                } else if (mode == ZOOM) {
//                    val newDist = spacing(event)
//                    if (newDist > 10f) {
//                        matrix.set(savedMatrix)
//                        val scale = newDist / oldDist
//                        matrix.postScale(scale, scale, mid.x, mid.y)
//                    }
//                    if (lastEvent != null && event.pointerCount == 2 || event.pointerCount == 3) {
//                        newRot = rotation(event)
//                        val r = newRot - d
//                        val values = FloatArray(9)
//                        matrix.getValues(values)
//                        val tx = values[2]
//                        val ty = values[5]
//                        val sx = values[0]
//                        val xc = this.width / 2 * sx
//                        val yc = this.height / 2 * sx
//                        if (rotation) matrix.postRotate(r, tx + xc, ty + yc)
//                    }
//                }
//            }
//
//            imageView.imageMatrix = matrix
//            val vals = FloatArray(9)
//            matrix.getValues(vals)
//            for (i in vals.indices) {
//                if (!scaledown && i == 0 && vals[i] < init_scale) {
//                    val drawableRect = RectF(
//                        0F,
//                        0F,
//                        imageView.drawable.intrinsicWidth.toFloat(),
//                        imageView.drawable.intrinsicHeight.toFloat()
//                    )
//                    val viewRect = RectF(0F, 0F, width.toFloat(), height.toFloat())
//                    matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER)
//                    imageView.imageMatrix = matrix
//                }
//                break
//            }
//            invalidate()
//            return@setOnTouchListener true
//        }


//        imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
//
//        // Load attributes
//        val a = context.obtainStyledAttributes(
//                attrs, R.styleable.ImageCutterView, defStyle, 0)
//        _ratio = a.getString(R.styleable.ImageCutterView_ratio) ?: "1:1"
//        _maxScale = a.getDimension(R.styleable.ImageCutterView_maxScale, defaultMaxScale)
//        _minScale = a.getDimension(R.styleable.ImageCutterView_minScale, defaultMinScale)
//        a.recycle()
//
//        (cutterFrame.layoutParams as LayoutParams).dimensionRatio = _ratio
//
//        imageView.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
//            override fun onImageLoaded() {
//                minScale = if (imageView.orientation == rotation0 || imageView.orientation == rotation180) {
//                    imageView.setScaleAndCenter(context.getWindowWidth()/imageView.sWidth.toFloat(), PointF(imageView.sWidth/2f, imageView.sHeight/2f))
//                    Math.max(Math.max(cutterFrame.width / imageView.sWidth.toFloat(), cutterFrame.height / imageView.sHeight.toFloat()), defaultMinScale)
//                } else {
//                    imageView.setScaleAndCenter(context.getWindowWidth()/imageView.sHeight.toFloat(), PointF(imageView.sHeight/2f, imageView.sWidth/2f))
//                    Math.max(Math.max(cutterFrame.width / imageView.sHeight.toFloat(), cutterFrame.height / imageView.sWidth.toFloat()), defaultMinScale)
//                }
//                maxScale = Math.max(minScale, _maxScale)
//                Timber.d("MinScale:$minScale,MaxScale:$maxScale")
//            }
//            override fun onImageLoadError(e: Exception?) {}
//            override fun onPreviewLoadError(e: Exception?) {}
//            override fun onPreviewReleased() {}
//            override fun onReady() {}
//            override fun onTileLoadError(e: Exception?) {}
//        })
//        imageView.setOnStateChangedListener(object : SubsamplingScaleImageView.OnStateChangedListener {
//            override fun onCenterChanged(newCenter: PointF?, origin: Int) {
//                if (inDrag || inCheck) {
//                    return
//                }
//                lazyCheck?.cancel()
//                lazyCheck = null
//                lazyCheck = Timer().schedule(100) {
//                    if (!inDrag && !inCheck) {
//                        inCheck = true
//                        resolvePositionCheck()
//                    }
//                }
//            }
//            override fun onScaleChanged(newScale: Float, origin: Int) {
//            }
//        })
//
//        imageView.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                    inDrag = false
//                    inCheck = false
//                    lazyCheck?.cancel()
//                    lazyCheck = null
//                    lazyCheck = Timer().schedule(100) {
//                        if (!inDrag && !inCheck) {
//                            inCheck = true
//                            resolvePositionCheck()
//                        }
//                    }
//                }
//                MotionEvent.ACTION_DOWN -> {
//                    inDrag = true
//                }
//
//            }
//            return@setOnTouchListener false
//        }
    }

    private fun resolveOrigin() {
        if (src.isEmpty()) {
            return
        }
        val myBitmap = BitmapFactory.decodeFile(src)
        imageView.setImageBitmap(myBitmap)

//        imageView.setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_OUTSIDE)
    }

    /*
    create new bitmap by given size and rotation
    this may take a while, call async is recommended
     */
    fun shutter(): Bitmap? {
        if (resultBitmap?.isRecycled == false) {
            resultBitmap?.recycle()
        }
//        val center = imageView.center ?: return null
        val imageScale = (imageView.scaleX * imageView.scaleY)

        val cut = Rect(
            (centreX - cutterFrame.width / 2 ).toInt(),
            (centreY - cutterFrame.height / 2).toInt(),
            (centreX + cutterFrame.width / 2).toInt(),
            (centreY + cutterFrame.height / 2).toInt()
        )
        val bitmap = BitmapFactory.decodeFile(src)
        val matrix = Matrix()
        matrix.postRotate(imageView.init_scale)
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)


        resultBitmap = Bitmap.createBitmap(
            rotatedBitmap,
            Math.max(cut.left, 0),
            Math.max(cut.top, 0),
            Math.min(cut.right - cut.left, rotatedBitmap.width),
            Math.min(cut.bottom - cut.top, rotatedBitmap.height),
            null,
            true
        )
        if (bitmap != resultBitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        if (rotatedBitmap != resultBitmap && !rotatedBitmap.isRecycled) {
            rotatedBitmap.recycle()
        }
        return resultBitmap
    }

//    fun reset() {
//        imageView.orientation = rotation0
//        imageView.setScaleAndCenter(
//            context.getWindowWidth() / imageView.sWidth.toFloat(),
//            PointF(imageView.sWidth / 2f, imageView.sHeight / 2f)
//        )
//    }
//
//    fun rotate(rotate: Int) {
//        imageView.orientation = rotate
//        minScale = if (rotate == rotation0 || rotate == rotation180) {
//            imageView.setScaleAndCenter(
//                context.getWindowWidth() / imageView.sWidth.toFloat(),
//                PointF(imageView.sWidth / 2f, imageView.sHeight / 2f)
//            )
//            Math.max(
//                Math.max(
//                    cutterFrame.width / imageView.sWidth.toFloat(),
//                    cutterFrame.height / imageView.sHeight.toFloat()
//                ), defaultMinScale
//            )
//        } else {
//            imageView.setScaleAndCenter(
//                context.getWindowWidth() / imageView.sHeight.toFloat(),
//                PointF(imageView.sHeight / 2f, imageView.sWidth / 2f)
//            )
//            Math.max(
//                Math.max(
//                    cutterFrame.width / imageView.sHeight.toFloat(),
//                    cutterFrame.height / imageView.sWidth.toFloat()
//                ), defaultMinScale
//            )
//        }
//        maxScale = Math.max(minScale, _maxScale)
//        Timber.d("MinScale:$minScale,MaxScale:$maxScale")
//    }

    private fun resolvePositionCheck() {
//        val center = imageView.center ?: return
        val x: Float
        val y: Float
        if (imageView.getRotation().toInt() == rotation0 || imageView.getRotation().toInt() == rotation180) {
            val left = cutterFrame.width / 2 / imageView.scaleX
            val top = cutterFrame.height / 2 / imageView.scaleY
            val right =
                left + (imageView.width * imageView.scaleX - cutterFrame.width) / imageView.scaleX
            val bottom =
                top + (imageView.height * imageView.scaleY - cutterFrame.height) / imageView.scaleY
            limitRect.set(left, top, right, bottom)
            x = if (centreX < limitRect.left) {
                if (imageView.width * imageView.scaleX < cutterFrame.width) {
                    if (centreX < limitRect.right) {
                        limitRect.right
                    } else {
                        centreX
                    }
                } else {
                    limitRect.left
                }
            } else if (centreX > limitRect.right) {
                if (imageView.width * imageView.scaleX < cutterFrame.width) {
                    limitRect.left
                } else {
                    limitRect.right
                }
            } else {
                centreX
            }
            y = if (centreY < limitRect.top) {
                if (imageView.height * imageView.scaleY < cutterFrame.height) {
                    if (centreY < limitRect.bottom) {
                        limitRect.bottom
                    } else {
                        centreY
                    }
                } else {
                    limitRect.top
                }
            } else if (centreY > limitRect.bottom) {
                if (imageView.height * imageView.scaleY < cutterFrame.height) {
                    limitRect.top
                } else {
                    limitRect.bottom
                }
            } else {
                centreY
            }
        } else {
            val left = cutterFrame.width / 2 / imageView.scaleX
            val top = cutterFrame.height / 2 / imageView.scaleY
            val right =
                left + (imageView.height * imageView.scaleY - cutterFrame.width) / imageView.scaleX
            val bottom =
                top + (imageView.width * imageView.scaleX - cutterFrame.height) / imageView.scaleY
            limitRect.set(left, top, right, bottom)
            x = if (centreX < limitRect.left) {
                if (imageView.height * imageView.scaleY < cutterFrame.width) {
                    if (centreX < limitRect.right) {
                        limitRect.right
                    } else {
                        centreX
                    }
                } else {
                    limitRect.left
                }
            } else if (centreX > limitRect.right) {
                if (imageView.height * imageView.scaleY < cutterFrame.width) {
                    limitRect.left
                } else {
                    limitRect.right
                }
            } else {
                centreX
            }
            y = if (centreY < limitRect.top) {
                if (imageView.width * imageView.scaleX < cutterFrame.height) {
                    if (centreY < limitRect.bottom) {
                        limitRect.bottom
                    } else {
                        centreY
                    }
                } else {
                    limitRect.top
                }
            } else if (centreY > limitRect.bottom) {
                if (imageView.width * imageView.scaleX < cutterFrame.height) {
                    limitRect.top
                } else {
                    limitRect.bottom
                }
            } else {
                centreY
            }
        }
//        if (x != centreX || y != centreY) {
//            GlobalScope.launch(Dispatchers.Main) {
//                (imageView).animateCenter(PointF(x, y))
//                    ?.withDuration(100)
//                    ?.withEasing(SubsamplingScaleImageView.EASE_OUT_QUAD)
//                    ?.withInterruptible(false)
//                    ?.start()
//            }
//        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (resultBitmap?.isRecycled == false) {
            resultBitmap?.recycle()
        }
        lazyCheck?.cancel()
        lazyCheck = null
    }

    companion object {
        const val rotation90 = 90
        const val rotation180 = 180
        const val rotation270 = 270
        const val rotation0 = 0
    }


    fun Context.getWindowWidth(): Int {
        return this.resources.displayMetrics.widthPixels
    }
}
