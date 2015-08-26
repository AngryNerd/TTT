package net.caseif.ttt.util.helper;

import net.caseif.ttt.TTTCore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Static utility class for NMS-related functionality. This class serves as an
 * abstraction layer for some under-the-hood hacks.
 */
public class NmsHelper {

    private static final boolean NMS_SUPPORT;
    public static final String VERSION_STRING;

    // general classes for sending packets
    public static final Method CRAFT_PLAYER_GET_HANDLE;
    public static final Field ENTITY_PLAYER_PLAYER_CONNECTION;
    public static final Method PLAYER_CONNECTION_A_PACKET_PLAY_IN_CLIENT_COMMAND;
    public static final Object CLIENT_COMMAND_PACKET_INSTANCE;

    static {
        boolean nmsException = false;

        String[] array = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        VERSION_STRING = array.length == 4 ? array[3] + "." : "";

        Method craftPlayer_getHandle = null;
        Field entityPlayer_playerConection = null;
        Method playerConnection_a_packetPlayInClientCommand = null;
        Object clientCommandPacketInstance = null;
        try {
            // get method for recieving CraftPlayer's EntityPlayer
            craftPlayer_getHandle = getCraftClass("entity.CraftPlayer").getMethod("getHandle");
            // get the PlayerConnection of the EntityPlayer
            entityPlayer_playerConection = getNmsClass("EntityPlayer").getDeclaredField("playerConnection");
            // method to send the packet
            playerConnection_a_packetPlayInClientCommand = getNmsClass("PlayerConnection")
                    .getMethod("a", getNmsClass("PacketPlayInClientCommand"));

            try {
                try { // 1.6 and above
                    Class<? extends Enum> enumClass;
                    Object performRespawn;
                    try {
                        // this changed at some point in 1.8 to an inner class; I don't care to figure out exactly when

                        //noinspection unchecked
                        enumClass = (Class<? extends Enum>) getNmsClass("PacketPlayInClientCommand$EnumClientCommand");
                    } catch (ClassNotFoundException ex) { // older 1.8 builds/1.7
                        //noinspection unchecked
                        enumClass = (Class<? extends Enum>) getNmsClass("EnumClientCommand");
                    }
                    performRespawn = Enum.valueOf(
                            enumClass, "PERFORM_RESPAWN"
                    );
                    clientCommandPacketInstance = getNmsClass("PacketPlayInClientCommand")
                            .getConstructor(performRespawn.getClass())
                            .newInstance(performRespawn);
                }
                catch (ClassNotFoundException ex) { // pre-1.6
                    ex.printStackTrace();
                    clientCommandPacketInstance = getNmsClass("Packet205ClientCommand").getConstructor().newInstance();
                    clientCommandPacketInstance.getClass().getDeclaredField("a").set(clientCommandPacketInstance, 1);
                }
            }
            catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                TTTCore.getInstance()
                        .logSevere(TTTCore.locale.getLocalizable("plugin.alert.nms.client-command").localize());
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            TTTCore.getInstance().logSevere(TTTCore.locale.getLocalizable("plugin.alert.nms.fail").localize());
            nmsException = true;
        }
        CRAFT_PLAYER_GET_HANDLE = craftPlayer_getHandle;
        ENTITY_PLAYER_PLAYER_CONNECTION = entityPlayer_playerConection;
        PLAYER_CONNECTION_A_PACKET_PLAY_IN_CLIENT_COMMAND = playerConnection_a_packetPlayInClientCommand;
        CLIENT_COMMAND_PACKET_INSTANCE = clientCommandPacketInstance;

        NMS_SUPPORT = !nmsException;
    }

    /**
     * Retrieves a class by the given name from the package
     * {@code net.minecraft.server}.
     *
     * @param name the class to retrieve
     * @return the class object from the package
     * {@code net.minecraft.server}
     * @throws ClassNotFoundException if the class does not exist in the
     * package
     */
    private static Class<?> getNmsClass(String name) throws ClassNotFoundException {
        String className = "net.minecraft.server." + VERSION_STRING + name;
        return Class.forName(className);
    }

    /**
     * Retrieves a class by the given name from the package
     * {@code org.bukkit.craftbukkit}.
     *
     * @param name the class to retrieve
     * @return the class object from the package
     * {@code org.bukkit.craftbukkit}
     * @throws ClassNotFoundException if the class does not exist in the
     * package
     */
    private static Class<?> getCraftClass(String name) throws ClassNotFoundException {
        String className = "org.bukkit.craftbukkit." + VERSION_STRING + name;
        return Class.forName(className);
    }

    /**
     * Sends a PlayInClientCommand packet to the given player.
     *
     * @param player the {@link Player} to send the packet to
     */
    public static void sendRespawnPacket(Player player) {
        if (NMS_SUPPORT) {
            try {
                Object nmsPlayer = CRAFT_PLAYER_GET_HANDLE.invoke(player);
                Object conn = ENTITY_PLAYER_PLAYER_CONNECTION.get(nmsPlayer);
                PLAYER_CONNECTION_A_PACKET_PLAY_IN_CLIENT_COMMAND.invoke(conn, CLIENT_COMMAND_PACKET_INSTANCE);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                TTTCore.log.severe("Failed to force-respawn player " + player.getName());
                ex.printStackTrace();
            }
        }
    }

}
