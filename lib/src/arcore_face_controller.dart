import 'dart:ffi';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

import '../arcore_flutter_plugin.dart';

class ArCoreFaceController {
  ArCoreFaceController(
      {required int id,
      required this.enableAugmentedFaces,
      this.debug = false}) {
    _channel = MethodChannel('arcore_flutter_plugin_$id');
    _channel.setMethodCallHandler(_handleMethodCalls);
    init();
  }

  final bool enableAugmentedFaces;
  final bool debug;
  late MethodChannel _channel;
  late StringResultHandler onError;

  init() async {
    try {
      await _channel.invokeMethod<void>('init', {
        'enableAugmentedFaces': enableAugmentedFaces,
      });
    } on PlatformException catch (ex) {
      print(ex.message);
    }
  }

  Future<dynamic> _handleMethodCalls(MethodCall call) async {
    if (debug) {
      print('_platformCallHandler call ${call.method} ${call.arguments}');
    }
    switch (call.method) {
      case 'onError':
        if (onError != null) {
          onError(call.arguments);
        }
        break;
      default:
        if (debug) {
          print('Unknown method ${call.method}');
        }
    }
    return Future.value();
  }

  Future<void> loadMesh(
      {required Uint8List textureBytes, String? skin3DModelFilename}) {
    assert(textureBytes != null);
    return _channel.invokeMethod('loadMesh', {
      'textureBytes': textureBytes,
      'skin3DModelFilename': skin3DModelFilename
    });
  }

  Future<dynamic> getMeshVertice({required int index}) async {
    return await _channel.invokeMethod('getLandmarks', {'parameter': index});
  }

  Future<dynamic> getCenterPose() async {
    return await _channel.invokeMethod('getCenterPose');
  }

  Future<dynamic> getScreenPosition({required int index}) async {
    return await _channel
        .invokeMethod('getScreenPosition', {'parameter': index});
  }

  void dispose() {
    _channel?.invokeMethod<void>('dispose');
  }
}
