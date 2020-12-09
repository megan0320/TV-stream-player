LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_STATIC_JAVA_LIBRARIES := cipl \
				usbtuner \
				zxing

LOCAL_STATIC_JAVA_AAR_LIBRARIES  :=	cidanaverifylib \
					licenseserver

LOCAL_MODULE_TAGS := optional

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_SRC_FILES := \
    $(call all-java-files-under, app/src/main/java) \
    $(call all-subdir-java-files-under, app/src/main/java)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/app/src/main/res
LOCAL_ASSET_DIR :=  $(LOCAL_PATH)/app/src/main/assets
LOCAL_MANIFEST_FILE := app/src/main/AndroidManifest.xml
LOCAL_PACKAGE_NAME := DtvPlayer
LOCAL_JAVA_LIBRARIES := com.asuka.framework

#LOCAL_POST_INSTALL_CMD :=  mkdir $(TARGET_OUT_PRIVILEGED)/app/$(LOCAL_PACKAGE_NAME)/lib \
#							&& mkdir $(TARGET_OUT_PRIVILEGED)/app/$(LOCAL_PACKAGE_NAME)/lib/arm64 \
#							&& cp -r $(LOCAL_PATH)/app/src/main/jniLibs/arm64-v8a/*.so $(TARGET_OUT_PRIVILEGED)/app/$(LOCAL_PACKAGE_NAME)/lib/arm64


LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_OVERRIDES_PACKAGES := Home Launcher3


#LOCAL_JACK_ENABLED:=disabled
LOCAL_JACK_ENABLED:=full

LOCAL_PREBUILT_JNI_LIBS:= app/src/main/jniLibs/arm64-v8a/libCiplJni.so \
				app/src/main/jniLibs/arm64-v8a/libcidana_sdk.so \
				app/src/main/jniLibs/arm64-v8a/libarpresent.so \
				app/src/main/jniLibs/arm64-v8a/libatsccc_dec.so \
				app/src/main/jniLibs/arm64-v8a/libciaudec.so \
				app/src/main/jniLibs/arm64-v8a/libCiplMgr.so \
				app/src/main/jniLibs/arm64-v8a/libCIPLSDK.so \
				app/src/main/jniLibs/arm64-v8a/libcividec.so \
				app/src/main/jniLibs/arm64-v8a/libcividechw1-21.so \
				app/src/main/jniLibs/arm64-v8a/libciviproc.so \
				app/src/main/jniLibs/arm64-v8a/libCmmbMediaSample.so \
				app/src/main/jniLibs/arm64-v8a/libEndeavour-jni.so \
				app/src/main/jniLibs/arm64-v8a/libFileTuner.so \
				app/src/main/jniLibs/arm64-v8a/libITETuner.so \
				app/src/main/jniLibs/arm64-v8a/libSUBDecoder.so \
				app/src/main/jniLibs/arm64-v8a/libTVStatusMonitor-arm-android.so \
				app/src/main/jniLibs/arm64-v8a/libvrpresent.so \
				app/src/main/jniLibs/arm64-v8a/libvrpresent-gles2.so \
				app/src/main/jniLibs/arm64-v8a/libvrpresenthw1-21.so 

LOCAL_PROGUARD_ENABLED:= disabled

LOCAL_STATIC_JAVA_LIBRARIES := cipl \
				usbtuner \
				zxing
								
LOCAL_STATIC_JAVA_AAR_LIBRARIES  := cidanaverifylib \
					licenseserver	


#LOCAL_AAPT_FLAGS := --extra-packages com.cidana.cidanadtvsample


include $(BUILD_PACKAGE)

include $(CLEAR_VARS)


LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := cipl:app/libs/CiplJava.jar \
					usbtuner:app/libs/USBTunerDriver.jar \
					zxing:app/libs/ZXingCore.jar \
					cidanaverifylib:app/libs/com.cidana.verificationlibrary.aar \
					licenseserver:app/libs/LicenseServerLibrary.aar
include $(BUILD_MULTI_PREBUILT)
				


