# KIRI release R8 규칙
# 라이브러리(Retrofit, kotlinx.serialization, Hilt, Firebase, Coil, Credentials 등)는 각자 규칙을 함께 배포한다.
# 여기에는 라이브러리가 챙기지 않는 것만 적는다.

# 크래시 리포트에서 줄 번호를 볼 수 있게 남기고, 원본 소스 파일 이름은 숨긴다
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- 앱 ----
# 로그인·토큰 재발급 API 는 @NoAuth 로 토큰을 붙이지 않는다 (AuthInterceptor 가 실행 중에 리플렉션으로 확인).
# 어노테이션이 지워지면 이 요청들에도 토큰이 붙으므로 어노테이션과 Retrofit 메서드의 어노테이션 정보를 남긴다
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-keep @interface com.kiri.data.auth.NoAuth

# ---- 카카오 SDK (v2-common·v2-network·v2-auth 는 규칙을 배포하지 않는다) ----
# 카카오 개발자 문서의 ProGuard 안내: 서버 응답을 Gson 으로 읽는 모델 클래스의 필드를 남긴다
-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class com.kakao.sdk.**.model.* { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.**
