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

package com.iboot.notify.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置。
 *
 * <p>只扫描本服务的 Mapper 包；共享内核不提供 Mapper，避免与其它服务的扫描范围重叠。
 * 本服务不挂载数据权限拦截器：消息与公告按接收人关联表与权限码控制可见性，不涉及行级数据权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
@MapperScan("com.iboot.notify.infrastructure.persistence.mapper")
public class MyBatisConfig {
}
