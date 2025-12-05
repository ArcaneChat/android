.PHONY: apk
apk:
	./scripts/rebrand.sh
	./gradlew --offline assembleGplayRelease
	./scripts/undo_rebrand.sh

.PHONY: aab
aab:
	./scripts/rebrand.sh
	sed -i 's/signingConfigs.releaseApk/signingConfigs.releaseBundle/g' build.gradle
	./gradlew --offline bundleGplayRelease
	sed -i 's/signingConfigs.releaseBundle/signingConfigs.releaseApk/g' build.gradle
	./scripts/undo_rebrand.sh

.PHONY: foss
foss:
	./scripts/rebrand.sh
	./gradlew --offline assembleFossRelease
	./scripts/undo_rebrand.sh

.PHONY: install
install:
	adb -d install -r build/outputs/apk/gplay/release/*universal*.apk

.PHONY: emulator-install
emulator-install:
	adb install -r build/outputs/apk/gplay/release/*universal*.apk

.PHONY: fetch
fetch:
	git fetch upstream

.PHONY: clean
clean:
	./gradlew --offline clean


# CORE:

.PHONY: fetch-core
fetch-core:
	cd ../core && git fetch upstream

.PHONY: core
core:
	rmdir jni/deltachat-core-rust; mv ../core jni/deltachat-core-rust; true
	./scripts/ndk-make.sh; true
	./scripts/undo_rebrand.sh
	mv jni/deltachat-core-rust ../core
	mkdir jni/deltachat-core-rust

.PHONY: core-fast
core-fast:
	rmdir jni/deltachat-core-rust; mv ../core jni/deltachat-core-rust; true
	./scripts/ndk-make.sh arm64-v8a; true
	./scripts/undo_rebrand.sh
	mv jni/deltachat-core-rust ../core
	mkdir jni/deltachat-core-rust

.PHONY: core-v7
core-v7:
	rmdir jni/deltachat-core-rust; mv ../core jni/deltachat-core-rust; true
	./scripts/ndk-make.sh armeabi-v7a; true
	./scripts/undo_rebrand.sh
	mv jni/deltachat-core-rust ../core
	mkdir jni/deltachat-core-rust

.PHONY: core-x86
core-x86:
	rmdir jni/deltachat-core-rust; mv ../core jni/deltachat-core-rust; true
	./scripts/ndk-make.sh x86; true
	./scripts/undo_rebrand.sh
	mv jni/deltachat-core-rust ../core
	mkdir jni/deltachat-core-rust

.PHONY: link
link:
	rmdir jni/deltachat-core-rust; mv ../core jni/deltachat-core-rust; true

.PHONY: unlink
unlink:
	mv jni/deltachat-core-rust ../core
	mkdir jni/deltachat-core-rust
