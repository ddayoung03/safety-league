# Pleos SDK는 내부에서 리플렉션과 AIDL을 쓴다. 이름을 바꾸면 차량 신호가 끊기므로 그대로 둔다.
-keep class ai.pleos.** { *; }
-keep interface ai.pleos.** { *; }
-dontwarn ai.pleos.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
