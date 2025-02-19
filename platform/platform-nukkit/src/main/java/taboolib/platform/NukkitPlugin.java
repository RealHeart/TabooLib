package taboolib.platform;

import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import taboolib.common.LifeCycle;
import taboolib.common.TabooLibCommon;
import taboolib.common.env.IsolatedClassLoader;
import taboolib.common.io.Project1Kt;
import taboolib.common.platform.Platform;
import taboolib.common.platform.PlatformSide;
import taboolib.common.platform.Plugin;
import taboolib.common.platform.function.ExecutorKt;

import java.io.File;
import java.net.URL;

/**
 * TabooLib
 * taboolib.platform.NukkitPlugin
 *
 * @author sky
 * @since 2021/6/26 8:22 下午
 */
@SuppressWarnings({"Convert2Lambda", "FieldCanBeLocal"})
@PlatformSide(Platform.NUKKIT)
public class NukkitPlugin extends PluginBase {

    @Nullable
    private static Plugin pluginInstance;
    private static NukkitPlugin instance;
    private static Class<?> delegateClass;
    private static Object delegateObject;

    static {
        if (!IsolatedClassLoader.isEnabled()) {
            TabooLibCommon.lifeCycle(LifeCycle.CONST, Platform.NUKKIT);
            if (TabooLibCommon.isKotlinEnvironment()) {
                pluginInstance = Project1Kt.findImplementation(Plugin.class);
            }
        } else {
            try {
                IsolatedClassLoader loader = new IsolatedClassLoader(
                        new URL[]{NukkitPlugin.class.getProtectionDomain().getCodeSource().getLocation()},
                        NukkitPlugin.class.getClassLoader()
                );
                delegateClass = Class.forName("taboolib.platform.NukkitPluginDelegate", true, loader);
                delegateObject = delegateClass.getConstructor().newInstance();
                delegateClass.getMethod("onConst").invoke(delegateObject);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public NukkitPlugin() {
        instance = this;
        
        if (!IsolatedClassLoader.isEnabled()) {
            TabooLibCommon.lifeCycle(LifeCycle.INIT);
        } else {
            try {
                delegateClass.getMethod("onInit").invoke(delegateObject);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void onLoad() {
        if (!IsolatedClassLoader.isEnabled()) {
            TabooLibCommon.lifeCycle(LifeCycle.LOAD);
            if (pluginInstance == null) {
                pluginInstance = Project1Kt.findImplementation(Plugin.class);
            }
            if (pluginInstance != null && !TabooLibCommon.isStopped()) {
                pluginInstance.onLoad();
            }
        } else {
            try {
                delegateClass.getMethod("onLoad").invoke(delegateObject);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void onEnable() {
        if (!IsolatedClassLoader.isEnabled()) {
            TabooLibCommon.lifeCycle(LifeCycle.ENABLE);
            if (!TabooLibCommon.isStopped()) {
                if (pluginInstance != null) {
                    pluginInstance.onEnable();
                }
                try {
                    ExecutorKt.startExecutor();
                } catch (NoClassDefFoundError ignored) {
                }
            }
            if (!TabooLibCommon.isStopped()) {
                Server.getInstance().getScheduler().scheduleTask(this, new Runnable() {
                    @Override
                    public void run() {
                        TabooLibCommon.lifeCycle(LifeCycle.ACTIVE);
                        if (pluginInstance != null) {
                            pluginInstance.onActive();
                        }
                    }
                });
            }
        } else {
            try {
                delegateClass.getMethod("onEnable").invoke(delegateObject);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public void onDisable() {
        if (!IsolatedClassLoader.isEnabled()) {
            TabooLibCommon.lifeCycle(LifeCycle.DISABLE);
            if (pluginInstance != null && !TabooLibCommon.isStopped()) {
                pluginInstance.onDisable();
            }
        } else {
            try {
                delegateClass.getMethod("onDisable").invoke(delegateObject);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public File getFile() {
        return super.getFile();
    }

    @NotNull
    public static NukkitPlugin getInstance() {
        return instance;
    }

    @Nullable
    public static Plugin getPluginInstance() {
        return pluginInstance;
    }
}
