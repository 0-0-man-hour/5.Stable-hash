package com.zeromh.consistenthash.server.application;

import com.zeromh.consistenthash.domain.ServerUpdateInfo;
import com.zeromh.consistenthash.domain.ServerStatus;
import com.zeromh.consistenthash.domain.HashKey;
import com.zeromh.consistenthash.domain.HashServer;
import com.zeromh.consistenthash.hash.port.out.HashServicePort;
import com.zeromh.consistenthash.server.port.in.ServerManageUseCase;
import com.zeromh.consistenthash.server.port.out.ServerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ServerManageService implements ServerManageUseCase {

    private final ServerPort serverPort;
    private final HashServicePort hashServicePort;

    @Override
    public ServerStatus addServer() {
        ServerStatus serverStatus = serverPort.getServerStatus();

        ServerUpdateInfo updateInfo = hashServicePort.addServerInfo(serverStatus);
        serverStatus = serverPort.addServer(updateInfo.getNewServer());

        if(updateInfo.getRehashServer() != null) {
            rehashServerAll(updateInfo.getRehashServer());
        }

        return serverStatus;
    }

    @Override
    public ServerStatus deleteServer(HashServer hashServer) {
        ServerStatus serverStatus = serverPort.getServerStatus();
        ServerUpdateInfo updateInfo = hashServicePort.deleteServerInfo(serverStatus, hashServer);

        rehashServerAll(updateInfo.getRehashServer());
        serverStatus = serverPort.deleteServer(hashServer);


        return serverStatus;
    }

    private void rehashServer(List<HashServer> rehashServers) {
        for(var fromServer : rehashServers) {
            Map<HashServer, List<HashKey>> serverMap = serverPort.getAllServerData(fromServer)
                    .stream()
                    .collect(Collectors.groupingBy(hashServicePort::getServer));

            for (var toServer : serverMap.keySet()) {
                if (toServer.getName().equals(fromServer.getName())) {
                    continue;
                }

                var targetKeyList = serverMap.get(toServer);
                serverPort.addDataList(toServer, targetKeyList);
                serverPort.delDataList(fromServer, targetKeyList);
            }
        }
    }

    private void rehashServerAll(List<HashServer> rehashServers) {
        List<HashKey> keys = new ArrayList<>();
        for (var fromSever : rehashServers) {
            var rehashKeys = serverPort.getAllServerData(fromSever);
            serverPort.delDataList(fromSever, rehashKeys);;
            keys.addAll(rehashKeys);
        }

        var serverMap = keys.stream().collect(Collectors.groupingBy(hashServicePort::getServer));

        for (var toServer : serverMap.keySet()) {
            var targetKeyList = serverMap.get(toServer);
            serverPort.addDataList(toServer, targetKeyList);

        }

    }
//    private void rehashKeys(List<StableHashServer> rehashServers) {
//        for(var fromServer : rehashServers) {
//            serverPort.getAllServerData(fromServer).stream()
//                    .filter(key -> hashServicePort.getServer(key).)
//        }
//    }


}