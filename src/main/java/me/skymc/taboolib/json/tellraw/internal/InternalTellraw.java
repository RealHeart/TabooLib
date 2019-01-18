package me.skymc.taboolib.json.tellraw.internal;

import me.skymc.taboolib.TabooLib;
import me.skymc.taboolib.common.packet.TPacketHandler;
import me.skymc.taboolib.common.util.SimpleReflection;
import me.skymc.taboolib.inventory.ItemUtils;
import me.skymc.taboolib.json.tellraw.TellrawVersion;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.List;
import java.util.Map;

/**
 * @Author 坏黑
 * @Since 2018-11-07 22:54
 */
public class InternalTellraw implements AbstractTellraw {

    private int bukkitVersion = TabooLib.getVersionNumber();

    public InternalTellraw() {
        SimpleReflection.saveField(NBTTagCompound.class, "map");
        SimpleReflection.saveField(NBTTagList.class, "list");
    }

    @Override
    public void sendRawMessage(Player player, String rawMessage) {
        TPacketHandler.sendPacket(player, new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a(rawMessage)));
    }

    @Override
    public String getItemComponent(ItemStack itemStack) {
        return getItemComponent(itemStack, TellrawVersion.CURRENT_VERSION);
    }

    @Override
    public String getItemComponent(ItemStack itemStack, TellrawVersion version) {
        return nbtToString(CraftItemStack.asNMSCopy(itemStack).save(new NBTTagCompound()), version);
    }

    @Override
    public ItemStack optimizeNBT(ItemStack itemStack, List<String> nbtWhitelist) {
        Object nmsItem = CraftItemStack.asNMSCopy(itemStack);
        if (((net.minecraft.server.v1_8_R3.ItemStack) nmsItem).hasTag()) {
            Object nbtTag = new NBTTagCompound();
            Map<String, NBTBase> mapNew =  (Map) SimpleReflection.getFieldValue(NBTTagCompound.class, nbtTag, "map");
            Map<String, NBTBase> mapOrigin = (Map) SimpleReflection.getFieldValue(NBTTagCompound.class, ((net.minecraft.server.v1_8_R3.ItemStack) nmsItem).getTag(), "map");
            for (Map.Entry<String, NBTBase> entry : mapOrigin.entrySet()) {
                if (nbtWhitelist.contains(entry.getKey())) {
                    mapNew.put(entry.getKey(), entry.getValue());
                }
            }
            ((net.minecraft.server.v1_8_R3.ItemStack) nmsItem).setTag((NBTTagCompound) nbtTag);
           return  CraftItemStack.asBukkitCopy(((net.minecraft.server.v1_8_R3.ItemStack) nmsItem));
        }
        return itemStack;
    }

    @Override
    public ItemStack optimizeShulkerBox(ItemStack item) {
        if (item.getType().name().endsWith("SHULKER_BOX")) {
            ItemStack itemClone = item.clone();
            BlockStateMeta blockStateMeta = (BlockStateMeta) itemClone.getItemMeta();
            ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
            ItemStack[] contents = shulkerBox.getInventory().getContents();
            ItemStack[] contentsClone = new ItemStack[contents.length];
            for (int i = 0; i < contents.length; i++) {
                ItemStack content = contents[i];
                if (!ItemUtils.isNull(content)) {
                    ItemStack contentClone = new ItemStack(Material.STONE, content.getAmount(), content.getDurability());
                    if (content.getItemMeta().hasDisplayName()) {
                        ItemUtils.setName(contentClone, content.getItemMeta().getDisplayName());
                    }
                    contentsClone[i] = contentClone;
                }
            }
            shulkerBox.getInventory().setContents(contentsClone);
            blockStateMeta.setBlockState(shulkerBox);
            itemClone.setItemMeta(blockStateMeta);
            return itemClone;
        } else if (item.getItemMeta() instanceof BlockStateMeta && ((BlockStateMeta) item.getItemMeta()).getBlockState() instanceof InventoryHolder) {
            ItemStack itemClone = item.clone();
            BlockStateMeta blockStateMeta = (BlockStateMeta) itemClone.getItemMeta();
            InventoryHolder inventoryHolder = (InventoryHolder) blockStateMeta.getBlockState();
            inventoryHolder.getInventory().clear();
            blockStateMeta.setBlockState((org.bukkit.block.BlockState) inventoryHolder);
            itemClone.setItemMeta(blockStateMeta);
            return itemClone;
        }
        return item;
    }

    private String nbtToString(Object nbtTagCompound, TellrawVersion version) {
        StringBuilder builder = new StringBuilder("{");
        Map map = (Map) SimpleReflection.getFieldValue(NBTTagCompound.class, nbtTagCompound, "map");
        int index = 0;
        for (Object nbtBaseEntry : map.entrySet()) {
            if (index++ != 0) {
                builder.append(",");
            }
            Object value = ((Map.Entry) nbtBaseEntry).getValue();
            if (value instanceof NBTTagList ) {
                builder.append(((Map.Entry) nbtBaseEntry).getKey()).append(":").append(nbtListToString(value, version));
            } else if (value instanceof NBTTagCompound) {
                builder.append(((Map.Entry) nbtBaseEntry).getKey()).append(":").append(nbtToString(value, version));
            } else {
                builder.append(((Map.Entry) nbtBaseEntry).getKey()).append(":").append(value);
            }
        }
        return builder.append('}').toString();
    }

    private String nbtListToString(Object nbtTagList, TellrawVersion version) {
        StringBuilder builder = new StringBuilder("[");
        List list = (List) SimpleReflection.getFieldValue(NBTTagList.class, nbtTagList, "list");
        for (int i = 0; i < list.size(); ++i) {
            if (i != 0) {
                builder.append(',');
            }
            if (version == TellrawVersion.HIGH_VERSION || (this.bukkitVersion >= 11200 && version == TellrawVersion.CURRENT_VERSION)) {
                builder.append(list.get(i));
            } else {
                builder.append(i).append(':').append(list.get(i));
            }
        }
        return builder.append(']').toString();
    }
}