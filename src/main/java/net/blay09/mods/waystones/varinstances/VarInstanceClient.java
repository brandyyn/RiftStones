package net.blay09.mods.waystones.varinstances;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;

public class VarInstanceClient {

    public KeyBinding closeGuiKey;

    public void initHook() {
        closeGuiKey = new KeyBinding("key.waystones.closegui", Keyboard.KEY_NONE, "key.categories.waystones");

        ClientRegistry.registerKeyBinding(closeGuiKey);
    }
}
