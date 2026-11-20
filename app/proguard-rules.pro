# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserva nombres de archivo, números de línea y firmas genéricas para monitoreo de crashes y Retrofit/Moshi
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Metadatos de Kotlin requeridos por Moshi KotlinJsonAdapterFactory (reflexión)
-keep class kotlin.Metadata { *; }

# DTOs de autenticación, sesión y perfil serializados/deserializados por Retrofit + Moshi
-keep class com.example.data.remote.AuthUser { *; }
-keep class com.example.data.remote.AuthResponse { *; }
-keep class com.example.data.remote.RegistroRequest { *; }
-keep class com.example.data.remote.InicioSesionRequest { *; }
-keep class com.example.data.remote.SolicitarRecuperacionRequest { *; }
-keep class com.example.data.remote.RestablecerContrasenaRequest { *; }
-keep class com.example.data.remote.GoogleAuthRequest { *; }
-keep class com.example.data.remote.UsuarioRespuesta { *; }
-keep class com.example.data.remote.PerfilUsuarioRespuesta { *; }
-keep class com.example.data.remote.PerfilRespuesta { *; }
-keep class com.example.data.remote.PerfilActualizarRequest { *; }
-keep class com.example.data.remote.PerfilActualizadoRespuesta { *; }

# Interfaz Retrofit del backend móvil (mantiene métodos y anotaciones de red)
-keep,allowobfuscation,allowshrinking interface com.example.data.remote.EleganAuthApi {
    <methods>;
}
