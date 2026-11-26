package com.example.caffeinbyreya.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.caffeinbyreya.R
import com.example.caffeinbyreya.data.Menu
import com.example.caffeinbyreya.databinding.ActivityFormMenuBinding
import com.example.caffeinbyreya.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormMenuBinding
    private val viewModel: MainViewModel by viewModels()
    private var selectedPhotoUri: String = ""
    private var editMenuId: Int = -1

    private var pendingCameraUri: Uri? = null
    private var pendingCameraFilePath: String? = null

    private var activeSessionCameraPath: String? = null
    private val sessionCameraFiles = ArrayList<String>()
    private var isSaved: Boolean = false

    companion object {
        private const val STATE_SELECTED_PHOTO_URI = "state_selected_photo_uri"
        private const val STATE_PENDING_CAMERA_URI = "state_pending_camera_uri"
        private const val STATE_PENDING_CAMERA_PATH = "state_pending_camera_path"
        private const val STATE_ACTIVE_SESSION_CAMERA_PATH = "state_active_session_camera_path"
        private const val STATE_SESSION_CAMERA_FILES = "state_session_camera_files"
    }

    private fun renderPreviewPhoto(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(binding.ivPreviewPhoto)
    }

    // 1. Launcher Galeri (OpenDocument)
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            hapusFileKameraSesiKecuali(null)
            activeSessionCameraPath = null

            selectedPhotoUri = uri.toString()
            renderPreviewPhoto(uri)
        } else {
            Toast.makeText(this, getString(R.string.msg_no_photo_selected), Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Launcher Kamera (TakePicture)
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess: Boolean ->
        if (isSuccess && pendingCameraUri != null && pendingCameraFilePath != null) {
            val newPath = pendingCameraFilePath!!
            hapusFileKameraSesiKecuali(newPath)
            activeSessionCameraPath = newPath

            selectedPhotoUri = pendingCameraUri.toString()
            renderPreviewPhoto(pendingCameraUri!!)
        } else {
            pendingCameraFilePath?.let { path ->
                hapusFileFisik(path)
                sessionCameraFiles.remove(path)
            }
            Toast.makeText(this, getString(R.string.msg_batal_ambil_foto), Toast.LENGTH_SHORT).show()
        }
        pendingCameraUri = null
        pendingCameraFilePath = null
    }

    // 3. Launcher Izin Runtime CAMERA
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            bukaKamera()
        } else {
            tanganiIzinKameraDitolak()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categories = listOf(
            getString(R.string.cat_coffee),
            getString(R.string.cat_non_coffee),
            getString(R.string.cat_pastry)
        )
        binding.spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        if (savedInstanceState != null) {
            selectedPhotoUri = savedInstanceState.getString(STATE_SELECTED_PHOTO_URI, "")
            val pendingUriStr = savedInstanceState.getString(STATE_PENDING_CAMERA_URI)
            pendingCameraUri = pendingUriStr?.let { Uri.parse(it) }
            pendingCameraFilePath = savedInstanceState.getString(STATE_PENDING_CAMERA_PATH)
            activeSessionCameraPath = savedInstanceState.getString(STATE_ACTIVE_SESSION_CAMERA_PATH)
            savedInstanceState.getStringArrayList(STATE_SESSION_CAMERA_FILES)?.let {
                sessionCameraFiles.clear()
                sessionCameraFiles.addAll(it)
            }
        }

        editMenuId = intent.getIntExtra("MENU_ID", -1)
        if (editMenuId != -1) {
            binding.tvFormTitle.text = getString(R.string.title_edit_menu)
            binding.btnSaveMenu.text = getString(R.string.btn_update_menu)

            if (savedInstanceState == null) {
                lifecycleScope.launch {
                    viewModel.getMenuById(editMenuId)?.let { menu ->
                        binding.etMenuName.setText(menu.namaMenu)
                        binding.etMenuPrice.setText(menu.harga.toString())
                        binding.etMenuDesc.setText(menu.deskripsi)
                        selectedPhotoUri = menu.fotoUri
                        val pos = categories.indexOf(menu.kategori)
                        if (pos >= 0) binding.spinnerCategory.setSelection(pos)
                        if (selectedPhotoUri.isNotEmpty()) {
                            renderPreviewPhoto(Uri.parse(selectedPhotoUri))
                        }
                    }
                }
            }
        }

        if (selectedPhotoUri.isNotEmpty()) {
            renderPreviewPhoto(Uri.parse(selectedPhotoUri))
        }

        binding.btnPickPhoto.setOnClickListener {
            tampilkanDialogSumberFoto()
        }

        binding.btnSaveMenu.setOnClickListener {
            val nama = binding.etMenuName.text.toString().trim()
            val hargaStr = binding.etMenuPrice.text.toString().trim()
            val deskripsi = binding.etMenuDesc.text.toString().trim()
            val kategori = binding.spinnerCategory.selectedItem.toString()

            if (nama.isEmpty()) {
                binding.etMenuName.error = getString(R.string.err_empty_menu_name)
                return@setOnClickListener
            }
            val harga = hargaStr.toIntOrNull()
            if (harga == null || harga <= 0) {
                binding.etMenuPrice.error = getString(R.string.err_invalid_price)
                return@setOnClickListener
            }
            if (deskripsi.isEmpty()) {
                binding.etMenuDesc.error = getString(R.string.err_empty_desc)
                return@setOnClickListener
            }

            if (editMenuId == -1) {
                val newMenu = Menu(
                    namaMenu = nama,
                    kategori = kategori,
                    harga = harga,
                    deskripsi = deskripsi,
                    fotoUri = selectedPhotoUri
                )
                viewModel.insertMenu(newMenu) {
                    isSaved = true
                    hapusFileKameraSesiKecuali(activeSessionCameraPath)
                    Toast.makeText(this, getString(R.string.msg_menu_saved), Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                val updated = Menu(
                    id = editMenuId,
                    namaMenu = nama,
                    kategori = kategori,
                    harga = harga,
                    deskripsi = deskripsi,
                    fotoUri = selectedPhotoUri
                )
                viewModel.updateMenu(updated) {
                    isSaved = true
                    hapusFileKameraSesiKecuali(activeSessionCameraPath)
                    Toast.makeText(this, getString(R.string.msg_menu_updated), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_PHOTO_URI, selectedPhotoUri)
        outState.putString(STATE_PENDING_CAMERA_URI, pendingCameraUri?.toString())
        outState.putString(STATE_PENDING_CAMERA_PATH, pendingCameraFilePath)
        outState.putString(STATE_ACTIVE_SESSION_CAMERA_PATH, activeSessionCameraPath)
        outState.putStringArrayList(STATE_SESSION_CAMERA_FILES, sessionCameraFiles)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && !isSaved) {
            hapusFileKameraSesiKecuali(null)
        }
    }

    private fun tampilkanDialogSumberFoto() {
        val pilihan = arrayOf(
            getString(R.string.opsi_ambil_foto),
            getString(R.string.opsi_pilih_galeri)
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_sumber_foto_title))
            .setItems(pilihan) { _, which ->
                when (which) {
                    0 -> cekHardwareDanIzinKamera()
                    1 -> pickImageLauncher.launch(arrayOf("image/*"))
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun cekHardwareDanIzinKamera() {
        val punyaKamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (!punyaKamera) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.opsi_ambil_foto))
                .setMessage(getString(R.string.msg_kamera_tidak_tersedia))
                .setPositiveButton(getString(R.string.btn_gunakan_galeri)) { _, _ ->
                    pickImageLauncher.launch(arrayOf("image/*"))
                }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            bukaKamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun tanganiIzinKameraDitolak() {
        val bisaTanyaLagi = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.CAMERA
        )

        if (bisaTanyaLagi) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_izin_kamera_title))
                .setMessage(getString(R.string.dialog_izin_kamera_ditolak_msg))
                .setPositiveButton(getString(R.string.btn_gunakan_galeri)) { _, _ ->
                    pickImageLauncher.launch(arrayOf("image/*"))
                }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_izin_kamera_title))
                .setMessage(getString(R.string.dialog_izin_kamera_permanen_msg))
                .setPositiveButton(getString(R.string.btn_buka_pengaturan)) { _, _ ->
                    bukaPengaturanAplikasi()
                }
                .setNeutralButton(getString(R.string.btn_gunakan_galeri)) { _, _ ->
                    pickImageLauncher.launch(arrayOf("image/*"))
                }
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show()
        }
    }

    private fun bukaPengaturanAplikasi() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun bukaKamera() {
        val photoFile: File? = try {
            buatFileFoto()
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }

        if (photoFile == null) {
            Toast.makeText(this, getString(R.string.msg_gagal_buat_file_foto), Toast.LENGTH_SHORT).show()
            return
        }

        val photoUri: Uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        pendingCameraUri = photoUri
        takePictureLauncher.launch(photoUri)
    }

    @Throws(IOException::class)
    private fun buatFileFoto(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(filesDir, "Pictures").apply { if (!exists()) mkdirs() }

        return File.createTempFile(
            "MENU_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            pendingCameraFilePath = absolutePath
            sessionCameraFiles.add(absolutePath)
        }
    }

    private fun hapusFileKameraSesiKecuali(keepPath: String?) {
        val iterator = sessionCameraFiles.iterator()
        while (iterator.hasNext()) {
            val path = iterator.next()
            if (path != keepPath) {
                hapusFileFisik(path)
                iterator.remove()
            }
        }
    }

    private fun hapusFileFisik(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}