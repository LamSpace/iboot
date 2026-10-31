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

/**
 * 操作日志：{@code @Log} 注解、切面与持久化链路。
 *
 * <p>当前各服务直写 {@code sys_operate_log}。这是过渡形态，等
 * {@code iboot-monitor} 拆分时改为事件投递。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
package com.iboot.kernel.logging;
