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

package com.iboot.system.infrastructure.config;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * TCC 专用 Druid 数据源。
 *
 * <p>Seata 2.5.0 的 {@code excludes-for-auto-proxying} 按数据源**类名**匹配，
 * 而不是 Bean 名。主数据源与 TCC 数据源都是 Druid 连接池，因此用独立子类区分，
 * 只把本类排除在 AT 自动代理之外。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see TccDataSourceConfig
 */
public class TccDruidDataSource extends DruidDataSource {
}
