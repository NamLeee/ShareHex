package com.namlee.sharehex

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.namlee.sharehex.ui.theme.ShareHexTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        // TODO: for testing firebase
        // FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        setContent {
            ShareHexTheme {
                MainAppScreen(this)
            }
        }
    }
}

@Composable
fun FullScreenBackground() {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current).data(data = R.drawable.background_secure).apply(block = fun ImageRequest.Builder.() {
            scale(Scale.FILL)
        }).build()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp), contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter, contentDescription = "Full Screen Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun MainAppScreen(activity: FragmentActivity) {
    var isAuthenticated by remember { mutableStateOf(false) }

    if (isAuthenticated) {
        activity.window.setDecorFitsSystemWindows(true)
        activity.window.statusBarColor = android.graphics.Color.BLACK
        MainView()
    } else {
        FullScreenBackground()
        Utils.listFirebaseData.clear()
        SecureScreen(onAuthenticationSuccess = { isAuthenticated = true }, onAuthenticationError = { })
    }
}

@Composable
fun SecureScreen(onAuthenticationError: () -> Unit, onAuthenticationSuccess: () -> Unit) {
    val context = LocalContext.current
    val biometricManager = BiometricManager.from(context)
    var authenticationTriggered by remember { mutableStateOf(false) }

    if (!authenticationTriggered) {
        LaunchedEffect(Unit) {
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                showBiometricPrompt(context, onAuthenticationSuccess, onAuthenticationError)
                authenticationTriggered = true
            } else {
                onAuthenticationError()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Text("Đang xác thực...", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun MainView() {
    val db = Firebase.firestore

    val textFieldValue = remember { mutableStateOf("") }
    val listDataSaved = remember { mutableStateListOf<String>() }
    val listDataRef = db.collection("listHex")
    val needToUpdate = remember { mutableStateOf(true) }
    val context = LocalContext.current

    if (needToUpdate.value) {
        LaunchedEffect(Unit) {
            db.collection("listHex").orderBy("time").limit(20).get().addOnSuccessListener { result ->
                for (document in result) {
                    Log.d("lam.lv", "${document.id} => ${document.data["hex"]} => ${document.data["time"]} => ${document.data["model"]}")
                    if (Utils.listFirebaseData.none { it.id == document.id }) {
                        Utils.listFirebaseData.add(
                            HexData(
                                document.id, document.data["hex"].toString(), document.data["time"] as Long?, document.data["model"].toString()
                            )
                        )
                        listDataSaved.add(document.data["hex"].toString())
                    }
                }
            }
            needToUpdate.value = false
            Utils.setLastTimeUpdate(context, System.currentTimeMillis() + 2000) // Add 2 seconds delay
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Column {
            MyField(userInput = textFieldValue, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(10.dp))
            Column(modifier = Modifier.weight(2f)) {
                PressArea(userInput = textFieldValue, listDataSaved = listDataSaved, firebaseRef = listDataRef, needToUpdate = needToUpdate)
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .padding(10.dp)
                        .clip(shape = RoundedCornerShape(5.dp))
                ) {
                    items(listDataSaved.size) { index ->
                        ItemView(
                            index = index,
                            userInput = textFieldValue,
                            listDataSaved = listDataSaved,
                            firebaseRef = listDataRef,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PressArea(
    userInput: MutableState<String>,
    listDataSaved: MutableList<String>,
    modifier: Modifier = Modifier,
    firebaseRef: CollectionReference,
    needToUpdate: MutableState<Boolean>
) {
    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isBasicMode by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("Online mode") }

    var rotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(
        targetValue = rotation, animationSpec = tween(durationMillis = 600), label = "" // Duration of the rotation animation
    )


    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = mode, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(10.dp))
        Switch(checked = isBasicMode, onCheckedChange = {
            isBasicMode = !isBasicMode
            Utils.isBasicMode = isBasicMode
            mode = if (isBasicMode) {
                "Offline mode"
            } else {
                "Online mode "
            }
        })
        Spacer(modifier = Modifier.width(20.dp))
        IconButton(onClick = {
            Utils.listFirebaseData.clear()
            listDataSaved.clear()
            needToUpdate.value = true
            rotation += 360f
        }) {
            Icon(
                imageVector = Icons.Filled.Refresh, contentDescription = "Refresh", modifier = Modifier.graphicsLayer(rotationZ = animatedRotation)
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        ElevatedButton(
            onClick = {
                if (userInput.value.isEmpty()) {
                    return@ElevatedButton
                }
                var output = ""
                if (userInput.value.startsWith("http:") || userInput.value.startsWith(
                        "https:"
                    )
                ) {
                    output = Utils.textToHex(userInput.value)
                    Utils.saveToClipboard(mContext, userInput.value)
                } else {
                    try {
                        Utils.saveToClipboard(
                            mContext, Utils.hexToText(userInput.value)
                        )
                        output = userInput.value
                    } catch (e: NumberFormatException) {
                        if (!isBasicMode) {
                            Toast.makeText(mContext, "Mã Hex bị sai!", Toast.LENGTH_SHORT).show()
                            userInput.value = ""
                            return@ElevatedButton
                        } else {
                            Utils.saveToClipboard(
                                mContext, Utils.textToHex(userInput.value)
                            )
                        }

                    }
                }
                if (!isBasicMode) {
                    Utils.setLastTimeUpdate(mContext, System.currentTimeMillis())
                    coroutineScope.launch { saveToFirebase(firebaseRef, output) }
                    listDataSaved.add(output)
                }
                userInput.value = ""
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Bấm")
        }
    }

}


@Composable
fun ItemView(
    index: Int,
    userInput: MutableState<String>,
    listDataSaved: MutableList<String>,
    modifier: Modifier = Modifier,
    firebaseRef: CollectionReference,
) {
    var showPopup by remember { mutableStateOf(false) }
    var popupPosition by remember { mutableStateOf(Offset.Zero) }
    var popupText by remember { mutableStateOf("") }

    var offsetX by remember { mutableFloatStateOf(0f) }
    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()



    Column(modifier = Modifier
        .fillMaxWidth()
        .background(Color.LightGray)
        .offset { IntOffset(offsetX.roundToInt(), 0) }
        .clip(shape = RoundedCornerShape(10.dp))
        .padding(7.dp, 5.dp)
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = {
                val link = Utils.hexToText(listDataSaved[listDataSaved.size - 1 - index])
                if (link.startsWith("http")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    mContext.startActivity(intent)
                }

            }, onTap = { tapOffset ->
                popupPosition = tapOffset
                popupText = Utils.listFirebaseData[listDataSaved.size - 1 - index].toString()
                showPopup = true
            }, onDoubleTap = {
                try {
                    Utils.saveToClipboard(
                        mContext, Utils.hexToText(listDataSaved[listDataSaved.size - 1 - index])
                    )
                } catch (e: NumberFormatException) {
                    Toast
                        .makeText(mContext, "Mã Hex bị sai!!", Toast.LENGTH_SHORT)
                        .show()
                    userInput.value = ""
                    return@detectTapGestures
                }
            })
        }
        .pointerInput(Unit) {
            detectDragGestures(onDragEnd = {
                if (offsetX.absoluteValue > 500) {
                    val posNeedToDelete = listDataSaved.size - 1 - index
                    val idNeedToDelete = Utils.listFirebaseData[posNeedToDelete].id ?: ""
                    try {
                        listDataSaved.removeAt(posNeedToDelete)
                        Utils.listFirebaseData.removeAt(posNeedToDelete)
                    } catch (e: Exception) {
                        Log.e("lam.lv", "Remove ItemView: ", e)
                    }

                    coroutineScope.launch {
                        deleteFromFirebase(firebaseRef, idNeedToDelete)
                    }


                }
                offsetX = 0f
            }, onDrag = { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
            })
        }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = listDataSaved[listDataSaved.size - 1 - index],
                modifier = Modifier
                    .padding(10.dp, 10.dp)
                    .weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            val timeItem: Long? = Utils.listFirebaseData.find { it.hex == listDataSaved[listDataSaved.size - 1 - index] }?.time
            timeItem?.takeIf { it > Utils.getLastTimeUpdate(mContext) }?.let {
                Icon(imageVector = Icons.Rounded.Info, contentDescription = "New", tint = Color.Green)
            }
        }
        if (index != listDataSaved.size - 1) {
            HorizontalDivider()
        }
    }

    if (showPopup) {
        Popup(alignment = Alignment.TopStart,
            offset = IntOffset(popupPosition.x.toInt(), popupPosition.y.toInt()),
            properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
            onDismissRequest = { showPopup = false }) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .clip(shape = RoundedCornerShape(20.dp))
                    .background(color = Color.DarkGray.copy(alpha = 0.85f))
                    .padding(16.dp)
            ) {
                Text(text = popupText, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun MyField(
    userInput: MutableState<String>, modifier: Modifier = Modifier
) {
    OutlinedTextField(value = userInput.value,
        textStyle = TextStyle(Color.Black),
        onValueChange = { userInput.value = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(30.dp),
        label = { Text("NHẬP") },
        placeholder = { Text("Nhập Hex hoặc Link vào đây....", color = Color.Gray) })
}

suspend fun saveToFirebase(firebaseRef: CollectionReference, output: String) {
    Log.i("lam.lv", "saveToFirebase: ")
    withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val documentReference = firebaseRef.add(mapOf("hex" to output, "time" to currentTime, "model" to android.os.Build.MODEL)).await()

            Utils.listFirebaseData.add(HexData(documentReference.id, output, currentTime, android.os.Build.MODEL))
            Log.i("lam.lv", "saveToFirebase: Done with id = ${documentReference.id}")

        } catch (e: Exception) {
            Log.e("lam.lv", "Error adding document", e)
        }
    }
}

suspend fun deleteFromFirebase(firebaseRef: CollectionReference, idNeedToDelete: String) {
    Log.i("lam.lv", "deleteFromFirebase: start  -- $idNeedToDelete ")
    try {
        firebaseRef.document(idNeedToDelete).delete().await()
        Log.d("lam.lv", "Document with hex $idNeedToDelete deleted")
    } catch (e: Exception) {
        Log.e("lam.lv", "Error deleting document", e)
    }
}

fun showBiometricPrompt(
    context: Context, onAuthenticationSuccess: () -> Unit, onAuthenticationError: () -> Unit
) {

    val executor: Executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt =
        BiometricPrompt(context as ComponentActivity as FragmentActivity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onAuthenticationError()
                val activity = context as Activity
                activity.finish()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticationSuccess()
            }

        })

    val promptInfo =
        BiometricPrompt.PromptInfo.Builder().setTitle("Nhận dạng chính chủ").setSubtitle("Sử dụng vân tay để kiểm tra.").setNegativeButtonText("Huỷ")
            .setConfirmationRequired(false).build()

    biometricPrompt.authenticate(promptInfo)
}