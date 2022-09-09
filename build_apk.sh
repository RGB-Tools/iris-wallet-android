#!/bin/bash -e
#
# build debug or (signed) release apk and open it in default file manager

VARIANTS=("bitcoinSignet" "bitcoinTestnet")
OUT_BASE="app/build/outputs/apk"
RELEASE_PATH="app/release"

APK_NAME=""
TARGET="Debug"

_die() {
    echo "$@"
    exit 1
}

_log() {
    echo "-" "$@"
}

_tit() {
    echo
    echo "--------------------------------"
    echo "$@"
    echo "--------------------------------"
}

_checks() {
    ./gradlew lint
    ./gradlew spotlessCheck
}

_confirm() {
    echo
    echo "Ready to build $TARGET APKs"
    read -r -p "Proceed? (y/n) " ANS
    case $ANS in
        [Yy]) echo;;
        *) _die "aborting";;
    esac
}

_set_apk_name() {
    APK_NAME="app-$VARIANT-${TARGET,,}"
}

_prepare() {
    local variant="$1"
    local debug_out="$OUT_BASE/$variant/debug"
    local release_out="$OUT_BASE/$variant/release"
    local creating="creating $TARGET output path..."
    local removing="removing existing APK(s) $APK_NAME..."
    if [ "$TARGET" = "Debug" ]; then
        _log "$creating"
        mkdir -vp "$debug_out"
        _log "$removing"
        rm -vf "$debug_out/$APK_NAME.apk"
    else
        _log "$creating"
        mkdir -vp "$release_out"
        mkdir -vp "$RELEASE_PATH"
        _log "$removing"
        rm -vf "$release_out/$APK_NAME-unsigned.apk"
        rm -vf "$release_out/$APK_NAME-aligned.apk"
        rm -vf "$RELEASE_PATH/$APK_NAME.apk"
    fi
}

_build() {
    ./gradlew "assemble$TARGET"
}

_align_and_sign() {
    local variant="$1"
    local release_out="$OUT_BASE/$variant/release"
    local apk="$release_out/$APK_NAME"
    _log "aligning $APK_NAME APK..."
    zipalign -v -p 4 "$apk-unsigned.apk" "$apk-aligned.apk"

    _log "signing $APK_NAME APK..."
    apksigner sign --ks "$KEYSTORE" \
        --out "$RELEASE_PATH/$APK_NAME.apk" "$apk-aligned.apk"
}

_output_info() {
    if [ "$TARGET" = "Debug" ]; then
        for variant in "${VARIANTS[@]}"; do
            realpath "$OUT_BASE/$variant/debug/"
        done
    else
        realpath "$RELEASE_PATH/"
    fi
}

# target selection
[ "${1,,}" = release ] && TARGET="Release"
KEYSTORE="$2"

# preliminary checks
_tit "running preliminary checks..."
[ "$TARGET" != "Debug" ] && [ "$TARGET" != "Release" ] && \
    _die "unrecognized target specified"
[ "$TARGET" = "Release" ] && [ -z "$KEYSTORE" ] && \
    _die "Please specify a keystore for signing"
_checks
_confirm

# APK build
_tit "preparing directories..."
for VARIANT in "${VARIANTS[@]}"; do
    _set_apk_name
    _prepare "$VARIANT"
done
_tit "building $TARGET APKs..."
_build
if [ "$TARGET" = "Release" ]; then
    _tit "signing $TARGET APKs..."
    for VARIANT in "${VARIANTS[@]}"; do
        _set_apk_name
        _align_and_sign "$VARIANT"
    done
fi

# output path info
_tit "Built APKs have been output to:"
_output_info
