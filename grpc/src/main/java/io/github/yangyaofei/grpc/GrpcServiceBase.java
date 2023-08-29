package io.github.yangyaofei.grpc;

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
    /**
     * The Grpc config.
     */
    protected final GrpcConfig grpcConfig;
    /**
     * The Credentials for the channel.
     */
    protected final ChannelCredentials credentials;

    /**
     * The Channel map to store the channel for each endpoint
     */
    protected final Map<String, ManagedChannel> channelMap;

    /**
     * @param grpcConfig the grpc config
     * @throws IOException the io exception
     */
    protected GrpcServiceBase(GrpcConfig grpcConfig) throws IOException {
        this.grpcConfig = grpcConfig;
        this.credentials = generateCredentials();
        this.channelMap = new HashMap<>();
    }

    /**
     * Generates a key for a gRPC channel based on the provided endpoint.
     *
     * @param endpoint The endpoint to generate the key for.
     * @return The generated channel key.
     */
    protected String generateChannelKey(GrpcEndpoint endpoint) {
        return endpoint.getHost() + ":" + endpoint.getPort();
    }

    /**
     * Retrieves a gRPC channel for the provided endpoint. If a channel for the endpoint already exists,
     * it checks if the channel is shutdown or terminated. If so, it shuts down the existing channel and
     * generates a new channel. If the channel is not shutdown or terminated, it returns the existing channel.
     *
     * @param endpoint The endpoint to retrieve the channel for.
     * @return The gRPC channel for the endpoint.
     */
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
