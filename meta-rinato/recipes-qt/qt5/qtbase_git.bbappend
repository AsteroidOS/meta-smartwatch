# Hardware accelerated rendering on mesa
PACKAGECONFIG:append:rinato = " gl gles2 eglfs gbm kms "
PACKAGECONFIG_GL:append:rinato = " eglfs gbm kms "
