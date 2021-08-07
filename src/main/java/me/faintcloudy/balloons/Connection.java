package me.faintcloudy.balloons;

import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Objects;

public class Connection {
    Cow cow;
    Player beLeasher, leasher;
    BukkitTask task;
    private static final PotionEffect floatEffect = new PotionEffect(PotionEffectType.LEVITATION, Integer.MAX_VALUE, 1);
    public Connection(Player beLeasher, Player leasher)
    {
        this.beLeasher = beLeasher;
        this.leasher = leasher;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "cow=" + cow +
                ", beLeasher=" + beLeasher +
                ", leasher=" + leasher +
                ", task=" + task +
                '}';
    }

    public Player getBeLeasher() {
        return beLeasher;
    }

    public Player getLeasher() {
        return leasher;
    }

    public Cow getCow() {
        return cow;
    }

    public BukkitTask getTask() {
        return task;
    }

    public void start()
    {
        if (cow != null)
            cow.remove();
        cow = Objects.requireNonNull(beLeasher.getLocation().getWorld()).spawn(beLeasher.getLocation(), Cow.class);
        cow.setLeashHolder(leasher);
        cow.setAI(true);
        cow.setCustomName("*" + beLeasher.getName());
        cow.setCustomNameVisible(false);
        cow.setInvisible(true);
        cow.setBaby();
        cow.setGravity(false);
        cow.setAware(false);
        cow.setAgeLock(true);
        cow.setRemoveWhenFarAway(false);
        cow.setCollidable(false);
        if (task != null)
            task.cancel();
        Balloons.getInstance().forceSwimming(beLeasher, true);
        task = new BukkitRunnable()
        {
            @Override
            public void run() {
                if (cow == null || cow.isDead() || getLeasher() == null ||
                        getBeLeasher() == null || !getLeasher().isOnline() || !getBeLeasher().isOnline())
                {
                    disconnect();
                    return;
                }

                if (!cow.isLeashed() || cow.getLeashHolder() != leasher)
                    cow.setLeashHolder(leasher);
                Vector upVelocity = getLeasher().getLocation().clone().toVector().add(new Vector(0, 5, 0))
                        .subtract(getCow().getLocation().clone().toVector()).setX(0).setZ(0);
                getCow().setVelocity(getCow().getVelocity().clone().add(upVelocity.multiply(0.1)));
                Vector vector = getCow().getLocation().clone().toVector().subtract(getBeLeasher().getLocation().clone().toVector());
                vector.setY(vector.getY() * 0.1);
                getBeLeasher().setVelocity(vector);
            }
        }.runTaskTimer(Balloons.getInstance(), 0, 1);
    }

    public void floatBeLeasher()
    {
        if (task != null)
            task.cancel();
        if (cow != null)
        {
            cow.setLeashHolder(null);
            cow.remove();
            cow = null;
        }

        beLeasher.addPotionEffect(floatEffect);
    }

    public void disconnect()
    {
        if (cow != null)
        {
            cow.remove();
            cow = null;
        }

        if (task != null)
            task.cancel();
        Balloons.getInstance().forceSwimming(beLeasher, false);
        beLeasher.removePotionEffect(PotionEffectType.LEVITATION);
        beLeasher = null;
        leasher = null;
        Balloons.getInstance().connections.remove(this);
    }

}
