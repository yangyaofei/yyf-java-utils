package com.lingjoin.grpc;

/**
 * 双向 tls gRPC 通讯所需配置
 */
public interface GrpcConfig {
    /**
     * 超时时间, 不能小于5秒
     *
     * @return the timeout
     */
    Integer getTimeout();

    /**
     * 连接的 Server 的 rootCa
     *
     * @return the root ca path
     */
    String getRootCaPath();

    /**
     * 根据 Client Root Ca 签名的 Ca
     *
     * @return the cert chain path
     */
    String getCertChainPath();

    /**
     * Client Ca 的私钥
     *
     * @return the private key path
     */
    String getPrivateKeyPath();

    /**
     * Enable the tls or not
     *
     * @return the boolean
     */
    Boolean getEnableTls();
}
