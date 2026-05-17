package com.tacz.guns.api.client.animation;

import com.tacz.guns.api.client.animation.gltf.AccessorModel;
import com.tacz.guns.api.client.animation.gltf.AnimationModel;
import com.tacz.guns.api.client.animation.gltf.AnimationStructure;
import com.tacz.guns.api.client.animation.gltf.NodeModel;
import com.tacz.guns.api.client.animation.gltf.accessor.AccessorData;
import com.tacz.guns.api.client.animation.gltf.accessor.AccessorFloatData;
import com.tacz.guns.api.client.animation.interpolator.CustomInterpolator;
import com.tacz.guns.api.client.animation.interpolator.InterpolatorUtil;
import com.tacz.guns.client.resource.pojo.animation.bedrock.*;
import com.tacz.guns.util.math.MathUtil;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Animations {
    public static AnimationController createControllerFromGltf(@Nonnull AnimationStructure structure, @Nonnull AnimationListenerSupplier supplier) {
        List<ObjectAnimation> prototypes = new ArrayList<>();

        List<AnimationModel> animationModels = structure.getAnimationModels();
        for (AnimationModel animationModel : animationModels) {
            ObjectAnimation animation = new ObjectAnimation(animationModel.getName());

            // 蛻晏ｧ句喧蜉ｨ逕ｻ霓ｨ驕・
            List<AnimationModel.Channel> channelModels = animationModel.getChannels();
            for (AnimationModel.Channel channelModel : channelModels) {
                ObjectAnimationChannel channel = new ObjectAnimationChannel(ObjectAnimationChannel.ChannelType.valueOf(channelModel.path().toUpperCase(Locale.ENGLISH)));
                AnimationModel.Sampler sampler = channelModel.sampler();

                // 蛻晏ｧ句喧霓ｨ驕鍋噪闃らせ蜷咲ｧｰ蜥梧薯蛟ｼ蝎ｨ
                AnimationModel.Interpolation interpolation = sampler.interpolation();
                NodeModel nodeModel = channelModel.nodeModel();
                // 蝗帛・謨ｰ髴隕∫音谿顔噪謠貞ｼ
                if (channel.type.equals(ObjectAnimationChannel.ChannelType.ROTATION) && interpolation.equals(AnimationModel.Interpolation.LINEAR)) {
                    channel.interpolator = InterpolatorUtil.fromInterpolation(InterpolatorUtil.InterpolatorType.SLERP);
                } else {
                    channel.interpolator = InterpolatorUtil.fromInterpolation(InterpolatorUtil.InterpolatorType.valueOf(interpolation.name()));
                }
                channel.node = nodeModel.getName();

                // 隶｡邂怜・蜷・ｸｪ蛻晏ｧ句ｼ逧・・
                AnimationListener animationListener = supplier.supplyListeners(channel.node, channel.type);
                if (animationListener == null) {
                    continue;
                }
                float[] inverseValue = animationListener.initialValue();
                if (channel.type == ObjectAnimationChannel.ChannelType.ROTATION) {
                    if (inverseValue.length == 3) {
                        inverseValue = MathUtil.toQuaternion(inverseValue[0], inverseValue[1], inverseValue[2]);
                    }
                    inverseValue = MathUtil.inverseQuaternion(inverseValue);
                } else if (channel.type == ObjectAnimationChannel.ChannelType.TRANSLATION) {
                    inverseValue[0] = -inverseValue[0];
                    inverseValue[1] = -inverseValue[1];
                    inverseValue[2] = -inverseValue[2];
                }

                // 蛻晏ｧ句喧霓ｨ驕鍋噪蜈ｳ髞ｮ蟶ｧ譌ｶ髣ｴ蜥悟・髞ｮ蟶ｧ謨ｰ蛟ｼ
                // 蜈ｳ髞ｮ蟶ｧ譌ｶ髣ｴ逧・ｮｿ髣ｮ蝎ｨ
                AccessorModel input = sampler.input();
                AccessorData inputData = input.getAccessorData();
                if (!(inputData instanceof AccessorFloatData inputFloatData)) {
                    throw new IllegalArgumentException("Input data is not an AccessorFloatData, but " + inputData.getClass());
                }
                // 蜈ｳ髞ｮ蟶ｧ譌ｶ髣ｴ逧・ｮｿ髣ｮ蝎ｨ
                AccessorModel output = sampler.output();
                AccessorData outputData = output.getAccessorData();
                if (!(outputData instanceof AccessorFloatData outputFloatData)) {
                    throw new IllegalArgumentException("Output data is not an AccessorFloatData, but " + inputData.getClass());
                }
                int numKeyElements = inputFloatData.getNumElements();
                int numValuesElements = outputFloatData.getTotalNumComponents() / numKeyElements;
                float[] keyframeTimeS = new float[numKeyElements];
                float[][] values = new float[numKeyElements][numValuesElements];
                for (int i = 0; i < numKeyElements; i++) {
                    keyframeTimeS[i] = inputFloatData.get(i);
                    for (int j = 0; j < numValuesElements; j++) {
                        values[i][j] = outputFloatData.get(i * numValuesElements + j);
                    }
                    if (channel.type == ObjectAnimationChannel.ChannelType.ROTATION) {
                        values[i] = MathUtil.toEulerAngles(values[i]);
                        values[i] = MathUtil.toQuaternion(-values[i][0], -values[i][1], values[i][2]);
                        values[i] = MathUtil.mulQuaternion(inverseValue, values[i]);
                    } else if (channel.type == ObjectAnimationChannel.ChannelType.TRANSLATION) {
                        values[i][0] = -values[i][0] + inverseValue[0];
                        values[i][1] = -(-values[i][1] + inverseValue[1]);
                        values[i][2] = values[i][2] + inverseValue[2];
                    }
                }
                channel.content.keyframeTimeS = keyframeTimeS;
                channel.content.values = values;

                // 蜉霓ｽ螳梧園譛牙・螳ｹ蜷守ｼ冶ｯ第薯蛟ｼ蝎ｨ
                channel.interpolator.compile(channel.content);

                // 蟆・ｽｨ驕捺ｷｻ蜉蛻ｰ蜉ｨ逕ｻ
                animation.addChannel(channel);
            }

            // 蟆・勘逕ｻ豺ｻ蜉蛻ｰ蜴溷梛蛻苓｡ｨ荳ｭ
            prototypes.add(animation);
        }
        return new AnimationController(prototypes, supplier);
    }

    public static AnimationController createControllerFromBedrock(BedrockAnimationFile animationFile, AnimationListenerSupplier supplier) {
        return new AnimationController(createAnimationFromBedrock(animationFile), supplier);
    }

    public static @Nonnull List<ObjectAnimation> createAnimationFromBedrock(BedrockAnimationFile animationFile) {
        List<ObjectAnimation> result = new ArrayList<>();
        for (Map.Entry<String, BedrockAnimation> animationEntry : animationFile.getAnimations().entrySet()) {
            ObjectAnimation animation = new ObjectAnimation(animationEntry.getKey());
            BedrockAnimation bedrockAnimation = animationEntry.getValue();
            if (bedrockAnimation.getBones() != null) {
                for (Map.Entry<String, AnimationBone> boneEntry : bedrockAnimation.getBones().entrySet()) {
                    AnimationBone bone = boneEntry.getValue();
                    AnimationKeyframes translationKeyframes = bone.getPosition();
                    AnimationKeyframes rotationKeyframes = bone.getRotation();
                    AnimationKeyframes scaleKeyframes = bone.getScale();
                    if (translationKeyframes != null) {
                        ObjectAnimationChannel translationChannel = new ObjectAnimationChannel(ObjectAnimationChannel.ChannelType.TRANSLATION);
                        translationChannel.node = boneEntry.getKey();
                        translationChannel.interpolator = new CustomInterpolator();
                        // 蟆・ｽ咲ｧｻ謨ｰ謐ｮ霓ｬ遘ｻ霑・AnimationChannel
                        writeBedrockTranslation(translationChannel, bone.getPosition());
                        translationChannel.interpolator.compile(translationChannel.content);
                        animation.addChannel(translationChannel);
                    }
                    if (rotationKeyframes != null) {
                        ObjectAnimationChannel rotationChannel = new ObjectAnimationChannel(ObjectAnimationChannel.ChannelType.ROTATION);
                        rotationChannel.node = boneEntry.getKey();
                        rotationChannel.interpolator = new CustomInterpolator();
                        // 蟆・雷霓ｬ謨ｰ謐ｮ霓ｬ遘ｻ霑・AnimationChannel
                        writeBedrockRotation(rotationChannel, bone.getRotation());
                        rotationChannel.interpolator.compile(rotationChannel.content);
                        animation.addChannel(rotationChannel);
                    }
                    if (scaleKeyframes != null) {
                        ObjectAnimationChannel scaleChannel = new ObjectAnimationChannel(ObjectAnimationChannel.ChannelType.SCALE);
                        scaleChannel.node = boneEntry.getKey();
                        scaleChannel.interpolator = new CustomInterpolator();
                        // 蟆・ｼｩ謾ｾ謨ｰ謐ｮ霓ｬ遘ｻ霑・AnimationChannel
                        writeBedrockScale(scaleChannel, bone.getScale());
                        scaleChannel.interpolator.compile(scaleChannel.content);
                        animation.addChannel(scaleChannel);
                    }
                }
            }
            // 蟆・｣ｰ髻ｳ謨ｰ謐ｮ霓ｬ遘ｻ蛻ｰ ObjectAnimation 荳ｭ
            SoundEffectKeyframes soundEffectKeyframes = bedrockAnimation.getSoundEffects();
            if (soundEffectKeyframes != null) {
                ObjectAnimationSoundChannel soundChannel = new ObjectAnimationSoundChannel();
                soundChannel.content = new AnimationSoundChannelContent();
                int keyframeNum = soundEffectKeyframes.getKeyframes().size();
                soundChannel.content.keyframeTimeS = new double[keyframeNum];
                soundChannel.content.keyframeSoundName = new Identifier[keyframeNum];
                int i = 0;
                for (Map.Entry<Double, Identifier> entry : soundEffectKeyframes.getKeyframes().double2ObjectEntrySet()) {
                    soundChannel.content.keyframeTimeS[i] = entry.getKey();
                    soundChannel.content.keyframeSoundName[i] = entry.getValue();
                    i++;
                }
                animation.setSoundChannel(soundChannel);
            }
            result.add(animation);
        }
        return result;
    }

    private static void writeBedrockTranslation(ObjectAnimationChannel animationChannel, AnimationKeyframes keyframes) {
        // 蝓ｺ蟯ｩ迚亥勘逕ｻ荳ｭ蛯ｨ蟄倡噪蜉ｨ逕ｻ謨ｰ謐ｮ荳ｺ逶ｸ蟇ｹ蛟ｼ・瑚・tac 逧・勘逕ｻ邉ｻ扈滉ｽｿ逕ｨ逧・弍扈晏ｯｹ蛟ｼ・梧園莉･髴隕∝匠蜉蛻晏ｧ句ｼ縲・
        // 豁､螟・ｰｱ譏ｯ蝨ｨ闔ｷ蜿門勘逕ｻ謨ｰ謐ｮ逧・・蟋句ｼ縲・
        Double2ObjectRBTreeMap<AnimationKeyframes.Keyframe> keyframesMap = keyframes.getKeyframes();
        animationChannel.content.keyframeTimeS = new float[keyframesMap.size()];
        animationChannel.content.values = new float[keyframesMap.size()][];
        animationChannel.content.lerpModes = new AnimationChannelContent.LerpMode[keyframesMap.size()];
        int index = 0;
        for (Double2ObjectMap.Entry<AnimationKeyframes.Keyframe> entry : keyframesMap.double2ObjectEntrySet()) {
            // 蜀吝・蜈ｳ髞ｮ蟶ｧ譌ｶ髣ｴ
            animationChannel.content.keyframeTimeS[index] = (float) entry.getDoubleKey();
            // 蜀吝・蜈ｳ髞ｮ蟶ｧ謨ｰ蛟ｼ縲・
            AnimationKeyframes.Keyframe keyframe = entry.getValue();
            if (keyframe.pre() != null || keyframe.post() != null) {
                if (keyframe.pre() != null && keyframe.post() != null) {
                    animationChannel.content.values[index] = new float[6];
                    Vector3f pre = new Vector3f(keyframe.pre());
                    Vector3f post = new Vector3f(keyframe.post());
                    pre.mul(1 / 16f, 1 / 16f, 1 / 16f);
                    post.mul(1 / 16f, 1 / 16f, 1 / 16f);
                    readVector3fToArray(animationChannel.content.values[index], pre, 0);
                    readVector3fToArray(animationChannel.content.values[index], post, 3);
                } else if (keyframe.pre() != null) {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f pre = new Vector3f(keyframe.pre());
                    pre.mul(1 / 16f, 1 / 16f, 1 / 16f);
                    readVector3fToArray(animationChannel.content.values[index], pre, 0);
                } else {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f post = new Vector3f(keyframe.post());
                    post.mul(1 / 16f, 1 / 16f, 1 / 16f);
                    readVector3fToArray(animationChannel.content.values[index], post, 0);
                }
            } else if (keyframe.data() != null) {
                animationChannel.content.values[index] = new float[3];
                Vector3f data = new Vector3f(keyframe.data());
                data.mul(1 / 16f, 1 / 16f, 1 / 16f);
                readVector3fToArray(animationChannel.content.values[index], data, 0);
            }
            // 蜀吝・蜈ｳ髞ｮ蟶ｧ謠貞ｼ邀ｻ蝙・
            String lerpModeName = keyframe.lerpMode();
            if (lerpModeName != null) {
                try {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.valueOf(lerpModeName.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException e) {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
                }
            } else {
                animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
            }
            index++;
        }
    }

    private static void writeBedrockRotation(ObjectAnimationChannel animationChannel, AnimationKeyframes keyframes) {
        Double2ObjectRBTreeMap<AnimationKeyframes.Keyframe> keyframesMap = keyframes.getKeyframes();
        animationChannel.content.keyframeTimeS = new float[keyframesMap.size()];
        animationChannel.content.values = new float[keyframesMap.size()][];
        animationChannel.content.lerpModes = new AnimationChannelContent.LerpMode[keyframesMap.size()];
        int index = 0;
        for (Double2ObjectMap.Entry<AnimationKeyframes.Keyframe> entry : keyframesMap.double2ObjectEntrySet()) {
            // 蜀吝・蜈ｳ髞ｮ蟶ｧ譌ｶ髣ｴ
            animationChannel.content.keyframeTimeS[index] = (float) entry.getDoubleKey();
            // 蜀吝・蜈ｳ髞ｮ蟶ｧ謨ｰ蛟ｼ縲・
            AnimationKeyframes.Keyframe keyframe = entry.getValue();
            if (keyframe.pre() != null || keyframe.post() != null) {
                if (keyframe.pre() != null && keyframe.post() != null) {
                    animationChannel.content.values[index] = new float[6];
                    Vector3f pre = new Vector3f(keyframe.pre());
                    Vector3f post = new Vector3f(keyframe.post());
                    toAngle(pre);
                    toAngle(post);
                    animationChannel.content.values[index][0] = pre.x();
                    animationChannel.content.values[index][1] = pre.y();
                    animationChannel.content.values[index][2] = pre.z();
                    animationChannel.content.values[index][3] = post.x();
                    animationChannel.content.values[index][4] = post.y();
                    animationChannel.content.values[index][5] = post.z();
                } else if (keyframe.pre() != null) {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f pre =  new Vector3f(keyframe.pre());
                    toAngle(pre);
                    animationChannel.content.values[index][0] = pre.x();
                    animationChannel.content.values[index][1] = pre.y();
                    animationChannel.content.values[index][2] = pre.z();
                } else {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f post =  new Vector3f(keyframe.post());
                    toAngle(post);
                    animationChannel.content.values[index][0] = post.x();
                    animationChannel.content.values[index][1] = post.y();
                    animationChannel.content.values[index][2] = post.z();
                }
            } else if (keyframe.data() != null) {
                animationChannel.content.values[index] = new float[3];
                Vector3f data =  new Vector3f(keyframe.data());
                toAngle(data);
                animationChannel.content.values[index][0] = data.x();
                animationChannel.content.values[index][1] = data.y();
                animationChannel.content.values[index][2] = data.z();
            }
            String lerpModeName = keyframe.lerpMode();
            if (lerpModeName != null) {
                if (lerpModeName.equals(AnimationChannelContent.LerpMode.CATMULLROM.name().toLowerCase())) {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.CATMULLROM;
                } else {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
                }
            } else {
                animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
            }
            index++;
        }
    }

    private static void writeBedrockScale(ObjectAnimationChannel animationChannel, AnimationKeyframes keyframes) {
        Double2ObjectRBTreeMap<AnimationKeyframes.Keyframe> keyframesMap = keyframes.getKeyframes();
        animationChannel.content.keyframeTimeS = new float[keyframesMap.size()];
        animationChannel.content.values = new float[keyframesMap.size()][];
        animationChannel.content.lerpModes = new AnimationChannelContent.LerpMode[keyframesMap.size()];
        int index = 0;
        for (Double2ObjectMap.Entry<AnimationKeyframes.Keyframe> entry : keyframesMap.double2ObjectEntrySet()) {
            // 蜀吝・蜈ｳ髞ｮ蟶ｧ譌ｶ髣ｴ
            animationChannel.content.keyframeTimeS[index] = (float) entry.getDoubleKey();
            // 蜀吝・蜈ｳ髞ｮ蟶ｧ謨ｰ蛟ｼ縲・
            AnimationKeyframes.Keyframe keyframe = entry.getValue();
            if (keyframe.pre() != null || keyframe.post() != null) {
                if (keyframe.pre() != null && keyframe.post() != null) {
                    animationChannel.content.values[index] = new float[6];
                    Vector3f pre = keyframe.pre();
                    Vector3f post = keyframe.post();
                    readVector3fToArray(animationChannel.content.values[index], pre, 0);
                    readVector3fToArray(animationChannel.content.values[index], post, 3);
                } else if (keyframe.pre() != null) {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f pre = keyframe.pre();
                    readVector3fToArray(animationChannel.content.values[index], pre, 0);
                } else {
                    animationChannel.content.values[index] = new float[3];
                    Vector3f post = keyframe.post();
                    readVector3fToArray(animationChannel.content.values[index], post, 0);
                }
            } else if (keyframe.data() != null) {
                animationChannel.content.values[index] = new float[3];
                Vector3f data = keyframe.data();
                readVector3fToArray(animationChannel.content.values[index], data, 0);
            }
            // 蜀吝・蜈ｳ髞ｮ蟶ｧ謠貞ｼ邀ｻ蝙・
            String lerpModeName = keyframe.lerpMode();
            if (lerpModeName != null) {
                try {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.valueOf(lerpModeName.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException e) {
                    animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
                }
            } else {
                animationChannel.content.lerpModes[index] = AnimationChannelContent.LerpMode.LINEAR;
            }
            index++;
        }
    }

    private static void toAngle(Vector3f vector3f) {
        vector3f.set((float) Math.toRadians(vector3f.x()), (float) Math.toRadians(vector3f.y()), (float) Math.toRadians(vector3f.z()));
    }

    private static void readVector3fToArray(float[] array, Vector3f vector3f, int offset) {
        array[offset] = vector3f.x();
        array[offset + 1] = vector3f.y();
        array[offset + 2] = vector3f.z();
    }
}

