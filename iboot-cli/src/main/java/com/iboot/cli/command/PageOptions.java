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

package com.iboot.cli.command;

import picocli.CommandLine.Option;

/**
 * 分页选项。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class PageOptions {

    /**
     * 页码，从 1 开始。
     */
    @Option(names = {"-p", "--page"}, defaultValue = "1", description = "页码，从 1 开始")
    public int page = 1;

    /**
     * 每页数量。
     */
    @Option(names = {"-s", "--size"}, defaultValue = "20", description = "每页数量")
    public int size = 20;
}
