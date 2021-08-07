package me.faintcloudy.balloons;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.network.protocol.game.PacketPlayOutAttachEntity;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import java.util.*;


public class Balloons extends JavaPlugin implements Listener {

    private static Balloons instance = null;

    public static void setInstance(Balloons instance) {
        Balloons.instance = instance;
    }

    public static Balloons getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        setInstance(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onToggleSwimming(EntityToggleSwimEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event)
    {
        if (event.getRightClicked() instanceof Player &&
                (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.LEAD ||
                        event.getPlayer().getInventory().getItemInOffHand().getType() == Material.LEAD))
        {
            leash((Player) event.getRightClicked(), event.getPlayer());
        }
    }


    /*@EventHandler
    public void onUnleash2(PlayerUnleashEntityEvent event)
    {
        if (event.getEntityType() == EntityType.COW)
        {
            if (event.getEntity().getCustomName() == null)
                return;
            if (!event.getEntity().getCustomName().startsWith("*"))
                return;
            Cow cow = (Cow) event.getEntity();
            cow.setLeashHolder(getConnectionByCow(cow).leasher);
        }
    }*/

    @EventHandler
    public void onLeash(PlayerLeashEntityEvent event)
    {
        if (event.getEntity().getType() == EntityType.COW && event.getEntity().getCustomName() != null)
        {
            if (event.getEntity().getCustomName().startsWith("*"))
                event.setCancelled(true);
        }
    }

    /*@EventHandler
    public void onUnleash(EntityUnleashEvent event)
    {
        if (event.getEntityType() == EntityType.COW)
        {
            if (event.getEntity().getCustomName() == null)
                return;
            if (!event.getEntity().getCustomName().startsWith("*"))
                return;
            Cow cow = (Cow) event.getEntity();
            if (getConnectionByCow(cow) == null)
            {
                return;
            }

            cow.setLeashHolder(getConnectionByCow(cow).leasher);
        }
    }*/

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event)
    {
        if (event.isCancelled())
            return;
        if (getConnectionByLeasher(event.getPlayer()) != null)
        {
            if (event.getPlayer().getInventory().getItem(event.getNewSlot()) == null ||
                    Objects.requireNonNull(event.getPlayer().getInventory().getItem(event.getNewSlot())).getType() != Material.LEAD)
            {
                getConnectionByLeasher(event.getPlayer()).floatBeLeasher();
            }
        }
    }

    public Set<Connection> connections = new HashSet<>();

    public Connection getConnectionByCow(Cow cow)
    {
        for (Connection connection : connections) {
            if (connection.cow == cow)
                return connection;
        }

        return null;
    }

    public Connection getConnectionByBeLeasher(Player beLeasher)
    {
        for (Connection connection : connections) {
            if (connection.beLeasher == beLeasher)
                return connection;
        }

        return null;
    }

    public Connection getConnectionByLeasher(Player leasher)
    {
        for (Connection connection : connections) {
            if (connection.leasher == leasher)
                return connection;
        }

        return null;
    }

    public Connection createConnection(Player beLeasher, Player leasher)
    {

        Connection connection = new Connection(beLeasher, leasher);
        connections.add(connection);
        return connection;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event)
    {
        if (event.isCancelled())
            return;
        if (event.isSneaking())
        {
            if (getConnectionByLeasher(event.getPlayer()) != null) {
                event.getPlayer().removePotionEffect(PotionEffectType.LEVITATION);
                getConnectionByLeasher(event.getPlayer()).disconnect();
            }
        }
    }

    public void leash(Player beLeasher, Player leasher)
    {
        for (Connection connection : connections) {
            leasher.sendMessage("connection: " + connection);
        }
        boolean beLeasherConnected = isLeashedByBeLeasher(beLeasher);
        boolean leasherConnected = isLeashedByLeasher(leasher);
        if (leasherConnected) //如果leasher有连接
        {
            Connection LConnection = this.getConnectionByLeasher(leasher); //创建变量
            if (!beLeasherConnected) //并且被牵人没有连接
            {
                Connection connection = createConnection(beLeasher, LConnection.beLeasher);
                connection.start();
            }
            return;
        }
        Connection connection = createConnection(beLeasher, leasher);
        connection.start();

    }

    public boolean hasConnection(Player player)
    {
        return isLeashedByBeLeasher(player) || isLeashedByLeasher(player);
    }

    public List<Connection> getConnections(Player player)
    {
        List<Connection> cons = new ArrayList<>();
        for (Connection connection : connections) {
            if (connection.beLeasher == player || connection.leasher == player)
                cons.add(connection);
        }

        return cons;
    }

    public boolean isLeashedByBeLeasher(Player beLeasher)
    {
        return getConnectionByBeLeasher(beLeasher) != null;
    }

    public boolean isLeashedByLeasher(Player leasher)
    {
        return getConnectionByLeasher(leasher) != null;
    }

    public boolean isLeashedByBeCow(Cow cow)
    {
        return getConnectionByCow(cow) != null;
    }
    
    @EventHandler
    public void onLeadSpawn(ItemSpawnEvent event)
    {
        if (event.getEntity().getItemStack().getType() == Material.LEAD)
            event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event)
    {
        if (getConnectionByBeLeasher(event.getEntity()) != null)
            getConnectionByBeLeasher(event.getEntity()).disconnect();
    }

    @EventHandler
    public void onFloat(PlayerMoveEvent event)
    {
        if (event.getTo() == null)
            return;
        if (event.getTo().getY() > 256 && getConnectionByBeLeasher(event.getPlayer()) != null)
        {
            getConnectionByBeLeasher(event.getPlayer()).disconnect();
            event.getPlayer().damage(event.getPlayer().getHealth() + 1);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event)
    {
        if (event.getEntity().getCustomName() == null)
            return;
        if (event.getEntityType() != EntityType.COW)
            return;
        if (event.getCause() == EntityDamageEvent.DamageCause.CUSTOM || event.getCause() == EntityDamageEvent.DamageCause.MAGIC)
            return;
        if (getConnectionByCow((Cow) event.getEntity()) != null)
            event.setCancelled(true);
    }

    ItemStack lead = new ItemStack(Material.LEAD);
    {
        ItemMeta meta = lead.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.AQUA + "栓绳");
        lead.setItemMeta(meta);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        event.getPlayer().getInventory().addItem(lead);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event)
    {
        if (event.getItemDrop().getItemStack().getType() == Material.LEAD)
            event.setCancelled(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event)
    {
        event.getPlayer().getInventory().addItem(lead);
    }


    public static EntityPlayer getHandle(Player player)
    {
        return ((CraftPlayer) player).getHandle();
    }

    public void forceSwimming(Player player, boolean swimming)
    {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        craftPlayer.getHandle().setFlag(4, swimming);
    }
}
