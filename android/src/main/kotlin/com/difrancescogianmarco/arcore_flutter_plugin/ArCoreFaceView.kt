package com.difrancescogianmarco.arcore_flutter_plugin

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.difrancescogianmarco.arcore_flutter_plugin.utils.ArCoreUtils
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.Pose
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.math.Vector3
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlin.collections.HashMap

class ArCoreFaceView(activity:Activity,context: Context, messenger: BinaryMessenger, id: Int, debug: Boolean) : BaseArCoreView(activity, context, messenger, id, debug) {

    private val TAG: String = ArCoreFaceView::class.java.name
    private var faceRegionsRenderable: ModelRenderable? = null
    private var faceMeshTexture: Texture? = null
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()
    private var faceSceneUpdateListener: Scene.OnUpdateListener
    

    init {
        faceSceneUpdateListener = Scene.OnUpdateListener { frameTime ->
            run {
                //                if (faceRegionsRenderable == null || faceMeshTexture == null) {
                if (faceMeshTexture == null) {
                    return@OnUpdateListener
                }
                val faceList = arSceneView?.session?.getAllTrackables(AugmentedFace::class.java)

                faceList?.let {
                    // Make new AugmentedFaceNodes for any new faces.
                    for (face in faceList) {
                        if (!faceNodeMap.containsKey(face)) {
                            val faceNode = AugmentedFaceNode(face)
                            faceNode.setParent(arSceneView?.scene)
                            faceNode.faceRegionsRenderable = faceRegionsRenderable
                            faceNode.faceMeshTexture = faceMeshTexture
                            faceNodeMap[face] = faceNode

                            // change assets on runtime
                        } else if(faceNodeMap[face]?.faceRegionsRenderable != faceRegionsRenderable  ||  faceNodeMap[face]?.faceMeshTexture != faceMeshTexture ){
                            faceNodeMap[face]?.faceRegionsRenderable = faceRegionsRenderable
                            faceNodeMap[face]?.faceMeshTexture = faceMeshTexture
                        }
                    }

                    // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
                    val iter = faceNodeMap.iterator()
                    while (iter.hasNext()) {
                        val entry = iter.next()
                        val face = entry.key
                        if (face.trackingState == TrackingState.STOPPED) {
                            val faceNode = entry.value
                            faceNode.setParent(null)
                            iter.remove()
                        }
                    }
                }
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if(isSupportedDevice){
            debugLog(call.method +"called on supported device")
            when (call.method) {
                "init" -> {
                    arScenViewInit(call, result)
                }
                "loadMesh" -> {
                    val map = call.arguments as HashMap<*, *>
                    val textureBytes = map["textureBytes"] as ByteArray
                    val skin3DModelFilename = map["skin3DModelFilename"] as? String
                    loadMesh(textureBytes, skin3DModelFilename)
                }
                "getLandmarks" -> {
                    val map = call.arguments as HashMap<*,*>
                    val parameter = map["parameter"] as Int
                    var landmark = getLandmarks(parameter)
                    result.success(landmark)
                }
                "getCenterPose" -> {
                    var centerPose = getCenterPose()
                    result.success(centerPose)
                }
                "getWorldPosition" -> {
                    val position = getWorldPosition()
                    result.success(position)
                }
                "dispose" -> {
                    debugLog( " updateMaterials")
                    dispose()
                }
                else -> {
                    result.notImplemented()
                }
            }
        }else{
            debugLog("Impossible call " + call.method + " method on unsupported device")
            result.error("Unsupported Device","",null)
        }
    }

    fun loadMesh(textureBytes: ByteArray?, skin3DModelFilename: String?) {
        if (skin3DModelFilename != null) {
            // Load the face regions renderable.
            // This is a skinned model that renders 3D objects mapped to the regions of the augmented face.
            ModelRenderable.builder()
                    .setSource(activity, Uri.parse(skin3DModelFilename))
                    .build()
                    .thenAccept { modelRenderable ->
                        faceRegionsRenderable = modelRenderable
                        modelRenderable.isShadowCaster = false
                        modelRenderable.isShadowReceiver = false
                    }
        }

        // Load the face mesh texture.
        Texture.builder()
                //.setSource(activity, Uri.parse("fox_face_mesh_texture.png"))
                .setSource(BitmapFactory.decodeByteArray(textureBytes, 0, textureBytes!!.size))
                .build()
                .thenAccept { texture -> faceMeshTexture = texture }
    }

    private fun getLandmarks(parameter: Int) : List<Float> {
        val faceList = arSceneView?.session?.getAllTrackables(AugmentedFace::class.java)
        faceList?.let {
            for (face in faceList){
                var buffer = face.getMeshVertices()
                var landmark = listOf(buffer.get(parameter * 3), buffer.get(parameter * 3+1), buffer.get(parameter * 3+2))
                return landmark
                }
            }
        return listOf(0.0f, 0.0f, 0.0f)
    }

    private fun getCenterPose() : Pose? {
        val faceList = arSceneView?.session?.getAllTrackables(AugmentedFace::class.java)
        faceList?.let {
            for (face in faceList){
                var facePose = face.getCenterPose()
                return facePose
                }
            }
        return null
    }

    private fun arScenViewInit(call: MethodCall, result: MethodChannel.Result) {
        val enableAugmentedFaces: Boolean? = call.argument("enableAugmentedFaces")
        if (enableAugmentedFaces != null && enableAugmentedFaces) {
            // This is important to make sure that the camera stream renders first so that
            // the face mesh occlusion works correctly.
            arSceneView?.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST
            arSceneView?.scene?.addOnUpdateListener(faceSceneUpdateListener)
        }

        result.success(null)
    }

    private fun getWorldPosition() : List<Float>? {
        val faceList = arSceneView?.session?.getAllTrackables(AugmentedFace::class.java)
        faceList?.let {
            for (face in faceList){
                val buffer = face.getMeshVertices();
                val vectorLeftEyeLeft = Vector3(buffer.get(33 * 3), buffer.get((33 * 3) + 1), buffer.get((33 * 3) + 2));
                val vectorLeftEyeRight = Vector3(buffer.get(133 * 3),buffer.get((133 * 3) + 1), buffer.get((133 * 3) + 2));
                val node = Node()
                node.setLocalPosition(Vector3((vectorLeftEyeLeft.x + vectorLeftEyeRight.x) / 2, (vectorLeftEyeLeft.y + vectorLeftEyeRight.y) / 2, (vectorLeftEyeLeft.z + vectorLeftEyeRight.z) / 2))
                node.setParent(faceNodeMap.get(face))
                val pos = node.getWorldPosition()
                val scene = arSceneView?.getScene()
                println(scene)
                // screenPointList = listOf(0.0f, 0.0f, 0.0f)
                screenPointList = null
                val camera = scene?.getCamera()
                camera?.let{
                    screenPoint = it.worldToScreenPoint(pos)
                    screenPointList = listOf(screenPoint.x, screenPoint.y, screenPoint.z)
                }
                return screenPointList
            }
        }
        return null
    }

    override fun onResume() {
        if (arSceneView == null) {
            return
        }

        if (arSceneView?.session == null) {

            // request camera permission if not already requested
            if (!ArCoreUtils.hasCameraPermission(activity)) {
                ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
            }

            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                val session = ArCoreUtils.createArSession(activity, installRequested, true)
                if (session == null) {
                    installRequested = false
                    return
                } else {
                    val config = Config(session)
                    config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    session.configure(config)
                    arSceneView?.setupSession(session)
                }
            } catch (e: UnavailableException) {
                ArCoreUtils.handleSessionException(activity, e)
            }
        }

        try {
            arSceneView?.resume()
        } catch (ex: CameraNotAvailableException) {
            ArCoreUtils.displayError(activity, "Unable to get camera", ex)
            activity.finish()
            return
        }

    }

    override fun onDestroy() {
        arSceneView?.scene?.removeOnUpdateListener(faceSceneUpdateListener)
        super.onDestroy()
    }

}