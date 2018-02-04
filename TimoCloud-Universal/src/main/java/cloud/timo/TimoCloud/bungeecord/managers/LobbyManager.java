package cloud.timo.TimoCloud.bungeecord.managers;

import cloud.timo.TimoCloud.api.TimoCloudAPI;
import cloud.timo.TimoCloud.api.objects.ServerGroupObject;
import cloud.timo.TimoCloud.api.objects.ServerObject;
import cloud.timo.TimoCloud.bungeecord.TimoCloudBungee;
import cloud.timo.TimoCloud.bungeecord.objects.LobbyChooseStrategy;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class LobbyManager {

    private static final long INVALIDATE_CACHE_TIME = 5000;

    private Map<UUID, List<String>> lobbyHistory;
    private Map<UUID, Long> lastUpdate;

    public LobbyManager() {
        lobbyHistory = new HashMap<>();
        lastUpdate = new HashMap<>();
    }

    private List<String> getVisitedLobbies(UUID uuid) {
        lobbyHistory.putIfAbsent(uuid, new ArrayList<>());
        lastUpdate.putIfAbsent(uuid, 0L);
        if (new Date().getTime()-lastUpdate.get(uuid) >= INVALIDATE_CACHE_TIME) {
            lobbyHistory.put(uuid, new ArrayList<>());
        }
        lastUpdate.put(uuid, new Date().getTime());
        return lobbyHistory.get(uuid);
    }

    public void addToHistory(UUID uuid, String server) {
        lobbyHistory.putIfAbsent(uuid, new ArrayList<>());
        lobbyHistory.get(uuid).add(server);
    }

    private LobbyChooseStrategy getLobbyChooseStrategy() {
        LobbyChooseStrategy lobbyChooseStrategy = LobbyChooseStrategy.valueOf(TimoCloudBungee.getInstance().getFileManager().getConfig().getString("LobbyChooseStrategy"));
        return lobbyChooseStrategy == null ? LobbyChooseStrategy.RANDOM : lobbyChooseStrategy;
    }

    private ServerInfo searchFreeLobby(UUID uuid, ServerInfo notThis) {
        ServerGroupObject group = TimoCloudAPI.getUniversalInstance().getServerGroup(TimoCloudBungee.getInstance().getFileManager().getConfig().getString("fallbackGroup"));
        List<ServerObject> servers = new ArrayList<>(notThis == null ? group.getServers() : group.getServers().stream().filter(server -> !server.getName().equals(notThis.getName())).collect(Collectors.toList()));
        List<ServerObject> removeServers = new ArrayList<>();
        ServerObject notThisServer = notThis == null ? null : TimoCloudAPI.getUniversalInstance().getServer(notThis.getName());
        if (notThisServer != null) removeServers.add(notThisServer);
        List<String> history = getVisitedLobbies(uuid);

        for (ServerObject server : servers) {
            if (history.contains(server.getName()) && ! removeServers.contains(server)) removeServers.add(server);
        }
        servers.removeAll(removeServers);

        if (servers.size() == 0) {
            return null;
        }

        servers.sort(Comparator.comparingInt(ServerObject::getOnlinePlayerCount));
        ServerObject target = null;
        switch (getLobbyChooseStrategy()) {
            case RANDOM:
                target = servers.get(new Random().nextInt(servers.size()));
            case FILL:
                for (int i = servers.size()-1; i>= 0; i--) {
                    ServerObject server = servers.get(i);
                    if (server.getOnlinePlayerCount() < server.getMaxPlayerCount()) {
                        target = server;
                        break;
                    }
                }
                break;
            case BALANCE:
                target = servers.get(0);
        }
        return TimoCloudBungee.getInstance().getProxy().getServers().get(target.getName());
    }

    public ServerInfo getFreeLobby(UUID uuid, boolean kicked) {
        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(uuid);
        ServerInfo notThis = null;
        if (proxiedPlayer != null && proxiedPlayer.getServer() != null) notThis = proxiedPlayer.getServer().getInfo();

        ServerInfo serverInfo = searchFreeLobby(uuid, notThis);
        if (serverInfo == null) {
            return TimoCloudBungee.getInstance().getProxy().getServerInfo(TimoCloudBungee.getInstance().getFileManager().getConfig().getString("emergencyFallback"));
        }

        if (kicked) addToHistory(uuid, serverInfo.getName());

        return serverInfo;
    }

    public ServerInfo getFreeLobby(UUID uuid) {
        return getFreeLobby(uuid, false);
    }

}
