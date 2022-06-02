package org.plusmc.pluslib.bukkit.voicechat;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoderMode;
import org.bukkit.Bukkit;
import org.plusmc.pluslib.bukkit.PlusLibBukkit;
import org.plusmc.pluslib.bukkit.util.FileUtil;
import org.plusmc.pluslib.reflect.bungeespigot.config.ConfigEntry;
import org.plusmc.pluslib.reflect.bungeespigot.config.IConfig;

import java.io.File;
import java.nio.ShortBuffer;
import java.util.*;

public class PlusLibVoicechat implements VoicechatPlugin {
    private static PlusLibVoicechat instance;
    @ConfigEntry private boolean useVoiceChat;
    @ConfigEntry private boolean keepAudioLoaded;

    private VoicechatServerApi api;
    private final List<PlusSound> queue;

    private final Map<String, ShortBuffer> loadedSounds;

    public static File getSoundsFolder() {
        return new File("pluginSounds");
    }


    private PlusLibVoicechat() {
        loadedSounds = new HashMap<>();
        queue = new ArrayList<>();
    }

    public static void createInstance(IConfig config) {

        if(getSoundsFolder().mkdir())
            PlusLibBukkit.getInstance().getLogger().info("Created Sound Folder");
        else PlusLibBukkit.getInstance().getLogger().severe("Could Not Create Sound Folder");


        if(instance != null) {
            PlusLibBukkit.getInstance().getLogger().warning("Voicechat Plugin is already initialized!");
            return;
        }



        BukkitVoicechatService service = Bukkit.getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            PlusLibBukkit.getInstance().getLogger().info("Registering Voicechat Plugin...");
            instance = new PlusLibVoicechat();
            config.writeIntoObject(instance);
            if(!instance.useVoiceChat) {
                PlusLibBukkit.getInstance().getLogger().info("Voicechat Plugin is disabled in config!");
                return;
            }

            if(!getSoundsFolder().exists() && !getSoundsFolder().mkdir()) {
                PlusLibBukkit.getInstance().getLogger().warning("Could not create sounds folder!");
            }

            service.registerPlugin(instance);
        }
    }

    public static boolean isEnabled() {
        return instance != null && instance.useVoiceChat;
    }


    public void loadSound(PlusSound sound) {
        if(!useVoiceChat) {
            return;
        }

        if(api == null) {
            queue.add(sound);
            return;
        }

        File file = new File(getSoundsFolder(), sound.path());
        if(!file.exists()) {
            PlusLibBukkit.logger().warning("Sound file " + file.getName() + " does not exist!");
            return;
        }

        if(!file.getName().endsWith(".wav")) {
            PlusLibBukkit.logger().warning("Sound file " + file.getName() + " is not a wav file!");
            return;
        }

        ShortBuffer buffer = ShortBuffer.wrap(bytesToShorts(FileUtil.readData(file)));
        loadedSounds.put(sound.name(), buffer);
        PlusLibBukkit.logger().info("Loaded sound " + sound.name());
    }


    public AudioPlayer createAudioPlayer(org.bukkit.entity.Player player, PlusSound sound) {

        ServerPlayer p = api.fromServerPlayer(player);
        Position pos = p.getPosition();
        ServerLevel level = p.getServerLevel();
        LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(), level, pos);
        OpusEncoder encoder = api.createEncoder(OpusEncoderMode.AUDIO);

        return api.createAudioPlayer(channel, encoder, loadedSounds.get(sound.name()).array());
    }

    private short[] bytesToShorts(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) ((bytes[i * 2] & 0xff) | (bytes[i * 2 + 1] << 8));
        }
        return shorts;
    }


    public static PlusLibVoicechat getInstance() {
        return instance;
    }

    @Override
    public String getPluginId() {
        return "pluslib";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }

    public void onServerStarted(VoicechatServerStartedEvent event) {
        api = event.getVoicechat();
        queue.forEach(this::loadSound);
        queue.clear();
        PlusLibBukkit.logger().info("Voicechat plugin started!");
    }

    @Override
    public void initialize(VoicechatApi api) {
        PlusLibBukkit.logger().info("Initialized Voicechat Plugin!");
    }





}
