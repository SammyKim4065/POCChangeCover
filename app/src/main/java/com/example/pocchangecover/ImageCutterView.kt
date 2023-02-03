package com.example.pocchangecover

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView.ScaleType
import androidx.constraintlayout.widget.ConstraintLayout


class ImageCutterView : ConstraintLayout {
    private lateinit var imageView: ZoomImageView
    private lateinit var cutterFrame: View
    private var resultBitmap: Bitmap? = null

    var centreX: Float = 0F
    var centreY: Float = 0F

    private var _src: String = ""
    var src: String
        get() = _src
        set(value) {
            _src = value
            resolveOrigin()
        }

    // these matrices will be used to move and zoom image
    private var _matrix = Matrix()

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
    }

    private fun resolveOrigin() {
        if (src.isEmpty()) {
            return
        }
        val myBitmap = BitmapFactory.decodeFile(src)
        imageView.setImageBitmap(myBitmap)
    }

    /*
    create new bitmap by given size and rotation
    this may take a while, call async is recommended
     */
    fun shutter(): Bitmap? {
        val cut = Rect(
            (centreX - cutterFrame.width / 2 / imageView.scaleX).toInt(),
            (centreY - cutterFrame.height / 2 / imageView.scaleY).toInt(),
            (centreX + cutterFrame.width / 2 / imageView.scaleX).toInt(),
            (centreY + cutterFrame.height / 2 / imageView.scaleY).toInt()
        )
        val bitmap = BitmapFactory.decodeFile(src)
        val matrix = Matrix()
        matrix.postRotate(imageView.getRotation())
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)


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
}
