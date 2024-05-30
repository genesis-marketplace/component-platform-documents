router {
    webPort = 9064
    socketPort = 9065
    authDisabled = true

    httpServerCodec {
        maxInitialLineLength = 4096
        maxHeaderSize = 8192
        maxChunkSize = 8192
        validateHeaders = true
        initialBufferSize = 128
    }
    httpObjectAggregator {
        maxContentLength = 262144
        closeOnExpectationFailed = false
    }
}
