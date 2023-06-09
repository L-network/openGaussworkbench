/**
 Copyright  starBlues.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.opengauss.admin.web.listener;

import com.gitee.starblues.core.PluginInfo;
import com.gitee.starblues.integration.listener.PluginListener;
import org.opengauss.admin.system.service.ISysPluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;


/**
 * Plugin Listener
 */
@Component
public class MypluginListener implements PluginListener {

    private static final Logger log = LoggerFactory.getLogger(MypluginListener.class);


    @Autowired
    private ISysPluginService sysPluginService;

    @Override
    public void loadSuccess(PluginInfo pluginInfo) {
        log.info("plugin [{}] load success.", pluginInfo.getPluginId());
    }

    @Override
    public void loadFailure(Path path, Throwable throwable) {
        StringWriter errorsWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errorsWriter));
        log.error("plugin[{}] load fail. {}", path, errorsWriter.toString());
    }

    @Override
    public void unLoadSuccess(PluginInfo pluginInfo) {
        log.info("plugin[{}] uninstall success.", pluginInfo.getPluginId());
        sysPluginService.uninstallPluginByPluginId(pluginInfo.getPluginId());
    }

    @Override
    public void unLoadFailure(PluginInfo pluginInfo, Throwable throwable) {
        StringWriter errorsWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errorsWriter));
        log.error("plugin[{}] uninstall fail. {}", pluginInfo.getPluginId(), errorsWriter.toString());
    }

    @Override
    public void startSuccess(PluginInfo pluginInfo) {
        log.info("plugin[{}] start success", pluginInfo.getPluginId());
    }

    @Override
    public void startFailure(PluginInfo pluginInfo, Throwable throwable) {
        StringWriter errorsWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errorsWriter));
        log.error("plugin[{}] start fail. {}", pluginInfo.getPluginId(), errorsWriter.toString());
        if (sysPluginService.getByPluginId(pluginInfo.getPluginId()) == null) {
            try{
                File file = new File(pluginInfo.getPluginDescriptor().getPluginPath());
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e){}
        }
    }

    @Override
    public void stopSuccess(PluginInfo pluginInfo) {
        log.info("plugin[{}] stop success.", pluginInfo.getPluginId());
    }

    @Override
    public void stopFailure(PluginInfo pluginInfo, Throwable throwable) {
        StringWriter errorsWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errorsWriter));
        log.error("plugin[{}] stop fail. {}", pluginInfo.getPluginId(), errorsWriter.toString());
    }
}
