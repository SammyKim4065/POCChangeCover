package com.example.pocchangecover

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File


class MainActivity : AppCompatActivity() {
    private val FILE_NAME = "changeCover.jpg"
    lateinit var photoFile: File
    private val activity = this@MainActivity

    lateinit var chooseImageView: TextView
    lateinit var ivCrop: ImageCutterView
    lateinit var selectedImageBtn: Button
    lateinit var displayImage: ImageView

    private val PERMISSION_REQUEST_CAMERA_CODE = 201
    private val PERMISSION_REQUEST_GALLARY_CODE = 202
    private val PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    fun init() {
        chooseImageView = findViewById(R.id.chooseNewImageBtn)
        selectedImageBtn = findViewById(R.id.selectedImageBtn)
        displayImage = findViewById(R.id.displayImage)
        ivCrop = findViewById(R.id.ivCrop)

        chooseImageView.setOnClickListener(clickListener)


        selectedImageBtn.setOnClickListener {
            val cutResBitMap = ivCrop.shutter()

            displayImage.visibility = View.VISIBLE
            displayImage.setImageBitmap(cutResBitMap)
            ivCrop.visibility = View.GONE
        }
    }

    private val clickListener: View.OnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.chooseNewImageBtn -> {
                if (!checkPermission()) {
                    requestPermission()
                } else {
                    takePicture()

                    displayImage.visibility = View.GONE
                    ivCrop.visibility = View.VISIBLE
                }
            }
        }
    }

    fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val result2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
                && result2 == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission() {
        ActivityCompat.requestPermissions(
            activity, arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), PERMISSION_REQUEST_CODE
        )
    }

    fun takePicture() {
        val options = arrayOf<CharSequence>("Camera", "Gallery", "Cancel")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Option")
        builder.setItems(options) { dialog, item ->
            if (options[item] == "Camera") {
                photoFile = getPhotoFileName()

                val fileProvider = FileProvider.getUriForFile(this,"com.example.pocchangecover.fileprovider",photoFile)
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

                startActivityForResult(cameraIntent, PERMISSION_REQUEST_CAMERA_CODE)


            } else if (options[item] == "Gallery") {

                val pickPhoto =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhoto, PERMISSION_REQUEST_GALLARY_CODE)

            } else if (options[item] == "Cancel") {
                dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun getPhotoFileName(): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(FILE_NAME, ".jpg", storageDirectory)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PERMISSION_REQUEST_CAMERA_CODE) {
                ivCrop.src = photoFile.absolutePath

            } else if (requestCode == PERMISSION_REQUEST_GALLARY_CODE) {

                val selectedImageUri = data!!.data
                val picturePath = getPath(this, selectedImageUri)

                ivCrop.src = picturePath

            }
        }
    }

    fun getPath(context: Context, uri: Uri?): String {
        var result: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = uri?.let { context.contentResolver.query(it, proj, null, null, null) }
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex: Int = cursor.getColumnIndexOrThrow(proj[0])
                result = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        if (result == null) {
            result = "Not found"
        }
        return result
    }
}