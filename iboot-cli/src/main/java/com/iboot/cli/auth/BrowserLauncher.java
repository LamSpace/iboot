/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.cli.auth;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * 打开系统默认浏览器。
 *
 * <p>优先使用 {@link Desktop}；不可用时按操作系统回退到 {@code xdg-open}、{@code open}
 * 或 {@code cmd /c start}。失败时返回可复制的授权地址，由登录命令提示用户手动打开。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class BrowserLauncher {

    private BrowserLauncher() {
    }

    /**
     * 打开授权地址。
     *
     * @param url 授权地址
     * @throws CliException 浏览器无法启动时抛出
     */
    public static void open(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (IOException | RuntimeException ignored) {
            // 回退到命令行打开方式。
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> command;
        if (os.contains("mac")) {
            command = List.of("open", url);
        } else if (os.contains("win")) {
            command = List.of("cmd", "/c", "start", "", url);
        } else {
            command = List.of("xdg-open", url);
        }
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            throw new CliException(ExitCode.BUSINESS,
                    "无法自动打开浏览器，请手动访问：" + url, e);
        }
    }
}
