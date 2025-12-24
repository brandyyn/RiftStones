package net.blay09.mods.waystones.compat;

import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import astrotibs.villagenames.banner.BannerGenerator;
import astrotibs.villagenames.name.NameGenerator;
import astrotibs.villagenames.nbt.VNWorldDataStructure;
import astrotibs.villagenames.utility.FunctionsVN;

public class VillageNamesCompat {

    public static String ensureVillageName(World world, int ax, int ay, int az) {

        VNWorldDataStructure data = VNWorldDataStructure.forWorld(world, "villagenames3_Village", "NamedStructures");

        NBTTagCompound root = data.getData();

        // 1) Look for an existing VN village entry NEAR this point
        for (Object key : root.func_150296_c()) {
            NBTTagCompound entry = root.getTagList(key.toString(), 10)
                .getCompoundTagAt(0);

            int x = entry.getInteger("signX");
            int y = entry.getInteger("signY");
            int z = entry.getInteger("signZ");

            double dx = x - ax;
            double dz = z - az;

            // VN uses <= 10000 (100 block radius squared)
            if (dx * dx + dz * dz <= 10000) {
                return (entry.getString("namePrefix") + " "
                    + entry.getString("nameRoot")
                    + " "
                    + entry.getString("nameSuffix")).trim();
            }
        }

        // 2) Generate deterministic VN name
        Random rand = new Random();
        rand.setSeed(world.getSeed() + FunctionsVN.getUniqueLongForXYZ(ax, ay, az));

        String[] name = NameGenerator.newRandomName("Village", rand);

        Object[] banner = BannerGenerator.randomBannerArrays(rand, -1, -1);

        @SuppressWarnings("unchecked")
        java.util.ArrayList<Integer> colors = (java.util.ArrayList<Integer>) banner[1];

        int townColor = 15 - colors.get(0);
        int townColor2 = colors.size() > 1 ? 15 - colors.get(1) : townColor;

        // 3) Store VN entry
        NBTTagList list = new NBTTagList();
        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("signX", ax);
        tag.setInteger("signY", ay);
        tag.setInteger("signZ", az);
        tag.setInteger("townColor", townColor);
        tag.setInteger("townColor2", townColor2);
        tag.setString("namePrefix", name[1]);
        tag.setString("nameRoot", name[2]);
        tag.setString("nameSuffix", name[3]);
        tag.setBoolean("fromEntity", true);

        list.appendTag(tag);

        String key = (name[1] + " " + name[2] + " " + name[3]).trim() + ", x" + ax + " y" + ay + " z" + az;

        root.setTag(key, list);
        data.markDirty();

        return (name[1] + " " + name[2] + " " + name[3]).trim();
    }

}
