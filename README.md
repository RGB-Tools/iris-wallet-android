# Iris Wallet

Iris Wallet manages RGB assets from issuance to spending and receiving,
wrapping all functionality in a familiar-looking wallet application and
abstracting away as many technical details as possible.

The RGB and Bitcoin functionality is provided by [rgb-lib] via
[rgb-lib-kotlin].


## Variants

The project has Signet and Testnet variants.

Together with the Debug and Release targets, this produces 6 possible versions
of the app:
- bitcoinSignetDebug
- bitcoinSignetRelease
- bitcoinTestnetDebug
- bitcoinTestnetRelease
- bitcoinMainnetDebug
- bitcoinMainnetRelease


### Supported architectures

The project produces an application supporting the following architectures:
- `x86_64`
- `arm64-v8a`


## Code formatting

The project uses [spotless](https://github.com/diffplug/spotless).

To format the code run:
```bash
./gradlew spotlessApply
```
and then commit the changes.


## Build

### Secrets

The app requires an API key for the faucet service. To provide the API key you need to
add a file `app/src/main/cpp/secrets.cpp` with the following content:

```cpp
#include <jni.h>
#include <string>
extern "C"
JNIEXPORT jstring JNICALL
Java_Keys_btcFaucetApiKey(JNIEnv *env, jobject thiz) {
    std::string api_key = "<put_here_your_secret_api_key>";
    return env->NewStringUTF(api_key.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_Keys_rgbFaucetApiKey(JNIEnv *env, jobject thiz) {
    std::string api_key = "<put_here_your_secret_api_key>";
    return env->NewStringUTF(api_key.c_str());
}
```

Building in release mode also requires the passwords for the keystore to be
set. This needs to be done in the `keystore.properties` file, in the project's
root directory, where the password are set like:
```
storePassword=<pass>
keyPassword=<pass>
```

### APKs

Open the project in Android Studio to manually build APKs or use the
`build_apk.sh` script for automated builds.

With no parameters, the script builds the `Debug` versions of the APKs. To do
so run:
```bash
./build_apk.sh
```

The `Release` versions can instead be built by passing "release"
(case-insensitive) as the first parameter and the path to the keystore for
signing as the second parameter. As an example:
```bash
./build_apk.sh release ~/android-keystores/iriswallet.jks
```

Upon invocation, code will be linted and checked with spotless to make sure
there are no outstanding issues and it's well formatted. The build process gets
aborted if anything is out of order.

### Bundles

As a first step, lint and format the code:
```bash
./gradlew lint
./gradlew spotlessCheck
```
and proceed to fix anything that needs attention.

Once the code is ok, to build the release bundles for upload to the Play Store
run:
```bash
./gradlew bundleRelease
```
This will produce unsigned bundles in the `app/build/outputs/bundle/`
directory, one per variant.

To sign the bundles, run:
```bash
jarsigner -keystore <key_store> app/build/outputs/bundle/<variant>/<bundle_name>.aab <key_alias>
```
As an example, to sign the testnet variant using the key store located in
`~/android-keystores/iriswallet.jks` and key alias `upload`, run:
```bash
jarsigner -keystore ~/android-keystores/iriswallet.jks app/build/outputs/bundle/bitcoinTestnetRelease/app-bitcoinTestnet-release.aab upload
```

### Page alignment

Google Play requires 16KB page alignment for apps targeting Android 15+.

The android app and all native libraries need to be built with 16KB page
alignment.

To check for alignment issues, first build an APK (`Build > Generate App
Bundles or APKs > Build APK`), then click `analyze` on the popup.
Alternatively, use `Build > Analyze APK ...` and select the desired APK file.

The `Alignment` column should report no issues for an APK that correctly
supports 16KB pages.

To test that the app runs fine on a 16KB-page device, an AVD running the
`Pre-release 16KB Page Size` system image variant can be used.

[rgb-lib]: https://github.com/RGB-Tools/rgb-lib
[rgb-lib-kotlin]: https://github.com/RGB-Tools/rgb-lib-kotlin
