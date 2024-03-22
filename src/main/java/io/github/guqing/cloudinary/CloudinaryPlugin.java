package io.github.guqing.cloudinary;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

@Component
public class CloudinaryPlugin extends BasePlugin {

    public CloudinaryPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }
}
