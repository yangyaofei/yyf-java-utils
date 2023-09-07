package com.lingjoin.grpc;

import io.grpc.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * gRPC 调用服务抽象类, 提供基础的公共内容封装
 */
@SuppressWarnings("unused")
abstract public class GrpcServiceBase {
    protected final GrpcConfig grpcConfig;
    protected final ChannelCredentials credentials;

    protected final Map<String, ManagedChannel> channelMap;

    @SuppressWarnings("MissingJavadoc")
    public GrpcServiceBase(GrpcConfig grpcConfig) throws IOException {
        this.grpcConfig = grpcConfig;
        this.credentials = generateCredentials();
        this.channelMap = new HashMap<>();
    }

    protected String generateChannelKey(GrpcEndpoint endpoint) {
        return endpoint.getHost() + ":" + endpoint.getPort();
    }

    protected ManagedChannel getChannel(GrpcEndpoint endpoint) {
        String channelKey = this.generateChannelKey(endpoint);
        if (!channelMap.containsKey(channelKey)) {
            channelMap.put(channelKey, this.generateChannel(endpoint));
            return channelMap.get(channelKey);
        } else {
            var channel = channelMap.get(channelKey);
            if (channel.isShutdown() | channel.isTerminated()) {
                channel.shutdown();
                channelMap.put(channelKey, this.generateChannel(endpoint));
                return channelMap.get(channelKey);
            } else {
                return channel;
            }
        }
    }


    /**
     * 获取 grpc 配置
     *
     * @return the grpc config
     */
    protected GrpcConfig getGrpcConfig() {
        return this.grpcConfig;
    }

    /**
     * Gets tls credentials. 一般使用
     *
     * @return the tls credentials
     */
    protected ChannelCredentials getTlsCredentials() {
        return this.credentials;
    }

    /**
     * Generate credentials channel credentials.
     *
     * @return the channel credentials
     * @throws IOException the io exception
     */
    private ChannelCredentials generateCredentials() throws IOException {
        if (this.grpcConfig.getEnableTls()) {
            return TlsChannelCredentials.newBuilder()
                    .keyManager(new File(getGrpcConfig().getCertChainPath()), new File(getGrpcConfig().getPrivateKeyPath()))
                    .trustManager(new File(getGrpcConfig().getRootCaPath()))
                    .build();
        } else {
            return InsecureChannelCredentials.create();
        }
    }

    /**
     * Generate channel managed channel.
     *
     * @param endpoint the endpoint
     * @return the managed channel
     */
    private ManagedChannel generateChannel(GrpcEndpoint endpoint) {
        return Grpc.newChannelBuilderForAddress(endpoint.getHost(), endpoint.getPort(), getTlsCredentials())
                .maxInboundMessageSize(Integer.MAX_VALUE)
                .build();
    }
}
