package com.tacz.guns.resource;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.config.PreLoadConfig;
import com.tacz.guns.util.GetJarResources;
import com.tacz.guns.util.ModListCompat;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.InclusiveRange;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraft.server.packs.PathPackResources;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public enum GunPackLoader implements RepositorySource {
    INSTANCE;
    private static final Marker MARKER = MarkerManager.getMarker("GunPackFinder");
    public PackType packType;
    private boolean firstLoad = true;


    @Override
    public void loadPacks(Consumer<Pack> pOnLoad) {
        GunMod.LOGGER.info(MARKER, "Loading gun packs for pack type: {}", this.packType);
        Pack extensionsPack = discoverExtensions();
        if (extensionsPack != null) {
            pOnLoad.accept(extensionsPack);
        }
    }

    public Pack discoverExtensions() {
        Path resourcePacksPath = FMLPaths.GAMEDIR.get().resolve("tacz");
        File folder = resourcePacksPath.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                GunMod.LOGGER.warn(MARKER, "Failed to init tacz resource directory...", e);
                return null;
            }
        }

        // 遑ｮ菫晞・鄂ｮ譁・ｻｶ蜉霓ｽ・瑚ｿ吩ｸｪ髦ｶ谿ｵ蟆・ｯ疲・㊥逧・orge驟咲ｽｮ譁・ｻｶ蜉霓ｽ譌ｩ
        PreLoadConfig.load(resourcePacksPath);

        // 莉・惠隨ｬ荳谺｡蜉霓ｽ譌ｶ螟榊宛鮟倩ｮ､襍・ｺ仙桁
        if (firstLoad) {
            if (!PreLoadConfig.override.get()) {
                for (ResourceManager.ExtraEntry entry : ResourceManager.EXTRA_ENTRIES) {
                    GetJarResources.copyModDirectory(entry.modMainClass(), entry.srcPath(), resourcePacksPath, entry.extraDirName());
                }
            }
            firstLoad = false;
        }

        GunMod.LOGGER.info(MARKER, "Start scanning for gun packs in {}", resourcePacksPath);
        List<GunPack> gunPacks = scanExtensions(resourcePacksPath);
        GunMod.LOGGER.info(MARKER, "Found {} possible gunpack(s) and added them to resource set.", gunPacks.size());
        List<PackResources> extensionPacks = new ArrayList<>();

        for(GunPack gunPack : gunPacks) {
            PackLocationInfo extensionInfo = new PackLocationInfo("tacz_extension_" + gunPack.name, Component.literal(gunPack.name), PackSource.BUILT_IN, Optional.empty());
            PackResources packResources = Files.isDirectory(gunPack.path)
                    ? new PathPackResources(extensionInfo, gunPack.path)
                    : new FilePackResources.FileResourcesSupplier(gunPack.path).openPrimary(extensionInfo);
            extensionPacks.add(packResources);
        }

        PackLocationInfo locationInfo = new PackLocationInfo("tacz_resources", Component.literal("TACZ Resources"), PackSource.BUILT_IN, Optional.empty());
        PackMetadataSection metadata = new PackMetadataSection(
                Component.translatable("tacz.resources.modresources"),
                new InclusiveRange<>(SharedConstants.getCurrentVersion().packVersion(packType))
        );
        DelegatingPackResources delegatingPackResources = new DelegatingPackResources(locationInfo, metadata, extensionPacks) {
                public IoSupplier<InputStream> getRootResource(String... paths) {
                    if (paths.length == 1 && paths[0].equals("pack.png")) {
                        Path logoPath = getModIcon("tacz");
                        if (logoPath != null) {
                            return IoSupplier.create(logoPath);
                        }
                    }
                    return null;
                }
        };
        return Pack.readMetaAndCreate(locationInfo, delegatingPackResources.supplier(), packType, new PackSelectionConfig(true, Pack.Position.BOTTOM, false));
    }

    public static @Nullable Path getModIcon(String modId) {
        Optional<? extends ModContainer> m = ModListCompat.getModContainerById(modId);
        if (m.isPresent()) {
            IModInfo mod = m.get().getModInfo();
            IModFile file = mod.getOwningFile().getFile();
            if (file != null) {
                Path logoPath = file.findResource("icon.png");
                if (Files.exists(logoPath)) {
                    return logoPath;
                }
            }
        }

        return null;
    }

    // 譽譟･霍ｯ蠕・ｸｭ逧・onfig.json
    // 蠎碑ｯ･荳堺ｼ壼惠逕ｨ霑吩ｸｪ莠・ｼ悟・菫晉蕗
//    private static RepositoryConfig checkConfig(Path resourcePacksPath) {
//        Path configPath = resourcePacksPath.resolve("config.json");
//        if (Files.exists(configPath)) {
//            try (InputStream stream = Files.newInputStream(configPath)) {
//                return GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), RepositoryConfig.class);
//            } catch (IOException | JsonSyntaxException | JsonIOException e) {
//                GunMod.LOGGER.warn(MARKER, "Failed to read config json: {}", configPath);
//            }
//        }
//        // 荳榊ｭ伜惠謌冶・・髣ｮ鬚倅ｺ・ｼ梧眠蟒ｺ荳荳ｪ
//        RepositoryConfig config = new RepositoryConfig(true);
//        // 菴ｿ逕ｨGson蜀呎枚莉ｶ
//        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
//            GSON.toJson(config, writer);
//        } catch (IOException e) {
//            GunMod.LOGGER.warn(MARKER, "Failed to init config json: {}", configPath);
//        }
//        return config;
//    }

    private static GunPack fromDirPath(Path path) throws IOException {
        Path packInfoFilePath = path.resolve("gunpack.meta.json");
        try (InputStream stream = Files.newInputStream(packInfoFilePath)) {
            PackMeta info = CommonAssetsManager.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

            if (info == null) {
                GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
                return null;
            }

            if (info.getDependencies() !=null && !modVersionAllMatch(info)) {
                GunMod.LOGGER.warn(MARKER, "Mod version mismatch: {}", packInfoFilePath.getFileName());
                return null;
            }

            return new GunPack(path, info.getName());
        } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException exception) {
            GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
            GunMod.LOGGER.warn(exception.getMessage());
        }
        return null;
    }

    private static GunPack fromZipPath(Path path)  {
        try(ZipFile zipFile = new ZipFile(path.toFile())){
            ZipEntry extDescriptorEntry = zipFile.getEntry("gunpack.meta.json");
            if (extDescriptorEntry == null) {
                GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), "No gunpack.meta.json found");
                return null;
            }

            try (InputStream stream = zipFile.getInputStream(extDescriptorEntry)) {
                PackMeta info = CommonAssetsManager.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

                if (info == null) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", path.getFileName());
                    return null;
                }

                if (info.getDependencies() !=null && !modVersionAllMatch(info)) {
                    GunMod.LOGGER.warn(MARKER, "Mod version mismatch: {}", path.getFileName());
                    return null;
                }

                return new GunPack(path, info.getName());
            } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException e) {
                GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
                return null;
            }
        } catch (IOException e) {
            GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
            return null;
        }
    }

    private static List<GunPack> scanExtensions(Path extensionsPath) {
        List<GunPack> gunPacks = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)){
            for (Path entry : stream) {
                GunPack gunPack = null;
                if (Files.isDirectory(entry)) {
                    gunPack = fromDirPath(entry);
                } else if (entry.toString().endsWith(".zip")) {
                    gunPack = fromZipPath(entry);
                }
                if (gunPack != null) {
                    GunMod.LOGGER.info(MARKER, "- {}, Main namespace: {}", gunPack.path.getFileName(), gunPack.name);
                    gunPacks.add(gunPack);
                }
            }
        } catch (IOException e) {
            GunMod.LOGGER.error(MARKER, "Failed to scan extensions from {}. Error: {}", extensionsPath, e);
        }

        return gunPacks;
    }

    private static boolean modVersionAllMatch(PackMeta info) throws InvalidVersionSpecificationException {
        HashMap<String, String> dependencies = info.getDependencies();
        for (String modId : dependencies.keySet()) {
            if (!modVersionMatch(modId, dependencies.get(modId))) {
                return false;
            }
        }
        return true;
    }

    private static boolean modVersionMatch(String modId, String version) throws InvalidVersionSpecificationException {
        VersionRange versionRange = VersionRange.createFromVersionSpec(version);
        return ModListCompat.getModContainerById(modId).map(mod -> {
            ArtifactVersion modVersion = mod.getModInfo().getVersion();
            return versionRange.containsVersion(modVersion);
        }).orElse(false);
    }


    public record GunPack(Path path, String name) {
    }
}

