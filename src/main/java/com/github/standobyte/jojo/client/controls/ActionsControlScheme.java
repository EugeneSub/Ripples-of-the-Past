package com.github.standobyte.jojo.client.controls;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.lwjgl.glfw.GLFW;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.client.ui.actionshud.QuickAccess.QuickAccessKeyConflictContext;
import com.github.standobyte.jojo.power.IPower;
import com.github.standobyte.jojo.power.IPower.PowerClassification;
import com.github.standobyte.jojo.power.IPowerType;
import com.github.standobyte.jojo.power.layout.ActionsLayout;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.ResourceLocation;

public class ActionsControlScheme {
//    public final ResourceLocation powerTypeId;
//    private OptionalInt savedSchemeNumber = OptionalInt.empty();
    private List<KeybindEntry> customKeybindEntries = new ArrayList<>();
    private final ActionsLayout<?> hotbarsLayout;
    
    public ActionsControlScheme(ActionsLayout<?> hotbarsLayout, ResourceLocation powerTypeId) {
        this.hotbarsLayout = hotbarsLayout;
//        this.powerTypeId = powerTypeId;
    }
    
    private static ActionsControlScheme createDefault(IPowerType<?, ?> powerType) {
        ActionsControlScheme controlScheme = new ActionsControlScheme(
                powerType.createDefaultLayout(), powerType.getRegistryName());
        
        Action<?> mmbAction = powerType.createDefaultLayout().mmbActionStarting;
        if (mmbAction != null) {
            controlScheme.addKeyBindingEntry(PressActionType.CLICK, mmbAction, 
                    InputMappings.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
        }
        
        return controlScheme;
    }
    
    public void clearInvalidKeybinds() {
        Iterator<KeybindEntry> iter = customKeybindEntries.iterator();
        while (iter.hasNext()) {
            KeybindEntry entry = iter.next();
            if (entry.action == null || entry.keybind.isUnbound() || entry.type == null) {
                iter.remove();
            }
        }
    }

    public KeybindEntry addKeyBindingEntry(ActionsControlScheme.PressActionType type, Action<?> action, int key) {
        return addKeyBindingEntry(type, action, InputMappings.Type.KEYSYM, key);
    }

    public KeybindEntry addKeyBindingEntry(ActionsControlScheme.PressActionType type, Action<?> action, InputMappings.Type inputType, int key) {
        KeyBinding keyBinding = createNewKeyBinding(inputType, key);
        KeybindEntry entry = new KeybindEntry(type, action, keyBinding);
        customKeybindEntries.add(entry);
        return entry;
    }

    private static final AtomicInteger KEY_ID = new AtomicInteger();
    static KeyBinding createNewKeyBinding(InputMappings.Type inputType, int key) {
        KeyBinding keyBinding = new KeyBinding(
                JojoMod.MOD_ID + ".key.action." + String.valueOf(KEY_ID.getAndIncrement()), 
                QuickAccessKeyConflictContext.INSTANCE, 
                inputType, key, 
                "key.categories." + JojoMod.MOD_ID + ".custom_keybinds");
        return keyBinding;
    }

    static KeyBinding createBlankKeyBinding() {
        return createNewKeyBinding(InputMappings.Type.KEYSYM, -1);
    }
    
    public Iterable<KeybindEntry> getEntriesView() {
        return customKeybindEntries;
    }
    
    public static class KeybindEntry {
        public PressActionType type;
        public Action<?> action;
        public KeyBinding keybind;
        public transient int delay;
        
        public KeybindEntry(PressActionType type, Action<?> action, KeyBinding keybind) {
            this.type = type;
            this.action = action;
            this.keybind = keybind;
        }
    }
    
    public enum PressActionType {
        SELECT,
        CLICK
    }
    
    public ActionsLayout<?> getHotbarsLayout() {
        return hotbarsLayout;
    }
    
    public void reset() {
        hotbarsLayout.resetLayout();
        
        customKeybindEntries.clear();
        Action<?> mmbAction = hotbarsLayout.mmbActionStarting;
        if (mmbAction != null) {
            addKeyBindingEntry(PressActionType.CLICK, mmbAction, 
                    InputMappings.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
        }
    }
    
    
    
    public JsonObject toJson(Gson gson) {
        JsonObject json = new JsonObject();
        json.add("customKeybindEntries", gson.toJsonTree(customKeybindEntries));
        json.add("hotbarsLayout", hotbarsLayout.stateToJson());
        return json;
    }
    
    public static ActionsControlScheme fromJson(IPowerType<?, ?> powerType, JsonObject jsonObject, Gson gson) {
        ActionsControlScheme controlScheme = new ActionsControlScheme(powerType.createDefaultLayout(), powerType.getRegistryName());
        controlScheme.customKeybindEntries = new ArrayList<>(gson.fromJson(
                jsonObject.get("customKeybindEntries"), new TypeToken<List<KeybindEntry>>() {}.getType()));
        controlScheme.hotbarsLayout.stateFromJson(
                jsonObject.get("hotbarsLayout").getAsJsonObject());
        return controlScheme;
    }
    
    
    
    
    
    private static Map<PowerClassification, PowerTypeControlsEntry> controlSchemeCache = new EnumMap<>(PowerClassification.class);
    
    public static PowerTypeControlsEntry getCtrlSchemes(PowerClassification power) {
        return controlSchemeCache.get(power);
    }
    
    public static ActionsControlScheme getCurrentCtrlScheme(PowerClassification power) {
        PowerTypeControlsEntry allSchemes = getCtrlSchemes(power);
        return allSchemes != null ? allSchemes.getCurrentCtrlScheme() : null;
    }
    
    public static <P extends IPower<P, ?>> ActionsLayout<P> getHotbarsLayout(P power) {
        ActionsControlScheme controlScheme = getCurrentCtrlScheme(power.getPowerClassification());
        return controlScheme != null ? (ActionsLayout<P>) controlScheme.getHotbarsLayout() : null;
    }
    
    public static void cacheCtrlScheme(PowerClassification power, IPowerType<?, ?> powerType) {
        if (powerType != null) {
            controlSchemeCache.put(power, getCtrlSchemesFor(powerType));
        }
        else {
            controlSchemeCache.remove(power);
        }
    }
    
    
    
    private static Map<ResourceLocation, PowerTypeControlsEntry> savedControlSchemes = new HashMap<>();
    
    public static PowerTypeControlsEntry getCtrlSchemesFor(IPowerType<?, ?> type) {
        ResourceLocation id = type.getRegistryName();
        PowerTypeControlsEntry ctrlSchemesHolder = savedControlSchemes.get(id);
        if (ctrlSchemesHolder == null) {
            ctrlSchemesHolder = new PowerTypeControlsEntry(ActionsControlScheme.createDefault(type));
            savedControlSchemes.put(id, ctrlSchemesHolder);
        }
        return ctrlSchemesHolder;
    }
    
    private static ControlSettingsJson jsonSaveLoad;
    public static void initSaveFile(File file) {
        jsonSaveLoad = new ControlSettingsJson(file);
        Map<ResourceLocation, PowerTypeControlsEntry> loaded = jsonSaveLoad.load();
        if (loaded != null) {
            savedControlSchemes = loaded;
        }
    }
    
    public static void save() {
        jsonSaveLoad.save(savedControlSchemes);
    }
    
}
