import 'package:vector_math/vector_math_64.dart';

import 'arcore_node.dart';

class ArCoreReferenceNode extends ArCoreNode {
  /// Filename of sfb object in assets folder (generated with Import Sceneform Asset)
  /// https://developers.google.com/ar/develop/java/sceneform/import-assets
  late final String object3DFileName;

  /// Url of gltf object for remote rendering
  late final String objectUrl;

  ArCoreReferenceNode({
    String? name,
    required this.object3DFileName,
    required this.objectUrl,
    List<ArCoreNode> children = const [],
    required Vector3 position,
    required Vector3 scale,
    required Vector4 rotation,
  }) : super(
            name: name,
            children: children,
            position: position,
            scale: scale,
            rotation: rotation);

  @override
  Map<String, dynamic> toMap() => <String, dynamic>{
        'object3DFileName': this.object3DFileName,
        'objectUrl': this.objectUrl,
      }..addAll(super.toMap());
}
