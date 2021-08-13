import 'package:vector_math/vector_math_64.dart';

Map<String, dynamic>? convertVector3ToMap(Vector3? vector) =>
    vector == null ? null : {'x': vector.x, 'y': vector.y, 'z': vector.z};

Map<String, dynamic>? convertVector4ToMap(Vector4? vector) => vector == null
    ? null
    : {'x': vector.x, 'y': vector.y, 'z': vector.z, 'w': vector.w};
