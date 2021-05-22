DEPENDS_append_tetra = " android-simg2img-native "

generate_sparse_image() {
    img2simg "${IMGDEPLOYDIR}/${IMAGE_NAME}${IMAGE_NAME_SUFFIX}.ext4" "${IMGDEPLOYDIR}/${IMAGE_NAME}${IMAGE_NAME_SUFFIX}.simg"
    ln -s "${IMAGE_NAME}${IMAGE_NAME_SUFFIX}.simg" "${IMGDEPLOYDIR}/${IMAGE_LINK_NAME}.simg"
}

IMAGE_POSTPROCESS_COMMAND_append_tetra = " generate_sparse_image "
