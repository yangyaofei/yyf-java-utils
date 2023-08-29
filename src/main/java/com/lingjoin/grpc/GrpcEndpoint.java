package com.lingjoin.grpc;


/**
 * gRPC 服务地址, 当有单个服务的时候继承此接口
 */
public interface GrpcEndpoint {

    /**
     * Server gRPC host.
     *
     * @return the host
     */
    String getHost();

    /**
     * Server gRPC port. 1024~65535
     *
     * @return the port
     */
    Integer getPort();
}
